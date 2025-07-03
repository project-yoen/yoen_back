package com.yoen.yoen_back.service;

import com.yoen.yoen_back.common.utils.Formatter;
import com.yoen.yoen_back.dto.RegisterRequestDto;
import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;


    public User register(RegisterRequestDto dto) {
        User user = User.builder()
                .username(dto.username())
                .password(bCryptPasswordEncoder.encode(dto.password()))
                .email(dto.email())
                .nickname(dto.nickname())
                .gender(dto.gender())
                .role(dto.role())
                .birthday(Formatter.getDate(dto.birthday()))
                .build();
        return userRepository.save(user);
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
