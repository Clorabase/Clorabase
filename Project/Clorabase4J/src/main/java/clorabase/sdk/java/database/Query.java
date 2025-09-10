package clorabase.sdk.java.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * A class that handle querying of data from the database. It contains all the functions that you can use
 * to retrieve data by filtering and ordering. Currently, only single query is supported with no indexing.
 */
public class Query {
    private List<Document> documents;
    private final Future<List<Document>> documentsFuture;

    public Query(Collection collection) {
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            documentsFuture = executor.submit(collection::getDocuments);
        }
    }

    private List<Document> getDocumentsSync() throws ClorastoreException {
        try {
            if (documents == null) {
                documents = documentsFuture.get(10, TimeUnit.SECONDS);
            }
            return documents;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClorastoreException("Failed to fetch documents", e);
        }
    }

    /**
     * Returns a list of documents that match the given condition.
     * @param condition The condition to be checked
     * @return The List of Document as the result, or null if error occurs
     * @throws Exception if any error occurs during query
     */
    public List<Document> where(Predicate<Map<String, Object>> condition) throws ClorastoreException {
        var result = new ArrayList<Document>();
        for (Document doc : getDocumentsSync()) {
            var docData = doc.fetch();
            if (condition.test(docData)) {
                result.add(doc);
            }
        }
        return result;
    }

    /**
     * Returns a list of documents that has the given field value greater than the provided value
     * @param field The field to be checked
     * @param value The value to check against
     * @return The List of Document as the result, or null if error occurs
     * @throws Exception if any error occurs during query
     */
    public List<Document> whereGreater(String field, Number value) throws ClorastoreException {
        var result = new ArrayList<Document>();
        for (Document doc : getDocumentsSync()) {
            var docData = doc.fetch();
            var v = (Number) docData.get(field);
            if (v != null && v.doubleValue() > value.doubleValue()) {
                result.add(doc);
            }
        }
        return result;
    }

    /**
     * Returns a list of documents that has the given field value less than the provided value
     * @param field The field to check
     * @param value The value to check against of the field
     * @return The List of Document as the result, or null if error occurs
     * @throws Exception if any error occurs during query
     */
    public List<Document> whereLess(String field, Number value) throws ClorastoreException {
        var result = new ArrayList<Document>();
        for (Document doc : getDocumentsSync()) {
            var docData = doc.fetch();
            var v = (Number) docData.get(field);
            if (v != null && v.doubleValue() < value.doubleValue()) {
                result.add(doc);
            }
        }
        return result;
    }

    /**
     * Returns a list of documents that has the given field value equal to the provided value
     * @param field The field to check
     * @param value The value to check against of the field
     * @return The List of Document as the result, or null if error occurs
     * @throws Exception if any error occurs during query
     */
    public List<Document> whereEqual(String field, Object value) throws ClorastoreException {
        var result = new ArrayList<Document>();
        for (Document doc : getDocumentsSync()) {
            var docData = doc.fetch();
            if (Objects.equals(docData.get(field), value)) {
                result.add(doc);
            }
        }
        return result;
    }

    /**
     * Returns a list of documents ordered by the given field in ascending order
     * @param field The field on which the documents are to be ordered
     * @param limit Max number of documents you wish to fetch
     * @return The List of Document as the result, or null if error occurs
     * @throws Exception if any error occurs during query
     */
    public List<Document> orderBy(String field, int limit) throws ClorastoreException {
        var queue = new PriorityQueue<Document>((a, b) -> {
            try {
                var aData = a.fetch();
                var bData = b.fetch();
                var aVal = (Number) aData.get(field);
                var bVal = (Number) bData.get(field);
                if (aVal == null || bVal == null) return 0;
                return Double.compare(aVal.doubleValue(), bVal.doubleValue());
            } catch (ClorastoreException e) {
                throw new RuntimeException(e);
            }
        });
        for (Document doc : getDocumentsSync()) {
            var docData = doc.fetch();
            if (docData.containsKey(field) && docData.get(field) instanceof Number) {
                queue.offer(doc);
                if (queue.size() > limit)
                    queue.poll();
            }
        }
        var result = new ArrayList<>(queue);
        result.sort((a, b) -> {
           try {
               var aVal = (Number) a.fetch().get(field);
               var bVal = (Number) b.fetch().get(field);
               if (aVal == null || bVal == null) return 0;
               return Double.compare(aVal.doubleValue(), bVal.doubleValue());
           } catch (ClorastoreException e) {
               throw new RuntimeException(e);
           }
        });
        return result;
    }
}
