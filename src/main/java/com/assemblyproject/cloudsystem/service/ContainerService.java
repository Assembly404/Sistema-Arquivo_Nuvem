package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
public class ContainerService {

    private final ContainerConfig config;
    private final FileMetadataRepository fileMetadataRepository;
    private final EncryptionService encryptionService;

    public Long uploadFile(User owner, MultipartFile file) throws IOException {
        byte[] rawData = file.getBytes();
        String checksum = sha256Hex(rawData);
        byte[] compressed = compress(rawData);
        byte[] fileKey = new byte[32];
        new SecureRandom().nextBytes(fileKey);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] encrypted = encrypt(compressed, fileKey, iv);
        byte[] container = buildContainerBytes(encrypted, iv);

        FileMetadata metadata = FileMetadata.builder()
                .owner(owner)
                .originalName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .originalSizeBytes((long) rawData.length)
                .storedSizeBytes((long) container.length)
                .checksumSha256(checksum)
                .uploadedAt(LocalDateTime.now())
                .build();
        FileMetadata saved = fileMetadataRepository.save(metadata);

        try {
            byte[] masterKey = java.util.Base64.getDecoder().decode(config.getMasterKey());
            metadata.setEncryptedDataKey(encryptionService.wrapKey(fileKey, masterKey));
            metadata.setKeyEncryptionIv(encryptionService.getLastIv());
        } catch (Exception e) {
            throw new IOException("Key wrapping failed", e);
        }
        fileMetadataRepository.save(metadata);

        return saved.getFileID();
    }

    public byte[] downloadContainer(Long fileID, User owner) throws IOException {
        throw new UnsupportedOperationException("Use downloadFromNode - server does not store containers");
    }

    public byte[] buildContainer(byte[] rawData, String originalName, String mimeType, Long originalSizeBytes) throws IOException {
        byte[] compressed = compress(rawData);
        byte[] fileKey = new byte[32];
        new SecureRandom().nextBytes(fileKey);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] encrypted = encrypt(compressed, fileKey, iv);
        return buildContainerBytes(encrypted, iv);
    }

    public byte[] downloadFromNode(byte[] containerData, byte[] fileKey) throws IOException {
        return extractContainer(containerData, fileKey);
    }

    public byte[] decryptKey(String encryptedKeyB64, byte[] masterKey) throws IOException {
        try {
            return encryptionService.unwrapKey(encryptedKeyB64, masterKey);
        } catch (Exception e) {
            throw new IOException("Key unwrap failed", e);
        }
    }

    public byte[] extractContainer(byte[] containerData, byte[] fileKey) throws IOException {
        int[] offset = {0};
        byte[] magic = readBytes(containerData, offset, 4); offset[0] += 4;
        if (!new String(magic).equals(config.getMagic())) throw new IOException("Invalid container magic");
        int payloadSize = readInt(containerData, offset); offset[0] += 4;
        byte[] iv = readBytes(containerData, offset, 12); offset[0] += 12;
        byte[] encrypted = readBytes(containerData, offset, payloadSize);
        byte[] compressed = decrypt(encrypted, fileKey, iv);
        return decompress(compressed);
    }

    public FileMetadata findById(Long fileID) {
        return fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new RuntimeException("FileMetadata not found with id: " + fileID));
    }

    private byte[] buildContainerBytes(byte[] encrypted, byte[] iv) {
        byte[] out = new byte[4 + 4 + 12 + encrypted.length];
        System.arraycopy(config.getMagic().getBytes(), 0, out, 0, 4);
        System.arraycopy(intToBytes(encrypted.length), 0, out, 4, 4);
        System.arraycopy(iv, 0, out, 8, 12);
        System.arraycopy(encrypted, 0, out, 20, encrypted.length);
        return out;
    }

    private byte[] encrypt(byte[] data, byte[] key, byte[] iv) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new IOException("Encryption failed", e);
        }
    }

    private byte[] decrypt(byte[] data, byte[] key, byte[] iv) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new IOException("Decryption failed", e);
        }
    }

    private byte[] compress(byte[] data) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) { gzos.write(data); }
        return baos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (GZIPInputStream gzis = new GZIPInputStream(bais)) { gzis.transferTo(baos); }
        return baos.toByteArray();
    }

    private byte[] readBytes(byte[] data, int[] offset, int len) {
        byte[] out = Arrays.copyOfRange(data, offset[0], offset[0] + len);
        offset[0] += len;
        return out;
    }

    private int readInt(byte[] data, int[] offset) {
        int v = ((data[offset[0]] & 0xFF) << 24) | ((data[offset[0]+1] & 0xFF) << 16)
              | ((data[offset[0]+2] & 0xFF) << 8)  | (data[offset[0]+3] & 0xFF);
        offset[0] += 4;
        return v;
    }

    private static byte[] intToBytes(int v) {
        return new byte[] { (byte)(v >> 24), (byte)(v >> 16), (byte)(v >> 8), (byte)v };
    }

    private String sha256Hex(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256", e);
        }
    }
}
