package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventorycount.*;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.*;
import com.smartflow.smestocksensebackend.repository.*;
import com.smartflow.smestocksensebackend.service.InventoryCountService;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class InventoryCountServiceImpl implements InventoryCountService {
    private final InventoryCountRepository countRepository;
    private final InventoryCountDetailRepository detailRepository;
    private final InventoryLevelRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryTransactionService inventoryTransactionService;

    @Override @Transactional
    public InventoryCountResponse create(InventoryCountRequests.Create request) {
        Employee actor = actor();
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId()).orElseThrow(() -> new NotFoundException("Kho khong ton tai."));
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) throw new BadRequestException("Kho da ngung hoat dong.");
        if (countRepository.existsByWarehouseIdAndStatusIn(warehouse.getId(), Set.of(InventoryCountStatus.DANG_KIEM_KE))) throw new ConflictException("Kho dang co dot kiem ke chua xu ly xong.");
        List<InventoryLevel> stocks = inventoryRepository.findByWarehouseId(warehouse.getId());
        Map<Long, Integer> quantities = new HashMap<>();
        stocks.forEach(s -> quantities.put(s.getProduct().getId(), s.getQuantity()));
        List<Product> products;
        if (request.productIds() == null || request.productIds().isEmpty()) {
            products = stocks.stream().map(InventoryLevel::getProduct).filter(p -> p.getStatus() == ProductStatus.HOAT_DONG).distinct().toList();
        } else {
            Set<Long> ids = new LinkedHashSet<>(request.productIds());
            if (ids.size() != request.productIds().size()) throw new BadRequestException("Danh sach san pham bi trung.");
            products = productRepository.findAllById(ids);
            if (products.size() != ids.size() || products.stream().anyMatch(p -> p.getStatus() != ProductStatus.HOAT_DONG)) throw new BadRequestException("San pham khong ton tai hoac da ngung hoat dong.");
        }
        if (products.isEmpty()) throw new BadRequestException("Khong co san pham hop le de kiem ke.");
        InventoryCount count = new InventoryCount(); count.setCode("KK-" + UUID.randomUUID().toString().substring(0,12).toUpperCase());
        count.setWarehouse(warehouse); count.setStatus(InventoryCountStatus.DANG_KIEM_KE); count.setNote(request.note()); count.setCreatedBy(actor);
        count = countRepository.saveAndFlush(count);
        List<InventoryCountDetail> lines = new ArrayList<>();
        for (Product p : products) { InventoryCountDetail d = new InventoryCountDetail(); d.setInventoryCount(count); d.setProduct(p); d.setSystemQuantity(quantities.getOrDefault(p.getId(),0)); lines.add(d); }
        lines = detailRepository.saveAllAndFlush(lines);
        return InventoryCountResponse.from(count, lines);
    }

    @Override @Transactional(readOnly=true)
    public Page<InventoryCountResponse> list(Long warehouseId, String status, Pageable pageable) {
        InventoryCountStatus parsed = parse(status); Page<InventoryCount> page;
        if (warehouseId != null && parsed != null) page=countRepository.findByWarehouseIdAndStatus(warehouseId,parsed,pageable);
        else if (warehouseId != null) page=countRepository.findByWarehouseId(warehouseId,pageable);
        else if (parsed != null) page=countRepository.findByStatus(parsed,pageable); else page=countRepository.findAll(pageable);
        return page.map(c -> InventoryCountResponse.from(c, List.of()));
    }
    @Override @Transactional(readOnly=true) public InventoryCountResponse get(Long id) { InventoryCount c=find(id); return response(c); }

    @Override @Transactional
    public InventoryCountResponse recordActual(Long id, Long detailId, InventoryCountRequests.RecordActual request) {
        InventoryCount c=find(id); ensureOpen(c); InventoryCountDetail d=detailRepository.findById(detailId).orElseThrow(() -> new NotFoundException("Dong kiem ke khong ton tai."));
        if (!d.getInventoryCount().getId().equals(id)) throw new NotFoundException("Dong kiem ke khong thuoc dot nay.");
        if (!Objects.equals(d.getVersion(),request.version())) throw new ConflictException("Dong kiem ke da duoc cap nhat.");
        d.setActualQuantity(request.actualQuantity()); d.setDifferenceQuantity(request.actualQuantity()-d.getSystemQuantity()); d.setNote(request.note()); detailRepository.saveAndFlush(d); return response(c);
    }
    @Override @Transactional
    public InventoryCountResponse finalizeCount(Long id, InventoryCountRequests.Finalize request) {
        InventoryCount c=find(id); ensureOpen(c); checkVersion(c,request.version()); List<InventoryCountDetail> lines=detailRepository.findByInventoryCountIdOrderByIdAsc(id);
        if (lines.stream().anyMatch(d -> d.getActualQuantity()==null)) throw new ConflictException("Phai nhap so luong thuc te cho tat ca san pham.");
        Warehouse warehouse = c.getWarehouse();
        for (InventoryCountDetail d : lines) {
            int before = d.getSystemQuantity();
            int after = d.getActualQuantity();
            int diff = after - before;
            d.setDifferenceQuantity(diff);
            if (diff == 0) continue;
            InventoryLevel stock = inventoryRepository.findByProductIdAndWarehouseIdForUpdate(d.getProduct().getId(), warehouse.getId())
                    .orElseThrow(() -> new NotFoundException("Ton kho khong ton tai de dieu chinh kiem ke."));
            stock.setQuantity(after);
            inventoryRepository.saveAndFlush(stock);
            inventoryTransactionService.recordTransaction(
                    d.getProduct().getId(),
                    warehouse.getId(),
                    diff > 0 ? InventoryTransactionType.DIEU_CHINH_TANG : InventoryTransactionType.DIEU_CHINH_GIAM,
                    Math.abs(diff),
                    before,
                    after,
                    null,
                    "Dieu chinh kiem ke " + c.getCode());
        }
        c.setStatus(InventoryCountStatus.DA_CHOT); c.setFinalizedBy(actor()); c.setFinalizedAt(LocalDateTime.now()); countRepository.saveAndFlush(c); return InventoryCountResponse.from(c,lines);
    }
    @Override @Transactional
    public InventoryCountResponse cancel(Long id, InventoryCountRequests.Cancel request) {
        InventoryCount c=find(id); ensureOpen(c); checkVersion(c,request.version()); c.setStatus(InventoryCountStatus.DA_HUY); c.setCancellationReason(request.reason().trim()); c.setCancelledBy(actor()); c.setCancelledAt(LocalDateTime.now()); countRepository.saveAndFlush(c); return response(c);
    }
    private InventoryCountResponse response(InventoryCount c){ return InventoryCountResponse.from(c,detailRepository.findByInventoryCountIdOrderByIdAsc(c.getId())); }
    private InventoryCount find(Long id){ return countRepository.findById(id).orElseThrow(() -> new NotFoundException("Dot kiem ke khong ton tai.")); }
    private void ensureOpen(InventoryCount c){ if(c.getStatus()!=InventoryCountStatus.DANG_KIEM_KE) throw new ConflictException("Dot kiem ke da chot hoac da huy."); }
    private void checkVersion(InventoryCount c,Long v){ if(!Objects.equals(c.getVersion(),v)) throw new ConflictException("Dot kiem ke da duoc cap nhat."); }
    private InventoryCountStatus parse(String s){ if(s==null||s.isBlank()) return null; try{return InventoryCountStatus.valueOf(s);}catch(Exception e){throw new BadRequestException("Trang thai kiem ke khong hop le.");} }
    private Employee actor(){ Authentication a=SecurityContextHolder.getContext().getAuthentication(); if(a!=null&&a.getPrincipal() instanceof Employee e) return e; throw new MissingRoleException("Khong xac dinh duoc nguoi dung."); }
}
