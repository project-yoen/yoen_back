package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.LoginRequestDto;
import com.yoen.yoen_back.dto.RegisterRequestDto;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.auth.InvalidCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();


    public User register(RegisterRequestDto dto) {
        User user = User.builder()
                .password(bCryptPasswordEncoder.encode(dto.password()))
                .email(dto.email())
                .nickname(dto.nickname())
                .gender(dto.gender())
                .role(dto.role())
                .birthday(Formatter.getDate(dto.birthday()))
                .build();
        return userRepository.save(user);
    }

    public User login(LoginRequestDto dto) throws InvalidCredentialsException {
        User user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다."));

        if (!bCryptPasswordEncoder.matches(dto.password(), user.getPassword())) {
            throw new InvalidCredentialsException("이메일 또는 비밀번호가 잘못되었습니다.");
        }

        return user;
    }

    public User signUp(User user) {
        return userRepository.save(user);
    }

    public List<User> test() {
        return userRepository.findAll();
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
