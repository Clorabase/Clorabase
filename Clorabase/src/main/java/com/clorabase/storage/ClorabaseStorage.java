package com.clorabase.storage;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clorabase.DriveHelper;

import java.io.File;

/**
 * ClorabaseStorage is a free place where you can upload and download files related to your apps.
 * All the operation of Storage is performed from this class. All method of this class runs on separate thread.
 *
 * @since Clorabase V1.0
 */
public class ClorabaseStorage {
    private final String id;
    private final ClorabaseStorageCallback callback;
    private final DriveHelper helper;


    /**
     * Gets the instance of the class. This instance may not be singleton.
     * @param storageId The id of your storage bucket. You can get it from console.
     */
    public ClorabaseStorage(@NonNull String storageId, @NonNull String token, @Nullable ClorabaseStorageCallback callback) {
        helper = new DriveHelper(token);
        id = storageId;
        this.callback = callback == null ? new ClorabaseStorageCallback() {
            @Override
            public void onFailed(@NonNull Exception e) {

            }

            @Override
            public void onProgress(int prcnt) {

            }

            @Override
            public void onComplete(@NonNull String fileId) {

            }
        }: callback;
    }

    /**
     * Uploads file asynchronously to the Clorabase Storage. Progress & result (fileId) will
     * be reported through callback passed in constructor.
     * @param file The file to upload, your application must have permission to read this file.
     */
    public void upload(@NonNull File file){
        helper.uploadFile(file,id,callback);
    }

    /**
     * Download the file from ClorabaseStorage to the provided directory. Progress will be reported through callback passed in constructor
     * @param fileId The ID of the file.
     * @param directory The external directory where to save the file.
     */
    public void download(@NonNull String fileId,@NonNull File directory){
        helper.downloadFile(directory,fileId,callback);
    }


    /**
     * The interface that is used as a callback for the ClorabaseStorage operations.
     */
    public interface ClorabaseStorageCallback{
        void onFailed(@NonNull Exception e);
        void onProgress(int prcnt);
        void onComplete(@NonNull String fileId);
    }
}
