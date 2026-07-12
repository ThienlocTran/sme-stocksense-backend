package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventorycount.*;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.*;
import com.smartflow.smestocksensebackend.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryCountServiceImplTest {
    @Mock InventoryCountRepository countRepository; @Mock InventoryCountDetailRepository detailRepository;
    @Mock InventoryLevelRepository inventoryRepository; @Mock WarehouseRepository warehouseRepository; @Mock ProductRepository productRepository;
    @InjectMocks InventoryCountServiceImpl service;
    Employee actor; Warehouse warehouse; Product product; InventoryCount count; InventoryCountDetail detail;

    @BeforeEach void setup(){
        actor=new Employee(); actor.setId(1L); actor.setFullName("Toan");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
        warehouse=new Warehouse(); warehouse.setId(2L); warehouse.setName("Kho A"); warehouse.setStatus(WarehouseStatus.HOAT_DONG);
        product=new Product(); product.setId(3L); product.setCode("SP3"); product.setName("San pham"); product.setStatus(ProductStatus.HOAT_DONG);
        count=new InventoryCount(); count.setId(4L); count.setCode("KK-1"); count.setWarehouse(warehouse); count.setCreatedBy(actor); count.setStatus(InventoryCountStatus.DANG_KIEM_KE); count.setVersion(0L);
        detail=new InventoryCountDetail(); detail.setId(5L); detail.setInventoryCount(count); detail.setProduct(product); detail.setSystemQuantity(10); detail.setVersion(0L);
    }
    @AfterEach void clear(){ SecurityContextHolder.clearContext(); }

    @Test void create_shouldSnapshotCurrentInventoryWithoutMutatingIt(){
        InventoryLevel stock=new InventoryLevel(); stock.setWarehouse(warehouse); stock.setProduct(product); stock.setQuantity(10);
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse)); when(inventoryRepository.findByWarehouseId(2L)).thenReturn(List.of(stock));
        when(countRepository.saveAndFlush(any())).thenAnswer(i->{InventoryCount c=i.getArgument(0);c.setId(4L);c.setVersion(0L);return c;});
        when(detailRepository.saveAllAndFlush(anyList())).thenAnswer(i->i.getArgument(0));
        InventoryCountResponse response=service.create(new InventoryCountRequests.Create(2L,null,"Kiem ke"));
        assertEquals("DANG_KIEM_KE",response.status()); assertEquals(10,response.details().getFirst().systemQuantity());
        verify(inventoryRepository,never()).save(any());
    }

    @Test void recordActual_shouldCalculateDifference(){
        when(countRepository.findById(4L)).thenReturn(Optional.of(count)); when(detailRepository.findById(5L)).thenReturn(Optional.of(detail));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(4L)).thenReturn(List.of(detail));
        service.recordActual(4L,5L,new InventoryCountRequests.RecordActual(7,"Thieu",0L));
        assertEquals(-3,detail.getDifferenceQuantity()); assertEquals(7,detail.getActualQuantity());
    }

    @Test void finalize_shouldRejectMissingActualQuantity(){
        when(countRepository.findById(4L)).thenReturn(Optional.of(count)); when(detailRepository.findByInventoryCountIdOrderByIdAsc(4L)).thenReturn(List.of(detail));
        assertThrows(ConflictException.class,()->service.finalizeCount(4L,new InventoryCountRequests.Finalize(0L)));
        verify(countRepository,never()).saveAndFlush(count);
    }

    @Test void finalize_shouldCloseWithoutChangingInventory(){
        detail.setActualQuantity(10); detail.setDifferenceQuantity(0);
        when(countRepository.findById(4L)).thenReturn(Optional.of(count)); when(detailRepository.findByInventoryCountIdOrderByIdAsc(4L)).thenReturn(List.of(detail)); when(countRepository.saveAndFlush(count)).thenReturn(count);
        InventoryCountResponse response=service.finalizeCount(4L,new InventoryCountRequests.Finalize(0L));
        assertEquals("DA_CHOT",response.status()); verify(inventoryRepository,never()).save(any());
    }

    @Test void cancelledCount_shouldNotAllowFurtherEditing(){
        count.setStatus(InventoryCountStatus.DA_HUY); when(countRepository.findById(4L)).thenReturn(Optional.of(count));
        assertThrows(ConflictException.class,()->service.recordActual(4L,5L,new InventoryCountRequests.RecordActual(1,null,0L)));
    }
}
