import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private Database db;
    private UserService userService;

    @BeforeEach
    void setUp() {
        db = mock(Database.class);
        userService = new UserService(db);
    }

    @Test
    void testFind_Success() {
        String id = "123";
        User expectedUser = new User("123", "John Doe");
        when(db.run("SELECT * FROM users WHERE id=123")).thenReturn(expectedUser);

        User result = userService.find(id);

        assertEquals(expectedUser, result);
        verify(db).run("SELECT * FROM users WHERE id=123");
    }

    @Test
    void testFind_UserNotFound() {
        String id = "999";
        when(db.run("SELECT * FROM users WHERE id=999")).thenReturn(null);

        User result = userService.find(id);

        assertNull(result);
    }

    @Test
    void testFind_EdgeCaseEmptyId() {
        String id = "";
        when(db.run("SELECT * FROM users WHERE id=")).thenReturn(null);

        User result = userService.find(id);

        assertNull(result);
    }

    @Test
    void testFind_SqlInjectionVulnerability() {
        // This test highlights the security flaw in the original code
        String id = "1 OR 1=1";
        when(db.run("SELECT * FROM users WHERE id=1 OR 1=1")).thenReturn(new User("1", "Admin"));

        User result = userService.find(id);

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    @Test
    void testFind_DatabaseThrowsException() {
        String id = "123";
        when(db.run(anyString())).thenThrow(new RuntimeException("Database Error"));

        assertThrows(RuntimeException.class, () -> userService.find(id));
    }
}