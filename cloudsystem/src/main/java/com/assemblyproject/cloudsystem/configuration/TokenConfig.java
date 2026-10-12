package com.assemblyproject.cloudsystem.configuration;

import com.assemblyproject.cloudsystem.entity.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class TokenConfig {

    @Value("${security.jwt.secret}")
    private String secret;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "security.jwt.secret nao configurado. Define JWT_SECRET (env) ou o ficheiro .env local.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "security.jwt.secret demasiado curto: usar no minimo 32 caracteres (HMAC256).");
        }
    }

    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("userID", user.getUserID())
                .withClaim("role", user.getRole() != null ? user.getRole().name() : "USER")
                .withClaim("suspended", user.isSuspended())
                .withSubject(user.getEmail())
                .withExpiresAt(Instant.now().plusSeconds(86400))
                .withIssuedAt(Instant.now())
                .sign(algorithm);
    }

    public Optional<JWTUserData> validateToken(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);

        try {
            DecodedJWT decode = JWT.require(algorithm).build().verify(token);

            return Optional.of(JWTUserData.builder()
                    .userID(decode.getClaim("userID").asLong())
                    .email(decode.getSubject())
                    .role(decode.getClaim("role").asString())
                    .suspended(decode.getClaim("suspended").asBoolean())
                    .build());

        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
