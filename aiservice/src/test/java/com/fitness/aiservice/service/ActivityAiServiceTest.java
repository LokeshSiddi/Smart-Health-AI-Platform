package com.fitness.aiservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ActivityAiService} — the Gemini response-parsing pipeline.
 * GeminiService is mocked; these tests lock in the JSON parsing contract,
 * the markdown-fence stripping and the graceful-degradation fallback.
 */
@ExtendWith(MockitoExtension.class)
class ActivityAiServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String VALID_ANALYSIS = """
            {
              "analysis": {
                "overall": "Solid zone-2 session",
                "pace": "5:40/km held consistently",
                "heartRate": "145 bpm average is appropriate",
                "caloriesBurned": "Matches a 30-minute run"
              },
              "improvements": [
                {"area": "Cardio", "recommendation": "Add one interval session per week"}
              ],
              "suggestions": [
                {"workout": "Tempo Run", "description": "20 minutes at threshold pace"}
              ],
              "safety": ["Warm up for 10 minutes", "Hydrate before and after"]
            }
            """;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ActivityAiService activityAiService;

    @Test
    @DisplayName("parses a fully populated, markdown-fenced Gemini response")
    void fencedResponse_fullParse() throws Exception {
        when(geminiService.getAnswer(anyString()))
                .thenReturn(envelope("```json\n" + VALID_ANALYSIS + "\n```"));

        Recommendation r = activityAiService.generateRecommendation(activity());

        assertThat(r.getRecommendation())
                .contains("Overall : Solid zone-2 session")
                .contains("Pace : 5:40/km held consistently")
                .contains("Heart Rate : 145 bpm average is appropriate")
                .contains("Calories Burned : Matches a 30-minute run");
        assertThat(r.getImprovements())
                .containsExactly("Cardio: Add one interval session per week");
        assertThat(r.getSuggestions())
                .containsExactly("Tempo Run: 20 minutes at threshold pace");
        assertThat(r.getSafety())
                .containsExactly("Warm up for 10 minutes", "Hydrate before and after");

        // activity data must flow through untouched
        assertThat(r.getActivityId()).isEqualTo("act-1");
        assertThat(r.getUserId()).isEqualTo("user-1");
        assertThat(r.getActivityType()).isEqualTo("RUNNING");
        assertThat(r.getDuration()).isEqualTo(30);
        assertThat(r.getCaloriesBurned()).isEqualTo(350);
        assertThat(r.getActivityDate()).isEqualTo(LocalDateTime.of(2026, 8, 12, 7, 30));
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("also parses when Gemini returns raw JSON WITHOUT code fences")
    void unfencedResponse_parses() throws Exception {
        when(geminiService.getAnswer(anyString())).thenReturn(envelope(VALID_ANALYSIS));

        Recommendation r = activityAiService.generateRecommendation(activity());

        assertThat(r.getRecommendation()).contains("Overall : Solid zone-2 session");
        assertThat(r.getImprovements()).hasSize(1);
    }

    @Test
    @DisplayName("empty arrays fall back to documented placeholder strings")
    void emptyArrays_defaults() throws Exception {
        String emptyJson = """
                {"analysis": {"overall": "ok"}, "improvements": [], "suggestions": [], "safety": []}
                """;
        when(geminiService.getAnswer(anyString())).thenReturn(envelope(emptyJson));

        Recommendation r = activityAiService.generateRecommendation(activity());

        assertThat(r.getImprovements()).containsExactly("No Specific Improvement Provided");
        assertThat(r.getSuggestions()).containsExactly("No Specific Suggestions Provided");
        assertThat(r.getSafety()).containsExactly("Follow General Safety Guidelines");
        assertThat(r.getRecommendation()).contains("Overall : ok");
    }

    @Test
    @DisplayName("missing analysis sub-sections are simply skipped (no empty prefixes)")
    void partialAnalysis_skipsMissingSections() throws Exception {
        String partial = """
                {"analysis": {"overall": "Decent effort"}, "improvements": [], "suggestions": [], "safety": []}
                """;
        when(geminiService.getAnswer(anyString())).thenReturn(envelope(partial));

        Recommendation r = activityAiService.generateRecommendation(activity());

        assertThat(r.getRecommendation()).contains("Overall : Decent effort");
        assertThat(r.getRecommendation())
                .doesNotContain("Pace :").doesNotContain("Heart Rate :").doesNotContain("Calories Burned :");
    }

    @Test
    @DisplayName("garbage / non-JSON text triggers the default-recommendation fallback (pipeline stays alive)")
    void garbage_fallback() throws Exception {
        when(geminiService.getAnswer(anyString())).thenReturn(envelope("Sorry, I cannot help with that."));

        Recommendation r = activityAiService.generateRecommendation(activity());

        assertThat(r.getRecommendation()).isEqualTo("Unable to Generate Detailed Analysis");
        assertThat(r.getImprovements()).containsExactly("Continue with your Current Routine");
        assertThat(r.getSuggestions()).containsExactly("Consider Consulting a Fitness Professional");
        assertThat(r.getSafety()).containsExactly(
                "Always warm up before Exercise", "Stay Hydrated", "Listen to your Body");
        // the fallback still anchors to the activity
        assertThat(r.getActivityId()).isEqualTo("act-1");
        assertThat(r.getUserId()).isEqualTo("user-1");
        assertThat(r.getDuration()).isEqualTo(30);
    }

    @Test
    @DisplayName("missing candidates array also degrades gracefully instead of throwing")
    void missingCandidates_fallback() {
        when(geminiService.getAnswer(anyString())).thenReturn("{}");

        Recommendation r = activityAiService.generateRecommendation(activity());

        assertThat(r.getRecommendation()).isEqualTo("Unable to Generate Detailed Analysis");
    }

    @Test
    @DisplayName("prompt sent to Gemini embeds the activity fields")
    void promptContainsActivityData() {
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        when(geminiService.getAnswer(prompt.capture())).thenReturn("{}");

        activityAiService.generateRecommendation(activity());

        assertThat(prompt.getValue())
                .contains("Activity Type: RUNNING")
                .contains("Duration: 30 minutes")
                .contains("Calories Burned: 350")
                .contains("avgHeartRate=145");
    }

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private Activity activity() {
        Activity a = new Activity();
        a.setId("act-1");
        a.setUserId("user-1");
        a.setType("RUNNING");
        a.setDuration(30);
        a.setCaloriesBurned(350);
        a.setStartTime(LocalDateTime.of(2026, 8, 12, 7, 0));
        a.setAdditionalMetrics(Map.of("avgHeartRate", 145));
        a.setCreatedAt(LocalDateTime.of(2026, 8, 12, 7, 30));
        return a;
    }

    /** Wraps raw text into the Gemini response envelope: candidates[0].content.parts[0].text */
    private static String envelope(String text) throws Exception {
        ObjectNode part = MAPPER.createObjectNode();
        part.put("text", text);
        ArrayNode parts = MAPPER.createArrayNode().add(part);
        ObjectNode content = MAPPER.createObjectNode();
        content.set("parts", parts);
        ObjectNode candidate = MAPPER.createObjectNode();
        candidate.set("content", content);
        ArrayNode candidates = MAPPER.createArrayNode().add(candidate);
        ObjectNode root = MAPPER.createObjectNode();
        root.set("candidates", candidates);
        return MAPPER.writeValueAsString(root);
    }
}
