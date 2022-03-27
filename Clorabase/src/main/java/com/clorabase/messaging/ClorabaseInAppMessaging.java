package com.clorabase.messaging;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.clorabase.DriveHelper;
import com.clorabase.R;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class ClorabaseInAppMessaging {
    private static String type,title,message,image,link,id;
    private static Intent intent;
    private static DriveHelper helper;
    protected ClorabaseInAppMessaging(){}

    /**
     * Initialize the In-App messaging service (Not this {@link android.app.Service}}) with the given context.
     * This class is responsible for listening In-App events and displaying Messages on the screen, So you must call this in application's
     * {@code onCreate()}. Any message sent after calling this method will not be shown. This only checks for the message at the time of initialization and not after that.
     */
    public static void init(@NonNull Context context,@NonNull String token,@NonNull String projectId) {
        try {
            helper = new DriveHelper(token);
            id = helper.getFileId("messages.json",projectId);
            if (id == null){
                throw new IllegalAccessException("Project id is invalid or clorabase messaging is not configured");
            } else {
                JSONObject object = new JSONObject(helper.getContent(id));
                title = object.optString("title");
                message = object.optString("message");
                image = object.optString("image");
                link = object.optString("link");
                if (title != null)
                    showAlertDialog(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showAlertDialog(Context context) {
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.simple_dialog,null);
        View cross = view.findViewById(R.id.close);
        TextView title = view.findViewById(R.id.title);
        ImageView image = view.findViewById(R.id.image);
        TextView message = view.findViewById(R.id.message);
        Button ok = view.findViewById(R.id.ok);

        title.setText(ClorabaseInAppMessaging.title);
        message.setText(ClorabaseInAppMessaging.message);
        try {
            image.setImageBitmap(BitmapFactory.decodeStream(new URL(ClorabaseInAppMessaging.image).openConnection().getInputStream()));
            if (link != null)
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        } catch (IOException e) {
            image.setVisibility(View.GONE);
            e.printStackTrace();
        }
        cross.setOnClickListener(v -> dialog.dismiss());
        ok.setOnClickListener(v -> {
            if (intent != null)
                context.startActivity(intent);
            dialog.dismiss();
            if (!helper.updateFile("",id))
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        });
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setLayout(-1,-2);
        dialog.show();
    }
}
