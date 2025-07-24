package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.payment.PaymentImageDto;
import com.yoen.yoen_back.dto.payment.PaymentRequestDto;
import com.yoen.yoen_back.dto.payment.PaymentResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementRequestDto;
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
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.payment.SettlementRepository;
import com.yoen.yoen_back.repository.payment.SettlementUserRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementUserRepository settlementUserRepository;
    private final PaymentImageRepository paymentImageRepository;

    /** 조회용 **/
    private final TravelUserRepository travelUserRepository;
    private final TravelRepository travelRepository;
    private final CategoryRepository categoryRepository;


    private final ImageService imageService;
    private final ExchangeRateUpdateService exchangeRateUpdateService;


    public List<Payment> getAllPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravel_TravelIdAndIsActiveTrue(travelId);
    }

    public Payment savePaymentEntity(PaymentRequestDto dto) {
        // isActive는 무조건 true
        Category category = categoryRepository.getReferenceById(dto.categoryId());
        TravelUser tu = travelUserRepository.getReferenceById(dto.travelUserId());

        // 환율 가져오는 로직
        LocalDateTime payTime = Formatter.getDateTime(dto.payTime());
        ExchangeRate exchangeRate = exchangeRateUpdateService.getExchangeRate(payTime);


        Travel tv = travelRepository.getReferenceById(dto.travelId());
        Payment payment = Payment.builder()
                .travel(tv)
                .payTime(payTime)
                .category(category)
                .payerType(dto.payerType())
                .paymentAccount(dto.paymentAccount())
                .exchangeRate(exchangeRate.getExchangeRate())
                .travelUser(tu)
                .paymentName(dto.paymentName())
                .paymentMethod(dto.paymentMethod())
                .build();

        return paymentRepository.save(payment);
    }

    public Settlement saveSettlementEntity(Payment payment, SettlementRequestDto dto) {
        Settlement sm = Settlement.builder()
                .payment(payment)
                .amount(dto.amount())
                .isPaid(dto.isPaid())
                .settlementName(dto.settlementName())
                .build();

        // 정산 저장
        return settlementRepository.save(sm);
    }

    public SettlementUser saveSettlementUserEntity(TravelUser tu, Settlement sm, SettlementRequestDto dto) {
        SettlementUser su = SettlementUser.builder()
                .travelUser(tu)
                .settlement(sm)
                .amount(dto.amount() / dto.travelUsers().size())
                .isPaid(dto.isPaid())
                .build();

        // 정산 유저 저장
        return settlementUserRepository.save(su);
    }

    // 정산 객체를 저장 -> 정산 객체와 금액기록을 매핑 -> 정산 객체에 정산_유저 객체 매핑 -> 함수 3개를 모은 createPayment 선언
    @Transactional
    public PaymentResponseDto createPayment(User user, PaymentRequestDto dto, List<MultipartFile> files) {
        //금액기록을 빌더 패턴으로 생성하여 저장한다
        Payment payment = savePaymentEntity(dto);

        // 정산리스트 저장 로직
        dto.settlementList().forEach(settlement -> {
            Settlement sm = saveSettlementEntity(payment, settlement);

            // 정산 유저 저장 로직
            settlement.travelUsers().forEach(travelUser -> {
                TravelUser tu = travelUserRepository.getReferenceById(travelUser);
                SettlementUser smu = saveSettlementUserEntity(tu, sm, settlement);
            });
        });

        // 이미지 파일이 존재할시
        if (files != null && !files.isEmpty()) {
            //받은 이미지들을 저장한다
            List<Image> images = imageService.saveImages(user, files);

            //이미지 리스트를 하나하나 변환하여 DTO List로 저장한다
            List<PaymentImageDto> imagesDto = images.stream().map(
                    image -> {
                        PaymentImage pi = PaymentImage.builder()
                                .image(image)
                                .payment(payment)
                                .build();
                        PaymentImage tmp = paymentImageRepository.save(pi);

                        return new PaymentImageDto(tmp.getPaymentImageId(), image.getImageId(), image.getImageUrl());
                    }
            ).toList();
            return new PaymentResponseDto(payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(),
                    payment.getPaymentMethod(), payment.getPaymentName(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), imagesDto);
        }

        // 아미지 파일이 존재 안할시
        return  new PaymentResponseDto(payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(),
                payment.getPaymentMethod(), payment.getPaymentName(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), new ArrayList<>());

    }

    // 정산유저 테스트
    public List<SettlementUser> getAllSettlementUsers() {
        return settlementUserRepository.findAll();
    }


    // 사진을 제외한 금액기록을 수정할시
    // Todo: 이미지 처리 해야하는데.. 지금 맨 처음 금액기록을 생성할 때는 이미지랑 금액기록이랑 한번에 보내는데 수정할때도 동일한 방식을 사용하면 시간이 너무 오래 걸릴거같아서
    public PaymentResponseDto updatePayment(PaymentRequestDto dto) {
        Payment pm = paymentRepository.getReferenceById(dto.paymentId());

        pm.setCategory(categoryRepository.getReferenceById(dto.categoryId())); // 카테고리
        pm.setPayerType(dto.payerType()); // 개인 or 공금
        pm.setPaymentMethod(dto.paymentMethod()); // 카드 or 현금
        pm.setPaymentAccount(dto.paymentAccount()); // 돈 총합
        pm.setExchangeRate(exchangeRateUpdateService.getExchangeRate(Formatter.getDateTime(dto.payTime())).getExchangeRate()); // 환율
        pm.setPaymentName(dto.paymentName()); // 금액기록 이름
        pm.setPayTime(Formatter.getDateTime(dto.payTime())); // 금액기록 시간
        paymentRepository.save(pm);
        return new PaymentResponseDto(pm.getPaymentId(), pm.getCategory().getCategoryId(), pm.getCategory().getCategoryName(), pm.getPayerType(),
                pm.getPaymentMethod(), pm.getPaymentName(), pm.getExchangeRate(), pm.getPayTime(), pm.getPaymentAccount(), new ArrayList<>());
    }

    // 기존 금액기록에 사진을 추가할시
    public void updatePaymentImages(User user, Long paymentId, List<MultipartFile> files) {
        //받은 이미지들을 저장한다
        List<Image> images = imageService.saveImages(user, files);
        Payment pm = paymentRepository.getReferenceById(paymentId);
        //이미지 리스트를 하나하나 변환하여 DTO List로 저장한다
        List<PaymentImageDto> imagesDto = images.stream().map(
                image -> {
                    PaymentImage pi = PaymentImage.builder()
                            .image(image)
                            .payment(pm)
                            .build();
                    PaymentImage tmp = paymentImageRepository.save(pi);

                    return new PaymentImageDto(tmp.getPaymentImageId(), image.getImageId(), image.getImageUrl());
                }
        ).toList();
    }

    // 기존 금액기록의 사진을 삭제할시
    public void deletePaymentImage(Long paymentImageId) {
        Optional<PaymentImage> paymentImage = paymentImageRepository.findByPaymentImageIdAndIsActiveTrue(paymentImageId);
        paymentImage.ifPresent(image -> {
            // 사진 모집단 삭제 (클라우드 삭제)
            Image img = image.getImage();
            imageService.deleteImage(img.getImageId());

            // paymentImage 삭제
            image.setIsActive(false);
            paymentImageRepository.save(image);
        });
    }

    public void deleteSettlement(Long settlementId) {
        Optional<Settlement> settlement = settlementRepository.findBySettlementIdAndIsActiveTrue(settlementId);

        settlement.ifPresent(stm -> {
            List<SettlementUser> su = settlementUserRepository.findAllBySettlement_SettlementId(stm.getSettlementId());
            su.forEach(stmu -> {
                // 관련 정산 유저 소프트 삭제
                stmu.setIsActive(false);
                settlementUserRepository.save(stmu);
            });

            // 관련 정산 소프트 삭제
            stm.setIsActive(false);
            settlementRepository.save(stm);
        });

    }

    // 금액기록 삭제하는 메서드
    public void deletePayment(Long paymentId) {
        // 금액기록 ID로 금액기록 찾아오기
        Payment pm = paymentRepository.getReferenceById(paymentId);
        // 금액기록 ID로 paymentImage 찾아오기
        List<PaymentImage> pi = paymentImageRepository.findByPayment_PaymentId(pm.getPaymentId());
        // 돌면서 이미지 soft delete
        pi.forEach(image -> {
            deletePaymentImage(image.getPaymentImageId());
        });

        // 마찬가지로 정산들 찾아와서 soft delete
        List<Settlement> st = settlementRepository.findByPayment_PaymentId(pm.getPaymentId());
        st.forEach(settlement -> {
            deleteSettlement(settlement.getSettlementId());
        });

        // 찾아온 금액기록 최종 soft delete
        pm.setIsActive(false);
        paymentRepository.save(pm);
    }

}
