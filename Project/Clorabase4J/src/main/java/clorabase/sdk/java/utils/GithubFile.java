package clorabase.sdk.java.utils;

public class GithubFile {
    private final String name;
    private final String path;
    public String rawUrl;
    private final String sha;
    private final boolean isFile;

    public GithubFile(String name, String path, String sha) {
        this.name = name;
        this.path = path;
        this.sha = sha;
        this.isFile = name.contains(".");
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getSha() {
        return sha;
    }

    public boolean isFile() {
        return isFile;
    }

    @Override
    public String toString() {
        return "GithubFile{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", sha='" + sha + '\'' +
                '}';
    }
}
