package com.codesentinel.secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Examples for the secret classifier. Non-secrets are real literals read from a CSV
 * (see tools/secrets/extract_negatives.py); secrets are generated here from public key
 * formats with a fixed seed, so no real or realistic key is ever stored in the repository.
 */
public final class SecretDataset {

    private SecretDataset() {}

    public record Example(String variable, String value, int label, String source) {}

    private static final String ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String UPPER_DIGITS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String HEX = "0123456789abcdef";
    private static final String B64 = ALNUM + "+/";
    private static final String B64URL = ALNUM + "-_";

    private static final List<String> SECRET_NAMES = List.of(
            "apiKey", "API_KEY", "secret", "clientSecret", "CLIENT_SECRET", "password", "pwd", "passwd",
            "pass", "dbPassword", "DB_PASS", "token", "accessToken", "authToken", "refreshToken",
            "credentials", "privateKey", "secretKey", "awsSecret", "stripeKey", "githubToken",
            "slackToken", "mdp", "motDePasse", "adminPwd");
    private static final List<String> NEUTRAL_NAMES = List.of(
            "value", "s", "str", "header", "data", "config", "defaultValue", "x", "raw", "param", "arg");
    private static final List<String> WORDS = List.of(
            "summer", "winter", "soleil", "azerty", "admin", "welcome", "dragon", "monkey", "esprit",
            "tunis", "football", "password", "letmein", "master", "shadow", "jasmin", "carthage", "qwerty");

    public static List<Example> readCsv(Path csv) throws IOException {
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        List<Example> out = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            List<String> f = parseCsvLine(line);
            out.add(new Example(f.get(0), f.get(1), Integer.parseInt(f.get(2)), f.get(3)));
        }
        return out;
    }

    /** Minimal RFC 4180 reader: quoted fields, doubled quotes. */
    static List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cur.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { fields.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        fields.add(cur.toString());
        return fields;
    }

    /** {@code count} generated secrets, identical for the same seed. */
    public static List<Example> generateSecrets(int count, long seed) {
        Random rng = new Random(seed);
        List<Example> out = new ArrayList<>();
        while (out.size() < count) {
            int kind = rng.nextInt(11);
            String value = switch (kind) {
                case 0 -> "sk_" + (rng.nextBoolean() ? "live_" : "test_") + random(rng, ALNUM, 24);
                case 1 -> "AKIA" + random(rng, UPPER_DIGITS, 16);
                case 2 -> random(rng, B64, 40);
                case 3 -> "ghp_" + random(rng, ALNUM, 36);
                case 4 -> "xoxb-" + random(rng, "0123456789", 11) + "-" + random(rng, ALNUM, 24);
                case 5 -> "AIza" + random(rng, B64URL, 35);
                case 6 -> "eyJ" + random(rng, B64URL, 30) + "." + random(rng, B64URL, 40) + "." + random(rng, B64URL, 43);
                case 7 -> random(rng, HEX, List.of(32, 40, 64).get(rng.nextInt(3)));
                case 8 -> random(rng, B64, 24 + rng.nextInt(20)) + (rng.nextBoolean() ? "=" : "");
                case 9 -> humanPassword(rng);
                default -> random(rng, ALNUM, 4 + rng.nextInt(5)); // short secret, below the regex's 6 characters
            };
            // people name their passwords; random keys sometimes sit in neutral variables
            boolean named = kind >= 9 || rng.nextDouble() < 0.7;
            String variable = pick(rng, named ? SECRET_NAMES : NEUTRAL_NAMES);
            out.add(new Example(variable, value, 1, "generated:kind" + kind));
        }
        return out;
    }

    private static String humanPassword(Random rng) {
        String w = pick(rng, WORDS);
        if (rng.nextBoolean()) w = Character.toUpperCase(w.charAt(0)) + w.substring(1);
        if (rng.nextInt(3) == 0) w = w.replace('a', '@').replace('o', '0').replace('e', '3');
        String digits = String.valueOf(rng.nextInt(10000));
        String symbol = rng.nextBoolean() ? String.valueOf("!?#$*".charAt(rng.nextInt(5))) : "";
        return w + digits + symbol;
    }

    private static String random(Random rng, String alphabet, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
        return sb.toString();
    }

    private static String pick(Random rng, List<String> list) {
        return list.get(rng.nextInt(list.size()));
    }
}
