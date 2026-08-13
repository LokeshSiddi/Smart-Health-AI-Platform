package com.fitness.activityservice.service;

import com.fitness.activityservice.dto.ActivityRequest;
import com.fitness.activityservice.dto.ActivityResponse;
import com.fitness.activityservice.exception.ActivityNotFoundException;
import com.fitness.activityservice.exception.InvalidActivityDataException;
import com.fitness.activityservice.exception.UserValidationException;
import com.fitness.activityservice.model.Activity;
import com.fitness.activityservice.model.ActivityType;
import com.fitness.activityservice.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ActivityService} — the validate → save → publish pipeline.
 */
@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private UserValidationService userValidationService;
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ActivityService activityService;

    @BeforeEach
    void injectRabbitConfig() {
        // @Value fields are not populated in a plain Mockito test
        ReflectionTestUtils.setField(activityService, "exchange", "activity.exchange");
        ReflectionTestUtils.setField(activityService, "routingKey", "activity.routing.key");
    }

    // ------------------------------------------------------------------
    // trackActivity()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("happy path: validate → save → publish, in that order, with a fully mapped response")
    void trackActivity_happyPath() {
        ActivityRequest request = validRequest();

        when(userValidationService.validateUser("user-1")).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId("act-1");
            a.setCreatedAt(LocalDateTime.now());
            a.setUpdatedAt(LocalDateTime.now());
            return a;
        });

        ActivityResponse response = activityService.trackActivity(request);

        // Ordering IS the contract: never persist or publish for an unvalidated user
        InOrder inOrder = inOrder(userValidationService, activityRepository, rabbitTemplate);
        inOrder.verify(userValidationService).validateUser("user-1");
        inOrder.verify(activityRepository).save(any(Activity.class));
        inOrder.verify(rabbitTemplate)
                .convertAndSend(eq("activity.exchange"), eq("activity.routing.key"), any(Activity.class));

        // The builder must carry every request field into the document
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity persisted = captor.getValue();
        assertThat(persisted.getUserId()).isEqualTo("user-1");
        assertThat(persisted.getType()).isEqualTo(ActivityType.RUNNING);
        assertThat(persisted.getDuration()).isEqualTo(30);
        assertThat(persisted.getCaloriesBurned()).isEqualTo(350);
        assertThat(persisted.getAdditionalMetrics()).containsEntry("avgHeartRate", 145);

        // ... and the response mirrors the saved document
        assertThat(response.getId()).isEqualTo("act-1");
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getType()).isEqualTo(ActivityType.RUNNING);
        assertThat(response.getAdditionalMetrics()).containsEntry("avgHeartRate", 145);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @ParameterizedTest(name = "rejects userId [{0}] before any collaborator is touched")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void trackActivity_blankUserId(String userId) {
        ActivityRequest request = validRequest();
        request.setUserId(userId);

        assertThatThrownBy(() -> activityService.trackActivity(request))
                .isInstanceOf(InvalidActivityDataException.class)
                .hasMessage("User ID cannot be empty");

        verifyNoInteractions(userValidationService, activityRepository, rabbitTemplate);
    }

    @ParameterizedTest(name = "rejects duration {0}")
    @ValueSource(ints = {0, -1, -120})
    void trackActivity_nonPositiveDuration(int duration) {
        ActivityRequest request = validRequest();
        request.setDuration(duration);

        assertThatThrownBy(() -> activityService.trackActivity(request))
                .isInstanceOf(InvalidActivityDataException.class)
                .hasMessage("Duration must be greater than 0");

        verifyNoInteractions(userValidationService, activityRepository, rabbitTemplate);
    }

    @Test
    @DisplayName("null duration is rejected (NPE guard on auto-unboxing)")
    void trackActivity_nullDuration() {
        ActivityRequest request = validRequest();
        request.setDuration(null);

        assertThatThrownBy(() -> activityService.trackActivity(request))
                .isInstanceOf(InvalidActivityDataException.class)
                .hasMessage("Duration must be greater than 0");
    }

    @Test
    @DisplayName("invalid user → UserValidationException, nothing saved or published")
    void trackActivity_invalidUser() {
        when(userValidationService.validateUser("ghost")).thenReturn(false);
        ActivityRequest request = validRequest();
        request.setUserId("ghost");

        assertThatThrownBy(() -> activityService.trackActivity(request))
                .isInstanceOf(UserValidationException.class)
                .hasMessage("Invalid User : ghost");

        verifyNoInteractions(activityRepository, rabbitTemplate);
    }

    @Test
    @DisplayName("RabbitMQ down must NOT fail the request — activity is still saved and returned (resilience contract)")
    void trackActivity_rabbitDown_stillSucceeds() {
        when(userValidationService.validateUser("user-1")).thenReturn(true);
        when(activityRepository.save(any(Activity.class))).thenAnswer(inv -> {
            Activity a = inv.getArgument(0);
            a.setId("act-1");
            return a;
        });
        doThrow(new RuntimeException("broker unreachable"))
                .when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Activity.class));

        ActivityResponse response = activityService.trackActivity(validRequest());

        assertThat(response.getId()).isEqualTo("act-1");
        verify(activityRepository).save(any(Activity.class));
    }

    // ------------------------------------------------------------------
    // getUserActivities() / getActivity()
    // ------------------------------------------------------------------

    @Test
    @DisplayName("getUserActivities maps every document; unknown user gets an empty list, not null")
    void getUserActivities() {
        Activity a1 = Activity.builder().id("a1").userId("user-1").type(ActivityType.RUNNING)
                .duration(30).caloriesBurned(300).additionalMetrics(Map.of("steps", 4200)).build();
        Activity a2 = Activity.builder().id("a2").userId("user-1").type(ActivityType.YOGA)
                .duration(45).caloriesBurned(150).build();

        when(activityRepository.findByUserId("user-1")).thenReturn(List.of(a1, a2));
        when(activityRepository.findByUserId("nobody")).thenReturn(List.of());

        List<ActivityResponse> responses = activityService.getUserActivities("user-1");
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAdditionalMetrics()).containsEntry("steps", 4200);
        assertThat(activityService.getUserActivities("nobody")).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("getActivity: found maps the response; missing throws ActivityNotFoundException with the id")
    void getActivity() {
        Activity stored = Activity.builder().id("act-1").userId("user-1").type(ActivityType.CYCLING)
                .duration(60).caloriesBurned(500).build();
        when(activityRepository.findById("act-1")).thenReturn(Optional.of(stored));
        when(activityRepository.findById("act-404")).thenReturn(Optional.empty());

        assertThat(activityService.getActivity("act-1").getType()).isEqualTo(ActivityType.CYCLING);

        assertThatThrownBy(() -> activityService.getActivity("act-404"))
                .isInstanceOf(ActivityNotFoundException.class)
                .hasMessageContaining("act-404");
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
}
