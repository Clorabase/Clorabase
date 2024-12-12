package sdk.clorabase;

import androidx.core.util.Consumer;

import sdk.clorabase.storage.ClorabaseStorageCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GithubUtils {
    public static String BASE_URL = "https://raw.githubusercontent.com/%s/" + Constants.REPO_NAME + "/main/";

    public static void getFileAsJSON(String path, Consumer<JSONObject> callable){
        new Thread(() -> {
            try {
                var connection = new URL(BASE_URL + path).openConnection();
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
                    var connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
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

    public static void download(File directory, String filename, String path, ClorabaseStorageCallback callback) {
        new Thread(() -> {
            try {
                var connection = new URL(BASE_URL + path).openConnection();
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
                os.close();
                in.close();
                callback.onComplete();
            } catch (IOException e) {
                callback.onFailed(e);
            }
        }).start();
    }

    public static void download(File directory,String filename,String path) throws IOException {
        var connection = new URL(BASE_URL + path).openConnection();
        connection.addRequestProperty("Accept-Encoding", "identity");
        var in = connection.getInputStream();
        var os = new FileOutputStream(new File(directory, filename));
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            os.write(buffer, 0, read);
        }
        in.close();
        os.close();
    }
}
