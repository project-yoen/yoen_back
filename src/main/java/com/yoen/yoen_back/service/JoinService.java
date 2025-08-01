package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.entity.InvalidJoinCodeException;
import com.yoen.yoen_back.dao.redis.TravelJoinCodeRedisDao;
import com.yoen.yoen_back.dto.etc.joincode.AcceptJoinRequestDto;
import com.yoen.yoen_back.dto.etc.joincode.JoinRequestListResponseDto;
import com.yoen.yoen_back.dto.etc.joincode.UserTravelJoinResponseDto;
import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.travel.Travel;
import com.yoen.yoen_back.entity.travel.TravelJoinRequest;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.travel.TravelJoinRequestRepository;
import com.yoen.yoen_back.repository.travel.TravelRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final SecureRandom random = new SecureRandom();
    private final static String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final TravelJoinCodeRedisDao travelJoinCodeRedisDao;
    private final TravelJoinRequestRepository travelJoinRequestRepository;

    /** 조회용 **/
    private final TravelRepository travelRepository;
    private final TravelUserRepository travelUserRepository;


    public String getJoinCode(Long travelId) {
        travelRepository.findById(travelId).orElseThrow(() -> new IllegalStateException("존재하지 않는 여행"));
        if (!travelJoinCodeRedisDao.existsTravelId(travelId)) {
            String code = getUniqueJoinCode(6);
            travelJoinCodeRedisDao.saveBidirectionalMapping(code, travelId);
        }
        return travelJoinCodeRedisDao.getCodeByTravelId(travelId)
                .orElseThrow(() -> new InvalidJoinCodeException("해당 여행의 참여 코드가 존재하지 않습니다."));
    }

    public void requestToJoinTravel(User user, String code) {
        String travelId = travelJoinCodeRedisDao.getTravelIdByCode(code).orElseThrow(() -> new InvalidJoinCodeException("유효하지 않은 코드입니다."));
        Long tl = Long.parseLong(travelId);
        Travel tv = travelRepository.getReferenceById(tl);
        List<TravelJoinRequest> tjrList = travelJoinRequestRepository.findByTravelAndUserAndIsActiveTrue(tv, user);

        // 이미 기존 유저가 존재하면 isJoined를 true
        List<TravelUser> tuList = travelUserRepository.findByTravelAndIsActiveTrue(tv);
        boolean isJoined = tuList.stream().anyMatch(tu -> user.getUserId().equals(tu.getUser().getUserId()));

        // 같은 여행에 isActive이 True인 레코드가 남아있거나 이미 추가된 유저면 추가 불가능 (그러므로 승인시 혹은 거부시 isActive를 false로 해줘야함)
        if (tjrList.isEmpty() && !isJoined) {
            TravelJoinRequest tjr = TravelJoinRequest.builder()
                    .user(user)
                    .travel(tv)
                    .isAccepted(false)
                    .build();
            travelJoinRequestRepository.save(tjr);
        }
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

    // 내 여행에 참여신청한 사람들 출력하는 함수
    public List<JoinRequestListResponseDto> getJoinRequestList(Long travelId){
        List<TravelJoinRequest> tjrList = travelJoinRequestRepository.findByTravel_TravelIdAndIsActiveTrue(travelId);
        return tjrList.stream().map(
                tjr -> {
                    User user = tjr.getUser();
                    Image profileImage = user.getProfileImage();
                    String profileImageUrl = (profileImage != null)
                            ? profileImage.getImageUrl()
                            : "https://your-default-image-url.com/default.jpg";
                    return new JoinRequestListResponseDto(tjr.getTravelJoinRequestId(), user.getGender(), user.getName(), profileImageUrl);
                }
        ).toList();
    }
    // 내 여행에 참여 신청한 사람 승인하는 함수
    public void acceptJoinRequest(AcceptJoinRequestDto dto) {
        TravelJoinRequest tjr = travelJoinRequestRepository.getReferenceById(dto.travelJoinRequestId());
        tjr.setIsAccepted(true); // 수락됨을 True로 변경
        tjr.setIsActive(false); // soft delete 수행
        travelJoinRequestRepository.save(tjr);
        TravelUser tu = TravelUser.builder()
                .travel(tjr.getTravel())
                .user(tjr.getUser())
                .role(dto.role())
                .travelNickname(tjr.getUser().getNickname())
                .build();
        travelUserRepository.save(tu);
    }
    // 내 여행에 참여 신청한 사람 거절하는 함수
    public void rejectJoinRequest(Long travelJoinRequestId) {
        TravelJoinRequest tjr = travelJoinRequestRepository.getReferenceById(travelJoinRequestId);
        tjr.setIsAccepted(false); // 기본값이 null일거같은데 false로 변경
        tjr.setIsActive(false); // 거절했으니까 목록에서 soft delete
        travelJoinRequestRepository.save(tjr);
    }

    // 자기 자신이 신청한 여행 리스트를 보기 위한 함수
    public List<UserTravelJoinResponseDto> getUserTravelJoinRequests(User user) {
        List<TravelJoinRequest> tjrList = travelJoinRequestRepository.findByUserAndIsActiveTrueAndIsAcceptedFalse(user);
        return tjrList.stream().map(tjr -> {
            Travel tv = tjr.getTravel();
            List<UserResponseDto> joinedUsers = travelUserRepository.findByTravelAndIsActiveTrue(tv).stream().map(tu -> {
                User tmpUser = tu.getUser();
                String imageUrl = tmpUser.getProfileImage() != null ? tmpUser.getProfileImage().getImageUrl() : "";
                return new UserResponseDto(tmpUser.getUserId(), tmpUser.getName(), tmpUser.getEmail(), tmpUser.getGender(), tmpUser.getNickname(), tmpUser.getBirthday(), imageUrl);
            }).toList();
            return new UserTravelJoinResponseDto(tjr.getTravelJoinRequestId(), tv.getTravelId(), tv.getTravelName(), tv.getNation(), joinedUsers);
        }).toList();
    }

    public void deleteUserTravelJoinRequest(Long travelJoinRequestId) {
        Optional<TravelJoinRequest> tjr = travelJoinRequestRepository.findById(travelJoinRequestId);
        tjr.ifPresent(travelJoinRequest -> {
            travelJoinRequest.setIsActive(false);
            travelJoinRequest.setIsAccepted(false); // 이미 false 일 확률이 더 높음
            travelJoinRequestRepository.save(travelJoinRequest);
        });
    }
}
