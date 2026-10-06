package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class ContainerService {

    private final FileMetadataRepository fileMetadataRepository;

    public ContainerService(FileMetadataRepository fileMetadataRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
    }

    public Long uploadFile(User owner, MultipartFile file) throws IOException {
        byte[] rawData = file.getBytes();
        String checksum = sha256Hex(rawData);

        FileMetadata metadata = FileMetadata.builder()
                .owner(owner)
                .originalName(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .originalSizeBytes((long) rawData.length)
                .storedSizeBytes((long) rawData.length)
                .checksumSha256(checksum)
                .uploadedAt(LocalDateTime.now())
                .build();

        FileMetadata saved = fileMetadataRepository.save(metadata);
        return saved.getFileID();
    }

    public byte[] buildContainer(byte[] rawData, String originalName, String mimeType, Long originalSizeBytes) {
        return rawData;
    }

    public byte[] extractContainer(byte[] containerData, User owner) throws IOException {
        return containerData;
    }

    public FileMetadata findById(Long fileID) {
        return fileMetadataRepository.findById(fileID)
                .orElseThrow(() -> new RuntimeException("FileMetadata not found with id: " + fileID));
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