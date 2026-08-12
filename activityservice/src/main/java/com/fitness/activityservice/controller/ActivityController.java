package com.fitness.activityservice.controller;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Activity Management", description = "APIs for tracking and retrieving user fitness activities")
public class    ActivityController {

    private final ActivityService activityService;

    @PostMapping
    @Operation(summary = "Log a new activity", description = "Records a new fitness activity and publishes an event to RabbitMQ for AI processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Activity logged successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid activity data")
    })
    public ResponseEntity<ActivityResponse> trackActivity(@RequestBody ActivityRequest activityRequest, @RequestHeader("X-User-ID") String userId) {
        if (userId != null) {
            activityRequest.setUserId(userId);
        }
        log.info("Logging activity for user: {}", activityRequest.getUserId());
        return ResponseEntity.ok(activityService.trackActivity(activityRequest));
    }

    @GetMapping
    @Operation(summary = "Get all activities", description = "Retrieves all logged activities of user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activities retrieved successfully")
    })
    public ResponseEntity<List<ActivityResponse>> getUserActivities(@RequestHeader("X-User-ID") String userId) {
        log.info("Retrieving all activities of user");
        return ResponseEntity.ok(activityService.getUserActivities(userId));
    }

    @GetMapping("/{activityId}")
    @Operation(summary = "Get the activity", description = "Retrieves the logged activity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Activity retrieved successfully")
    })
    public ResponseEntity<ActivityResponse> getActivity(@PathVariable("activityId") String activityId) {
        log.info("Retrieving the activity");
        return ResponseEntity.ok(activityService.getActivity(activityId));
    }

}
