package com.janushub.resolver;

import com.janushub.model.User;
import com.janushub.repository.UserRepository;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class UserQueryResolver {
    
    // Inyecta el repositorio (DAO) de MongoDB
    private final UserRepository userRepository;

    public UserQueryResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public List<User> findAllUsers() { // <-- ¡CORREGIR ESTA LÍNEA!
        return userRepository.findAll();
    }

    @QueryMapping
    public List<User> allUsers() {
        return userRepository.findAll(); 
    }
}