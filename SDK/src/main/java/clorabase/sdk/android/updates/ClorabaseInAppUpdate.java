package clorabase.sdk.android.updates;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;

import clorabase.sdk.java.utils.GithubUtils;


/**
 * This class is of App Update feature in Clorabase. Using this feature, you can inform users obout the new update.
 * This is specially for apps that are not on play-store.
 */
public class ClorabaseInAppUpdate {

    private static final String FLEXIBLE = "flexible";
    private static final String IMMEDIATE = "urgent";


    /**
     * This will start the flow of checking and updating the app. Call this when you want to check for update
     *
     * @param context   The context of the activity.
     */
    public static void init(@NonNull Context context, @NonNull String project) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                var raw = GithubUtils.getRaw(project + "/updates/" + context.getPackageName() + "/version.json");
                var json = new JSONObject(new String(raw));
                int versionCode = json.getInt("code");
                String link = json.getString("downloadUrl");
                String mode = json.getString("mode");

                int currentVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).versionCode;
                if (versionCode > currentVersion)
                    ((Activity) context).runOnUiThread(() -> startUpdateFlow(context, mode, link));

            } catch (IOException | JSONException | PackageManager.NameNotFoundException e) {
                context.getMainExecutor().execute(() -> {
                    Toast.makeText(context, "Something went extremely wrong in-app update", Toast.LENGTH_LONG).show();
                });
            }
        });
    }


    private static void startUpdateFlow(Context context, String mode, String link) {
        if (mode.equals(FLEXIBLE)) {
            new AlertDialog.Builder(context)
                    .setTitle("Update available")
                    .setMessage("A new version is available. Update now to get better and uninterrupted experience")
                    .setPositiveButton("update", (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))))
                    .setNegativeButton("later", null)
                    .setCancelable(true).
                    show();
        } else if (mode.equals(IMMEDIATE)) {
            new AlertDialog.Builder(context)
                    .setTitle("Update needed")
                    .setMessage("This version of the application has expired and is no longer working. Please update to the latest version to continue using the app.")
                    .setPositiveButton("update", (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))))
                    .setNegativeButton("exit", (dialogInterface, i) -> ((Activity) context).finish())
                    .setCancelable(false)
                    .show();
        }
    }
}
