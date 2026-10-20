import java.io.IOException;
class PingService {
    // --- extrait original ---
    public String ping(String host) throws IOException {
        Process p = Runtime.getRuntime().exec("ping -c 1 " + host);
        return new String(p.getInputStream().readAllBytes());
    }
}
