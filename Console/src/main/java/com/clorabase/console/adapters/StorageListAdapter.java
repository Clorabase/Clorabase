package com.clorabase.console.adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.clorabase.console.MainActivity;
import com.clorabase.console.Utils;
import com.clorabase.console.databinding.ListStorageBinding;
import com.clorabase.console.storage.ClorabaseStorage;
import com.clorabase.console.storage.ClorabaseStorageCallback;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StorageListAdapter extends BaseAdapter {
    private final Context context;
    private final List<ClorabaseStorage.File> files;

    public StorageListAdapter(Context context, List<ClorabaseStorage.File> files) {
        this.files = files;
        this.context = context;
    }

    @Override
    public int getCount() {
        return files.size();
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
        var file = files.get(position);
        binding.name.setText(file.name);
        binding.size.setText(file.size/(1024*1024) + " MB");
        binding.menu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, binding.menu);
            menu.getMenu().add("Download");
            menu.getMenu().add("Delete");
            menu.setGravity(Gravity.START);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getTitle().equals("Download")) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(file.download_url)));
                } else if (item.getTitle().equals("Delete")) {
                    ClorabaseStorage.delete(MainActivity.CURRENT_PROJECT,file.name, new ClorabaseStorageCallback() {
                        @Override
                        public void onFailed(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(int percent) {

                        }

                        @Override
                        public void onComplete() {
                            files.remove(position);
                            notifyDataSetChanged();
                            Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return true;
            });
            menu.show();
            Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show();
        });
        return binding.getRoot();
    }


    public void clear(){
        files.clear();
        notifyDataSetChanged();
    }
}
