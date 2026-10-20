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
                    0, true, m, hit.verified(), hit.failedAgents(), 0);
        }

        long t0 = System.currentTimeMillis();

        int[] tokens = {0};
        List<String> failed = new ArrayList<>();
        List<Finding> all = new ArrayList<>();
        addOrFail(all, parse(ask(m, Prompts.SECURITY, safe, tokens), "security"), "security", failed);
        addOrFail(all, parse(ask(m, Prompts.QUALITY, safe, tokens), "quality"), "quality", failed);
        String generatedTests = extractTests(ask(m, Prompts.TESTS, safe, tokens));
        if (generatedTests == null) {
            failed.add("tests");
            generatedTests = "";
        }

        List<Finding> result = all;
        boolean wasVerified = false;
        if (verify && !all.isEmpty()) {
            try {
                String payload = "CODE:\n" + safe + "\n\nFINDINGS:\n" + mapper.writeValueAsString(all);
                List<Finding> filtered = parse(ask(m, Prompts.VERIFIER, payload, tokens), "verified");
                if (filtered == null) {
                    failed.add("verifier"); // keep the unverified list rather than an empty one
                } else {
                    result = filtered;
                    wasVerified = true;
                }
            } catch (Exception e) {
                result = all; // keep the unverified list if the verifier fails
            }
        }

        AnalysisResponse res = new AnalysisResponse(result, generatedTests, computeScore(result),
                System.currentTimeMillis() - t0, false, m, wasVerified, List.copyOf(failed), tokens[0]);
        if (failed.isEmpty()) cache.put(key, res); // a partial result must not be served again

        return res;
    }

    private String ask(String model, String system, String user, int[] tokens) {
        GeminiClient.Reply reply = llm.ask(model, system, user);
        tokens[0] += reply.tokens();
        return reply.text();
    }

    private void addOrFail(List<Finding> all, List<Finding> found, String agent, List<String> failed) {
        if (found == null) failed.add(agent);
        else all.addAll(found);
    }

    /** Returns null when the model output is not the expected JSON, so a failure never looks like clean code. */
    private List<Finding> parse(String json, String agent) {
        try {
            JsonNode arr = mapper.readTree(json).get("findings");
            if (arr == null || !arr.isArray()) return null;
            List<Finding> out = new ArrayList<>();
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
            return null; // invalid output is never displayed
        }
    }

    private String extractTests(String json) {
        try {
            JsonNode tests = mapper.readTree(json).get("tests");
            return tests == null || !tests.isTextual() ? null : tests.asText();
        } catch (Exception e) {
            return null;
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