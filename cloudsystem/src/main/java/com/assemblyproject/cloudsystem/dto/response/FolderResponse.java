package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.Folder;

import java.time.LocalDateTime;

public record FolderResponse(Long folderID,
                             String name,
                             Long parentID,
                             LocalDateTime createdAt,
                             LocalDateTime deletedAt) {

    public static FolderResponse from(Folder folder) {
        return new FolderResponse(
                folder.getFolderID(),
                folder.getName(),
                folder.getParent() != null ? folder.getParent().getFolderID() : null,
                folder.getCreatedAt(),
                folder.getDeletedAt());
    }
}
