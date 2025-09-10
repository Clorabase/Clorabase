package clorabase.sdk.android.clorastore;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import clorabase.sdk.java.database.ClorastoreException;
import clorabase.sdk.java.database.Document;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClorastoreDocument extends Document {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected ClorastoreDocument(String path, String name) {
        super(path, name);
    }

    public static ClorastoreDocument from(Document document){
        return new ClorastoreDocument(document.getPath(), document.getName());
    }

    public Task<Map<String, Object>> getDataAsync() {
        TaskCompletionSource<Map<String, Object>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                Map<String, Object> data = getData();
                mainHandler.post(() -> tcs.setResult(data));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Map<String, Object>> fetchAsync() {
        TaskCompletionSource<Map<String, Object>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                Map<String, Object> data = fetch();
                mainHandler.post(() -> tcs.setResult(data));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> setDataAsync(Map<String, Object> data) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                setData(data);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> putAsync(String key, Object value) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                put(key, value);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> updateAsync(Map<String, Object> fields) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                update(fields);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> addItemAsync(String list, Object value) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                addItem(list, value);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> removeItemAsync(String key) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                removeItem(key);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> deleteAsync() {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                delete();
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }
}

