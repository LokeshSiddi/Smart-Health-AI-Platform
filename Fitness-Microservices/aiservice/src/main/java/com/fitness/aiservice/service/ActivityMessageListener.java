package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListener {

    private final ActivityAiService aiService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "activity.queue")
    public void processActivity(Activity activity) {
        log.info("Received activity for Processing : {}", activity.getId());

        try {
            Recommendation recommendation = aiService.generateRecommendation(activity);
            if (recommendation != null) {
                recommendationRepository.save(recommendation);
                log.info("Successfully generated and saved recommendation for activity: {}", activity.getId());
            }
        } catch (Exception e) {
            // Log the failure without re-throwing the exception.
            // This prevents RabbitMQ from re-queueing the message indefinitely.
            log.error("Failed to generate recommendation for activity {}: {}", activity.getId(), e.getMessage());
        }
    }
}
