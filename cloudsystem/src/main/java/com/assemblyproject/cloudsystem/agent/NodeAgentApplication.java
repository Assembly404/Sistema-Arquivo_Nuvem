package com.assemblyproject.cloudsystem.agent;

import com.assemblyproject.cloudsystem.service.StandaloneNodeServer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableScheduling;
import org.springframework.context.annotation.Import;

/**
 * Aplicação dedicada ao agente de armazenamento (nó pessoal).
 * <p>
 * Corre apenas o servidor HTTP local que serve as réplicas do dispositivo,
 * sem JPA, sem base de dados, sem controllers REST e sem segurança web.
 * Regista-se automaticamente no servidor CloudSystem através de um código
 * de pareamento gerado no frontend.
 * <p>
 * Uso:
 * <pre>
 * java -jar cloudsystem-node.jar --node.agent=true \
 *   --cloud.node.server-url=https://cloudsystem-backend.onrender.com \
 *   --cloud.node.server-port=8081 \
 *   --cloud.node.storage-dir=/home/user/cloudstorage \
 *   --cloud.node.auto-register=true \
 *   --cloud.node.pairing-code=A7K9M2P4
 * </pre>
 */
@Configuration
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@Import(StandaloneNodeServer.class)
@EnableScheduling
public class NodeAgentApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(NodeAgentApplication.class)
                .web(WebApplicationType.NONE)
                .properties("spring.config.location=optional:classpath:/application-node.properties,optional:./node.properties")
                .sources(NodeAgentApplication.class)
                .run(args);
    }
}
