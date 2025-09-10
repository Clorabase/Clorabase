package clorabase.sdk.java.storage;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import clorabase.sdk.java.utils.GithubFile;
import clorabase.sdk.java.utils.GithubUtils;
import clorabase.sdk.java.Reason;

/**
 * Represents a directory in the Clorabase storage system. It is just like your traditional
 * directory in a file system, but it is used to organize and manage files within the Clorabase storage.
 */
public class ClorabaseStorage {
    public String path;
    public final String name;
    public final String storageTag;

    /**
     * Constructs a Directory object with the specified path and name.
     *
     * @param path the path of the directory
     * @param name the name of the directory
     */
    public ClorabaseStorage(String path, String name, String project) {
        this.path = path;
        this.name = name;
        this.storageTag = project;
    }

    /**
     * Goes into a subdirectory of this directory or creates a new one if it does not exist.
     * @param name the name of the subdirectory. Eg "images", "documents", etc.
     * @return a new ClorabaseStorage instance representing the subdirectory
     */
    public ClorabaseStorage directory(String name) {
        return new ClorabaseStorage(this.path + "/" + name, name, storageTag);
    }

    /**
     * Adds a file to this directory.
     *
     * @param contents the contents of the file as a byte array
     * @param fileName the name of the file to be added
     * @throws StorageException if the file already exists or if there is an error adding the file
     */
    public void addFile(byte[] contents, String fileName) throws StorageException {
        String filePath = this.path + "/" + fileName;
        try {
            GithubUtils.create(contents, filePath);
        } catch (IOException e) {
            var msg = e.getMessage();
            if (msg != null && msg.contains("\"status\":\"422\"")) {
                throw new StorageException("File already exists: " + filePath, Reason.File_ALREADY_EXISTS);
            } else
                throw new StorageException("Failed to add file to directory: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads a blob (binary large object) to this directory. This consumes 2 API calls:
     * 1. Uploading the blob to the release assets on GitHub.
     * 2. Creating a pointer file in the Clorabase storage to reference the blob.
     *
     * @param io       the InputStream containing the blob data
     * @param fileName the name of the file to be uploaded
     * @param listener a ProgressListener to track upload progress
     */
    public void uploadBlob(@NotNull InputStream io, @NotNull String fileName, @NotNull ProgressListener listener) {
        String filePath = this.path + "/" + fileName + ".ptr";
        JSONObject asset;
        try {
            if (GithubUtils.assetExists(fileName, storageTag)){
                listener.onError(new StorageException("BLOB already exists: " + filePath, Reason.File_ALREADY_EXISTS));
                return;
            }

            asset = GithubUtils.uploadAsset(io,fileName, storageTag,listener);
            if (asset != null){
                var json = new JSONObject();
                json.put("url", asset.getString("browser_download_url"));
                json.put("size",asset.getLong("size"));
                json.put("timestamp", System.currentTimeMillis());
                json.put("id",asset.getInt("id"));
                GithubUtils.create(json.toString().getBytes(), filePath);
                listener.onComplete();
            }
        } catch (IOException e) {
            if (e.getMessage().contains("\"code\":\"already_exists\""))
                listener.onError(new StorageException("File already exists: " + filePath, Reason.File_ALREADY_EXISTS));
            else
                listener.onError(new StorageException("Failed to upload file: " + e.getMessage(), e));
        }
    }

    /**
     * Retrieves an InputStream for a blob stored in this directory.
     *
     * @param name the name of the blob file (with the .ptr extension)
     * @return an InputStream to read the blob data
     * @throws StorageException if the blob does not exist or if there is an error retrieving it
     */
    public InputStream getBlobStream(String name) throws StorageException {
        if (!name.endsWith(".ptr"))
            throw new IllegalArgumentException("Given file does not appear to be a blob. (.ptr extension is missing)");

        String filePath = this.path + "/" + name;
        try {
            var content = new String(GithubUtils.getRaw(filePath));
            var json = new JSONObject(content);
            String url = json.getString("url");
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/octet-stream");
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new StorageException("Failed to download file: " + connection.getResponseMessage(), Reason.NOT_EXISTS);
            } else
                return connection.getInputStream();
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new StorageException("File not found: " + filePath, Reason.NOT_EXISTS);
            } else {
                throw new StorageException("Failed to retrieve download stream: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Retrieves download url for this blob
     *
     * @param name the name of the blob file (with the .ptr extension)
     * @return an InputStream to read the blob data
     * @throws StorageException if the blob does not exist or if there is an error retrieving it
     */
    public URL getBlobDownloadURL(String name) throws StorageException{
        if (!name.endsWith(".ptr"))
            throw new IllegalArgumentException("Given file does not appear to be a blob. (.ptr extension is missing)");

        String filePath = this.path + "/" + name;
        try {
            var content = new String(GithubUtils.getRaw(filePath));
            var json = new JSONObject(content);
            String url = json.getString("url");
            return new URL(url);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new StorageException("File not found: " + filePath, Reason.NOT_EXISTS);
            } else {
                throw new StorageException("Failed to retrieve download stream: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Deletes a file from this directory.
     *
     * @param name the name of the file to be deleted
     * @throws StorageException if the file does not exist or if there is an error deleting it
     */
    public void delete(String name) throws StorageException {
        String filePath = this.path + "/" + name;
        try {
            GithubUtils.delete(filePath);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new StorageException("File not found: " + filePath, Reason.NOT_EXISTS);
            } else {
                throw new StorageException("Failed to delete file: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Deletes a blob from this directory.
     *
     * @param name the name of the blob file (without the .ptr extension) to be deleted
     * @throws StorageException if the blob does not exist or if there is an error deleting it
     */
    public void deleteBlob(String name) throws StorageException {
        try {
            var path = this.path + "/" + name + ".ptr";
            var ptr = GithubUtils.getRaw(path);
            var json = new JSONObject(new String(ptr));
            var id = json.getInt("id");
            GithubUtils.deleteAsset(id);
            GithubUtils.delete(path);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new StorageException("Blob file not found: " + name, Reason.NOT_EXISTS);
            } else {
                throw new StorageException("Failed to delete blob: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Retrieves the contents of a file stored in this directory.
     *
     * @param name the name of the file to be retrieved
     * @return the contents of the file as a byte array
     * @throws StorageException if the file does not exist or if there is an error retrieving it
     */
    public byte[] getFile(String name) throws StorageException {
        String filePath = this.path + "/" + name;
        try {
            return GithubUtils.getRaw(filePath);
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                throw new StorageException("File not found: " + filePath, Reason.NOT_EXISTS);
            } else {
                throw new StorageException("Failed to retrieve file: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Lists all files in this directory.
     *
     * @return a list of file names in the directory
     * @throws StorageException if there is an error listing the files
     */
    public List<String> listFiles() throws StorageException {
        try {
            return GithubUtils.listFiles(this.path)
                    .stream()
                    .map(GithubFile::getName)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new StorageException("Failed to list files in directory: " + e.getMessage(), e);
        }
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getProject() {
        return storageTag;
    }
}
