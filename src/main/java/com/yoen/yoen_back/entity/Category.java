package com.yoen.yoen_back.entity;

import com.yoen.yoen_back.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** 카테고리 엔티티
 */
@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "categories")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(nullable = false)
    private String categoryName;

    @Column(nullable = false)
    private String type;

}
