package com.kivo.backend.catalog.persistence.repository;

import com.kivo.backend.catalog.persistence.entity.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

  @Query(
      """
      select distinct category
      from Category category
      join fetch category.mall mall
      left join fetch category.translations translation
      where mall.code = :mallCode
        and mall.active = true
        and category.active = true
      order by category.displayOrder, category.code
      """)
  List<Category> findActiveByMallCode(@Param("mallCode") String mallCode);
}
