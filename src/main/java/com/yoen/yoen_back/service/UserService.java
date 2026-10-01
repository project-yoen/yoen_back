package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dao.redis.RefreshTokenRedisDao;
import com.yoen.yoen_back.dao.redis.UserCacheRedisDao;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.RegisterRequestDto;
import com.yoen.yoen_back.dto.user.UpdateUserDto;
import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.travel.TravelUser;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.repository.NotificationRepository;
import com.yoen.yoen_back.repository.travel.TravelJoinRequestRepository;
import com.yoen.yoen_back.repository.travel.TravelUserRepository;
import com.yoen.yoen_back.repository.user.FirebaseTokenRepository;
import com.yoen.yoen_back.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final UserCacheRedisDao userCacheRedisDao;
    private final RefreshTokenRedisDao refreshTokenRedisDao;
    private final FirebaseTokenRepository firebaseTokenRepository;
    private final TravelUserRepository travelUserRepository;
    private final TravelJoinRequestRepository travelJoinRequestRepository;
    private final NotificationRepository notificationRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    private static final String DELETED_USER_NAME = "탈퇴한 사용자";
    private static final LocalDate DELETED_USER_BIRTHDAY = LocalDate.of(1970, 1, 1);


    public void register(RegisterRequestDto dto) {
        User user = User.builder()
                .password(bCryptPasswordEncoder.encode(dto.password()))
                .email(dto.email())
                .gender(dto.gender())
                .name(dto.name())
                .nickname(dto.name())
                .birthday(Formatter.getDate(dto.birthday()))
                .build();
        userRepository.save(user);
        log.info("event=user_registered userId={}", user.getUserId());
    }

    public UserResponseDto login(LoginRequestDto dto) throws InvalidCredentialsException {
        User user = userRepository.findByEmailAndIsActiveTrue(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!bCryptPasswordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다.");
        }
        log.info("event=user_login_succeeded userId={}", user.getUserId());
        Image profileImage = user.getProfileImage();
        String imageUrl = (profileImage != null) ? profileImage.getImageUrl() : "";

        return  new UserResponseDto(user.getUserId(), user.getName(), user.getEmail(), user.getGender(), user.getNickname(), user.getBirthday(), imageUrl);
    }


    public User findById(Long id) {
        return userRepository.findByUserIdAndIsActiveTrue(id).orElse(null);
    }

    public UserResponseDto findUserResponseById(Long id) {
        User user = userRepository.findByUserIdAndIsActiveTrue(id).orElseThrow(() -> new IllegalStateException("존재하지 않는 유저입니다."));
        Image profileImage = user.getProfileImage();
        String imageUrl = (profileImage != null) ? profileImage.getImageUrl() : "";
        return new UserResponseDto(user.getUserId(), user.getName(), user.getEmail(), user.getGender(), user.getNickname(), user.getBirthday(), imageUrl);
    }

    public UserResponseDto updateUser(User user, UpdateUserDto dto) {
        // 인증 필터에서 온 user는 캐시 복원본(detached)일 수 있으므로 관리 엔티티를 다시 조회해 수정한다
        User managed = userRepository.findByUserIdAndIsActiveTrue(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 유저입니다."));
        managed.setName(dto.name());
        managed.setNickname(dto.nickname());
        managed.setGender(dto.gender());
        managed.setBirthday(Formatter.getDate(dto.birthday()));
        userRepository.save(managed);
        userCacheRedisDao.evict(managed.getUserId());
        log.info("event=user_profile_updated userId={}", managed.getUserId());
        Image image = managed.getProfileImage();
        String imageUrl = (image != null) ? image.getImageUrl() : "";
        return new UserResponseDto(managed.getUserId(), managed.getName(), managed.getEmail(), managed.getGender(), managed.getNickname(), managed.getBirthday(), imageUrl);
    }


    // 유저 프로필 사진 세팅 함수
    public String saveProfileUrl(User user, MultipartFile file) {
        // 인증 필터에서 온 user는 캐시 복원본(detached)일 수 있으므로 관리 엔티티를 다시 조회해 수정한다
        User managed = userRepository.findByUserIdAndIsActiveTrue(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 유저입니다."));
        Image profileImage = imageService.saveImage(managed, file);
        if (managed.getProfileImage() != null) imageService.deleteImage(managed.getProfileImage().getImageId());

        managed.setProfileImage(profileImage);
        userRepository.save(managed);
        userCacheRedisDao.evict(managed.getUserId());
        log.info("event=user_profile_image_updated userId={} imageId={}", managed.getUserId(), profileImage.getImageId());

        return profileImage.getImageUrl();
    }

    public Boolean validateEmail(String email) {
        return userRepository.existsByEmailAndIsActiveTrue(email);
    }

    @Transactional
    public void deleteAccount(User user, String password) throws InvalidCredentialsException {
        User managed = userRepository.findByUserIdAndIsActiveTrue(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 유저입니다."));

        if (!bCryptPasswordEncoder.matches(password, managed.getPassword())) {
            throw new InvalidCredentialsException("비밀번호가 올바르지 않습니다.");
        }

        Long userId = managed.getUserId();
        Image profileImage = managed.getProfileImage();

        List<TravelUser> travelUsers = travelUserRepository.findByUserAndIsActiveTrue(managed);
        travelUsers.forEach(travelUser -> travelUser.setTravelNickname(DELETED_USER_NAME));
        travelUserRepository.saveAll(travelUsers);

        managed.setProfileImage(null);
        if (profileImage != null) {
            imageService.deleteImage(profileImage.getImageId());
        }

        firebaseTokenRepository.deleteAllByUser_UserId(userId);
        travelJoinRequestRepository.deleteAllByUser_UserId(userId);
        notificationRepository.deleteAllByUser_UserId(userId);

        managed.setEmail("deleted-" + userId + "-" + UUID.randomUUID() + "@deleted.invalid");
        managed.setPassword(bCryptPasswordEncoder.encode(UUID.randomUUID().toString()));
        managed.setName(DELETED_USER_NAME);
        managed.setNickname(DELETED_USER_NAME);
        managed.setGender(Gender.OTHERS);
        managed.setBirthday(DELETED_USER_BIRTHDAY);
        managed.setIsActive(false);
        userRepository.saveAndFlush(managed);

        refreshTokenRedisDao.delete(String.valueOf(userId));
        userCacheRedisDao.evict(userId);
        log.info("event=user_account_deleted userId={}", userId);
    }
}
