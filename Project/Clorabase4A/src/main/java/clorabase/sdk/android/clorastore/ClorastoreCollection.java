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

/**
 * Represents a collection in the Clorastore database.
 * <p>
 * A collection is a container for documents and sub-collections. This class provides methods
 * to navigate the database hierarchy, perform queries, and manage documents.
 * All asynchronous operations are executed on a background thread, while results
 * are posted back to the main thread via {@link Task}.
 */
public class ClorastoreCollection {
    private final Collection coreCollection;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Creates a new collection with the given parent, path and name. MUST not used directly
     * as it is used internally by the SDK.
     */
    public ClorastoreCollection(Collection core) {
        coreCollection = core;
    }


    /**
     * Goes into the specified collection.
     *
     * @param name The name of the collection.
     * @return The new collection.
     */
    public ClorastoreCollection collection(String name) {
        return new ClorastoreCollection(coreCollection.collection(name));
    }

    /**
     * Gets all the documents in this collection.
     * @return A list of documents in this collection.
     */
    public Task<List<ClorastoreDocument>> getDocuments() {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> docs = coreCollection.getDocuments()
                        .stream()
                        .map(ClorastoreDocument::new)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(docs));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Gets all the collections in this collection.
     * @return A list of collections in this collections name.
     */
    public Task<List<String>> getCollections() {
        TaskCompletionSource<List<String>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<String> cols = coreCollection.getCollections();
                mainHandler.post(() -> tcs.setResult(cols));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Deletes a document from this collection.
     * @param name The name of the document.
     * @return A task that completes when the document is deleted.
     */
    public Task<Void> delete(String name) {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                coreCollection.delete(name);
                mainHandler.post(() -> tcs.setResult(null));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Gets the document with the given name. If the document does not exist, it will be created upon any operation.
     *
     * @param name The name of the document
     * @return The document.
     */
    public ClorastoreDocument document(String name) {
        return new ClorastoreDocument(coreCollection.document(name));
    }

    /**
     * Return a Query object that can be used to perform queries on the collection.
     *
     * @return A Query object that can be used to perform queries on the collection.
     */
    public ClorastoreQuery query() {
        return new ClorastoreQuery(coreCollection.query());
    }
}

