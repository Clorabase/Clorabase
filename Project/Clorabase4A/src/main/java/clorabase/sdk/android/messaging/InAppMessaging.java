package clorabase.sdk.android.messaging;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import clorabase.sdk.java.utils.GithubFile;
import clorabase.sdk.java.utils.GithubUtils;

/**
 * InAppMessaging handles the fetching and display of in-app messages.
 * Messages are fetched from the 'messages/' directory of the project in the GitHub repository.
 */
public class InAppMessaging {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static InAppMessaging instance;
    private static Queue<Message> messageQueue;
    private final Context context;
    private final String messagePath;
    private final SharedPreferences pref;
    private OnPrimaryClickListener primaryClickListener = (message, dialog) -> {
        dialog.dismiss();
    };

    private InAppMessaging(@NonNull Context context, @NonNull String project) {
        this.context = context.getApplicationContext();
        this.messagePath = project + "/messages/";
        this.pref = context.getSharedPreferences("inapp", Context.MODE_PRIVATE);
    }

    /**
     * Returns the singleton instance of ClorabaseInAppMessaging.
     *
     * @param context  The context used to initialize the service.
     * @param project  The project identifier.
     * @param autoShow If true, fetches and shows the most recent message immediately.
     * @return The singleton instance.
     */
    public static InAppMessaging getInstance(@NonNull Context context, @NonNull String project, boolean autoShow) {
        if (instance == null) {
            instance = new InAppMessaging(context, project);
        }
        if (autoShow) {
            instance.fetchAndShow(context);
        }
        return instance;
    }

    /**
     * Sets a custom listener for the primary button click.
     * If null, the default behavior is to open the link if present.
     *
     * @param listener The listener to set.
     */
    public void setOnPrimaryClickListener(@NonNull OnPrimaryClickListener listener) {
        this.primaryClickListener = listener;
    }

    /**
     * Fetches all valid pending messages from the repository.
     *
     * @return A Task containing the list of valid messages, sorted by timestamp (newest first).
     */
    public Task<Queue<Message>> fetch() {
        TaskCompletionSource<Queue<Message>> tcs = new TaskCompletionSource<>();
        executor.execute(() -> {
            try {
                List<GithubFile> files = GithubUtils.listFiles(messagePath);
                List<GithubFile> jsonFiles = files.stream()
                        .filter(f -> f.isFile() && f.getName().endsWith(".json"))
                        .collect(Collectors.toList());

                messageQueue = new PriorityQueue<>((message, t1) -> Long.compare(t1.timestamp, message.timestamp));
                long currentTime = System.currentTimeMillis();
                for (GithubFile file : jsonFiles) {
                    try {
                        Map<String, Object> data = GithubUtils.getJsonResponse(file.rawUrl);
                        Message message = new Message(data, file.getSha(), messagePath + file.getName());

                        if (message.showOnce && pref.getBoolean(message.sha, false)) {
                            continue;
                        }

                        if (message.expiry > 0 && currentTime > message.expiry) {
                            continue;
                        }

                        // Fetch image if present
                        if (message.imageUrl != null && !message.imageUrl.isBlank()) {
                            try (InputStream stream = new URL(message.imageUrl).openStream()) {
                                message.bitmap = BitmapFactory.decodeStream(stream);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        messageQueue.add(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                tcs.setResult(messageQueue);
            } catch (Exception e) {
                tcs.setException(e);
            }
        });

        return tcs.getTask();
    }


    /**
     * Fetches the latest message and shows it if found.
     *
     * @param context The context to show the dialog in.
     */
    public void fetchAndShow(@NonNull Context context) {
        fetch().addOnSuccessListener(messages -> {
            if (!messages.isEmpty()){
                show(context);
            }
        }).addOnFailureListener(Throwable::printStackTrace);
    }

    public void show(@Nullable Context context) {
        if (context instanceof Activity activity) {
            if (activity.isFinishing() || activity.isDestroyed()) return;
        }

        Message message = messageQueue.poll();
        if (message != null && context != null) {
            var style = message.style.toUpperCase();
            var isCancelable = style.equals("SIMPLE");

            DialogFactory.DialogConfig config = new DialogFactory.DialogConfig.Builder()
                    .setTitle(message.title)
                    .setMessage(message.content)
                    .setImageBitmap(message.bitmap)
                    .setPrimaryButtonText("OK")
                    .setSecondaryButtonText("Close")
                    .setCancelable(isCancelable)
                    .setAutoDismiss(false)
                    .setDeepLink(message.link)
                    .setListener(new DialogFactory.DialogActionListener() {
                        @Override
                        public void onPrimaryClick(Dialog dialog, String deepLink) {
                            handleMessageAction(message, dialog);
                        }

                        @Override
                        public void onSecondaryClick(Dialog dialog) {
                            dialog.dismiss();
                        }
                    })
                    .build();


            Dialog dialog = switch (style) {
                case "COUPON" -> DialogFactory.createCouponDialog(context, config);
                case "PROMO" -> DialogFactory.createPromoDialog(context, config);
                default -> DialogFactory.createSimpleDialog(context, config);
            };

            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessageAction(@NonNull Message message, @NonNull Dialog dialog) {
        if (message.showOnce) {
            pref.edit().putBoolean(message.sha, true).apply();
        }

        if (primaryClickListener != null) {
            primaryClickListener.onClick(message, dialog);
        } else if (message.link != null && !message.link.isBlank()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(message.link));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
