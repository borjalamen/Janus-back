package com.janushub.service;

import org.springframework.stereotype.Service;

import com.janushub.model.Users;
import com.janushub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // ---------- AVATAR ----------
    public void updateAvatar(String username, String path) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setAvatarPath(path);
        userRepository.save(user);
    }

    public String getAvatarPath(String username) {
        return userRepository.findByUsername(username)
                .map(Users::getAvatarPath)
                .orElse(null);
    }

    public void removeAvatar(String username) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setAvatarPath(null);
        userRepository.save(user);
    }

    // ---------- CV ----------
    public void updateCv(String username, String path) {
        Users user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setCvPath(path);
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
        userRepository.save(user);
    }
}
