package com.yoen.yoen_back.dto.user;

import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;

import java.time.LocalDate;

/**
 * JWT 인증 필터용 유저 캐시 스냅샷.
 * 인증 컨텍스트에서 읽기 용도로만 쓰이므로 비밀번호/프로필이미지는 담지 않는다.
 * 이 스냅샷으로 복원한 User는 detached 상태이므로 절대 save() 하면 안 된다.
 */
public record CachedUser(Long userId, String email, String name, String nickname, Gender gender, LocalDate birthday) {

    public static CachedUser from(User user) {
        return new CachedUser(user.getUserId(), user.getEmail(), user.getName(),
                user.getNickname(), user.getGender(), user.getBirthday());
    }

    public User toUser() {
        return User.builder()
                .userId(userId)
                .email(email)
                .name(name)
                .nickname(nickname)
                .gender(gender)
                .birthday(birthday)
                .build();
    }
}
