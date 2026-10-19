package com.codesentinel;

import java.util.List;

public record AnalysisResponse(List<Finding> findings, String generatedTests,
                               int score, long latencyMs, boolean cached,
                               String model, boolean verified) {}