package com.assemblyproject.cloudsystem.configuration;

import com.assemblyproject.cloudsystem.entity.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class TokenConfig {

    private String secret = "Evgueny25087Nenane";

    public String generateToken(User user) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withClaim("userID", user.getUserID())
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
                    .build());

        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
