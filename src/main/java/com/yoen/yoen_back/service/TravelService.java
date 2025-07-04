package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.PaymentRequestDto;
import com.yoen.yoen_back.dto.TravelRecordRequestDto;
import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public List<Travel> getAllTravels() {
        return travelRepository.findAll();
    }

    public List<TravelRecord> getAllTravelRecordsByTravelId(Long travelId) {
        return travelRecordRepository.findByTravel_TravelId(travelId);
    }

    public List<Payment> getAllPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravel_TravelId(travelId);
    }

    // 권한 관련해서 어떻게 입력받을건지 생각
    @Transactional
    public TravelUser setTravel (Long userId, Travel travel) {
        Travel tv = travelRepository.save(travel);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        Role role = Role.Writer;
        TravelUser travelUser = TravelUser.builder()
                .travel(tv)
                .user(user)
                .role(role)
                .build();
        return travelUserRepository.save(travelUser);
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
            Long code = getUniqueJoinCode();
            travelJoinCodeRedisDao.saveBidirectionalMapping(code, travelId);
        }
        return travelJoinCodeRedisDao.getCodeByTravelId(travelId)
                .orElseThrow(() -> new IllegalStateException("해당 여행의 참여 코드가 존재하지 않습니다."));
    }

    public Long getUniqueJoinCode() {
        return 123123L;
    }

}
