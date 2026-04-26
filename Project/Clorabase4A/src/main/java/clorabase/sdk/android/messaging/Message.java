package clorabase.sdk.android.messaging;

import android.app.Dialog;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Internal data model for an In-App message.
 */
public class Message {
    final String sha;
    final String title;
    final String content;
    final String imageUrl;
    final String link;
    final long timestamp;
    final long expiry;
    final boolean showOnce;
    final String style;
    final String githubPath;
    @Nullable
    Bitmap bitmap;

    protected Message(Map<String, Object> map, String sha, String githubPath) {
        this.sha = sha;
        this.githubPath = githubPath;
        this.title = String.valueOf(map.getOrDefault("title", ""));
        this.content = String.valueOf(map.getOrDefault("message", ""));
        this.imageUrl = (String) map.get("image");
        this.link = (String) map.get("link");
        this.timestamp = parseLong(map.get("timestamp"));
        this.expiry = parseLong(map.get("expiry"));
        this.showOnce = parseBoolean(map.get("show_once"));
        this.style = (String) map.getOrDefault("style", "SIMPLE");
    }

    private long parseLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private boolean parseBoolean(Object o) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }
}

/**
 * Listener for the primary action on a message.
 */
interface OnPrimaryClickListener {
    void onClick(Message message, Dialog dialog);
}