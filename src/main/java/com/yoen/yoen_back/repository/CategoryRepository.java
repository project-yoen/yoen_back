package com.yoen.yoen_back.repository;

import com.yoen.yoen_back.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
