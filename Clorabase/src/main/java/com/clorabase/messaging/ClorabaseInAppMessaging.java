package com.clorabase.messaging;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.clorabase.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class ClorabaseInAppMessaging {
    private static String type,title,message,image,link;
    private static Intent intent;
    protected ClorabaseInAppMessaging(){}

    /**
     * Initialize the In-App messaging service (Not this {@link android.app.Service}}) with the given context.
     * This class is responsible for listening In-App events and displaying Messages on the screen, So you must call this in application's
     * {@code onCreate()}. Any message sent after calling this method will not be shown. This only checks for the message at the time of initialization and not after that.
     */
    public static void init(Context context) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());
        FirebaseApp app = FirebaseApp.initializeApp(context, new FirebaseOptions.Builder()
                .setApplicationId("1:1084008315464:android:7de49d002d21c79928767d")
                .setProjectId("clora-base")
                .setApiKey("AIzaSyAw__oC4tCQDcuP-4-ZZPS3ObsI-SiQOGo")
                .build());
        CollectionReference db = FirebaseFirestore.getInstance(app).collection("Messages");
        db.document(context.getPackageName()).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String,String> map = (Map) documentSnapshot.getData();
                if (map != null){
                    type = map.get("type");
                    title = map.get("title");
                    message = map.get("message");
                    image = map.get("image");
                    link = map.get("link");
                    assert type != null;
                    if (type.equals("simple"))
                        showAlertDialog(context);

                    db.document(context.getPackageName()).delete();
                }
            }
        });
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
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        } catch (IOException e) {
            image.setVisibility(View.GONE);
            e.printStackTrace();
        }
        cross.setOnClickListener(v -> dialog.dismiss());
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (intent != null)
                    context.startActivity(intent);
                dialog.dismiss();
            }
        });
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setLayout(-1,-2);
        dialog.show();
    }
}
