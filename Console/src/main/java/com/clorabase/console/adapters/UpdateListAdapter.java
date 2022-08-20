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
import com.clorabase.console.Utils;
import com.clorabase.console.databinding.ListUpdatesBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class UpdateListAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> names,packages;
    private final List<Integer> versions;

    public UpdateListAdapter(Context context, List<String> packages, List<String> names, List<Integer> versions) {
        this.context = context;
        this.names = names;
        this.packages = packages;
        this.versions = versions;
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
        binding.version.setText(versions.get(position) + "");

        var path = MainActivity.CURRENT_PROJECT + "/updates/" + packages.get(position) + ".json";

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
                        var appName = names.get(position);
                        JSONObject json = new JSONObject();
                        try {
                            json.put("versionCode", code);
                            json.put("name",appName);
                            json.put("mode", "flexible");
                            Utils.update(json.toString().getBytes(), path, new Utils.AsyncCallback() {
                                @Override
                                public void onComplete() {
                                    versions.set(position, code);
                                    Toast.makeText(context, "Version incremented", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    Toast.makeText(context, "Failed to increment version", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }).setNegativeButton("cancel", null).show();
        });

        binding.delete.setOnClickListener(v -> Utils.delete(path, new Utils.AsyncCallback() {
            @Override
            public void onComplete() {
                names.remove(position);
                versions.remove(position);
                packages.remove(position);

                notifyDataSetChanged();
                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show();
            }
        }));
        return binding.getRoot();
    }
}
