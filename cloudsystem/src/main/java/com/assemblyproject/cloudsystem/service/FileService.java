package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.FileMetadata;
import com.assemblyproject.cloudsystem.entity.FileReplica;
import com.assemblyproject.cloudsystem.entity.FileVersion;
import com.assemblyproject.cloudsystem.entity.Folder;
import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.UploadDestination;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.FileMetadataRepository;
import com.assemblyproject.cloudsystem.repository.FileReplicaRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileMetadataRepository fileMetadataRepository;
    private final FileReplicaRepository fileReplicaRepository;
    private final ContainerService containerService;
    private final ContainerClient containerClient;
    private final ReplicationService replicationService;
    private final FileVersionService versionService;
    private final FolderService folderService;
    private final ShareService shareService;
    private final PurgeService purgeService;

    public List<FileMetadata> listByFolder(User owner, Long folderId) {
        if (folderId == null) {
            return fileMetadataRepository.findAllByOwnerAndFolderIsNullAndDeletedAtIsNullOrderByUploadedAtDesc(owner);
        }
        Folder folder = folderService.requireOwned(owner, folderId);
        return fileMetadataRepository.findAllByOwnerAndFolderAndDeletedAtIsNullOrderByUploadedAtDesc(owner, folder);
    }

    public List<FileMetadata> search(User owner, String name, String mime,
                                     LocalDateTime from, LocalDateTime to) {
        return fileMetadataRepository.search(owner,
                blankToNull(name), blankToNull(mime), from, to);
    }

    public List<FileMetadata> listTrash(User owner) {
        return fileMetadataRepository.findAllByOwnerAndDeletedAtIsNotNullOrderByDeletedAtDesc(owner);
    }

    @Transactional
    public FileMetadata rename(User owner, Long fileId, String name) {
        FileMetadata metadata = requireOwned(owner, fileId);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do ficheiro e obrigatorio.");
        }
        metadata.setOriginalName(name.trim());
        metadata.setUpdatedAt(LocalDateTime.now());
        return fileMetadataRepository.save(metadata);
    }

    @Transactional
    public FileMetadata move(User owner, Long fileId, Long folderId) {
        FileMetadata metadata = requireOwned(owner, fileId);
        Folder folder = folderId != null ? folderService.requireOwned(owner, folderId) : null;
        if (folder != null && folder.getDeletedAt() != null) {
            throw new IllegalArgumentException("A pasta de destino esta na lixeira.");
        }
        metadata.setFolder(folder);
        metadata.setUpdatedAt(LocalDateTime.now());
        return fileMetadataRepository.save(metadata);
    }

    @Transactional
    public void softDelete(User owner, Long fileId) {
        FileMetadata metadata = requireOwned(owner, fileId);
        metadata.setDeletedAt(LocalDateTime.now());
        fileMetadataRepository.save(metadata);
    }

    @Transactional
    public FileMetadata restore(User owner, Long fileId) {
        FileMetadata metadata = requireOwned(owner, fileId);
        if (metadata.getDeletedAt() == null) {
            throw new IllegalStateException("O ficheiro nao esta na lixeira.");
        }
        metadata.setDeletedAt(null);
        if (metadata.getFolder() != null && metadata.getFolder().getDeletedAt() != null) {
            metadata.setFolder(null);
        }
        return fileMetadataRepository.save(metadata);
    }

    @Transactional
    public void purge(User owner, Long fileId) throws IOException {
        FileMetadata metadata = requireOwned(owner, fileId);
        purgeService.purgeFile(metadata);
    }

    @Transactional
    public FileMetadata updateContent(User owner, Long fileId, MultipartFile upload) throws IOException {
        FileMetadata metadata = requireOwned(owner, fileId);
        ContainerService.PreparedContainer prepared = containerService.prepare(upload);

        byte[] current = tryReadCurrentContainer(metadata);
        if (current != null) {
            versionService.snapshot(metadata, current);
        } else {
            log.warn("Sem replica online para versionar o ficheiro {}; versao anterior nao arquivada.", fileId);
        }

        applyPrepared(metadata, prepared);
        metadata.setUpdatedAt(LocalDateTime.now());
        metadata.setContainerVersion((metadata.getContainerVersion() == null ? 1 : metadata.getContainerVersion()) + 1);
        FileMetadata saved = fileMetadataRepository.save(metadata);

        byte[] container = prepared.containerData();
        replaceReplicas(saved, owner, container);
        return saved;
    }

    public List<FileVersion> listVersions(User owner, Long fileId) {
        return versionService.list(requireOwned(owner, fileId));
    }

    @Transactional
    public FileMetadata restoreVersion(User owner, Long fileId, Integer versionNumber) throws IOException {
        FileMetadata metadata = requireOwned(owner, fileId);
        FileVersion version = versionService.find(metadata, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Versao inexistente."));
        byte[] versionContainer = versionService.readContainer(version);

        byte[] current = tryReadCurrentContainer(metadata);
        if (current != null) {
            versionService.snapshot(metadata, current);
        }

        metadata.setOriginalName(version.getOriginalName());
        metadata.setMimeType(version.getMimeType());
        metadata.setOriginalSizeBytes(version.getOriginalSizeBytes());
        metadata.setStoredSizeBytes(version.getStoredSizeBytes());
        metadata.setChecksumSha256(version.getChecksumSha256());
        metadata.setEncryptedDataKey(version.getEncryptedDataKey());
        metadata.setKeyEncryptionIv(version.getKeyEncryptionIv());
        metadata.setUpdatedAt(LocalDateTime.now());
        metadata.setContainerVersion((metadata.getContainerVersion() == null ? 1 : metadata.getContainerVersion()) + 1);
        FileMetadata saved = fileMetadataRepository.save(metadata);

        replaceReplicas(saved, owner, versionContainer);
        return saved;
    }

    public byte[] download(User owner, Long fileId, Integer versionNumber) throws IOException {
        return download(requireOwned(owner, fileId), versionNumber);
    }

    /**
     * Move o conteúdo de um ficheiro entre a nuvem do sistema e um dispositivo
     * pessoal (RF21). Lê a réplica atual e volta a colocar o ficheiro no destino.
     */
    @Transactional
    public FileMetadata moveToSpace(User owner, Long fileId, UploadDestination destination, Long targetNodeId)
            throws IOException {
        FileMetadata metadata = requireOwned(owner, fileId);
        byte[] container = readCurrentContainer(metadata);
        // Reproduz primeiro no destino e só depois remove as réplicas antigas,
        // para que nunca exista um instante sem qualquer cópia persistida.
        List<StorageNode> stored = replicationService.replicateFile(
                fileId, owner, container, destination, targetNodeId);
        replicationService.deleteReplicasExcept(fileId, replicationService.nodeIds(stored));
        metadata.setUpdatedAt(LocalDateTime.now());
        return fileMetadataRepository.save(metadata);
    }

    /**
     * Substitui as réplicas do ficheiro por novas cópias (feito para
     * {@code updateContent}/{@code restoreVersion}): replica primeiro para o
     * espaço onde o ficheiro já reside e só apaga as réplicas antigas depois de
     * as novas estarem gravadas com sucesso.
     */
    private void replaceReplicas(FileMetadata metadata, User owner, byte[] container) throws IOException {
        StorageNode current = firstReplicaNode(metadata);
        UploadDestination destination = UploadDestination.AUTO;
        Long targetNodeId = null;
        if (current != null) {
            if (current.isSystem()) {
                destination = UploadDestination.SYSTEM;
            } else if (current.isEnabled() && current.getStatus() == NodeStatus.ONLINE) {
                destination = UploadDestination.DEVICE;
                targetNodeId = current.getNodeID();
            } else {
                log.warn("Dispositivo {} offline/desativado ao atualizar o ficheiro {}; "
                        + "nova versão será colocada na nuvem do sistema.",
                        current.getNodeID(), metadata.getFileID());
            }
        }
        List<StorageNode> stored = replicationService.replicateFile(
                metadata.getFileID(), owner, container, destination, targetNodeId);
        replicationService.deleteReplicasExcept(metadata.getFileID(), replicationService.nodeIds(stored));
    }

    private StorageNode firstReplicaNode(FileMetadata metadata) {
        for (FileReplica replica : fileReplicaRepository.findAllByFile(metadata)) {
            if (replica.getNode() != null) {
                return replica.getNode();
            }
        }
        return null;
    }

    /**
     * Copia um ficheiro para outro espaço (RF21), criando uma nova entrada que
     * reutiliza o mesmo conteúdo cifrado.
     */
    @Transactional
    public FileMetadata copyToSpace(User owner, Long fileId, UploadDestination destination, Long targetNodeId)
            throws IOException {
        FileMetadata source = requireOwned(owner, fileId);
        byte[] container = readCurrentContainer(source);

        FileMetadata copy = FileMetadata.builder()
                .owner(owner)
                .folder(source.getFolder())
                .originalName(buildCopyName(source.getOriginalName()))
                .mimeType(source.getMimeType())
                .originalSizeBytes(source.getOriginalSizeBytes())
                .storedSizeBytes(source.getStoredSizeBytes())
                .checksumSha256(source.getChecksumSha256())
                .encryptedDataKey(source.getEncryptedDataKey())
                .keyEncryptionIv(source.getKeyEncryptionIv())
                .uploadedAt(LocalDateTime.now())
                .build();
        FileMetadata saved = fileMetadataRepository.save(copy);
        replicationService.replicateFile(saved.getFileID(), owner, container, destination, targetNodeId);
        return saved;
    }

    private String buildCopyName(String original) {
        if (original == null || original.isBlank()) {
            return "copia";
        }
        int dot = original.lastIndexOf('.');
        if (dot > 0) {
            return original.substring(0, dot) + " (copia)" + original.substring(dot);
        }
        return original + " (copia)";
    }

    /**
     * Comprime o ficheiro com DEFLATE (se ainda não estiver comprimido).
     * Reprocessa o container e replica no mesmo espaço.
     */
    @Transactional
    public FileMetadata compressFile(User owner, Long fileId) throws IOException {
        FileMetadata metadata = requireOwned(owner, fileId);
        String currentAlgo = metadata.getCompressionAlgorithm();
        if (currentAlgo != null && !"NONE".equalsIgnoreCase(currentAlgo)) {
            throw new IllegalStateException("O ficheiro já está comprimido (" + currentAlgo + ").");
        }
        byte[] container = readCurrentContainer(metadata);
        byte[] fileKey = containerService.resolveFileKey(metadata);
        byte[] rawData = containerService.extractContainer(container, fileKey);
        // Reprocessa com compressão DEFLATE forçada
        ContainerService.PreparedContainer prepared = containerService.prepare(
                rawData, metadata.getOriginalName(), metadata.getMimeType(),
                metadata.getOriginalSizeBytes(), CompressionMode.DEFLATE);
        replaceReplicas(metadata, owner, prepared.containerData());
        applyPrepared(metadata, prepared);
        metadata.setCompressionAlgorithm(CompressionAlgorithm.DEFLATE.name());
        metadata.setUpdatedAt(LocalDateTime.now());
        return fileMetadataRepository.save(metadata);
    }

    /**
     * Descomprime o ficheiro (guarda como NONE).
     */
    @Transactional
    public FileMetadata decompressFile(User owner, Long fileId) throws IOException {
        FileMetadata metadata = requireOwned(owner, fileId);
        String currentAlgo = metadata.getCompressionAlgorithm();
        if (currentAlgo == null || "NONE".equalsIgnoreCase(currentAlgo)) {
            throw new IllegalStateException("O ficheiro não está comprimido.");
        }
        byte[] container = readCurrentContainer(metadata);
        byte[] fileKey = containerService.resolveFileKey(metadata);
        byte[] rawData = containerService.extractContainer(container, fileKey);
        // Reprocessa sem compressão
        ContainerService.PreparedContainer prepared = containerService.prepare(
                rawData, metadata.getOriginalName(), metadata.getMimeType(),
                metadata.getOriginalSizeBytes(), CompressionMode.NONE);
        replaceReplicas(metadata, owner, prepared.containerData());
        applyPrepared(metadata, prepared);
        metadata.setCompressionAlgorithm(CompressionAlgorithm.NONE.name());
        metadata.setUpdatedAt(LocalDateTime.now());
        return fileMetadataRepository.save(metadata);
    }

    /**
     * Descarrega um ficheiro que o utilizador pode ler porque é dono OU porque
     * lhe foi partilhado (VIEW/EDIT). Não permite operações de escrita.
     */
    public byte[] downloadShared(User user, Long fileId, Integer versionNumber) throws IOException {
        return download(requireReadable(user, fileId), versionNumber);
    }

    /**
     * Descarrega o conteúdo de um ficheiro sem verificar o dono. Destina-se a
     * fluxos já autorizados por outro meio (link público - RF15).
     */
    public byte[] downloadMetadata(FileMetadata metadata, Integer versionNumber) throws IOException {
        return download(metadata, versionNumber);
    }

    public FileMetadata requireReadable(User user, Long fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("Ficheiro inexistente."));
        if (shareService.accessToFile(user, metadata) == null) {
            throw new IllegalArgumentException("Ficheiro inexistente.");
        }
        return metadata;
    }

    private byte[] download(FileMetadata metadata, Integer versionNumber) throws IOException {
        if (versionNumber != null) {
            FileVersion version = versionService.find(metadata, versionNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Versao inexistente."));
            byte[] container = versionService.readContainer(version);
            byte[] key = containerService.unwrapStoredKey(version.getEncryptedDataKey(), version.getKeyEncryptionIv());
            return containerService.extractContainer(container, key);
        }
        byte[] container = readCurrentContainer(metadata);
        byte[] key = containerService.resolveFileKey(metadata);
        return containerService.extractContainer(container, key);
    }

    public FileMetadata requireOwned(User owner, Long fileId) {
        return fileMetadataRepository.findByFileIDAndOwner(fileId, owner)
                .orElseThrow(() -> new IllegalArgumentException("Ficheiro inexistente."));
    }

    private byte[] readCurrentContainer(FileMetadata metadata) throws IOException {
        StorageNode node = pickOnlineNodeWithReplica(metadata);
        if (node == null) {
            List<FileReplica> replicas = fileReplicaRepository.findAllByFile(metadata);
            if (!replicas.isEmpty()) {
                throw new DeviceOfflineException(
                        "O ficheiro \"" + metadata.getOriginalName()
                                + "\" está guardado num dispositivo offline. "
                                + "Liga o dispositivo para o aceder.", null);
            }
            throw new IOException("Nenhum no online com replica do ficheiro " + metadata.getFileID());
        }
        return containerClient.downloadReplica(node, metadata.getFileID());
    }

    private byte[] tryReadCurrentContainer(FileMetadata metadata) {
        try {
            return readCurrentContainer(metadata);
        } catch (IOException e) {
            return null;
        }
    }

    private StorageNode pickOnlineNodeWithReplica(FileMetadata metadata) {
        List<FileReplica> replicas = fileReplicaRepository.findAllByFileAndStatus(metadata, ReplicaStatus.SYNCED);
        for (FileReplica replica : replicas) {
            StorageNode node = replica.getNode();
            if (node != null && node.getStatus() == NodeStatus.ONLINE) {
                return node;
            }
        }
        return null;
    }

    private void applyPrepared(FileMetadata metadata, ContainerService.PreparedContainer prepared) {
        metadata.setOriginalName(prepared.originalName());
        metadata.setMimeType(prepared.mimeType());
        metadata.setOriginalSizeBytes(prepared.originalSizeBytes());
        metadata.setStoredSizeBytes(prepared.storedSizeBytes());
        metadata.setChecksumSha256(prepared.checksumSha256());
        metadata.setEncryptedDataKey(prepared.encryptedDataKey());
        metadata.setKeyEncryptionIv(prepared.keyEncryptionIv());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
