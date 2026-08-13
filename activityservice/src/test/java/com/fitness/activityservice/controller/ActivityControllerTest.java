package com.fitness.activityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.exception.InvalidActivityDataException;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.service.ActivityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice for {@link ActivityController} — key behavior under test:
 * the X-User-ID header OVERRIDES whatever userId is in the request body.
 */
@WebMvcTest(ActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivityService activityService;

    @Test
    @DisplayName("POST /api/activities: X-User-ID header takes precedence over the body userId")
    void trackActivity_headerOverridesBody() throws Exception {
        ActivityRequest body = validRequest();
        body.setUserId("body-user"); // should be ignored
        when(activityService.trackActivity(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "header-user")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("act-1"));

        ArgumentCaptor<ActivityRequest> captor = ArgumentCaptor.forClass(ActivityRequest.class);
        verify(activityService).trackActivity(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("header-user")
                .as("the gateway-injected header is authoritative, not the client body");
    }

    @Test
    @DisplayName("POST /api/activities: without the header the body userId is preserved")
    void trackActivity_noHeader_bodyUserIdPreserved() throws Exception {
        when(activityService.trackActivity(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk());

        ArgumentCaptor<ActivityRequest> captor = ArgumentCaptor.forClass(ActivityRequest.class);
        verify(activityService).trackActivity(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("POST /api/activities: InvalidActivityDataException → 400 ErrorResponse")
    void trackActivity_invalidData_mapsTo400() throws Exception {
        when(activityService.trackActivity(any()))
                .thenThrow(new InvalidActivityDataException("Duration must be greater than 0"));

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-ID", "header-user")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Duration must be greater than 0"));
    }

    @Test
    @DisplayName("GET /api/activities uses the X-User-ID header")
    void getUserActivities_usesHeader() throws Exception {
        when(activityService.getUserActivities("header-user")).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/activities").header("X-User-ID", "header-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("act-1"))
                .andExpect(jsonPath("$[0].type").value("RUNNING"));

        verify(activityService).getUserActivities("header-user");
    }

    @Test
    @DisplayName("GET /api/activities/{id} → 200 with the mapped document")
    void getActivity_found() throws Exception {
        when(activityService.getActivity("act-1")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/activities/act-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caloriesBurned").value(350))
                .andExpect(jsonPath("$.additionalMetrics.avgHeartRate").value(145));
    }

    @Test
    @DisplayName("GET /api/activities/{id} → 404 ErrorResponse when missing")
    void getActivity_notFound() throws Exception {
        when(activityService.getActivity("act-404"))
                .thenThrow(new ActivityNotFoundException("Activity Not Found with Id : act-404"));

        mockMvc.perform(get("/api/activities/act-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Activity Not Found with Id : act-404"))
                .andExpect(jsonPath("$.path").value("/api/activities/act-404"));
    }

    private ActivityRequest validRequest() {
        ActivityRequest r = new ActivityRequest();
        r.setUserId("user-1");
        r.setType(ActivityType.RUNNING);
        r.setDuration(30);
        r.setCaloriesBurned(350);
        r.setStartTime(LocalDateTime.of(2026, 8, 12, 7, 0));
        r.setAdditionalMetrics(Map.of("avgHeartRate", 145));
        return r;
    }

    private ActivityResponse sampleResponse() {
        ActivityResponse r = new ActivityResponse();
        r.setId("act-1");
        r.setUserId("user-1");
        r.setType(ActivityType.RUNNING);
        r.setDuration(30);
        r.setCaloriesBurned(350);
        r.setStartTime(LocalDateTime.of(2026, 8, 12, 7, 0));
        r.setAdditionalMetrics(Map.of("avgHeartRate", 145));
        r.setCreatedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return r;
    }
}
