package com.yoen.yoen_back.service;

import com.yoen.yoen_back.dto.user.LoginRequestDto;
import com.yoen.yoen_back.dto.user.RegisterRequestDto;
import com.yoen.yoen_back.dto.user.UpdateUserDto;
import com.yoen.yoen_back.dto.user.UserResponseDto;
import com.yoen.yoen_back.entity.image.Image;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.enums.Gender;
import com.yoen.yoen_back.repository.user.UserRepository;
import org.apache.http.auth.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 시 비밀번호를 암호화하고 사용자 정보를 저장한다")
    void register_savesUserWithEncodedPassword() {
        RegisterRequestDto dto = new RegisterRequestDto(
                "plain-password",
                "홍길동",
                "hong@example.com",
                "길동",
                Gender.MALE,
                "1998-03-12",
                null
        );
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        userService.register(dto);

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("hong@example.com");
        assertThat(savedUser.getName()).isEqualTo("홍길동");
        assertThat(savedUser.getNickname()).isEqualTo("홍길동");
        assertThat(savedUser.getGender()).isEqualTo(Gender.MALE);
        assertThat(savedUser.getBirthday()).isEqualTo(LocalDate.of(1998, 3, 12));
        assertThat(savedUser.getPassword()).isNotEqualTo("plain-password");
        assertThat(new BCryptPasswordEncoder().matches("plain-password", savedUser.getPassword())).isTrue();
    }

    @Test
    @DisplayName("로그인 성공 시 사용자 응답 DTO를 반환한다")
    void login_returnsUserResponse_whenCredentialsAreValid() throws InvalidCredentialsException {
        User user = userWithEncodedPassword("plain-password");
        user.setProfileImage(image(1L, "https://cdn.example.com/profile.png", "profile.png"));
        when(userRepository.findByEmailAndIsActiveTrue("hong@example.com")).thenReturn(Optional.of(user));

        UserResponseDto response = userService.login(new LoginRequestDto("hong@example.com", "plain-password"));

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.email()).isEqualTo("hong@example.com");
        assertThat(response.gender()).isEqualTo(Gender.MALE);
        assertThat(response.nickname()).isEqualTo("길동");
        assertThat(response.birthday()).isEqualTo(LocalDate.of(1998, 3, 12));
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/profile.png");
    }

    @Test
    @DisplayName("로그인 시 이메일이 없으면 인증 예외를 던진다")
    void login_throwsException_whenEmailDoesNotExist() {
        when(userRepository.findByEmailAndIsActiveTrue("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(new LoginRequestDto("missing@example.com", "plain-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("로그인 시 비밀번호가 틀리면 인증 예외를 던진다")
    void login_throwsException_whenPasswordDoesNotMatch() {
        when(userRepository.findByEmailAndIsActiveTrue("hong@example.com"))
                .thenReturn(Optional.of(userWithEncodedPassword("plain-password")));

        assertThatThrownBy(() -> userService.login(new LoginRequestDto("hong@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("활성 사용자 ID로 사용자를 조회한다")
    void findById_returnsActiveUser() {
        User user = userWithEncodedPassword("plain-password");
        when(userRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID 조회 시 null을 반환한다")
    void findById_returnsNull_whenUserDoesNotExist() {
        when(userRepository.findByUserIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        User result = userService.findById(99L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("사용자 프로필 정보를 응답 DTO로 조회한다")
    void findUserResponseById_returnsUserResponse() {
        User user = userWithEncodedPassword("plain-password");
        when(userRepository.findByUserIdAndIsActiveTrue(1L)).thenReturn(Optional.of(user));

        UserResponseDto response = userService.findUserResponseById(1L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.imageUrl()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 프로필 조회 시 예외를 던진다")
    void findUserResponseById_throwsException_whenUserDoesNotExist() {
        when(userRepository.findByUserIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserResponseById(99L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("사용자 정보를 수정하고 수정된 응답 DTO를 반환한다")
    void updateUser_updatesUserAndReturnsResponse() {
        User user = userWithEncodedPassword("plain-password");
        UpdateUserDto dto = new UpdateUserDto(1L, "김철수", Gender.FEMALE, "철수", "2000-01-02");

        UserResponseDto response = userService.updateUser(user, dto);

        assertThat(user.getName()).isEqualTo("김철수");
        assertThat(user.getNickname()).isEqualTo("철수");
        assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(user.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 2));
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.nickname()).isEqualTo("철수");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 이미지를 저장하고 기존 이미지가 있으면 삭제한다")
    void saveProfileUrl_savesNewProfileImageAndDeletesPreviousImage() {
        User user = userWithEncodedPassword("plain-password");
        Image previousImage = image(1L, "https://cdn.example.com/old.png", "old.png");
        Image newImage = image(2L, "https://cdn.example.com/new.png", "new.png");
        MultipartFile multipartFile = mock(MultipartFile.class);
        user.setProfileImage(previousImage);
        when(imageService.saveImage(user, multipartFile)).thenReturn(newImage);

        String imageUrl = userService.saveProfileUrl(user, multipartFile);

        assertThat(imageUrl).isEqualTo("https://cdn.example.com/new.png");
        assertThat(user.getProfileImage()).isSameAs(newImage);
        verify(imageService).deleteImage(1L);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 이미지가 없던 사용자는 새 이미지만 저장한다")
    void saveProfileUrl_savesNewProfileImageWithoutDeleting_whenPreviousImageDoesNotExist() {
        User user = userWithEncodedPassword("plain-password");
        Image newImage = image(2L, "https://cdn.example.com/new.png", "new.png");
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(imageService.saveImage(user, multipartFile)).thenReturn(newImage);

        String imageUrl = userService.saveProfileUrl(user, multipartFile);

        assertThat(imageUrl).isEqualTo("https://cdn.example.com/new.png");
        assertThat(user.getProfileImage()).isSameAs(newImage);
        verify(imageService, never()).deleteImage(any(Long.class));
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("이메일 존재 여부를 Repository 결과 그대로 반환한다")
    void validateEmail_returnsRepositoryResult() {
        when(userRepository.existsByEmailAndIsActiveTrue("hong@example.com")).thenReturn(true);

        Boolean exists = userService.validateEmail("hong@example.com");

        assertThat(exists).isTrue();
    }

    private User userWithEncodedPassword(String rawPassword) {
        return User.builder()
                .userId(1L)
                .email("hong@example.com")
                .password(new BCryptPasswordEncoder().encode(rawPassword))
                .name("홍길동")
                .nickname("길동")
                .gender(Gender.MALE)
                .birthday(LocalDate.of(1998, 3, 12))
                .build();
    }

    private Image image(Long imageId, String imageUrl, String objectKey) {
        return Image.builder()
                .imageId(imageId)
                .imageUrl(imageUrl)
                .objectKey(objectKey)
                .build();
    }
}
