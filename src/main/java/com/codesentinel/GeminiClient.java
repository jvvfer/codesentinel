package com.codesentinel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private static final int MAX_ATTEMPTS = 3;

    private final RestClient http;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public GeminiClient(@Value("${llm.api-key}") String apiKey,
                        @Value("${llm.timeout-seconds}") int timeout) {
        this.apiKey = apiKey;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        this.http = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .requestFactory(factory)
                .build();
    }

    public String ask(String model, String system, String user) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "API key missing");
        }

        var body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", system))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", user)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "maxOutputTokens", 4000));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                String raw = http.post()
                        .uri("/v1beta/models/{m}:generateContent", model)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

                JsonNode res = mapper.readTree(raw);
                String text = res.path("candidates").path(0).path("content")
                        .path("parts").path(0).path("text").asText("");
                return text.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$", "").trim();

            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_ATTEMPTS) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Quota exceeded, wait a minute");
                }
                pause(attempt);
            } catch (HttpServerErrorException e) {
                // Gemini 500 / 503 / 504: model overloaded, usually temporary
                if (attempt == MAX_ATTEMPTS) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Model overloaded, try again in a few minutes or switch model");
                }
                pause(attempt);
            } catch (HttpClientErrorException e) {
                // 400 / 403 / 404: bad key, bad model name, bad request
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Gemini rejected the request (" + e.getStatusCode().value()
                                + "), check the key and the model name");
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid LLM response");
            } catch (RestClientException e) {
                // timeouts and network errors
                if (attempt == MAX_ATTEMPTS) {
                    throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "LLM timeout");
                }
                pause(attempt);
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "LLM unavailable");
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(5000L * attempt);   // 5 s, then 10 s
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}