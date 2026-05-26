package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.record.TravelRecordRequestDto;
import com.yoen.yoen_back.dto.record.TravelRecordResponseDto;
import com.yoen.yoen_back.dto.record.TravelRecordUpdateDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.RecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecordControllerTest {

    @Mock
    private RecordService recordService;

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User user;
    private Travel travel;
    private TravelUser travelUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecordController(recordService, authService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
        user = user(1L);
        travel = travel(10L);
        travelUser = travelUser(100L, travel, user, Role.WRITER);
        userDetails = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 전체 기록 조회 전에 reader/writer 권한 체크를 수행하고 Travel 기준으로 조회하는지 확인한다.
    @Test
    void travelRecord_validTravelId_checksRoleAndReturnsRecords() throws Exception {
        when(authService.checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(recordService.getAllTravelRecordsByTravel(travel)).thenReturn(List.of());

        mockMvc.perform(get("/record/all")
                        .param("travelId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER));
        verify(recordService).getAllTravelRecordsByTravel(travel);
    }

    // multipart 기록 생성 요청의 dto part를 읽고 writer 권한 체크 후 생성 서비스에 넘기는지 확인한다.
    @Test
    void setTravelRecord_validMultipartRequest_checksRoleAndCreatesRecord() throws Exception {
        TravelRecordRequestDto request = new TravelRecordRequestDto(null, 10L, "제목", "내용", "2026-06-01T10:30:00");
        TravelRecordResponseDto response = new TravelRecordResponseDto(
                20L,
                "지민",
                "제목",
                "내용",
                LocalDateTime.of(2026, 6, 1, 10, 30),
                List.of()
        );
        when(recordService.createTravelRecord(eq(user), eq(request), eq(null))).thenReturn(response);

        mockMvc.perform(multipart("/record/create")
                        .file(jsonPart("dto", request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.travelRecordId").value(20))
                .andExpect(jsonPath("$.data.title").value("제목"));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER));
        verify(recordService).createTravelRecord(user, request, null);
    }

    // multipart 기록 수정 요청의 dto part를 읽고 writer 권한 체크 후 수정 서비스에 넘기는지 확인한다.
    @Test
    void updateTravelRecord_validMultipartRequest_checksRoleAndUpdatesRecord() throws Exception {
        TravelRecordUpdateDto request = new TravelRecordUpdateDto(20L, 10L, "수정 제목", "수정 내용", "2026-06-02T11:30:00", List.of());
        TravelRecordResponseDto response = new TravelRecordResponseDto(
                20L,
                "지민",
                "수정 제목",
                "수정 내용",
                LocalDateTime.of(2026, 6, 2, 11, 30),
                List.of()
        );
        when(recordService.updateTravelRecord(eq(user), eq(request), eq(null))).thenReturn(response);

        mockMvc.perform(multipart("/record/update")
                        .file(jsonPart("dto", request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.travelRecordId").value(20))
                .andExpect(jsonPath("$.data.title").value("수정 제목"));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER));
        verify(recordService).updateTravelRecord(user, request, null);
    }

    // 날짜별 기록 조회 전에 reader/writer 권한 체크를 수행하고 Travel과 date를 서비스에 전달하는지 확인한다.
    @Test
    void getTravelRecordByDate_validRequest_checksRoleAndReturnsRecords() throws Exception {
        when(authService.checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(recordService.getTravelRecordsByDate(travel, "2026-06-01T10:30:00")).thenReturn(List.of());

        mockMvc.perform(get("/record")
                        .param("travelId", "10")
                        .param("date", "2026-06-01T10:30:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER));
        verify(recordService).getTravelRecordsByDate(travel, "2026-06-01T10:30:00");
    }

    // 기록 삭제 전에 writer 권한 체크를 수행하고 삭제 서비스를 호출하는지 확인한다.
    @Test
    void deleteTravelRecord_validId_checksRoleAndDeletesRecord() throws Exception {
        mockMvc.perform(delete("/record/delete")
                        .param("id", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("성공적으로 삭제되었습니다."));

        verify(authService).checkTravelUserRoleByRecord(user, 20L, List.of(Role.WRITER));
        verify(recordService).deleteTravelRecord(20L);
    }

    private MockMultipartFile jsonPart(String name, Object value) throws Exception {
        return new MockMultipartFile(name, "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(value));
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

    private Travel travel(Long travelId) {
        return Travel.builder()
                .travelId(travelId)
                .travelName("도쿄 여행")
                .numOfPeople(4L)
                .numOfJoinedPeople(1L)
                .nation(Nation.JAPAN)
                .sharedFund(0L)
                .build();
    }

    private TravelUser travelUser(Long travelUserId, Travel travel, User user, Role role) {
        return TravelUser.builder()
                .travelUserId(travelUserId)
                .travel(travel)
                .user(user)
                .role(role)
                .travelNickname("지민")
                .build();
    }
}
