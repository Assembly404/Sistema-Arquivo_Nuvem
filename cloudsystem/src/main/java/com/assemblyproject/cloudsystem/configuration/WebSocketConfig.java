package com.assemblyproject.cloudsystem.configuration;

/*
 * WebSocket configuration disabled for deploy simplicity.
 * Re-enable by adding spring-boot-starter-websocket dependency and uncommenting:
 */
/*
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(org.springframework.messaging.simp.config.MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-nodes")
                .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173");
    }
}
*/

/** Placeholder to keep package structure without websocket dependency */
public class WebSocketConfig {
    // WebSocket disabled
}