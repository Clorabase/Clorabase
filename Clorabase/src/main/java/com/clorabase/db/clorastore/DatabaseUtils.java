package com.clorabase.db.clorastore;

import android.util.Base64;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DatabaseUtils {
    public static Map<String, String> shaMap = new HashMap<>();
    public static String BASE_URL = "https://api.github.com/repos/Clorabase-databases/";
    public static String token;
    public static String repo;

    public static void init(String token, String repo) {
        DatabaseUtils.token = token;
        DatabaseUtils.repo = repo;
        BASE_URL += repo;
    }

    protected static synchronized void createFile(Map<String, Object> content, String path) throws Exception {
        var json = new JSONObject();
        var data = new JSONObject(content).toString(3).getBytes();
        json.put("message", "Created by Clorastore");
        json.put("content", Base64.encodeToString(data, Base64.DEFAULT));
        var response = AndroidNetworking.put(BASE_URL + "/contents/" + path)
                .addHeaders("Authorization", "token " + token)
                .addHeaders("Content-Type", "application/json")
                .addHeaders("Accept", "application/vnd.github.v3+json")
                .addJSONObjectBody(json)
                .build()
                .executeForOkHttpResponse();

        if (!response.isSuccess() || response.getOkHttpResponse().code() > 300) {
            var error = response.getOkHttpResponse().code();
            if (error == 404)
                throw new FileNotFoundException("Database does not exist");
            else if (error == 409)
                throw new Exception("Too many requests at one time. Please use another method for batching multiple requests");
            else
                throw new RuntimeException("Error creating file: " + error);
        }
    }

    protected static synchronized void updateFile(JSONObject oldContent, JSONObject newContent, String path) throws Exception {
        var itr = newContent.keys();
        while (itr.hasNext()) {
            var key = itr.next();
            oldContent.put(key, newContent.get(key));
        }

        var sha = shaMap.get(path) == null ? getFileSha(path) : shaMap.get(path);
        var json = new JSONObject();
        json.put("message", "Created by Clorastore");
        json.put("sha",sha);
        json.put("content", Base64.encodeToString(oldContent.toString().getBytes(), Base64.DEFAULT));
        var response = AndroidNetworking.put(BASE_URL + "/contents/" + path)
                .addHeaders("Authorization", "token " + token)
                .addHeaders("Content-Type", "application/json")
                .addHeaders("Accept", "application/vnd.github.v3+json")
                .addJSONObjectBody(json)
                .build()
                .executeForOkHttpResponse();

        if (!response.isSuccess() || response.getOkHttpResponse().code() > 300) {
            var error = response.getOkHttpResponse().code();
            if (error == 404)
                throw new FileNotFoundException("Document or collection does not exist");
            else if (error == 409)
                throw new Exception("Too many requests at one time. Please use another method for batching multiple requests");
            else
                throw new RuntimeException("Error creating file: " + error);
        }
    }

    protected static synchronized void deleteFile(String path) throws Exception {
        var json = new JSONObject();
        json.put("message", "Deleted by Clorastore");
        json.put("sha", getFileSha(path));
        var response = AndroidNetworking.delete(BASE_URL + "/contents/" + path)
                .addHeaders("Authorization", "token " + token)
                .addHeaders("Content-Type", "application/json")
                .addHeaders("Accept", "application/vnd.github.v3+json")
                .addJSONObjectBody(json)
                .build()
                .executeForOkHttpResponse();

        if (!response.isSuccess() || response.getOkHttpResponse().code() > 300) {
            var error = response.getOkHttpResponse().code();
            if (error == 404)
                throw new FileNotFoundException("Document or collection does not exist");
            else if (error == 409)
                throw new Exception("Too many requests at one time. Please use another method for batching multiple requests");
            else
                throw new RuntimeException("Error creating file: " + error);
        }
    }

    protected static Map<String, Object> getContent(String path) {
        try {
            var content = getFileContent(path);
            return asMap(new JSONObject(content));
        } catch (JSONException | IOException e) {
            if (e instanceof JSONException)
                throw new RuntimeException("Invalid JSON data in document. Delete this document and create a new one with valid JSON data");
            else if (e instanceof UnknownHostException)
                throw new IllegalStateException("Please check your internet connection");
            else if (e instanceof FileNotFoundException)
                return new HashMap<>();
            else
                throw new RuntimeException("Server error 500. This is a bug. Please report it. Error derails:-\n" + e.getLocalizedMessage());
        }
    }

    protected static List<GitFile> getDocuments(String path) throws Exception {
        List<GitFile> files = new ArrayList<>();
        ANResponse<JSONArray> response = AndroidNetworking.get(BASE_URL + "/contents/" + path)
                .addHeaders("Authorization", "token " + token)
                .addHeaders("Accept", "application/vnd.github.v3+json")
                .build()
                .executeForJSONArray();

        if (response.isSuccess() && response.getOkHttpResponse().code() < 300) {
            var json = response.getResult();
            for (int i = 0; i < json.length(); i++) {
                var file = json.getJSONObject(i);
                if (file.getString("type").equals("file")){
                    var content = asMap(new JSONObject(getFileContent(file.getString("path"))));
                    files.add(new GitFile(file.getString("name"),content));
                }
            }
            return files;
        } else {
            var error = response.getOkHttpResponse().code();
            if (error == 404)
                throw new FileNotFoundException("Document or collection does not exist");
            else if (error == 409)
                throw new Exception("Too many requests at one time. Please use another method for batching multiple requests");
            else
                throw new RuntimeException("Error creating file: " + error);
        }
    }

    protected static Map<String, Object> asMap(JSONObject object) throws JSONException {
        var map = new HashMap<String, Object>();
        var iterator = object.keys();
        while (iterator.hasNext()) {
            var key = iterator.next();
            map.put(key, object.get(key));
        }
        return map;
    }

    private static String getFileContent(String path) throws IOException {
        var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/" + repo + "/main/" + path).openConnection();
        var scanner = new Scanner(connection.getInputStream());
        var builder = new StringBuilder();
        while (scanner.hasNext())
            builder.append(scanner.nextLine());
        scanner.close();
        return builder.toString();
    }

    private static String getFileSha(String path) throws JSONException, FileNotFoundException {
        ANResponse<JSONObject> response = AndroidNetworking.get(BASE_URL + "/contents/" + path)
                .addHeaders("Authorization", "token " + token)
                .addHeaders("Accept", "application/vnd.github.v3+json")
                .build()
                .executeForJSONObject();

        if (response.isSuccess()) {
            var sha = response.getResult().getString("sha");
            shaMap.put(path,sha);
            return sha;
        } else if (response.getOkHttpResponse().code() == 404) {
            throw new FileNotFoundException("File does not exist");
        } else {
            throw new RuntimeException("Error getting file sha: " + response.getOkHttpResponse().code());
        }
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
