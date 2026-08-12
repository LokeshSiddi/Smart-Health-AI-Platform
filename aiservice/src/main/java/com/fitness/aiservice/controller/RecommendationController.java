package com.fitness.aiservice.controller;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
@Slf4j
@Tag(name = "AI Recommendations", description = "APIs for retrieving AI-powered fitness recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get recommendations for a user", description = "Retrieves AI-generated fitness recommendations for a specific user based on their activity data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No recommendations found for user")
    })
    public ResponseEntity<List<Recommendation>> getUserRecommendation(@PathVariable("userId") String userId) {
        log.info("Retrieving recommendations for user: {}", userId);
        return ResponseEntity.ok(recommendationService.getUserRecommendation(userId));
    }

    @GetMapping("/activity/{activityId}")
    @Operation(summary = "Get recommendations for a activity", description = "Retrieves AI-generated fitness recommendations for a specific activity data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No recommendations found for activity")
    })
    public ResponseEntity<Recommendation> getActivityRecommendation(@PathVariable("activityId") String activityId) {
        Recommendation recommendation = recommendationService.getActivityRecommendation(activityId);
        if (recommendation == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        log.info("Retrieving recommendations for activity: {}", activityId);
        return ResponseEntity.ok(recommendation);
    }

}
