package com.smartflow.smestocksensebackend.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Base Specification: tập hợp các helper build {@link Predicate} động bằng CriteriaBuilder.
 *
 * Mọi entity Specification (Product, Warehouse, Partner) kế thừa lớp này để tái sử dụng
 * logic lọc chung, tránh lặp code và giữ if-else ra khỏi Controller / Service.
 *
 * Quy ước: helper trả về {@code null} khi giá trị lọc rỗng — phần tử null sẽ bị
 * {@link #combineAnd} bỏ qua, nhờ đó query chỉ chứa đúng điều kiện cần thiết.
 *
 * @param <T> kiểu entity
 */
public abstract class BaseSpecification<T> {

    /**
     * LIKE không phân biệt hoa thường trên 1 cột.
     * @return null nếu value rỗng (không thêm điều kiện)
     */
    protected Predicate likeIgnoreCase(CriteriaBuilder cb, Path<String> field, String value) {
        if (isBlank(value)) {
            return null;
        }
        return cb.like(cb.lower(field), "%" + value.trim().toLowerCase() + "%");
    }

    /**
     * LIKE không phân biệt hoa thường trên nhiều cột, nối bằng OR.
     * Dùng cho ô search gõ 1 từ khoá nhưng quét nhiều trường (code / name / sku).
     * @return null nếu value rỗng
     */
    @SafeVarargs
    protected final Predicate keywordOnAnyField(CriteriaBuilder cb, String value, Path<String>... fields) {
        if (isBlank(value) || fields.length == 0) {
            return null;
        }
        String pattern = "%" + value.trim().toLowerCase() + "%";
        List<Predicate> ors = new ArrayList<>();
        for (Path<String> field : fields) {
            ors.add(cb.like(cb.lower(field), pattern));
        }
        return cb.or(ors.toArray(Predicate[]::new));
    }

    /**
     * So sánh bằng. @return null nếu value là null (không thêm điều kiện).
     */
    protected Predicate equal(CriteriaBuilder cb, Expression<?> field, Object value) {
        if (value == null) {
            return null;
        }
        return cb.equal(field, value);
    }

    /**
     * field &gt;= value. @return null nếu value là null (không thêm điều kiện).
     */
    protected <Y extends Comparable<? super Y>> Predicate greaterThanOrEqual(
            CriteriaBuilder cb, Expression<Y> field, Y value) {
        if (value == null) {
            return null;
        }
        return cb.greaterThanOrEqualTo(field, value);
    }

    /**
     * field &lt;= value. @return null nếu value là null (không thêm điều kiện).
     */
    protected <Y extends Comparable<? super Y>> Predicate lessThanOrEqual(
            CriteriaBuilder cb, Expression<Y> field, Y value) {
        if (value == null) {
            return null;
        }
        return cb.lessThanOrEqualTo(field, value);
    }

    /**
     * Gộp danh sách predicate bằng AND, tự loại bỏ phần tử null.
     * @return conjunction (luôn true) nếu không có điều kiện nào.
     */
    protected Predicate combineAnd(CriteriaBuilder cb, Root<T> root, List<Predicate> predicates) {
        List<Predicate> valid = new ArrayList<>();
        for (Predicate p : predicates) {
            if (p != null) {
                valid.add(p);
            }
        }
        return valid.isEmpty()
                ? cb.conjunction()
                : cb.and(valid.toArray(Predicate[]::new));
    }

    /**
     * Mỗi entity Specification override method này để khai báo điều kiện riêng,
     * tận dụng các helper ở trên. Trả về một {@link Specification} dùng trực tiếp
     * với JpaSpecificationExecutor.
     */
    public abstract Specification<T> build();

    protected boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
