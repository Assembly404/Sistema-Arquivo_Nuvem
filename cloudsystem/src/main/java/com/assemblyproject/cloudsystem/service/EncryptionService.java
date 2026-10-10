package com.assemblyproject.cloudsystem.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    /**
     * Chave de dados (AES-256) já cifrada com a master key, juntamente com o IV
     * usado nessa operação. Ambos em Base64 para persistência direta no metadata.
     */
    public record WrappedKey(String wrappedKeyBase64, String ivBase64) {
    }

    public WrappedKey wrapKey(byte[] fileKey, byte[] masterKey) throws Exception {
        byte[] iv = randomIv();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.WRAP_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] wrapped = cipher.wrap(new SecretKeySpec(fileKey, "AES"));
        return new WrappedKey(Base64.getEncoder().encodeToString(wrapped), Base64.getEncoder().encodeToString(iv));
    }

    public byte[] unwrapKey(String wrappedKeyB64, String ivB64, byte[] masterKey) throws Exception {
        byte[] wrapped = Base64.getDecoder().decode(wrappedKeyB64);
        byte[] iv = Base64.getDecoder().decode(ivB64);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.UNWRAP_MODE, new SecretKeySpec(masterKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.unwrap(wrapped, "AES", Cipher.SECRET_KEY).getEncoded();
    }

    private byte[] randomIv() {
        byte[] iv = new byte[GCM_IV_BYTES];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
