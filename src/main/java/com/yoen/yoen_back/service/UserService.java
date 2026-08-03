package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dao.redis.UserCacheRedisDao;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.RegisterRequestDto;
import com.yoen.yoen_back.dto.user.UpdateUserDto;
import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final UserCacheRedisDao userCacheRedisDao;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();


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
}
