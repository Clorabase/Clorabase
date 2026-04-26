package clorabase.sdk.android.storage;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Represents a directory in the Clorabase storage system. It is just like your traditional
 * directory in a file system, but it is used to organize and manage files within the Clorabase storage.
 */
public class ClorabaseStorage {
    private static final Executor executor = Executors.newSingleThreadExecutor();
    private final clorabase.sdk.java.storage.ClorabaseStorage coreStorge;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    public ClorabaseStorage(@NonNull clorabase.sdk.java.storage.ClorabaseStorage core) {
        this.coreStorge = core;
    }



    /**
     * Goes into a subdirectory of this directory or creates a new one if it does not exist.
     * @param name the name of the subdirectory. Eg "images", "documents", etc.
     * @return a new ClorabaseStorage instance representing the subdirectory
     */
    public ClorabaseStorage directory(String name) {
        var core = coreStorge.directory(name);
        return new ClorabaseStorage(core);
    }

    /**
     * Uploads a blob to the Clorabase storage asynchronously. The file is uploaded as a release asset
     * and a pointer to the blob is created in the directory.
     * @param io       the InputStream containing the blob data
     * @param fileName the name of the file to be uploaded
     * @param listener a ProgressListener to track upload progress
     */
    public void uploadBlob(@NonNull InputStream io, @NonNull String fileName, @NonNull ProgressListener listener) {
        executor.execute(() -> {
            coreStorge.uploadBlob(io, fileName, new clorabase.sdk.java.storage.ProgressListener() {
                @Override
                public void onProgress(long bytesWritten, long totalBytes) {
                    mainHandler.post(() -> listener.onProgress(bytesWritten, totalBytes));
                }
                @Override
                public void onComplete(String result) {
                    mainHandler.post(listener::onComplete);
                }
                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> listener.onError(e));
                }
            });
        });
    }

    /**
     * Adds a file to this directory asynchronously. The file must not be more than 1 MB
     *
     * @param contents the contents of the file as a byte array
     * @param fileName the name of the file to be added
     * @return a Task that completes when the file is added
     */
    public Task<Void> addFile(byte[] contents, String fileName) {
        return Tasks.call(executor, () -> {
            coreStorge.addFile(contents, fileName);
            return null;
        });
    }

    /**
     * Uploads a file to this directory asynchronously. The file must be less than 50 MB.
     *
     * @param file   The file to be uploaded
     * @param name   The name of the file to be uploaded
     * @param listener A ProgressListener to track the upload progress
     */
    public void uploadFile(@NonNull InputStream file,@NonNull String name, @NonNull ProgressListener listener){
        executor.execute(() -> {
            coreStorge.uploadFile(file, name, new clorabase.sdk.java.storage.ProgressListener() {
                @Override
                public void onProgress(long bytesWritten, long totalBytes) {
                    mainHandler.post(() -> listener.onProgress(bytesWritten, totalBytes));
                }

                @Override
                public void onComplete(String result) {
                    mainHandler.post(listener::onComplete);
                }

                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> listener.onError(e));
                }

            });
        });
    }

    /**
     * Gets the contents of a file as a byte array asynchronously.
     *
     * @param name the name of the file to retrieve
     * @return a Task that completes with the file contents
     */
    public Task<InputStream> getBlobStream(String name) {
        return Tasks.call(executor, () -> coreStorge.getBlobStream(name));
    }

    /**
     * Deletes a file from the directory asynchronously.
     *
     * @param name the name of the file to delete
     * @return a Task that completes when the file is deleted
     */
    public Task<Void> delete(String name) {
        return Tasks.call(executor, () -> {
            coreStorge.delete(name);
            return null;
        });
    }

    /**
     * Deletes a blob from the directory asynchronously.
     *
     * @param name the name of the blob to delete
     * @return a Task that completes when the blob is deleted
     */
    public Task<Void> deleteBlob(String name) {
        return Tasks.call(executor, () -> {
            coreStorge.deleteBlob(name);
            return null;
        });
    }

    /**
     * Retrieves the contents of a file stored in this directory asynchronously.
     *
     * @param name the name of the file to be retrieved
     * @return a Task that completes with the contents of the file as a byte array
     */
    public Task<byte[]> getFile(String name) {
        return Tasks.call(executor, () -> coreStorge.getFile(name));
    }

    /**
     * Lists all files in this directory asynchronously.
     *
     * @return a Task that completes with a list of file names in this directory
     */
    public Task<List<String>> listFiles() {
        return Tasks.call(executor, coreStorge::listFiles);
    }


}
