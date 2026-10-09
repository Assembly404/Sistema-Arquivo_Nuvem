package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.Role;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<UserDetails> findUserByEmail(String username);

    Optional<com.assemblyproject.cloudsystem.entity.User> findByEmail(String email);

    long countByRole(Role role);

    @Query("select count(u) from Users u where u.suspended = false")
    long countBySuspendedFalse();
}
