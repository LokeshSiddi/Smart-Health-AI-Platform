package com.fitness.aiservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;

@Service
@Slf4j
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public GeminiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String getAnswer(String question) {

        Map<String, Object> requestBody = Map.of(
            "contents", new Object[] {
                Map.of("parts", new Object[] {
                    Map.of("text", question)
                }
            )
        });

//        log.info("ApiUrl : {}", geminiApiUrl);
//        log.info("ApiKey : {}", geminiApiKey);

        String fullUrl = geminiApiUrl + geminiApiKey;
        return webClient.post()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                // 1. Handle 429 Rate Limits
                .onStatus(status -> status.value() == 429, response -> {
                    log.warn("Gemini Rate Limit Exceeded (429)");
                    return Mono.error(new RuntimeException("RATE_LIMIT_EXCEEDED"));
                })
                // 2. Handle 404 Not Found (Invalid Model)
                .onStatus(status -> status.value() == 404, response -> {
                    log.error("Gemini Endpoint / Model Not Found (404)");
                    return Mono.error(new RuntimeException("MODEL_NOT_FOUND"));
                })
                // 3. Handle 5xx Google Internal Server Errors
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    log.error("Gemini Server Error (5xx)");
                    return Mono.error(new RuntimeException("GEMINI_SERVER_ERROR"));
                })
                .bodyToMono(String.class)
                .onErrorReturn("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"{\\\"analysis\\\": {\\\"overall\\\": \\\"AI service temporarily unavailable. Maintain a balanced workout routine.\\\"}, \\\"improvements\\\": [], \\\"suggestions\\\": [], \\\"safety\\\": []}\"}]}}]}")
                .block();

//                .retrieve()
//                .onStatus(
//                        status -> status.is4xxClientError() || status.is5xxServerError(),
//                        clientResponse -> clientResponse.bodyToMono(String.class)
//                                .flatMap(errorBody -> {
//                                    System.err.println("Gemini API Error: " + errorBody);
//                                    return Mono.error(new RuntimeException("Gemini API error: " + errorBody));
//                                })
//                )
//                .bodyToMono(String.class)
//                .block();

//        return response;
    }

}
