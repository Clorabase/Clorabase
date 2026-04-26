package clorabase.sdk.java.database;


import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
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
    protected final String name;
    protected final String basePath;

    /**
     * Creates a new collection with the given parent, path and name. MUST not used directly
     * as it is used internally by the SDK.
     *
     * @param parent The parent collection of this collection.
     */
    public Collection(Collection parent, String name, String basePath) {
        this.basePath = basePath;
        this.parent = parent;
        this.path = parent == null ? name : parent.path + "/" + name;
        this.name = name;
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

        return new Collection(this,name,basePath);
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

        var collections = path.split("/");
        Collection current = this;
        for (String collection : collections) {
            current = current.collection(collection);
        }
        return current;
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
        return new Document(this,name);
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
        return Document.fromPath(this,finalPath);
    }

    /**
     * Gets reference to all the documents with in this collection.
     * @return A list of documents in this collection.
     */
    public List<Document> getDocuments() throws ClorastoreException {
        try {
            return GithubUtils.listFiles(basePath + path).stream()
                    .filter(githubFile -> githubFile.isFile() && !githubFile.getName().equals("index"))
                    .map(s -> new Document(this,s.getName().replace(".doc","")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            ClorastoreException.handle(e, "Collection does not exist.", "Failed to get documents in collection.");
            return null; // Unreachable
        }
    }

    /**
     * Gets names of all the collections in this collection.
     * @return A list of collections in this collection.
     */
    public List<String> getCollections() throws ClorastoreException {
        try {
            return GithubUtils.listFiles(basePath + path)
                    .stream()
                    .filter(it -> !it.isFile())
                    .map(GithubFile::getName)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            ClorastoreException.handle(e, "Collection does not exist.", "Failed to get collections in collection.");
            return null; // Unreachable
        }
    }

    /**
     * Deletes the document with this name. If the document does not exist, it will throw an exception.
     * You cannot delete a collection using SDK, you must delete it manually from the repository.
     * @param name The name of the collection or document
     */
    public void delete(String name) throws ClorastoreException {
        try {
            GithubUtils.delete(basePath + path + "/" + name + ".doc");
        } catch (IOException e) {
            ClorastoreException.handle(e, "Document with name '" + name + "' does not exist.", "Failed to delete document with name '" + name + "'.");
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
