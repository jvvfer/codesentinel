package com.codesentinel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class SecretMasker {

    private record Rule(Pattern pattern, String replacement) {}

    private static final List<Rule> RULES = List.of(
            new Rule(Pattern.compile(
                    "(?i)(api[_-]?key|secret|token|password|passwd)\\s*[:=]\\s*[\"']?[^\"'\\s]{6,}[\"']?"),
                    "$1 = \"[MASKED_SECRET]\""),
            new Rule(Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[MASKED_EMAIL]"),
            new Rule(Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._-]{20,}"), "Bearer [MASKED_SECRET]"),
            new Rule(Pattern.compile("jdbc:[a-z]+://\\S+"), "[MASKED_DB_URL]")
    );

    public String mask(String code) {
        String out = code;
        for (Rule r : RULES) {
            out = r.pattern().matcher(out).replaceAll(r.replacement());
        }
        return out;
    }
}