package com.janushub.repository;

import com.janushub.model.AppNotification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<AppNotification, String> {

    /**
     * Notificaciones para todos (targetRoles null o vacío) o que incluyan al menos uno de los roles dados,
     * creadas a partir del timestamp indicado, ordenadas de más reciente a más antigua.
     */
    @Query("{ $and: [ { 'timestamp': { $gte: ?1 } }, " +
           "{ $or: [ { 'targetRoles': { $exists: false } }, { 'targetRoles': { $size: 0 } }, { 'targetRoles': { $in: ?0 } } ] } ] }")
    List<AppNotification> findRecentForRoles(List<String> roles, Instant since);
}
