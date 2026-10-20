package com.codesentinel;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class BenchmarkService {

    public record Sample(int id, String name, String code, String expectedRegex) {}

    public record Result(int id, String name, boolean expectsIssue, boolean detected,
                         int extraFindings, int totalFindings, long latencyMs, int score) {}

    private static final List<Sample> SAMPLES = List.of(
            new Sample(1, "Injection SQL", """
            public User find(String id) {
                String q = "SELECT * FROM users WHERE id=" + id;
                return db.run(q);
            }
            """, "sql|injection|cwe-89"),

            new Sample(2, "Secret en dur", """
            public class PaymentClient {
                private static final String API_KEY = "sk_live_9f8e7d6c5b4a3210";
                public void pay(double amount) {
                    http.post("https://pay.example.com/charge", API_KEY, amount);
                }
            }
            """, "798|secret|en dur|hardcod|codé"),

            new Sample(3, "NullPointerException", """
            public int nameLength(Map<String, String> users, String key) {
                String name = users.get(key);
                return name.length();
            }
            """, "null|npe|nullpointer"),

            new Sample(4, "Fuite de ressource", """
            public String readFirstLine(String path) throws IOException {
                BufferedReader reader = new BufferedReader(new FileReader(path));
                return reader.readLine();
            }
            """, "ressource|close|ferm|fuite|leak|try-with|libér"),

            new Sample(5, "Injection de commande", """
            public String ping(String host) throws IOException {
                Process p = Runtime.getRuntime().exec("ping -c 1 " + host);
                return new String(p.getInputStream().readAllBytes());
            }
            """, "commande|command|cwe-78|injection"),

            new Sample(6, "Code sain (témoin)", """
            public int add(int a, int b) {
                return Math.addExact(a, b);
            }
            """, null)
    );

    private final AnalysisService analysis;

    public BenchmarkService(AnalysisService analysis) {
        this.analysis = analysis;
    }

    public List<Sample> samples() {
        return SAMPLES;
    }

    public Result run(int id, String model, boolean verify) {
        Sample s = SAMPLES.stream().filter(x -> x.id() == id).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown sample"));

        // unique comment so the cache never hides the real latency
        String code = s.code() + "\n// bench-" + System.nanoTime();
        AnalysisResponse r = analysis.analyze(code, model, verify);

        int total = r.findings().size();
        boolean expects = s.expectedRegex() != null;
        boolean detected;
        int extra;

        if (expects) {
            Pattern p = Pattern.compile(s.expectedRegex(),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            long matched = r.findings().stream()
                    .filter(f -> p.matcher(f.title() + " " + f.explanation()).find())
                    .count();
            detected = matched > 0;
            extra = (int) (total - matched);
        } else {
            detected = total == 0;   // control sample: nothing should be reported
            extra = total;
        }
        return new Result(s.id(), s.name(), expects, detected, extra, total, r.latencyMs(), r.score());
    }
}