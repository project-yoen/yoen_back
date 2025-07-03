package com.yoen.yoen_back.dto;

import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Role;

public record RegisterRequestDto (String username, String password, String name, String email,
                                  String nickname, Role role, Gender gender, String birthday,
                                  String profileImageUrl) {
}
