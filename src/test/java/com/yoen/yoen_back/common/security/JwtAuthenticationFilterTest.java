package com.yoen.yoen_back.common.security;

import com.yoen.yoen_back.common.infrastructure.JwtProvider;
import com.yoen.yoen_back.dao.redis.UserCacheRedisDao;
import com.yoen.yoen_back.service.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserService userService;

    @Mock
    private UserCacheRedisDao userCacheRedisDao;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveUserWithValidJwt_isNotAuthenticated() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtProvider,
                userService,
                userCacheRedisDao
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserIdFromToken("valid-token")).thenReturn("1");
        when(userCacheRedisDao.find(1L)).thenReturn(Optional.empty());
        when(userService.findById(1L)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userCacheRedisDao, never()).save(org.mockito.ArgumentMatchers.any());
        verify(filterChain).doFilter(request, response);
    }
}
