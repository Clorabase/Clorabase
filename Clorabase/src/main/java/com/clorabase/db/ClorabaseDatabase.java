package com.clorabase.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ClorabaseDatabase {
    static ClorabaseDatabase instance;
    final CollectionReference root;

    private ClorabaseDatabase(Context context) {
        FirebaseApp app = FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("1:1084008315464:android:7de49d002d21c79928767d")
                .setProjectId("clora-base")
                .setApiKey("AIzaSyAw__oC4tCQDcuP-4-ZZPS3ObsI-SiQOGo")
                .build());
        root = FirebaseFirestore.getInstance(app).collection("Database/apps/" + context.getPackageName());
    }

    /**
     * Returns the instance of database. You can write,read in the database using this instance.
     *
     * @return {@link ClorabaseDatabase}.
     */
    public static ClorabaseDatabase getInstance(Context context) {
        if (instance == null)
            instance = new ClorabaseDatabase(context);
        return instance;
    }


    /**
     * Creates fields in the document. Creates document if not already exist. This will override any existing data in the document
     *
     * @param name   The name of the document
     * @param fields The fields in the form of map to store.
     * @return A FutureTask representation success or failure of the task.
     */
    public FutureTask<Void> document(String name, Map<String, Object> fields, boolean update) {
        FutureTask<Void> futureTask = new FutureTask<>();
        root.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                if (queryDocumentSnapshots.size() < 10) {
                    Task<Void> task;
                    if (update)
                        task = root.document(name).set(fields, SetOptions.merge());
                    else
                        task = root.document(name).set(fields);

                    task.addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful())
                                futureTask.successListener.onSuccess(task.getResult());
                            else {
                                futureTask.exception = task.getException();
                                futureTask.failureListener.onFailed(task.getException());
                            }
                        }
                    });
                } else
                    futureTask.exception = new Exception("You can only have maximum of 10 document in the database.");
            }
        });
        return futureTask;
    }


    /**
     * Gets the data from the document in the form of map.
     *
     * @param name the name of the document
     * @return Task containing map as value.
     */
    public FutureTask<Map<String, Object>> getDocumentData(String name) {
        FutureTask<Map<String, Object>> futureTask = new FutureTask<>();
        root.document(name).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult() == null)
                        futureTask.failureListener.onFailed(new Exception("Document " + name + " does not exist."));
                    else
                        futureTask.successListener.onSuccess(task.getResult().getData());
                } else {
                    futureTask.failureListener.onFailed(task.getException());
                }
            }
        });
        return futureTask;
    }
}
