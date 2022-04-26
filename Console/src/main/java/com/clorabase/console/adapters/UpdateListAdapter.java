package com.clorabase.console.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.clorabase.console.MainActivity;
import com.clorabase.console.databinding.ListUpdatesBinding;
import com.clorabase.console.fragments.UpdatesFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class UpdateListAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> links, versions, names;

    public UpdateListAdapter(Context context, List<String> names, List<String> links, List<String> versions) {
        this.context = context;
        this.names = names;
        this.versions = versions;
        this.links = links;
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
        ListUpdatesBinding binding = ListUpdatesBinding.inflate(LayoutInflater.from(context));
        binding.name.setText(names.get(position));
        binding.link.setText(links.get(position));
        binding.version.setText(versions.get(position));

        binding.delete.setOnClickListener(v -> MainActivity.drive.delete(UpdatesFragment.ids.get(position)).setOnErrorCallback(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show()));
        binding.update.setOnClickListener(v -> {
            EditText editText = new EditText(context);
            editText.setSingleLine(true);
            editText.setHint("Example for version 3.2.5, enter 325");
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setPaddingRelative(50, 0, 0, 0);

            new AlertDialog.Builder(context)
                    .setTitle("Increment app version")
                    .setView(editText)
                    .setMessage("Enter the new (latest) version code of your app.")
                    .setPositiveButton("ok", (dialog, which) -> {
                        int code = Integer.parseInt(editText.getText().toString());
                        MainActivity.drive.getContent(UpdatesFragment.ids.get(position)).setOnSuccessCallback(bytes -> {
                            try {
                                JSONObject json = new JSONObject(new String(bytes));
                                json.put("versionCode", code);
                                json.put("name", names.get(position));
                                json.put("mode", "flexible");
                                json.put("link", links.get(position));

                                MainActivity.updateFile(context, UpdatesFragment.ids.get(position), json.toString(), call -> {
                                    if (call.isSuccessful)
                                        Toast.makeText(context, "version incremented", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(context, "error incrementing version", Toast.LENGTH_SHORT).show();
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }).setOnErrorCallback(e -> Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show());
                    }).setNegativeButton("cancel", null).show();
        });
        return binding.getRoot();
    }
}
