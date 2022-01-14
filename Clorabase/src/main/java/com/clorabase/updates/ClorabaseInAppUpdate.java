package com.clorabase.updates;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.clorabase.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This class is of App Update feature in Clorabase. Using this feature, you can inform users obout the new update.
 * This is specially for apps that are not on play-store.
 */
public class ClorabaseInAppUpdate {

    protected ClorabaseInAppUpdate(){}

    /**
     * This will start the flow of checking and updating the app. This should be called as soon the activity starts.
     * @param context The context of the activity.
     * @param link The link to the new update.
     */
    public static void init(@NonNull Context context,@NonNull String link){
        FirebaseApp app = FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("1:1084008315464:android:7de49d002d21c79928767d")
                .setProjectId("clora-base")
                .setApiKey("AIzaSyAw__oC4tCQDcuP-4-ZZPS3ObsI-SiQOGo")
                .build());
        try {
            int current = context.getPackageManager().getPackageInfo(context.getPackageName(),PackageManager.GET_META_DATA).versionCode;
            FirebaseFirestore.getInstance(app).document("Updates/versions").get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    Integer latest = documentSnapshot.get(context.getPackageName().replace('.','_'),Integer.class);
                    if (latest == null)
                        return;

                    String url = documentSnapshot.get("link",String.class);
                    String mode = documentSnapshot.get("mode",String.class);
                    String note = documentSnapshot.get("note",String.class);

                    if (latest > current)
                        startUpdateFlow(context,AppUpdateMode.FLEXIBLE,link,"note");
                }
            });
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }


    private static void startUpdateFlow(Context context, String mode,String link,String note) {
        if (mode.equals(AppUpdateMode.FLEXIBLE)) {
            new AlertDialog.Builder(context)
                    .setTitle("Update available")
                    .setMessage("A new version is available. This new version has few bug fixes. Update now to get better experience")
                    .setPositiveButton("update", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                        }
                    }).setNegativeButton("later", null).show();
        }
//            else if (mode.equals(AppUpdateMode.STRICT)){
//            CharSequence name = context.getApplicationInfo().loadLabel(context.getPackageManager());
//            View view = LayoutInflater.from(context).inflate(R.layout.update_dialog,null);
//            TextView title = view.findViewById(R.id.title);
//            TextView description = view.findViewById(R.id.description);
//            Button download= view.findViewById(R.id.download);
//            title.setText(name + " needs an update !");
//            description.append("\nUpdate notes :-\n" + note);
//            download.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
//                }
//            });
//
//            Dialog dialog = new Dialog(context);
//            dialog.setContentView(view);
//            dialog.setCancelable(false);
//            dialog.setCanceledOnTouchOutside(false);
//            dialog.getWindow().setLayout(-1,-2);
//            dialog.show();
//        } else if ("immediate".equals(mode)){
//            CharSequence name = context.getApplicationInfo().loadLabel(context.getPackageManager());
//            View view = LayoutInflater.from(context).inflate(R.layout.update_dialog,null);
//            TextView title = view.findViewById(R.id.title);
//            TextView description = view.findViewById(R.id.description);
//            ProgressBar progressBar = view.findViewById(R.id.progressBar);
//            Button download= view.findViewById(R.id.download);
//            title.setText(name + " needs an update !");
//            description.append("\nUpdate notes :-\n" + note);
//            download.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    v.setVisibility(View.GONE);
//                    progressBar.setVisibility(View.VISIBLE);
//
//                    try {
//                        URLConnection link = new URL(link).openConnection();
//                        InputStream in = link.getInputStream();
//                        FileOutputStream out = new FileOutputStream(new File(context.getExternalCacheDir(),"update.apk"));
//                        progressBar.setMax(in.available());
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                byte[] bytes = new byte[1024];
//                                int read;
//                                int progress = 0;
//                                while (true) {
//                                    try {
//                                        if ((read = in.read(bytes)) == -1){
//                                            out.write(bytes,0,read);
//                                            int finalProgress = progress + read;
//                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    progressBar.setProgress(finalProgress);
//                                                }
//                                            });
//                                        }
//
//                                        context.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(new File(context.getExternalCacheDir(),"update.apk")),"application/vnd.android.package-archive"));
//                                        ((Activity) context).finish();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                        Toast.makeText(context, "Error downloading", Toast.LENGTH_LONG).show();
//                                        description.setText("There was a problem downloading update");
//                                        description.setTextColor(Color.RED);
//                                    }
//                                }
//                            }
//                        }).start();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Toast.makeText(context, "Error downloading", Toast.LENGTH_LONG).show();
//                        description.setText("There was a problem downloading update");
//                        description.setTextColor(Color.RED);
//                    }
//                }
//            });
//
//            Dialog dialog = new Dialog(context);
//            dialog.setContentView(view);
//            dialog.setCancelable(false);
//        } else
//            throw new IllegalArgumentException("Invalid update mode : " + mode);
    }
}
