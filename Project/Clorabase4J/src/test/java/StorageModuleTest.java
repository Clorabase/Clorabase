import clorabase.sdk.java.Clorabase;
import clorabase.sdk.java.storage.ClorabaseStorage;
import clorabase.sdk.java.storage.ProgressListener;
import clorabase.sdk.java.storage.StorageException;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StorageModuleTest {
    static Clorabase clorabase;
    static ClorabaseStorage storage;
    static final String TEST_FILE = "last1.txt";
    static final byte[] TEST_CONTENT = "Hello, Clorabase..!".getBytes();

    @BeforeAll
    static void setup() throws Exception {
        var apiKey = System.getenv("github");
        assertNotNull(apiKey);
        clorabase = Clorabase.getInstance("ErrorxCode",apiKey,"test_v6");
        storage = clorabase.getStorage();
    }

    @Test
    @Order(1)
    void testAddFile() throws Exception {
        storage.addFile(TEST_CONTENT, TEST_FILE);
        byte[] fetched = storage.getFile(TEST_FILE);
        assertArrayEquals(TEST_CONTENT, fetched);
    }

    @Test
    @Order(2)
    void testUploadFile() throws Exception {
        File file = new File("D:\\Android studio projects\\Clorabase\\Clorabase4J\\src\\main\\java\\clorabase\\sdk\\java\\Quota.java");
        try (InputStream input = new FileInputStream(file)) {
            storage.uploadFile(input, "DatabaseModuleTest.java", new ProgressListener() {
                @Override
                public void onProgress(long bytesRead, long totalBytes) {
                    System.out.println("Uploaded: " + bytesRead + "/" + totalBytes);
                }

                @Override
                public void onComplete(String result) {
                }

                @Override
                public void onError(Exception e) {
                }
            });
        }
        byte[] fetched = storage.getFile("DatabaseModuleTest.java");
        assertNotNull(fetched);
    }

    @Test
    @Order(3)
    void testListFiles() throws Exception {
        List<String> files = storage.listFiles();
        assertTrue(files.contains(TEST_FILE));
    }

    @Test
    @Order(4)
    void testUploadFileWithProgress() {
        InputStream input = new ByteArrayInputStream(TEST_CONTENT);
        final boolean[] completed = {false};
        storage.uploadBlob(input, "blob_" + TEST_FILE, new ProgressListener() {
            @Override
            public void onProgress(long bytesRead, long totalBytes) {
                // Optionally assert progress
            }
            @Override
            public void onComplete(String result) {
                completed[0] = true;
            }
            @Override
            public void onError(Exception e) {
                fail("Upload failed: " + e.getMessage());
            }
        });
        assertTrue(completed[0], "Upload did not complete");
    }

    @Test
    @Order(5)
    void testDeleteFile() throws Exception {
        assertDoesNotThrow((NamedExecutable) () -> {
            Thread.sleep(5000);
            storage.deleteBlob("blob_" + TEST_FILE);
            storage.delete(TEST_FILE);
            storage.delete("DatabaseModuleTest.java");
        });
    }
}
