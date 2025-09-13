package clorabase.sdk.android.messaging;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import clorabase.sdk.android.R;
import clorabase.sdk.java.utils.GithubUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * ClorabaseInAppMessaging is a class that handles the in-app messaging.
 */
public class ClorabaseInAppMessaging {
    private static String type, title, message, image, link, id;
    private static Bitmap bitmap;


    /**
     * Initialize the In-App messaging service (Not this {@link android.app.Service}}) with the given context.
     * This class is responsible for listening In-App events and displaying Messages on the screen, So you must call this in application's
     * {@code onCreate()}. Any message sent after calling this method will not be shown. This only checks for the message at the time of initialization and not after that.
     */
    public static void init(@NonNull Context context, @NonNull String project) {
        var path = project + "/messages/";

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                var msg = GithubUtils.listFiles(path).get(0);
                var file = msg.rawUrl;
                var object = GithubUtils.getJsonResponse(file);
                type = object.getString("type");
                title = object.getString("title");
                message = object.getString("message");
                image = object.optString("image");
                link = object.optString("link");
                id = object.optString("id");
                var decBytes = Base64.decode(image,Base64.DEFAULT);
                bitmap = BitmapFactory.decodeByteArray(decBytes, 0, decBytes.length);
                context.getMainExecutor().execute(() -> showAlertDialog(context,path + msg.getName()));
            } catch (IOException | JSONException e) {
                throw new RuntimeException(e);
            } catch (IndexOutOfBoundsException e){

            }
        });
    }

    private static void showAlertDialog(Context context,String path) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.simple_dialoge, null);
        View cross = view.findViewById(R.id.close);
        TextView title = view.findViewById(R.id.title);
        ImageView image = view.findViewById(R.id.image);
        TextView message = view.findViewById(R.id.message);
        Button ok = view.findViewById(R.id.ok);

        title.setText(ClorabaseInAppMessaging.title);
        message.setText(ClorabaseInAppMessaging.message);
        cross.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setLayout(-1, -2);
        dialog.show();

        try {
            image.setImageBitmap(bitmap);
        } catch (Exception e) {
            image.setVisibility(View.GONE);
            e.printStackTrace();
        }

        ok.setOnClickListener(v -> {
            dialog.dismiss();
            new Thread(() -> {
                try {
                    if (link != null && !link.isBlank())
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));

                    GithubUtils.delete(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
}
