package com.fitness.aiservice.service;

import com.fitness.aiservice.exception.GeminiApiException;
import com.fitness.aiservice.exception.RecommendationGenerationException;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link ActivityMessageListener} — the RabbitMQ consumer contract:
 * success → save; Gemini hiccup → swallow silently; unexpected error →
 * RecommendationGenerationException so the message is NOT re-queued forever.
 */
@ExtendWith(MockitoExtension.class)
class ActivityMessageListenerTest {

    @Mock
    private ActivityAiService aiService;
    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private ActivityMessageListener listener;

    @Test
    @DisplayName("generated recommendation is saved exactly once")
    void success_savesOnce() {
        Recommendation rec = Recommendation.builder()
                .activityId("act-1").userId("user-1").activityType("RUNNING").build();
        when(aiService.generateRecommendation(any(Activity.class))).thenReturn(rec);

        listener.processActivity(activity());

        verify(recommendationRepository, times(1)).save(rec);
    }

    @Test
    @DisplayName("null recommendation is not saved (defensive branch)")
    void nullRecommendation_notSaved() {
        when(aiService.generateRecommendation(any(Activity.class))).thenReturn(null);

        assertThatCode(() -> listener.processActivity(activity())).doesNotThrowAnyException();
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    @DisplayName("GeminiApiException is swallowed: no rethrow, no save, message considered consumed")
    void geminiApiException_swallowed() {
        when(aiService.generateRecommendation(any(Activity.class)))
                .thenThrow(new GeminiApiException("RATE_LIMIT_EXCEEDED"));

        assertThatCode(() -> listener.processActivity(activity())).doesNotThrowAnyException();
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    @DisplayName("unexpected failure → RecommendationGenerationException (logged, single retry semantics, no save)")
    void unexpectedException_wrapped() {
        when(aiService.generateRecommendation(any(Activity.class)))
                .thenThrow(new IllegalStateException("mongo down"));

        assertThatThrownBy(() -> listener.processActivity(activity()))
                .isInstanceOf(RecommendationGenerationException.class)
                .hasMessage("Failed to process activity event")
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(recommendationRepository, never()).save(any());
    }

    private Activity activity() {
        Activity a = new Activity();
        a.setId("act-1");
        a.setUserId("user-1");
        a.setType("RUNNING");
        a.setDuration(30);
        a.setCaloriesBurned(350);
        return a;
    }
}
