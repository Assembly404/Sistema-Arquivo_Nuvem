package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

@Service
@RequiredArgsConstructor
public class ContainerService {

    /** Versão do container (.dbs) com compressão selecionável por chunks. */
    public static final String MAGIC_V2 = "DBS2";
    public static final int VERSION_V2 = 2;

    /** Tamanho de cada chunk antes de comprimir/cifrar (1 MiB). */
    private static final int CHUNK_SIZE = 1024 * 1024;
    private static final int GCM_TAG_BYTES = 16;

    /** Container pronto a replicar nos nós, com a chave de dados já persistida. */
    public record StoredContainer(Long fileID, byte[] containerData) {
    }

    /**
     * Resultado do processamento de um upload, sem tocar na base de dados.
     * Permite criar um ficheiro novo ou substituir o conteudo de um existente.
     */
    public record PreparedContainer(byte[] containerData,
                                    String originalName,
                                    String mimeType,
                                    Long originalSizeBytes,
                                    Long storedSizeBytes,
                                    String checksumSha256,
                                    String encryptedDataKey,
                                    String keyEncryptionIv,
                                    String compressionAlgorithm) {
    }

    private final ContainerConfig config;
    private final FileMetadataRepository fileMetadataRepository;
    private final EncryptionService encryptionService;

