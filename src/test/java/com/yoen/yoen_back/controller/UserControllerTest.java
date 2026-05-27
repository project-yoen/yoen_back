package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.common.security.JwtAuthenticationFilter;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.LoginResponseDto;
import com.yoen.yoen_back.dto.user.RegisterRequestDto;
import com.yoen.yoen_back.dto.user.UpdateUserDto;
import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
@AutoConfigureMockMvc
class UserControllerTest {

    // UserController의 요청 매핑과 응답 형태만 검증하고 서비스 로직은 mock으로 고정한다.
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @TestConfiguration
    static class TestSecurityConfig {

        // 테스트에서는 JWT 검증을 제외하고 @AuthenticationPrincipal 주입만 사용할 수 있게 둔다.
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void signUp_validRequest_callsUserServiceAndReturnsSuccess() throws Exception {
        // 회원가입 요청 body가 UserService.register로 전달되는지 검증한다.
        RegisterRequestDto request = new RegisterRequestDto(
                "password",
                "Alice",
                "alice@example.com",
                "alice",
                Gender.FEMALE,
                "2000-01-01",
                "https://image.example/profile.jpg"
        );

        mockMvc.perform(post("/user/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Sign Up Success"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(userService).register(request);
    }

    @Test
    void login_validRequest_returnsLoginResponse() throws Exception {
        // 로그인 요청이 AuthService로 전달되고 토큰 응답이 JSON으로 내려오는지 검증한다.
        LoginRequestDto request = new LoginRequestDto("alice@example.com", "password");
        UserResponseDto userResponse = userResponse();
        LoginResponseDto response = new LoginResponseDto(userResponse, "access-token", "refresh-token");
        when(authService.loginAndGetToken(request)).thenReturn(response);

        mockMvc.perform(post("/user/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.userId").value(1L))
                .andExpect(jsonPath("$.data.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(authService).loginAndGetToken(request);
    }

    @Test
    void existEmail_validEmail_returnsBoolean() throws Exception {
        // 이메일 중복 확인 요청 파라미터가 UserService로 전달되는지 검증한다.
        when(userService.validateEmail("alice@example.com")).thenReturn(true);

        mockMvc.perform(get("/user/exists")
                        .param("email", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(userService).validateEmail("alice@example.com");
    }

    @Test
    void profile_authenticatedUser_returnsUserResponse() throws Exception {
        // 인증 principal의 userId로 프로필을 조회하는지 검증한다.
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        when(userService.findUserResponseById(1L)).thenReturn(userResponse());

        mockMvc.perform(get("/user/profile")
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("alice"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(userService).findUserResponseById(1L);
    }

    @Test
    void updateUser_authenticatedUser_returnsUpdatedUserResponse() throws Exception {
        // 인증된 사용자와 수정 DTO가 UserService.updateUser로 전달되는지 검증한다.
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        UpdateUserDto request = new UpdateUserDto(1L, "Alice Updated", Gender.FEMALE, "alice-updated", "2000-01-02");
        UserResponseDto response = new UserResponseDto(1L, "Alice Updated", "alice@example.com", Gender.FEMALE, "alice-updated", LocalDate.of(2000, 1, 2), "");
        when(userService.updateUser(eq(userDetails.user()), eq(request))).thenReturn(response);

        mockMvc.perform(post("/user/update")
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Alice Updated"))
                .andExpect(jsonPath("$.data.nickname").value("alice-updated"))
                .andExpect(jsonPath("$.data.birthday").value("2000-01-02"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(userService).updateUser(userDetails.user(), request);
    }

    @Test
    void setProfileImage_authenticatedUserAndMultipartFile_returnsImageUrl() throws Exception {
        // multipart profileImage가 프로필 이미지 저장 서비스로 전달되는지 검증한다.
        CustomUserDetails userDetails = new CustomUserDetails(userEntity());
        MockMultipartFile profileImage = new MockMultipartFile("profileImage", "profile.jpg", "image/jpeg", "image".getBytes());
        when(userService.saveProfileUrl(eq(userDetails.user()), any())).thenReturn("https://image.example/profile.jpg");

        mockMvc.perform(multipart("/user/profileImage")
                        .file(profileImage)
                        .with(csrf())
                        .with(authentication(authenticationToken(userDetails))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("https://image.example/profile.jpg"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(userService).saveProfileUrl(eq(userDetails.user()), any());
    }

    private User userEntity() {
        // @AuthenticationPrincipal 테스트에 사용할 최소 User fixture.
        return User.builder()
                .userId(1L)
                .email("alice@example.com")
                .password("password")
                .name("Alice")
                .nickname("alice")
                .gender(Gender.FEMALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private UsernamePasswordAuthenticationToken authenticationToken(CustomUserDetails userDetails) {
        // MockMvc 요청에 CustomUserDetails principal을 넣기 위한 인증 fixture.
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    private UserResponseDto userResponse() {
        // UserController 응답 검증에 재사용하는 사용자 DTO fixture.
        return new UserResponseDto(
                1L,
                "Alice",
                "alice@example.com",
                Gender.FEMALE,
                "alice",
                LocalDate.of(2000, 1, 1),
                "https://image.example/profile.jpg"
        );
    }
}
