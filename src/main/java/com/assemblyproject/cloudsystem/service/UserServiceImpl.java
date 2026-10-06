package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public User getCurrentUser() {
        // Implementação temporária - em produção real usar SecurityContextHolder
        return null;
    }
}