package com.yoen.yoen_back.service;

import com.yoen.yoen_back.entity.user.User;
import com.yoen.yoen_back.repository.jpa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public User signUp(User user) {
        return userRepository.save(user);
    }

    public List<User> test() {
        return userRepository.findAll();
    }
}
