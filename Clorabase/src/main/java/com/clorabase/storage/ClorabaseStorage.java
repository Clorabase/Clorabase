package com.clorabase.storage;


import androidx.annotation.NonNull;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.OkHttpResponseListener;
import com.clorabase.GithubUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Response;

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
    public static void download(@NonNull String project,@NonNull String filename, @NonNull File directory, @NonNull ClorabaseStorage.ClorabaseStorageCallback listener) {
        new Thread(() -> GithubUtils.download(directory,filename,project + "/Storage/" + filename,listener)).start();
    }

    /**
     * Uploads the file from the given directory to the given project storage bucket.
     *
     * @param project  Project name. Should be created in the Clorabase console.
     * @param file     File to be uploaded.
     * @param listener Listener for the upload progress and file-id of the uploaded file.
     */
    public static void upload(@NonNull String project, @NonNull File file, @NonNull ClorabaseStorageCallback listener) {
        AndroidNetworking.upload("https://clorabase.herokuapp.com/github/upload")
                .addQueryParameter("owner", "Clorabase-databases")
                .addQueryParameter("repo", "CloremDatabases")
                .addQueryParameter("path", project + "/Storage/" + file.getName())
                .addQueryParameter("token", GithubUtils.token)
                .addMultipartFile("file",file)
                .build()
                .setUploadProgressListener((l, l1) -> listener.onProgress((int) ((l*100)/l1))).getAsOkHttpResponse(new OkHttpResponseListener() {
            @Override
            public void onResponse(Response response) {
                if (response.isSuccessful())
                    listener.onComplete();
                else 
                    listener.onFailed(new Exception(response.message()));
            }

            @Override
            public void onError(ANError anError) {
                listener.onFailed(anError);
            }
        });
    }

    public static void delete(@NonNull String project, @NonNull String filename, @NonNull ClorabaseStorageCallback listener) {
        new Thread(() -> {
            try {
                GithubUtils.github.getRepository("Clorabase-databases/CloremDatabases")
                        .getFileContent(project + "/Storage/" + filename)
                        .delete("Deleted from the clorabase");
                listener.onComplete();
            } catch (IOException e) {
                listener.onFailed(e);
            }
        }).start();
    }


    /**
     * The interface that is used as a callback for the ClorabaseStorage operations.
     */
    public interface ClorabaseStorageCallback {
        void onFailed(@NonNull Exception e);

        void onProgress(int prcnt);

        void onComplete();
    }
}
