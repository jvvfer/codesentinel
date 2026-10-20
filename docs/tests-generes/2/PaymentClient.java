interface HttpService { void post(String url, String key, double amount); }
class PaymentClient {
    private final HttpService http;
    PaymentClient(HttpService http) { this.http = http; }
    // --- extrait original ---
    private static final String API_KEY = "sk_live_9f8e7d6c5b4a3210";
    public void pay(double amount) {
        http.post("https://pay.example.com/charge", API_KEY, amount);
    }
}
