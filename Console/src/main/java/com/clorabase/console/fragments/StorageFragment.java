package com.clorabase.console.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ammarptn.gdriverest.GoogleDriveFileHolder;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.StorageListAdapter;
import com.clorabase.console.databinding.DialogAddCommonBinding;
import com.clorabase.console.databinding.FragmentStorageBinding;
import com.clorem.db.Node;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageFragment extends Fragment implements View.OnClickListener {
    private FragmentStorageBinding binding;
    private StorageListAdapter listAdapter;
    private ArrayAdapter<String> spinnerAdapter;
    private final Map<String, String> map = new HashMap<>();
    private List<String> files;
    private final List<String> packages = new ArrayList<>();
    private List<String> fileIds;
    private List<Integer> sizes;
    private Node db;
    private Node spinnerNode;
    private long size;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStorageBinding.inflate(inflater);
        db = Utils.db.node("Storage");
        spinnerNode = db.node("spinner");
        fileIds = db.getListString("ids");
        files = db.getListString("names");
        sizes = db.getListInt("sizes");
        for (String app : spinnerNode.getChildren()) {
            map.put(app, spinnerNode.getString(app, null));
            packages.add(app);
        }
        listAdapter = new StorageListAdapter(requireContext(), files, fileIds, sizes);
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, packages);

        binding.list.setAdapter(listAdapter);
        binding.packages.setAdapter(spinnerAdapter);
        ImageView imageView = new ImageView(getContext());
        imageView.setImageResource(R.drawable.empty_list);
        ((ViewGroup) binding.list.getParent()).addView(imageView);
        binding.list.setEmptyView(imageView);

        if (listAdapter.getCount() > 100)
            binding.addFile.setVisibility(View.GONE);
        if (map.keySet().size() > 10)
            binding.packages.setVisibility(View.GONE);
        if (packages.size() == 0) {
            binding.addFile.setVisibility(View.GONE);
            binding.packages.setVisibility(View.GONE);
            binding.delete.setVisibility(View.GONE);
        }
        binding.packages.setSelection((int) db.getNumber("selection", 0));
        binding.addFile.setOnClickListener(this);
        binding.addPackage.setOnClickListener(this);
        binding.delete.setOnClickListener(this);

        Utils.helper.queryFiles(map.get((String) binding.packages.getSelectedItem())).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (GoogleDriveFileHolder holder : task.getResult()){
                    size += holder.getSize();
                }
                size = size / (1024*1024);
            }
        });

        populateList();
        binding.packages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listAdapter.clear();
                populateList();
                db.put("selection", position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        return binding.getRoot();
    }


    @Override
    public void onClick(View v) {
        if (v == binding.addFile) {
            if (size < 512) {
                startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), 0);
            } else
                Toast.makeText(getContext(), "Your storage quota is full", Toast.LENGTH_SHORT).show();
        } else if (v == binding.delete) {
            String position = (String) binding.packages.getSelectedItem();
            if (spinnerAdapter.getCount() == 1) {
                binding.packages.setVisibility(View.GONE);
                binding.delete.setVisibility(View.GONE);
                binding.addFile.setVisibility(View.GONE);
            }
            Utils.helper.deleteFolderFile(map.get(position)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    map.put(position,null);
                    packages.remove(position);
                    spinnerAdapter.notifyDataSetChanged();
                } else
                    Toast.makeText(getContext(), "Failed to delete app", Toast.LENGTH_SHORT).show();
            });
        } else if (v == binding.addPackage) {
            DialogAddCommonBinding dialogBinding = DialogAddCommonBinding.inflate(getLayoutInflater());
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Add app")
                    .setView(dialogBinding.getRoot())
                    .setPositiveButton("add", (dialog, which) -> {
                        String name = dialogBinding.appName.getText().toString();
                        String pName = dialogBinding.appPackage.getText().toString();
                        if (name.isEmpty() || pName.isEmpty())
                            Toast.makeText(requireContext(), "Please fill app details", Toast.LENGTH_SHORT).show();
                        else {
                            if (map.containsKey(pName))
                                Toast.makeText(getContext(), "Package name already exist in database", Toast.LENGTH_SHORT).show();
                            else {
                                Snackbar.make(getActivity().findViewById(android.R.id.content), "Adding app...", BaseTransientBottomBar.LENGTH_LONG).show();
                                String folder = Utils.getFileId(pName, Utils.clorabaseID);
                                if (folder == null) {
                                    Utils.helper.createFolder(pName, Utils.clorabaseID).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Utils.helper.createFolder("Storage", task.getResult().getId()).addOnCompleteListener(finakTask -> {
                                                if (finakTask.isSuccessful()) {
                                                    if (packages.size() == 0) {
                                                        binding.addFile.setVisibility(View.VISIBLE);
                                                        binding.packages.setVisibility(View.VISIBLE);
                                                    }
                                                    map.put(pName, finakTask.getResult().getId());
                                                    packages.add(pName);
                                                    binding.packages.setSelection(packages.size() - 1);
                                                    spinnerAdapter.notifyDataSetChanged();
                                                } else
                                                    Toast.makeText(getContext(), "Error creating storage bucket", Toast.LENGTH_SHORT).show();
                                            });
                                        } else {
                                            Toast.makeText(getContext(), "Error adding app to clorabase", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    String storage = Utils.getFileId("Storage", folder);
                                    if (storage == null) {
                                        Utils.helper.createFolder("Storage", folder).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()){
                                                if (packages.size() == 0) {
                                                    binding.addFile.setVisibility(View.VISIBLE);
                                                    binding.packages.setVisibility(View.VISIBLE);
                                                }
                                                map.put(pName, task.getResult().getId());
                                                packages.add(pName);
                                                spinnerAdapter.notifyDataSetChanged();
                                            } else
                                                Toast.makeText(getContext(), "Failed to create storage bucket", Toast.LENGTH_SHORT).show();
                                        });
                                    } else {
                                        if (packages.size() == 0) {
                                            binding.addFile.setVisibility(View.VISIBLE);
                                            binding.packages.setVisibility(View.VISIBLE);
                                        }
                                        map.put(pName, storage);
                                        System.out.println("Storage id : " + storage);
                                        packages.add(pName);
                                        spinnerAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    }).setNegativeButton("candle", null).show();
        }
    }


    @SuppressLint("Range")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ProgressDialog dialog = new ProgressDialog(getContext());
            dialog.setMax(100);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setTitle("Uploading file");
            dialog.show();

            String filename = "Unnamed.file";
            Uri uri = data.getData();
            if (uri.toString().startsWith("file"))
                filename = data.getData().getLastPathSegment();
            else {
                Cursor cursor = getContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    cursor.close();
                } else
                    Toast.makeText(getContext(), "Cannot retrieve filename", Toast.LENGTH_SHORT).show();
            }
            try {
                InputStream in = getContext().getContentResolver().openInputStream(uri);
                int size = in.available() / 1024 * 1024;
                String finalFilename = filename;
                Utils.upload(in, filename, map.get((String) binding.packages.getSelectedItem()), new ProgressListener() {
                    @Override
                    public void onProgress(int percent) {
                        dialog.setProgress(percent);
                    }

                    @Override
                    public void onComplete(String fileId) {
                        dialog.dismiss();
                        files.add(finalFilename);
                        fileIds.add(fileId);
                        sizes.add(size);
                        listAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailed(Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                dialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void populateList() {
        if (map.size() > 0) {
            Utils.helper.queryFiles(map.get((String) binding.packages.getSelectedItem()))
                    .addOnSuccessListener(list -> {
                        for (GoogleDriveFileHolder file : list) {
                            files.add(file.getName());
                            sizes.add((int) (file.getSize() / 1024 * 1024));
                            fileIds.add(file.getId());
                        }
                        listAdapter.notifyDataSetChanged();
                    }).addOnFailureListener(e -> Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        for (String key : map.keySet()){
            spinnerNode.put(key, map.get(key));
        }
    }

    public interface ProgressListener {
        void onProgress(int percent);

        void onComplete(String fileId);

        void onFailed(Exception e);
    }
}

