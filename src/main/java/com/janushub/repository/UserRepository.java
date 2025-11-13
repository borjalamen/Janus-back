package com.janushub.repository;

import com.janushub.model.Users; // <-- ¡ASEGÚRATE DE QUE ES SINGULAR!
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<Users, String> { // <-- ¡SINGULAR!
    Users findByUsername(String username); // <-- ¡SINGULAR!
}
