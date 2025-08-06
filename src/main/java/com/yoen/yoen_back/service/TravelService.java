package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.travel.*;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Role;
import com.yoen.yoen_back.repository.image.TravelRecordImageRepository;
import com.yoen.yoen_back.repository.travel.TravelRecordRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TravelService {
    private final TravelRepository travelRepository;
    private final TravelUserRepository travelUserRepository;

    private final CommonService commonService;
    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordImageRepository travelRecordImageRepository;
    private final UserRepository userRepository;


    public List<Travel> getAllTravels() {
        return travelRepository.findAll();
    }


    public List<TravelResponseDto> getAllTravelByUser(User user) {
        // Todo: @Query 쓰지 않고 단계별로 조회하기
        List<Travel> tvList = travelUserRepository.findActiveTravelsByUser(user);
        
        // Todo: TravelList 돌면서 해당 Travel의 travelImage가 비어있을 때만(유저가 설정한 대표이미지가 없을 때만) 여행기록 가져오게 변경
        // Todo: 여행에 속한 유저들 정보 보내서 프론트에서 표시할 수 있게 하기
        // Todo: 함수로 빼도 ㄱㅊ
        // Todo: 첫 여행기록에 이미지가 없으면 이미지 있는 여행기록 나올때까지 탐색해야 함
        return tvList.stream().map(travel -> {
            Optional<String> imageUrl;
            List<Image> images = travelRecordImageRepository.findFirstByTravelOrderByCreatedAtAsc(travel.getTravelId());
            Optional<Image> image = images.stream().findFirst();
            imageUrl = image.map(Image::getImageUrl);

//            List<TravelRecord> trList = travelRecordRepository.findByTravel_TravelIdAndIsActiveTrue(travel.getTravelId());
//            if(!trList.isEmpty()) {
//                TravelRecord tr = trList.get(0);
//                List<Image> images = travelRecordImageRepository.findFirstByTravelRecordOrderByCreatedAtAsc(travel.getTravelId());
//                Optional<Image> image = images.stream().findFirst();
//                imageUrl = image.map(Image::getImageUrl);
//            }
            return new TravelResponseDto(travel.getTravelId(), travel.getNumOfPeople(), travel.getTravelName(), travel.getStartDate(), travel.getEndDate(), imageUrl.orElse(""));
        }).toList();
    }
    // Todo: 여행의 프로필 이미지 바꾸는 함수 구현


    // Todo 여행을 삭제할 때 관련된 모든 테이블의 레코드를 비활성화 해야 할까?
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
                .numOfJoinedPeople(1L)
                .sharedFund(0L)
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
                .orElseThrow(() -> new IllegalStateException("해당 유저의 TravelUser가 존재하지 않습니다."));
        return new TravelUserDto(tu.getTravelUserId(), tu.getUser().getUserId(), tu.getTravel().getTravelId(), tu.getRole(), tu.getTravelNickname());
    }

    // 여행에 대한 여행 유저 반환하는 함수
    public void updateTravelUserNickname(TravelNicknameUpdateDto dto) {
        TravelUser tu = travelUserRepository.findByTravelUserIdAndIsActiveTrue(dto.travelUserId()).orElseThrow(() -> new IllegalStateException("존재하지 않은 참여자 입니다."));
        tu.setTravelNickname(dto.travelNickname());
        travelUserRepository.save(tu);
    }

    public List<TravelUserResponseDto> getDetailTravelUser(Travel tv) {
        List<TravelUser> tuList = travelUserRepository.findByTravelAndIsActiveTrue(tv);
        return tuList.stream().map(traveluser -> {
            User user = userRepository.getReferenceById(traveluser.getUser().getUserId());
            String imageUrl = "";
            if(user.getProfileImage() != null) {
                Image image = user.getProfileImage();
                imageUrl = image.getImageUrl();
            }
            return new TravelUserResponseDto(traveluser.getTravelUserId(), user.getNickname(),traveluser.getTravelNickname(), user.getGender(), user.getBirthday(), imageUrl);
        }).toList();
    }

    public Boolean increaseNumOfJoinedPeople(Travel tv) {
        Long numOfJoinedPeople = tv.getNumOfJoinedPeople();
        if (tv.getNumOfPeople() >= numOfJoinedPeople + 1) {
            tv.setNumOfJoinedPeople(numOfJoinedPeople + 1);
            travelRepository.save(tv);
            return true;
        }
        return false;
    }

    public Boolean decreaseNumOfJoinedPeople(Travel tv) {
        Long numOfJoinedPeople = tv.getNumOfJoinedPeople();
        if (numOfJoinedPeople - 1 >= 0) {
            tv.setNumOfJoinedPeople(numOfJoinedPeople - 1);
            travelRepository.save(tv);
            return true;
        }
        return false;
    }

    public void leaveTravel(TravelUser tu) {
        if (decreaseNumOfJoinedPeople(tu.getTravel())) {
            tu.setIsActive(false);
            travelUserRepository.save(tu);
        } else {
            throw new IllegalStateException("지원자가 음수가 될 수 없습니다.");
        }
    }


}
