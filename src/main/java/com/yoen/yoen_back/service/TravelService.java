package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.entity.InvalidJoinCodeException;
import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.*;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.ExchangeRate;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.PaymentImage;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.payment.Settlement;
import com.yoen.yoen_back.entity.payment.SettlementUser;
import com.yoen.yoen_back.entity.travel.*;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.payment.SettlementRepository;
import com.yoen.yoen_back.repository.payment.SettlementUserRepository;
import com.yoen.yoen_back.repository.travel.*;
import com.yoen.yoen_back.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelRepository travelRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final PaymentRepository paymentRepository;
    private final TravelUserRepository travelUserRepository;
    private final UserRepository userRepository;
    private final TravelJoinCodeRedisDao travelJoinCodeRedisDao;
    private final ImageService imageService;
    private final TravelRecordImageRepository travelRecordImageRepository;
    private final CategoryRepository categoryRepository;

    private final TravelDestinationRepository travelDestinationRepository;
    private final DestinationRepository destinationRepository;
    private final PaymentImageRepository paymentImageRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementUserRepository settlementUserRepository;

    private final ExchangeRateUpdateService exchangeRateUpdateService;


    private final SecureRandom random = new SecureRandom();
    private final static String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final TravelJoinRequestRepository travelJoinRequestRepository;


    public List<Travel> getAllTravels() {
        return travelRepository.findAll();
    }

    public List<TravelRecord> getAllTravelRecordsByTravelId(Long travelId) {
        return travelRecordRepository.findByTravel_TravelIdAndIsActiveTrue(travelId);
    }

    public List<Payment> getAllPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravel_TravelIdAndIsActiveTrue(travelId);
    }

    //Todo 여행을 삭제할 때 관련된 모든 테이블의 레코드를 비활성화 해야 할까?
    @Transactional
    public void deleteTravel(Long travelId) {
//        List<TravelRecord> tr = travelRecordRepository.findByTravel_TravelId(travelId);
//        List<TravelUser> tu = travelUserRepository.findByTravel_TravelId(travelId);
//        List<TravelDestination> td = travelDestinationRepository.findByTravel_TravelId(travelId);
//        List<Payment> pay= paymentRepository.findByTravel_TravelId(travelId);
//        List<PrePayment> prePayment = paymentRepository.findByTravel_TravelId(travelId);
        travelRepository.deleteById(travelId);
    }

    // todo: 여행 객체를 생성 -> 여행 객체와 유저를 매핑 -> 여행 객체에 여행_목적지 객체 매핑 -> 함수 3개를 모은 setTravel 선언
    // 여행 객체 생성
    public Travel setTravel(TravelRequestDto dto) {
        // save할 여행 객체 생성
        Travel tv = Travel.builder()
                .nation(dto.nation())
                .travelName(dto.travelName())
                .numOfPeople(dto.numOfPeople())
                .startDate(Formatter.getDate(dto.startDate()))
                .endDate(Formatter.getDate(dto.endDate()))
                .build();

        return travelRepository.save(tv);
    }


    //여행_유저 객체 매핑
    public TravelUser createTravelUser(Travel tv, User user) {
        Role role = Role.Writer;
        TravelUser tu = TravelUser.builder()
                .travel(tv)
                .user(user)
                .role(role)
                .build();
        return travelUserRepository.save(tu);
    }

    //여행_목적지 객체 매핑
    @Transactional
    public void createTravelDestination(Travel tv, List<Long> destinationIds) {
        destinationIds.forEach(destinationId -> {
            Destination dt = destinationRepository.findByDestinationIdAndIsActiveTrue(destinationId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 목적지 ID: " + destinationId));
            List<TravelDestination> tdl = travelDestinationRepository.findByTravel_TravelId(tv.getTravelId());

            // TravelDestination 이었던 것들 전부 비활성화
            tdl.forEach(travelDestination -> {
                travelDestination.setIsActive(false);
                travelDestinationRepository.save(travelDestination);
            });

            TravelDestination td = TravelDestination.builder()
                    .travel(tv)
                    .destination(dt)
                    .build();
            travelDestinationRepository.save(td);
        });
    }

    @Transactional
    public Travel createTravel(User user, TravelRequestDto dto) {
        Travel tv = setTravel(dto);
        createTravelUser(tv, user);
        createTravelDestination(tv, dto.destinationIds());
        return tv;
    }

    @Transactional
    public Travel updateTravel(TravelRequestDto dto) {
        // save할 여행 객체 생성
        Travel tv = travelRepository.getReferenceById(dto.travelId());
        tv.setTravelName(dto.travelName());
        tv.setNumOfPeople(dto.numOfPeople());
        tv.setStartDate(Formatter.getDate(dto.startDate()));
        tv.setEndDate(Formatter.getDate(dto.endDate()));
        tv.setNation(dto.nation());
        createTravelDestination(tv, dto.destinationIds());

        return travelRepository.save(tv);
    }

    public Destination createDestination(DestinationDto dto) {
        Destination dt = Destination.builder()
                .name(dto.name())
                .nation(dto.nation())
                .build();
        return destinationRepository.save(dt);
    }


    public List<TravelUser> getAllTravelUser() {
        return travelUserRepository.findAll();
    }

    public List<TravelDestination> getAllTravelDestination() {
        return travelDestinationRepository.findAll();
    }


    public List<Destination> createDestinations(List<DestinationDto> dtos) {
        dtos.forEach(this::createDestination);
        return destinationRepository.findAll();
    }

    // 여행 기록 추가 할시 (한번에 이미지까지 저장) (생성)
    @Transactional
    public TravelRecordResponseDto createTravelRecord(User user, TravelRecordRequestDto dto, List<MultipartFile> files) {
        List<Image> images = imageService.saveImages(user, files); // 클라우드에 업로드 및 image 레포지토리에 저장
        Travel tv = travelRepository.getReferenceById(dto.travelId());
        TravelUser tu = travelUserRepository.getReferenceById(dto.travelUserId());
        TravelRecord travelRecord = TravelRecord.builder()
                .travel(tv)
                .travelUser(tu)
                .title(dto.title())
                .content(dto.content())
                .recordTime(Formatter.getDateTime(dto.recordTime()))
                .build();
        // 먼저 저장
        TravelRecord tr = travelRecordRepository.save(travelRecord);

        // TODO: 여기서부턴 좀 수정이 있어야할거 같음 지금 이미지를 불러다가 응답하는게 좀 복잡함 (왜 세개로 분리했는지 고민)
        List<TravelRecordImageDto> imagesDto = new ArrayList<>();
        images.forEach(image -> {
            TravelRecordImage tri = TravelRecordImage.builder()
                    .image(image)
                    .travelRecord(travelRecord)
                    .build();
            TravelRecordImage tmp = travelRecordImageRepository.save(tri); // travelRecordImage 레포에 image들 저장
            imagesDto.add(new TravelRecordImageDto(tmp.getTravelRecordImageId(), image.getImageId(), image.getImageUrl()));
        });

        return new TravelRecordResponseDto(tr.getTravelRecordId(), tr.getTitle(), tr.getContent(), tr.getRecordTime(), imagesDto);
    }

    // 사진을 제외한 여행기록을 수정할시 (수정)
    @Transactional
    public TravelRecordResponseDto updateTravelRecord(User user, TravelRecordRequestDto dto, List<MultipartFile> files) {
        TravelRecord tr = travelRecordRepository.getReferenceById(dto.travelRecordId());

        // 수정
        tr.setTitle(dto.title());
        tr.setContent(dto.content());
        tr.setRecordTime(Formatter.getDateTime(dto.recordTime()));
        travelRecordRepository.save(tr);

        return new TravelRecordResponseDto(tr.getTravelRecordId(), tr.getTitle(), tr.getContent(), tr.getRecordTime(), new ArrayList<>());
    }

    // 기존 여행기록에 사진들을 추가할시 (수정)
    public void updateTravelRecordImages(User user, Long paymentId, List<MultipartFile> files) {
        //받은 이미지들을 저장한다
        List<Image> images = imageService.saveImages(user, files);
        TravelRecord tr = travelRecordRepository.getReferenceById(paymentId);
        //이미지 리스트를 하나하나 변환하여 DTO List로 저장한다
        List<TravelRecordImageDto> imagesDto = images.stream().map(
                image -> {
                    TravelRecordImage tri = TravelRecordImage.builder()
                            .image(image)
                            .travelRecord(tr)
                            .build();
                    TravelRecordImage tmp = travelRecordImageRepository.save(tri);

                    return new TravelRecordImageDto(tmp.getTravelRecordImageId(), image.getImageId(), image.getImageUrl());
                }
        ).toList();
    }

    // 기존 여행기록에서 사진을 삭제할시 (수정)
    public void deleteTravelRecordImage(Long travelRecordImageId) {
        Optional<TravelRecordImage> paymentImage = travelRecordImageRepository.findByTravelRecordImageIdAndIsActiveTrue(travelRecordImageId);
        paymentImage.ifPresent(image -> {
            // 사진 모집단 삭제 (클라우드 삭제)
            Image img = image.getImage();
            imageService.deleteImage(img.getImageId());

            // paymentImage 삭제
            image.setIsActive(false);
            travelRecordImageRepository.save(image);
        });
    }

    // 여행기록 삭제 (삭제)
    public void deleteTravelRecord(Long travelRecordId) {
        TravelRecord travelRecord = travelRecordRepository.getReferenceById(travelRecordId);
        // 관련 이미지 가져오기
        List<TravelRecordImage> images = travelRecordImageRepository.findAllByTravelRecord_TravelRecordId(travelRecordId);
        // 관련 이미지 삭제
        images.forEach(image -> {
            // 이미지 모집단 중 삭제
            deleteTravelRecordImage(image.getTravelRecordImageId());
        });

        // 여행 기록 삭제
        travelRecord.setIsActive(false);
        travelRecordRepository.save(travelRecord);
    }

    // 여행에 대한 여행 유저 반환하는 함수
    public TravelUser getTravelUser(User user, Long travelId) {
        return travelUserRepository.findByTravel_TravelIdAndUser(travelId, user)
                .orElseThrow(() -> new RuntimeException("해당 유저의 TravelUser가 존재하지 않습니다."));
    }

    public Payment setPayment(PaymentRequestDto dto) {
        // isActive는 무조건 true
        Category category = categoryRepository.getReferenceById(dto.categoryId());
        // 환율 가져오는 로직
        LocalDateTime payTime = Formatter.getDateTime(dto.payTime());
        ExchangeRate exchangeRate = exchangeRateUpdateService.getExchangeRate(payTime);

        Travel tv = travelRepository.getReferenceById(dto.travelId());
        Payment payment = Payment.builder().
                travel(tv).
                payTime(payTime).
                category(category).
                payerType(dto.payerType()).
                paymentAccount(dto.paymentAccount()).
                exchangeRate(exchangeRate.getExchangeRate()).
                paymentName(dto.paymentName())
                .paymentMethod(dto.paymentMethod())
                .build();

        return paymentRepository.save(payment);
    }


    public String getJoinCode(User user, Long travelId) {
        if (!travelJoinCodeRedisDao.existsTravelId(travelId)) {
            String code = getUniqueJoinCode(6);
            travelJoinCodeRedisDao.saveBidirectionalMapping(code, travelId);
        }
        return travelJoinCodeRedisDao.getCodeByTravelId(travelId)
                .orElseThrow(() -> new IllegalStateException("해당 여행의 참여 코드가 존재하지 않습니다."));
    }

    public void requestToJoinTravel(User user, String code) {
        String travelId = travelJoinCodeRedisDao.getTravelIdByCode(code).orElseThrow(() -> new InvalidJoinCodeException("유효하지 않은 코드입니다."));
        Long tl = Long.parseLong(travelId);
        TravelJoinRequest tjr = TravelJoinRequest.builder()
                .user(user)
                .travel(travelRepository.getReferenceById(tl))
                .isAccepted(false)
                .build();
        travelJoinRequestRepository.save(tjr);
    }

    public String getUniqueJoinCode(int length) {
        String code;
        do {
            StringBuilder codeBuilder = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = random.nextInt(CHARACTERS.length());
                codeBuilder.append(CHARACTERS.charAt(index));
            }
            code = codeBuilder.toString();
        } while (travelJoinCodeRedisDao.existsCode(code));
        return code;
    }

    public LocalDateTime getCodeExpiredTime(String code) {
        return travelJoinCodeRedisDao.getExpirationTime(code)
                .orElseThrow(() -> new InvalidJoinCodeException("유효하지 않은 코드입니다."));
    }

    @Transactional
    public PaymentResponseDto createTravelPayment(User user, PaymentRequestDto dto, List<MultipartFile> files) {
        //받은 이미지들을 저장한다
        List<Image> images = imageService.saveImages(user, files);
        //금액기록을 빌더 패턴으로 생성하여 저장한다
        Payment payment = setPayment(dto);
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

        // 정산리스트 저장 로직
        List<Settlement> settlementList = dto.settlementList().stream().map(settlement -> {
            Settlement sm = Settlement.builder()
                    .payment(payment)
                    .amount(settlement.amount())
                    .isPaid(settlement.isPaid())
                    .settlementName(settlement.settlementName())
                    .build();

            // 정산 저장
            Settlement savedSettlement = settlementRepository.save(sm);

            // 정산 유저 저장 로직
            settlement.travelUsers().forEach(travelUser -> {
                TravelUser tu = travelUserRepository.getReferenceById(travelUser);
                SettlementUser su = SettlementUser.builder()
                        .travelUser(tu)
                        .settlement(savedSettlement)
                        .amount(settlement.amount() / settlement.travelUsers().size())
                        .isPaid(settlement.isPaid())
                        .build();

                // 정산 유저 저장
                settlementUserRepository.save(su);
            });
            return settlementRepository.save(sm);
        }).toList();

        return new PaymentResponseDto(payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(),
                payment.getPaymentMethod(), payment.getPaymentName(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), imagesDto);
    }

    // 정산유저 테스트
    public List<SettlementUser> getAllSettlementUsers() {
        return settlementUserRepository.findAll();
    }


    // 사진을 제외한 금액기록을 수정할시
    // Todo: 이미지 처리 해야하는데.. 지금 맨 처음 금액기록을 생성할 때는 이미지랑 금액기록이랑 한번에 보내는데 수정할때도 동일한 방식을 사용하면 시간이 너무 오래 걸릴거같아서
    public PaymentResponseDto updateTravelPayment(PaymentRequestDto dto) {
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
    public void deleteTravelPayment(Long paymentId) {
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

    public CategoryRequestDto createCategory(CategoryRequestDto dto) {
        Category category = Category.builder()
                .categoryName(dto.categoryName())
                .type(dto.categoryType())
                .build();
        categoryRepository.save(category);
        return new CategoryRequestDto(category.getCategoryId(), category.getCategoryName(), category.getType());
    }
}
