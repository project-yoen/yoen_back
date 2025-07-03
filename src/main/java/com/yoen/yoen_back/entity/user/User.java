package com.yoen.yoen_back.entity.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yoen.yoen_back.common.entity.BaseEntity;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/** 유저 엔티티
 * 유저정보를 관리
 */
@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String name;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;
    @Column(nullable = false)
    private Gender gender;
    private LocalDate birthday;

    private String profileImageUrl;
}
