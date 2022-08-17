package com.clorabase.console;

import android.os.Handler;
import android.os.Looper;

import androidx.core.util.Consumer;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Utils {
    private static final Executor executor = Executors.newCachedThreadPool();
    protected static volatile GHRepository repo;

    public static void init(){
        new Thread(() -> {
            try {
                repo = GitHub.connectUsingOAuth("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").getRepository("Clorabase-databases/CloremDatabases");
                System.out.println("Repo initialized");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void create(byte[] contents, String path, AsyncCallback runnable) {
        executor.execute(() -> {
            try {
                repo.createContent(contents, "Created through console", path);
                runOnUIThread(runnable::onComplete);
            } catch (IOException e) {
                runOnUIThread(() -> {
                    if (e instanceof HttpException)
                        runnable.onComplete();
                    else
                        runOnUIThread(() -> runnable.onError(e));
                });
            }
        });
    }

    public static void update(byte[] contents, String path, AsyncCallback runnable) {
        executor.execute(() -> {
            try {
                repo.getFileContent(path).update(contents, "Updated through console");
                runOnUIThread(runnable::onComplete);
            } catch (IOException e) {
                runOnUIThread(() -> runnable.onError(e));
            }
        });
    }

    public static void delete(String path, AsyncCallback runnable) {
        executor.execute(() -> {
            try {
                repo.getFileContent(path).delete("Deleted through console");
                runOnUIThread(runnable::onComplete);
            } catch (IOException e) {
                runOnUIThread(() -> runnable.onError(e));
            }
        });
    }

    public static void read(String path, Consumer<byte[]> callback) {
        executor.execute(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openConnection();
                connection.addRequestProperty("Accept-Encoding", "identity");
                var in = connection.getInputStream();
                byte[] buffer = new byte[connection.getContentLength()];
                in.read(buffer);
                in.close();
                System.out.println(new String(buffer));
                runOnUIThread(() -> callback.accept(buffer));
            } catch (IOException e) {
                runOnUIThread(() -> callback.accept(null));
            }
        });
    }

    public static void download(File directory, String filename, String path, AsyncCallback callback) {
        executor.execute(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openConnection();
                var in = connection.getInputStream();
                var os = new FileOutputStream(new File(directory, filename));
                byte[] buffer = new byte[1024];
                int read = 0;
                while ((read = in.read(buffer)) > 0) {
                    os.write(buffer, 0, read);
                }
                in.close();
                os.close();
                runOnUIThread(callback::onComplete);
            } catch (IOException e) {
                runOnUIThread(() -> callback.onError(e));
            }
        });
    }

    public static Scanner getFileReader(String path) throws Exception {
        var in = new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openStream();
        return new Scanner(in);
    }

    public static boolean exists(String path) {
        try {
            return Executors.newCachedThreadPool().submit(() -> {
                try {
                    var connection = (HttpURLConnection) new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openConnection();
                    return connection.getResponseCode() != 404;
                } catch (IOException e) {
                    e.printStackTrace();
                    return true;
                }
            }).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getFileSize(String id) {
        ANResponse<JSONObject> response = AndroidNetworking.get("https://api.anonfiles.com/v2/file/{id}/info")
                .addPathParameter("id", id)
                .build()
                .executeForJSONObject();

        if (response.isSuccess()) {
            try {
                return response.getResult().getJSONObject("data").getJSONObject("file").getJSONObject("metadata").getJSONObject("size").getString("readable");
            } catch (JSONException e) {
                return "N/A";
            }
        } else
            return "N/A";
    }

    private static void runOnUIThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public interface AsyncCallback {
        void onComplete();

        void onError(Exception e);
    }
}
