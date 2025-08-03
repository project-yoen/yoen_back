package com.yoen.yoen_back.dto.user;

import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Role;

public record RegisterRequestDto (String password, String name, String email,
                                  String nickname, Gender gender, String birthday,
                                  String profileImageUrl) {

}
