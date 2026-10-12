package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.dto.response.FileMetadataResponse;
import com.assemblyproject.cloudsystem.dto.response.FolderResponse;
import com.assemblyproject.cloudsystem.dto.response.PublicLinkInfoResponse;
import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.PublicLink;
import com.assemblyproject.cloudsystem.service.PublicLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Acesso anónimo a itens partilhados por link público (RF15). Não exige
 * autenticação; a autorização é o token (e, se aplicável, a senha).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/links")
public class PublicAccessController {

    private final PublicLinkService publicLinkService;

    @GetMapping("/{token}")
    public ResponseEntity<PublicLinkInfoResponse> info(@PathVariable String token) {
        return ResponseEntity.ok(publicLinkService.describe(token));
    }

    @PostMapping("/{token}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable String token,
                                                      @RequestParam(value = "password", required = false) String password) {
        publicLinkService.resolve(token, password);
        return ResponseEntity.ok(Map.of("valid", true));
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<byte[]> download(@PathVariable String token,
                                           @RequestParam(value = "password", required = false) String password,
                                           @RequestParam(value = "version", required = false) Integer version) throws IOException {
        PublicLink link = publicLinkService.resolve(token, password);
        FileMetadata file = publicLinkService.fileOf(link);
        byte[] data = publicLinkService.download(link, version);
        String name = file != null ? sanitizeFilename(file.getOriginalName()) : "download";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(data);
    }

    @GetMapping("/{token}/contents")
    public ResponseEntity<Map<String, Object>> contents(@PathVariable String token,
                                                        @RequestParam(value = "folderId", required = false) Long folderId,
                                                        @RequestParam(value = "password", required = false) String password) {
        PublicLink link = publicLinkService.resolve(token, password);
        List<FolderResponse> folders = publicLinkService.listFolders(link, folderId).stream()
                .map(FolderResponse::from)
                .collect(Collectors.toList());
        List<FileMetadataResponse> files = publicLinkService.listFiles(link, folderId).stream()
                .map(FileMetadataResponse::fromEntity)
                .collect(Collectors.toList());
        publicLinkService.recordAccess(link);
        return ResponseEntity.ok(Map.of("folders", folders, "files", files));
    }

    /** Remove caracteres que permitiriam injeção de cabeçalhos na resposta. */
    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "download";
        }
        return name.replaceAll("[\\r\\n\"\\\\]", "_").trim();
    }
}
