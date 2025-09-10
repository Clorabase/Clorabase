package clorabase.sdk.java.database;

import static org.json.JSONObject.NULL;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clorabase.sdk.java.utils.GithubUtils;
import clorabase.sdk.java.Reason;

/**
 * This class represents a document in the database. A document is a single record in a collection.
 * To know more about documents, see {@link <a href="https://www.mongodb.com/docs/compass/current/documents/">...</a>}
 */
public class Document implements Comparable<Document> {
    protected final String path;
    protected final String finalPath;
    protected long lastUpdatedTime = 0;
    protected Map<String, Object> data;
    protected static final int DOCUMENT_MAX_SIZE = 2*1024*1024;
    public final String name;
    protected String orderingField = "timestamp";

    protected Document(String base,String path){
        this.name = path.substring(path.lastIndexOf("/") + 1); // get the name from the path
        this.path = path;
        this.finalPath = base + this.path;
    }


    /**
     * Gets the stored data of the document. If the data is not available, it will fetch from the database
     * and return it. Otherwise, it will return the cached data.
     * <p>
     *
     * If you want to get the latest data immediately, use {@link #fetch()}.
     * @return {@code Map} as the data of documents.
     */
    public Map<String,Object> getData() throws ClorastoreException {
        if (data == null || data.isEmpty())
            return fetch();
        else
            return data;
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
     * Fetches the data of the document from the database. This will always return the latest data.
     * If the data is not available, it will return an empty map.
     * @return {@code Map} as the data of documents.
     * @throws ClorastoreException if there is an error while fetching the data
     */
    public Map<String,Object> fetch() throws ClorastoreException {
        try {
            if (System.currentTimeMillis() - lastUpdatedTime < 5*1000) {
                return data;
            }

            var commit = GithubUtils.getLatestCommit("/");
            var enc = GithubUtils.getImmediateRaw(finalPath, commit);
            var decStr = SecurityProvider.decrypt(enc);
            assert decStr != null;
            data = toMap(new JSONObject(decStr));
            return data;
        } catch (IOException e) {
            if (e instanceof FileNotFoundException){
                return new HashMap<>();
            } else
                throw new ClorastoreException("Error while getting document data",Reason.UNKNOWN);
        }
    }

    /**
     * Sets the data of the document in the database. This will create a new document if it does not exist.
     * This overwrites the existing data.
     * @param data The {@link Map} of fields with its name as key and value as value
     * @throws ClorastoreException if there is an error while setting the data or if the data is invalid
     */
    public void setData(@NotNull Map<String,Object> data) throws ClorastoreException {
        validateData(data);
        this.data = new HashMap<>(data);
        this.data.put("_timestamp",new Date().getTime());
        try {
            var bytes = SecurityProvider.encrypt(new JSONObject(data).toString());
            if (bytes.length > DOCUMENT_MAX_SIZE)
                throw new ClorastoreException("Document size cannot be more then 2MB", Reason.DOCUMENT_SIZE_EXCEEDED);

            System.out.println("Creating document at path: " + finalPath);
            GithubUtils.create(bytes, finalPath);
            lastUpdatedTime = System.currentTimeMillis();
        } catch (Exception e) {
            throw new ClorastoreException("Unknown error : " + e.getMessage(),Reason.UNKNOWN);
        }
    }

    private void updateData(@NotNull Map<String,Object> data) throws ClorastoreException {
        try {
            var bytes = SecurityProvider.encrypt(new JSONObject(data).toString());
            if (bytes.length > DOCUMENT_MAX_SIZE)
                throw new ClorastoreException("Document size cannot be more then 2MB", Reason.DOCUMENT_SIZE_EXCEEDED);

            GithubUtils.update(bytes, finalPath);
            lastUpdatedTime = System.currentTimeMillis();
        } catch (Exception e) {
            if (e instanceof IOException) {
                if (e instanceof FileNotFoundException) {
                    throw new ClorastoreException("Document with name '" + name + "' does not exists in this collection",Reason.NOT_EXISTS);
                } else {
                    throw new ClorastoreException("Failed to update document with name '" + name + "'.", Reason.UNKNOWN);
                }
            } else {
                throw new ClorastoreException("Unknown error", Reason.UNKNOWN);
            }
        }
    }

    /**
     * Updates a field in the document. Create a new document with the given field if not already exists.
     * @param key The key you are updating the value of
     * @param value The value
     * @throws ClorastoreException if there is an error while updating the data
     */
    public void put(@NotNull String key, Object value) throws ClorastoreException {
        if (!key.matches("^[a-zA-Z0-9_]+$"))
            throw new IllegalArgumentException("Field name can only contain alphabets, numbers and underscores");

        if (!isValidType(value))
            throw new ClorastoreException("Invalid datatype : " + value,Reason.INVALID_DATATYPE);

        if (System.currentTimeMillis() - lastUpdatedTime < 5*1000) {
            throw new ClorastoreException("You cannot update the document within 5 seconds of the last update. Use update method instead", Reason.TOO_MANY_REQUESTS);
        }

        if (data == null)
            data = fetch();

        if (value == null)
            data.remove(key);
        else
            data.put(key,value);

        if (data.size() == 1){
            setData(data);
        } else {
            updateData(data);
        }
    }

    /**
     * Updates multiple fields in the document.
     * @param fields The map of fields to be updated, where key is the field name and value is the field value
     * @throws ClorastoreException if document does not exist or if there is an error while updating the data
     */
    public void update(Map<String,Object> fields) throws ClorastoreException {
        if (data == null)
            data = fetch();

        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            Object value = entry.getValue();
            if (value == null)
                data.remove(entry.getKey());
            else
                data.put(entry.getKey(),value);
        }

       updateData(data);
    }

