package com.clorabase.storage;


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.clorabase.GithubUtils;

import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ClorabaseStorage is a free place where you can upload and download files related to your apps.
 * All the operation of Storage is performed from this class. All method of this class runs on separate thread.
 *
 * @since Clorabase V1.0
 */
public class ClorabaseStorage {
    private static long current;
    private final static Handler handler = new Handler(Looper.getMainLooper());
    private static final ExecutorService executor = Executors.newCachedThreadPool();


    public static void download(@NonNull String projectId,@NonNull String filename, @NonNull java.io.File directory,@Nullable ClorabaseStorageCallback callback) {
        var project = GithubUtils.getProjectById(projectId);
        executor.submit(() -> {
            try {
                var connection = new URL("https://github.com/Clorabase-databases/OpenDatabases/releases/download/" + project + "/" + filename).openConnection();
                var in = connection.getInputStream();
                var os = new FileOutputStream(new java.io.File(directory, filename));
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                    if (callback != null) {
                        trackProgress(read, connection.getContentLength(),callback);
                    }
                }
                os.close();
                in.close();
                if (callback != null) handler.post(callback::onComplete);
            } catch (IOException e) {
                e.printStackTrace();
                if (callback != null)
                    handler.post(() -> callback.onFailed(e));
            }
        });
    }

    public static void upload(@NonNull String projectId,@NonNull java.io.File file,@Nullable ClorabaseStorageCallback callback) {
        var project = GithubUtils.getProjectById(projectId);
        executor.submit(() -> {
            try {
                var release = GithubUtils.getRepository().getReleaseByTagName(project);
                if (release == null) {
                    throw new RuntimeException("Storage bucket not configured for this project.");
                } else {
                    InputStream in;
                    if (callback == null) {
                        in = new FileInputStream(file);
                    } else {
                        in = new ProgressInputStream(file, callback, handler);
                    }
                    release.uploadAsset(file.getName(), in, "application/octet-stream");
                }
                if (callback != null) handler.post(callback::onComplete);
            } catch (IOException e) {
                if (callback != null)
                    handler.post(() -> {
                        if (Objects.requireNonNull(e.getMessage()).contains("already_exists"))
                            callback.onFailed(new RuntimeException("A file with this name already exists"));
                    });
            }
        });
    }

    public static void delete(@NonNull String projectId,@NonNull String filename,@Nullable ClorabaseStorageCallback callback) {
        var project = GithubUtils.getProjectById(projectId);
        executor.submit(() -> {
            try {
                var release = GithubUtils.getRepository().getReleaseByTagName(project);
                if (release == null) {
                    throw new RuntimeException("Storage bucket not configured for this project.");
                } else {
                    for (GHAsset asset : release.listAssets()) {
                        if (asset.getName().equals(filename)) {
                            asset.delete();
                            if (callback != null)
                                handler.post(callback::onComplete);
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                if (callback != null)
                    handler.post(() -> callback.onFailed(e));
            }
        });
    }

    /**
     * List all the files from the storage bucked of this project in the form of a paginated iterator.
     * The callback is invoked on the background thread because the {@link Iterator#hasNext()} is a network call.
     * @param projectId The id of the project
     * @param callback The callback that holds tha data. The data can be null if an IO error occurs
     *                 (most probably when the storage isn't configured)
     */
    public static void list(@NonNull String projectId, @NonNull Consumer<Iterator<File[]>> callback){
        var project = GithubUtils.getProjectById(projectId);
        executor.submit(() -> {
            try {
                var release = GithubUtils.getRepository().getReleaseByTagName(project);
                if (release == null) {
                    throw new RuntimeException("Storage bucket not configured for this project.");
                } else {
                    var pager = release.listAssets().iterator();
                    var iterator = new Iterator<File[]>() {
                        @Override
                        public boolean hasNext() {
                            return pager.hasNext();
                        }

                        @Override
                        public File[] next() {
                            var files = pager.nextPage();
                            var filesArray = new File[files.size()];
                            for (int i = 0; i < files.size(); i++) {
                                GHAsset file = files.get(i);
                                filesArray[i] = new File(file.getName(),file.getBrowserDownloadUrl(), file.getSize());
                            }
                            return filesArray;
                        }
                    };
                    callback.accept(iterator);
                }
            } catch (IOException e) {
                callback.accept(null);
            }
        });
    }

    private static void trackProgress(int read, long total,ClorabaseStorageCallback callback) {
        current += read;
        int percentage = (int) ((current * 100) / total);
        handler.post(() -> callback.onProgress(percentage));
    }

    public static class File {
        public String name;
        public String download_url;
        public long size;

        public File(String name, String download_url, long size) {
            this.name = name;
            this.download_url = download_url;
            this.size = size;
        }
    }
}
