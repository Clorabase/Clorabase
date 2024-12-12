package sdk.clorabase.clorastore;

import androidx.annotation.NonNull;

import com.clorabase.clorastore.Reasons;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Document implements Comparable<Document> {
    public final String path;
    protected long EXPIRY;
    protected Map<String, Object> data;
    protected static final int DOCUMENT_MAX_SIZE = 5*1024*1024;
    protected final Executor executor = Executors.newCachedThreadPool();
    protected final DBUtils utils;
    public final String name;
    protected String orderingField = "timestamp";

    protected Document(String path, DBUtils utils, String name){
        this.utils = utils;
        this.name = name;
        this.path = path + "/" + name + ".doc";
    }

    public Task<Map<String,Object>> get(String path, DBUtils utils, String name){
        return Tasks.call(executor,() -> Tasks.await(getData(),5,TimeUnit.SECONDS));
    }

    /**
     * Gets the stored data of the document from the database if not already exists in cache.
     * @return A {@link Tasks} of the {@code Map} as the data of documents.
     */
    public Task<Map<String,Object>> getData(){
        return Tasks.call(executor, () -> {
            if (EXPIRY < new Date().getTime()) {
                data = utils.getContent(path);
                EXPIRY = new Date().getTime() + TimeUnit.MINUTES.toMillis(5);
            }
            return data;
        });
    }

    /**
     * Sets the data of the document in the database.
     * @param data The {@link Map} of fields with its name as key and value as value
     * @return A {@link Tasks} with the id of the document as its successful result
     */
    public Task<Void> setData(@NotNull Map<String,Object> data){
        validateData(data);
        this.data = data;
        return Tasks.call(executor,() -> {
            data.put("timestamp",new Date().getTime());
            utils.createFile(data,path);
            return null;
        });
    }

    /**
     * Updates a field in the document. Create a new document with the given field if not already exists.
     * @param key The key you are updating the value of
     * @param value The value
     * @return Task of type void.
     */
    public Task<Void> put(@NotNull String key, Object value){
        if (!key.matches("^[a-zA-Z0-9_]+$"))
            throw new IllegalArgumentException("Field name can only contain alphabets, numbers and underscores");

        if (!isValidType(value))
            throw new ClorastoreException("Invalid datatype : " + value,Reasons.INVALID_DATATYPE);

        return Tasks.call(executor,() -> {
            if (data == null)
                Tasks.await(getData(),5,TimeUnit.SECONDS);

            if (value == null)
                data.remove(key);
            else
                data.put(key,value);

            if (data.size() == 1){
                Tasks.await(setData(data));
            } else
                utils.updateFile(data,path);

            return null;
        });
    }

    public Task<Void> update(Map<String,Object> fields){
        return Tasks.call(executor,() -> {
            if (data == null)
                Tasks.await(getData(),5,TimeUnit.SECONDS);

            if (data.isEmpty())
                throw new ClorastoreException("Document does not exist or is empty!",Reasons.NO_DOC_EXIST);

            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                Object value = entry.getValue();
                if (value == null)
                    data.remove(entry.getKey());
                else
                    data.put(entry.getKey(),value);
            }

            utils.updateFile(data,path);
            return null;
        });
    }

    /**
     * Adds an item to the list. Create a new list if not already exists.
     * @param list The name of the list
     * @param value The value to be added
     * @return Task of type void.
     */
    public Task<Void> addItem(@NotNull String list,@NotNull Object value){
        return Tasks.call(executor,() -> {
            if (data == null)
                Tasks.await(getData(),5,TimeUnit.SECONDS);

            if (data.containsKey(list) && data.get(list) instanceof List){
                List<Object> list1 = (List<Object>) data.get(list);
                list1.add(value);
            } else {
                List<Object> list1 = new ArrayList<>();
                list1.add(value);
                data.put(list,list1);
            }

            if (data.size() > 1){
                utils.updateFile(data,path);
            } else
                Tasks.await(setData(data));

            return null;
        });
    }

    public Task<String> removeItem(@NonNull String key){
        return Tasks.call(executor,() -> {
            if (data == null)
                Tasks.await(getData(),5,TimeUnit.SECONDS);

            data.remove(key);
            if (data.size() > 1){
                utils.updateFile(data,path);
            } else
                Tasks.await(setData(data));

            return null;
        });
    }

    public boolean isExpired(){
        return EXPIRY < new Date().getTime();
    }

    private static void validateData(Map<String, Object> data) throws ClorastoreException {
        if (data == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();

            if (isValidType(value)) {
                continue;
            } else {
                throw new ClorastoreException("Invalid data type: " + value.getClass().getName(), Reasons.INVALID_DATATYPE);
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

    @NonNull
    @Override
    public String toString() {
        return "Document{" +
                "path='" + path + '\'' +
                ", EXPIRY=" + EXPIRY +
                ", data=" + getData() +
                ", executor=" + executor +
                ", utils=" + utils +
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
