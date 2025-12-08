package com.janushub.repository;

import com.janushub.model.Users; 
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends MongoRepository<Users, String> { 
    Users findByUsername(String username); 
    List<Users> findByUsernameContainingIgnoreCase(String username);
}
