package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import com.assemblyproject.cloudsystem.dto.request.PublicLinkCreateRequest;
import com.assemblyproject.cloudsystem.dto.response.PublicLinkResponse;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.service.PublicLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestão autenticada de links públicos (RF15) pelo dono do recurso.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/shares/links")
public class PublicLinkController {

    private final PublicLinkService publicLinkService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<PublicLinkResponse> create(@AuthenticationPrincipal JWTUserData principal,
                                                     @RequestBody PublicLinkCreateRequest request) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(PublicLinkResponse.from(publicLinkService.create(user, request)));
    }

    @GetMapping
    public ResponseEntity<List<PublicLinkResponse>> list(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(publicLinkService.listOwned(user).stream()
                .map(PublicLinkResponse::from)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/{linkID}")
    public ResponseEntity<Map<String, Object>> revoke(@AuthenticationPrincipal JWTUserData principal,
                                                      @PathVariable Long linkID) {
        User user = resolveUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        publicLinkService.revoke(user, linkID);
        return ResponseEntity.ok(Map.of("revoked", true, "linkID", linkID));
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
