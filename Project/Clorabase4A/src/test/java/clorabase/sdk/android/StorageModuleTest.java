package clorabase.sdk.android;

import static org.junit.jupiter.api.Assertions.*;

import com.google.android.gms.tasks.Tasks;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.NamedExecutable;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robolectric.annotation.LooperMode;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import clorabase.sdk.android.storage.ClorabaseStorage;
import clorabase.sdk.android.storage.ProgressListener;
import tech.apter.junit.jupiter.robolectric.RobolectricExtension;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ExtendWith(RobolectricExtension.class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
public class StorageModuleTest {
    static Clorabase clorabase;
    static ClorabaseStorage storage;
    static final String TEST_FILE = "last1.txt";
    static final byte[] TEST_CONTENT = "Hello, Clorabase..!".getBytes();


    @Test
    @Order(1)
    public void testAddFile() throws Exception {
        var apiKey = System.getenv("github");
        assertNotNull(apiKey);
        clorabase = Clorabase.getInstance("ErrorxCode",apiKey,"test_v6");
        storage = clorabase.getStorage();


        storage.addFile(TEST_CONTENT, TEST_FILE);
        byte[] fetched = Tasks.await(storage.getFile(TEST_FILE));
        assertArrayEquals(TEST_CONTENT, fetched);
    }

    @Test
    @Order(2)
    public void testUploadFile() throws Exception {
        File file = new File("D:\\Android studio projects\\Clorabase\\Clorabase4J\\src\\main\\java\\clorabase\\sdk\\java\\Quota.java");
        storage.uploadFile(new FileInputStream(file), "DatabaseModuleTest.java", new ProgressListener() {
            @Override
            public void onProgress(long bytesRead, long totalBytes) {
                System.out.println("Uploaded: " + bytesRead + "/" + totalBytes);
            }

            @Override
            public void onComplete() {
                System.out.println("Upload completed");
            }


            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        byte[] fetched = Tasks.await(storage.getFile("DatabaseModuleTest.java"));
        assertNotNull(fetched);
    }

    @Test
    @Order(3)
    public void testListFiles() throws Exception {
        List<String> files = Tasks.await(storage.listFiles());
        assertTrue(files.contains(TEST_FILE));
    }

    @Test
    @Order(4)
    public void testUploadFileWithProgress() {
        InputStream input = new ByteArrayInputStream(TEST_CONTENT);
        storage.uploadBlob(input, "blob_" + TEST_FILE, new ProgressListener() {
            @Override
            public void onProgress(long bytesRead, long totalBytes) {
                System.out.println("Uploaded: " + bytesRead + "/" + totalBytes);
            }

            @Override
            public void onComplete() {
                System.out.println("Upload completed");
            }

            @Override
            public void onError(Exception e) {
                fail("Upload failed: " + e.getMessage());
            }
        });
    }

    @Test
    @Order(5)
    public void testDeleteFile() {
        assertDoesNotThrow((NamedExecutable) () -> {
            Thread.sleep(5000);
            Tasks.await(storage.deleteBlob("blob_" + TEST_FILE));
            Tasks.await(storage.delete(TEST_FILE));
            Tasks.await(storage.delete("DatabaseModuleTest.java"));
        });
    }
}
