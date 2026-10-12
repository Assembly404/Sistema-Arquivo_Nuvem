package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.Share;
import com.assemblyproject.cloudsystem.entity.SharePermission;

import java.time.LocalDateTime;

/**
 * Item partilhado (ficheiro ou pasta) apresentado ao destinatário ou ao dono.
 */
public record SharedItemResponse(
        Long shareID,
        String type,
        Long itemID,
        String name,
        String mimeType,
        Long sizeBytes,
        SharePermission permission,
        String ownerName,
        String ownerEmail,
        String recipientEmail,
        LocalDateTime createdAt,
        String status
) {

    public static SharedItemResponse from(Share share) {
        if (share.getFile() != null) {
            return new SharedItemResponse(
                    share.getShareID(),
                    "FILE",
                    share.getFile().getFileID(),
                    share.getFile().getOriginalName(),
                    share.getFile().getMimeType(),
                    share.getFile().getOriginalSizeBytes(),
                    share.getPermission(),
                    share.getOwner().getName() + " " + share.getOwner().getSurname(),
                    share.getOwner().getEmail(),
                    share.getRecipient().getEmail(),
                    share.getSharedAt(),
                    share.getFile().getDeletedAt() != null ? "DELETED" : "ACTIVE");
        }
        return new SharedItemResponse(
                share.getShareID(),
                "FOLDER",
                share.getFolder().getFolderID(),
                share.getFolder().getName(),
                null,
                null,
                share.getPermission(),
                share.getOwner().getName() + " " + share.getOwner().getSurname(),
                share.getOwner().getEmail(),
                share.getRecipient().getEmail(),
                share.getSharedAt(),
                share.getFolder().getDeletedAt() != null ? "DELETED" : "ACTIVE");
    }
}