package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.service.PartnerService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp triển khai các phương thức nghiệp vụ của PartnerService.
 */
@Service
@RequiredArgsConstructor
public class PartnerServiceImpl implements PartnerService {

    private final PartnerRepository partnerRepository;

    /**
     * Lấy danh sách đối tác có hỗ trợ tìm kiếm động và lọc theo loại đối tác, trạng thái.
     * API này chỉ đọc danh sách đối tác, không thay đổi dữ liệu.
     */
    @Override
    @Transactional(readOnly = true)
    public List<PartnerResponse> getPartners(String keyword, String loaiDoiTac, String trangThai) {
        PartnerType type = parseType(loaiDoiTac);
        PartnerStatus status = parseStatus(trangThai);
        String keywordLike = normalizeKeyword(keyword);

        Specification<Partner> specification = buildSpecification(keywordLike, type, status);

        return partnerRepository.findAll(specification).stream()
                .map(PartnerResponse::from)
                .toList();
    }

    /**
     * Phân tích loại đối tác từ chuỗi sang Enum PartnerType tương ứng.
     */
    private PartnerType parseType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        try {
            return PartnerType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Loại đối tác không hợp lệ. Chỉ nhận NHA_CUNG_CAP, KHACH_HANG hoặc CA_HAI.");
        }
    }

    /**
     * Phân tích trạng thái hoạt động từ chuỗi sang Enum PartnerStatus tương ứng.
     */
    private PartnerStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PartnerStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái không hợp lệ. Chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.");
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
    private Specification<Partner> buildSpecification(String keywordLike, PartnerType type, PartnerStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keywordLike != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("contactPerson")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), keywordLike)
                ));
            }

            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
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
