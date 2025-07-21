package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.*;
import com.yoen.yoen_back.entity.Category;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.image.PaymentImage;
import com.yoen.yoen_back.entity.image.TravelRecordImage;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.*;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.CategoryRepository;
import com.yoen.yoen_back.repository.image.PaymentImageRepository;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
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

    private final static String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();
    private final TravelDestinationRepository travelDestinationRepository;
    private final DestinationRepository destinationRepository;
    private final PaymentImageRepository paymentImageRepository;


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
    public Travel createTravel(TravelRequestDto dto) {
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
            TravelDestination td = TravelDestination.builder()
                    .travel(tv)
                    .destination(dt)
                    .build();
            travelDestinationRepository.save(td);
        });
    }

    @Transactional
    public Travel setTravel(User user, TravelRequestDto dto) {
        Travel tv = createTravel(dto);
        createTravelUser(tv, user);
        createTravelDestination(tv, dto.destinationIds());
        return tv;
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

    @Transactional
    public TravelRecordResponseDto setTravelRecord(User user, TravelRecordRequestDto dto, List<MultipartFile> files) {
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
                    .travelrecord(travelRecord)
                    .build();
            TravelRecordImage tmp = travelRecordImageRepository.save(tri); // travelRecordImage 레포에 image들 저장
            imagesDto.add(new TravelRecordImageDto(tmp.getTravelRecordImageId(), image.getImageId(), image.getImageUrl()));
        });

        return new TravelRecordResponseDto(tr.getTravelRecordId(), tr.getTitle(), tr.getContent(), tr.getRecordTime(), imagesDto);
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
        Double exchangeRate = 10.2;


        Travel tv = travelRepository.getReferenceById(dto.travelId());
        Payment payment = Payment.builder().
                travel(tv).
                payTime(payTime).
                category(category).
                payerType(dto.payerType()).
                paymentAccount(dto.paymentAccount()).
                exchangeRate(exchangeRate).
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
                .orElseThrow(() -> new IllegalStateException("유효하지 않은 코드입니다."));
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
        return new PaymentResponseDto(payment.getPaymentId(), payment.getCategory().getCategoryId(), payment.getCategory().getCategoryName(), payment.getPayerType(),
                payment.getPaymentMethod(), payment.getPaymentName(), payment.getExchangeRate(), payment.getPayTime(), payment.getPaymentAccount(), imagesDto);
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
