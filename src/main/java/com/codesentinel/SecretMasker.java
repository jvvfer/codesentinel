package com.codesentinel;

import com.codesentinel.secrets.SecretClassifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
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

    /** A string literal assigned to a variable: {@code name = "value"}. */
    private static final Pattern ASSIGNMENT =
            Pattern.compile("(?<![=!<>])\\b([A-Za-z_]\\w*)(\\s*=\\s*\")((?:[^\"\\\\\\n]|\\\\.)*)\"");

    private final SecretClassifier classifier;

    public SecretMasker() {
        this(SecretClassifier.loadDefault());
    }

    /** @param classifier null for regex-only masking */
    public SecretMasker(SecretClassifier classifier) {
        this.classifier = classifier;
    }

    public String mask(String code) {
        String out = code;
        for (Rule r : RULES) {
            out = r.pattern().matcher(out).replaceAll(r.replacement());
        }
        return classifier == null ? out : maskWithModel(out);
    }

    /** Second pass: the Random Forest judges each literal the regex left, and masks it the same way. */
    private String maskWithModel(String code) {
        Matcher m = ASSIGNMENT.matcher(code);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = m.group(3);
            boolean secret = !value.isEmpty() && !value.contains("[MASKED_")
                    && classifier.isSecret(m.group(1), value);
            String kept = secret ? m.group(1) + m.group(2) + "[MASKED_SECRET]\"" : m.group();
            m.appendReplacement(sb, Matcher.quoteReplacement(kept));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
