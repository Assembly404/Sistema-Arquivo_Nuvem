package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.FileVersion;

import java.time.LocalDateTime;

public record FileVersionResponse(Long versionID,
                                  Integer versionNumber,
                                  String originalName,
                                  String mimeType,
                                  Long originalSizeBytes,
                                  Long storedSizeBytes,
                                  String checksumSha256,
                                  LocalDateTime createdAt) {

    public static FileVersionResponse from(FileVersion version) {
        return new FileVersionResponse(
                version.getVersionID(),
                version.getVersionNumber(),
                version.getOriginalName(),
                version.getMimeType(),
                version.getOriginalSizeBytes(),
                version.getStoredSizeBytes(),
                version.getChecksumSha256(),
                version.getCreatedAt());
    }
}
