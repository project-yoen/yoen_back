package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.payment.PaymentImageDto;
import com.yoen.yoen_back.dto.payment.PaymentRequestDto;
import com.yoen.yoen_back.dto.payment.PaymentResponseDto;
import com.yoen.yoen_back.dto.payment.PaymentSimpleResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementRequestDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementResponseDto;
import com.yoen.yoen_back.dto.payment.settlement.SettlementUserResponseDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
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
import com.yoen.yoen_back.enums.Payer;
import com.yoen.yoen_back.enums.PaymentType;
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

    /**
     * 조회용
     **/
    private final TravelUserRepository travelUserRepository;
    private final TravelRepository travelRepository;
    private final CategoryRepository categoryRepository;


    private final ImageService imageService;
    private final ExchangeRateUpdateService exchangeRateUpdateService;


    public List<Payment> getAllPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravel_TravelIdAndIsActiveTrue(travelId);
    }

    public List<Payment> getAllPaymentsByTravel(Travel travel) {
        return paymentRepository.findByTravelAndIsActiveTrue(travel);
    }


    // TODO: 날짜별 금액기록 리스트 받기
    public List<PaymentSimpleResponseDto> getAllPaymentResponseDtoByTravelId(Travel tv, String date) {
        LocalDateTime localDateTime = Formatter.getDateTime(date);
        List<Payment> pmList = paymentRepository.findAllByTravelAndPayTimeBetween(tv, localDateTime, localDateTime.plusDays(1));
        return pmList.stream().map(payment ->
                new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                        payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount())).toList();
    }

    // TODO: 금액기록 아이디로 금액기록 정보 받기


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
                .type(dto.paymentType())
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

    public SettlementUser saveSettlementUserEntity(TravelUser tu, Settlement sm, Long size) {
        SettlementUser su = SettlementUser.builder()
                .travelUser(tu)
                .settlement(sm)
                .amount(sm.getAmount() / size)
                .isPaid(sm.getIsPaid())
                .build();

        // 정산 유저 저장
        return settlementUserRepository.save(su);
    }

    // 정산 객체를 저장 -> 정산 객체와 금액기록을 매핑 -> 정산 객체에 정산_유저 객체 매핑 -> 함수 3개를 모은 createPayment 선언
    @Transactional
    public PaymentResponseDto createPayment(User user, PaymentRequestDto dto, List<MultipartFile> files) {
        // 금액기록을 빌더 패턴으로 생성하여 저장한다
        Payment payment = savePaymentEntity(dto);
        if (payment.getType().equals(PaymentType.SHAREDFUND)) {
            Travel tv = travelRepository.getReferenceById(dto.travelId());
            tv.setSharedFund(tv.getSharedFund() + payment.getPaymentAccount());
            travelRepository.save(tv);
        }

        if (payment.getType().equals(PaymentType.PAYMENT) && payment.getPayerType().equals(Payer.SHAREDFUND)) {
            Travel tv = travelRepository.getReferenceById(dto.travelId());
            if (tv.getSharedFund() - payment.getPaymentAccount() < 0) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }
            tv.setSharedFund(tv.getSharedFund() - payment.getPaymentAccount());
            travelRepository.save(tv);
        }

        // 정산리스트 저장 로직
        List<SettlementResponseDto> settlementResponse = dto.settlementList().stream().map(settlement -> {
            Settlement sm = saveSettlementEntity(payment, settlement);

            // 정산 유저 저장 로직
            List<TravelUserDto> travelUsersResponse = getTravelUserDtosBySettlement(settlement, sm);

            return new SettlementResponseDto(sm.getSettlementId(), settlement.paymentId(), settlement.settlementName(), settlement.amount(), sm.getIsPaid(), travelUsersResponse);

        }).toList();
        TravelUser payer = payment.getTravelUser();
        TravelUserDto payerDto = new TravelUserDto(payer.getTravelUserId(), payer.getTravel().getTravelId(), payer.getUser().getUserId(), payer.getRole(), payer.getTravelNickname());
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

                        return new PaymentImageDto(tmp.getPaymentImageId(), image.getImageUrl());
                    }
            ).toList();
            return new PaymentResponseDto(payment.getTravel().getTravelId(), payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(), payerDto,
                    payment.getPaymentMethod(), payment.getPaymentName(), payment.getType(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), settlementResponse, imagesDto);
        }

        // 아미지 파일이 존재 안할시
        return new PaymentResponseDto(payment.getTravel().getTravelId(), payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(), payerDto,
                payment.getPaymentMethod(), payment.getPaymentName(), payment.getType(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), settlementResponse, new ArrayList<>());

    }

    // 정산유저 테스트
    public List<SettlementUserResponseDto> getAllSettlementUsers() {
        List<SettlementUser> stuList = settlementUserRepository.findAll();
        return stuList.stream().map(settlementUser -> new SettlementUserResponseDto(settlementUser.getSettlementUserId(),
                settlementUser.getSettlement().getSettlementId(), settlementUser.getTravelUser().getTravelUserId(), settlementUser.getAmount(),
                settlementUser.getIsPaid())
        ).toList();
    }

    // 기존 settlement를 전부 삭제 후, 새 settlement 추가
    public List<SettlementResponseDto> updateSettlement(Payment payment, List<Settlement> preSettlements, List<SettlementRequestDto> dto) {
        preSettlements.forEach(this::deleteSettlement);

        return dto.stream().map(settlement -> {
            Settlement st = saveSettlementEntity(payment, settlement);
            // 정산 유저 저장 로직
            settlement.travelUsers().forEach(travelUser -> {
                TravelUser tu = travelUserRepository.getReferenceById(travelUser);
                SettlementUser smu = saveSettlementUserEntity(tu, st, (long) settlement.travelUsers().size());
            });
            Settlement savedSettlement = settlementRepository.save(st);

            // 정산 유저 저장 로직
            List<TravelUserDto> travelUsersResponse = getTravelUserDtosBySettlement(settlement, savedSettlement);

            return new SettlementResponseDto(savedSettlement.getSettlementId(), payment.getPaymentId(), savedSettlement.getSettlementName(), savedSettlement.getAmount(), savedSettlement.getIsPaid(), travelUsersResponse);
        }).toList();
    }

    private List<TravelUserDto> getTravelUserDtosBySettlement(SettlementRequestDto settlement, Settlement savedSettlement) {
        return settlement.travelUsers().stream().map(travelUser -> {
            TravelUser tu = travelUserRepository.getReferenceById(travelUser);
            SettlementUser smu = saveSettlementUserEntity(tu, savedSettlement, (long) settlement.travelUsers().size());
            return new TravelUserDto(tu.getTravelUserId(), tu.getTravel().getTravelId(), tu.getUser().getUserId(), tu.getRole(), tu.getTravelNickname());
        }).toList();
    }

    // 사진을 제외한 금액기록을 수정할시
    // Todo: 이미지 처리 해야하는데.. 지금 맨 처음 금액기록을 생성할 때는 이미지랑 금액기록이랑 한번에 보내는데 수정할때도 동일한 방식을 사용하면 시간이 너무 오래 걸릴거같아서
    public PaymentResponseDto updatePayment(PaymentRequestDto dto) {
        Payment pm = paymentRepository.getReferenceById(dto.paymentId());

        // 1.공금을 채운 경우 (전체 금액이 높아졌을 때)
        if (dto.paymentType().equals(PaymentType.SHAREDFUND)) {
            Travel tv = travelRepository.getReferenceById(dto.travelId());
            if (tv.getSharedFund() + dto.paymentAccount() - pm.getPaymentAccount() < 0) {
                throw new IllegalStateException("잔액이 충분하지 않습니다.(공금이 음수)");
            } else {
                tv.setSharedFund(tv.getSharedFund() - pm.getPaymentAccount() + dto.paymentAccount());
                travelRepository.save(tv);
            }
        }
        List<Settlement> settlements = settlementRepository.findByPayment(pm);
        // 기존 settlement들 삭제 후 다시 생성 그리고 settlementResponseDto 반환
        List<SettlementResponseDto> updatedSettlements = updateSettlement(pm, settlements, dto.settlementList());


        pm.setCategory(categoryRepository.getReferenceById(dto.categoryId())); // 카테고리
        pm.setPayerType(dto.payerType()); // 개인 or 공금
        pm.setPaymentMethod(dto.paymentMethod()); // 카드 or 현금
        pm.setPaymentAccount(dto.paymentAccount()); // 돈 총합
        pm.setExchangeRate(exchangeRateUpdateService.getExchangeRate(Formatter.getDateTime(dto.payTime())).getExchangeRate()); // 환율
        pm.setPaymentName(dto.paymentName()); // 금액기록 이름
        pm.setPayTime(Formatter.getDateTime(dto.payTime())); // 금액기록 시간
        paymentRepository.save(pm);

        TravelUser payer = pm.getTravelUser();
        TravelUserDto payerDto = new TravelUserDto(payer.getTravelUserId(), payer.getTravel().getTravelId(), payer.getUser().getUserId(), payer.getRole(), payer.getTravelNickname());
        return new PaymentResponseDto(pm.getTravel().getTravelId(), pm.getPaymentId(), pm.getCategory().getCategoryId(), pm.getCategory().getCategoryName(), pm.getPayerType(), payerDto,
                pm.getPaymentMethod(), pm.getPaymentName(), pm.getType(), pm.getExchangeRate(), pm.getPayTime(), pm.getPaymentAccount(), updatedSettlements, new ArrayList<>());
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

                    return new PaymentImageDto(tmp.getPaymentImageId(), image.getImageUrl());
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

    // settlementID로 settlement관련 모든거 지우는함수
    private void deleteSettlement(Long settlementId) {
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

    // settlement로 settlement관련 모든거 지우는함수
    private void deleteSettlement(Settlement settlement) {
        List<SettlementUser> su = settlementUserRepository.findAllBySettlement_SettlementId(settlement.getSettlementId());
        su.forEach(stmu -> {
            // 관련 정산 유저 소프트 삭제
            stmu.setIsActive(false);
            settlementUserRepository.save(stmu);
        });

        // 관련 정산 소프트 삭제
        settlement.setIsActive(false);
        settlementRepository.save(settlement);
    }

    // 금액기록 삭제하는 메서드
    public void deletePayment(Long paymentId) {
        // 금액기록 ID로 금액기록 찾아오기
        Payment pm = paymentRepository.getReferenceById(paymentId);
        Travel tv = pm.getTravel();

        // 공금등록기록을 삭제했을시, 그만큼 공금에서 빼주기
        if (pm.getType().equals(PaymentType.SHAREDFUND)) {
            if (tv.getSharedFund() - pm.getPaymentAccount() < 0) throw new IllegalStateException("잔액이 부족합니다.");
            tv.setSharedFund(tv.getSharedFund() - pm.getPaymentAccount());
            travelRepository.save(tv);
        }

        // 공금으로 계산했을시, 공금계산기록 삭제했을때 그만큼 공금에 더해주기
        if (pm.getType().equals(PaymentType.PAYMENT) && pm.getPayerType().equals(Payer.SHAREDFUND)) {
            tv.setSharedFund(tv.getSharedFund() + pm.getPaymentAccount());
            travelRepository.save(tv);
        }
        // 공금 등록

        // Todo: 금액기록 삭제할 때 공금이였으면 현재 공금에서 삭제되는만큼의 가격을 빼준다..?
//        if(pm.getType().equals(PaymentType.SHAREDFUND)){
//
//        }
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
    //PaymentId로 Payment찾고 settlement 안의 paymentId로 settlement 찾고 travelUser를 찾아서 PaymentResponseDto채워서 보내기
    //금액기록을 클릭했을 때 세부적인 내용을 반환하는 메서드
    public PaymentResponseDto getDetailPayment(Long paymentId) {
        //paymentId로 payment 찾아오고 payment에 있는 travelId로 travelUser 찾기
        Payment pm = paymentRepository.getReferenceById(paymentId);

        //Payment에 속한 settlement 리스트 받아오기
        List<Settlement> stList = settlementRepository.findByPayment_PaymentId(paymentId);
        //settlement 리스트 돌면서 PaymentResponseDto에 들어갈 SettlementResponseDto 만들기
        List<SettlementResponseDto> stResponseDtoList = stList.stream().map(settlement -> {
                List<SettlementUser> stuList = settlementUserRepository.findBySettlement(settlement);
                List<TravelUserDto> tuDtoList = stuList.stream().map(stu -> {
                    TravelUser tu = stu.getTravelUser();
                    return new TravelUserDto(tu.getTravelUserId(), pm.getTravel().getTravelId(), tu.getUser().getUserId(),
                            tu.getRole(), tu.getTravelNickname());
                }).toList();
                return new SettlementResponseDto(settlement.getSettlementId(), pm.getPaymentId(), settlement.getSettlementName(), settlement.getAmount(),
                        settlement.getIsPaid(), tuDtoList);
        }).toList();

        //PaymentImage에 존재하는 이미지 PayemntId로 가져오기
        List<PaymentImage> pmiList = paymentImageRepository.findByPayment(pm);
        //PaymentImage 리스트 돌면서 PaymentResponseDto에 들어갈 PaymentImageDtoList 만들기
        List<PaymentImageDto> pmimageDtoList = pmiList.stream().map(paymentImage -> new PaymentImageDto(paymentImage.getPaymentImageId(),
                paymentImage.getImage().getImageUrl())).toList();
        TravelUser payer = pm.getTravelUser();
        TravelUserDto payerDto = new TravelUserDto(payer.getTravelUserId(), payer.getTravel().getTravelId(), payer.getUser().getUserId(), payer.getRole(), payer.getTravelNickname());
        return new PaymentResponseDto(pm.getTravel().getTravelId(), pm.getPaymentId(), pm.getCategory().getCategoryId(), pm.getCategory().getCategoryName(),
                pm.getPayerType(), payerDto, pm.getPaymentMethod(), pm.getPaymentName(), pm.getType(), pm.getExchangeRate(), pm.getPayTime(),
                pm.getPaymentAccount(), stResponseDtoList, pmimageDtoList);
    }
}
