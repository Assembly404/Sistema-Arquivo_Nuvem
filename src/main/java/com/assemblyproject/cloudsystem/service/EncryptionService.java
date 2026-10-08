package com.assemblyproject.cloudsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    @Value("${cloud.container.master-key:EvguenyEwenAllenEdmundoMalik}")
    private String masterKeyBase64;

    private byte[] lastIv;

    public String wrapKey(byte[] fileKey, byte[] masterKey) throws Exception {
        byte[] iv = randomIv();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.WRAP_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] wrapped = cipher.wrap(new SecretKeySpec(fileKey, "AES"));
        this.lastIv = iv;
        return Base64.getEncoder().encodeToString(wrapped);
    }

    public byte[] unwrapKey(String wrappedKeyB64, byte[] masterKey) throws Exception {
        byte[] wrapped = Base64.getDecoder().decode(wrappedKeyB64);
        // Precisamos do IV usado — vamos buscar ao último ou guardar em BD
        // Em produção o IV deve ser guardado junto com a chave cifrada
        byte[] iv = this.lastIv != null ? this.lastIv : randomIv();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.UNWRAP_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.unwrap(wrapped, "AES", Cipher.SECRET_KEY).getEncoded();
    }

    public String getLastIv() {
        return lastIv != null ? Base64.getEncoder().encodeToString(lastIv) : null;
    }

    private byte[] randomIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
