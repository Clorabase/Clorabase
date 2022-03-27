package com.clorabase;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.DownloadListener;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.clorabase.storage.ClorabaseStorage;
import com.xcoder.easyauth.EasyAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Response;

public class DriveHelper {
    public static final String ID = "402416439097-j01jvkbrkjttqb1ugoopi4hu7bi94a3o.apps.googleusercontent.com";
    public static final String SECRET = "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";
    public String token;
    public Thread thread;
    public Exception exception;

    public DriveHelper(String token) {
        EasyAuth auth = new EasyAuth(ID,SECRET);
        thread = new Thread(() -> {
            try {
                DriveHelper.this.token = auth.getAccessToken(token);
            } catch (Exception e) {
                exception = e;
            }
        });
        thread.start();
    }

    public void updateFileAsync(File file, String id, Callback callback) {
        AndroidNetworking.patch("https://www.googleapis.com/upload/drive/v3/files/" + id + "?uploadType=media")
                .addHeaders("Authorization", "Bearer " + token)
                .addFileBody(file)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(ANError anError) {
                        if (anError.getErrorCode() == 401) {
                            try {
                                thread.join(5000);
                                if (exception == null) {
                                    updateFileAsync(file, id, callback);
                                } else
                                    callback.onError(exception);
                            } catch (InterruptedException e) {
                                callback.onError(new Exception("Database not initialized successfully"));
                            }
                        } else {
                            String errorBody = anError.getErrorBody();
                            callback.onError(new Exception(errorBody == null ? anError.getMessage() : errorBody));
                        }
                    }
                });
    }

    public boolean updateFile(String content, String id) {
        ANResponse response = AndroidNetworking.patch("https://www.googleapis.com/upload/drive/v3/files/" + id + "?uploadType=media")
                .addHeaders("Authorization", "Bearer " + token)
                .addStringBody(content)
                .build()
                .executeForOkHttpResponse();

        return response.isSuccess();
    }

    @Nullable
    public String createFile(@NonNull String filepath, @Nullable String parentId) {
        try {
            JSONObject body = new JSONObject();
            if (filepath.startsWith("/"))
                filepath = filepath.substring(1);
            String[] folders = filepath.split("/");
            for (int i = 0; i < folders.length; i++) {
                body.put("name", folders[i]);
                body.put("mimeType", i == folders.length - 1 ? null : "application/vnd.google-apps.folder");
                body.put("parents", parentId == null ? null : new JSONArray("[" + parentId + "]"));
                ANResponse<JSONObject> response = AndroidNetworking.post("https://www.googleapis.com/drive/v3/files")
                        .addHeaders("Authorization", "Bearer " + token)
                        .addJSONObjectBody(body)
                        .build()
                        .executeForJSONObject();

                if (response.isSuccess())
                    parentId = response.getResult().optString("id");
                else
                    return null;
            }
            return parentId;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void download(File filepath, String id) throws Exception {
        ANResponse response = AndroidNetworking.download("https://www.googleapis.com/drive/v3/files/" + id + "?alt=media", filepath.getParent(), filepath.getName())
                .addHeaders("Authorization", "Bearer " + token)
                .build()
                .executeForDownload();

        if (!response.isSuccess()) {
            if (response.getError().getErrorCode() == 401) {
                try {
                    thread.join(5000);
                    if (exception == null) {
                        download(filepath, id);
                    } else
                        throw exception;
                } catch (InterruptedException e) {
                    throw new Exception("Database not initialized successfully");
                }
            } else {
                String errorBody = response.getError().getErrorBody();
                throw new Exception(errorBody == null ? response.getError().getMessage() : errorBody);
            }
        }
    }


    @Nullable
    public String getContent(String id) {
        ANResponse<Response> response = AndroidNetworking.get("https://www.googleapis.com/drive/v3/files/" + id + "?alt=media")
                .addHeaders("Authorization", "Bearer " + token)
                .build()
                .executeForOkHttpResponse();

        try {
            if (response.isSuccess()) {
                return new String(response.getOkHttpResponse().body().bytes());
            } else {
                if (response.getError().getErrorCode() == 401) {
                    try {
                        thread.join(5000);
                        if (exception == null) {
                            return getContent(id);
                        } else
                            return new String(response.getOkHttpResponse().body().bytes());
                    } catch (InterruptedException e) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }


    public String getFileId(String filename,String folderId) {
        ANResponse<JSONObject> response = AndroidNetworking.get("https://www.googleapis.com/drive/v3/files/?trashed=false")
                .addHeaders("Authorization", "Bearer " + token)
                .addPathParameter("q", "name = '" + filename + "' and '" + folderId + "' in parents")
                .build()
                .executeForJSONObject();
        if (response.isSuccess()) {
            JSONArray array = response.getResult().optJSONArray("files");
            if (array != null && array.length() > 0) {
                return array.optJSONObject(0).optString("id");
            } else
                return null;
        }
        return null;
    }


    public void downloadAsync(File file, String id, Callback callback) {
        AndroidNetworking.download("https://www.googleapis.com/drive/v3/files/" + id + "?alt=media", file.getParent(), file.getName())
                .addHeaders("Authorization", "Bearer " + token)
                .build()
                .startDownload(new DownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(ANError anError) {
                        if (anError.getErrorCode() == 401) {
                            try {
                                thread.join(5000);
                                if (exception == null) {
                                    downloadAsync(file, id, callback);
                                } else
                                    callback.onError(exception);
                            } catch (InterruptedException e) {
                                callback.onError(new Exception("Database not initialized successfully"));
                            }
                        } else {
                            String errorBody = anError.getErrorBody();
                            callback.onError(new Exception(errorBody == null ? anError.getMessage() : errorBody));
                        }
                    }
                });
    }

    public void downloadFile(File file, String fileId, @NonNull ClorabaseStorage.ClorabaseStorageCallback listener) {
        ANResponse<JSONObject> response = AndroidNetworking.get("https://www.googleapis.com/drive/v3/files/" + fileId + "?fields=size&trashed=false")
                .addHeaders("Authorization", "Bearer " + token)
                .build()
                .executeForJSONObject();

        if (response.isSuccess()) {
            long size = response.getResult().optLong("size");
            AndroidNetworking.download("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media", file.getParent(), file.getName())
                    .addHeaders("Authorization", "Bearer " + token)
                    .build()
                    .setDownloadProgressListener((l, l1) -> {
                        listener.onProgress((int) ((l * 100) / size));
                    })
                    .startDownload(new DownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            listener.onComplete(fileId);
                        }

                        @Override
                        public void onError(ANError anError) {
                            listener.onFailed(new Exception(anError.getErrorBody() == null ? anError.getMessage() : anError.getErrorBody()));
                        }
                    });
        } else {
            ANError anError = response.getError();
            listener.onFailed(new Exception(anError.getErrorBody() == null ? anError.getMessage() : anError.getErrorBody()));
        }
    }

    public void uploadFile(File file, String folderID, @NonNull ClorabaseStorage.ClorabaseStorageCallback listener) {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                String id = createFile(file.getName(), folderID);
                if (id == null){
                    listener.onFailed(new Exception("Cannot create file on drive. Make sure you have passed valid storage bucket id & it is configured properly"));
                    return;
                }
                HttpURLConnection connection = (HttpURLConnection) new URL("https://www.googleapis.com/upload/drive/v3/files/" + id + "?uploadType=media").openConnection();
                connection.addRequestProperty("Authorization", "Bearer " + token);
                OutputStream os = connection.getOutputStream();
                InputStream in = new FileInputStream(file);
                long uploaded = 0;
                long total = in.available();
                int read;
                byte[] bytes = new byte[1024 * 5];
                while ((read = in.read(bytes)) != -1) {
                    uploaded += read;
                    os.write(bytes, 0, read);
                    long finalUploaded = uploaded;
                    handler.post(() -> listener.onProgress((int) ((finalUploaded * 100) / total)));
                }
                os.close();
                in.close();
                os.flush();
                handler.post(() -> listener.onComplete(id));
            } catch (IOException e) {
                listener.onFailed(e);
            }
        }).start();
    }

    public int getFolderSize(@Nullable String folderId) {
        ANResponse<JSONObject> response = AndroidNetworking.get("https://www.googleapis.com/drive/v3/files/?fields=(size,mimeType,id)&trashed=false")
                .addHeaders("Authorization", "Bearer " + token)
                .addPathParameter("q","'" + (folderId == null ? "root" : folderId) + "' in parents")
                .build()
                .executeForJSONObject();
        if (response.isSuccess()) {
            long size = 0;
            JSONArray array = response.getResult().optJSONArray("files");
            for (int i = 0; i < array.length(); i++) {
                JSONObject file = array.optJSONObject(i);
                if (file.optString("mimeType").equals("application/vnd.google-apps.folder")) {
                    size += getFolderSize(file.optString("id"));
                } else
                    size += file.optLong("size");
            }
            return (int) (size/(1024*1024));
        } else {
            return 0;
        }
    }

    public interface Callback {
        void onSuccess();

        void onError(Exception e);
    }
}
