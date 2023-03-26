package com.clorabase.clorastore;

import android.util.Base64;

import com.clorabase.GithubUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHContent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DatabaseUtils {
    protected static Map<String, String> shaMap = new HashMap<>();
    protected static String token;

    public static void init(String token, String repo) {
        if (GithubUtils.exists(repo + "/config.json")){
            DatabaseUtils.token = token;

        } else
            throw new IllegalArgumentException("Project does not exists. First create a project from the console");
    }

    protected static synchronized void createFile(Map<String, Object> content, String path) throws Exception {
        var data = new JSONObject(content).toString().getBytes();
        var sha = GithubUtils.getRepository().createContent()
                .content(data).path(path)
                .message("Created on " + new Date()).commit()
                .getCommit().getSha();
        shaMap.put(path, sha);
    }

    protected static synchronized void updateFile(JSONObject oldContent, JSONObject newContent, String path) throws Exception {
        var itr = newContent.keys();
        while (itr.hasNext()) {
            var key = itr.next();
            oldContent.put(key, newContent.get(key));
        }
        var sha = shaMap.get(path);
        if (sha == null){
            GithubUtils.getRepository().getFileContent(path).update(oldContent.toString().getBytes(), "Updated on " + new Date());
        } else {
            GithubUtils.getRepository().createContent()
                    .content(oldContent.toString().getBytes())
                    .path(path)
                    .message("Updated on " + new Date())
                    .sha(sha)
                    .commit();
        }
    }

    protected static synchronized void deleteFile(String path) throws IOException {
        try {
            GithubUtils.getRepository().getFileContent(path).delete("Deleted on " + new Date());
        } catch (FileNotFoundException e){
            throw new FileNotFoundException("There is no document at " + path);
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
                throw new RuntimeException(new FileNotFoundException("There is no document at " + path));
            else
                throw new RuntimeException("Server error 500. This is a bug. Please report it. Error derails:-\n" + e.getLocalizedMessage());
        }
    }

    protected static List<GitFile> getDocuments(String path) throws Exception {
        try {
            var files = GithubUtils.getRepository().getDirectoryContent(path);
            var gitFiles = new ArrayList<GitFile>();
            for (GHContent file : files) {
                var gitFile = new GitFile(file.getName(),asMap(new JSONObject(getFileContent(file.getPath()))));
                gitFiles.add(gitFile);
            }
            return gitFiles;
        } catch (FileNotFoundException e){
            throw new FileNotFoundException("No collection found at " + path);
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
                        list.add(i);
                    }
                    map.put(key,list);
                }
            }
            map.put(key, object.get(key));
        }
        return map;
    }

    private static String getFileContent(String path) throws IOException {
        var connection = new URL("https://raw.githubusercontent.com/Clorabase-databases/OpenDatabases/main/" + path).openConnection();
        var scanner = new Scanner(connection.getInputStream());
        var builder = new StringBuilder();
        while (scanner.hasNext())
            builder.append(scanner.nextLine());
        scanner.close();
        return builder.toString();
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
