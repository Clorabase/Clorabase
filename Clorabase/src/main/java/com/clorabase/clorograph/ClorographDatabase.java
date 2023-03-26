package com.clorabase.clorograph;

import android.content.Context;

import androidx.annotation.NonNull;

import com.clorabase.GithubUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import db.clorabase.clorograph.CloroGraph;
import db.clorabase.clorograph.Savable;
import db.clorabase.clorograph.graphs.Graph;
import db.clorabase.clorograph.trees.Tree;

public class ClorographDatabase {
    private static ClorographDatabase INSTANCE;
    private CloroGraph db;
    private String PATH;
    private File DB_DIR;
    private final Executor executor = Executors.newCachedThreadPool();

    public static ClorographDatabase getInstance(@NonNull String projectId, @NonNull Context context) {
        var project = GithubUtils.getProjectById(projectId);
        if (INSTANCE == null) {
            if (GithubUtils.exists(project + "/config.json")) {
                INSTANCE = new ClorographDatabase();
                INSTANCE.PATH = project + "/db/";
                INSTANCE.DB_DIR = context.getFilesDir();
                INSTANCE.db = CloroGraph.getInstance(context.getFilesDir());
            } else
                throw new RuntimeException("There is no project created with name " + project + ". Please create the project first from the console");
        }
        return INSTANCE;
    }

    public <T extends Savable> Task<Graph<T>> createGraph(@NonNull Graph<T> graph) {
        var name = graph.getName() + ".graph";
        return Tasks.call(executor, () -> {
            if (GithubUtils.exists(PATH + name))
                throw new RuntimeException("Graph with name " + name + " already exists");
            else {
                var newGraph = db.createOrOpenGraph(graph);
                var file = new File(DB_DIR, name);
                var in = new FileInputStream(file);
                var bytes = new byte[(int) file.length()];
                in.read(bytes);
                GithubUtils.getRepository().createContent()
                        .content(bytes)
                        .path(PATH + name)
                        .message("Created on " + new Date())
                        .commit();
                return newGraph;
            }
        });
    }

    public <T extends Savable> Task<Tree<T>> createTree(@NonNull String name,@NonNull Tree<T> tree) {
        return Tasks.call(executor, () -> {
            if (GithubUtils.exists(PATH + name + ".tree"))
                throw new RuntimeException("Tree with name " + name + ".tree" + " already exists");
            else {
                var newTree = db.createOrOpenTree(name,tree);
                var file = new File(DB_DIR, name + ".tree");
                var in = new FileInputStream(file);
                var bytes = new byte[(int) file.length()];
                in.read(bytes);
                GithubUtils.getRepository().createContent()
                        .content(bytes)
                        .path(PATH + name + ".tree")
                        .message("Created on " + new Date())
                        .commit();
                return newTree;
            }
        });
    }

    public <T extends Savable> Task<Void> saveGraph(@NonNull Graph<T> graph) {
        return Tasks.call(executor, () -> {
            graph.commit(db);
            var file = new File(DB_DIR, graph.getName() + ".graph");
            var in = new FileInputStream(file);
            var bytes = new byte[(int) file.length()];
            in.read(bytes);
            GithubUtils.getRepository()
                    .getFileContent(PATH + file.getName())
                    .update(bytes, "Updated on " + new Date());
            return null;
        });
    }

    public <T extends Savable> Task<Void> saveTree(@NonNull String name,@NonNull Tree<T> tree) {
        return Tasks.call(executor, () -> {
            db.saveTree(name,tree);
            var file = new File(DB_DIR, name + ".tree");
            var in = new FileInputStream(file);
            var bytes = new byte[(int) file.length()];
            in.read(bytes);
            GithubUtils.getRepository()
                    .getFileContent(PATH + file.getName())
                    .update(bytes, "Updated on " + new Date());
            return null;
        });
    }

    public Task<Graph<?>> openGraph(@NonNull String name) {
        return Tasks.call(executor, () -> {
            var path = PATH + name + ".graph";
            if (GithubUtils.exists(path)) {
                GithubUtils.download(DB_DIR, name + ".graph", path);
                return db.openGraph(name);
            } else
                throw new RuntimeException("Graph with name " + name + " does not exist");
        });
    }

    public Task<Tree<?>> openTree(@NonNull String name) {
        return Tasks.call(executor, () -> {
            var path = PATH + name + ".tree";
            if (GithubUtils.exists(path)) {
                GithubUtils.download(DB_DIR, name + ".tree", path);
                return db.openTree(name);
            } else
                throw new RuntimeException("Tree with name " + name + " does not exist");
        });
    }

    public <T extends Savable> void updateTree(@NonNull String name, @NonNull OnCompleteCallback<Tree<T>> callback) {
        openTree(name).addOnSuccessListener(tree -> {
            callback.onFetched((Tree<T>) tree);
            saveTree(name,tree).addOnCompleteListener(task -> {
                if (task.isSuccessful())
                    callback.onUpdated();
                else
                    callback.onFailure(task.getException());
            });
        }).addOnFailureListener(callback::onFailure);
    }

    public <T extends Savable> void updateGraph(@NonNull String name, @NonNull OnCompleteCallback<Graph<T>> callback) {
        openGraph(name).addOnSuccessListener(graph -> {
            callback.onFetched((Graph<T>) graph);
            saveGraph(graph).addOnCompleteListener(task -> {
                if (task.isSuccessful())
                    callback.onUpdated();
                else
                    callback.onFailure(task.getException());
            });
        }).addOnFailureListener(callback::onFailure);
    }

    public Task<Void> delete(@NonNull String name) {
        return Tasks.call(executor, () -> {
            var path = PATH + name;
            if (GithubUtils.exists(path)) {
                GithubUtils.getRepository().getFileContent(path).delete("Deleted on " + new Date());
                return null;
            } else
                throw new RuntimeException("Graph with name " + name + " does not exist");
        });
    }
}
