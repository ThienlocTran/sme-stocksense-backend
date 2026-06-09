package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.WarehouseResponse;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.WarehouseService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai các phương thức nghiệp vụ của WarehouseService.
 */
@Service // Đăng ký lớp này thành một Service Spring Bean xử lý logic nghiệp vụ
@RequiredArgsConstructor // Tự sinh constructor để tự động inject các thuộc tính có từ khóa final
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository; // Inject interface Repository kết nối CSDL

    @Override
    @Transactional(readOnly = true) // Chế độ chỉ đọc giúp tối ưu bộ nhớ đệm và cải thiện tốc độ truy vấn
    public List<WarehouseResponse> getWarehouses(String keyword, String status) {
        // Phân tích trạng thái dạng chuỗi truyền từ controller về enum, ném lỗi nếu nhập sai
        WarehouseStatus parsedStatus = parseStatus(status);
        
        // Chuẩn hóa từ khóa tìm kiếm để dùng trong câu truy vấn so khớp mẫu (LIKE)
        String keywordLike = normalizeKeyword(keyword);

        // Xây dựng điều kiện truy vấn SQL động dựa trên từ khóa tìm kiếm và trạng thái kho
        Specification<Warehouse> specification = buildSpecification(keywordLike, parsedStatus);

        // Lấy tất cả kho thỏa mãn điều kiện, chuyển sang dạng DTO Response và đóng gói vào danh sách
        return warehouseRepository.findAll(specification).stream()
                .map(WarehouseResponse::from)
                .toList();
    }

    /**
     * Phân tích trạng thái hoạt động của kho hàng từ chuỗi sang kiểu Enum thích hợp.
     * Ném ra ngoại lệ BadRequestException (mã lỗi HTTP 400) nếu giá trị không hợp lệ.
     */
    private WarehouseStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null; // Bỏ qua không lọc trạng thái nếu tham số rỗng
        }
        try {
            // Loại bỏ khoảng trắng thừa và chuyển thành chữ viết hoa để so khớp với Enum (VD: "active" -> ACTIVE)
            return WarehouseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Ném lỗi 400 báo trạng thái gửi lên không đúng định dạng ACTIVE hoặc INACTIVE
            throw new BadRequestException("status không hợp lệ.");
        }
    }

    /**
     * Chuẩn hóa từ khóa sang dạng tìm kiếm tương đối của SQL (LIKE '%keyword%')
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null; // Trả về null nếu không nhập từ khóa tìm kiếm
        }
        // Chuyển toàn bộ ký tự sang dạng chữ thường (lowercase) và thêm ký tự wildcard
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    /**
     * Xây dựng câu truy vấn SQL động (WHERE clause) với Criteria API của JPA.
     */
    private Specification<Warehouse> buildSpecification(String keywordLike, WarehouseStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>(); // Mảng chứa các điều kiện lọc

            // Nếu có từ khóa tìm kiếm, tạo câu lệnh điều kiện kết hợp OR trên các cột 'name', 'code', 'address'
            if (keywordLike != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordLike),     // LIKE theo tên kho
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), keywordLike),     // LIKE theo mã kho
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), keywordLike)   // LIKE theo địa chỉ
                ));
            }

            // Nếu lọc theo trạng thái hoạt động cụ thể, tạo điều kiện so khớp chính xác bằng '='
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // Kết hợp toàn bộ điều kiện trong mảng bằng phép toán logic AND.
            // Nếu danh sách predicates trống, trả về câu truy vấn mặc định (luôn luôn đúng).
            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
