package com.yoen.yoen_back.repository.user;

import com.yoen.yoen_back.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
