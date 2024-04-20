package com.clorabase.console.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.clorabase.console.DocumentActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.fragments.DatabaseFragment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GithubFilesAdapter extends BaseAdapter {
    public static ExecutorService executor = Executors.newFixedThreadPool(4);
    public final Intent intent;
    private final List<File> files;
    private final Stack<List<File>> history = new Stack<>();
    private final Fragment fragment;
    private String currentCollection;

    public GithubFilesAdapter(Fragment fragment, List<File> files) {
        this.files = files;
        this.fragment = fragment;
        intent = new Intent(fragment.getContext(), DocumentActivity.class);
        currentCollection = DatabaseFragment.BASE_PATH;
    }


    @Override
    public int getCount() {
        return files.size() + 3;
    }

    @Override
    public Object getItem(int i) {
        return files.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (i > 2) {
            i = i - 3;
            var file = files.get(i);
            return addfile(file);
        } else if (i == 0) {
            return goback();
        } else if (i == 1) {
            return addDocument();
        } else if (i == 2) {
            return addCollection();
        } else {
            return new TextView(fragment.getContext());
        }
    }

    private View addDocument() {
        var textview = new TextView(fragment.getContext());
        textview.setText("New document");
        textview.setTextSize(20);
        textview.setPadding(10, 10, 10, 10);
        textview.setTextColor(Color.GREEN);
        textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_document, 0, 0, 0);
        textview.setOnClickListener(v -> {
            var edittext = new EditText(fragment.getContext());
            new AlertDialog.Builder(fragment.getContext())
                    .setTitle("Add new document")
                    .setView(edittext)
                    .setMessage("Enter the document name")
                    .setPositiveButton("create", (dialog, which) -> {
                        var name = edittext.getText().toString();
                        if (name.matches("^[a-zA-Z0-9]*$")) {
                            String finalName = name + ".doc";
                            if (files.stream().anyMatch(file -> file.name.equals(finalName))) {
                                Toast.makeText(fragment.getContext(), "Document already exists", Toast.LENGTH_SHORT).show();
                            } else {
                                intent.putExtra("mode", "create");
                                intent.putExtra("path", currentCollection + finalName);
                                fragment.startActivityForResult(intent,11);
                            }
                        } else {
                            dialog.cancel();
                            Toast.makeText(fragment.getContext(), "Document name can only contain letters and numbers", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        return textview;
    }

    @SuppressLint("SetTextI18n")
    private View addCollection() {
        var textview = new TextView(fragment.getContext());
        textview.setText("New Collection");
        textview.setTextSize(20);
        textview.setPadding(10, 10, 10, 10);
        textview.setTextColor(Color.GREEN);
        textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
        textview.setOnClickListener(v -> {
            var edittext = new EditText(fragment.getContext());
            new AlertDialog.Builder(fragment.getContext())
                    .setTitle("Add new collection")
                    .setView(edittext)
                    .setMessage("Enter the collection name")
                    .setPositiveButton("create", (dialog, which) -> {
                        var name = edittext.getText().toString();
                        if (name.matches("^[a-zA-Z0-9]*$")) {
                            if (files.stream().anyMatch(file -> file.name.equals(name))) {
                                Toast.makeText(fragment.getContext(), "Collection already exists", Toast.LENGTH_SHORT).show();
                            } else {
                                currentCollection = currentCollection + name + "/";
                                history.push(files);
                                files.clear();
                                notifyDataSetChanged();
                            }
                        } else {
                            Toast.makeText(fragment.getContext(), "Collection name can only contain letters and numbers", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });
        return textview;
    }

    private View goback() {
        var textview = new TextView(fragment.getContext());
        textview.setTextSize(20);
        textview.setPadding(10, 10, 10, 10);
        textview.setText("Go back ...");
        textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back, 0, 0, 0);
        textview.setOnClickListener(v -> {
            if (history.size() > 0) {
                files.clear();
                files.addAll(history.pop());
                System.out.println(files);
                currentCollection = currentCollection.substring(0, currentCollection.lastIndexOf("/"));
                notifyDataSetChanged();
            } else {
                textview.setEnabled(false);
            }

        });
        return textview;
    }

    private View addfile(File file) {
        var textview = new TextView(fragment.getContext());
        textview.setTextSize(20);
        textview.setPadding(10, 10, 10, 10);
        textview.setTextColor(Color.BLACK);
        textview.setText(file.name);
        textview.setCompoundDrawablesWithIntrinsicBounds(file.icon, 0, 0, 0);
        textview.setOnClickListener(v -> {
            if (file.isFile) {
                Utils.read(file.path, bytes -> {
                    intent.putExtra("mode", "edit");
                    intent.putExtra("enc_data",bytes);
                    intent.putExtra("path", file.path);
                    fragment.startActivity(intent);
                });
            } else {
                var dialog = new ProgressDialog(fragment.getContext());
                dialog.setMessage("Fetching...");
                dialog.show();
                new Thread(() -> {
                    try {
                        history.push(new ArrayList<>(files));
                        files.clear();
                        files.addAll(file.list());
                        ((Activity) fragment.getContext()).runOnUiThread(() -> {
                            dialog.dismiss();
                            notifyDataSetChanged();
                            currentCollection += file.name + "/";
                        });
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        fragment.getActivity().runOnUiThread(() -> Toast.makeText(fragment.getContext(), "Interrupted", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        });
        return textview;
    }

    public static class File implements Serializable {
        public boolean isFile;
        public String name;
        public String path;
        public String sha;
        public int icon;

        public File(boolean isFile, String name, String sha, String path) {
            this.isFile = isFile;
            this.name = name;
            this.sha = sha;
            this.path = path;
            this.icon = isFile ? R.drawable.ic_file : R.drawable.ic_folder;
        }

        public File(String name) {
            this.name = name;
        }

        public List<File> list() throws InterruptedException, ExecutionException {
            return executor.submit(() -> Utils.listFiles(path)).get();
        }

        @Override
        public String toString() {
            return "File{" +
                    "isFile=" + isFile +
                    ", name='" + name + '\'' +
                    ", sha='" + sha + '\'' +
                    ", path='" + path + '\'' +
                    ", icon=" + icon +
                    '}';
        }
    }
}
