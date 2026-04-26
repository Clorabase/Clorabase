package clorabase.sdk.android;

import static org.junit.jupiter.api.Assertions.*;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.junit.rules.BackgroundTestRule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import clorabase.sdk.android.clorastore.ClorastoreCollection;
import clorabase.sdk.android.clorastore.ClorastoreDocument;
import clorabase.sdk.android.clorastore.ClorastoreQuery;
import tech.apter.junit.jupiter.robolectric.RobolectricExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(RobolectricExtension.class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
public class DatabaseModuleTest {
    static Clorabase clorabase;
    static ClorastoreCollection db;
    static ClorastoreCollection users;
    static ClorastoreDocument userDoc;


    @Test
    @Order(1)
    public void testSetAndGetDocumentData() throws Exception {
        var apiKey = System.getenv("github");
        assertNotNull(apiKey);
        clorabase = Clorabase.getInstance("ErrorxCode",apiKey,"test_v6");
        db = clorabase.getDatabase();
        users = db.collection("users");
        userDoc = users.document("user11");


        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "Alice");
        userData.put("age", 30);
        Tasks.await(userDoc.setData(userData));
        Thread.sleep(5000); // Shorter sleep is usually sufficient
        Map<String, Object> fetched = Tasks.await(userDoc.fetch());
        assertEquals("Alice", fetched.get("name"));
        assertEquals(30, ((Number)fetched.get("age")).intValue());
    }

    @Test
    @Order(2)
    public void testUpdateDocument() throws Exception {
        Thread.sleep(2000); // Shorter sleep is usually sufficient
        Tasks.await(userDoc.put("age", 31));
        Map<String, Object> updated = Tasks.await(userDoc.getData());
        assertEquals(31, ((Number)updated.get("age")).intValue());
    }

    @Test
    @Order(3)
    public void testQueryDocuments() throws Exception {
        ClorastoreQuery query = users.query();
        List<ClorastoreDocument> results = Tasks.await(query.where(map -> {
            System.out.println(map);
            return true;
        }));
        System.out.println(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(4)
    public void testDeleteDocument() throws Exception {
        Tasks.await(userDoc.delete());
        Thread.sleep(2000);
    }
}
