package com.clorabase.clorastore;

import androidx.core.util.Predicate;

import com.clorabase.GithubUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The only class to access whole database.
 */
public class ClorastoreDatabase {
    private String PATH = "";
    private final Executor executor = Executors.newCachedThreadPool();
    private static ClorastoreDatabase INSTANCE;

    private ClorastoreDatabase(String name) {
        if (name.startsWith("/"))
            name = name.substring(1);
        if (name.endsWith("/"))
            name = name.substring(0, name.length() - 1);

        PATH += name + "/";
    }

    public static ClorastoreDatabase getInstance(String project) {
        if (INSTANCE == null) {
            INSTANCE = new ClorastoreDatabase(project + "/db");
            DatabaseUtils.init(GithubUtils.token,project);
        }
        return INSTANCE;
    }

    /**
     * Goes into a collection creating it if not already exist.
     *
     * @param name The name of the collection you want to go.
     * @return new instance with this collection.
     */
    public ClorastoreDatabase collection(@NotNull String name) {
        return new ClorastoreDatabase(PATH + name);
    }

    /**
     * Creates or updates document with the provided data as its fields.
     *
     * @param name   The name of the document
     * @param fields The {@link Map} of fields with its name as key and value as value
     * @return A {@link Tasks} with the id of the document as its successful result
     */
    public Task<Void> document(@NotNull String name, @NotNull Map<String, Object> fields) {
        return Tasks.call(executor, () -> {
            var content = DatabaseUtils.getContent(PATH + name);
            if (content.isEmpty())
                DatabaseUtils.createFile(fields, PATH + name);
            else
                DatabaseUtils.updateFile(new JSONObject(content), new JSONObject(fields), PATH + name);
            return null;
        });
    }

    /**
     * Creates or updates documents with the provided data as its fields. This is used for batch writing to the database.
     *
     * @param name_then_fields Alternate varargs of name & fields for the documents
     * @return A final {@link Tasks} indicating the success or failure of the operation.
     */
    public synchronized Task<Void> documents(Object... name_then_fields) {
        if (name_then_fields.length % 2 != 0)
            throw new IllegalArgumentException("You must provide an even number of arguments. " +
                    "The first argument is the name of the document and the second is the map of fields.");

        var tasks = new ArrayList<Task<Void>>();
        for (int i = 0; i < name_then_fields.length; i += 2) {
            var name = name_then_fields[i].toString();
            var fields = (Map) name_then_fields[i + 1];
            tasks.add(document(name, fields));
        }
        return Tasks.whenAll(tasks);
    }

    /**
     * Gets the data of the document as a Map of its field.
     *
     * @param doc The name of the doc
     * @return Tasks of map
     */
    public Task<Map<String, Object>> getData(@NotNull String doc) {
        return Tasks.call(executor, () -> DatabaseUtils.getContent(PATH + doc));
    }

    /**
     * Deletes the collection or document with this name.
     *
     * @param name The name of the collection or document
     * @return A empty {@link Tasks}.
     */
    public Task<Void> delete(String name) {
        return Tasks.call(executor, () -> {
            DatabaseUtils.deleteFile(PATH + name);
            return null;
        });
    }

    /**
     * Deletes the collection or document with the names
     *
     * @param names The name array of the collection or document to delete
     * @return A empty {@link Tasks}. Result of the operation.
     */
    public Task<Void> batchDelete(String... names) {
        var tasks = new ArrayList<Task<Void>>();
        for (var name : names)
            tasks.add(delete(name));
        return Tasks.whenAll(tasks);
    }

    /**
     * Query database on the basis of a condition
     *
     * @param condition The condition which is to be checked against the query
     * @return A {@link Tasks} of the names of document which matched the query.
     */
    public Task<String[]> query(Predicate<Map<String, Object>> condition, int limit) {
        List<String> docs = new ArrayList<>();
        return Tasks.call(executor, () -> {
            var files = DatabaseUtils.getDocuments(PATH);
            for (DatabaseUtils.GitFile file : files) {
                if (limit == docs.size())
                    break;
                else {
                    var shouldKeep = condition.test(file.content);
                    if (shouldKeep)
                        docs.add(file.name);
                }
            }
            return docs.toArray(new String[0]);
        });
    }

    /**
     * Gets the data of all the documents of this collection.
     * @return A {@link Tasks} of the {@code Map} as the data of documents.
     */
    public Task<Map<String, Object>[]> getDocuments() {
        return Tasks.call(executor, () -> {
            List<Map<String, Object>> list = new ArrayList<>();
            var files = DatabaseUtils.getDocuments(PATH);
            for (DatabaseUtils.GitFile file : files) {
                list.add(file.content);
            }
            return list.toArray(new Map[0]);
        });
    }
}
