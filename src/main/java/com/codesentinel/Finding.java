package com.codesentinel;

public record Finding(String agent, String type, String severity,
                      Integer line, String title, String explanation, String fix) {}