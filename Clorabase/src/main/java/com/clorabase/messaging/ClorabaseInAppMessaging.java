package com.clorabase.messaging;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.clorabase.Constants;
import com.clorabase.R;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import apis.xcoder.easydrive.EasyDrive;

/**
 * ClorabaseInAppMessaging is a class that handles the in-app messaging.
 */
public class ClorabaseInAppMessaging {
    private static String type, title, message, image, link, id;
    private static Bitmap bitmap;
    private static Intent intent;
    private static EasyDrive drive;


    /**
     * Initialize the In-App messaging service (Not this {@link android.app.Service}}) with the given context.
     * This class is responsible for listening In-App events and displaying Messages on the screen, So you must call this in application's
     * {@code onCreate()}. Any message sent after calling this method will not be shown. This only checks for the message at the time of initialization and not after that.
     */
    public static void init(@NonNull Context context, @NonNull String token, @NonNull String messagingId) {
        try {
            drive = new EasyDrive(Constants.CLIENT_ID, Constants.CLIENT_SECRET, token);
            drive.getContent(messagingId).setOnSuccessCallback(content -> {
                try {
                    JSONObject object = new JSONObject(new String(content));
                    title = object.optString("title");
                    message = object.optString("message");
                    image = object.optString("image");
                    link = object.optString("link");
                    if (image !=  null)
                        bitmap = BitmapFactory.decodeStream(new URL(image).openConnection().getInputStream());

                    System.out.println(object.toString(3));
                    if (title != null)
                        ((Activity) context).runOnUiThread(() -> showAlertDialog(context));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showAlertDialog(Context context) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.simple_dialog, null);
        View cross = view.findViewById(R.id.close);
        TextView title = view.findViewById(R.id.title);
        ImageView image = view.findViewById(R.id.image);
        TextView message = view.findViewById(R.id.message);
        Button ok = view.findViewById(R.id.ok);

        title.setText(ClorabaseInAppMessaging.title);
        message.setText(ClorabaseInAppMessaging.message);
        try {
            image.setImageBitmap(bitmap);
            if (link != null)
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        } catch (Exception e) {
            image.setVisibility(View.GONE);
            e.printStackTrace();
        }
        cross.setOnClickListener(v -> dialog.dismiss());
        ok.setOnClickListener(v -> {
            dialog.dismiss();
            drive.delete(id);
            if (intent != null)
                context.startActivity(intent);
        });
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setLayout(-1, -2);
        dialog.show();
    }
}
