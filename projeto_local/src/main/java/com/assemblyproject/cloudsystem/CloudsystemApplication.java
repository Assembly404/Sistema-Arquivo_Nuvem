package com.assemblyproject.cloudsystem;

import com.assemblyproject.cloudsystem.service.ContainerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ContainerConfig.class)
@EnableScheduling
@EnableAsync
public class CloudsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CloudsystemApplication.class, args);
	}

}
