package com.clorabase;

import android.util.Base64;

import androidx.core.util.Consumer;

import com.clorabase.storage.ClorabaseStorage;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GithubUtils {
    private static volatile GitHub github;
    public static final String token = new String(Base64.decode("Z2hwX2dCM0xNdno1dWFVSTQxWFBSdTUwVU42YTMzSG9CWTBDVVBWeA==", Base64.DEFAULT));

    public static void getFileAsJSON(String path, Consumer<JSONObject> callable){
        new Thread(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
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
                    var connection = (HttpURLConnection) new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
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

    public static void download(File directory, String filename, String path, ClorabaseStorage.ClorabaseStorageCallback callback) {
        new Thread(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
                connection.addRequestProperty("Accept-Encoding", "identity");
                var in = connection.getInputStream();
                var os = new FileOutputStream(new File(directory, filename));
                byte[] buffer = new byte[1024];
                int total = connection.getContentLength();
                int read;
                int current = 0;
                while ((read = in.read(buffer)) != -1) {
                    current += read;
                    os.write(buffer, 0, read);
                    callback.onProgress((current*100)/total);
                }
                callback.onComplete();
            } catch (IOException e) {
                callback.onFailed(e);
            }
        }).start();
    }

    public static void download(File directory,String filename,String path) throws IOException {
        var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
        connection.addRequestProperty("Accept-Encoding", "identity");
        var in = connection.getInputStream();
        var os = new FileOutputStream(new File(directory, filename));
        byte[] buffer = new byte[1024];
        int total = connection.getContentLength();
        int read;
        int current = 0;
        while ((read = in.read(buffer)) != -1) {
            current += read;
            os.write(buffer, 0, read);
        }
    }

    public static String getProjectById(String projectId) {
        return new String(Base64.decode(projectId,Base64.DEFAULT));
    }

    public static GHRepository getRepository(){
        try {
            if (github == null) {
                github = GitHub.connectUsingOAuth(token);
            }
            return github.getRepository("Clorabase-databases/OpenDatabases");
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }
}
