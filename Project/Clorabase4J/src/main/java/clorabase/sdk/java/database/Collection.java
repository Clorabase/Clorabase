package clorabase.sdk.java.database;


import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import clorabase.sdk.java.utils.GithubFile;
import clorabase.sdk.java.utils.GithubUtils;
import clorabase.sdk.java.Reason;

/**
 * This class represents a collection in the database. A collection is a group of documents or other collections.
 * To know more about collections, see {@link <a href="https://www.mongodb.com/docs/compass/current/collections/">...</a>}
 */
public class Collection {
    protected final Collection parent;
    protected final ExecutorService executor = Executors.newCachedThreadPool();
    protected final String path;
    protected final String base;
    protected final String name;

    /**
     * Creates a new collection with the given parent, path and name. MUST not used directly
     * as it is used internally by the SDK.
     *
     * @param parent The parent collection of this collection.
     * @param path   The path of the collection in the database.
     * @param base  The base path of the database (usually "projectName/db/").
     */
    public Collection(Collection parent, String base, String path) {
        this.parent = parent;
        this.base = base;
        this.path = path;
        this.name = path.substring(path.lastIndexOf("/") + 1); // get the name from the path
    }

    public String getPath() {
        if (path.startsWith("/"))
            return path.substring(1);

        return path;
    }

    public String getName() {
        return name;
    }

    /**
     * Goes into the specified collection.
    @param name The name of the collection.
     * @return The new collection.
     */
    public Collection collection(String name) {
        if (name.contains("/") || name.contains(" ") || name.contains("\\") || name.contains(".") || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid collection name, can only include alphabet and numbers");
        }

        return new Collection(this,base,path + "/"+ name);
    }

    /**
     * Goes into the specified collection by its path.
     * @param path The path of the collection relative to the current collection.
     * @return The new collection.
     *
     * NOTE - The parent of the collection will be this collection.
     */
    public Collection collectionAt(@NotNull String path) {
        if (!path.contains("/"))
            return collection(path);

        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        return new Collection(this, base, this.path + path);
    }

    /**
     * Gets the document with the given name. If the document does not exist, it will be created upon any operation.
     * @param name The name of the document
     *
     * @return The document.
     */
    public Document document(@NotNull String name) {
        name = name.toLowerCase().trim();
        if (name.contains("/") || name.contains(" ") || name.contains(".") || name.isEmpty()) {
            throw new IllegalArgumentException(name + " : " + "Invalid document name, can only include alphabet and numbers");
        }
        return new Document(base,path + "/" + name + ".doc");
    }

    /**
     * Gets the document with the given path. If the document does not exist, it will be created upon any operation.
     * @param path The path of the document. MUST be relative to the current collection and have .doc extension.
     *
     * @return The document.
     */
    public Document documentAt(@NotNull String path) {
        if (!path.endsWith(".doc"))
            throw new IllegalArgumentException("Document path must end with .doc extension. Given: " + path);

        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        if (!path.contains("/"))
            return document(path.replace(".doc", ""));

        var finalPath = this.path + "/" + path;
        return new Document(base,finalPath);
    }

    /**
     * Gets reference to all the documents with in this collection.
     * @return A list of documents in this collection.
     */
    public List<Document> getDocuments() throws ClorastoreException {
        try {
            return GithubUtils.listFiles(base + path).stream()
                    .filter(GithubFile::isFile)
                    .map(s -> new Document(base,path  + "/" + s.getName()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            if (e instanceof FileNotFoundException)
                throw new ClorastoreException("Collection does not exist.", Reason.NOT_EXISTS);
            else
                throw new ClorastoreException("Failed to get documents in collection.", Reason.UNKNOWN);
        }
    }

    /**
     * Gets names of all the collections in this collection.
     * @return A list of collections in this collection.
     */
    public List<String> getCollections() throws ClorastoreException {
        try {
            return GithubUtils.listFiles(base + path)
                    .stream()
                    .filter(it -> !it.isFile())
                    .map(GithubFile::getName)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            if (e instanceof FileNotFoundException)
                throw new ClorastoreException("Collection does not exist.", Reason.NOT_EXISTS);
            else
                throw new ClorastoreException("Failed to get collections in collection.", Reason.UNKNOWN);
        }
    }

    /**
     * Deletes the document with this name. If the document does not exist, it will throw an exception.
     * You cannot delete a collection using SDK, you must delete it manually from the repository.
     * @param name The name of the collection or document
     */
    public void delete(String name) throws ClorastoreException {
        try {
            GithubUtils.delete(base + path + "/" + name + ".doc");
        } catch (IOException e) {
            if (e instanceof FileNotFoundException)
                throw new ClorastoreException("Document with name '" + name + "' does not exist.",e);
            else
                throw new ClorastoreException("Failed to delete document with name '" + name + "'.", Reason.UNKNOWN);
        }
    }

    /**
     * Gets the parent collection of this collection.
     * @return The parent collection.
     */
    public Collection getParent(){
        return parent;
    }

    /**
     * Creates a new query for this collection.
     * @return A new query object for this collection.
     */
    public Query query(){
        return new Query(this);
    }

    @Override
    public String toString() {
        return "Collection{" +
                "parent=" + parent +
                ", executor=" + executor +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

