package com.yoen.yoen_back.common.security;

import com.yoen.yoen_back.entity.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record CustomUserDetails(User user) implements UserDetails {

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    // 사용자의 권한은 불필요하고 해당 여행에서의 유저 권한이 중요
    // 따라서 controller에서 분기 문으로 사용자 아이디와 여행 id를 받아서(또는 TravelUserId) 직접 권한인증함수 만들기
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(); // 비워두기
    }

    // 생략 가능한 override들
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
