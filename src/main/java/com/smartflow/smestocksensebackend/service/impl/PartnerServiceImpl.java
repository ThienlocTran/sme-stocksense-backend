package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.request.CreatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.request.UpdatePartnerRequest;
import com.smartflow.smestocksensebackend.dto.response.PartnerDropdownResponse;
import com.smartflow.smestocksensebackend.dto.response.PartnerResponse;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.service.PartnerService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     * Nghiệp vụ thêm mới đối tác vào hệ thống:
     * - Tự động phát sinh mã đối tác duy nhất nếu Client không truyền lên.
     * - Chuẩn hóa mã đối tác (loại bỏ khoảng trắng, chuyển chữ in hoa).
     * - Kiểm tra trùng lặp mã đối tác.
     * - Thiết lập trạng thái mặc định là HOAT_DONG nếu không truyền.
     */
    @Override
    @Transactional
    public PartnerResponse createPartner(CreatePartnerRequest request) {
        String code;
        if (request.maDoiTac() == null || request.maDoiTac().isBlank()) {
            // Tự động sinh mã đối tác duy nhất để giữ tính định danh nghiệp vụ
            code = "DT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } else {
            code = request.maDoiTac().trim().toUpperCase();
            if (partnerRepository.existsByCodeIgnoreCase(code)) {
                throw new FieldValidationException(Map.of("maDoiTac", "Mã đối tác đã tồn tại."));
            }
        }

        Partner partner = new Partner();
        partner.setCode(code);
        partner.setName(request.tenDoiTac().trim());
        PartnerType type = parseType(request.loaiDoiTac());
        if (type == null) {
            throw new BadRequestException("Loại đối tác không được để trống.");
        }
        partner.setType(type);
        partner.setContactPerson(normalizeOptional(request.nguoiLienHe()));
        partner.setPhoneNumber(normalizeOptional(request.soDienThoai()));
        partner.setEmail(normalizeOptional(request.email()));
        partner.setAddress(normalizeOptional(request.diaChi()));
        partner.setStatus(parseStatusOrDefault(request.trangThai()));

        Partner savedPartner = partnerRepository.saveAndFlush(partner);
        return PartnerResponse.from(savedPartner);
    }

    /**
     * Nghiệp vụ cập nhật thông tin đối tác:
     * - Không cho phép thay đổi mã đối tác nhằm giữ tính nhất quán dữ liệu.
     */
    @Override
    @Transactional
    public PartnerResponse updatePartner(Long id, UpdatePartnerRequest request) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Đối tác không tồn tại."));

        partner.setName(request.tenDoiTac().trim());
        // loaiDoiTac bị giới hạn trong NHA_CUNG_CAP, KHACH_HANG hoặc CA_HAI để tránh dữ liệu sai nghiệp vụ
        PartnerType type = parseType(request.loaiDoiTac());
        if (type == null) {
            throw new BadRequestException("Loại đối tác không được để trống.");
        }
        partner.setType(type);
        partner.setContactPerson(normalizeOptional(request.nguoiLienHe()));
        partner.setPhoneNumber(normalizeOptional(request.soDienThoai()));
        partner.setEmail(normalizeOptional(request.email()));
        partner.setAddress(normalizeOptional(request.diaChi()));
        
        PartnerStatus parsedStatus = parseStatus(request.trangThai());
        if (parsedStatus == null) {
            throw new BadRequestException("Trạng thái không được để trống.");
        }
        partner.setStatus(parsedStatus);

        Partner savedPartner = partnerRepository.saveAndFlush(partner);
        return PartnerResponse.from(savedPartner);
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
    private PartnerStatus parseStatusOrDefault(String status) {
        if (status == null || status.isBlank()) {
            return PartnerStatus.HOAT_DONG;
        }
        try {
            return PartnerStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ.");
        }
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

    @Override
    @Transactional(readOnly = true)
    public List<PartnerDropdownResponse> getActiveSuppliers() {
        return partnerRepository
                .findByTypeInAndStatusOrderByNameAsc(
                        List.of(PartnerType.NHA_CUNG_CAP, PartnerType.CA_HAI), PartnerStatus.HOAT_DONG)
                .stream()
                .map(PartnerDropdownResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartnerDropdownResponse> getActiveCustomers() {
        return partnerRepository
                .findByTypeInAndStatusOrderByNameAsc(
                        List.of(PartnerType.KHACH_HANG, PartnerType.CA_HAI), PartnerStatus.HOAT_DONG)
                .stream()
                .map(PartnerDropdownResponse::from)
                .toList();
    }
}
