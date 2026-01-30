package com.janushub.service;

import org.springframework.stereotype.Service;

import com.janushub.model.Users;
import com.janushub.repository.UserRepository;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private Users getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    // ---------- AVATAR ----------
    public void updateAvatar(String username, String path) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setAvatarPath(path);
         user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public String getAvatarPath(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getAvatarPath)
                .orElse(null);
    }

    public void removeAvatar(String username) {
        Users user = getUserOrThrow(username);

        user.setAvatarPath(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ---------- CV ----------
    public void updateCv(String username, String path) {
    Users user = getUserOrThrow(username);

    user.setCvPath(path);
    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);
}

    public String getCvPath(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getCvPath)
                .orElse(null);
    }

    public void removeCv(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setCvPath(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }
}
