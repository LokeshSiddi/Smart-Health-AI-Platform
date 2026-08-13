package com.fitness.aiservice.controller;

import com.fitness.aiservice.model.Recommendation;
import com.fitness.aiservice.service.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice for {@link RecommendationController}.
 * Key contract: the controller converts a null service result into a 404 with
 * an EMPTY body (not an ErrorResponse — the GlobalExceptionHandler is bypassed).
 */
@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    @DisplayName("GET /api/recommendations/user/{userId} → 200 with the recommendation list")
    void userRecommendations() throws Exception {
        when(recommendationService.getUserRecommendation("user-1"))
                .thenReturn(List.of(sampleRecommendation()));

        mockMvc.perform(get("/api/recommendations/user/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].activityId").value("act-1"))
                .andExpect(jsonPath("$[0].activityType").value("RUNNING"))
                .andExpect(jsonPath("$[0].improvements[0]").value("Cardio: add intervals"))
                .andExpect(jsonPath("$[0].safety[0]").value("Stay Hydrated"));
    }

    @Test
    @DisplayName("GET /api/recommendations/activity/{id} → 200 when a recommendation exists")
    void activityRecommendation_found() throws Exception {
        when(recommendationService.getActivityRecommendation("act-1")).thenReturn(sampleRecommendation());

        mockMvc.perform(get("/api/recommendations/activity/act-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.duration").value(30))
                .andExpect(jsonPath("$.caloriesBurned").value(350));
    }

    @Test
    @DisplayName("GET /api/recommendations/activity/{id} → 404 with empty body when service returns null")
    void activityRecommendation_missing() throws Exception {
        when(recommendationService.getActivityRecommendation("act-404")).thenReturn(null);

        mockMvc.perform(get("/api/recommendations/activity/act-404"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("GET /api/recommendations/user/{userId} → 200 with [] when the user has none")
    void userRecommendations_empty() throws Exception {
        when(recommendationService.getUserRecommendation("ghost")).thenReturn(List.of());

        mockMvc.perform(get("/api/recommendations/user/ghost"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    private Recommendation sampleRecommendation() {
        return Recommendation.builder()
                .id("rec-1")
                .activityId("act-1")
                .userId("user-1")
                .activityType("RUNNING")
                .duration(30)
                .caloriesBurned(350)
                .activityDate(LocalDateTime.of(2026, 8, 12, 7, 30))
                .recommendation("Overall : good")
                .improvements(List.of("Cardio: add intervals"))
                .suggestions(List.of("Tempo Run: 20 min"))
                .safety(List.of("Stay Hydrated"))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
