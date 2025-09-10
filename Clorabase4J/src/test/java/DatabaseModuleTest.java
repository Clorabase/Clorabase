import clorabase.sdk.java.Clorabase;
import clorabase.sdk.java.database.ClorastoreException;
import clorabase.sdk.java.database.Collection;
import clorabase.sdk.java.database.Document;
import clorabase.sdk.java.database.Query;
import org.junit.jupiter.api.*;

import java.io.FileNotFoundException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseModuleTest {
    static Clorabase clorabase;
    static Collection db;
    static Collection users;
    static Document userDoc;

    @BeforeAll
    static void setup() throws Exception {
        // Use test credentials or mock as needed
        clorabase = Clorabase.getInstance("ErrorxCode","API TOKEN HERE","test3");
        db = clorabase.getDatabase();
        users = db.collection("users");
        userDoc = users.document("user1");
    }

    @Test
    @Order(1)
    void testSetAndGetDocumentData() throws Exception {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "Alice");
        userData.put("age", 30);
        userDoc.setData(userData);
        Map<String, Object> fetched = userDoc.getData();
        assertEquals("Alice", fetched.get("name"));
        assertEquals(30, ((Number)fetched.get("age")).intValue());
    }

    @Test
    @Order(2)
    void testUpdateDocument() throws Exception {
        Thread.sleep(5000);
        userDoc.put("age", 31);
        Map<String, Object> updated = userDoc.getData();
        assertEquals(31, ((Number)updated.get("age")).intValue());
    }

    @Test
    @Order(3)
    void testQueryDocuments() throws Exception {
        Query query = users.query();
        List<Document> results = query.where(map -> map.containsKey("age") && ((Number)map.get("age")).intValue() > 25);
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(4)
    void testDeleteDocument() throws Exception {
        userDoc.delete();
        Thread.sleep(5000);
        assertEquals(new HashMap<>(),userDoc.fetch());
    }

}

