package com.clorabase.clorastore;

import com.clorabase.GithubUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTreeEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class DatabaseUtils {
    private static GHRepository repo;
    private static String username;
    protected static Map<String, String> shaMap = new HashMap<>();
    private static Key key;

    public static void init(GHRepository repo, String project, String username) {
        if (GithubUtils.exists(project + "/config.json")){
            DatabaseUtils.repo = repo;
            DatabaseUtils.username = username;
            try {
                key = generateKey(project);
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException("Failed to decrypt database. Please check your password");
            }
        } else
            throw new IllegalArgumentException("Project does not exists. First create a project from the console");
    }

    protected static synchronized void createFile(Map<String, Object> content, String path) throws Exception {
        var json_data = new JSONObject(content).toString();
        var enc_data = encrypt(json_data);
        if (enc_data.length > 1024 * 1024 * 3)
            throw new IOException("Document size must be less then 3 MB. Current size is " + enc_data.length/1024 + " KB");

        var sha = repo.createContent(enc_data, "Created through library", path).getCommit().getSha();
        shaMap.put(path, sha);
    }

    protected static synchronized void updateFile(JSONObject oldContent, JSONObject newContent, String path) throws Exception {
        var itr = newContent.keys();
        while (itr.hasNext()) {
            var key = itr.next();
            oldContent.put(key, newContent.get(key));
        }
        var sha = shaMap.get(path);
        var data = encrypt(oldContent.toString());
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

    protected static synchronized void deleteFile(String path) throws IOException {
        try {
            repo.getFileContent(path).delete("Deleted on " + new Date());
        } catch (FileNotFoundException e){
            throw new FileNotFoundException("There is no document at " + path);
        }
    }

    protected static Map<String, Object> getContent(String path) {
        try {
            var content = decrypt(getFileContent(path));
            assert content != null;
            return asMap(new JSONObject(content));
        } catch (JSONException | IOException e) {
            e.printStackTrace();
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

    protected static List<GitFile> getDocuments(String path) throws Exception {
        try {
            var sha = repo.getFileContent(path).getSha();
            var files = repo.getTree(sha).getTree();
            var gitFiles = new ArrayList<GitFile>();
            for (GHTreeEntry file : files) {
                var fileName = file.getPath().substring(file.getPath().lastIndexOf('/') + 1);
                var dec = decrypt(getFileContent(file.getPath()));
                assert dec != null;
                var gitFile = new GitFile(fileName,asMap(new JSONObject(dec)));
                gitFiles.add(gitFile);
            }
            return gitFiles;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("No collection found at " + path);
        } catch (AssertionError error) {
            throw new RuntimeException("Failed to decrypt the document. Your document is either corrupted or password/key is invalid");
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

    private static byte[] getFileContent(String path) throws IOException {
        var connection = new URL("https://raw.githubusercontent.com/" + username + "/Clorabase-databases/main/" + path).openConnection();
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

    protected static byte[] encrypt(String str) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        return cipher.doFinal(str.getBytes());
    }

    public static SecretKey generateKey(String password) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), "abcdefgh".getBytes(), 786, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static String decrypt(byte[] str) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,key);
            var bytes = cipher.doFinal(str);
            return new String(bytes);
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
