package com.assemblyproject.cloudsystem.configuration;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Timeout de ligação para chamadas entre o servidor e os nós de
     * armazenamento. Evita que um dispositivo offline/inacessível (ex.: atrás
     * de NAT) bloqueie threads durante o timeout por omissão do sistema
     * operativo (~2 min), que tornava o registo, a replicação e o heartbeat
     * lentos.
     */
    @Value("${cloud.node.connect-timeout:5000}")
    private int nodeConnectTimeoutMillis;

    @Value("${cloud.node.read-timeout:30000}")
    private int nodeReadTimeoutMillis;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configure(http))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health").permitAll()
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/password/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/nodes/jar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/install/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/nodes/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/nodes/*/heartbeat").permitAll()
                        // O agente anuncia o seu endereço autenticando-se pelo
                        // token do dispositivo (X-Node-Token), não por JWT.
                        .requestMatchers(HttpMethod.POST, "/nodes/*/address").permitAll()
                        // Leitura do catálogo (perfis/recursos/permissões) é permitida a qualquer acesso de administração
                        .requestMatchers(HttpMethod.GET, "/admin/papeis/**", "/admin/recursos/**", "/admin/permissoes/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN_ACESSAR", "ADMIN_USUARIOS", "ADMIN_PERMISSOES")
                        // Alterações a perfis/recursos/permissões exigem ADMIN_PERMISSOES
                        .requestMatchers("/admin/papeis/**", "/admin/recursos/**", "/admin/permissoes/**", "/admin/permissions/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN_PERMISSOES")
                        .requestMatchers("/admin/users/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN_USUARIOS")
                        .requestMatchers("/admin/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ADMIN_ACESSAR")
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean 
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:5500",
            "http://127.0.0.1:5500",
            "https://debesis-sage.vercel.app/",
            "https://debesis-i47jpwogv-projetositc2026-9121.vercel.app/",
            "https://debesis-git-main-projetositc2026-9121.vercel.app/"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(nodeConnectTimeoutMillis));
        factory.setReadTimeout(Duration.ofMillis(nodeReadTimeoutMillis));
        return new RestTemplate(factory);
    }
}