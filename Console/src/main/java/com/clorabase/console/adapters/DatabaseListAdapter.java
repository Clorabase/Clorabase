package com.clorabase.console.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.fragments.DatabaseFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class DatabaseListAdapter extends BaseAdapter {
    private final List<String> names;
    private final List<String> packages;
    private final List<Integer> sizes;
    private final Context context;

    public DatabaseListAdapter(@NonNull Context context, List<String> names, List<String> packages, List<Integer> size) {
        this.sizes = size;
        this.names = names;
        this.packages = packages;
        this.context = context;
    }


    @Override
    @SuppressLint("ViewHolder")
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.database_list, parent, false);

        TextView name = view.findViewById(R.id.name);
        TextView packageName = view.findViewById(R.id.package_name);
        TextView size = view.findViewById(R.id.size);
        ImageView delete = view.findViewById(R.id.delete);
        ImageView preview = view.findViewById(R.id.preview);

        name.setText(names.get(position));
        packageName.setText(packages.get(position));
        if (sizes.size() > position)
            size.append(sizes.get(position) + " MB");

        delete.setOnClickListener(v -> new MaterialAlertDialogBuilder(context)
                .setTitle("Are you sure ?")
                .setMessage("This will delete the whole database. You can't undo this action")
                .setPositiveButton("Delete", (dialog, which) -> {
                    MainActivity.delete(context, DatabaseFragment.ids.get(position), task -> {
                        if (task.isSuccessful) {
                            names.remove(position);
                            packages.remove(position);
                            sizes.remove(position);
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(context, "Failed to delete database", Toast.LENGTH_SHORT).show();
                            task.exception.printStackTrace();
                        }
                    });
                }).show());

        preview.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com/file/d/" + DatabaseFragment.ids.get(position) + "/view"))));
        return view;
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
    public void notifyDataSetChanged() {
        DatabaseFragment.db.put("packages",packages);
        DatabaseFragment.db.put("names",names);
        DatabaseFragment.db.put("sizes",sizes);
        DatabaseFragment.db.put("ids",DatabaseFragment.ids);
        super.notifyDataSetChanged();
    }
}
