package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.RegisterRequestDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.InvalidCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ImageService imageService;
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
    }

    public User login(LoginRequestDto dto) throws InvalidCredentialsException {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!bCryptPasswordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        return user;
    }


    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }


    // 유저 프로필 사진 세팅 함수
    public String saveProfileUrl(User user, MultipartFile file) {
        Image profileImage = imageService.saveImage(user, file);
        if (user.getProfileImage() != null) imageService.deleteImage(user.getProfileImage().getImageId());

        user.setProfileImage(profileImage);
        userRepository.save(user);

        return profileImage.getImageUrl();
    }
    public Boolean validateEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
