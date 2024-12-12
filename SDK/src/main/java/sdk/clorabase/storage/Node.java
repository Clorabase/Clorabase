package sdk.clorabase.storage;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Node {
    private final GHRelease release;
    private final JSONObject root;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final JSONObject node;
    private final String username;
    private final String path;
    private final GHRepository repo;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public Node(GHRelease release,GHRepository repo,JSONObject root, JSONObject node, String path,String username) {
        this.release = release;
        this.root = root;
        this.node = node;
        this.path = path;
        this.username = username;
        this.repo = repo;
    }

    public void download(@NonNull String filename, @NonNull OutputStream os, @NonNull ClorabaseStorageCallback callback) {
        executor.submit(() -> {
            try {
                var link = node.getString(filename);
                var connection = new URL(link).openConnection();
                var in = new ProgressInputStream(connection.getInputStream(), connection.getContentLength(), callback);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.close();
                in.close();
                handler.post(callback::onComplete);
            } catch (JSONException | IOException e) {
                throw new UncheckedIOException(new FileNotFoundException("No such file found at this node with filename " + filename));
            }
        });
    }

    public void upload(@NonNull String filename, @NonNull InputStream in, @NonNull ClorabaseStorageCallback callback) {
        executor.submit(() -> {
            InputStream pin;
            try {
                if (release == null) {
                    throw new RuntimeException("Storage bucket not configured for this project.");
                } else {
                    var newFilename = path.replace('/', '_') + '_' + filename;
                    var asset = release.uploadAsset(newFilename, pin = new ProgressInputStream(in, in.available(), callback), "application/octet-stream");
                    node.put(filename, asset.getBrowserDownloadUrl());
                    repo.getFileContent(username + "/storage/structure.json").update(root.toString().getBytes(),"Updated using sdk");
                }
                pin.close();
                handler.post(callback::onComplete);
            } catch (IOException e) {
                handler.post(() -> {
                    if (Objects.requireNonNull(e.getMessage()).contains("already_exists"))
                        callback.onFailed(new RuntimeException("A file with this name already exists"));
                });
            } catch (JSONException e) {
                throw new RuntimeException(e); // Never happening!
            }
        });
    }

    public void delete(@NonNull String filename, @NonNull ClorabaseStorageCallback callback) {
        executor.submit(() -> {
            try {
                if (release == null) {
                    throw new RuntimeException("Storage bucket not configured for this project.");
                } else {
                    var actualFilename = path.replace('/', '_') + '_' + filename;
                    for (GHAsset asset : release.listAssets()) {
                        if (asset.getName().equals(actualFilename)) {
                            asset.delete();
                            handler.post(callback::onComplete);
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                handler.post(() -> callback.onFailed(e));
            }
        });
    }

    /**
     * List all the files that resides in this node
     *
     * @return A list of all the filenames in this node
     */
    public List<String> list() {
        Iterator<String> iterator = node.keys();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false).collect(Collectors.toList());
    }

    /**
     * Get into a sub node
     *
     * @param name The name of the node
     * @return the node
     */
    public Node node(String name) {
        try {
            return new Node(release,repo, root, node.getJSONObject(path), path + "/" + name,username);
        } catch (JSONException e) {
            throw new UncheckedIOException(new FileNotFoundException());
        }
    }

    public static void putJson(String path,String key, String value,JSONObject obj) throws JSONException {
        JSONObject current = obj;
        var paths = path.split("/");
        for (String path1 : paths) {
            if (!current.has(path1)){
                current.put(path1, new JSONObject());
            }
            current = current.getJSONObject(path1);

        }

        current.put(key,value);
    }
}
