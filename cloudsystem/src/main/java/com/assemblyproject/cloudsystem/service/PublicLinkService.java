package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.dto.request.PublicLinkCreateRequest;
import com.assemblyproject.cloudsystem.dto.response.PublicLinkInfoResponse;
import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.PublicLink;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FolderRepository;
import com.assemblyproject.cloudsystem.repository.PublicLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestão de links públicos (RF15): criação/revogação pelo dono e resolução
 * anónima por token, com suporte a expiração e senha opcional.
 */
@Service
@RequiredArgsConstructor
public class PublicLinkService {

    private final PublicLinkRepository publicLinkRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public PublicLink create(User owner, PublicLinkCreateRequest request) {
        if (request == null || (request.fileId() == null && request.folderId() == null)) {
            throw new IllegalArgumentException("Indica um ficheiro ou uma pasta para gerar o link.");
        }
        if (request.fileId() != null && request.folderId() != null) {
            throw new IllegalArgumentException("Indica apenas um ficheiro ou uma pasta.");
        }

        PublicLink.PublicLinkBuilder builder = PublicLink.builder()
                .token(generateToken())
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .accessCount(0L)
                .revoked(false);

        if (request.fileId() != null) {
            FileMetadata file = fileMetadataRepository.findByFileIDAndOwner(request.fileId(), owner)
                    .orElseThrow(() -> new IllegalArgumentException("Ficheiro inexistente."));
            if (file.getDeletedAt() != null) {
                throw new IllegalStateException("O ficheiro está na lixeira.");
            }
            builder.file(file);
        } else {
            Folder folder = folderRepository.findByFolderIDAndOwner(request.folderId(), owner)
                    .orElseThrow(() -> new IllegalArgumentException("Pasta inexistente."));
            if (folder.getDeletedAt() != null) {
                throw new IllegalStateException("A pasta está na lixeira.");
            }
            builder.folder(folder);
        }

        if (request.expiresInHours() != null) {
            if (request.expiresInHours() <= 0) {
                throw new IllegalArgumentException("A expiração tem de ser positiva.");
            }
            builder.expiresAt(LocalDateTime.now().plusHours(request.expiresInHours()));
        }

        if (request.password() != null && !request.password().isBlank()) {
            builder.passwordHash(passwordEncoder.encode(request.password().trim()));
        }

        return publicLinkRepository.save(builder.build());
    }

