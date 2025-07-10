package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.DestinationDto;
import com.yoen.yoen_back.dto.PaymentRequestDto;
import com.yoen.yoen_back.dto.TravelRecordRequestDto;
import com.yoen.yoen_back.dto.TravelRequestDto;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.*;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.travel.*;
import com.yoen.yoen_back.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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

    private final static String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();
    private final TravelDestinationRepository travelDestinationRepository;
    private final DestinationRepository destinationRepository;


    public List<Travel> getAllTravels() {
        return travelRepository.findAll();
    }

    public List<TravelRecord> getAllTravelRecordsByTravelId(Long travelId) {
        return travelRecordRepository.findByTravel_TravelId(travelId);
    }

    public List<Payment> getAllPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravel_TravelId(travelId);
    }

    // todo: 여행 객체를 생성 -> 여행 객체와 유저를 매핑 -> 여행 객체에 여행_목적지 객체 매핑 -> 함수 3개를 모은 setTravel 선언
    // 여행 객체 생성
    public Travel createTravel (TravelRequestDto dto) {
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
    public TravelUser createTravelUser (Travel tv, User user) {
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
    public void createTravelDestination (Travel tv, List<Long> destinationIds) {
        destinationIds.forEach(destinationId -> {
            Destination dt = destinationRepository.findById(destinationId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 목적지 ID: " + destinationId));
            TravelDestination td = TravelDestination.builder()
                    .travel(tv)
                    .destination(dt)
                    .build();
            travelDestinationRepository.save(td);
        });
    }
    @Transactional
    public Travel setTravel(User user, TravelRequestDto dto){
        Travel tv = createTravel(dto);
        createTravelUser(tv, user);
        createTravelDestination(tv, dto.destinationIds());
        return tv;
    }
    public Destination createDestination (DestinationDto dto) {
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





    public List<Destination> createDestinations (List<DestinationDto> dtos) {
        dtos.forEach(this::createDestination);
        return destinationRepository.findAll();
    }

    public TravelRecord setTravelRecord (Long userId, TravelRecordRequestDto dto) {
        Travel tv = travelRepository.getReferenceById(dto.travelId());
        TravelUser tu = travelUserRepository.getReferenceById(dto.travelUserId());
        TravelRecord travelRecord = TravelRecord.builder()
                .travel(tv)
                .travelUser(tu)
                .title(dto.title())
                .content(dto.content())
                .recordTime(Formatter.getDateTime(dto.recordTime()))
                .build();
        return travelRecordRepository.save(travelRecord);
    }




    public Payment setPayment (Long userId, PaymentRequestDto dto) {
        Travel travel = travelRepository.getReferenceById(dto.travelId());
        Payment payment = Payment.builder().
                travel(travel).
                payTime(Formatter.getDateTime(dto.payTime())).
                category(dto.category()).
                payerType(dto.payerType()).
                paymentAccount(dto.paymentAccount()).
                build();

        return paymentRepository.save(payment);
    }


    public String getJoinCode (User user, Long travelId) {
        if(!travelJoinCodeRedisDao.existsTravelId(travelId)){
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
}
