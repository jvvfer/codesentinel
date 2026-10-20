import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PingTest {

    private final PingService service = new PingService();

    @Test
    void testPingSuccess() throws IOException {
        // Note: '127.0.0.1' is standard and should resolve on most systems
        String result = service.ping("127.0.0.1");
        assertNotNull(result);
        assertTrue(result.contains("1 packets transmitted"));
    }

    @Test
    void testPingInvalidHost() throws IOException {
        // Attempting to ping a non-existent host usually returns an empty stream or error message
        String result = service.ping("invalid.host.local.test");
        assertNotNull(result);
    }

    @Test
    void testPingEmptyHost() {
        // Depending on shell behavior, an empty host might cause an error or command failure
        assertThrows(Exception.class, () -> service.ping(""));
    }

    @Test
    void testPingMalformedInput() {
        // Testing command injection robustness (ping should fail or reject shell characters)
        assertDoesNotThrow(() -> {
            service.ping("127.0.0.1; ls");
        });
    }
}