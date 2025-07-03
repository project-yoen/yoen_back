package com.yoen.yoen_back.repository.jpa;

import com.yoen.yoen_back.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
