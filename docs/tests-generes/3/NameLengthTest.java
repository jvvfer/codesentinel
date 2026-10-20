import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class NameLengthTest {

    private NameLengthService service;

    @BeforeEach
    public void setUp() {
        service = new NameLengthService();
    }

    @Test
    public void testNameLengthNormalCase() {
        Map<String, String> users = new HashMap<>();
        users.put("1", "Alice");
        assertEquals(5, service.nameLength(users, "1"));
    }

    @Test
    public void testNameLengthEmptyString() {
        Map<String, String> users = new HashMap<>();
        users.put("1", "");
        assertEquals(0, service.nameLength(users, "1"));
    }

    @Test
    public void testNameLengthNullKeyThrowsException() {
        Map<String, String> users = new HashMap<>();
        users.put("1", "Alice");
        assertThrows(NullPointerException.class, () -> service.nameLength(users, null));
    }

    @Test
    public void testNameLengthMissingKeyThrowsException() {
        Map<String, String> users = new HashMap<>();
        assertThrows(NullPointerException.class, () -> service.nameLength(users, "nonExistent"));
    }

    @Test
    public void testNameLengthNullValueThrowsException() {
        Map<String, String> users = new HashMap<>();
        users.put("1", null);
        assertThrows(NullPointerException.class, () -> service.nameLength(users, "1"));
    }
}