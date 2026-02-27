package com.womansday.api.repository;

import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
    List<User> findByRoleNot(Role role);
    long countByRoleNot(Role role);
}
