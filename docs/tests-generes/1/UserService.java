class User { private final String id, name; User(String id, String name) { this.id = id; this.name = name; } String getId() { return id; } }
interface Database { User run(String q); }
class UserService {
    private final Database db;
    UserService(Database db) { this.db = db; }
    // --- extrait original ---
    public User find(String id) {
        String q = "SELECT * FROM users WHERE id=" + id;
        return db.run(q);
    }
}
