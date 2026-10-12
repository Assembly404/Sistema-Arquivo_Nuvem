package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Códigos de pareamento de curta duração (RF17.4). Um utilizador autenticado
 * gera um código; o agente local apresenta-o no registo, vinculando o
 * dispositivo à conta correta em vez de o atribuir a um administrador
 * arbitrário.
 */
@Service
@RequiredArgsConstructor
public class PairingCodeService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Pairing> codes = new ConcurrentHashMap<>();

    @Value("${cloud.node.pairing-ttl-minutes:15}")
    private long ttlMinutes;

    public record Pairing(Long userID, LocalDateTime expiresAt) {
    }

    public String create(User user) {
        purgeExpired();
        String code = randomCode();
        codes.put(code, new Pairing(user.getUserID(), LocalDateTime.now().plusMinutes(ttlMinutes)));
        return code;
    }

    /**
     * Consome um código e devolve o id do utilizador associado. O código deixa
     * de ser válido após a utilização.
     */
    public Long consume(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidPairingException("É necessário um código de pareamento válido.");
        }
        purgeExpired();
        Pairing pairing = codes.remove(code.trim().toUpperCase());
        if (pairing == null) {
            throw new InvalidPairingException("Código de pareamento inválido ou já utilizado.");
        }
        if (pairing.expiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidPairingException("O código de pareamento expirou.");
        }
        return pairing.userID();
    }

    public long ttlMinutes() {
        return ttlMinutes;
    }

    private void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        codes.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
