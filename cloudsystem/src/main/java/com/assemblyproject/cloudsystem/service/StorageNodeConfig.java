package com.assemblyproject.cloudsystem.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cloud.node")
public class StorageNodeConfig {
    private String storageDir = "./node-storage";
    private int serverPort = 8081;
    private String serverAddress = "0.0.0.0";
}
