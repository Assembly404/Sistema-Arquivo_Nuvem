package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    User getCurrentUser();
}