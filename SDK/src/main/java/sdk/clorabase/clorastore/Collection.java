package sdk.clorabase.clorastore;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import sdk.clorabase.Clorabase;

/**
 * This class represents a collection in the database. A collection is a group of documents or other collections.
 * To know more about collections, see {@link <a href="https://www.mongodb.com/docs/compass/current/collections/">...</a>}
 */
public class Collection {
    protected final Collection previous;
    protected final Executor executor = Executors.newCachedThreadPool();
    protected static final String BASE_PATH = "/db/";
    protected final DBUtils utils;
    protected final String path;
    protected final String name;

    public Collection(Collection previous, DBUtils utils, String path, String name) {
        this.previous = previous;
        this.utils = utils;
        this.path = path;
        this.name = name;
    }

    /**
     * Creates a new collection with the given name.
     * @param name The name of the collection.
     * @return The new collection.
     */
    public Collection collection(String name) {
        name = name.toLowerCase().trim();
        if (name.contains("/") || name.contains(" ") || name.contains("\\")) {
            throw new IllegalArgumentException("Invalid collection name, can only include alphabet and numbers");
        } else
            return new Collection(this, utils, path + "/" + name, name);
    }

    /**
     * Gets the document with the given name along with the data. If the document does not exist, it will be created upon any operation.
     * @param name The name of the document.
     * @return The document.
     */
    public Document document(@NotNull String name) {
        name = name.toLowerCase().trim();
        if (name.contains("/") || name.contains(" ") || name.contains("\\")) {
            throw new IllegalArgumentException("Invalid document name, can only include alphabet and numbers");
        }
        return new Document(path, utils, name);
    }

    /**
     * Gets reference to all the documents in the collection. This is intensive operation,
     * should not be called when you don't need the data of each document
     * @return The list of documents.
     */
    public Task<List<Document>> getDocuments() {
        return Tasks.call(executor, () -> utils.list(path, true).stream().map(s -> {
            s = s.replace(".doc","");
            return new Document(path, utils, s);
        }).collect(Collectors.toList()));
    }

    public Task<List<String>> getCollections(){
        return Tasks.call(executor, () -> utils.list(path,false));
    }

    /**
     * Deletes the collection or document with this name.
     * @param name The name of the collection or document
     * @return A empty {@link Tasks}.
     */
    public Task<Void> delete(String name){
        return Tasks.call(executor, () -> {
            utils.deleteFile(path + "/" + name + ".doc");
            return null;
        });
    }

    /**
     * Gets the parent collection of this collection.
     * @return The parent collection.
     */
    public Collection getParents(){
        return previous;
    }

    public Query query(){
        return new Query(this);
    }

    @Override
    public String toString() {
        return "Collection{" +
                "previous=" + previous +
                ", executor=" + executor +
                ", utils=" + utils +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

