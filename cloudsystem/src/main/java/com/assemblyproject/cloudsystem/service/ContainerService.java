package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

@Service
@RequiredArgsConstructor
public class ContainerService {

    /** Container pronto a replicar nos nós, com a chave de dados já persistida. */
    public record StoredContainer(Long fileID, byte[] containerData) {
    }

    private final ContainerConfig config;
    private final FileMetadataRepository fileMetadataRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public StoredContainer uploadFile(User owner, MultipartFile file) throws IOException {
        byte[] rawData = file.getBytes();
        String checksum = sha256Hex(rawData);
        byte[] compressed = compress(rawData);
        byte[] fileKey = new byte[32];
        new SecureRandom().nextBytes(fileKey);
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] encrypted = encrypt(compressed, fileKey, iv);
        byte[] container = buildContainerBytes(encrypted, iv);

        byte[] masterKey = loadMasterKey();
        EncryptionService.WrappedKey wrapped;
        try {
            wrapped = encryptionService.wrapKey(fileKey, masterKey);
        } catch (Exception e) {
            throw new IOException("Key wrapping failed", e);
        }

        FileMetadata metadata = FileMetadata.builder()
                .owner(owner)
                .originalName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .originalSizeBytes((long) rawData.length)
                .storedSizeBytes((long) container.length)
                .checksumSha256(checksum)
                .uploadedAt(LocalDateTime.now())
                .encryptedDataKey(wrapped.wrappedKeyBase64())
                .keyEncryptionIv(wrapped.ivBase64())
                .build();
        FileMetadata saved = fileMetadataRepository.save(metadata);

        return new StoredContainer(saved.getFileID(), container);
    }

    public byte[] downloadFromNode(byte[] containerData, byte[] fileKey) throws IOException {
        return extractContainer(containerData, fileKey);
    }

    /**
     * Recupera a fileKey de um ficheiro usando o IV guardado no próprio metadata.
     */
    public byte[] resolveFileKey(FileMetadata metadata) throws IOException {
        try {
            return encryptionService.unwrapKey(
                    metadata.getEncryptedDataKey(), metadata.getKeyEncryptionIv(), loadMasterKey());
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

    public byte[] buildContainerBytes(byte[] encrypted, byte[] iv) {
        byte[] out = new byte[4 + 4 + 12 + encrypted.length];
        System.arraycopy(config.getMagic().getBytes(), 0, out, 0, 4);
        System.arraycopy(intToBytes(encrypted.length), 0, out, 4, 4);
        System.arraycopy(iv, 0, out, 8, 12);
        System.arraycopy(encrypted, 0, out, 20, encrypted.length);
        return out;
    }

    public FileMetadata findById(Long fileID) {
        return fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new RuntimeException("FileMetadata not found with id: " + fileID));
    }

    /**
     * Carrega e valida a master key (Base64) configurada. Tem de ter 16, 24 ou 32 bytes.
     */
    public byte[] loadMasterKey() throws IOException {
        byte[] masterKey;
        try {
            masterKey = Base64.getDecoder().decode(config.getMasterKey());
        } catch (IllegalArgumentException e) {
            throw new IOException("A master key tem de estar em Base64 válido", e);
        }
        if (masterKey.length != 16 && masterKey.length != 24 && masterKey.length != 32) {
            throw new IOException("A master key tem de descodificar para 16, 24 ou 32 bytes (obtido: "
                    + masterKey.length + " bytes)");
        }
        return masterKey;
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
