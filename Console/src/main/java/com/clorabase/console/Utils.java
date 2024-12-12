package com.clorabase.console;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.util.Consumer;

import com.clorabase.console.adapters.GithubFilesAdapter;

import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    private static final Executor executor = Executors.newCachedThreadPool();
    public static GHRepository repo;
    public static GitHub gitHub;
    public static Handler handler = new Handler(Looper.getMainLooper());
    public static String username;

    public static void init(Context context,String username,String token) {
        Constants.BASE_RAW_URL = String.format(Constants.BASE_RAW_URL, username);
        Constants.GIT_TREE_URL = String.format(Constants.GIT_TREE_URL, username);
        Constants.TREE_PRIVATE_URL = String.format(Constants.TREE_PRIVATE_URL, username);

        new Thread(() -> {
            try {
                gitHub = GitHub.connectUsingOAuth(token);
                Utils.username = username;
                repo = gitHub.getRepository(username + "/" + Constants.REPO_NAME);
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Unable to connect")
                            .setMessage("Could not connect to the backend server. Please check your internet connection and try again\n" +
                                    "If this problem persist, check your github access token on github if its deleted or expired. Raise an issue on " +
                                    "github if you still can't resolve the problem")
                            .setPositiveButton("Retry", (dialog, which) -> ((MainActivity) context).recreate())
                            .setNegativeButton("Exit",(d,w) -> ((MainActivity) context).finish())
                            .show();

                    if (e instanceof HttpException) {
                        if (e.getCause() instanceof UnknownHostException) {
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
                var sha = repo.createContent(contents, "Created through console", path).getCommit().getSha();
                handler.post(() -> runnable.onComplete(sha));
            } catch (IOException e) {
                handler.post(() -> runnable.onError(e));
            }
        });
    }

    public static void update(byte[] contents, String path, AsyncCallback runnable) {
        executor.execute(() -> {
            try {
                repo.getFileContent(path).update(contents, "Updated through console");
                handler.post(() -> runnable.onComplete(null));
            } catch (IOException e) {
                handler.post(() -> runnable.onError(e));
            }
        });
    }

    public static void delete(String path, AsyncCallback runnable) {
        executor.execute(() -> {
            try {
                repo.getFileContent(path).delete("Deleted through console");
                handler.post(() -> runnable.onComplete(null));
            } catch (IOException e) {
                handler.post(() -> runnable.onError(e));
            }
        });
    }

    public static void read(String path, Consumer<byte[]> callback) {
        executor.execute(() -> {
            try {
                var connection = new URL(Constants.BASE_RAW_URL + path).openConnection();
                connection.addRequestProperty("Accept-Encoding", "identity");
                var in = connection.getInputStream();
                byte[] buffer = new byte[connection.getContentLength()];
                in.read(buffer);
                in.close();
                handler.post(() -> callback.accept(buffer));
            } catch (IOException e) {
                e.printStackTrace();
                handler.post(() -> callback.accept(null));
            }
        });
    }

    public static void download(File directory, String filename, String path, AsyncCallback callback) {
        executor.execute(() -> {
            try {
                var connection = new URL(Constants.BASE_RAW_URL + path).openConnection();
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
                handler.post(() -> callback.onComplete(null));
            } catch (IOException e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }

    public static Scanner getFileReader(String path) throws Exception {
        var in = new URL(Constants.BASE_RAW_URL + path).openStream();
        return new Scanner(in);
    }

    public static boolean exists(String path) {
        try {
            return Executors.newCachedThreadPool().submit(() -> {
                try {
                    var connection = (HttpURLConnection) new URL(Constants.BASE_RAW_URL + path).openConnection();
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

    public static List<GithubFilesAdapter.File> listFiles(String path) throws IOException {
        if (!path.endsWith("/"))
            path = path + "/";

        var connection = new URL(Constants.TREE_PRIVATE_URL + path).openConnection();
        connection.addRequestProperty("accept", "application/json");
        StringBuilder str = new StringBuilder();
        Scanner scanner = new Scanner(connection.getInputStream());
        while (scanner.hasNext())
            str.append(scanner.nextLine());

        scanner.close();
        try {
            var files = new ArrayList<GithubFilesAdapter.File>();
            var json = new JSONObject(str.toString());
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                var next = keys.next();
                var isFile = next.endsWith(".doc") || next.endsWith(".txt") || next.endsWith(".json");
                var sha = json.getJSONObject(next).getString("oid");
                var file = new GithubFilesAdapter.File(isFile, next,sha,path + next);
                files.add(file);
            }
            return files;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static void reportBug(Context context, Exception e) {
        e.printStackTrace();
        try {
            var appVersion = context.getPackageManager().getPackageInfo("com.clorabase.console", PackageManager.GET_META_DATA).versionName;
            var deviceInfo = String.format("""
                    Device : %1s
                    Android : %2s
                    App version : %3s
                    Date : %4s
                    ---------[ STACK TRACE ]------------
                                           \s
                   \s""", Build.DEVICE, Build.VERSION.SDK, appVersion, new Date()) + e.getLocalizedMessage();
            Toast.makeText(context, "App crashed! Please send bug report to the developer", Toast.LENGTH_LONG).show();

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"clorabase@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Crash Report - Clorabase console");
            emailIntent.putExtra(Intent.EXTRA_TEXT, deviceInfo);

            context.startActivity(emailIntent);
        } catch (PackageManager.NameNotFoundException ex) {
            Toast.makeText(context, "Please report this to the developer manually", Toast.LENGTH_SHORT).show();
        }
    }

    protected static byte[] encrypt(String str, String pass) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE,generateKey(pass));
        return cipher.doFinal(str.getBytes());
    }

    public static SecretKey generateKey(String password) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), "abcdefgh".getBytes(), 786, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static String decrypt(byte[] str, String pass) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,generateKey(pass));
            var bytes = cipher.doFinal(str);
            return new String(bytes);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void runAsync(Callable<Void> task, AsyncCallback callback){
        executor.execute(() -> {
            try {
                task.call();
                handler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }

    public static List<String> getProjects(){
        try {
            return listFiles("/").stream().filter(file -> !file.isFile).map(file -> file.name).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            if (!(e instanceof FileNotFoundException))
                throw new RuntimeException(e);
            else
                return new ArrayList<>();
        }
    }

    public interface AsyncCallback {
        void onComplete(@Nullable String sha);

        void onError(Exception e);
    }
}
