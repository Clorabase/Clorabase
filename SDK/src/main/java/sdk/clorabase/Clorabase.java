package sdk.clorabase;

import android.content.Context;

import sdk.clorabase.clorastore.Collection;
import sdk.clorabase.clorastore.DBUtils;
import sdk.clorabase.clorastore.Document;
import sdk.clorabase.messaging.ClorabaseInAppMessaging;
import sdk.clorabase.push.ClorabasePushMessaging;
import sdk.clorabase.storage.Node;
import sdk.clorabase.updates.ClorabaseInAppUpdate;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public final static Map<String, Document> cachedDocuments = new LinkedHashMap<>(){
        @Override
        protected boolean removeEldestEntry(Entry<String, Document> eldest) {
            return size() > 10 || eldest.getValue().isExpired();
        }
    };

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
                    INSTANCE.repo = INSTANCE.github.getRepository(INSTANCE.USERNAME + "/" + Constants.REPO_NAME);
                    GithubUtils.BASE_URL = String.format(GithubUtils.BASE_URL, INSTANCE.USERNAME);
                } catch (IOException e) {
                    if (e instanceof FileNotFoundException)
                        throw new RuntimeException("Project does not exists. Please verify that the project with name " + project + " exists");
                    else
                        throw new RuntimeException("Failed to initialize Clorabase. Error : " + e.getMessage());
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
     * Get the root collection of the database.
     * @return The root collection.
     */
    public Collection getClorastoreDatabase() {
        return new Collection(null, new DBUtils(repo,USERNAME,PROJECT), PROJECT + "/db","db");
    }

    /**
     * Returns the root node of the storage bucket
     * @return The instance of ClorabaseStorage.
     */
    public Node getClorabaseStorage() {
        try {
            var main = new JSONObject();
            var node = main.optJSONObject("root");
            if (node == null){
                node = new JSONObject();
                main.put("root",node);
            }
            return new Node(repo.getReleaseByTagName(PROJECT),repo,main,node, "root",USERNAME);
        } catch (IOException e) {
            throw new IllegalArgumentException("Storage bucket does not exists. First create a storage bucket from the console");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
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
