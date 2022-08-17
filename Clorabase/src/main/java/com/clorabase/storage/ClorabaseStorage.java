package com.clorabase.storage;


import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.clorabase.GithubUtils;
import com.jsoup.filescrapper.DriveException;
import com.jsoup.filescrapper.FileScrapper;
import com.jsoup.filescrapper.Provider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

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
       new Thread(() -> {
           try {
               var link = FileScrapper.getDownloadLink(Provider.ANONFILES, "https://anonfiles.com/" + getFileId(project,filename));
               AndroidNetworking.download(link, directory.getAbsolutePath(), filename)
                       .setTag("download")
                       .addHeaders("Accept-Encoding","compress")
                       .build()
                       .setDownloadProgressListener((downloaded, total) -> new Handler(Looper.getMainLooper()).post(() -> listener.onProgress((int) ((int) downloaded * 100 / total))))
                       .startDownload(new DownloadListener() {
                   @Override
                   public void onDownloadComplete() {
                       listener.onComplete();
                   }

                   @Override
                   public void onError(ANError anError) {
                       System.out.println(anError.getErrorBody());
                       listener.onFailed(anError);
                   }
               });
           } catch (DriveException | Exception e) {
               listener.onFailed(new Exception(e));
           }
       }).start();
    }

    private static String getFileId(@NonNull String project,@NonNull String filename) throws Exception {
        var scanner = GithubUtils.getFileReader(project + "/storage.prop");
        if (scanner == null)
            return null;
        else {
            var fileId = "";
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                if (line.contains(filename)) {
                    fileId = line.split("=")[1];
                    break;
                }
            }
            scanner.close();
            return fileId;
        }
    }

    /**
     * Uploads the file from the given directory to the given project storage bucket.
     *
     * @param project  Project name. Should be created in the Clorabase console.
     * @param file     File to be uploaded.
     * @param listener Listener for the upload progress and file-id of the uploaded file.
     */
    public static void upload(@NonNull String project, @NonNull File file, @NonNull ClorabaseStorageCallback listener) {
        if (!GithubUtils.exists(project + "/storage.prop")) {
            listener.onFailed(new IllegalStateException("Project storage bucket does not exist. Please configure storage from console"));
            return;
        }
        AndroidNetworking.upload("https://api.anonfiles.com/upload?token=1e949238139b6cad")
                .addMultipartFile("file", file)
                .build()
                .setUploadProgressListener((uploaded, total) -> listener.onProgress((int) (uploaded * 100 / total)))
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        new Thread(() -> {
                            try {
                                var id = jsonObject.getJSONObject("data").getJSONObject("file").getJSONObject("metadata").getString("id");
                                GithubUtils.getFileContent(project + "/storage.prop", bytes -> {
                                    var str = new String(bytes);
                                    str += file.getName() + "=" + id + "\n";
                                    try {
                                        GithubUtils.github.getRepository("Clorabase-databases/CloremDatabases")
                                                .getFileContent(project + "/storage.prop")
                                                .update(str.getBytes(),"Deleting item from storage");
                                        new Handler(Looper.getMainLooper()).post(listener::onComplete);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        new Handler(Looper.getMainLooper()).post(() -> listener.onFailed(e));
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                                new Handler(Looper.getMainLooper()).post(() -> listener.onFailed(e));
                            }
                        }).start();
                    }

                    @Override
                    public void onError(ANError anError) {
                        listener.onFailed(anError);
                    }
                });
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
