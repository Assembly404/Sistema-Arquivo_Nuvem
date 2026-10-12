package com.assemblyproject.cloudsystem.dto.response;

public record UploadResponse(
        Long fileID,
        String originalName,
        String contentType,
        Long size,
        Long storedSize
) {
}