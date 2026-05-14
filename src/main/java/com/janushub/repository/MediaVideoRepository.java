package com.janushub.repository;

import com.janushub.model.MediaVideo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MediaVideoRepository extends MongoRepository<MediaVideo, String> {
    List<MediaVideo> findAllByOrderByCreatedAtDesc();
}