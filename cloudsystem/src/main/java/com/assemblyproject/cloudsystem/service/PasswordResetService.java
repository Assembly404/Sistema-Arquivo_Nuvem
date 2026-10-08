package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.PasswordResetToken;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.PasswordResetTokenRepository;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    public static final long OTP_VALIDITY_MINUTES = 15;
    public static final long RESET_TOKEN_VALIDITY_MINUTES = 10;
    public static final int MAX_ATTEMPTS = 5;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void requestReset(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setOtpHash(passwordEncoder.encode(otp));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALIDITY_MINUTES));
        token.setAttempts(0);
        token.setUsed(false);
        tokenRepository.save(token);

        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            emailService.sendOtp(email, otp, OTP_VALIDITY_MINUTES);
        }
        log.debug("[forgot] pedido de reposição para {}", email);
    }

    public String verifyOtp(String email, String otp) {
        PasswordResetToken token = tokenRepository.findTopByEmailOrderByIdDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido."));
        if (token.isUsed()) {
            throw new IllegalArgumentException("Código inválido.");
        }
        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new IllegalStateException("Código expirado.");
        }
        if (token.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalStateException("Demasiadas tentativas. Peça um novo código.");
        }
        token.setAttempts(token.getAttempts() + 1);
        if (!passwordEncoder.matches(otp, token.getOtpHash())) {
            tokenRepository.save(token);
            throw new IllegalArgumentException("Código inválido.");
        }
        String resetToken = UUID.randomUUID().toString();
        token.setResetTokenHash(passwordEncoder.encode(resetToken));
        token.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALIDITY_MINUTES));
        tokenRepository.save(token);
        return resetToken;
    }

    public void resetPassword(String resetToken, String newPassword, String passwordRegex) {
        PasswordResetToken token = tokenRepository.findAll().stream()
                .filter(t -> t.getResetTokenHash() != null && !t.isUsed())
                .filter(t -> passwordEncoder.matches(resetToken, t.getResetTokenHash()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Token de reposição inválido."));
        if (token.getResetTokenExpiresAt() == null || LocalDateTime.now().isAfter(token.getResetTokenExpiresAt())) {
            throw new IllegalStateException("Token de reposição expirado.");
        }
        if (newPassword == null || !newPassword.matches(passwordRegex)) {
            throw new IllegalArgumentException("A nova password não cumpre os requisitos.");
        }
        User user = userRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado."));
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("A nova password deve ser diferente da atual.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
