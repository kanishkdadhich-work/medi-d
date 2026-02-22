package com.medid.repository;

import com.medid.entity.User;
import com.medid.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<User> findByUsernameAndRole(String username, Role role);

    List<User> findByRole(Role role);
}
