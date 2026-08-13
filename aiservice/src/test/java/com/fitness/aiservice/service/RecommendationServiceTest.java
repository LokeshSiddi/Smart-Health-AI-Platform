package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RecommendationService}.
 * NOTE: getActivityRecommendation() returns NULL when the Optional is empty
 * (this changed from the earlier orElseThrow version). The controller turns
 * that null into a 404. These tests lock in the current null-based contract.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    @DisplayName("getUserRecommendation is a pure passthrough of the repository result")
    void userRecommendations_passthrough() {
        Recommendation r = Recommendation.builder().id("rec-1").userId("user-1").build();
        when(recommendationRepository.findByUserId("user-1")).thenReturn(List.of(r));

        assertThat(recommendationService.getUserRecommendation("user-1")).containsExactly(r);
        verify(recommendationRepository).findByUserId("user-1");
    }

    @Test
    @DisplayName("getActivityRecommendation returns the document when present")
    void activityRecommendation_found() {
        Recommendation r = Recommendation.builder().id("rec-1").activityId("act-1").build();
        when(recommendationRepository.findByActivityId("act-1")).thenReturn(Optional.of(r));

        assertThat(recommendationService.getActivityRecommendation("act-1")).isEqualTo(r);
    }

    @Test
    @DisplayName("getActivityRecommendation returns NULL (not an exception) when absent")
    void activityRecommendation_missing_returnsNull() {
        when(recommendationRepository.findByActivityId("act-404")).thenReturn(Optional.empty());

        assertThat(recommendationService.getActivityRecommendation("act-404")).isNull();
    }
}
