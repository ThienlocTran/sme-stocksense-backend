package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    @Query("select count(category) > 0 from Category category where lower(trim(category.code)) = lower(trim(:code))")
    boolean existsByNormalizedCode(@Param("code") String code);

    @Query("select count(category) > 0 from Category category where lower(trim(category.name)) = lower(trim(:name))")
    boolean existsByNormalizedName(@Param("name") String name);
}
