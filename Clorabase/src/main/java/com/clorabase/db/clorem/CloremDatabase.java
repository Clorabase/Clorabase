package com.clorabase.db.clorem;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import db.clorabase.clorem.Clorem;
import db.clorabase.clorem.Node;
import okhttp3.OkHttpClient;

/**
 * ClorabaseDatabase is the only main class for interacting with the Clorabase database. Clorabase serverless database uses
 * CloremDB internally to store the data, So you should know how to work with it. You must configure the database from console before using it.
 * This class is thread-safe and all the methods runs asynchronously. It supports offline capabilities too.
 *
 * @author Rahil khan
 * @version 2.0
 * @see <a href="https://docs.clorabase.tk/#/documents/database?id=clorabase-database">Documentation</a>
 * @see <a href="https://github.com/ErrorxCode/CloremDB">CloremDB</a>
 * @since 1.0
 */
public class CloremDatabase {
    protected String BASE_URL;
    protected String db_name;
    protected Context context;
    protected Node node;
    protected static CloremDatabase INSTANCE;
    protected ExecutorService executor;

    private CloremDatabase(Node node, Context context) {
        this.node = node;
        this.context = context.getApplicationContext();
    }

    /**
     * Initialize the database. Does not does anything except initiating the class.
     *
     * @param context any context, however, application's context will be used.
     * @return ClorabaseDatabase instance with root node.
     */
    public static synchronized CloremDatabase getInstance(@NonNull Context context,@NonNull String database_name) {
        if (INSTANCE == null) {
            var root = Clorem.getInstance(context.getFilesDir(), "clorabase").getDatabase();
            INSTANCE = new CloremDatabase(root,context);
            INSTANCE.db_name = database_name;
            INSTANCE.BASE_URL = "https://clorabase.herokuapp.com/clorem/" + database_name;

            var client = new OkHttpClient().newBuilder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(10,TimeUnit.SECONDS)
                    .build();
            AndroidNetworking.initialize(context,client);
            boolean success = AndroidNetworking.get(INSTANCE.BASE_URL + "/init").build().executeForOkHttpResponse().isSuccess();
            if (!success)
                throw new RuntimeException("Failed to initialize database");
        }
        return INSTANCE;
    }


    /**
     * Goes to the specified node. If the node does not exist, it will be created.
     *
     * @param name The name or relatives path of the node (from root)
     * @return Same instance with the new node
     */
    public CloremDatabase node(String name) {
        return new CloremDatabase(node.node(name), context);
    }

    /**
     * Gets the data of the current node in the form of a MAP.
     *
     * @return Task<Map < String, Object>> that contains the data of the current node
     */
    public Task<Map<String, Object>> getData() {
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                ANResponse<JSONObject> response = AndroidNetworking.get(BASE_URL)
                        .addQueryParameter("node", this.node.getPath())
                        .build()
                        .executeForJSONObject();
                if (response.isSuccess())
                    return asMap(response.getResult());
                else
                    throw new Exception(response.getOkHttpResponse().message());
            } else
                return this.node.getData();
        });
    }

    /**
     * Inserts the data in the current node. If the key already exists, its value will be updated.
     *
     * @param data Map<String, Object> that contains the data to be inserted
     * @return Task<String> that contains the response  of the operation
     */
    public Task<Void> setData(Map<String, Object> data) {
        node.put(data);
        node.commit();
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                var response = AndroidNetworking.patch(BASE_URL)
                        .addQueryParameter("node",node.getPath())
                        .addJSONObjectBody(new JSONObject(data))
                        .build()
                        .executeForOkHttpResponse();
                if (response.isSuccess())
                    return null;
                else
                    throw new Exception("Unexpected error occurred, this should not occur generally. If it happens again and again, create a issue on github");
            } else
                throw new Exception("No internet connection");
        });
    }

    /**
     * Delete a underlying nested node within the current node.
     *
     * @param node Name or relative path (from current node) of the node to be deleted
     * @return Task<Void> holding result
     */
    public Task<Void> delete(@NonNull String node) {
        this.node.delete(node);
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                var response = AndroidNetworking.delete(BASE_URL)
                        .addQueryParameter("node",node)
                        .build()
                        .executeForOkHttpResponse();
                if (!response.isSuccess())
                    throw new Exception("Cannot delete the node. Make sure it exists");
                else
                    return null;
            } else
                throw new Exception("No internet connection");
        });
    }

    /**
     * Sorts the node by a database query & returns then returns the data of the sorted node.
     *
     * @param query Query to be executed. See <a href="https://github.com/ErrorxCode/CloremDB/wiki/Guide#quering-database">Queries</a>
     * @return Task<Map < String, Object>> that contains the data of the resulted nodes
     */
    public Task<List<Map<String, Object>>> query(String query) {
        List<Map<String, Object>> result = new ArrayList<>();
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                ANResponse<JSONArray> response = (ANResponse<JSONArray>) AndroidNetworking.get(BASE_URL)
                        .addQueryParameter("node",node.getPath())
                        .addQueryParameter("query",query)
                        .build()
                        .executeForJSONArray();
                if (response.isSuccess()){
                    var resultList = new ArrayList<Map<String,Object>>();
                    var jsonArray = response.getResult();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        resultList.add(asMap(jsonArray.getJSONObject(i)));
                    }
                    return resultList;
                }
                return result;
            } else {
                List<Node> nodes = node.query().fromQuery(query);
                for (Node node : nodes) {
                    result.add(node.getData());
                }
                return null;
            }
        });
    }

