package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.request.CreateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.WarehouseService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lớp triển khai các phương thức nghiệp vụ của WarehouseService.
 */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    /**
     * Lấy danh sách kho hàng có hỗ trợ tìm kiếm động và lọc trạng thái.
     * API này chỉ đọc danh sách kho, không thay đổi dữ liệu.
     */
    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehouses(String keyword, String status) {
        WarehouseStatus parsedStatus = parseStatus(status);
        String keywordLike = normalizeKeyword(keyword);
        Specification<Warehouse> specification = buildSpecification(keywordLike, parsedStatus);

        return warehouseRepository.findAll(specification).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    /**
     * Nghiệp vụ thêm mới kho hàng vào hệ thống:
     * - Chuẩn hóa mã kho (loại bỏ khoảng trắng, chuyển chữ in hoa).
     * - Mã kho là định danh nghiệp vụ duy nhất nên không được trùng.
     * - Thiết lập trạng thái mặc định là HOAT_DONG nếu không truyền.
     */
    @Override
    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        String code = request.maKho().trim().toUpperCase();

        if (warehouseRepository.existsByCodeIgnoreCase(code)) {
            throw new FieldValidationException(Map.of("code", "Mã kho đã tồn tại."));
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(request.tenKho().trim());
        warehouse.setAddress(normalizeOptional(request.diaChi()));
        warehouse.setStatus(parseStatusOrDefault(request.trangThai()));

        Warehouse savedWarehouse = warehouseRepository.saveAndFlush(warehouse);
        return WarehouseResponse.from(savedWarehouse);
    }

    /**
     * Nghiệp vụ cập nhật thông tin kho hàng.
     * Chỉ cho phép sửa tên kho, địa chỉ và trạng thái. Không cho phép sửa mã kho.
     * Ghi chú: không cho đổi mã kho để tránh ảnh hưởng dữ liệu nhập/xuất/tồn sau này.
     */
    @Override
    @Transactional
    public WarehouseResponse updateWarehouse(Long id, UpdateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));

        // Ghi chú: không cho đổi mã kho để tránh ảnh hưởng dữ liệu nhập/xuất/tồn sau này.
        warehouse.setName(request.tenKho().trim());
        warehouse.setAddress(normalizeOptional(request.diaChi()));

        // Validate trạng thái hoạt động: chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG
        WarehouseStatus parsedStatus;
        try {
            parsedStatus = WarehouseStatus.valueOf(request.trangThai().trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Trạng thái chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.");
        }
        warehouse.setStatus(parsedStatus);

        Warehouse savedWarehouse = warehouseRepository.saveAndFlush(warehouse);
        return WarehouseResponse.from(savedWarehouse);
    }

    /**
     * Ngừng hoạt động kho hàng (soft delete).
     * Ghi chú rõ: không xóa vật lý kho để bảo toàn dữ liệu lịch sử nhập/xuất/tồn.
     */
    @Override
    @Transactional
    public WarehouseResponse deactivateWarehouse(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));

        // Ghi chú rõ: không xóa vật lý kho để bảo toàn dữ liệu lịch sử nhập/xuất/tồn.
        warehouse.setStatus(WarehouseStatus.NGUNG_HOAT_DONG);

        Warehouse savedWarehouse = warehouseRepository.saveAndFlush(warehouse);
        return WarehouseResponse.from(savedWarehouse);
    }

    /**
     * Chuẩn hóa các trường thông tin tùy chọn: Trả về null nếu trống hoặc chỉ chứa khoảng trắng.
     */
    private String normalizeOptional(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim();
    }

    /**
     * Phân tích chuỗi trạng thái hoặc trả về giá trị mặc định là HOAT_DONG nếu không truyền trạng thái.
     * Ném ra ngoại lệ BadRequestException nếu giá trị không hợp lệ.
     */
    private WarehouseStatus parseStatusOrDefault(String status) {
        if (status == null || status.isBlank()) {
            return WarehouseStatus.HOAT_DONG;
        }
        try {
            return WarehouseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ.");
        }
    }

    /**
     * Phân tích trạng thái lọc của kho hàng từ chuỗi sang Enum tương ứng.
     */
    private WarehouseStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return WarehouseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ.");
        }
    }

    /**
     * Chuẩn hóa từ khóa tìm kiếm tương đối SQL (LIKE '%keyword%').
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    /**
     * Xây dựng Specification truy vấn động với Criteria API của JPA.
     */
    private Specification<Warehouse> buildSpecification(String keywordLike, WarehouseStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keywordLike != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), keywordLike)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
