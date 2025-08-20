package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.payment.*;
import com.yoen.yoen_back.dto.payment.settlement.*;
import com.yoen.yoen_back.dto.travel.TravelUserResponseDto;
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
import com.yoen.yoen_back.enums.*;
import com.yoen.yoen_back.enums.Currency;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.payment.SettlementRepository;
import com.yoen.yoen_back.repository.payment.SettlementUserRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
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


    // 날짜별 금액기록 리스트 받기
    public List<PaymentSimpleResponseDto> getAllPaymentResponseDtoByTravelIdAndDate(Travel tv, String date, PaymentType paymentType) {
        // date가 전달된 경우 해당 날짜의 기록을 반환
        if (date != null) {
            LocalDateTime localDateTime = Formatter.getDateTime(date)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            if (paymentType == PaymentType.PAYMENT) {
                return getPaymentByTravelIdAndDate(tv, localDateTime, paymentType);
            } else if (paymentType == PaymentType.SHAREDFUND) {
                return getSharedFundByTravelIdAndDate(tv, localDateTime, paymentType);
            } else {
                return getPaymentAndSharedFundByTravelId(tv, localDateTime, paymentType);
            }

            // date가 null인 경우 type에 따라서 모든 기록 반환
        } else {
            return getPaymentByTypeAndTravelId(tv, paymentType);

        }
    }

    // 날짜별 Payment를 가져오는 메서드
    public List<PaymentSimpleResponseDto> getPaymentByTravelIdAndDate(Travel tv, LocalDateTime localDateTime, PaymentType paymentType) {
        List<Payment> pmList = paymentRepository.findAllByTravelAndTypeAndPayTimeBetweenAndIsActiveTrue(tv, paymentType, localDateTime, localDateTime.plusDays(1));
        return pmList.stream().map(payment -> {
            if (payment.getPayerType() == Payer.SHAREDFUND) {
                return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                        payment.getPayTime(), "공금", payment.getPaymentAccount(), Payer.SHAREDFUND, PaymentType.PAYMENT, payment.getCurrency());
            }
            return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                    payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount(), Payer.INDIVIDUAL, PaymentType.PAYMENT, payment.getCurrency());
        }).toList();
    }

    // 날짜별 sharedFund를 가져오는 메서드
    public List<PaymentSimpleResponseDto> getSharedFundByTravelIdAndDate(Travel tv, LocalDateTime localDateTime, PaymentType paymentType) {
        List<Payment> pmList = paymentRepository.findAllByTravelAndTypeAndPayTimeBetweenAndIsActiveTrue(tv, paymentType, localDateTime, localDateTime.plusDays(1));
        return pmList.stream().map(payment -> {
            if (payment.getType() == PaymentType.SHAREDFUND) {
                return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), "공금 채우기",
                        payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount(), Payer.INDIVIDUAL, PaymentType.SHAREDFUND, payment.getCurrency());
            }
            return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                    payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount(), Payer.INDIVIDUAL, PaymentType.PAYMENT, payment.getCurrency());
        }).toList();

    }


    // 여행에 있는 Payment를 type에 따라 가져오는 메서드
    public List<PaymentSimpleResponseDto> getPaymentByTypeAndTravelId(Travel tv, PaymentType paymentType) {
        List<Payment> pmList = paymentRepository.findAllByTravelAndTypeAndIsActiveTrue(tv, paymentType);
        return pmList.stream().map(payment ->
                new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                        payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount(),
                        Payer.INDIVIDUAL, PaymentType.PAYMENT, payment.getCurrency())).toList();
    }

    // 날짜별 sharedFund와 Payment를 모두 가져오는 메서드
    public List<PaymentSimpleResponseDto> getPaymentAndSharedFundByTravelId(Travel tv, LocalDateTime localDateTime, PaymentType paymentType) {
        List<Payment> pmList = paymentRepository.findAllByTravelAndTypeInAndPayTimeBetweenAndIsActiveTrue(
                tv, List.of(PaymentType.PAYMENT, PaymentType.SHAREDFUND), localDateTime, localDateTime.plusDays(1));
        return pmList.stream().map(payment -> {
            if (payment.getPayerType() == Payer.SHAREDFUND) {

                return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                        payment.getPayTime(), "공금", payment.getPaymentAccount(), Payer.SHAREDFUND, PaymentType.PAYMENT, payment.getCurrency());
            }
            if (payment.getType() == PaymentType.SHAREDFUND) {
                return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), "공금 채우기",
                        payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount(), Payer.INDIVIDUAL, PaymentType.SHAREDFUND, payment.getCurrency());
            }

            return new PaymentSimpleResponseDto(payment.getPaymentId(), payment.getPaymentName(), payment.getCategory().getCategoryName(),
                    payment.getPayTime(), payment.getTravelUser().getTravelNickname(), payment.getPaymentAccount(), Payer.INDIVIDUAL, PaymentType.PAYMENT, payment.getCurrency());
        }).toList();
    }
    // TODO: 금액기록 아이디로 금액기록 정보 받기


    public Payment savePaymentEntity(PaymentRequestDto dto) {
        // isActive는 무조건 true
        Category category = categoryRepository.findById(dto.categoryId()).orElse(null);
        TravelUser tu = travelUserRepository.findByTravelUserIdAndIsActiveTrue(dto.travelUserId()).orElse(null);

        // 환율 가져오는 로직
        LocalDateTime payTime = Formatter.getDateTime(dto.payTime());
        ExchangeRate exchangeRate = exchangeRateUpdateService.getExchangeRate(payTime);


        Travel tv = travelRepository.getReferenceById(dto.travelId());
        Payment payment = Payment.builder()
                .travel(tv)
                .payTime(payTime)
                .category(category)
                .payerType(dto.payerType())
                .currency(dto.currency())
                .paymentAccount(dto.paymentAccount())
                .exchangeRate(exchangeRate.getExchangeRate())
                .travelUser(tu)
                .paymentName(dto.paymentName())
                .paymentMethod(dto.paymentMethod())
                .type(dto.paymentType())
                .build();

        return paymentRepository.save(payment);
    }


    public Settlement saveSettlementEntity(Payment payment, SettlementRequestDto dto, Boolean isPaid) {
        Settlement sm = Settlement.builder()
                .payment(payment)
                .amount(dto.amount())
                .isPaid(isPaid)
                .settlementName(dto.settlementName())
                .build();

        // 정산 저장
        return settlementRepository.save(sm);
    }

    public SettlementUser saveSettlementUserEntity(TravelUser tu, Settlement sm, Long size, Payment payment, Boolean isPaid) {
        Long amount = (payment.getCurrency() != Currency.WON) ? Math.round(sm.getAmount() * payment.getExchangeRate()) : sm.getAmount();
        SettlementUser su = SettlementUser.builder()
                .travelUser(tu)
                .settlement(sm)
                .amount(amount / size)
                .isPaid(isPaid)
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
            double leftAmount;
            if (dto.currency() != Currency.WON) {
                leftAmount = tv.getSharedFund() + payment.getPaymentAccount() * payment.getExchangeRate();
            } else {
                leftAmount = tv.getSharedFund() + payment.getPaymentAccount();
            }
            tv.setSharedFund(Math.round(leftAmount));
            travelRepository.save(tv);
        }

        if (payment.getType().equals(PaymentType.PAYMENT) && payment.getPayerType().equals(Payer.SHAREDFUND)) {
            Travel tv = travelRepository.getReferenceById(dto.travelId());
            double leftAmount;
            if (dto.currency() != Currency.WON) {
                leftAmount = tv.getSharedFund() - payment.getPaymentAccount() * payment.getExchangeRate();
            } else {
                leftAmount = tv.getSharedFund() - payment.getPaymentAccount();
            }
            if (leftAmount < 0) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }
            tv.setSharedFund(Math.round(leftAmount));
        }

        // 정산리스트 저장 로직
        List<SettlementResponseDto> settlementResponse = dto.settlementList().stream().map(settlement -> {
            boolean allPaid = settlement.travelUsers()
                    .stream()
                    .allMatch(SettlementParticipantDto::isPaid);
            Settlement sm = saveSettlementEntity(payment, settlement, allPaid);
            // 정산 유저 저장 로직
            List<SettlementParticipantDto> travelUsersResponse = getTravelUserAndSaveSettlementUsers(payment, settlement, sm);

            return new SettlementResponseDto(sm.getSettlementId(), sm.getPayment().getPaymentId(), sm.getSettlementName(), sm.getAmount(), sm.getIsPaid(), travelUsersResponse);

        }).toList();
        TravelUserResponseDto payerDto;
        if (dto.payerType() == Payer.SHAREDFUND) {
            payerDto = new TravelUserResponseDto(-1L, "", "", Gender.OTHERS, LocalDate.now(), "");
        } else {
            TravelUser tu = payment.getTravelUser();
            User tmpUser = tu.getUser();
            String imageUrl = (tmpUser.getProfileImage() != null) ? tmpUser.getProfileImage().getImageUrl() : "";
            payerDto = new TravelUserResponseDto(tu.getTravelUserId(), user.getNickname(), tu.getTravelNickname(), tmpUser.getGender(), tmpUser.getBirthday(), imageUrl);
        }
        // 이미지 파일이 존재할시
        if (files != null && !files.isEmpty()) {
            //받은 이미지들을 저장한다
            List<Image> images = getSaveImages(user, files);

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
                    payment.getPaymentMethod(), payment.getPaymentName(), payment.getType(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), payment.getCurrency(), settlementResponse, imagesDto);
        }

        // 아미지 파일이 존재 안할시
        return new PaymentResponseDto(payment.getTravel().getTravelId(), payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(), payerDto,
                payment.getPaymentMethod(), payment.getPaymentName(), payment.getType(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), payment.getCurrency(), settlementResponse, new ArrayList<>());

    }

    private List<Image> getSaveImages(User user, List<MultipartFile> files) {
        return imageService.saveImages(user, files);
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
            boolean allPaid = settlement.travelUsers()
                    .stream()
                    .allMatch(SettlementParticipantDto::isPaid);
            Settlement st = saveSettlementEntity(payment, settlement, allPaid);

            // 정산 유저 저장 로직
            List<SettlementParticipantDto> travelUsersResponse = getTravelUserAndSaveSettlementUsers(payment, settlement, st);

            return new SettlementResponseDto(st.getSettlementId(), payment.getPaymentId(), st.getSettlementName(), st.getAmount(), st.getIsPaid(), travelUsersResponse);
        }).toList();
    }

    private List<SettlementParticipantDto> getTravelUserAndSaveSettlementUsers(Payment payment, SettlementRequestDto settlement, Settlement savedSettlement) {
        return settlement.travelUsers().stream().map(travelUser -> {
            TravelUser tu = travelUserRepository.getReferenceById(travelUser.travelUserId());
            SettlementUser smu = saveSettlementUserEntity(tu, savedSettlement, (long) settlement.travelUsers().size(), payment, travelUser.isPaid());
            return new SettlementParticipantDto(tu.getTravelUserId(), tu.getTravelNickname(), smu.getIsPaid());
        }).toList();
    }

    // 사진을 제외한 금액기록을 수정할시
    // Todo: 이미지 처리 해야하는데.. 지금 맨 처음 금액기록을 생성할 때는 이미지랑 금액기록이랑 한번에 보내는데 수정할때도 동일한 방식을 사용하면 시간이 너무 오래 걸릴거같아서
    public void updatePayment(User user, PaymentRequestDto dto, List<MultipartFile> files) {
        Payment pm = paymentRepository.getReferenceById(dto.paymentId());
        Travel tv = travelRepository.getReferenceById(dto.travelId());
        // 결제 방법 (개인, 공금)
        long shared = getShared(dto, pm, tv);


        // 3) 잔액 반영 저장
        tv.setSharedFund(shared);
        travelRepository.save(tv);

        List<Settlement> settlements = settlementRepository.findByPaymentAndIsActiveTrue(pm);
        log.info("PRESETTLEMENTS");
        settlements.forEach(settlement -> {
            log.info(String.valueOf(settlement.getSettlementId()));
        });
        // 기존 settlement들 삭제 후 다시 생성 그리고 settlementResponseDto 반환
        List<SettlementResponseDto> updatedSettlements = updateSettlement(pm, settlements, dto.settlementList());

        Category category = null;
        if (dto.paymentType() != PaymentType.SHAREDFUND) {
            category = categoryRepository.getReferenceById(dto.categoryId());
        }

        TravelUser newTravelUser = null;
        if (dto.payerType() != Payer.SHAREDFUND) {
            newTravelUser = travelUserRepository.getReferenceById(dto.travelUserId());
        }
        pm.setTravelUser(newTravelUser);
        pm.setCategory(category); // 카테고리
        pm.setPayerType(dto.payerType()); // 개인 or 공금
        pm.setCurrency(dto.currency());
        pm.setPaymentMethod(dto.paymentMethod()); // 카드 or 현금
        pm.setPaymentAccount(dto.paymentAccount()); // 돈 총합
        pm.setExchangeRate(exchangeRateUpdateService.getExchangeRate(Formatter.getDateTime(dto.payTime())).getExchangeRate()); // 환율
        pm.setPaymentName(dto.paymentName()); // 금액기록 이름
        pm.setPayTime(Formatter.getDateTime(dto.payTime())); // 금액기록 시간
        Payment newPayment = paymentRepository.save(pm);

        dto.removeImageIds().forEach(paymentImageRepository::deleteById);

        // 이미지 파일이 존재할시
        if (files != null && !files.isEmpty()) {
            //받은 이미지들을 저장한다
            List<Image> images = getSaveImages(user, files);

            //이미지 리스트를 하나하나 변환하여 DTO List로 저장한다
            List<PaymentImageDto> imagesDto = images.stream().map(
                    image -> {
                        PaymentImage pi = PaymentImage.builder()
                                .image(image)
                                .payment(newPayment)
                                .build();
                        PaymentImage tmp = paymentImageRepository.save(pi);

                        return new PaymentImageDto(tmp.getPaymentImageId(), image.getImageUrl());
                    }
            ).toList();
        }
    }

    private static long getShared(PaymentRequestDto dto, Payment pm, Travel tv) {
        Payer prevPayer = pm.getPayerType();
        Payer nextPayer = dto.payerType();
        // 공금을 채운건지 아닌지
        PaymentType prevType = pm.getType();
        PaymentType nextType = dto.paymentType();

        long shared = tv.getSharedFund();                 // 공금 잔액
        long prevAmt = pm.getPaymentAccount();            // 이전 결제/충전 금액
        long nextAmt = dto.paymentAccount();              // 새 결제/충전 금액

        // 0) 입력 방어: 동시에 공금 결제 + 공금 충전이면 비정상으로 간주 (원하면 제거 가능)
//        if (nextPayer == Payer.SHAREDFUND && nextType == PaymentType.SHAREDFUND) {
//            throw new IllegalStateException("공금 결제와 공금 충전을 동시에 처리할 수 없습니다.");
//        }

        // 1) 이전 기록 효과 되돌리기
        // 기존 결제가 공금으로 결제된 경우: 공금에서 빠졌던 금액을 되돌려 더한다.
        if (prevPayer == Payer.SHAREDFUND) {
            shared += prevAmt;
        }
        // 기존 기록이 공금 충전인 경우: 충전되어 더해졌던 금액을 되돌려 뺀다.
        if (prevType == PaymentType.SHAREDFUND) {
            shared -= prevAmt;
        }

        // 2) 새 기록 적용
        // 새로 공금 결제로 바뀐 경우: 공금에서 차감 (잔액 체크)
        if (nextPayer == Payer.SHAREDFUND) {
            if (shared < nextAmt) {
                throw new IllegalStateException("잔액이 충분하지 않습니다.(공금이 음수)");
            }
            shared -= nextAmt;
        }
        // 새로 공금 충전으로 바뀐 경우: 공금에 가산
        if (nextType == PaymentType.SHAREDFUND) {
            if (shared + nextAmt < 0) {
                throw new IllegalStateException("잔액이 충분하지 않습니다.(공금이 음수)");
            }
            shared += nextAmt;
        }
        return shared;
    }

    // 기존 금액기록에 사진을 추가할시
    public void updatePaymentImages(User user, Long paymentId, List<MultipartFile> files) {
        //받은 이미지들을 저장한다
        List<Image> images = getSaveImages(user, files);
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
            List<SettlementUser> su = settlementUserRepository.findAllBySettlementAndIsActiveTrue(stm);
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
        List<SettlementUser> su = settlementUserRepository.findAllBySettlementAndIsActiveTrue(settlement);

        su.forEach(stmu -> {
            log.info("DELETED SETTLEMENTUSER: {}", stmu.getSettlementUserId());
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

        // 금액기록 ID로 paymentImage 찾아오기
        List<PaymentImage> pi = paymentImageRepository.findByPayment_PaymentId(pm.getPaymentId());
        // 돌면서 이미지 soft delete
        pi.forEach(image -> {
            deletePaymentImage(image.getPaymentImageId());
        });

        // 마찬가지로 정산들 찾아와서 soft delete
        List<Settlement> st = settlementRepository.findByPayment_PaymentIdAndIsActiveTrue(pm.getPaymentId());
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
        List<Settlement> stList = settlementRepository.findByPayment_PaymentIdAndIsActiveTrue(paymentId);
        //settlement 리스트 돌면서 PaymentResponseDto에 들어갈 SettlementResponseDto 만들기
        List<SettlementResponseDto> stResponseDtoList = stList.stream().map(settlement -> {
            List<SettlementUser> stuList = settlementUserRepository.findBySettlementAndIsActiveTrue(settlement);
            List<SettlementParticipantDto> tuDtoList = stuList.stream().map(stu -> {
                TravelUser tu = stu.getTravelUser();
                User user = tu.getUser();

                return new SettlementParticipantDto(tu.getTravelUserId(), tu.getTravelNickname(), stu.getIsPaid());
            }).toList();
            return new SettlementResponseDto(settlement.getSettlementId(), pm.getPaymentId(), settlement.getSettlementName(), settlement.getAmount(),
                    settlement.getIsPaid(), tuDtoList);
        }).toList();

        //PaymentImage에 존재하는 이미지 PayemntId로 가져오기
        List<PaymentImage> pmiList = paymentImageRepository.findByPayment(pm);
        //PaymentImage 리스트 돌면서 PaymentResponseDto에 들어갈 PaymentImageDtoList 만들기
        List<PaymentImageDto> pmimageDtoList = pmiList.stream().map(paymentImage -> new PaymentImageDto(paymentImage.getPaymentImageId(),
                paymentImage.getImage().getImageUrl())).toList();

        TravelUser tu = pm.getTravelUser();
        TravelUserResponseDto payerDto;
        if (tu != null) {
            User user = tu.getUser();
            String imageUrl = (user.getProfileImage() != null) ? user.getProfileImage().getImageUrl() : "";
            payerDto = new TravelUserResponseDto(tu.getTravelUserId(), user.getNickname(), tu.getTravelNickname(), user.getGender(), user.getBirthday(), imageUrl);
        } else {
            payerDto = new TravelUserResponseDto(-1L, "공금", "공금", Gender.OTHERS, LocalDate.now(), "");

        }

        Category category = null;
        if (!pm.getType().equals(PaymentType.SHAREDFUND)) {
            category = pm.getCategory();
        }
        return new PaymentResponseDto(pm.getTravel().getTravelId(), pm.getPaymentId(), (category != null) ? category.getCategoryId() : -1, (category != null) ? category.getCategoryName() : "공금채우기",
                pm.getPayerType(), payerDto, pm.getPaymentMethod(), pm.getPaymentName(), pm.getType(), pm.getExchangeRate(), pm.getPayTime(),
                pm.getPaymentAccount(), pm.getCurrency(), stResponseDtoList, pmimageDtoList);
    }

    public SettlementResultResponseDto getSettlement(Travel tv, Boolean includePreUseAmount, Boolean includeSharedFund, Boolean includeRecordedAmount, String startAt, String endAt) {
        LocalDateTime startDateTime = Formatter.getDateTime(startAt);
        LocalDateTime endDateTime = Formatter.getDateTime(endAt);
        List<PaymentType> paymentOptionList = new ArrayList<>();

        if (includePreUseAmount) {paymentOptionList.add(PaymentType.PREPAYMENT);}
        if (includeSharedFund) {paymentOptionList.add(PaymentType.SHAREDFUND);}
        if (includeRecordedAmount) {paymentOptionList.add(PaymentType.PAYMENT);}


        List<Settlement> settlementList = settlementRepository.findSettlementByOptions(tv, paymentOptionList, startDateTime, endDateTime);
        List<TravelUser> travelUserList = travelUserRepository.findByTravelAndIsActiveTrue(tv);
        int travelUserCount = travelUserList.size();
        Long[][] totalSettlementAmount = new Long[travelUserCount][travelUserCount];

        for (int i = 0; i < travelUserCount; i++) {
            for (int j = 0; j < travelUserCount; j++) {
                totalSettlementAmount[i][j] = 0L;
            }
        }

        Map<Long, Integer> hashMap = new HashMap<>();
        for (int i = 0; i < travelUserList.size(); i++) {
            hashMap.put(travelUserList.get(i).getTravelUserId(), i);
        }


        List<SettlementPaymentTypeDto> paymentTypeList = List.of(
                getSettlementPaymentTypeDto(settlementList, PaymentType.PREPAYMENT, totalSettlementAmount, hashMap),
                getSettlementPaymentTypeDto(settlementList, PaymentType.PAYMENT, totalSettlementAmount, hashMap),
                getSettlementPaymentTypeDto(settlementList, PaymentType.SHAREDFUND, totalSettlementAmount, hashMap)
        );
        List<SettlementResponseUserDetailDto> settlementResponseUserDetailDtoList = new ArrayList<>();

        hashMap.forEach((travelUserId, index) -> {
            String receiverNickname = travelUserRepository.getReferenceById(travelUserId).getTravelNickname();
            List<SettlementUserDetailsDto> settlementUserDetailsDto = new ArrayList<>();
            for (int i = 0; i < travelUserCount; i++){
                if (i == index) continue;
                String senderNickname = travelUserList.get(i).getTravelNickname();
                settlementUserDetailsDto.add(new SettlementUserDetailsDto(senderNickname, totalSettlementAmount[index][i]));
            }
            settlementResponseUserDetailDtoList.add(new SettlementResponseUserDetailDto(receiverNickname, settlementUserDetailsDto));

        });

        return new SettlementResultResponseDto(settlementResponseUserDetailDtoList, paymentTypeList);
    }


    private SettlementPaymentTypeDto getSettlementPaymentTypeDto(List<Settlement> settlementList, PaymentType paymentType, Long[][] totalSettlementAmount, Map<Long, Integer> hashMap) {
        List<Settlement> prePaymentList = settlementList.stream().filter(tmp -> tmp.getPayment().getType() == paymentType).toList();

        List<SettlementResponseUserDetailDto> responseUserDto = prePaymentList.stream().filter(tmp -> tmp.getPayment().getPayerType() != Payer.SHAREDFUND) // 건너뛰기
                .map(tmp -> {
            Payment payment = tmp.getPayment();
            List <SettlementUser> stuList = settlementUserRepository.findBySettlementAndIsActiveTrue(tmp);
            Integer receiverIndex = hashMap.get(payment.getTravelUser().getTravelUserId());
            List<SettlementUserDetailsDto> userDetailsDto = stuList.stream().map(stu -> {
                Integer senderIndex = hashMap.get(stu.getTravelUser().getTravelUserId());
                Long senderAmount = stu.getAmount();
                // 정산이 안된것에 대하여
                if (!stu.getIsPaid()) {
                    // 돈을 보내는사람이 지금 받을돈이 있다면 (그리고 받을돈이 보내야할돈보다 더 많다면)
                    if (totalSettlementAmount[senderIndex][receiverIndex] - senderAmount >= 0) {
                        totalSettlementAmount[senderIndex][receiverIndex] -= senderAmount;
                    }
                    // 돈을 보내는 사람이 지금 받을돈이 없거나, 줘야할 돈 보다 적다면
                    else {
                        senderAmount -= totalSettlementAmount[senderIndex][receiverIndex];
                        totalSettlementAmount[senderIndex][receiverIndex] = 0L;
                        totalSettlementAmount[receiverIndex][senderIndex] += senderAmount;
                    }
                }


                return new SettlementUserDetailsDto(stu.getTravelUser().getTravelNickname(), payment.getPaymentId(), payment.getPaymentName(), stu.getAmount(), stu.getIsPaid(), payment.getPayTime());
            }
            ).toList();
            return new SettlementResponseUserDetailDto(payment.getTravelUser().getTravelNickname(), userDetailsDto);
        }).toList();

        return  new SettlementPaymentTypeDto(paymentType, responseUserDto);
    }

}
