package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.request.MoveRequest;
import com.assemblyproject.cloudsystem.dto.request.RenameRequest;
import com.assemblyproject.cloudsystem.dto.response.FileMetadataResponse;
import com.assemblyproject.cloudsystem.dto.response.FileVersionResponse;
import com.assemblyproject.cloudsystem.dto.response.UploadResponse;
import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.NotificationType;
import com.assemblyproject.cloudsystem.entity.UploadDestination;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.service.CompressionMode;
import com.assemblyproject.cloudsystem.service.ContainerService;
import com.assemblyproject.cloudsystem.service.FileService;
import com.assemblyproject.cloudsystem.service.NotificationService;
import com.assemblyproject.cloudsystem.service.ReplicationService;
import com.assemblyproject.cloudsystem.service.FolderService;
import com.assemblyproject.cloudsystem.configuration.JWTUserData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final ContainerService containerService;
    private final FileService fileService;
    private final ReplicationService replicationService;
    private final FolderService folderService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@AuthenticationPrincipal JWTUserData principal,
                                        @RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "folderId", required = false) Long folderId,
                                        @RequestParam(value = "destination", required = false, defaultValue = "AUTO") String destination,
                                        @RequestParam(value = "nodeId", required = false) Long nodeId,
                                        @RequestParam(value = "compression", required = false, defaultValue = "AUTO") String compression) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Folder folder = folderId != null ? folderService.requireOwned(user, folderId) : null;
        if (folder != null && folder.getDeletedAt() != null) {
            return ResponseEntity.status(409).build();
        }
        Long quota = user.getQuotaBytes();
        long usedBefore = fileMetadataRepository.sumOriginalSizeBytesByOwner(user);
        if (quota != null && quota > 0 && usedBefore + file.getSize() > quota) {
            return ResponseEntity.status(400)
                    .body(Map.of("message", "Cota de armazenamento excedida na nuvem do sistema."));
        }
        UploadDestination dest = parseDestination(destination);
        ContainerService.StoredContainer stored = containerService.uploadFile(
                user, file, folder, CompressionMode.parse(compression));
        try {
            replicationService.replicateFile(stored.fileID(), user, stored.containerData(), dest, nodeId);
        } catch (RuntimeException e) {
            // O upload só é válido se ficou pelo menos uma réplica persistida.
            try {
                fileService.purge(user, stored.fileID());
            } catch (IOException ignored) {
                // melhor esforço
            }
            throw e;
        }

        if (quota != null && quota > 0) {
            long usedAfter = usedBefore + file.getSize();
            if (usedBefore < quota * 0.8 && usedAfter >= quota * 0.8) {
                long percent = Math.round((double) usedAfter / quota * 100);
                notificationService.notify(user, NotificationType.QUOTA_WARNING,
                        "Armazenamento quase cheio",
                        "Já usaste " + percent + "% da tua cota de armazenamento na nuvem do sistema.",
                        "/dashboard/armazenamento");
            }
        }

        UploadResponse response = new UploadResponse(
                stored.fileID(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataResponse>> listUserFiles(
            @AuthenticationPrincipal JWTUserData principal,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "mime", required = false) String mime,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        boolean searching = search != null || mime != null || from != null || to != null;
        List<FileMetadata> files = searching
                ? fileService.search(user, search, mime, from, to)
                : fileService.listByFolder(user, folderId);
        return ResponseEntity.ok(files.stream().map(FileMetadataResponse::fromEntity).collect(Collectors.toList()));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<FileMetadataResponse>> listTrash(@AuthenticationPrincipal JWTUserData principal) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(fileService.listTrash(user).stream()
                .map(FileMetadataResponse::fromEntity)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@AuthenticationPrincipal JWTUserData principal,
                                               @PathVariable Long fileId,
                                               @RequestParam(value = "version", required = false) Integer version) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        FileMetadata metadata = fileService.requireReadable(user, fileId);
        byte[] fileData = fileService.downloadShared(user, fileId, version);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalName() + "\"")
                .body(fileData);
    }

    @PatchMapping("/{fileId}")
    public ResponseEntity<FileMetadataResponse> renameFile(@AuthenticationPrincipal JWTUserData principal,
                                                           @PathVariable Long fileId,
                                                           @Valid @RequestBody RenameRequest request) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(fileService.rename(user, fileId, request.name())));
    }

    @PatchMapping("/{fileId}/move")
    public ResponseEntity<FileMetadataResponse> moveFile(@AuthenticationPrincipal JWTUserData principal,
                                                         @PathVariable Long fileId,
                                                         @RequestBody MoveRequest request) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(fileService.move(user, fileId, request.folderId())));
    }

    @PutMapping("/{fileId}/content")
    public ResponseEntity<FileMetadataResponse> updateContent(@AuthenticationPrincipal JWTUserData principal,
                                                              @PathVariable Long fileId,
                                                              @RequestParam("file") MultipartFile file,
                                                              @RequestParam(value = "compression", required = false, defaultValue = "AUTO") String compression) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(
                fileService.updateContent(user, fileId, file, CompressionMode.parse(compression))));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Map<String, Object>> deleteFile(@AuthenticationPrincipal JWTUserData principal,
                                                          @PathVariable Long fileId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        fileService.softDelete(user, fileId);
        return ResponseEntity.ok(Map.of("deleted", true, "fileID", fileId, "trashed", true));
    }

    @PostMapping("/{fileId}/restore")
    public ResponseEntity<FileMetadataResponse> restoreFile(@AuthenticationPrincipal JWTUserData principal,
                                                            @PathVariable Long fileId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(fileService.restore(user, fileId)));
    }

    @DeleteMapping("/{fileId}/purge")
    public ResponseEntity<Map<String, Object>> purgeFile(@AuthenticationPrincipal JWTUserData principal,
                                                         @PathVariable Long fileId) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        fileService.purge(user, fileId);
        return ResponseEntity.ok(Map.of("purged", true, "fileID", fileId));
    }

    @GetMapping("/{fileId}/versions")
    public ResponseEntity<List<FileVersionResponse>> listVersions(@AuthenticationPrincipal JWTUserData principal,
                                                                  @PathVariable Long fileId) {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(fileService.listVersions(user, fileId).stream()
                .map(FileVersionResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/{fileId}/versions/{versionNumber}/restore")
    public ResponseEntity<FileMetadataResponse> restoreVersion(@AuthenticationPrincipal JWTUserData principal,
                                                               @PathVariable Long fileId,
                                                               @PathVariable Integer versionNumber) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(fileService.restoreVersion(user, fileId, versionNumber)));
    }

    /**
     * Move um ficheiro entre a nuvem do sistema e um dispositivo pessoal
     * (RF21), lendo o conteúdo da réplica atual e voltando a replicá-lo.
     */
    @PostMapping("/{fileId}/move-to-space")
    public ResponseEntity<?> moveToSpace(@AuthenticationPrincipal JWTUserData principal,
                                         @PathVariable Long fileId,
                                         @RequestBody Map<String, Object> body) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        UploadDestination destination = parseDestination(asString(body.get("destination")));
        Long nodeId = asLong(body.get("nodeId"));
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(
                fileService.moveToSpace(user, fileId, destination, nodeId)));
    }

    /**
     * Copia um ficheiro para outro espaço (RF21), criando uma nova entrada que
     * partilha o mesmo conteúdo cifrado.
     */
    @PostMapping("/{fileId}/copy-to-space")
    public ResponseEntity<?> copyToSpace(@AuthenticationPrincipal JWTUserData principal,
                                         @PathVariable Long fileId,
                                         @RequestBody Map<String, Object> body) throws IOException {
        User user = resolveUser(principal);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        UploadDestination destination = parseDestination(asString(body.get("destination")));
        Long nodeId = asLong(body.get("nodeId"));
        return ResponseEntity.ok(FileMetadataResponse.fromEntity(
                fileService.copyToSpace(user, fileId, destination, nodeId)));
    }

    private UploadDestination parseDestination(String value) {
        if (value == null || value.isBlank()) {
            return UploadDestination.AUTO;
        }
        try {
            return UploadDestination.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UploadDestination.AUTO;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private User resolveUser(JWTUserData principal) {
        if (principal == null || principal.userID() == null) {
            return null;
        }
        return userRepository.findById(principal.userID()).orElse(null);
    }
}
