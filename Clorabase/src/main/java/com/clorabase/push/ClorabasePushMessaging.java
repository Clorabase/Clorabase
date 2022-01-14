package com.clorabase.push;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.onesignal.OSNotificationOpenedResult;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClorabasePushMessaging {

    /**
     * Initialize the push notification service. This must be called in your Application's <code>onCreate()</code>
     * @param context Applications context.
     */
    public static void init(@NonNull Context context) {
        OneSignal.initWithContext(context);
        OneSignal.setAppId("eb9c7891-3211-4a18-9317-6316897e837f");
        OneSignal.setExternalUserId(context.getPackageName());
    }


    private static Map<String, Object> toMap(JSONObject json) {
        Map<String,Object> map = new HashMap<>();
        Iterator<String> iterator = json.keys();
        while (iterator.hasNext()) {
            try {
                map.put(iterator.next(),json.get(iterator.next()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return map;
    }


    public interface OnNotificationClicked {
        void onClick(Map<String,Object> data);
    }
}
