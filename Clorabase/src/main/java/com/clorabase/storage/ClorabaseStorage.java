package com.clorabase.storage;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * ClorabaseStorage is a free place where you can upload and download files related to your apps.
 * All the operation of Storage is performed from this class. All method of this class runs on separate thread.
 *
 * @since Clorabase V1.0
 */
public class ClorabaseStorage {
    private static ClorabaseStorage INSTANCE;
    private static DocumentReference storage;
    private static String fileId;
    private static ClorabaseCallback callback;
    private static OnCompleteListener<Void> listener;

    protected ClorabaseStorage() {}

    /**
     * Gets the instance of the class. This instance may not be singleton instance.
     * @param context Activity's context
     * @return A newly created instance.
     */
    public static ClorabaseStorage getInstance(Context context) {
        if (INSTANCE == null){
            INSTANCE = new ClorabaseStorage();
            FirebaseApp app = FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                    .setApplicationId("1:1084008315464:android:7de49d002d21c79928767d")
                    .setProjectId("clora-base")
                    .setApiKey("AIzaSyAw__oC4tCQDcuP-4-ZZPS3ObsI-SiQOGo")
                    .build());
            storage = FirebaseFirestore.getInstance(app).collection("Storage").document(context.getPackageName());
        }

        listener = new OnCompleteListener<Void>() {
            int taskCount;
            boolean failed;

            @Override
            public void onComplete(@NonNull Task task) {
                if (!task.isSuccessful())
                    failed = true;

                if (taskCount == 2){
                    if (failed)
                        callback.onFailed(new Exception("Failed to save file information in database."));
                    else
                        callback.onSuccess(fileId);
                } else
                    taskCount++;
            }
        };
        return INSTANCE;
    }

    /**
     * Uploads file to the Clorabase Storage.
     * @param fileStream The InputStream containing bytes of the file. To get a InputStream from a uri, use {@code getContentResolver.openStream(uri)}
     * @param filename The name of the file. To get name from the uri use, {@code uri.getLastSegmentPath()}
     * @param uploadingCallback The callback indication success/failure and progress of the upload. Note that
     * Success / Failure are independent of progress. It means that upload may fail even if it invokes progress method.
     */
    public void upload(InputStream fileStream,String filename,ClorabaseCallback uploadingCallback){
        callback = uploadingCallback;
        // Checking if file already exist.
        storage.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists() && documentSnapshot.getData() != null)
                    if (((List<String>) documentSnapshot.getData().get("names")).contains(filename)){
                        callback.onFailed(new Exception("A file with name '" + filename + "' already exist in the database"));
                    } else {
                        // Uploading file to server
                        HttpPost post = new HttpPost("https://api.anonfiles.com/upload");
                        post.setEntity(MultipartEntityBuilder.create().addPart("file",new ProgressFileBody(fileStream,filename,callback)).build());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Executing & getting the response.
                                    String json = EntityUtils.toString(HttpClients.createDefault().execute(post).getEntity());
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                JSONObject object =  new JSONObject(json);
                                                if (!object.getBoolean("status")){
                                                    callback.onFailed(new Exception(object.getJSONObject("error").getString("message")));
                                                    return;
                                                }
                                                // Extracting data from response
                                                object = object.getJSONObject("data").getJSONObject("file").getJSONObject("metadata");
                                                fileId = object.getString("id");
                                                String name = object.getString("name");
                                                String size = object.getJSONObject("size").getString("readable");
                                                // Saving file info on database
                                                storage.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                        if (!documentSnapshot.exists()){
                                                            Map<String,Object> map = new HashMap<>();
                                                            map.put("names",new ArrayList<>());
                                                            map.put("sizes",new ArrayList<>());
                                                            map.put("ids",new ArrayList<>());
                                                            storage.set(map);
                                                        }
                                                        storage.update("names", FieldValue.arrayUnion(name)).addOnCompleteListener(listener);
                                                        storage.update("sizes", FieldValue.arrayUnion(size)).addOnCompleteListener(listener);
                                                        storage.update("ids", FieldValue.arrayUnion(fileId)).addOnCompleteListener(listener);
                                                    }
                                                });
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onFailed(new Exception("API request error"));
            }
        });
    }

    /**
     * Download the file from ClorabaseStorage to the provided location.
     * @param fileId The unique ID of the file.
     * @param directory The external directory where to save the file.
     * @param callback The callback indication success,failure and progress of the download. Note that
     *                 Success / Failure are independent of progress. It means that download may fail even if it invokes progress method.
     */
    public void download(@NonNull String fileId,@NonNull File directory,@Nullable ClorabaseCallback callback){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Document document = Jsoup.connect("https://anonfiles.com/" + fileId).get();
                    Element element = document.getElementById("download-url");
                    String link = element.attr("href");
                    URLConnection response = new URL(link).openConnection();
                    String header = response.getHeaderField("Content-Disposition");
                    String filename = header.substring(header.indexOf("filename=\"") + 10, header.length() - 1);
                    InputStream in = response.getInputStream();
                    FileOutputStream out = new FileOutputStream(new File(directory,filename));
                    byte[] bytes = new byte[1024];
                    int read;
                    long bytesWritten = 0;
                    while ((read = in.read(bytes)) != -1) {
                        bytesWritten += read;
                        out.write(bytes, 0, read);
                        if (callback != null)
                            callback.onProgress((int) ((bytesWritten * 100) / response.getContentLength()));
                    }
                    in.close();
                    out.close();
                    if (callback != null)
                        callback.onSuccess(null);
                } catch (IOException e){
                    if (callback != null)
                        callback.onFailed(e);
                }
            }
        }).start();
    }

    /**
     * Deletes the file from the storage denoted with the file id.
     * @param fileId The id of the file to delete.
     * @param deleteCallback The callback indication success or failure of the process. {@code onProgress()} is unaffected.
     * @throws NullPointerException If file id does not exist.
     */
    public void delete(@NonNull String fileId,ClorabaseCallback deleteCallback){
        callback = deleteCallback;
        storage.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String,List<String>> map = (Map) documentSnapshot.getData();
                List<String> ids = map.get("ids");
                List<String> names = map.get("names");
                List<String> sizes = map.get("sizes");
                int position = ids.indexOf(fileId);
                storage.update("ids", FieldValue.arrayRemove(fileId)).addOnCompleteListener(listener);
                storage.update("names", FieldValue.arrayRemove(names.get(position))).addOnCompleteListener(listener);
                storage.update("sizes", FieldValue.arrayRemove(sizes.get(position))).addOnCompleteListener(listener);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callback.onFailed(new Exception("API callback error"));
            }
        });
    }


    /**
     * The interface that is used as a callback for the ClorabaseStorage operations.
     */
    public interface ClorabaseCallback extends ProgressFileBody.ProgressListener {
        void onFailed(@NonNull Exception e);
        void onSuccess(@Nullable String fileId);
    }
}
