package com.janushub.resolver;

import com.janushub.model.Users;
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

    @QueryMapping
    public List<Users> allUsers() {
        return userRepository.findAll(); 
    }
}