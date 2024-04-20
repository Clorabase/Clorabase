package com.clorabase;

import android.content.Context;

import com.clorabase.clorastore.ClorastoreDatabase;
import com.clorabase.clorograph.ClorographDatabase;
import com.clorabase.messaging.ClorabaseInAppMessaging;
import com.clorabase.push.ClorabasePushMessaging;
import com.clorabase.storage.ClorabaseStorage;
import com.clorabase.updates.ClorabaseInAppUpdate;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;

/**
 * This is the main class to access the Clorabase. It is the entry point to access the database, storage and graph database.
 * You can only have one instance of this class in your application. It is recommended to use this class as singleton.
 * <p>
 * <b>Example:</b>
 * <pre>
 *         Clorabase clorabase = Clorabase.getInstance("your_token","your_project");
 *         ClorastoreDatabase database = clorabase.getDatabase();
 *         ClorabaseStorage storage = clorabase.getStorage();
 *         ClorographDatabase graphDatabase = clorabase.getGraphDatabase(context);
 * </pre>
 */
public class Clorabase {
    protected static Clorabase INSTANCE;
    protected GitHub github;
    protected GHRepository repo;
    protected String PROJECT;
    protected String USERNAME;

    /**
     * This method initializes the Clorabase with the provided token and project name.
     * @param token The github auth token with all the necessary permissions. See documentation for more details.
     * @param project The project name you want to access.
     * @return The instance of Clorabase.
     */
    public static Clorabase getInstance(String token, String project) {
        if (INSTANCE == null) {
            var thread = new Thread(() -> {
                try {
                    INSTANCE = new Clorabase();
                    INSTANCE.PROJECT = project;
                    INSTANCE.github = GitHub.connectUsingOAuth(token);
                    INSTANCE.USERNAME = INSTANCE.github.getMyself().getLogin();
                    INSTANCE.repo = INSTANCE.github.getRepository(INSTANCE.USERNAME + "/Clorabase-databases");
                    GithubUtils.BASE_URL = String.format(GithubUtils.BASE_URL, INSTANCE.USERNAME);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to initialize Clorabase. Please check your token or internet connection.");
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return INSTANCE;
    }

    /**
     * Get the clorastore database of the project. This is also singleton.
     * @return The instance of ClorastoreDatabase.
     */
    public ClorastoreDatabase getClorastoreDatabase() {
        return ClorastoreDatabase.getInstance(repo, PROJECT,USERNAME);
    }

    /**
     * Get the storage bucket of the project.
     * @return The instance of ClorabaseStorage.
     */
    public ClorabaseStorage getStorage() {
        try {
            return new ClorabaseStorage(PROJECT, USERNAME, repo.getReleaseByTagName(PROJECT));
        } catch (IOException e) {
            throw new IllegalArgumentException("Storage bucket does not exists. First create a storage bucket from the console");
        }
    }

    /**
     * Get the clorograph database of the project. This is also singleton.
     * @param context The context of the application.
     * @return The instance of ClorographDatabase.
     */
    public ClorographDatabase getGraphDatabase(Context context) {
        return ClorographDatabase.getInstance(repo, PROJECT, context);
    }

    /**
     * Initialize the In-App messaging service with the given context and channel.
     * @param context The context of the application.
     * @param channel The channel name to listen for messages.
     */
    public void initInAppMessaging(Context context, String channel) {
        ClorabaseInAppMessaging.init(context, PROJECT, channel, repo);
    }

    /**
     * Initialize the push notification service with the given context and tag.
     * @param context The context of the application.
     * @param tag The tag to identify the user.
     * @param callback The callback to be called when a notification is clicked.
     */
    public void initPushMessaging(Context context, String tag, ClorabasePushMessaging.OnNotificationClicked callback) {
        ClorabasePushMessaging.init(context, tag, callback);
    }

    /**
     * Initialize the In-App update service with the given context.
     * @param context The context of the application.
     */
    public void initInAppUpdate(Context context) {
        ClorabaseInAppUpdate.init(context, PROJECT);
    }
}
