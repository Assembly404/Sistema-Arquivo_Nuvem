package com.assemblyproject.cloudsystem.dto.response;

import com.assemblyproject.cloudsystem.entity.ReplicaStatus;
import lombok.Data;

@Data
public class ReplicaInfo {
    private Long nodeID;
    private String nodeName;
    private ReplicaStatus status;

    public ReplicaInfo(Long nodeID, String nodeName, ReplicaStatus status) {
        this.nodeID = nodeID;
        this.nodeName = nodeName;
        this.status = status;
    }
}