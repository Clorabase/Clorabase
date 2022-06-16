package com.clorabase.db.clorem;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;

import com.clorabase.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import db.clorabase.clorem.Clorem;
import db.clorabase.clorem.Node;

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
    protected Node node;
    protected static WeakReference<Context> context;
    protected static Node root;
    protected static CloremDatabase INSTANCE;
    protected static ExecutorService executor;
    protected static CloremClient database;

    private CloremDatabase(Node name) {
        this.node = name;
    }

    /**
     * Initialize the database synchronously. This method blocks the thread for at most 5 seconds until the database is initialized.
     * If the database is not initialized successfully within 5 seconds, further calls to the class method will throw an exception. Only use if the
     * internet is not slow and database size is not too large.
     *
     * @param context Activity context
     * @param DB_ID   Database ID that you got from console
     * @param token   Access token that you got from console
     * @return ClorabaseDatabase instance
     */
    public static synchronized CloremDatabase getInstance(@NonNull Context context, @NonNull String DB_ID, @NonNull String token) {
        if (INSTANCE == null) {
            database = new CloremClient(Constants.DATABASE_BASE_URL, init(context, DB_ID, token));
            try {
                boolean connected = database.connectBlocking(15, TimeUnit.SECONDS);
                if (!connected)
                    System.err.println("Connection to the database failed. Please check your internet connection.");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return INSTANCE;
    }

    /**
     * Initialize the database asynchronously and returns the instance immediately.
     * If the database failed to initialize or is not initialized yet, further calls
     * to the class method will throw an exception. Only use if internet is slow or database size is too large.
     *
     * @param context Activity context
     * @param DB_ID   Database ID that you got from console
     * @param token   Access token that you got from console
     * @return ClorabaseDatabase instance
     */
    public static synchronized CloremDatabase getInstanceAsync(@NonNull Context context, @NonNull String DB_ID, @NonNull String token) {
        if (INSTANCE == null) {
            database = new CloremClient(Constants.DATABASE_BASE_URL, init(context, DB_ID, token));
            database.connect();
        }
        return INSTANCE;
    }

    private static Map<String, String> init(Context context, String DB_ID, String token) {
        root = Clorem.getInstance(context.getFilesDir(), "clorabase").getDatabase();
        INSTANCE = new CloremDatabase(root);
        executor = Executors.newCachedThreadPool();
        CloremDatabase.context = new WeakReference<>(context);
        Map<String, String> headers = new HashMap<>();
        headers.put("Client-ID", Constants.CLIENT_ID);
        headers.put("Client-Secret", Constants.CLIENT_SECRET);
        headers.put("DB-ID", DB_ID);
        headers.put("Access-Token", token);

        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                getInstanceAsync(context, DB_ID, token);
            }

            @Override
            public void onLost(Network network) {
                System.err.print("Connection to the database lost. We will reconnect it when the network is available.");
            }
        };

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } else {
            NetworkRequest request = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        }
        return headers;
    }

    /**
     * Goes to the specified node. If the node does not exist, it will be created.
     *
     * @param name The name or relatives path of the node (from root)
     * @return Same instance with the new node
     */
    public CloremDatabase node(String name) {
        return new CloremDatabase(node.node(name));
    }

    /**
     * Gets the data of the current node in the form of a MAP.
     *
     * @return Task<Map < String, Object>> that contains the data of the current node
     */
    public Task<Map<String, Object>> getData() {
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                JSONObject json = new JSONObject();
                json.put("node", node.getPath());
                json.put("method", "getData");
                JSONObject jsonObject = new JSONObject(database.sendMessage(json));
                return asMap(jsonObject);
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
    public Task<String> setData(Map<String, Object> data) {
        node.put(data);
        node.commit();
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                JSONObject json = new JSONObject();
                json.put("method", "putData");
                json.put("node", node.getPath());
                json.put("data", new JSONObject(data));
                return database.sendMessage(json);
            } else
                throw new Exception("No internet connection");
        });
    }

    /**
     * Delete a underlying nested node within the current node.
     *
     * @param node Name or relative path (from current node) of the node to be deleted
     * @return Task<String> that contains the response of the operation
     */
    public Task<String> delete(@NonNull String node) {
        this.node.delete(node);
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                JSONObject json = new JSONObject();
                json.put("method", "delete");
                json.put("node", node);
                return database.sendMessage(json);
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
                JSONObject json = new JSONObject();
                json.put("method", "query");
                json.put("node", node.getPath());
                json.put("query", query);
                JSONArray jsonArray = new JSONArray(database.sendMessage(json));
                for (int i = 0; i < jsonArray.length(); i++) {
                    result.add(asMap((jsonArray.getJSONObject(i))));
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

    /**
     * Insert a new item to the list. If the value type is not same as the items of the list, operation will fail.
     *
     * @param key   Key of list (within the current node)
     * @param value The item to be inserted
     * @return Task<String> that contains the response of the operation
     */
    public Task<String> addItem(@NonNull String key, @NonNull Object value) {
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                JSONObject json = new JSONObject();
                json.put("method", "addItem");
                json.put("node", node.getPath());
                json.put("key", key);
                json.put("value", value);
                return database.sendMessage(json);
            } else
                throw new Exception("No internet connection");
        });
    }

    /**
     * Removes an item from the list.
     *
     * @param key   Key of list (which is in the current node)
     * @param index Index of the item to be removed
     * @return Task<String> that contains the response of the operation
     */
    public Task<String> removeItem(@NonNull String key, int index) {
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                JSONObject json = new JSONObject();
                json.put("method", "removeItem");
                json.put("node", node.getPath());
                json.put("key", key);
                json.put("index", index);
                return database.sendMessage(json);
            } else
                throw new Exception("No internet connection");
        });
    }

    /**
     * Forces the server to push the database to the drive. Should not be used if it is not necessary.
     *
     * @return Task<String> that contains the response of the operation
     */
    public Task<String> forceCommit() {
        return Tasks.call(executor, () -> {
            if (hasInternet()) {
                return database.sendMessage(new JSONObject("{\"method\":\"commit\"}"));
            } else
                throw new Exception("No internet connection");
        });
    }

    private boolean hasInternet() {
        NetworkInfo info = context.get().getSystemService(ConnectivityManager.class).getActiveNetworkInfo();
        return info != null && info.isConnected();
    }


    private Map<String, Object> asMap(JSONObject jsonObject) {
        Iterator<String> iterator = jsonObject.keys();
        Map<String, Object> map = new HashMap<>();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object opt = jsonObject.opt(key);
            map.put(key, opt instanceof JSONObject ? asMap((JSONObject) opt) : opt);
        }
        return map;
    }

    /**
     * Gets the {@link Node} reference from its data.
     *
     * @param data Data of the node
     * @return The {@link Node} reference
     */
    public Node getReference(Map<String, Object> data) {
        return root.node((String) data.get("_address"));
    }
}
