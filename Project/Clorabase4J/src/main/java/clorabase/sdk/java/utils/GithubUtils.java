package clorabase.sdk.java.utils;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import clorabase.sdk.java.storage.ProgressListener;


/**
 * Utility class for interacting with GitHub repositories.
 */
public class GithubUtils {
    public static GHRepository repo;
    public static GitHub gitHub;
    public static String username;
    public static String token;

    /**
     * Initializes the GitHub connection with the provided token and username. This also checks if the repository exists or not
     * @param token GitHub OAuth token
     * @param user GitHub username
     * @throws Exception if repository does not exist or if there is an error connecting to GitHub
     */
    public static void init(String token,String user) throws Exception {
        try {
            GithubUtils.token = token;
            Constants.init(user);
            gitHub = GitHub.connectUsingOAuth(token);
            username = user;
            repo = gitHub.getRepository(username + "/" + "Clorabase-projects");
        } catch (IOException e){
            try {
                var json = new JSONObject(e.getMessage());
                if (json.has("message")) {
                    throw new Exception(json.getString("message"));
                } else {
                    throw new IOException("An error occurred while connecting to the repository.");
                }
            } catch (JSONException jsonException) {
                throw new IOException("An error occurred while connecting to the repository: " + e.getMessage());
            }
        }
    }

    /**
     * Creates a new file in the repository with the specified contents and path.
     * @param contents the contents of the file as a byte array
     * @param path the path where the file should be created in the repository
     * @return the SHA of the commit that created the file
     * @throws IOException if there is an error creating the file
     */
    public static String create(byte[] contents, String path) throws IOException {
        return repo.createContent()
                .content(contents)
                .path(path)
                .message("Created through SDK")
                .commit()
                .getCommit()
                .getSha();
    }

    /**
     * Updates an existing file in the repository with the specified contents and path.
     * @param contents the new contents of the file as a byte array
     * @param path the path of the file to be updated in the repository
     * @return the SHA of the commit that updated the file
     * @throws IOException if there is an error updating the file
     */
    public static String update(byte[] contents, String path) throws IOException {
        return repo.getFileContent(path).update(contents, "Updated through SDK").getCommit().getSha();
    }

    /**
     * Deletes a file from the repository at the specified path.
     * @param path the path of the file to be deleted in the repository
     * @throws IOException if there is an error deleting the file
     */
    public static void delete(String path) throws IOException {
        repo.getFileContent(path).delete("Deleted through SDK");
    }

    /**
     * Checks if a file exists in the repository at the specified path.
     * @param path the path of the file to check
     * @return true if the file exists, false otherwise
     */
    public static boolean exists(String path) {
        try {
            var connection = (HttpURLConnection) new URL(Constants.BASE_RAW_URL + "/main/" + path).openConnection();
            return connection.getResponseCode() != 404;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Fetches the raw contents of a file from the repository at the specified path.
     * @param path the path of the file to fetch
     * @return the raw contents of the file as a byte array
     * @throws IOException if there is an error fetching the file
     */
    public static byte[] getRaw(String path) throws IOException {
        return fetchBytes(Constants.BASE_RAW_URL + "/main/" + path);
    }

    /**
     * Fetches the latest updated raw contents of a file from the repository at the specified path and commit.
     * @param path the path of the file to fetch
     * @param commit the commit SHA to fetch the file from
     * @return the raw contents of the file as a byte array
     * @throws IOException if there is an error fetching the file
     */
    public static byte[] getImmediateRaw(String path,String commit) throws IOException {
        var url =  Constants.BASE_RAW_URL + commit + "/" + path;
        return fetchBytes(url);
    }

    /**
     * Fetches the latest commit SHA for a file at the specified path in the repository.
     * @param path the path of the file to get the latest commit for
     * @return the SHA of the latest commit for the file
     * @throws IllegalArgumentException if the path does not exist or is invalid
     * @throws IOException if there is an error fetching the commit information
     */
    public static String getLatestCommit(String path) throws IllegalArgumentException, IOException {
        var res = getJsonResponse(Constants.COMMIT_INFO + path);
        if (res.has("oid")) {
            return res.getString("oid");
        } else {
            throw new IllegalArgumentException("Path does not exist or is invalid: " + path);
        }
    }

    /**
     * Fetches the contents of a file from the repository at the specified URL.
     * @param url the URL of the file to fetch
     * @return the raw contents of the file as a byte array
     * @throws IOException if there is an error fetching the file
     */
    public static byte[] fetchBytes(String url) throws IOException {
        var connection = new URL(url).openConnection();
        connection.addRequestProperty("Accept-Encoding", "identity");
        connection.addRequestProperty("accept", "application/json");
        byte[] bytes = new byte[connection.getContentLength()];
        try (InputStream in = connection.getInputStream()) {
            in.read(bytes);
            return bytes;
        }
    }

    /**
     * Fetches a JSON response from the specified URL.
     * @param url the URL to fetch the JSON response from
     * @return a JSONObject representing the JSON response
     * @throws IOException if there is an error fetching the response
     * @throws JSONException if the response is not valid JSON
     */
    public static JSONObject getJsonResponse(String url) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.connect();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return new JSONObject(response.toString());
        }
    }




