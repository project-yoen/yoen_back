package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.payment.PaymentRequestDto;
import com.yoen.yoen_back.dto.payment.PaymentResponseDto;
import com.yoen.yoen_back.dto.payment.PaymentSimpleResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementParticipantDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementRequestDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementResultResponseDto;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.ExchangeRate;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.PaymentImage;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.payment.Settlement;
import com.yoen.yoen_back.entity.payment.SettlementUser;
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
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.payment.SettlementRepository;
import com.yoen.yoen_back.repository.payment.SettlementUserRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    // PaymentService는 결제, 정산, 이미지, 여행 공금 등 의존성이 많다.
    // 이 테스트에서는 모든 의존성을 Mock으로 두고 PaymentService의 계산/상태 변경만 검증한다.
    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementUserRepository settlementUserRepository;

    @Mock
    private PaymentImageRepository paymentImageRepository;

    @Mock
    private TravelUserRepository travelUserRepository;

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private ExchangeRateUpdateService exchangeRateUpdateService;

    // 위 Mock들을 PaymentService 생성자에 주입한다.
    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 생성 시 결제, 정산, 정산 사용자를 저장하고 응답 DTO를 반환한다")
    void createPayment_savesPaymentSettlementsAndSettlementUsers() {
        User payer = user(1L, "payer@example.com", "지민");
        Travel travel = travel(10L, 0L);
        TravelUser payerTravelUser = travelUser(100L, travel, payer, "지민");
        TravelUser memberTravelUser = travelUser(101L, travel, user(2L, "member@example.com", "서준"), "서준");
        Category category = category(20L, "식비", PaymentType.PAYMENT);
        PaymentRequestDto request = paymentRequest(
                null,
                travel.getTravelId(),
                payerTravelUser.getTravelUserId(),
                category.getCategoryId(),
                Payer.INDIVIDUAL,
                12000L,
                PaymentType.PAYMENT,
                Currency.WON,
                List.of(new SettlementRequestDto(
                        null,
                        null,
                        "저녁",
                        12000L,
                        false,
                        List.of(
                                new SettlementParticipantDto(payerTravelUser.getTravelUserId(), "지민", true),
                                new SettlementParticipantDto(memberTravelUser.getTravelUserId(), "서준", false)
                        )
                ))
        );
        // 결제 저장에 필요한 공통 Repository 응답을 한 번에 준비한다.
        stubBasePaymentSave(category, payerTravelUser, travel, 1.0);
        when(travelUserRepository.getReferenceById(payerTravelUser.getTravelUserId())).thenReturn(payerTravelUser);
        when(travelUserRepository.getReferenceById(memberTravelUser.getTravelUserId())).thenReturn(memberTravelUser);
        // 실제 DB가 없으므로 정산 저장 시 ID가 생긴 것처럼 응답한다.
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(invocation -> {
            Settlement settlement = invocation.getArgument(0);
            settlement.setSettlementId(200L);
            return settlement;
        });
        // 정산 참여자도 저장 후 ID가 생긴 것처럼 응답한다.
        when(settlementUserRepository.save(any(SettlementUser.class))).thenAnswer(invocation -> {
            SettlementUser settlementUser = invocation.getArgument(0);
            settlementUser.setSettlementUserId(settlementUser.getTravelUser().getTravelUserId() + 1000);
            return settlementUser;
        });

        PaymentResponseDto response = paymentService.createPayment(payer, request, null);

        assertThat(response.travelId()).isEqualTo(10L);
        assertThat(response.paymentId()).isEqualTo(300L);
        assertThat(response.categoryId()).isEqualTo(20L);
        assertThat(response.payerName().travelUserId()).isEqualTo(100L);
        assertThat(response.paymentAccount()).isEqualTo(12000L);
        assertThat(response.images()).isEmpty();
        assertThat(response.settlements()).hasSize(1);
        assertThat(response.settlements().get(0).amount()).isEqualTo(12000L);
        assertThat(response.settlements().get(0).isPaid()).isFalse();
        assertThat(response.settlements().get(0).travelUsers()).hasSize(2);

        // 각 참여자에게 저장된 정산 금액과 결제 완료 여부를 직접 확인한다.
        ArgumentCaptor<SettlementUser> settlementUserCaptor = ArgumentCaptor.forClass(SettlementUser.class);
        verify(settlementUserRepository, times(2)).save(settlementUserCaptor.capture());
        assertThat(settlementUserCaptor.getAllValues())
                .extracting(SettlementUser::getAmount)
                .containsExactly(6000L, 6000L);
        assertThat(settlementUserCaptor.getAllValues())
                .extracting(SettlementUser::getIsPaid)
                .containsExactly(true, false);
    }

    @Test
    @DisplayName("공금 충전 결제 생성 시 여행 공금을 증가시킨다")
    void createPayment_increasesSharedFund_whenTypeIsSharedFund() {
        User payer = user(1L, "payer@example.com", "지민");
        Travel travel = travel(10L, 1000L);
        TravelUser payerTravelUser = travelUser(100L, travel, payer, "지민");
        PaymentRequestDto request = paymentRequest(
                null,
                travel.getTravelId(),
                payerTravelUser.getTravelUserId(),
                20L,
                Payer.INDIVIDUAL,
                5000L,
                PaymentType.SHAREDFUND,
                Currency.WON,
                List.of()
        );
        stubBasePaymentSave(category(20L, "공금 채우기", PaymentType.SHAREDFUND), payerTravelUser, travel, 1.0);

        paymentService.createPayment(payer, request, null);

        assertThat(travel.getSharedFund()).isEqualTo(6000L);
        verify(travelRepository).save(travel);
    }

    @Test
    @DisplayName("공금으로 결제 생성 시 여행 공금을 감소시킨다")
    void createPayment_decreasesSharedFund_whenPayerIsSharedFund() {
        User user = user(1L, "payer@example.com", "지민");
        Travel travel = travel(10L, 10000L);
        Category category = category(20L, "식비", PaymentType.PAYMENT);
        PaymentRequestDto request = paymentRequest(
                null,
                travel.getTravelId(),
                null,
                category.getCategoryId(),
                Payer.SHAREDFUND,
                4000L,
                PaymentType.PAYMENT,
                Currency.WON,
                List.of()
        );
        stubBasePaymentSave(category, null, travel, 1.0);

        paymentService.createPayment(user, request, null);

        assertThat(travel.getSharedFund()).isEqualTo(6000L);
    }

    @Test
    @DisplayName("공금 잔액보다 큰 금액을 공금으로 결제하면 예외를 던진다")
    void createPayment_throwsException_whenSharedFundIsNotEnough() {
        User user = user(1L, "payer@example.com", "지민");
        Travel travel = travel(10L, 1000L);
        Category category = category(20L, "식비", PaymentType.PAYMENT);
        PaymentRequestDto request = paymentRequest(
                null,
                travel.getTravelId(),
                null,
                category.getCategoryId(),
                Payer.SHAREDFUND,
                4000L,
                PaymentType.PAYMENT,
                Currency.WON,
                List.of()
        );
        stubBasePaymentSave(category, null, travel, 1.0);

        assertThatThrownBy(() -> paymentService.createPayment(user, request, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("외화 정산 사용자 금액은 환율 적용 후 인원수로 나누어 저장한다")
    void saveSettlementUserEntity_appliesExchangeRateForForeignCurrency() {
        Travel travel = travel(10L, 0L);
        TravelUser travelUser = travelUser(100L, travel, user(1L, "payer@example.com", "지민"), "지민");
        Payment payment = payment(300L, travel, travelUser, category(20L, "식비", PaymentType.PAYMENT), 1000L, PaymentType.PAYMENT, Payer.INDIVIDUAL);
        payment.setCurrency(Currency.YEN);
        payment.setExchangeRate(9.5);
        Settlement settlement = settlement(200L, payment, 1000L, false);
        // 외화 결제는 settlement.amount * exchangeRate를 먼저 계산한 뒤 인원수로 나눈다.
        when(settlementUserRepository.save(any(SettlementUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SettlementUser result = paymentService.saveSettlementUserEntity(travelUser, settlement, 2L, payment, false);

        assertThat(result.getAmount()).isEqualTo(4750L);
        assertThat(result.getTravelUser()).isSameAs(travelUser);
        assertThat(result.getSettlement()).isSameAs(settlement);
        assertThat(result.getIsPaid()).isFalse();
    }

    @Test
    @DisplayName("결제 상세 조회 시 결제, 정산 참여자, 이미지를 응답 DTO로 조립한다")
    void getDetailPayment_returnsPaymentResponse() {
        User payer = user(1L, "payer@example.com", "지민");
        payer.setProfileImage(image(9L, "https://cdn.example.com/profile.png", "profile.png"));
        Travel travel = travel(10L, 0L);
        TravelUser payerTravelUser = travelUser(100L, travel, payer, "지민");
        Category category = category(20L, "식비", PaymentType.PAYMENT);
        Payment payment = payment(300L, travel, payerTravelUser, category, 12000L, PaymentType.PAYMENT, Payer.INDIVIDUAL);
        Settlement settlement = settlement(200L, payment, 12000L, false);
        SettlementUser settlementUser = settlementUser(400L, settlement, payerTravelUser, 12000L, false);
        PaymentImage paymentImage = paymentImage(500L, payment, image(30L, "https://cdn.example.com/receipt.png", "receipt.png"));
        // 상세 조회는 Payment 기준으로 정산 목록, 정산 참여자, 첨부 이미지를 차례로 조립한다.
        when(paymentRepository.getReferenceById(300L)).thenReturn(payment);
        when(settlementRepository.findByPayment_PaymentIdAndIsActiveTrue(300L)).thenReturn(List.of(settlement));
        when(settlementUserRepository.findBySettlementAndIsActiveTrue(settlement)).thenReturn(List.of(settlementUser));
        when(paymentImageRepository.findByPayment(payment)).thenReturn(List.of(paymentImage));

        PaymentResponseDto response = paymentService.getDetailPayment(300L);

        assertThat(response.paymentId()).isEqualTo(300L);
        assertThat(response.categoryName()).isEqualTo("식비");
        assertThat(response.payerName().travelUserId()).isEqualTo(100L);
        assertThat(response.payerName().imageUrl()).isEqualTo("https://cdn.example.com/profile.png");
        assertThat(response.settlements()).hasSize(1);
        assertThat(response.settlements().get(0).travelUsers()).hasSize(1);
        assertThat(response.images()).hasSize(1);
        assertThat(response.images().get(0).imageUrl()).isEqualTo("https://cdn.example.com/receipt.png");
    }

    @Test
    @DisplayName("날짜와 타입으로 결제 목록을 간단 응답 DTO로 조회한다")
    void getAllPaymentResponseDtoByTravelIdAndDate_returnsSimpleResponses() {
        Travel travel = travel(10L, 0L);
        TravelUser travelUser = travelUser(100L, travel, user(1L, "payer@example.com", "지민"), "지민");
        Category category = category(20L, "식비", PaymentType.PAYMENT);
        Payment payment = payment(300L, travel, travelUser, category, 12000L, PaymentType.PAYMENT, Payer.INDIVIDUAL);
        // date 파라미터는 해당 날짜 00:00부터 다음 날 00:00 전까지의 범위로 변환된다.
        when(paymentRepository.findAllByTravelAndTypeAndPayTimeBetweenAndIsActiveTrue(
                eq(travel),
                eq(PaymentType.PAYMENT),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 2, 0, 0))
        )).thenReturn(List.of(payment));

        List<PaymentSimpleResponseDto> responses = paymentService.getAllPaymentResponseDtoByTravelIdAndDate(
                travel,
                "2026-06-01T13:30:00",
                PaymentType.PAYMENT
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).paymentId()).isEqualTo(300L);
        assertThat(responses.get(0).payer()).isEqualTo("지민");
        assertThat(responses.get(0).categoryName()).isEqualTo("식비");
    }

    @Test
    @DisplayName("공금 충전 기록 삭제 시 공금을 차감하고 연관 데이터를 비활성화한다")
    void deletePayment_decreasesSharedFundAndSoftDeletesRelatedData_whenSharedFundPayment() {
        Travel travel = travel(10L, 10000L);
        TravelUser payer = travelUser(100L, travel, user(1L, "payer@example.com", "지민"), "지민");
        Payment payment = payment(300L, travel, payer, category(20L, "공금 채우기", PaymentType.SHAREDFUND), 4000L, PaymentType.SHAREDFUND, Payer.INDIVIDUAL);
        Image receiptImage = image(30L, "https://cdn.example.com/receipt.png", "receipt.png");
        PaymentImage paymentImage = paymentImage(500L, payment, receiptImage);
        Settlement settlement = settlement(200L, payment, 4000L, false);
        SettlementUser settlementUser = settlementUser(400L, settlement, payer, 4000L, false);
        // 삭제는 Payment만 지우는 것이 아니라 이미지/정산/정산 참여자까지 soft delete한다.
        when(paymentRepository.getReferenceById(300L)).thenReturn(payment);
        when(paymentImageRepository.findByPayment_PaymentId(300L)).thenReturn(List.of(paymentImage));
        when(paymentImageRepository.findByPaymentImageIdAndIsActiveTrue(500L)).thenReturn(Optional.of(paymentImage));
        when(settlementRepository.findByPayment_PaymentIdAndIsActiveTrue(300L)).thenReturn(List.of(settlement));
        when(settlementRepository.findBySettlementIdAndIsActiveTrue(200L)).thenReturn(Optional.of(settlement));
        when(settlementUserRepository.findAllBySettlementAndIsActiveTrue(settlement)).thenReturn(List.of(settlementUser));

        paymentService.deletePayment(300L);

        assertThat(travel.getSharedFund()).isEqualTo(6000L);
        assertThat(payment.getIsActive()).isFalse();
        assertThat(paymentImage.getIsActive()).isFalse();
        assertThat(settlement.getIsActive()).isFalse();
        assertThat(settlementUser.getIsActive()).isFalse();
        verify(imageService).deleteImage(30L);
        verify(paymentRepository).save(payment);
        verify(travelRepository).save(travel);
    }

    @Test
    @DisplayName("정산 결과 조회 시 미정산 금액을 받을 사람 기준으로 계산한다")
    void getSettlement_calculatesUnpaidAmountsByReceiver() {
        Travel travel = travel(10L, 0L);
        TravelUser payer = travelUser(100L, travel, user(1L, "payer@example.com", "지민"), "지민");
        TravelUser member = travelUser(101L, travel, user(2L, "member@example.com", "서준"), "서준");
        Payment payment = payment(300L, travel, payer, category(20L, "식비", PaymentType.PAYMENT), 12000L, PaymentType.PAYMENT, Payer.INDIVIDUAL);
        Settlement settlement = settlement(200L, payment, 12000L, false);
        SettlementUser paidPayer = settlementUser(400L, settlement, payer, 6000L, true);
        SettlementUser unpaidMember = settlementUser(401L, settlement, member, 6000L, false);
        // 기록된 결제만 포함하도록 옵션을 구성하면 PaymentType.PAYMENT 정산만 조회된다.
        when(settlementRepository.findSettlementByOptions(
                eq(travel),
                eq(List.of(PaymentType.PAYMENT)),
                eq(LocalDateTime.of(2026, 6, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 6, 2, 0, 0))
        )).thenReturn(List.of(settlement));
        when(travelUserRepository.findByTravelAndIsActiveTrue(travel)).thenReturn(List.of(payer, member));
        when(travelUserRepository.getReferenceById(100L)).thenReturn(payer);
        when(travelUserRepository.getReferenceById(101L)).thenReturn(member);
        when(settlementUserRepository.findBySettlementAndIsActiveTrue(settlement)).thenReturn(List.of(paidPayer, unpaidMember));

        SettlementResultResponseDto response = paymentService.getSettlement(
                travel,
                false,
                false,
                true,
                "2026-06-01T00:00:00",
                "2026-06-02T00:00:00"
        );

        assertThat(response.userSettlementList()).hasSize(2);
        assertThat(response.userSettlementList())
                .filteredOn(userSettlement -> userSettlement.receiverNickname().equals("지민"))
                .singleElement()
                .satisfies(userSettlement -> {
                    assertThat(userSettlement.userSettlementList()).hasSize(1);
                    assertThat(userSettlement.userSettlementList().get(0).senderNickname()).isEqualTo("서준");
                    assertThat(userSettlement.userSettlementList().get(0).amount()).isEqualTo(6000L);
                });
        assertThat(response.paymentTypeList()).hasSize(3);
    }

    private void stubBasePaymentSave(Category category, TravelUser travelUser, Travel travel, Double exchangeRate) {
        when(categoryRepository.findById(category.getCategoryId())).thenReturn(Optional.of(category));
        if (travelUser != null) {
            when(travelUserRepository.findByTravelUserIdAndIsActiveTrue(travelUser.getTravelUserId()))
                    .thenReturn(Optional.of(travelUser));
        }
        when(exchangeRateUpdateService.getExchangeRate(any(LocalDateTime.class)))
                .thenReturn(ExchangeRate.builder().exchangeRate(exchangeRate).build());
        when(travelRepository.getReferenceById(travel.getTravelId())).thenReturn(travel);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setPaymentId(300L);
            return payment;
        });
    }

    private PaymentRequestDto paymentRequest(
            Long paymentId,
            Long travelId,
            Long travelUserId,
            Long categoryId,
            Payer payerType,
            Long paymentAccount,
            PaymentType paymentType,
            Currency currency,
            List<SettlementRequestDto> settlements
    ) {
        return new PaymentRequestDto(
                paymentId,
                travelId,
                travelUserId,
                categoryId,
                payerType,
                "2026-06-01T12:30:00",
                paymentAccount,
                "점심",
                PaymentMethod.CARD,
                currency,
                settlements,
                List.of(),
                paymentType
        );
    }

    private Payment payment(Long paymentId, Travel travel, TravelUser travelUser, Category category, Long amount, PaymentType type, Payer payer) {
        return Payment.builder()
                .paymentId(paymentId)
                .travel(travel)
                .travelUser(travelUser)
                .category(category)
                .paymentName("점심")
                .paymentMethod(PaymentMethod.CARD)
                .payerType(payer)
                .payTime(LocalDateTime.of(2026, 6, 1, 12, 30))
                .paymentAccount(amount)
                .currency(Currency.WON)
                .exchangeRate(1.0)
                .type(type)
                .build();
    }

    private Settlement settlement(Long settlementId, Payment payment, Long amount, Boolean isPaid) {
        return Settlement.builder()
                .settlementId(settlementId)
                .payment(payment)
                .settlementName("정산")
                .amount(amount)
                .isPaid(isPaid)
                .build();
    }

    private SettlementUser settlementUser(Long settlementUserId, Settlement settlement, TravelUser travelUser, Long amount, Boolean isPaid) {
        return SettlementUser.builder()
                .settlementUserId(settlementUserId)
                .settlement(settlement)
                .travelUser(travelUser)
                .amount(amount)
                .isPaid(isPaid)
                .build();
    }

    private PaymentImage paymentImage(Long paymentImageId, Payment payment, Image image) {
        return PaymentImage.builder()
                .paymentImageId(paymentImageId)
                .payment(payment)
                .image(image)
                .build();
    }

    private Travel travel(Long travelId, Long sharedFund) {
        return Travel.builder()
                .travelId(travelId)
                .travelName("도쿄 여행")
                .numOfPeople(3L)
                .numOfJoinedPeople(2L)
                .nation(Nation.JAPAN)
                .sharedFund(sharedFund)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 5))
                .build();
    }

    private TravelUser travelUser(Long travelUserId, Travel travel, User user, String travelNickname) {
        return TravelUser.builder()
                .travelUserId(travelUserId)
                .travel(travel)
                .user(user)
                .role(Role.WRITER)
                .travelNickname(travelNickname)
                .build();
    }

    private User user(Long userId, String email, String nickname) {
        return User.builder()
                .userId(userId)
                .email(email)
                .password("encoded-password")
                .name(nickname)
                .nickname(nickname)
                .gender(Gender.MALE)
                .birthday(LocalDate.of(1998, 3, 12))
                .build();
    }

    private Category category(Long categoryId, String name, PaymentType type) {
        return Category.builder()
                .categoryId(categoryId)
                .categoryName(name)
                .type(type)
                .build();
    }

    private Image image(Long imageId, String imageUrl, String objectKey) {
        return Image.builder()
                .imageId(imageId)
                .imageUrl(imageUrl)
                .objectKey(objectKey)
                .build();
    }
}
