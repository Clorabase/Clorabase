package com.clorabase.console;

import java.text.MessageFormat;

public class Constants {
    public static final int REQUEST_CODE_LOGIN = 85;
    public static final int REQUEST_CODE_PROJECT = 58;
    public static final String REPO_NAME = "Clorabase-projects";
    public static final String FRAGMENT_TAG = "currentFragment";
    public static final String RELEASE_DOWNLOAD_URL = "https://github.com/{0}/Clorabase-projects/releases/download/{1}/";
    public static String BASE_RAW_URL = "https://raw.githubusercontent.com/%1$s/Clorabase-projects/main/";
    public static String GIT_TREE_URL = "https://api.github.com/repos/%1$s/Clorabase-projects/git/trees/main:";
    public static String TREE_PRIVATE_URL = "https://github.com/%1$s/Clorabase-projects/tree-commit-info/main/";
    public static final String PATH_STORAGE_JSON = "/storage/structure.json";
}
