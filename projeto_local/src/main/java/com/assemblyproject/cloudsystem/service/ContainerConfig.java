package com.assemblyproject.cloudsystem.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "cloud.container")
public class ContainerConfig {
    private String extension = ".dbs";
    private String masterKey = "EvguenyEwenAllenEdmundoMalik";
    private String storageDir = "./containers";
    private String magic = "DBS1";
}
