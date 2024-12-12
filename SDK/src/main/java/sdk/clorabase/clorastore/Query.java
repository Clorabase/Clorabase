package sdk.clorabase.clorastore;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A class that handle querying of data from the database. It contains all the functions that you can use
 * to retrieve data by filtering and ordering. Currently, only single query is supported with no indexing.
 */
public class Query {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Task<List<Document>> documentTask;

    public Query(Collection collection) {
        this.documentTask = collection.getDocuments();
    }

    /**
     * Returns a list of documents that match the given condition.
     * @param condition The condition to be checked
     * @return A {@link Tasks} of the {@code List} of {@link Document} as the result
     */
    public Task<List<Document>> where(Predicate<Map<String, Object>> condition) {
        return Tasks.call(executor, () -> {
            var result = new ArrayList<Document>();
            var docs = Tasks.await(documentTask, 10, TimeUnit.SECONDS);
            for (Document doc : docs) {
                var docData = Tasks.await(doc.getData(), 5, TimeUnit.SECONDS);
                if (condition.test(docData))
                    result.add(doc);
            }
            return result;
        });
    }

    /**
     * Returns a list of documents that has the given field value greater then the provided value
     * @param field The field to be checked
     * @param value The value to check against
     * @return A {@link Tasks} of the {@code List} of {@link Document} as the result
     */
    public Task<List<Document>> whereGreater(String field, Number value) {
        return where(data -> {
            var v = (Number) data.get(field);
            return v != null && v.doubleValue() > value.doubleValue();
        });
    }

    /**
     * Returns a list of documents that has the given field value less then the provided value
     * @param field The field to check
     * @param value The value to check against of the field
     * @return A {@link Tasks} of the {@code List} of {@link Document} as the result
     */
    public Task<List<Document>> whereLess(String field, Number value) {
        return where(data -> {
            var v = (Number) data.get(field);
            return v != null && v.doubleValue() < value.doubleValue();
        });
    }


    public Task<List<Document>> whereEqual(String field, Object value) {
        return where(data -> Objects.equals(data.get(field), value));
    }

    /**
     * Returns a list of documents that has the given field orders them by the given field in ascending order
     * @param field The field on which the document are to be orders
     * @param limit Max number of documents you wish to fetch
     * @return A {@link Tasks} of the {@code List} of {@link Document} as the result
     */
    public Task<List<Document>> orderBy(String field, int limit) {
        return Tasks.call(executor, () -> {
            var docs = Tasks.await(documentTask, 10, TimeUnit.SECONDS);
            var queue = new PriorityQueue<Document>();

            for (Document doc : docs) {
                doc.orderingField = field;
                var mDoc = Tasks.await(doc.getData(), 5, TimeUnit.SECONDS);
                queue.offer(doc);

                if (queue.size() > limit)
                    queue.poll();
            }


            return queue.stream().filter(document -> {
                var data = document.data;
                return data.containsKey(field) && data.get(field) instanceof Number;
            }).sorted().collect(Collectors.toList());
        });
    }
}
