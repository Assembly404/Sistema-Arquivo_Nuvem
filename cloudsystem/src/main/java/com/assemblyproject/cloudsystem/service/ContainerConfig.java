package com.assemblyproject.cloudsystem.service;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "cloud.container")
public class ContainerConfig {
    private String extension = ".dbs";
    private String masterKey = "6jE/QADMd/vCKeyzyq9GRUOiSfrssVSY2AZgwZL8BHQ=";
    private String storageDir = "./containers";
    private String magic = "DBS1";
}
