package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByType(PaymentType type);
}