//    /**
//     * Insert a new item to the list. If the value type is not same as the items of the list, operation will fail.
//     *
//     * @param key   Key of list (within the current node)
//     * @param value The item to be inserted
//     * @return Task<String> that contains the response of the operation
//     */
//    public Task<String> addItem(@NonNull String key, @NonNull Object value) {
//        return Tasks.call(executor, () -> {
//            if (hasInternet()) {
//                JSONObject json = new JSONObject();
//                json.put("method", "addItem");
//                json.put("node", node.getPath());
//                json.put("key", key);
//                json.put("value", value);
//                return database.sendMessage(json);
//            } else
//                throw new Exception("No internet connection");
//        });
//    }
//
//    /**
//     * Removes an item from the list.
//     *
//     * @param key   Key of list (which is in the current node)
//     * @param index Index of the item to be removed
//     * @return Task<String> that contains the response of the operation
//     */
//    public Task<String> removeItem(@NonNull String key, int index) {
//        return Tasks.call(executor, () -> {
//            if (hasInternet()) {
//                JSONObject json = new JSONObject();
//                json.put("method", "removeItem");
//                json.put("node", node.getPath());
//                json.put("key", key);
//                json.put("index", index);
//                return database.sendMessage(json);
//            } else
//                throw new Exception("No internet connection");
//        });
//    }

    /**
     * Forces the server to push the changes to the database. Should not be used if it is not necessary.
     *
     * @return Task<String> that contains the response of the operation
     */
    public Task<Void> forceCommit() {
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                var response = AndroidNetworking.get(BASE_URL + "/commit").build().executeForOkHttpResponse();
                if (response.isSuccess())
                    return null;
                else
                    throw new Exception("Force commit failed ! The server will automatically try to commit the changes after some times");
            } else
                throw new Exception("No internet connection");
        });
    }

    private boolean hasInternet() {
        NetworkInfo info = context.getSystemService(ConnectivityManager.class).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }


    private Map<String, Object> asMap(JSONObject jsonObject) {
        Iterator<String> iterator = jsonObject.keys();
        Map<String, Object> map = new HashMap<>();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object opt = jsonObject.opt(key);
            map.put(key, opt instanceof JSONObject ? asMap((JSONObject) opt) : opt instanceof JSONArray array ? asList(array) : opt);
        }
        return map;
    }

    private List<Object> asList(JSONArray array) {
        var list = new ArrayList<>();
        try {
            for (int i = 0; i < array.length(); i++) {
                list.add(array.get(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Gets the {@link Node} reference from its data.
     *
     * @param data Data of the node
     * @return The {@link Node} reference
     */
    public Node getReference(Map<String, Object> data) {
        return node.node((String) data.get("_address"));
    }
}
