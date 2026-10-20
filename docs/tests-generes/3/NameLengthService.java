import java.util.Map;
class NameLengthService {
    // --- extrait original ---
    public int nameLength(Map<String, String> users, String key) {
        String name = users.get(key);
        return name.length();
    }
}
