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

import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.UpdateListAdapter;
import com.clorabase.console.databinding.DialogAddUpdateBinding;
import com.clorabase.console.databinding.FragmentUpdatesBinding;
import com.clorem.db.Node;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UpdatesFragment extends Fragment {
    UpdateListAdapter adapter;
    Node db;
    List<Integer> codes = new ArrayList<>();
    public static List<String> ids;
    List<String> packages;
    List<String> names;
    List<String> links;
    List<String> versions;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentUpdatesBinding binding = FragmentUpdatesBinding.inflate(inflater);

        db = Utils.db.node("Updater");
        packages = db.getListString("packages");
        names = db.getListString("names");
        versions = db.getListString("versions");
        ids = db.getListString("ids");
        links = db.getListString("links");
        adapter = new UpdateListAdapter(getContext(),names,links,versions);
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
                        String version = dialog.version.getText().toString();
                        String link = dialog.link.getText().toString();
                        int code = Integer.parseInt(dialog.versionCode.getText().toString());
                        if (packages.contains(packageName)){
                            Toast.makeText(getContext(), "App already exist", Toast.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Adding versioned app...", BaseTransientBottomBar.LENGTH_LONG).show();
                            try {
                                JSONObject object = new JSONObject();
                                object.put("name",name);
                                object.put("versionCode",code);
                                object.put("link",link);
                                object.put("mode","flexible");

                                String fileId = Utils.getFileId(packageName, Utils.clorabaseID);
                                if (fileId == null) {
                                    Utils.helper.createFolder(packageName, Utils.clorabaseID).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Utils.helper.createTextFile("version.json", object.toString(), task.getResult().getId()).addOnCompleteListener(versionTask -> {
                                                if (versionTask.isSuccessful()) {
                                                    addData2list(packageName, name, code, version, link);
                                                    ids.add(task.getResult().getId());
                                                } else {
                                                    Toast.makeText(getContext(), "Failed to add app", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        } else {
                                            Toast.makeText(getContext(), "Failed to add app", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    String updateFile = Utils.getFileId("version.json", fileId);
                                    if (updateFile == null){
                                        Utils.helper.createTextFile("version.json", object.toString(), fileId).addOnCompleteListener(versionTask -> {
                                            if (versionTask.isSuccessful()) {
                                                addData2list(name, packageName, code, version, link);
                                                ids.add(versionTask.getResult().getId());
                                            }
                                        });
                                    } else {
                                        Utils.updateFile(updateFile,object.toString()).addOnCompleteListener(task -> {
                                           if (task.isSuccessful()) {
                                               addData2list(name, packageName, code, version, link);
                                           } else {
                                               Toast.makeText(getContext(), "Failed to add app", Toast.LENGTH_SHORT).show();
                                           }
                                        });
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }).setNegativeButton("cancel",null).show();
        });
        return binding.getRoot();
    }

    public void addData2list(String name, String packageName, int code, String version, String link){
        names.add(name);
        packages.add(packageName);
        codes.add(code);
        versions.add(version);
        links.add(link);
        db.putStringList("packages", packages);
        db.putStringList("names", names);
        db.putStringList("versions", versions);
        db.putStringList("ids", ids);
        db.putStringList("links", links);
        adapter.notifyDataSetChanged();
    }
}