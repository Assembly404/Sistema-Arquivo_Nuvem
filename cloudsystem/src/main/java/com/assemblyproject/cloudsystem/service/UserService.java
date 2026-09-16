package com.assemblyproject.cloudsystem.service;

import com.assemblyproject.cloudsystem.entity.User;
import com.assemblyproject.cloudsystem.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public Optional<User> createUser(User user) {
        List<User> verifyUserEmail = userRepository.findAll();

        for (User u : verifyUserEmail) {
            if (user.getEmail().equalsIgnoreCase(u.getEmail())) {
                throw new RuntimeException("Já existe um usuário com esse email.");
            }
        }

        LocalDateTime dateTime = LocalDateTime.now();
        user.setRegisterDate(dateTime);

        user.setPassword(encoder.encode(user.getPassword()));

        userRepository.save(user);

        Optional<User> validate;

        return validate = userRepository.findByEmail(user.getEmail());
    }

    public List<User> readAll() {
        return userRepository.findAll();
    }

    public Optional<User> readOne(long id) {
        if (id == 0) throw new IllegalArgumentException("Não existe alguem com esse id");
        return userRepository.findById(id);
    }

    public Optional<User> login(User user) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (user == null) return Optional.empty();

        Optional<User> userByEmail = userRepository.findByEmail(user.getEmail());

        if (userByEmail.isEmpty()) {
            return Optional.empty();
        }

        return userByEmail;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with email: " + username));
        return new UserInfoDetails(user);
    }
}
