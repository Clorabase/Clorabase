package sdk.clorabase.clorastore;

import sdk.clorabase.Constants;
import sdk.clorabase.GithubUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBUtils {
    private final GHRepository repo;
    private final String username;
    protected final Map<String, String> shaMap = new HashMap<>();
    private final Key key;

    public DBUtils(GHRepository repo, String username, String project) {
        if (GithubUtils.exists(project + "/config.json")){
            this.repo = repo;
            this.username = username;
            try {
                key = SecurityProvider.generateKey(project);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException("Failed to decrypt database. Please check your password");
            }
        } else
            throw new IllegalArgumentException("Project does not exists. First create a project from the console");
    }

    protected synchronized void createFile(Map<String, Object> content, String path) throws Exception {
        var json_data = new JSONObject(content).toString();
        var enc_data = SecurityProvider.encrypt(json_data,key);
        if (enc_data.length > Constants.DOCUMENT_SIZE_LIMIT)
            throw new IOException("Document size must be less then 5 MB. Current size is " + enc_data.length/1024 + " KB");

        var sha = repo.createContent(enc_data, "Created through library", path).getCommit().getSha();
        shaMap.put(path, sha);
    }

    protected synchronized void updateFile(Map<String,Object> map, String path) throws Exception {
        var sha = shaMap.get(path);
        var data = SecurityProvider.encrypt(new JSONObject(map).toString(),key);
        if (sha == null){
            repo.getFileContent(path).update(data, "Updated on " + new Date());
        } else {
            repo.createContent()
                    .content(data)
                    .path(path)
                    .message("Updated on " + new Date())
                    .sha(sha)
                    .commit();
        }
    }

    protected synchronized void deleteFile(String path) throws IOException {
        try {
            repo.getFileContent(path).delete("Deleted on " + new Date());
        } catch (FileNotFoundException e){
            throw new FileNotFoundException("There is no document at " + path);
        }
    }

    protected Map<String, Object> getContent(String path) {
        try {
            var content = SecurityProvider.decrypt(getFileContent(path,username),key);
            assert content != null;
            return asMap(new JSONObject(content));
        } catch (JSONException | IOException e) {
            if (e instanceof JSONException)
                throw new RuntimeException("Invalid JSON data in document. Delete this document and create a new one with valid JSON data");
            else if (e instanceof UnknownHostException)
                throw new RuntimeException("Please check your internet connection");
            else if (e instanceof FileNotFoundException)
                return new HashMap<>();
            else
                throw new RuntimeException("Server error 500. This is a bug. Please report it. Error derails:-\n" + e.getLocalizedMessage());
        } catch (AssertionError error){
            throw new RuntimeException("Failed to decrypt the document. Your document is either corrupted or password/key is invalid");
        }
    }

    protected List<String> list(String path,boolean documents) throws Exception {
        var urlStr = "https://github.com/" + username + "/" + Constants.REPO_NAME + "/tree-commit-info/main/" + path;
        var keys = get(urlStr).keys();
        List<String> list = new ArrayList<>();
        while (keys.hasNext()) {
            var key = keys.next();
            if (documents){
                if (key.endsWith(".doc"))
                    list.add(key);
            } else {
                if (!key.endsWith(".doc"))
                    list.add(keys.next());
            }
        }
        return list;
    }

    private static JSONObject get(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return new JSONObject(response.toString());
        } else {
            throw new Exception(connection.getResponseMessage());
        }
    }

    protected static Map<String, Object> asMap(JSONObject object) throws JSONException {
        var map = new HashMap<String, Object>();
        var iterator = object.keys();
        while (iterator.hasNext()) {
            var key = iterator.next();
            var value = object.get(key);
            if (value instanceof JSONObject obj)
                map.put(key,asMap(obj));
            else if (value instanceof JSONArray array){
                if (array.length() == 0)
                    map.put(key,new ArrayList<>());
                else {
                    List list;
                    var clazz = array.get(0);
                    if (clazz instanceof String)
                        list = new ArrayList<String>();
                    else if (clazz instanceof Boolean)
                        list = new ArrayList<Boolean>();
                    else if (clazz instanceof Number)
                        list = new ArrayList<Number>();
                    else
                        throw new IllegalStateException("Invalid data in document");

                    for (int i = 0; i < array.length(); i++) {
                        list.add(array.get(i));
                    }
                    map.put(key,list);
                }
            }
            map.put(key, object.get(key));
        }
        return map;
    }

    private static byte[] getFileContent(String path,String username) throws IOException {
        var connection = new URL("https://raw.githubusercontent.com/" + username + "/Clorabase-projects/main/" + path).openConnection();
        connection.addRequestProperty("Accept-Encoding", "identity");
        var in = connection.getInputStream();
        var array = new byte[connection.getContentLength()];
        in.read(array);
        in.close();
        return array;
    }


    public static class GitFile {
        public final String name;
        public final Map<String,Object> content;

        private GitFile(String name,Map<String, Object> content) {
            this.name = name;
            this.content = content;
        }
    }
}
