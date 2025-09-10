package clorabase.sdk.android.clorastore;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import clorabase.sdk.java.database.ClorastoreException;
import clorabase.sdk.java.database.Collection;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ClorastoreCollection extends Collection {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Creates a new collection with the given parent, path and name. MUST not used directly
     * as it is used internally by the SDK.
     *
     * @param parent The parent collection of this collection.
     * @param path   The path of the collection in the database.
     * @param name   The name of the collection.
     */
    protected ClorastoreCollection(Collection parent, String path, String name) {
        super(parent, path, name);
    }

    public static ClorastoreCollection from(Collection collection) {
        return new ClorastoreCollection(collection.getParent(), collection.getPath(), collection.getName());
    }

    public ClorastoreCollection collection(String name) {
        return new ClorastoreCollection(this, path + "/" + name, name);
    }

    public Task<List<ClorastoreDocument>> getDocumentsAsync() {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> docs = getDocuments().stream()
                        .map(ClorastoreDocument::from)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(docs));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<List<String>> getCollectionsAsync() {
        TaskCompletionSource<List<String>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<String> cols = getCollections();
                mainHandler.post(() -> tcs.setResult(cols));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<Void> deleteAsync(String name) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                delete(name);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public ClorastoreDocument documentAsync(String name) {
        return new ClorastoreDocument(path, name);
    }

    public ClorastoreQuery queryAsync() {
        return new ClorastoreQuery(this);
    }
}

