package com.clorabase.console.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Toast;

import com.clorabase.console.MainActivity;
import com.clorabase.console.databinding.ListStorageBinding;

import java.util.List;

public class StorageListAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> names;
    private final List<String> ids;
    private final List<Integer> sizes;

    public StorageListAdapter(Context context,List<String> names, List<String> ids, List<Integer> sizes) {
        this.names = names;
        this.ids = ids;
        this.sizes = sizes;
        this.context = context;
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
        com.clorabase.console.databinding.ListStorageBinding binding = ListStorageBinding.inflate(LayoutInflater.from(context));
        binding.name.setText(names.get(position));
        binding.id.setText(ids.get(position));
        binding.size.append(sizes.get(position) + "MB");

        binding.delete.setOnClickListener(v -> {
            MainActivity.delete(context, ids.get(position), task -> {
                if (task.isSuccessful){
                    ids.remove(position);
                    sizes.remove(position);
                    names.remove(position);
                    notifyDataSetChanged();
                } else
                    Toast.makeText(context, task.exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
        });
        binding.download.setOnClickListener(v -> {
            Toast.makeText(context, "You can download file from browser", Toast.LENGTH_SHORT).show();
            MainActivity.drive.getFileId(ids.get(position),null).setOnSuccessCallback(id -> {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/" + id)));
            }).setOnErrorCallback(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
        });
        return binding.getRoot();
    }


    public void clear(){
        names.clear();
        sizes.clear();
        ids.clear();
        notifyDataSetChanged();
    }
}
