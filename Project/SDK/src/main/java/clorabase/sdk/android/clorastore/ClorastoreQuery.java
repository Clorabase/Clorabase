package clorabase.sdk.android.clorastore;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import clorabase.sdk.java.database.ClorastoreException;
import clorabase.sdk.java.database.Collection;
import clorabase.sdk.java.database.Query;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClorastoreQuery extends Query {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ClorastoreQuery(Collection collection) {
        super(collection);
    }

    public Task<List<ClorastoreDocument>> whereAsync(Predicate<Map<String, Object>> condition) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = where(condition)
                        .stream()
                        .map(doc -> new ClorastoreDocument(doc.path, doc.name))
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<List<ClorastoreDocument>> whereGreaterAsync(String field, Number value) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = whereGreater(field, value)
                        .stream()
                        .map(doc -> new ClorastoreDocument(doc.path, doc.name))
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<List<ClorastoreDocument>> whereLessAsync(String field, Number value) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = whereLess(field, value)
                        .stream()
                        .map(doc -> new ClorastoreDocument(doc.path, doc.name))
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<List<ClorastoreDocument>> whereEqualAsync(String field, Object value) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = whereEqual(field, value)
                        .stream()
                        .map(doc -> new ClorastoreDocument(doc.path, doc.name))
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }

    public Task<List<ClorastoreDocument>> orderByAsync(String field, int limit) {
        TaskCompletionSource<List<ClorastoreDocument>> tcs = new TaskCompletionSource<>();
        executor.submit(() -> {
            try {
                List<ClorastoreDocument> result = orderBy(field, limit)
                        .stream()
                        .map(doc -> new ClorastoreDocument(doc.path, doc.name))
                        .collect(Collectors.toList());
                mainHandler.post(() -> tcs.setResult(result));
            } catch (ClorastoreException e) {
                mainHandler.post(() -> tcs.setException(e));
            }
        });
        return tcs.getTask();
    }
}

