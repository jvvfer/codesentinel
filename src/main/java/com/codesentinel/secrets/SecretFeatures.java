package com.codesentinel.secrets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Turns a string literal and the name of the variable it is assigned to into numbers a model can learn from. */
public final class SecretFeatures {

    private SecretFeatures() {}

    public static final List<String> NAMES = List.of(
            "length", "entropy", "digitRatio", "upperRatio", "lowerRatio", "symbolRatio",
            "charClasses", "knownPrefix", "nameHint", "hasSpace", "looksHex", "looksUuid", "hasDotOrSlash");

    private static final Pattern NAME_HINT =
            Pattern.compile("(?i)(key|pass|pwd|mdp|token|secret|cred|auth|private)");
    private static final Pattern KNOWN_PREFIX =
            Pattern.compile("^(sk_|pk_|rk_|AKIA|ASIA|ghp_|gho_|ghs_|github_pat_|xox[abpr]-|AIza|eyJ)");
    private static final Pattern HEX = Pattern.compile("^[0-9a-fA-F]{16,}$");
    private static final Pattern UUID =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public static double[] of(String variable, String value) {
        int n = value.length();
        int digits = 0, upper = 0, lower = 0, symbols = 0;
        for (char c : value.toCharArray()) {
            if (Character.isDigit(c)) digits++;
            else if (Character.isUpperCase(c)) upper++;
            else if (Character.isLowerCase(c)) lower++;
            else if (!Character.isWhitespace(c)) symbols++;
        }
        double len = Math.max(n, 1);
        int classes = (digits > 0 ? 1 : 0) + (upper > 0 ? 1 : 0) + (lower > 0 ? 1 : 0) + (symbols > 0 ? 1 : 0);
        return new double[] {
                n,
                entropy(value),
                digits / len,
                upper / len,
                lower / len,
                symbols / len,
                classes,
                flag(KNOWN_PREFIX.matcher(value).find()),
                flag(NAME_HINT.matcher(variable).find()),
                flag(value.contains(" ")),
                flag(HEX.matcher(value).matches()),
                flag(UUID.matcher(value).matches()),
                flag(value.contains(".") || value.contains("/"))
        };
    }

    /** Shannon entropy in bits per character: high for random keys, low for words. */
    static double entropy(String s) {
        if (s.isEmpty()) return 0;
        Map<Character, Integer> counts = new HashMap<>();
        for (char c : s.toCharArray()) counts.merge(c, 1, Integer::sum);
        double h = 0;
        for (int count : counts.values()) {
            double p = (double) count / s.length();
            h -= p * (Math.log(p) / Math.log(2));
        }
        return h;
    }

    private static double flag(boolean b) {
        return b ? 1 : 0;
    }
}
