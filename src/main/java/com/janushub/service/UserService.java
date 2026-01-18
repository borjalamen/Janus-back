package com.janushub.service;

import java.util.Optional;

import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.stereotype.Service;

import com.janushub.model.Users;
import com.janushub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
  

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
}

    

