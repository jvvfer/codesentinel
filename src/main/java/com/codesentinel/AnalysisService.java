package com.codesentinel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisService {

    private final GeminiClient llm;
    private final SecretMasker masker;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, AnalysisResponse> cache = new ConcurrentHashMap<>();

    @Value("${llm.model}")
    private String defaultModel;

    public AnalysisService(GeminiClient llm, SecretMasker masker) {
        this.llm = llm;
        this.masker = masker;
    }

    public AnalysisResponse analyze(String code, String model, boolean verify) {
        String m = (model == null || model.isBlank()) ? defaultModel : model;
        String safe = masker.mask(code);
        String hash = DigestUtils.md5DigestAsHex(safe.getBytes(StandardCharsets.UTF_8));
        String key = m + ":" + verify + ":" + hash;

        AnalysisResponse hit = cache.get(key);
        if (hit != null) {
            return new AnalysisResponse(hit.findings(), hit.generatedTests(), hit.score(),
                    0, true, m, hit.verified());
        }

        long t0 = System.currentTimeMillis();

        List<Finding> all = new ArrayList<>();
        all.addAll(parse(llm.ask(m, Prompts.SECURITY, safe), "security"));
        all.addAll(parse(llm.ask(m, Prompts.QUALITY, safe), "quality"));
        String generatedTests = extractTests(llm.ask(m, Prompts.TESTS, safe));

        List<Finding> result = all;
        boolean wasVerified = false;
        if (verify && !all.isEmpty()) {
            try {
                String payload = "CODE:\n" + safe + "\n\nFINDINGS:\n" + mapper.writeValueAsString(all);
                result = parse(llm.ask(m, Prompts.VERIFIER, payload), "verified");
                wasVerified = true;
            } catch (Exception e) {
                result = all; // keep the unverified list if the verifier fails
            }
        }

        AnalysisResponse res = new AnalysisResponse(result, generatedTests, computeScore(result),
                System.currentTimeMillis() - t0, false, m, wasVerified);
        cache.put(key, res);
        return res;
    }

    private List<Finding> parse(String json, String agent) {
        try {
            JsonNode arr = mapper.readTree(json).get("findings");
            List<Finding> out = new ArrayList<>();
            if (arr == null || !arr.isArray()) return out;
            for (JsonNode n : arr) {
                out.add(new Finding(
                        agent,
                        n.path("type").asText("smell"),
                        n.path("severity").asText("low"),
                        n.hasNonNull("line") ? n.get("line").asInt() : null,
                        n.path("title").asText(),
                        n.path("explanation").asText(),
                        n.path("fix").asText()));
            }
            return out;
        } catch (Exception e) {
            return List.of(); // invalid output is never displayed
        }
    }

    private String extractTests(String json) {
        try {
            return mapper.readTree(json).path("tests").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private int computeScore(List<Finding> findings) {
        int penalty = findings.stream().mapToInt(f -> switch (f.severity()) {
            case "critical" -> 30;
            case "high" -> 15;
            case "medium" -> 7;
            default -> 2;
        }).sum();
        return Math.max(0, 100 - penalty);
    }
}