package com.clorabase.console.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.util.Consumer;

import com.clorabase.console.DocumentActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.fragments.DatabaseFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GithubFilesAdapter extends BaseAdapter {
    public final Intent intent;
    private List<File> files;
    private final List<List<File>> history = new ArrayList<>();
    private final Context context;
    private String path;

    public GithubFilesAdapter(Context context,List<File> files) {
        this.files = files;
        this.context = context;
        intent = new Intent(context,DocumentActivity.class);
    }


    @Override
    public int getCount() {
        return files.size()+2;
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
        var textview = new TextView(context);
        textview.setTextSize(20);
        textview.setPadding(10,10,10,10);
        textview.setTextColor(Color.BLACK);
        textview.setCompoundDrawablePadding(10);
        if (i > 1){
            i = i-2;
            var file = files.get(i);
            textview.setText(file.name);
            textview.setCompoundDrawablesWithIntrinsicBounds(file.icon,0,0,0);
            textview.setOnClickListener(v -> {
                if (file.collection.isEmpty())
                    path = DatabaseFragment.BASE_PATH + file.collection + file.name;
                else
                    path = DatabaseFragment.BASE_PATH + file.collection + "/" + file.name;

                if (file.isFile){
                    Utils.read(path, bytes -> {
                        intent.putExtra("mode","edit");
                        intent.putExtra("data",new String(bytes));
                        intent.putExtra("path",path);
                        context.startActivity(intent);
                    });
                } else {
                    var dialog = new ProgressDialog(context);
                    dialog.setMessage("Fetching...");
                    dialog.show();
                    new Thread(() -> {
                        try {
                            history.add(files);
                            files = file.list();
                            ((Activity) context).runOnUiThread(() -> {
                                dialog.dismiss();
                                notifyDataSetChanged();
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
            });
        } else if (i == 0){
            textview.setText("Go back ...");
            textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_back,0,0,0);
            textview.setOnClickListener(v -> {
                if (history.size() > 0){
                    files = history.get(history.size()-1);
                    notifyDataSetChanged();
                } else {
                    textview.setEnabled(false);
                }
            });
        } else if (i == 1){
            textview.setText("Add new document");
            textview.setTextColor(Color.GREEN);
            textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_add_document,0,0,0);
            textview.setOnClickListener(v -> {
                var edittext = new EditText(context);
                new AlertDialog.Builder(context)
                        .setTitle("Add new document")
                        .setView(edittext)
                        .setMessage("Enter the document name")
                        .setPositiveButton("create", (dialog, which) -> {
                            intent.putExtra("mode","create");
                            intent.putExtra("path",path + "/" + edittext.getText().toString());
                            context.startActivity(intent);
                        }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }
        return textview;
    }

    public static class File {
        public boolean isFile;
        public String name;
        public String sha;
        public String collection;
        public int icon;

        public File(boolean isFile,int icon, String name, String sha, String collection) {
            this.isFile = isFile;
            this.name = name;
            this.sha = sha;
            this.collection = collection;
            this.icon = icon;
        }

        public List<File> list() throws InterruptedException {
            var files = new ArrayList<File>();
            var thread = new Thread(() -> {
                try {
                    var tree = Utils.repo.getTree(sha).getTree();
                    var collection = this.collection  + "/" + name;
                    for (var entry : tree){
                        var isFile = entry.getType().equals("blob");
                        var icon = isFile ? R.drawable.ic_file : R.drawable.ic_folder;
                        files.add(new File(isFile,icon, entry.getPath(), entry.getSha(),collection));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            thread.join();
            return files;
        }

        @Override
        public String toString() {
            return "File{" +
                    "isFile=" + isFile +
                    ", name='" + name + '\'' +
                    ", sha='" + sha + '\'' +
                    ", path='" + collection + '\'' +
                    ", icon=" + icon +
                    '}';
        }
    }
}
