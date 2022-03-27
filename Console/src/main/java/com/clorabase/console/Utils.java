package com.clorabase.console;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.ammarptn.gdriverest.DriveServiceHelper;
import com.clorabase.console.fragments.StorageFragment;
import com.clorem.db.Clorem;
import com.clorem.db.Node;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class Utils {
    public static Node db;
    public static Drive drive;
    public static DriveServiceHelper helper;
    public static String clorabaseID;
    public static String fileId;
    public static final String TOKEN = "1//04jTcoedCwNbGCgYIARAAGAQSNwF-L9Ir5bneWMUGot0Aif1y4Iedon8pSA189tf5qiHLJ100H3zfWtP0I6u27vL_FY4a8dNt0Po";

    public static void init(Context context){
        db = Clorem.getInstance(context, "main").getDatabase();
        Credential credential = new GoogleCredential.Builder()
                .setClientSecrets("402416439097-j01jvkbrkjttqb1ugoopi4hu7bi94a3o.apps.googleusercontent.com", "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")
                .setJsonFactory(AndroidJsonFactory.getDefaultInstance())
                .setTransport(AndroidHttp.newCompatibleTransport())
                .build()
                .setAccessToken("ya29.A0ARrdaM8l-ym8pfePD9xklLoaXDe3SG_9be0qK99RAmu6yK4aOnFAbbk-yVDU7yisbsj4ZvhTo9OgQh3bbJeIKtrjMhSkBk838vpCOIFppgkkSNYED3Omt3SKJ2tGxFpWM9LoDBHujlvhY1FzdaJt_h1r2NLB")
                .setRefreshToken(TOKEN);
        drive = new Drive(AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(), credential);
        helper = new DriveServiceHelper(drive);
        if (db.getString("clorabaseRoot", null) == null){
            helper.createFolderIfNotExist("Clorabase", null).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    clorabaseID = task.getResult().getId();
                    db.put("clorabaseRoot", clorabaseID);
                } else
                    task.getException().printStackTrace();
            });
        } else
            clorabaseID = db.getString("clorabaseRoot",null);
    }


    public static String getFileId(String name,String folderId){
        try {
            return Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    FileList result = drive.files().list()
                            .setQ("'" + folderId + "' in parents and name = '" + name + "'")
                            .setSpaces("drive")
                            .execute();

                    if (result.size() > 0)
                        return result.getFiles().get(0).getId();
                    else
                        return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }).get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }


    public static Task<Void> updateFile(String fileId, String content){
        return Tasks.call(Executors.newSingleThreadExecutor(),() -> {
            drive.files().update(fileId, new File(), ByteArrayContent.fromString("application/json",content)).execute();
            return null;
        });
    }


    public static void upload(InputStream in, String filename, String folderId,StorageFragment.ProgressListener listener){
        new Thread(() -> {
            try {
                File file = new File();
                file.setName(filename);
                file.setParents(Collections.singletonList(folderId));
                Drive.Files.Create uploader = drive.files().create(file, new AbstractInputStreamContent("*/*") {
                    @Override
                    public InputStream getInputStream() {
                        return in;
                    }

                    @Override
                    public long getLength() throws IOException {
                        return in.available();
                    }

                    @Override
                    public boolean retrySupported() {
                        return false;
                    }
                });
                uploader.getMediaHttpUploader().setProgressListener(uploader1 -> {
                    MediaHttpUploader.UploadState state = uploader1.getUploadState();
                    int progress = (int) (uploader1.getProgress() * 100);
                    if (state == MediaHttpUploader.UploadState.MEDIA_COMPLETE)
                        new Handler(Looper.getMainLooper()).post(() -> listener.onComplete(fileId));
                    else if (state == MediaHttpUploader.UploadState.MEDIA_IN_PROGRESS)
                        new Handler(Looper.getMainLooper()).post(() -> listener.onProgress(progress));
                });
                uploader.getMediaHttpUploader().setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);
                fileId = uploader.execute().getId();
            } catch (IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> listener.onFailed(e));
            }
        }).start();
    }


    public static Task<Long> getFolderSize(String folderId){
        return Tasks.call(Executors.newSingleThreadExecutor(),() -> {
            try {
                FileList result = drive.files().list()
                        .setQ("'" + folderId + "' in parents")
                        .setSpaces("drive")
                        .execute();
                long size = 0;
                for (File file : result.getFiles()) {
                    if (file.getMimeType().equals("application/vnd.google-apps.folder"))
                        size += Tasks.await(getFolderSize(file.getId()));
                    else
                        size += file.getSize();
                }
                return size;
            } catch (IOException e) {
                e.printStackTrace();
                return 0L;
            }
        });
    }
}
