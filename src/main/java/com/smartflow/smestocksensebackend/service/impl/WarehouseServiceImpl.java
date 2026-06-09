package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.request.CreateWarehouseRequest;
import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
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
     * - Kiểm tra trùng mã kho hàng trong hệ thống (không phân biệt hoa/thường).
     * - Thiết lập trạng thái mặc định là ACTIVE nếu không truyền.
     */
    @Override
    @Transactional
    public WarehouseResponse createWarehouse(CreateWarehouseRequest request) {
        // Chuẩn hóa mã kho
        String code = request.code().trim().toUpperCase();

        // Kiểm tra trùng mã kho hàng trong hệ thống
        if (warehouseRepository.existsByCodeIgnoreCase(code)) {
            throw new FieldValidationException(Map.of("code", "Mã kho đã tồn tại."));
        }

        // Tạo mới thực thể Warehouse và thiết lập thông tin
        Warehouse warehouse = new Warehouse();
        warehouse.setCode(code);
        warehouse.setName(request.name().trim());
        warehouse.setAddress(normalizeOptional(request.address()));
        warehouse.setStatus(parseStatusOrDefault(request.status()));

        // Lưu vào CSDL và chuyển đổi sang DTO phản hồi
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
     * Phân tích chuỗi trạng thái hoặc trả về giá trị mặc định là ACTIVE nếu không truyền trạng thái.
     * Ném ra ngoại lệ BadRequestException nếu giá trị không hợp lệ.
     */
    private WarehouseStatus parseStatusOrDefault(String status) {
        if (status == null || status.isBlank()) {
            return WarehouseStatus.ACTIVE;
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
