package com.assemblyproject.cloudsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração e proteção dos tokens de vinculação dos dispositivos pessoais
 * (RF17.4). O token é guardado cifrado com a master key de forma reversível,
 * porque o servidor precisa de o enviar ao agente local nas chamadas HTTP.
 */
@Service
@RequiredArgsConstructor
public class NodeSecretService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final ContainerService containerService;

    public String generateToken() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    public String encrypt(String token) {
        if (token == null) {
            return null;
        }
        try {
            byte[] key = containerService.loadMasterKey();
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_BYTES + cipherText.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(cipherText, 0, out, IV_BYTES, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar o token do dispositivo.", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        try {
            byte[] key = containerService.loadMasterKey();
            byte[] all = Base64.getDecoder().decode(encrypted);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(all, 0, iv, 0, IV_BYTES);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plain = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar o token do dispositivo.", e);
        }
    }

    /**
     * Compara o token recebido com o token guardado no nó, em tempo constante.
     */
    public boolean matches(String encryptedStored, String candidate) {
        if (encryptedStored == null || candidate == null) {
            return false;
        }
        String stored;
        try {
            stored = decrypt(encryptedStored);
        } catch (RuntimeException e) {
            return false;
        }
        if (stored == null) {
            return false;
        }
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }
}
