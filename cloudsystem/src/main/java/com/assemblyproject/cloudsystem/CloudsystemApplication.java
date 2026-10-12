package com.assemblyproject.cloudsystem;

import com.assemblyproject.cloudsystem.agent.NodeAgentApplication;
import com.assemblyproject.cloudsystem.service.ContainerConfig;
import java.util.Arrays;
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
        if (isAgentMode(args)) {
            NodeAgentApplication.main(args);
            return;
        }
        SpringApplication.run(CloudsystemApplication.class, args);
    }

    private static boolean isAgentMode(String[] args) {
        return Arrays.stream(args)
                .anyMatch(a -> a.equals("--node.agent")
                        || a.equals("--node.agent=true")
                        || a.equals("--agent"));
    }
}
