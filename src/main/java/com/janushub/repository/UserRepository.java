package com.janushub.repository;
 
import com.janushub.model.Users;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
 
@Repository
public interface UserRepository extends MongoRepository<Users, String> {
    Optional<Users> findByUsername(String username);
   
    /**
     * Encuentra un usuario por su email (case-insensitive).
     */
    Optional<Users> findByEmailIgnoreCase(String email);
   
    List<Users> findByUsernameContainingIgnoreCase(String username);
   
    /**
     * Alias para búsqueda convenient de email.
     */
    default Optional<Users> findByEmail(String email) {
        return findByEmailIgnoreCase(email);
    }
}