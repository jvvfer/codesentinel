import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    private final FileService service = new FileService();

    @TempDir
    Path tempDir;

    @Test
    void testReadFirstLineNormal() throws IOException {
        Path file = tempDir.resolve("test.txt");
        java.nio.file.Files.writeString(file, "Hello World\nSecond Line");
        assertEquals("Hello World", service.readFirstLine(file.toString()));
    }

    @Test
    void testReadFirstLineEmptyFile() throws IOException {
        Path file = tempDir.resolve("empty.txt");
        java.nio.file.Files.writeString(file, "");
        assertNull(service.readFirstLine(file.toString()));
    }

    @Test
    void testReadFirstLineSingleLineNoNewline() throws IOException {
        Path file = tempDir.resolve("single.txt");
        java.nio.file.Files.writeString(file, "Just one line");
        assertEquals("Just one line", service.readFirstLine(file.toString()));
    }

    @Test
    void testReadFirstLineFileNotFound() {
        assertThrows(FileNotFoundException.class, () -> {
            service.readFirstLine("non_existent_file.txt");
        });
    }

    @Test
    void testReadFirstLineDirectoryPath() {
        assertThrows(IOException.class, () -> {
            service.readFirstLine(tempDir.toString());
        });
    }
}