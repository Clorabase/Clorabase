package com.clorabase.storage;


import androidx.annotation.NonNull;

import com.clorabase.GithubUtils;

import org.kohsuke.github.GHFileNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

/**
 * ClorabaseStorage is a free place where you can upload and download files related to your apps.
 * All the operation of Storage is performed from this class. All method of this class runs on separate thread.
 *
 * @since Clorabase V1.0
 */
public class ClorabaseStorage {

    /**
     * Download's the file from the given file-id and save it in the given directory.
     * The file is saved with the name of the file-id.
     *
     * @param directory Directory where the file will be saved.
     * @param listener  Listener for the download progress.
     */
    public static void download(@NonNull String projectId, @NonNull String filename, @NonNull File directory, @NonNull ClorabaseStorage.ClorabaseStorageCallback listener) {
        var project = GithubUtils.getProjectById(projectId);
        new Thread(() -> GithubUtils.download(directory, filename, project + "/Storage/" + filename, listener)).start();
    }

    /**
     * Uploads the file from the given directory to the given project storage bucket.
     *
     * @param file     File to be uploaded.
     * @param listener Listener for the success and failure of the upload.
     */
    public static void upload(@NonNull String projectId, @NonNull File file, @NonNull ClorabaseStorageCallback listener) {
        var project = GithubUtils.getProjectById(projectId);
        new Thread(() -> {
            try {
                var in = new FileInputStream(file);
                var content = new byte[(int) file.length()];
                in.read(content);
                in.close();
                GithubUtils.getRepository().createContent()
                        .content(content)
                        .path(project + "/Storage/" + file.getName())
                        .message("Uploaded on " + new Date())
                        .commit();
                listener.onComplete();
            } catch (IOException e) {
                listener.onFailed(e);
            }
        }).start();
    }

    public static void delete(@NonNull String projectId, @NonNull String filename, @NonNull ClorabaseStorageCallback listener) {
        var project = GithubUtils.getProjectById(projectId);
        new Thread(() -> {
            try {
                GithubUtils.getRepository().getFileContent(project + "/Storage/" + filename).delete("Deleted from the clorabase");
                listener.onComplete();
            } catch (IOException e) {
                if (e instanceof GHFileNotFoundException)
                    listener.onFailed(new FileNotFoundException("There is no file present in your storage bucket with this name"));
                else
                    listener.onFailed(e);
            }
        }).start();
    }


    /**
     * The interface that is used as a callback for the ClorabaseStorage operations.
     */
    public interface ClorabaseStorageCallback {
        void onFailed(@NonNull Exception e);

        void onComplete();
    }
}
