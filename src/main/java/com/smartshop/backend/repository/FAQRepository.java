package com.smartshop.backend.repository;

import com.smartshop.backend.entity.FAQ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FAQRepository extends JpaRepository<FAQ, Long> {
    List<FAQ> findByStatusOrderBySortOrderAscCreatedAtAsc(String status);
    List<FAQ> findByCategoryAndStatusOrderBySortOrderAsc(String category, String status);
    List<FAQ> findByCategoryOrderBySortOrderAsc(String category);
}
