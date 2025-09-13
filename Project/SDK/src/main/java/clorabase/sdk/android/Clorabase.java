package clorabase.sdk.android;

import android.content.Context;

import clorabase.sdk.android.clorastore.ClorastoreCollection;
import clorabase.sdk.android.messaging.ClorabaseInAppMessaging;
import clorabase.sdk.android.storage.ClorabaseStorage;
import clorabase.sdk.android.updates.ClorabaseInAppUpdate;
import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;

/**
 * This is the main class to access the Clorabase. It is the entry point to access the database, storage and graph database.
 * You can only have one instance of this class in your application. It is recommended to use this class as singleton.
 * <p>
 * <b>Example:</b>
 * <pre>
 *         Clorabase clorabase = Clorabase.getInstance("your_token","your_project");
 *         ClorastoreDatabase database = clorabase.getDatabase();
 *         ClorabaseStorage storage = clorabase.getStorage();
 * </pre>
 */
public class Clorabase {
    protected static Clorabase instance;
    protected final clorabase.sdk.java.Clorabase clorabase4j;

    protected Clorabase(String token, String project, String username) throws Exception {
        clorabase4j = clorabase.sdk.java.Clorabase.getInstance(username,token,project);
    }



    /**
     * This method initializes the Clorabase with the provided token and project name.
     * @param token The github auth token with all the necessary permissions. See documentation for more details.
     * @param project The project name you want to access.
     * @return The instance of Clorabase.
     */
    public static Clorabase getInstance(@NotNull String username, @NotNull String token,@NotNull String project) throws Exception {
        if (instance == null) {
            if (username.isBlank() || token.isBlank() || project.isBlank()) {
                throw new IllegalArgumentException("Username, token, and project must not be blank.");
            }

            instance = new Clorabase(token, project, username);
        }
        return instance;
    }

    /**
     * Get the root collection of the database.
     * @return The root collection.
     */
    public ClorastoreCollection getDatabase() {
        var db = clorabase4j.getDatabase();
        return ClorastoreCollection.from(db);
    }

    /**
     * Returns the root node of the storage bucket
     * @return The instance of ClorabaseStorage.
     */
    public ClorabaseStorage getStorage() throws IllegalStateException {
        return ClorabaseStorage.from(clorabase4j.getStorage());
    }

    /**
     * Initialize the In-App messaging service with the given context and channel.
     * @param context The context of the application.
     */
    public void initInAppMessaging(Context context) {
        ClorabaseInAppMessaging.init(context, clorabase4j.getConfig().project);
    }

    /**
     * Initialize the In-App update service with the given context.
     * @param context The context of the application.
     */
    public void initInAppUpdate(Context context) {
        ClorabaseInAppUpdate.init(context, clorabase4j.getConfig().project);
    }
}
