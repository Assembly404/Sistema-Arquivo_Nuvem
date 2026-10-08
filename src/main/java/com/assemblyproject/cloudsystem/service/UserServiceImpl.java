package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User getCurrentUser() {
        return null;
    }

    @Override
    public User createAdmin(User user) {
        User currentUser = userRepository.findById(user.getUserID()).get();

        if (currentUser == null) {
            return currentUser;
        }

//        currentUser.setRole();

        return user;
    }
}