package com.assemblyproject.cloudsystem.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenConfig tokenConfig;

    public SecurityFilter(TokenConfig tokenConfig) {
        this.tokenConfig = tokenConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authorizedHeader = request.getHeader("Authorization");

        if (Strings.isNotEmpty(authorizedHeader) && authorizedHeader.startsWith("Bearer ")) {
            String token = authorizedHeader.substring("Bearer ".length());
            Optional<JWTUserData> optUser = tokenConfig.validateToken(token);


            if (optUser.isPresent()) {
                JWTUserData userData = optUser.get();
                String role = userData.role() != null ? userData.role() : "USER";

                // Re-verificar o estado de suspensão do utilizador a cada pedido
                // (RF06 — exclusão de conta / suspensão deve ser respeitada imediatamente)
                if (!userData.suspended()) {
                    UsernamePasswordAuthenticationToken userAndPass = new UsernamePasswordAuthenticationToken(
                            userData, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                    SecurityContextHolder.getContext().setAuthentication(userAndPass);
                }
            }

            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
