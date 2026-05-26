package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.etc.joincode.AcceptJoinRequestDto;
import com.yoen.yoen_back.dto.etc.joincode.JoinRequestListResponseDto;
import com.yoen.yoen_back.dto.etc.joincode.UserTravelJoinResponseDto;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.JoinService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class JoinControllerTest {

    @Mock
    private JoinService joinService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new JoinController(joinService, authService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        user = user(1L);
        userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 참여 코드와 만료 시간을 조합해 응답 DTO로 반환하는지 확인한다.
    @Test
    void getCode_validTravelId_returnsJoinCodeResponse() throws Exception {
        LocalDateTime expiredAt = LocalDateTime.of(2026, 6, 1, 12, 0);
        when(joinService.getJoinCode(10L)).thenReturn("ABC123");
        when(joinService.getCodeExpiredTime("ABC123")).thenReturn(expiredAt);

        mockMvc.perform(get("/join/code")
                        .param("travelId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("ABC123"));

        verify(joinService).getJoinCode(10L);
        verify(joinService).getCodeExpiredTime("ABC123");
    }

    // 인증된 사용자의 참여 신청 목록 조회를 서비스에 위임하는지 확인한다.
    @Test
    void getUserTravelJoinList_authenticatedUser_returnsJoinRequests() throws Exception {
        when(joinService.getUserTravelJoinRequests(user))
                .thenReturn(List.of(new UserTravelJoinResponseDto(1L, 10L, "도쿄 여행", Nation.JAPAN, List.of())));

        mockMvc.perform(get("/join/userlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].travelJoinId").value(1))
                .andExpect(jsonPath("$.data[0].travelName").value("도쿄 여행"));

        verify(joinService).getUserTravelJoinRequests(user);
    }

    // 참여 신청 삭제 요청이 경로 변수 id로 서비스에 전달되는지 확인한다.
    @Test
    void deleteUserTravelJoinRequest_validId_returnsSuccessMessage() throws Exception {
        mockMvc.perform(delete("/join/delete/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("제거되었습니다."));

        verify(joinService).deleteUserTravelJoinRequest(5L);
    }

    // 참여 코드로 여행 참여 신청을 생성하도록 서비스에 위임하는지 확인한다.
    @Test
    void setTravelJoinRequest_authenticatedUser_returnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/join/ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("가입 신청이 완료되었습니다."));

        verify(joinService).requestToJoinTravel(user, "ABC123");
    }

    // 여행 참여 신청 목록 조회 전에 reader/writer 권한 체크를 수행하는지 확인한다.
    @Test
    void getTravelJoinRequest_validTravelId_checksRoleAndReturnsRequests() throws Exception {
        when(joinService.getJoinRequestList(10L))
                .thenReturn(List.of(new JoinRequestListResponseDto(7L, Gender.MALE, "지민", "https://image.example/profile.png")));

        mockMvc.perform(get("/join/travellist")
                        .param("travelId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].travelJoinRequestId").value(7))
                .andExpect(jsonPath("$.data[0].name").value("지민"));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER, Role.READER));
        verify(joinService).getJoinRequestList(10L);
    }

    // 참여 신청 승인 전에 writer 권한 체크를 수행하고 승인 서비스를 호출하는지 확인한다.
    @Test
    void acceptJoinRequest_validRequest_checksRoleAndAccepts() throws Exception {
        AcceptJoinRequestDto request = new AcceptJoinRequestDto(7L, Role.READER);

        mockMvc.perform(post("/join/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Join request accepted"));

        verify(authService).checkTravelUserRoleByTravelJoinRequest(user, 7L, List.of(Role.WRITER));
        verify(joinService).acceptJoinRequest(request);
    }

    // 참여 신청 거절 전에 writer 권한 체크를 수행하고 거절 서비스를 호출하는지 확인한다.
    @Test
    void rejectJoinRequest_validId_checksRoleAndRejects() throws Exception {
        mockMvc.perform(post("/join/reject/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Join request rejected"));

        verify(authService).checkTravelUserRoleByTravelJoinRequest(user, 7L, List.of(Role.WRITER));
        verify(joinService).rejectJoinRequest(7L);
    }

    private User user(Long userId) {
        return User.builder()
                .userId(userId)
                .email("user" + userId + "@example.com")
                .password("password")
                .name("User " + userId)
                .nickname("user" + userId)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }
}
