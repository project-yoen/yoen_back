package com.yoen.yoen_back.service;

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

    DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

    public TravelRecord setTravelRecord (Long userId, TravelRecordRequestDto travelRecordRequestDto) {
        Travel tv = travelRepository.getReferenceById(travelRecordRequestDto.travelId());
        TravelUser tu = travelUserRepository.getReferenceById(travelRecordRequestDto.travelUserId());
        LocalDateTime parsedTime = LocalDateTime.parse(travelRecordRequestDto.recordTime(), formatter);
        TravelRecord travelRecord = TravelRecord.builder()
                .travel(tv)
                .travelUser(tu)
                .title(travelRecordRequestDto.title())
                .content(travelRecordRequestDto.content())
                .recordTime(parsedTime)
                .build();
        return travelRecordRepository.save(travelRecord);
    }

    public Payment setPayment (Long userId, PaymentRequestDto paymentRequestDto) {
        Travel travel = travelRepository.getReferenceById(paymentRequestDto.travelId());
        LocalDateTime parsedTime = LocalDateTime.parse(paymentRequestDto.payTime(), formatter);
        Payment payment = Payment.builder().
                travel(travel).
                payTime(parsedTime).
                category(paymentRequestDto.category()).
                payerType(paymentRequestDto.payerType()).
                paymentAccount(paymentRequestDto.paymentAccount()).
                build();

        return paymentRepository.save(payment);
    }
}
