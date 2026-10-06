package com.assemblyproject.cloudsystem.controller;

import com.assemblyproject.cloudsystem.entity.NodeStatus;
import com.assemblyproject.cloudsystem.entity.StorageNode;
import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.StorageNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nodes")
public class NodeController {

    private final StorageNodeRepository nodeRepository;

    @PostMapping("/register")
    public ResponseEntity<StorageNode> registerNode(@RequestParam("name") String name,
                                                    @RequestParam(value = "totalSpaceBytes", required = false) Long totalSpaceBytes,
                                                    @AuthenticationPrincipal User currentUser) {
        Optional<StorageNode> existing = nodeRepository.findAllByOwner(currentUser).stream().findFirst();
        if (existing.isPresent()) {
            return ResponseEntity.ok(existing.get());
        }

        StorageNode node = StorageNode.builder()
                .owner(currentUser)
                .name(name)
                .totalSpaceBytes(totalSpaceBytes != null ? totalSpaceBytes : 0L)
                .status(NodeStatus.ONLINE)
                .lastHeartbeat(LocalDateTime.now())
                .build();

        StorageNode saved = nodeRepository.save(node);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<StorageNode>> listNodes(@AuthenticationPrincipal User currentUser) {
        List<StorageNode> nodes = nodeRepository.findAllByOwner(currentUser);
        return ResponseEntity.ok(nodes);
    }

    @PostMapping("/{nodeId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable Long nodeId) {
        Optional<StorageNode> optional = nodeRepository.findById(nodeId);
        if (optional.isPresent()) {
            StorageNode node = optional.get();
            node.setStatus(NodeStatus.ONLINE);
            node.setLastHeartbeat(LocalDateTime.now());
            nodeRepository.save(node);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}