package com.fitness.aiservice.repository;

import com.fitness.aiservice.model.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    List<Recommendation> findByUserId(String userId);

    Optional<Recommendation> findByActivityId(String activityId);
}
