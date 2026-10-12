package com.assemblyproject.cloudsystem.repository;

import com.assemblyproject.cloudsystem.entity.Notification;
import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByRecipientOrderByCreatedAtDesc(User recipient);

    List<Notification> findAllByRecipientAndReadFalseOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndReadFalse(User recipient);

    Optional<Notification> findByNotificationIDAndRecipient(Long notificationID, User recipient);

    void deleteAllByRecipient(User recipient);
}
