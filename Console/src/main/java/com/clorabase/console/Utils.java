package com.clorabase.console;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.util.Consumer;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Utils {
    private static final Executor executor = Executors.newCachedThreadPool();
    public static GHRepository repo;

    public static void init(Context context){
        new Thread(() -> {
            try {
                repo = GitHub.connectUsingOAuth("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx").getRepository("Clorabase-databases/OpenDatabases");
            } catch (IOException e) {
                e.printStackTrace();
                runOnUIThread(() -> {
                    if (e instanceof HttpException){
                        if (e.getCause() instanceof UnknownHostException){
                            Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
                            ((MainActivity) context).finish();
                        } else
                            Toast.makeText(context, "Failed to connect to GitHub. Please try again later", Toast.LENGTH_SHORT).show();
                    } else
                        Toast.makeText(context, "Failed to connect to GitHub: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    public static void create(byte[] contents, String path, AsyncCallback runnable) {
        executor.execute(() -> {
            try {
                repo.createContent(contents, "Created through console", path);
                runOnUIThread(runnable::onComplete);
            } catch (IOException e) {
                runOnUIThread(() -> runnable.onError(e));
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
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
                connection.addRequestProperty("Accept-Encoding", "identity");
                var in = connection.getInputStream();
                byte[] buffer = new byte[connection.getContentLength()];
                in.read(buffer);
                in.close();
                runOnUIThread(() -> callback.accept(buffer));
            } catch (IOException e) {
                runOnUIThread(() -> callback.accept(null));
            }
        });
    }

    public static void download(File directory, String filename, String path, AsyncCallback callback) {
        executor.execute(() -> {
            try {
                var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
                connection.addRequestProperty("Accept-Encoding", "identity");
                var in = connection.getInputStream();
                var os = new FileOutputStream(new File(directory, filename));
                byte[] buffer = new byte[1024];
                int read;
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
        var in = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openStream();
        return new Scanner(in);
    }

    public static boolean exists(String path) {
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

    public static void reportBug(Context context,Exception e){
        var deviceInfo = String.format("""
                Device : %1s
                Android : %2s
                App version : %3s
                Date : %4s
                ---------[ STACK TRACE ]------------
                                        
                """,Build.DEVICE,Build.VERSION.SDK,BuildConfig.VERSION_NAME,new Date()) + e.getLocalizedMessage();
        Toast.makeText(context, "App crashed! Please send bug report to the developer", Toast.LENGTH_LONG).show();
        var intent = new Intent(Intent.ACTION_SENDTO);
        intent.setDataAndType( Uri.parse("mailto:bugreports@rahilxcode.tk"),"message/rfc822");
        intent.putExtra(Intent.EXTRA_TEXT,deviceInfo);
        intent.putExtra(Intent.EXTRA_SUBJECT,"An unexpected error occurred while using the app 'clorabase console'");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private static void runOnUIThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    public interface AsyncCallback {
        void onComplete();

        void onError(Exception e);
    }
}