    public List<PublicLink> listOwned(User owner) {
        return publicLinkRepository.findAllByOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional
    public PublicLink revoke(User owner, Long linkID) {
        PublicLink link = publicLinkRepository.findByLinkIDAndOwner(linkID, owner)
                .orElseThrow(() -> new IllegalArgumentException("Link inexistente."));
        link.setRevoked(true);
        return publicLinkRepository.save(link);
    }

    @Transactional
    public void revokeAllForFile(FileMetadata file) {
        for (PublicLink link : publicLinkRepository.findAllByFile(file)) {
            link.setRevoked(true);
        }
    }

    @Transactional
    public void revokeAllForFolder(Folder folder) {
        for (PublicLink link : publicLinkRepository.findAllByFolder(folder)) {
            link.setRevoked(true);
        }
    }

    /**
     * Metadados do link para a página pública. Não exige senha (apenas revela o
     * nome/tipo do item). Lança se o link não existir, estiver revogado ou expirado.
     */
    public PublicLinkInfoResponse describe(String token) {
        PublicLink link = requireActive(token);
        if (link.getFile() != null) {
            FileMetadata file = link.getFile();
            return new PublicLinkInfoResponse(
                    "FILE",
                    file.getOriginalName(),
                    file.getMimeType(),
                    file.getOriginalSizeBytes(),
                    displayName(link.getOwner()),
                    link.isPasswordProtected(),
                    link.getExpiresAt());
        }
        Folder folder = link.getFolder();
        return new PublicLinkInfoResponse(
                "FOLDER",
                folder.getName(),
                null,
                null,
                displayName(link.getOwner()),
                link.isPasswordProtected(),
                link.getExpiresAt());
    }

    /**
     * Valida o link e, se protegido, a senha indicada. Devolve o link pronto a usar.
     */
    public PublicLink resolve(String token, String password) {
        PublicLink link = requireActive(token);
        if (link.isPasswordProtected()) {
            if (password == null || !passwordEncoder.matches(password, link.getPasswordHash())) {
                throw new InvalidLinkPasswordException();
            }
        }
        return link;
    }

    @Transactional
    public byte[] download(PublicLink link, Integer versionNumber) throws IOException {
        if (link.getFile() == null) {
            throw new IllegalArgumentException("Este link não aponta para um ficheiro.");
        }
        FileMetadata file = link.getFile();
        if (file.getDeletedAt() != null) {
            throw new IllegalStateException("O ficheiro já não está disponível.");
        }
        byte[] data = fileService.downloadMetadata(file, versionNumber);
        recordAccess(link);
        return data;
    }

    public FileMetadata fileOf(PublicLink link) {
        return link.getFile();
    }

    @Transactional
    public void recordAccess(PublicLink link) {
        link.setAccessCount((link.getAccessCount() == null ? 0L : link.getAccessCount()) + 1);
        link.setLastAccessAt(LocalDateTime.now());
        publicLinkRepository.save(link);
    }

    /**
     * Lista o conteúdo de uma pasta partilhada publicamente. {@code folderId}
     * tem de ser a própria raiz do link ou uma subpasta sua.
     */
    public List<Folder> listFolders(PublicLink link, Long folderId) {
        Folder root = link.getFolder();
        if (root == null) {
            throw new IllegalArgumentException("Este link não aponta para uma pasta.");
        }
        Folder target = resolveSubfolder(root, folderId);
        return folderRepository.findAllByOwnerAndParentAndDeletedAtIsNullOrderByNameAsc(target.getOwner(), target);
    }

    public List<FileMetadata> listFiles(PublicLink link, Long folderId) {
        Folder root = link.getFolder();
        if (root == null) {
            throw new IllegalArgumentException("Este link não aponta para uma pasta.");
        }
        Folder target = resolveSubfolder(root, folderId);
        return fileMetadataRepository
                .findAllByOwnerAndFolderAndDeletedAtIsNullOrderByUploadedAtDesc(target.getOwner(), target);
    }

    private Folder resolveSubfolder(Folder root, Long folderId) {
        if (root.getDeletedAt() != null) {
            throw new IllegalStateException("A pasta já não está disponível.");
        }
        if (folderId == null || folderId.equals(root.getFolderID())) {
            return root;
        }
        Folder candidate = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Pasta inexistente."));
        if (candidate.getDeletedAt() != null || !isWithin(root, candidate)) {
            throw new IllegalArgumentException("Pasta inexistente.");
        }
        return candidate;
    }

    /** Confirma que {@code candidate} é a raiz ou um descendente de {@code root}. */
    private boolean isWithin(Folder root, Folder candidate) {
        Folder current = candidate;
        int guard = 0;
        while (current != null && guard++ < 1000) {
            if (current.getFolderID().equals(root.getFolderID())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private PublicLink requireActive(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Link inválido.");
        }
        PublicLink link = publicLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link inválido."));
        if (link.isRevoked()) {
            throw new IllegalStateException("Este link foi revogado.");
        }
        if (link.isExpired()) {
            throw new IllegalStateException("Este link expirou.");
        }
        return link;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String displayName(User user) {
        return user.getName() + " " + user.getSurname();
    }

    /** Sinaliza senha incorreta ou em falta num link protegido. */
    public static class InvalidLinkPasswordException extends RuntimeException {
        public InvalidLinkPasswordException() {
            super("Senha do link incorreta.");
        }
    }
}
