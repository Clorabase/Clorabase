package clorabase.sdk.android.clorastore;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import clorabase.sdk.java.database.ClorastoreException;
import clorabase.sdk.java.database.Query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a query that can be executed to retrieve a filtered and ordered list of documents.
 * Queries are performed asynchronously, and results are delivered via {@link Task}.
 * Callbacks are executed on the main thread.
 *
 * @author Clorabase
 * @since 1.0
 */
public class ClorastoreQuery {
    private final Query coreQuery;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected ClorastoreQuery(Query query) {
        coreQuery = query;
    }

    /**
     * Filters the query results based on a custom predicate.
     *
     * @param condition The condition to filter documents.
     * @return A {@link Task} containing the list of filtered documents.
     */
    public Task<List<ClorastoreDocument>> where(Predicate<Map<String, Object>> condition) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = coreQuery.where(condition)
                        .stream()
                        .map(ClorastoreDocument::new)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Filters the query results where the specified field's value is greater than the given number.
     *
     * @param field The field to compare.
     * @param value The value to compare against.
     * @return A {@link Task} containing the list of matching documents.
     */
    public Task<List<ClorastoreDocument>> whereGreater(String field, Number value) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = coreQuery.whereGreater(field, value)
                        .stream()
                        .map(ClorastoreDocument::new)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Filters the query results where the specified field's value is less than the given number.
     *
     * @param field The field to compare.
     * @param value The value to compare against.
     * @return A {@link Task} containing the list of matching documents.
     */
    public Task<List<ClorastoreDocument>> whereLess(String field, Number value) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = coreQuery.whereLess(field, value)
                        .stream()
                        .map(ClorastoreDocument::new)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Filters the query results where the specified field's value is equal to the given object.
     *
     * @param field The field to compare.
     * @param value The value to compare against.
     * @return A {@link Task} containing the list of matching documents.
     */
    public Task<List<ClorastoreDocument>> whereEqual(String field, Object value) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = coreQuery.whereEqual(field, value)
                        .stream()
                        .map(ClorastoreDocument::new)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    /**
     * Orders the query results by the specified field and limits the number of results.
     *
     * @param field The field to order by.
     * @param limit The maximum number of documents to return.
     * @return A {@link Task} containing the ordered and limited list of documents.
     */
    public Task<List<ClorastoreDocument>> orderBy(String field, int limit) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = coreQuery.orderBy(field, limit)
                        .stream()
                        .map(ClorastoreDocument::new)
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }
}
