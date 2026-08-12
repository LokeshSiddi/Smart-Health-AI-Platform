package com.fitness.activityservice.service;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.exception.InvalidActivityDataException;
import com.fitness.activityservice.exception.UserValidationException;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserValidationService userValidationService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.name}")
    private String exchange;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    public ActivityResponse trackActivity(ActivityRequest activityRequest) {
        if (activityRequest.getUserId() == null || activityRequest.getUserId().isBlank()) {
            throw new InvalidActivityDataException("User ID cannot be empty");
        }
        if (activityRequest.getDuration() == null || activityRequest.getDuration() <= 0) {
            throw new InvalidActivityDataException("Duration must be greater than 0");
        }

        boolean isValidUser = userValidationService.validateUser(activityRequest.getUserId());
        if (!isValidUser)
            throw new UserValidationException("Invalid User : " + activityRequest.getUserId());


        Activity activity = Activity.builder()
                .userId(activityRequest.getUserId())
                .type(activityRequest.getType())
                .caloriesBurned(activityRequest.getCaloriesBurned())
                .duration(activityRequest.getDuration())
                .startTime(activityRequest.getStartTime())
                .additionalMetrics(activityRequest.getAdditionalMetrics())
                .build();

        Activity savedActivity = activityRepository.save(activity);
        log.info("Activity logged successfully: {}", savedActivity.getId());

        // Publish to RabbitMQ for AI Processing
        try{
            rabbitTemplate.convertAndSend(exchange, routingKey, savedActivity);
            log.info("Activity event published to RabbitMQ for activity ID: {}", savedActivity.getId());
        } catch (Exception e) {
            log.error("Failed to publish activity event: {}", e.getMessage(), e);
        }

        return mapToResponse(savedActivity);
    }

    private ActivityResponse mapToResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();

        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setCaloriesBurned(activity.getCaloriesBurned());
        response.setDuration(activity.getDuration());
        response.setStartTime(activity.getStartTime());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());

        return response;
    }

    public List<ActivityResponse> getUserActivities(String userId) {
        List<Activity> activities = activityRepository.findByUserId(userId);

        return activities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ActivityResponse getActivity(String activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ActivityNotFoundException("Activity Not Found with Id : " + activityId));

        ActivityResponse response = mapToResponse(activity);

        return response;
    }
}
