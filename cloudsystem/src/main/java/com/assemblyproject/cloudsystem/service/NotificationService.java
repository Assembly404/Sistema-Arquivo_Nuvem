package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.Notification;
import com.assemblyproject.cloudsystem.entity.NotificationType;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Criação e gestão de notificações de eventos (RF35). Os produtores de eventos
 * (partilhas, heartbeat, replicação) chamam {@link #notify} sem interromper o
 * fluxo principal caso a notificação falhe.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification notify(User recipient, NotificationType type, String title, String message, String link) {
        if (recipient == null) {
            return null;
        }
        try {
            Notification notification = Notification.builder()
                    .recipient(recipient)
                    .type(type)
                    .title(title)
                    .message(message)
                    .link(link)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            return notificationRepository.save(notification);
        } catch (RuntimeException e) {
            log.warn("Falha ao criar notificação para o utilizador {}: {}",
                    recipient.getUserID(), e.getMessage());
            return null;
        }
    }

    public List<Notification> list(User recipient) {
        return notificationRepository.findAllByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long unreadCount(User recipient) {
        return notificationRepository.countByRecipientAndReadFalse(recipient);
    }

    @Transactional
    public Notification markRead(User recipient, Long notificationID) {
        Notification notification = notificationRepository.findByNotificationIDAndRecipient(notificationID, recipient)
                .orElseThrow(() -> new IllegalArgumentException("Notificação inexistente."));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllRead(User recipient) {
        List<Notification> unread = notificationRepository.findAllByRecipientAndReadFalseOrderByCreatedAtDesc(recipient);
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    @Transactional
    public void delete(User recipient, Long notificationID) {
        Notification notification = notificationRepository.findByNotificationIDAndRecipient(notificationID, recipient)
                .orElseThrow(() -> new IllegalArgumentException("Notificação inexistente."));
        notificationRepository.delete(notification);
    }

    @Transactional
    public void clearAll(User recipient) {
        notificationRepository.deleteAllByRecipient(recipient);
    }
}
