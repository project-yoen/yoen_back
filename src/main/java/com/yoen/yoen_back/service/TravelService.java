package com.yoen.yoen_back.service;

import com.yoen.yoen_back.entity.payment.Payment;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelRecord;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.payment.PaymentRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<Travel> getAllTravels() {
        return travelRepository.findAll();
    }

    public List<TravelRecord> getAllTravelRecordsByTravelId(Long travelId) {
        return travelRecordRepository.findByTravel_TravelId(travelId);
    }

    public List<Payment> getAllPaymentsByTravelId(Long travelId) {
        return paymentRepository.findByTravel_TravelId(travelId);
    }

    public void setTravel (Long userId, Travel travel) {
        Travel tv = travelRepository.save(travel);
        Optional<User> user = userRepository.findById(userId);

        user.ifPresent(user1 -> {
            TravelUser tu = TravelUser.builder().travel(tv).user(user1).role().build();
            travelUserRepository.save(tu);
        });
    }
}
