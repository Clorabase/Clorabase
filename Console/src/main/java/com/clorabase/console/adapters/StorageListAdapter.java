package com.clorabase.console.adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.clorabase.console.ClorabaseStorage;
import com.clorabase.console.MainActivity;
import com.clorabase.console.Utils;
import com.clorabase.console.databinding.ListStorageBinding;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StorageListAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> names;
    private final List<String> sizes;

    public StorageListAdapter(Context context, List<String> names, List<String> sizes) {
        this.names = names;
        this.context = context;
        this.sizes = sizes;
    }

    @Override
    public int getCount() {
        return names.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    @SuppressLint("ViewHolder")
    public View getView(int position, View convertView, ViewGroup parent) {
        ListStorageBinding binding = ListStorageBinding.inflate(LayoutInflater.from(context));
        binding.name.setText(names.get(position));
        binding.size.setText(sizes.get(position));

        binding.delete.setOnClickListener(v -> {
            Utils.delete(MainActivity.CURRENT_PROJECT + "/Storage/" + names.get(position), new Utils.AsyncCallback() {
                @Override
                public void onComplete() {
                    names.remove(position);
                    sizes.remove(position);
                    notifyDataSetChanged();
                    Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            });
        });

        binding.download.setOnClickListener(v -> {
            var dialog = new ProgressDialog(context);
            dialog.setTitle("Downloading");
            dialog.setMessage("Downloading " + names.get(position) + " to app's external storage directory");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
            ClorabaseStorage.download(MainActivity.CURRENT_PROJECT, names.get(position), context.getExternalFilesDir(null), new ClorabaseStorage.ClorabaseStorageCallback() {
                @Override
                public void onFailed(@NonNull Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(int prcnt) {
                    dialog.setProgress(prcnt);
                }

                @Override
                public void onComplete() {
                    var cr = context.getContentResolver();
                    var file = new File(context.getExternalFilesDir(null),names.get(position));
                    var cv = new ContentValues();
                    cv.put(MediaStore.Files.FileColumns.DISPLAY_NAME, names.get(position));
                    cv.put(MediaStore.Files.FileColumns.DATA,file.getPath());;
                    cv.put(OpenableColumns.DISPLAY_NAME, names.get(position));
                    var uri = cr.insert(MediaStore.Files.getContentUri("external"),cv);
                    try {
                        var os = cr.openOutputStream(uri);
                        FileUtils.copyFile(file, os);
                        os.close();
                        dialog.dismiss();
                        Toast.makeText(context, "File saved to 'Downloads' directory", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "Failed to save file, you can access it from the app external files dir", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
        return binding.getRoot();
    }


    public void clear(){
        names.clear();
        sizes.clear();
        notifyDataSetChanged();
    }
}
