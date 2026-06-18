package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    @Query("select count(category) > 0 from Category category where lower(trim(category.code)) = lower(trim(:code))")
    boolean existsByNormalizedCode(@Param("code") String code);

    @Query("select count(category) > 0 from Category category where lower(trim(category.name)) = lower(trim(:name))")
    boolean existsByNormalizedName(@Param("name") String name);

    @Query("""
            select count(category) > 0
            from Category category
            where category.id <> :id
              and lower(trim(category.code)) = lower(trim(:code))
            """)
    boolean existsByNormalizedCodeAndIdNot(@Param("code") String code, @Param("id") Long id);

    @Query("""
            select count(category) > 0
            from Category category
            where category.id <> :id
              and lower(trim(category.name)) = lower(trim(:name))
            """)
    boolean existsByNormalizedNameAndIdNot(@Param("name") String name, @Param("id") Long id);

    List<Category> findByStatusOrderByNameAsc(CategoryStatus status);
}
