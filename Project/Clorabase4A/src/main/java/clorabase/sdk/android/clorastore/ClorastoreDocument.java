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

/**
 * Represents a document in the Clorastore database.
 * This class provides asynchronous methods to interact with document data,
 * returning {@link Task} objects and ensuring callbacks are executed on the main UI thread.
 *
 * <p>A document contains data in the form of key-value pairs and can also
 * contain nested collections.</p>
 *
 * @author Clorabase
 * @since 1.0
 */
public class ClorastoreDocument {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Document coreDocument;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected ClorastoreDocument(Document core) {
        coreDocument = core;
    }

    /**
     * Retrieves the data of this document. This operation is performed asynchronously.
     *
     * @return A {@link Task} that will contain a {@link Map} of the document's data if successful.
     */
    public Task<Map<String, Object>> getData() {
        TaskCompletionSource<Map<String, Object>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                Map<String, Object> data = coreDocument.getData();
                mainHandler.post(() -> tcs.setResult(data));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Fetches the latest data of this document from the server.
     *
     * @return A {@link Task} that will contain a {@link Map} of the fetched data.
     */
    public Task<Map<String, Object>> fetch() {
        TaskCompletionSource<Map<String, Object>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                Map<String, Object> data = coreDocument.fetch();
                mainHandler.post(() -> tcs.setResult(data));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Overwrites the data of this document with the provided map.
     *
     * @param data The new data to set for this document.
     * @return A {@link Task} that completes when the operation is done.
     */
    public Task<Void> setData(Map<String, Object> data) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreDocument.setData(data);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Adds or updates a single field in the document.
     *
     * @param key   The field name.
     * @param value The value to set.
     * @return A {@link Task} that completes when the operation is done.
     */
    public Task<Void> put(String key, Object value) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreDocument.put(key, value);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Updates specific fields in the document. Fields not in the map remain unchanged.
     *
     * @param fields A map of fields to update.
     * @return A {@link Task} that completes when the operation is done.
     */
    public Task<Void> update(Map<String, Object> fields) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreDocument.update(fields);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Appends an item to a list field in the document.
     *
     * @param list  The name of the list field.
     * @param value The value to add to the list.
     * @return A {@link Task} that completes when the operation is done.
     */
    public Task<Void> addItem(String list, Object value) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreDocument.addItem(list, value);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Removes a field from the document.
     *
     * @param key The name of the field to remove.
     * @return A {@link Task} that completes when the operation is done.
     */
    public Task<Void> removeItem(String key) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreDocument.removeItem(key);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Deletes this document from the database.
     *
     * @return A {@link Task} that completes when the document is deleted.
     */
    public Task<Void> delete() {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreDocument.delete();
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }
}
