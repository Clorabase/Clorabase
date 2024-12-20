package com.clorabase.console.storage;


import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.clorabase.console.Constants;
import com.clorabase.console.Utils;

import org.kohsuke.github.GHAsset;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Objects;
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


    public static void download(@NonNull String user,@NonNull String project,@NonNull String filename, @NonNull java.io.File directory,@Nullable ClorabaseStorageCallback callback) {
        executor.submit(() -> {
            try {
                var url = MessageFormat.format(Constants.RELEASE_DOWNLOAD_URL,user, project) + filename;
                var connection = new URL(url).openConnection();
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

    public static void upload(@NonNull String project, @NonNull java.io.File file, @Nullable ClorabaseStorageCallback callback) {
        executor.submit(() -> {
            try {
                var release = Utils.repo.getReleaseByTagName(project);
                if (release == null && callback != null) {
                    handler.post(() -> callback.onFailed(new IllegalStateException("Storage bucket not configured for this project.")));
                } else {
                    InputStream in = new FileInputStream(file);
                    if (callback != null) {
                        in = new ProgressInputStream(in, file.length(), callback);
                    }
                    release.uploadAsset(file.getName(), in, "application/octet-stream");
                }
                if (callback != null) handler.post(callback::onComplete);
            } catch (IOException | AssertionError e) {
                if (callback != null){
                    if (e instanceof AssertionError)
                        handler.post(() -> callback.onFailed(new RuntimeException("Storage bucket not configured for this project.")));
                    else
                        handler.post(() -> {
                            if (Objects.requireNonNull(e.getMessage()).contains("already_exists"))
                                callback.onFailed(new RuntimeException("A file with this name already exists"));
                        });
                }
            }
        });
    }

    public static void delete(@NonNull String project, @NonNull String filename, @Nullable ClorabaseStorageCallback callback) {
        executor.submit(() -> {
            try {
                var release = Utils.repo.getReleaseByTagName(project);
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

    public static void list(@NonNull String project, @NonNull Consumer<Iterator<File[]>> callback){
        executor.submit(() -> {
            try {
                var release = Utils.repo.getReleaseByTagName(project);
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
                                var pathStr = file.getName().substring(0,file.getName().lastIndexOf('_'));
                                var path = pathStr.replace('_','/');
                                filesArray[i] = new File(file.getName(),path,file.getBrowserDownloadUrl(), file.getSize());
                            }
                            return filesArray;
                        }
                    };

                    Utils.handler.post(() -> callback.accept(iterator));
                }
            } catch (IOException e) {
                Utils.handler.post(() -> callback.accept(null));
            }
        });
    }

    private static void trackProgress(int read, long total,ClorabaseStorageCallback callback) {
        current += read;
        int percentage = (int) ((current * 100) / total);
        handler.post(() -> callback.onProgress(percentage));
    }

    public static class File {
        public String path;
        public String name;
        public String download_url;
        public long size;

        public File(String name,String path, String download_url, long size) {
            this.name = name;
            this.download_url = download_url;
            this.size = size;
            this.path = path;
        }
    }
}