    /**
     * Downloads a file from the repository at the specified path.
     * @param path the path of the file to download
     * @return an InputStream to read the file contents
     * @throws IOException if there is an error downloading the file
     */
    public static InputStream download(String path) throws IOException {
        var connection = new URL(Constants.BASE_RAW_URL + "/main/" + path).openConnection();
        connection.addRequestProperty("Accept-Encoding", "identity");
        return connection.getInputStream();
    }

    public static void deleteAsset(int id) throws IOException {
        URL url = new URL(Constants.DELETE_ASSETS + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");

        int responseCode = conn.getResponseCode();
        if (responseCode != 204) {
            throw new IOException("Failed to delete asset. HTTP response code: " + responseCode);
        }
        conn.disconnect();
    }

    public static JSONObject uploadAsset(InputStream stream, String name, String tag, ProgressListener listener) {
        try {
            var releaseID = repo.getReleaseByTagName(tag).getId();
            long size = stream.available();
            URL url = new URL(String.format(Constants.UPLOAD_ASSETS,releaseID) + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true); // Enable writing to the connection
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(size));

            OutputStream os = conn.getOutputStream();
            long totalByte = 0;
            byte[] buffer = new byte[4096]; // Use a buffer to stream the data
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalByte += bytesRead;
                listener.onProgress(totalByte,size);
            }

            os.close();
            stream.close();

            int responseCode = conn.getResponseCode();

            InputStream in = (responseCode >= 200 && responseCode < 300) ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                StringBuilder responseBody = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
                return new JSONObject(responseBody.toString());
            }
        } catch (IOException e){
            listener.onError(e);
            return null;
        }
    }

    public static boolean assetExists(String filename,String tag) throws IOException {
        var url = Constants.RELEASE_DOWNLOAD_URL + tag + "/" + filename;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            var resCode = conn.getResponseCode();
            return (conn.getResponseCode()/100) == 2;
        } catch (IOException e) {
            if (e instanceof FileNotFoundException)
                return false;
            else
                throw e;
        }
    }

    /**
     * Lists all files in the repository at the specified path.
     * @param path the path to list files from
     * @return a list of GithubFile objects representing the files in the repository
     * @throws IOException if there is an error listing the files
     */
    public static List<GithubFile> listFiles(String path) throws IOException {
        if (!path.endsWith("/"))
            path = path + "/";


        var connection = new URL(Constants.TREE_PRIVATE_URL + path).openConnection();
        connection.addRequestProperty("accept", "application/json");
        StringBuilder str = new StringBuilder();
        Scanner scanner = new Scanner(connection.getInputStream());
        while (scanner.hasNext())
            str.append(scanner.nextLine());

        scanner.close();
        try {
            var files = new ArrayList<GithubFile>();
            var json = new JSONObject(str.toString());
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                var next = keys.next();
                var sha = json.getJSONObject(next).getString("oid");
                var file = new GithubFile(next, path + next, sha);
                file.rawUrl = Constants.BASE_RAW_URL + "main/" + path + next;
                files.add(file);
            }
            return files;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
