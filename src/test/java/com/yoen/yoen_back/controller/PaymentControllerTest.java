package com.yoen.yoen_back.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoen.yoen_back.common.security.CustomUserDetails;
import com.yoen.yoen_back.dto.payment.PaymentRequestDto;
import com.yoen.yoen_back.dto.payment.PaymentResponseDto;
import com.yoen.yoen_back.dto.payment.PaymentSimpleResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementResultResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementUserResponseDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Currency;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.enums.Nation;
import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentMethod;
import com.yoen.yoen_back.enums.PaymentType;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.service.AuthService;
import com.yoen.yoen_back.service.PaymentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

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
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService, authService))
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

    // 결제 목록 조회 전에 reader/writer 권한 체크를 수행하고 필터 조건을 서비스에 넘기는지 확인한다.
    @Test
    void getSimplePayment_validRequest_checksRoleAndReturnsPayments() throws Exception {
        when(authService.checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(paymentService.getAllPaymentResponseDtoByTravelIdAndDate(travel, "2026-06-01T10:30:00", PaymentType.PAYMENT))
                .thenReturn(List.of(new PaymentSimpleResponseDto(
                        20L,
                        "저녁",
                        "식비",
                        LocalDateTime.of(2026, 6, 1, 10, 30),
                        "지민",
                        12000L,
                        Payer.INDIVIDUAL,
                        PaymentType.PAYMENT,
                        Currency.WON
                )));

        mockMvc.perform(get("/payment")
                        .param("travelId", "10")
                        .param("date", "2026-06-01T10:30:00")
                        .param("type", "PAYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].paymentId").value(20))
                .andExpect(jsonPath("$.data[0].paymentName").value("저녁"));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER));
        verify(paymentService).getAllPaymentResponseDtoByTravelIdAndDate(travel, "2026-06-01T10:30:00", PaymentType.PAYMENT);
    }

    // 결제 상세 조회 전에 reader/writer 권한 체크를 수행하고 상세 조회 서비스를 호출하는지 확인한다.
    @Test
    void getDetailPayment_validPaymentId_checksRoleAndReturnsPayment() throws Exception {
        when(paymentService.getDetailPayment(20L)).thenReturn(paymentResponse(20L));

        mockMvc.perform(get("/payment/detail")
                        .param("paymentId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(20))
                .andExpect(jsonPath("$.data.paymentName").value("저녁"));

        verify(authService).checkTravelUserRoleByPayment(user, 20L, List.of(Role.READER, Role.WRITER));
        verify(paymentService).getDetailPayment(20L);
    }

    // 전체 결제 엔티티 조회 전에 reader/writer 권한 체크를 수행하고 Travel 기준으로 조회하는지 확인한다.
    @Test
    void payment_validTravelId_checksRoleAndReturnsPayments() throws Exception {
        when(authService.checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER))).thenReturn(travelUser);
        when(paymentService.getAllPaymentsByTravel(travel)).thenReturn(List.of());

        mockMvc.perform(get("/payment/all")
                        .param("travelId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.READER, Role.WRITER));
        verify(paymentService).getAllPaymentsByTravel(travel);
    }

    // multipart 결제 생성 요청의 dto part를 읽고 writer 권한 체크 후 생성 서비스에 넘기는지 확인한다.
    @Test
    void createTravelPayment_validMultipartRequest_checksRoleAndCreatesPayment() throws Exception {
        PaymentRequestDto request = paymentRequest(null);
        when(paymentService.createPayment(eq(user), eq(request), eq(null))).thenReturn(paymentResponse(20L));

        mockMvc.perform(multipart("/payment/create")
                        .file(jsonPart("dto", request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value(20));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER));
        verify(paymentService).createPayment(user, request, null);
    }

    // 정산 사용자 전체 조회는 권한 체크 없이 서비스 결과를 ApiResponse로 감싸는지 확인한다.
    @Test
    void getAllSettlementUser_serviceReturnsUsers_returnsSettlementUsers() throws Exception {
        when(paymentService.getAllSettlementUsers())
                .thenReturn(List.of(new SettlementUserResponseDto(1L, 2L, 100L, 6000L, false)));

        mockMvc.perform(get("/payment/settlementuser/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].settlementUserId").value(1))
                .andExpect(jsonPath("$.data[0].amount").value(6000));

        verify(paymentService).getAllSettlementUsers();
    }

    // 결제 이미지 삭제 전에 writer 권한 체크를 수행하고 삭제 서비스를 호출하는지 확인한다.
    @Test
    void deletePaymentImage_validImageId_checksRoleAndDeletesImage() throws Exception {
        mockMvc.perform(delete("/payment/image/delete")
                        .param("imageId", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Image deleted successfully"));

        verify(authService).checkTravelUserRoleByPaymentImage(user, 30L, List.of(Role.WRITER));
        verify(paymentService).deletePaymentImage(30L);
    }

    // 결제 삭제 전에 writer 권한 체크를 수행하고 삭제 서비스를 호출하는지 확인한다.
    @Test
    void deletePayment_validPaymentId_checksRoleAndDeletesPayment() throws Exception {
        mockMvc.perform(delete("/payment/delete")
                        .param("paymentId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Payment deleted successfully"));

        verify(authService).checkTravelUserRoleByPayment(user, 20L, List.of(Role.WRITER));
        verify(paymentService).deletePayment(20L);
    }

    // multipart 결제 수정 요청의 dto part를 읽고 writer 권한 체크 후 수정 서비스에 넘기는지 확인한다.
    @Test
    void updatePayment_validMultipartRequest_checksRoleAndUpdatesPayment() throws Exception {
        PaymentRequestDto request = paymentRequest(20L);

        mockMvc.perform(multipart("/payment/update")
                        .file(jsonPart("dto", request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Payment updated successfully"));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER));
        verify(paymentService).updatePayment(user, request, null);
    }

    // 정산 결과 조회 전에 writer 권한 체크를 수행하고 옵션 값을 서비스에 전달하는지 확인한다.
    @Test
    void getSettlement_validRequest_checksRoleAndReturnsSettlement() throws Exception {
        when(authService.checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER))).thenReturn(travelUser);
        when(paymentService.getSettlement(travel, true, false, true, "2026-06-01T00:00:00", "2026-06-02T00:00:00"))
                .thenReturn(new SettlementResultResponseDto(List.of(), List.of()));

        mockMvc.perform(get("/payment/settlement")
                        .param("travelId", "10")
                        .param("includePreUseAmount", "true")
                        .param("includeSharedFund", "false")
                        .param("includeRecordedAmount", "true")
                        .param("startAt", "2026-06-01T00:00:00")
                        .param("endAt", "2026-06-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userSettlementList").isArray())
                .andExpect(jsonPath("$.data.paymentTypeList").isArray());

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER));
        verify(paymentService).getSettlement(travel, true, false, true, "2026-06-01T00:00:00", "2026-06-02T00:00:00");
    }

    // 정산 확정 전에 writer 권한 체크를 수행하고 확정 서비스를 호출하는지 확인한다.
    @Test
    void doSettlement_validRequest_checksRoleAndConfirmsSettlement() throws Exception {
        when(authService.checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER))).thenReturn(travelUser);

        mockMvc.perform(post("/payment/settlement/confirm")
                        .param("travelId", "10")
                        .param("includePreUseAmount", "true")
                        .param("includeSharedFund", "false")
                        .param("includeRecordedAmount", "true")
                        .param("startAt", "2026-06-01T00:00:00")
                        .param("endAt", "2026-06-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("모두 정상적으로 정산완료 되었습니다."));

        verify(authService).checkTravelUserRoleByTravel(user, 10L, List.of(Role.WRITER));
        verify(paymentService).doSettlement(travel, true, false, true, "2026-06-01T00:00:00", "2026-06-02T00:00:00");
    }

    private PaymentRequestDto paymentRequest(Long paymentId) {
        return new PaymentRequestDto(
                paymentId,
                10L,
                100L,
                5L,
                Payer.INDIVIDUAL,
                "2026-06-01T10:30:00",
                12000L,
                "저녁",
                PaymentMethod.CARD,
                Currency.WON,
                List.of(),
                List.of(),
                PaymentType.PAYMENT
        );
    }

    private PaymentResponseDto paymentResponse(Long paymentId) {
        return new PaymentResponseDto(
                10L,
                paymentId,
                5L,
                "식비",
                Payer.INDIVIDUAL,
                null,
                PaymentMethod.CARD,
                "저녁",
                PaymentType.PAYMENT,
                1.0,
                LocalDateTime.of(2026, 6, 1, 10, 30),
                12000L,
                Currency.WON,
                List.of(),
                List.of()
        );
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
