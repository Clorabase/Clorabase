package com.clorabase.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clorabase.DriveHelper;
import com.clorem.db.Clorem;
import com.clorem.db.Node;

import java.io.File;

/**
 * Clorabase database is the online database hosted on google drive and deployed on our server. All the
 * data is saved to the google drive while we only provide medium (API gateway) to access it.
 * @author Rahil khan
 * @version 2.0
 */
public class ClorabaseDatabase {
    private final String databaseId;
    private final Clorem clorem;
    private final DriveHelper helper;
    private DriveHelper.Callback callback;
    private volatile int size;
    private final File file;

    /**
     * Initializes the database connecting it to the server. The connection is authenticated using provided token.
     * If the token is invalid then the subsequent call on method of this class will fail throwing an exception for the
     * failure of the authentication. Authentication is done in background so it's better not to use database just after
     * calling this.
     * @param context Activity contexts
     * @param token Clorabase account token. You can get this from the console.
     * @param projectId The ID of your clorabase project. Get it from console.
     */
    public ClorabaseDatabase(@NonNull Context context,@NonNull String token,@NonNull String projectId) {
        helper = new DriveHelper(token);
        this.databaseId = helper.getFileId("database.json,",projectId);
        if (databaseId == null)
            throw new IllegalArgumentException("Invalid project id or database is not configured");
        else {
            new Thread(() -> size = helper.getFolderSize(databaseId)).start();
            clorem = Clorem.getInstance(context, "clorabase");
            file = new File(context.getApplicationInfo().dataDir, "databases/clorabase.json");
            this.callback = callback == null ? new DriveHelper.Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Exception e) {

                }
            } : callback;
        }
    }

    /**
     * Returns the local database of the device. This database is not synced with the server.
     * To sync it with the server, you need to call {@link #syncDatabase()} before calling this.
     * @return The root node of the database.
     */
    public Node getDatabase() {
        return clorem.getDatabase();
    }

    /**
     * Syncs and return the database. This database is synced with the server. You don't explicitly need
     * to call {@link #syncDatabase()}.
     * @param callback The callback which will have to node of the database if succeed, else an exception
     */
    public void getDatabaseAsync(@NonNull DatabaseCallback callback){
        helper.downloadAsync(file,databaseId,new DriveHelper.Callback() {
            @Override
            public void onSuccess() {
                callback.onCreated(clorem.getDatabase());
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Sync the local database with the server. This is synchronous call.
     * @throws Exception If failed to sync.
     */
    public void syncDatabase() throws Exception {
        helper.download(file,databaseId);
    }


    /**
     * Syncs the server with the local database. This is asynchronous call.
     * @param callback Optional callback indicating success and failure of the request.
     */
    public void syncServer(@Nullable DriveHelper.Callback callback) {
        callback = callback == null ? new DriveHelper.Callback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(Exception e) {

            }
        } : callback;
        if (size > 512)
            callback.onError(new Exception("Database size quota exceeds, please delete some data to save further"));
        else
            helper.updateFileAsync(file,databaseId,callback);
    }


    /**
     * Interface used with {@link #getDatabaseAsync(DatabaseCallback)} method.
     */
    public interface DatabaseCallback {
        void onCreated(Node node);
        void onError(Exception e);
    }
}
