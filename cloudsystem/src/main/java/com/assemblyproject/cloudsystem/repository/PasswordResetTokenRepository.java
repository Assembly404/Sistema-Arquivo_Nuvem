package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTopByEmailOrderByIdDesc(String email);

    Optional<PasswordResetToken> findByResetTokenHashAndUsedFalse(String resetTokenHash);
}
