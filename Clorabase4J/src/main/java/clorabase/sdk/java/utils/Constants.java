package clorabase.sdk.java.utils;

public class Constants {
    public static final String REPO_NAME = "Clorabase-projects";
    public static String RELEASE_DOWNLOAD_URL = "https://github.com/%s/Clorabase-projects/releases/download/";
    public static String DELETE_ASSETS = "https://api.github.com/repos/%s/Clorabase-projects/releases/assets/";
    public static String UPLOAD_ASSETS = "https://uploads.github.com/repos/USER/Clorabase-projects/releases/%s/assets?name=";
    public static String BASE_RAW_URL = "https://raw.githubusercontent.com/%1$s/Clorabase-projects/";
    public static String GIT_TREE_URL = "https://api.github.com/repos/%1$s/Clorabase-projects/git/trees/main:";
    public static String TREE_PRIVATE_URL = "https://github.com/%1$s/Clorabase-projects/tree-commit-info/main/";
    public static String COMMIT_INFO = "https://github.com/%1$s/Clorabase-projects/latest-commit/main/";

    public static void init(String username) {
        BASE_RAW_URL = String.format(BASE_RAW_URL, username);
        GIT_TREE_URL = String.format(GIT_TREE_URL, username);
        TREE_PRIVATE_URL = String.format(TREE_PRIVATE_URL, username);
        COMMIT_INFO = String.format(COMMIT_INFO, username);
        DELETE_ASSETS = String.format(DELETE_ASSETS, username);
        UPLOAD_ASSETS = UPLOAD_ASSETS.replace("USER",username);
        RELEASE_DOWNLOAD_URL = String.format(RELEASE_DOWNLOAD_URL, username);
    }
}
