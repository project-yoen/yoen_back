package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.travel.TravelRequestDto;
import com.yoen.yoen_back.dto.travel.TravelUserDto;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelRepository travelRepository;
    private final TravelUserRepository travelUserRepository;

    private final CommonService commonService;


    public List<Travel> getAllTravels() {
        return travelRepository.findAll();
    }


    //Todo 여행을 삭제할 때 관련된 모든 테이블의 레코드를 비활성화 해야 할까?
    @Transactional
    public void deleteTravel(Travel tv) {
//        List<TravelRecord> tr = travelRecordRepository.findByTravel_TravelId(travelId);
//        List<TravelUser> tu = travelUserRepository.findByTravel_TravelId(travelId);
//        List<TravelDestination> td = travelDestinationRepository.findByTravel_TravelId(travelId);
//        List<Payment> pay= paymentRepository.findByTravel_TravelId(travelId);
//        List<PrePayment> prePayment = paymentRepository.findByTravel_TravelId(travelId);
        tv.setIsActive(false);
        travelRepository.save(tv);
    }

    // 여행 객체를 저장 -> 여행 객체와 유저를 매핑 -> 여행 객체에 여행_목적지 객체 매핑 -> 함수 3개를 모은 createTravel 선언
    // 여행 객체 저장
    public Travel saveTravelEntity(TravelRequestDto dto) {
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
    public void saveTravelUserEntity(Travel tv, User user) {
        Role role = Role.WRITER;
        TravelUser tu = TravelUser.builder()
                .travel(tv)
                .user(user)
                .role(role)
                .travelNickname(user.getNickname())
                .build();
        travelUserRepository.save(tu);
    }


    @Transactional
    public Travel createTravel(User user, TravelRequestDto dto) {
        Travel tv = saveTravelEntity(dto);
        saveTravelUserEntity(tv, user);
        commonService.createTravelDestination(tv, dto.destinationIds());
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
        commonService.createTravelDestination(tv, dto.destinationIds());

        return travelRepository.save(tv);
    }


    public List<TravelUserDto> getAllTravelUser(Travel tv) {
        List<TravelUser> tuList = travelUserRepository.findByTravelAndIsActiveTrue(tv);
        return tuList.stream().map(tu -> new TravelUserDto(tu.getTravelUserId(), tu.getTravel().getTravelId(), tu.getUser().getUserId(), tu.getRole(), tu.getTravelNickname())).toList();
    }


    // 여행에 대한 여행 유저 반환하는 함수
    public TravelUserDto getTravelUser(User user, Travel tv) {
        TravelUser tu = travelUserRepository.findByTravelAndUserAndIsActiveTrue(tv, user)
                .orElseThrow(() -> new RuntimeException("해당 유저의 TravelUser가 존재하지 않습니다."));
        return new TravelUserDto(tu.getTravelUserId(), tu.getUser().getUserId(), tu.getTravel().getTravelId(), tu.getRole(), tu.getTravelNickname());
    }


}