    @PostConstruct
    void validateMasterKey() {
        try {
            loadMasterKey();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "cloud.container.master-key invalida: " + e.getMessage()
                            + ". Define CLOUD_MASTER_KEY (env) ou o ficheiro .env local.", e);
        }
    }

    @Transactional
    public StoredContainer uploadFile(User owner, MultipartFile file, Folder folder) throws IOException {
        return uploadFile(owner, file, folder, CompressionMode.AUTO);
    }

    @Transactional
    public StoredContainer uploadFile(User owner, MultipartFile file, Folder folder,
                                      CompressionMode mode) throws IOException {
        PreparedContainer prepared = prepare(file, mode);

        FileMetadata metadata = FileMetadata.builder()
                .owner(owner)
                .folder(folder)
                .originalName(prepared.originalName())
                .mimeType(prepared.mimeType())
                .originalSizeBytes(prepared.originalSizeBytes())
                .storedSizeBytes(prepared.storedSizeBytes())
                .checksumSha256(prepared.checksumSha256())
                .compressionAlgorithm(prepared.compressionAlgorithm())
                .uploadedAt(LocalDateTime.now())
                .encryptedDataKey(prepared.encryptedDataKey())
                .keyEncryptionIv(prepared.keyEncryptionIv())
                .build();
        FileMetadata saved = fileMetadataRepository.save(metadata);

        return new StoredContainer(saved.getFileID(), prepared.containerData());
    }

    public PreparedContainer prepare(MultipartFile file) throws IOException {
        return prepare(file, CompressionMode.AUTO);
    }

    /**
     * Cifra, (opcionalmente) comprime e empacota o upload num container .dbs,
     * devolvendo tambem os metadados necessarios, sem persistir nada.
     */
    public PreparedContainer prepare(MultipartFile file, CompressionMode mode) throws IOException {
        byte[] rawData = file.getBytes();
        String checksum = sha256Hex(rawData);
        CompressionAlgorithm algorithm = chooseAlgorithm(rawData, file.getOriginalFilename(),
                file.getContentType(), mode);

        byte[] fileKey = new byte[32];
        new SecureRandom().nextBytes(fileKey);
        byte[] baseIv = new byte[12];
        new SecureRandom().nextBytes(baseIv);

        byte[] container = buildContainerV2(rawData, fileKey, algorithm, baseIv);

        byte[] masterKey = loadMasterKey();
        EncryptionService.WrappedKey wrapped;
        try {
            wrapped = encryptionService.wrapKey(fileKey, masterKey);
        } catch (Exception e) {
            throw new IOException("Key wrapping failed", e);
        }

        return new PreparedContainer(
                container,
                file.getOriginalFilename(),
                file.getContentType(),
                (long) rawData.length,
                (long) container.length,
                checksum,
                wrapped.wrappedKeyBase64(),
                wrapped.ivBase64(),
                algorithm.name());
    }

    /**
     * Decide o algoritmo de compressão. Em AUTO salta ficheiros já compactados
     * (HU 2.1) e só comprime se houver ganho real.
     */
    public CompressionAlgorithm chooseAlgorithm(byte[] rawData, String originalName,
                                                String mimeType, CompressionMode mode) throws IOException {
        CompressionMode effective = mode != null ? mode : CompressionMode.AUTO;
        return switch (effective) {
            case NONE -> CompressionAlgorithm.NONE;
            case DEFLATE -> CompressionAlgorithm.DEFLATE;
            case GZIP -> CompressionAlgorithm.GZIP;
            case AUTO -> autoAlgorithm(rawData, originalName, mimeType);
        };
    }

    private CompressionAlgorithm autoAlgorithm(byte[] rawData, String originalName,
                                               String mimeType) throws IOException {
        if (rawData.length < 256 || CompressionSupport.isAlreadyCompressed(originalName, mimeType)) {
            return CompressionAlgorithm.NONE;
        }
        byte[] deflated = compress(rawData, CompressionAlgorithm.DEFLATE);
        byte[] gzipped = compress(rawData, CompressionAlgorithm.GZIP);
        CompressionAlgorithm best = CompressionAlgorithm.NONE;
        int bestSize = rawData.length;
        if (deflated.length < bestSize) {
            best = CompressionAlgorithm.DEFLATE;
            bestSize = deflated.length;
        }
        if (gzipped.length < bestSize) {
            best = CompressionAlgorithm.GZIP;
            bestSize = gzipped.length;
        }
        // Só compacta se o ganho justificar face ao custo de CPU.
        if (best != CompressionAlgorithm.NONE && CompressionSupport.isBeneficial(rawData.length, bestSize)) {
            return best;
        }
        return CompressionAlgorithm.NONE;
    }

    public byte[] downloadFromNode(byte[] containerData, byte[] fileKey) throws IOException {
        return extractContainer(containerData, fileKey);
    }

    /**
     * Recupera a fileKey de um ficheiro usando o IV guardado no próprio metadata.
     */
    public byte[] resolveFileKey(FileMetadata metadata) throws IOException {
        return unwrapStoredKey(metadata.getEncryptedDataKey(), metadata.getKeyEncryptionIv());
    }

    public byte[] unwrapStoredKey(String encryptedDataKey, String keyEncryptionIv) throws IOException {
        try {
            return encryptionService.unwrapKey(encryptedDataKey, keyEncryptionIv, loadMasterKey());
        } catch (Exception e) {
            throw new IOException("Key unwrap failed", e);
        }
    }

    public byte[] extractContainer(byte[] containerData, byte[] fileKey) throws IOException {
        if (containerData.length < 4) {
            throw new IOException("Container demasiado curto.");
        }
        String magic = new String(Arrays.copyOfRange(containerData, 0, 4),
                java.nio.charset.StandardCharsets.US_ASCII);
        if (MAGIC_V2.equals(magic)) {
            return extractContainerV2(containerData, fileKey);
        }
        if (config.getMagic().equals(magic)) {
            return extractContainerV1(containerData, fileKey);
        }
        throw new IOException("Magic de container inválido: " + magic);
    }

    /** Formato antigo DBS1: um único bloco GZIP cifrado. */
    private byte[] extractContainerV1(byte[] containerData, byte[] fileKey) throws IOException {
        int[] offset = {0};
        readBytes(containerData, offset, 4); // magic
        int payloadSize = readInt(containerData, offset);
        byte[] iv = readBytes(containerData, offset, 12);
        byte[] encrypted = readBytes(containerData, offset, payloadSize);
        byte[] compressed = decrypt(encrypted, fileKey, iv);
        return decompressGzip(compressed);
    }

    // --- Formato DBS2 (compressão selecionável + chunking) ---

    public byte[] buildContainerV2(byte[] rawData, byte[] fileKey,
                                   CompressionAlgorithm algorithm, byte[] baseIv) throws IOException {
        int chunkCount = rawData.length == 0 ? 0 : (rawData.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        int crc = crc32(rawData);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC_V2.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(VERSION_V2);
        out.write(algorithm.code());
        out.write(0); // flags reservadas
        writeLong(out, rawData.length);
        writeInt(out, crc);
        writeInt(out, chunkCount);
        out.write(baseIv, 0, baseIv.length);

        SecureRandom random = new SecureRandom();
        for (int i = 0; i < chunkCount; i++) {
            int from = i * CHUNK_SIZE;
            int to = Math.min(from + CHUNK_SIZE, rawData.length);
            byte[] rawChunk = Arrays.copyOfRange(rawData, from, to);
            byte[] compressed = compress(rawChunk, algorithm);
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            byte[] encrypted = encrypt(compressed, fileKey, iv);

            writeInt(out, rawChunk.length);
            writeInt(out, compressed.length);
            out.write(iv, 0, iv.length);
            out.write(encrypted, 0, encrypted.length);
        }
        return out.toByteArray();
    }

    private byte[] extractContainerV2(byte[] data, byte[] fileKey) throws IOException {
        int[] offset = {4}; // salta o magic
        readBytes(data, offset, 1); // version
        int algoCode = data[offset[0]++] & 0xFF;
        CompressionAlgorithm algorithm = CompressionAlgorithm.fromCode(algoCode);
        readBytes(data, offset, 1); // flags
        long originalSize = readLong(data, offset);
        int expectedCrc = readInt(data, offset);
        int chunkCount = readInt(data, offset);
        readBytes(data, offset, 12); // baseIv reservado

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < chunkCount; i++) {
            int rawLen = readInt(data, offset);
            int compLen = readInt(data, offset);
            byte[] iv = readBytes(data, offset, 12);
            byte[] cipher = readBytes(data, offset, compLen + GCM_TAG_BYTES);
            byte[] compressed = decrypt(cipher, fileKey, iv);
            byte[] chunk = decompress(compressed, algorithm);
            if (chunk.length != rawLen) {
                throw new IOException("Chunk corrompido: tamanho inesperado.");
            }
            out.write(chunk, 0, chunk.length);
        }

        byte[] result = out.toByteArray();
        if (result.length != originalSize) {
            throw new IOException("Container corrompido: tamanho final inesperado.");
        }
        if (crc32(result) != expectedCrc) {
            throw new IOException("Container corrompido: CRC32 inválido.");
        }
        return result;
    }

    /** Mantido por compatibilidade com chamadas antigas (formato DBS1). */
    public byte[] buildContainerBytes(byte[] encrypted, byte[] iv) {
        byte[] out = new byte[4 + 4 + 12 + encrypted.length];
        System.arraycopy(config.getMagic().getBytes(), 0, out, 0, 4);
        System.arraycopy(intToBytes(encrypted.length), 0, out, 4, 4);
        System.arraycopy(iv, 0, out, 8, 12);
        System.arraycopy(encrypted, 0, out, 20, encrypted.length);
        return out;
    }

    /**
     * Reprocessa dados já extraídos (raw) num novo container com o modo de
     * compressão indicado. Usado para comprimir/descomprimir ficheiros existentes.
     */
    public PreparedContainer prepare(byte[] rawData, String originalName, String mimeType,
                                     Long originalSizeBytes, CompressionMode mode) throws IOException {
        String checksum = sha256Hex(rawData);
        CompressionAlgorithm algorithm = chooseAlgorithm(rawData, originalName, mimeType, mode);

        byte[] fileKey = new byte[32];
        new SecureRandom().nextBytes(fileKey);
        byte[] baseIv = new byte[12];
        new SecureRandom().nextBytes(baseIv);

        byte[] container = buildContainerV2(rawData, fileKey, algorithm, baseIv);

        byte[] masterKey = loadMasterKey();
        EncryptionService.WrappedKey wrapped;
        try {
            wrapped = encryptionService.wrapKey(fileKey, masterKey);
        } catch (Exception e) {
            throw new IOException("Key wrapping failed", e);
        }

        return new PreparedContainer(
                container,
                originalName,
                mimeType,
                originalSizeBytes,
                (long) container.length,
                checksum,
                wrapped.wrappedKeyBase64(),
                wrapped.ivBase64(),
                algorithm.name());
    }

    private byte[] compress(byte[] data, CompressionAlgorithm algorithm) throws IOException {
        if (algorithm == CompressionAlgorithm.NONE || data.length == 0) {
            return data;
        }
        if (algorithm == CompressionAlgorithm.GZIP) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(data);
            }
            return baos.toByteArray();
        }
        // DEFLATE cru (RFC 1951), sem cabeçalho zlib.
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!deflater.finished()) {
                int len = deflater.deflate(buffer);
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private byte[] decompress(byte[] data, CompressionAlgorithm algorithm) throws IOException {
        if (algorithm == CompressionAlgorithm.NONE || data.length == 0) {
            return data;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (algorithm == CompressionAlgorithm.GZIP) {
            try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                gzis.transferTo(baos);
            }
            return baos.toByteArray();
        }
        try (InflaterInputStream inflater = new InflaterInputStream(bais, new Inflater(true))) {
            inflater.transferTo(baos);
        }
        return baos.toByteArray();
    }

    private byte[] decompressGzip(byte[] data) throws IOException {
        return decompress(data, CompressionAlgorithm.GZIP);
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

    private long readLong(byte[] data, int[] offset) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (data[offset[0]++] & 0xFFL);
        }
        return v;
    }

    private void writeInt(ByteArrayOutputStream out, int v) {
        out.write((v >>> 24) & 0xFF);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private void writeLong(ByteArrayOutputStream out, long v) {
        for (int i = 7; i >= 0; i--) {
            out.write((int) ((v >>> (8 * i)) & 0xFF));
        }
    }

    private int crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return (int) crc.getValue();
    }

    private static byte[] intToBytes(int v) {
        return new byte[] { (byte)(v >> 24), (byte)(v >> 16), (byte)(v >> 8), (byte)v };
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
}