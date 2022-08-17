package com.clorabase;

import androidx.core.util.Consumer;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GithubUtils {
    public static GitHub github;
    public static Scanner scanner;
    static {
        try {
            github = GitHub.connectUsingOAuth("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");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getFileAsJSON(String path, Consumer<JSONObject> callable){
        new Thread(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openConnection();
                var in = connection.getInputStream();
                byte[] buffer = new byte[connection.getContentLength()];
                in.read(buffer);
                in.close();
                callable.accept(new JSONObject(new String(buffer)));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static boolean exists(String path){
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

    public static Scanner getFileReader(String path) throws Exception {
        var thread = new Thread(() -> {
            try {
                var in = new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openStream();
                scanner = new Scanner(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        thread.start();
        thread.join(10*1000);
        if (scanner == null)
            throw new Exception("Can't connect to database");
        else
            return scanner;
    }

    public static void getFileContent(String path, Consumer<byte[]> callable){
        new Thread(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/CloremDatabases/main/" + path).openConnection();
                var in = connection.getInputStream();
                byte[] buffer = new byte[connection.getContentLength()];
                in.read(buffer);
                in.close();
                callable.accept(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
