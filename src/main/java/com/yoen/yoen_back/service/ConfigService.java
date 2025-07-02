package com.yoen.yoen_back.service;

import com.yoen.yoen_back.entity.user.Role;
import com.yoen.yoen_back.repository.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {
    private final RoleRepository roleRepository;

    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    public List<Role> saveRoles(List<Role> roles) {
        return roleRepository.saveAll(roles);
    }
}
