package com.clorabase.console.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.UpdateListAdapter;
import com.clorabase.console.databinding.DialogAddUpdateBinding;
import com.clorabase.console.databinding.FragmentUpdatesBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.GHContent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdatesFragment extends Fragment {
    UpdateListAdapter adapter;
    List<Integer> codes = new ArrayList<>();
    List<String> packages = new ArrayList<>();
    List<String> names = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentUpdatesBinding binding = FragmentUpdatesBinding.inflate(inflater);

        populateList();
        adapter = new UpdateListAdapter(getContext(), packages, names, codes);
        ImageView image = new ImageView(getContext());
        image.setImageResource(R.drawable.empty_list);
        ((ViewGroup) binding.list.getParent()).addView(image);
        binding.list.setEmptyView(image);
        binding.list.setAdapter(adapter);

        binding.add.setOnClickListener(v -> {
            DialogAddUpdateBinding dialog = DialogAddUpdateBinding.inflate(getLayoutInflater());
            new AlertDialog.Builder(getContext())
                    .setTitle("Add a new app")
                    .setView(dialog.getRoot())
                    .setPositiveButton("ok", (dialogInterface, which) -> {
                        String name = dialog.name.getText().toString();
                        String packageName = dialog.packageName.getText().toString();
                        String link = dialog.link.getText().toString();
                        int code = Integer.parseInt(dialog.versionCode.getText().toString());

                        if (packages != null && packages.contains(packageName)) {
                            Toast.makeText(getContext(), "App already exist", Toast.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(getActivity().findViewById(android.R.id.content), "Adding versioned app...", Snackbar.LENGTH_LONG).show();
                            try {
                                JSONObject object = new JSONObject();
                                object.put("name", name);
                                object.put("versionCode", code);
                                object.put("link", link);
                                object.put("mode", "flexible");

                                Utils.create(object.toString().getBytes(), MainActivity.CURRENT_PROJECT + "/updates/" + packageName + ".json", new Utils.AsyncCallback() {
                                    @Override
                                    public void onComplete() {
                                        names.add(name);
                                        packages.add(packageName);
                                        codes.add(code);

                                        adapter.notifyDataSetChanged();
                                        Toast.makeText(getContext(), "App version added", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Toast.makeText(getContext(), "Error adding app", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (JSONException ignored) {
                                Toast.makeText(getContext(), "Impossible glitch!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).setNegativeButton("cancel", null).show();
        });
        return binding.getRoot();
    }

    private void populateList() {
        Toast.makeText(getContext(), "Fetching list...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                var files = Utils.repo.getDirectoryContent(MainActivity.CURRENT_PROJECT + "/updates");
                for (GHContent file : files) {
                    JSONObject object = new JSONObject(file.getContent());
                    names.add(object.getString("name"));
                    packages.add(file.getName().replace(".json", ""));
                    codes.add(object.getInt("versionCode"));
                }
                getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }
}