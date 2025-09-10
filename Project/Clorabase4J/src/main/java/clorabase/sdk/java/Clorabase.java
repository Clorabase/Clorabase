package clorabase.sdk.java;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.IOException;

import clorabase.sdk.java.database.Collection;
import clorabase.sdk.java.database.SecurityProvider;
import clorabase.sdk.java.storage.ClorabaseStorage;
import clorabase.sdk.java.utils.Constants;
import clorabase.sdk.java.utils.GithubUtils;

/**
 * Clorabase SDK for standalone Java. This SDK only includes database and storage functionality.
 * It does not include any other features such as AI, web, or mobile.
 * <p>
 * This class is the entry point for the Clorabase SDK. You can use it to access the database and storage functionality.
 *
 * @author Rahil Khan
 * @version 0.4
 */
public class Clorabase {
    private static Clorabase instance;
    private final String project;
    private final Config config;

    protected Clorabase(String token, String project, String username) throws Exception {
        this.project = project;

        try {
            GithubUtils.init(token, username);
            SecurityProvider.init(token);

            byte[] bytes = GithubUtils.getRaw(project + "/" + "config.json");
            var json = new JSONObject(new String(bytes));
            this.config = new Config();
            this.config.project = json.getString("project");
            this.config.username = username;
            this.config.version = json.getString("version");
            this.config.isStorageConfigured = json.optBoolean("isStorageConfigured", false);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("Project not configured or does not exist: " + project, e);
        } catch (JSONException e) {
            throw new RuntimeException("Invalid config file format: " + e.getMessage());
        }
    }

    /**
     * Returns the singleton instance of Clorabase.
     *
     * @param username The username of the Clorabase account.
     * @param token    The access token for the Clorabase account.
     * @param project  The project name.
     * @return The singleton instance of Clorabase.
     * @throws Exception If the instance is not initialized or if any parameter is invalid.
     */
    public static Clorabase getInstance(@NotNull String username, @NotNull String token, @NotNull String project) throws Exception {
        if (instance == null || !instance.project.equals(project)) {
            if (username.isBlank() || token.isBlank() || project.isBlank()) {
                throw new IllegalArgumentException("Username, token, and project must not be blank.");
            } else
                instance = new Clorabase(token, project, username);
        }
        return instance;
    }

    /**
     * Returns the project configuration file "config.json".
     * @return The project configuration.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Returns root collection of the database.
     * @return Collection representing the root of the database.
     */
    public Collection getDatabase() {
        return new Collection(null, project + "/db","");
    }

    /**
     * Returns the storage instance for the project.
     * @return ClorabaseStorage instance for the project.
     * @throws IllegalStateException if storage is not configured for the project.
     */
    public ClorabaseStorage getStorage() throws IllegalStateException {
        if (config.isStorageConfigured)
            return new ClorabaseStorage(project + "/storage", "",project);
        else
            throw new IllegalStateException("Storage is not configured for this project: " + project);
    }

    public Quota getAPIQuota() throws IOException {
        var rl = GithubUtils.gitHub.getRateLimit().getCore();
        return new Quota(rl.getRemaining(), rl.getResetEpochSeconds());
    }
}