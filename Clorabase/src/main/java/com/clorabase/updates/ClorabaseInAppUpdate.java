package com.clorabase.updates;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.clorabase.GithubUtils;

import org.json.JSONException;

/**
 * This class is of App Update feature in Clorabase. Using this feature, you can inform users obout the new update.
 * This is specially for apps that are not on play-store.
 */
public class ClorabaseInAppUpdate {

    private static final String FLEXIBLE = "flexible";
    private static final String IMMEDIATE = "immediate";


    /**
     * This will start the flow of checking and updating the app. Call this when you want to check for update
     *
     * @param context   The context of the activity.
     */
    public static void init(@NonNull Context context, @NonNull String project) {
        GithubUtils.getFileAsJSON(project + "/updates/" + context.getPackageName() + ".json", json -> {
            try {
                int versionCode = json.getInt("versionCode");
                String link = json.getString("link");
                String mode = json.getString("mode");

                int currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).versionCode;
                if (versionCode > currentVersion)
                    ((Activity) context).runOnUiThread(() -> startUpdateFlow(context, mode, link));
            } catch (PackageManager.NameNotFoundException | JSONException e) {
                e.printStackTrace();
            }
        });
    }


    private static void startUpdateFlow(Context context, String mode, String link) {
        if (mode.equals(FLEXIBLE)) {
            new AlertDialog.Builder(context)
                    .setTitle("Update available")
                    .setMessage("A new version is available. This new version has few bug fixes. Update now to get better experience")
                    .setPositiveButton("update", (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))))
                    .setNegativeButton("later", null).
                    show();
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