    /**
     * Adds an item to the list. Create a new list or whole document if not already exists.
     * @param list The name of the list
     * @param value The value to be added
     * @throws ClorastoreException if there is an error while adding the item or if the data is invalid
     */
    public void addItem(@NotNull String list,@NotNull Object value) throws ClorastoreException {
        if (data == null)
            data = fetch();

        if (data.containsKey(list) && data.get(list) instanceof List){
            List<Object> list1 = (List<Object>) data.get(list);
            list1.add(value);
        } else {
            List<Object> list1 = new ArrayList<>();
            list1.add(value);
            data.put(list,list1);
        }

        if (data.size() > 1){
            updateData(data);
        } else
            setData(data);
    }

    /**
     * Removes an item from the list. If the list does not exist, it will do nothing.
     * @param key The name of the list
     * @throws ClorastoreException if there is an error while removing the item or if the data is invalid
     */
    public void removeItem(@NonNull String key) throws ClorastoreException {
        if (data == null)
            data = fetch();

        data.remove(key);
        if (data.size() > 1){
            updateData(data);
        } else
            setData(data);
    }

    /**
     * Deletes the document from the database. This will remove the document and all its data.
     * If the document does not exist, it will throw an exception.
     * @throws ClorastoreException if there is an error while deleting the document or if the document does not exist
     */
    public void delete() throws ClorastoreException {
        try {
            GithubUtils.delete(finalPath);
            data = null; // Clear the cached data
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new ClorastoreException("Document with name '" + name + "' does not exist.", Reason.NOT_EXISTS);
            } else {
                throw new ClorastoreException("Failed to delete document with name '" + name + "'.", Reason.UNKNOWN);
            }
        }
    }

    private static void validateData(Map<String, Object> data) throws ClorastoreException {
        if (data == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();

            if (!isValidType(value)) {
                throw new ClorastoreException("Invalid datatype for field '" + entry.getKey() + "': " + value, Reason.INVALID_DATATYPE);
            }
        }
    }

    private static boolean isValidType(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean || value instanceof String) {
            return true;
        }

        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (!isValidType(item)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public Map<String, Object> toMap(JSONObject object) {
        Map<String, Object> results = new HashMap<String, Object>();

        var list = new ArrayList<Map.Entry<String,Object>>();
        object.keys().forEachRemaining(it -> {
            var entry = new AbstractMap.SimpleEntry<String, Object>(it, object.get(it));
            list.add(entry);
        });

        for (Map.Entry<String, Object> entry : list) {
            Object value;
            if (entry.getValue() == null || NULL.equals(entry.getValue())) {
                value = null;
            } else if (entry.getValue() instanceof JSONObject) {
                value = toMap(((JSONObject) entry.getValue()));
            } else if (entry.getValue() instanceof JSONArray) {
                value = toList(((JSONArray) entry.getValue()));
            } else {
                value = entry.getValue();
            }
            results.put(entry.getKey(), value);
        }
        return results;
    }

    public List<Object> toList(JSONArray myArrayList) {
        List<Object> results = new ArrayList<Object>(myArrayList.length());
        for (int i = 0; i < myArrayList.length(); i++) {
            var element = myArrayList.get(i);
            if (element == null || NULL.equals(element)) {
                results.add(null);
            } else if (element instanceof JSONArray) {
                var list = toList(((JSONArray) element));
                results.add(list);
            } else if (element instanceof JSONObject) {
                var map = toMap(((JSONObject) element));
                results.add(map);
            } else {
                results.add(element);
            }
        }
        return results;
    }

    @NonNull
    @Override
    public String toString() {
        return "Document{" +
                "path='" + path + '\'' +
                ", data=" + data +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public int compareTo(Document anotherDoc) {
        Number v1 = (Number) this.data.get(orderingField);
        Number v2 = (Number) anotherDoc.data.get(orderingField);

        assert v1 != null;
        assert v2 != null;

        return (int) (v1.doubleValue() - v2.doubleValue());
    }
}
