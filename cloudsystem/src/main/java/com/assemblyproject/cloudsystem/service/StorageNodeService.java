package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.NodeAccess;
import com.assemblyproject.cloudsystem.entity.NodeVisibility;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.NodeAccessRepository;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorageNodeService {

    private final StorageNodeRepository nodeRepository;
    private final NodeAccessRepository nodeAccessRepository;
    private final StorageNodeConfig config;

    public Path getNodeStorageDir(StorageNode node) {
        Path dir = Paths.get(config.getStorageDir(), String.valueOf(node.getNodeID()));
        return dir;
    }

    public Path saveReplica(StorageNode node, Long fileID, byte[] data) throws IOException {
        Path dir = getNodeStorageDir(node);
        Files.createDirectories(dir);
        Path file = dir.resolve(fileID + ".dbs");
        Files.write(file, data);
        return file;
    }

    public byte[] readReplica(StorageNode node, Long fileID) throws IOException {
        Path path = getNodeStorageDir(node).resolve(fileID + ".dbs");
        if (!Files.exists(path)) throw new IOException("Replica not found on node: " + path);
        return Files.readAllBytes(path);
    }

    public boolean hasReplica(StorageNode node, Long fileID) {
        return Files.exists(getNodeStorageDir(node).resolve(fileID + ".dbs"));
    }

    public long getStoredBytes(StorageNode node) throws IOException {
        Path dir = getNodeStorageDir(node);
        if (!Files.exists(dir)) return 0L;
        final long[] total = {0L};
        Files.list(dir).filter(Files::isRegularFile).forEach(p -> {
            try { total[0] += Files.size(p); } catch (IOException ignored) {}
        });
        return total[0];
    }

    public void deleteReplica(StorageNode node, Long fileID) throws IOException {
        Path path = getNodeStorageDir(node).resolve(fileID + ".dbs");
        Files.deleteIfExists(path);
    }

    public boolean canAccess(StorageNode node, User user) {
        if (node == null) return false;
        if (node.isSystem()) return true;
        if (user == null) return false;
        if (node.getOwner() != null && node.getOwner().getUserID().equals(user.getUserID())) return true;
        if (node.getVisibility() == NodeVisibility.PUBLIC) return true;
        if (node.getVisibility() == NodeVisibility.PRIVATE) {
            return nodeAccessRepository.findByNodeAndGrantedUser(node, user).isPresent();
        }
        return false;
    }

    public List<NodeAccess> getGrantedUsers(StorageNode node) {
        return nodeAccessRepository.findAllByNode(node);
    }

    public NodeAccess grantAccess(StorageNode node, User user, LocalDateTime expiresAt) {
        NodeAccess access = NodeAccess.builder()
                .node(node)
                .grantedUser(user)
                .expiresAt(expiresAt)
                .grantedAt(LocalDateTime.now())
                .build();
        return nodeAccessRepository.save(access);
    }

    public void revokeAccess(StorageNode node, User user) {
        nodeAccessRepository.findByNodeAndGrantedUser(node, user)
                .ifPresent(nodeAccessRepository::delete);
    }

    /**
     * Recalcula o espaço usado de um nó a partir do conteúdo em disco.
     */
    public long refreshUsedSpace(StorageNode node) {
        try {
            long used = getStoredBytes(node);
            node.setUsedSpaceBytes(used);
            return used;
        } catch (IOException e) {
            return node.getUsedSpaceBytes() == null ? 0L : node.getUsedSpaceBytes();
        }
    }

    /**
     * Apaga todo o armazenamento local associado a um nó (usado na remoção de um
     * dispositivo após a migração das réplicas). Devolve o número de ficheiros
     * eliminados.
     */
    public int deleteNodeStorage(StorageNode node) {
        Path dir = getNodeStorageDir(node);
        if (!Files.exists(dir)) {
            return 0;
        }
        final int[] removed = {0};
        try {
            Files.list(dir).filter(Files::isRegularFile).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                    removed[0]++;
                } catch (IOException ignored) {
                    // ficheiro já removido
                }
            });
            Files.deleteIfExists(dir);
        } catch (IOException ignored) {
            // diretório já removido
        }
        return removed[0];
    }
}
