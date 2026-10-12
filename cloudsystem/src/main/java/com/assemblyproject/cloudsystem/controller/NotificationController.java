package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.response.NotificationResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Notificações do utilizador autenticado (RF35).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(notificationService.list(user).stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("unread", notificationService.unreadCount(user)));
    }

    @PostMapping("/{notificationID}/read")
    public ResponseEntity<NotificationResponse> markRead(@AuthenticationPrincipal JWTUserData principal,
                                                         @PathVariable Long notificationID) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(NotificationResponse.from(notificationService.markRead(user, notificationID)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("marked", notificationService.markAllRead(user)));
    }

    @DeleteMapping("/{notificationID}")
    public ResponseEntity<Map<String, Object>> delete(@AuthenticationPrincipal JWTUserData principal,
                                                      @PathVariable Long notificationID) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.delete(user, notificationID);
        return ResponseEntity.ok(Map.of("deleted", true, "notificationID", notificationID));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearAll(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        notificationService.clearAll(user);
        return ResponseEntity.ok(Map.of("cleared", true));
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
