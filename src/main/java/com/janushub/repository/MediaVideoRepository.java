package com.janushub.repository;

import com.janushub.model.MediaVideo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MediaVideoRepository extends MongoRepository<MediaVideo, String> {
}