package com.yoen.yoen_back.common.security;


import com.yoen.yoen_back.common.infrastructure.JwtProvider;
import com.yoen.yoen_back.dao.redis.UserCacheRedisDao;
import com.yoen.yoen_back.dto.user.CachedUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserService userService;
    private final UserCacheRedisDao userCacheRedisDao;

    public JwtAuthenticationFilter(JwtProvider jwtProvider, UserService userService, UserCacheRedisDao userCacheRedisDao) {
        this.jwtProvider = jwtProvider;
        this.userService = userService;
        this.userCacheRedisDao = userCacheRedisDao;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null && jwtProvider.validateToken(token)) {
                Long userId = Long.parseLong(jwtProvider.getUserIdFromToken(token));
                // 캐시 히트 시 DB 조회 생략, 미스 시 DB 조회 후 캐시 적재 (요청당 유저 DB 조회 제거)
                User user = userCacheRedisDao.find(userId)
                        .map(CachedUser::toUser)
                        .orElseGet(() -> {
                            User dbUser = userService.findById(userId);
                            if (dbUser != null) {
                                userCacheRedisDao.save(CachedUser.from(dbUser));
                            }
                            return dbUser;
                        });

                // CustomUserDetails로 wrapping
                CustomUserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 토큰값이 있는데 유저가 없는경우에 대한 에러처리인데 (걍 로그인, 회원가입시 토큰값이있을때 에러방지)
            log.warn("event=authentication_rejected method={} path={} reason={}",
                    request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        return (bearer != null && bearer.startsWith("Bearer ")) ? bearer.substring(7) : null;
    }
}
