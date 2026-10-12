package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.Notification;
import com.assemblyproject.cloudsystem.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationID,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationID(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
