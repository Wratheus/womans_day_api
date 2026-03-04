package com.womansday.api.repository;

import com.womansday.api.entity.User;
import com.womansday.api.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
    List<User> findByRoleNot(Role role);
    long countByRoleNot(Role role);

    @Query("SELECT u FROM User u WHERE u.role != :role AND (u.hidden IS NULL OR u.hidden = false)")
    List<User> findVisibleByRoleNot(@Param("role") Role role);
}
