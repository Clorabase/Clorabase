package com.clorabase.console.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
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
import androidx.core.provider.DocumentsContractCompat;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.adapters.StorageListAdapter;
import com.clorabase.console.databinding.DialogAddCommonBinding;
import com.clorabase.console.databinding.FragmentStorageBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import apis.xcoder.easydrive.AsyncTask;
import apis.xcoder.easydrive.EasyDrive;
import apis.xcoder.easydrive.FileMetadata;
import db.clorabase.clorem.Node;

public class StorageFragment extends Fragment implements View.OnClickListener {
    private FragmentStorageBinding binding;
    private StorageListAdapter listAdapter;
    private ArrayAdapter<String> spinnerAdapter;
    private Map<String, String> map;
    private List<String> files;
    private final List<String> packages = new ArrayList<>();
    private List<String> fileIds;
    private List<Integer> sizes;
    private Node db;
    private Node spinnerNode;
    private long size;
    private String currentApp;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStorageBinding.inflate(inflater);
        db = MainActivity.db.node("Storage");
        spinnerNode = db.node("spinner");
        fileIds = db.getListString("ids");
        files = db.getListString("names");
        sizes = (List) db.getListInt("sizes");
        map = spinnerNode.getData() == null ? new HashMap<>(0) : (Map) spinnerNode.getData();
        packages.addAll(map.keySet());

        listAdapter = new StorageListAdapter(requireContext(), files, fileIds, sizes);
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item,packages);
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
        binding.packages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                listAdapter.clear();
                populateList();
                currentApp = packages.get(position);
                db.put("selection", position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        MainActivity.drive.getFileSize(map.get(currentApp.toString())).setOnSuccessCallback(Size -> {
            size = Size;
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
            String position = (String) currentApp;
            if (spinnerAdapter.getCount() == 1) {
                binding.packages.setVisibility(View.GONE);
                binding.delete.setVisibility(View.GONE);
                binding.addFile.setVisibility(View.GONE);
            }
            MainActivity.delete(getContext(), map.get(position), task -> {
                if (task.isSuccessful) {
                    map.put(position, null);
                    packages.remove(position);
                    spinnerAdapter.notifyDataSetChanged();
                    listAdapter.clear();
                } else{
                    Toast.makeText(getContext(), "Failed to delete app", Toast.LENGTH_SHORT).show();
                    task.exception.printStackTrace();
                }
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
                                MainActivity.configureFeature(getContext(),pName + "/Storage",task -> {
                                    if (task.isSuccessful) {
                                        if (packages.size() == 0) {
                                            binding.addFile.setVisibility(View.VISIBLE);
                                            binding.packages.setVisibility(View.VISIBLE);
                                        }
                                        map.put(pName, task.result);
                                        packages.add(pName);
                                        spinnerAdapter.notifyDataSetChanged();
                                    } else
                                        Toast.makeText(getContext(), "Failed to create storage bucket", Toast.LENGTH_SHORT).show();
                                });
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
            dialog.setCancelable(true);
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
                MainActivity.drive.uploadFile(filename,in,map.get((String) currentApp), new EasyDrive.ProgressListener() {
                    @Override
                    public void onProgress(int percentage) {
                        getActivity().runOnUiThread(() -> dialog.setProgress(percentage));
                    }

                    @Override
                    public void onFinish(String fileId) {
                        getActivity().runOnUiThread(() -> {
                            dialog.dismiss();
                            files.add(finalFilename);
                            fileIds.add(fileId);
                            sizes.add(size);
                            listAdapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onFailed(Exception e) {
                        e.printStackTrace();
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
            MainActivity.drive.listFiles(map.get(currentApp)).setOnSuccessCallback(list -> {
                for (FileMetadata file : list) {
                    System.out.println(file.name);
                    files.add(file.name);
                    sizes.add((int) (file.size));
                    fileIds.add(file.id);
                }
                getActivity().runOnUiThread(() -> {
                    listAdapter.notifyDataSetChanged();
                });
            }).setOnErrorCallback(Throwable::printStackTrace);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        for (String key : map.keySet()) {
            spinnerNode.put(key, map.get(key));
        }
    }

    public interface ProgressListener {
        void onProgress(int percent);

        void onComplete(String fileId);

        void onFailed(Exception e);
    }
}

