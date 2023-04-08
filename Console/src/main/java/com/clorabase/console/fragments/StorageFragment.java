package com.clorabase.console.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.StorageListAdapter;
import com.clorabase.console.databinding.FragmentStorageBinding;
import com.clorabase.console.storage.ClorabaseStorage;
import com.clorabase.console.storage.ClorabaseStorageCallback;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StorageFragment extends Fragment {
    private StorageListAdapter listAdapter;
    private Iterator<ClorabaseStorage.File[]> iterator;
    private final Executor executor = Executors.newCachedThreadPool();
    private final List<ClorabaseStorage.File> files = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentStorageBinding binding = FragmentStorageBinding.inflate(inflater);

        ImageView image = new ImageView(getContext());
        image.setImageResource(R.drawable.empty_list);
        ((ViewGroup) binding.list.getParent()).addView(image);
        binding.list.setEmptyView(image);

        listAdapter = new StorageListAdapter(requireContext(), files);
        binding.list.setAdapter(listAdapter);


        try {
            ClorabaseStorage.list(MainActivity.CURRENT_PROJECT, iterator -> {
                if (iterator == null)
                    Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                else {
                    this.iterator = iterator;
                    if (iterator.hasNext()) {
                        var fileFetched = iterator.next();
                        Collections.addAll(files, fileFetched);
                    }
                    getActivity().runOnUiThread(() -> {
                        listAdapter = new StorageListAdapter(requireContext(), files);
                        binding.list.setAdapter(listAdapter);
                    });
                }
            });
        } catch (RuntimeException e) {
            executor.execute(() -> {
                try {
                    Utils.repo.createRelease(MainActivity.CURRENT_PROJECT)
                            .name("Storage bucket")
                            .body("This release is only for storing files for the project.")
                            .create();
                } catch (IOException ex) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "An Unexpected error occurred", Toast.LENGTH_SHORT).show());
                    getActivity().finish();
                }
            });
        }

        binding.list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                executor.execute(() -> {
                    if (iterator != null && iterator.hasNext() && totalItemCount - (firstVisibleItem + visibleItemCount) < 10) {
                        var fileFetched = iterator.next();
                        Collections.addAll(files, fileFetched);
                        getActivity().runOnUiThread(() -> listAdapter.notifyDataSetChanged());
                    }
                });
            }
        });

        binding.addFile.setOnClickListener(v -> startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("*/*"), 0));
        return binding.getRoot();
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
                var tempDir = System.getProperty("java.io.tmpdir");
                var file = new File(tempDir, filename);
                FileUtils.copyInputStreamToFile(in, file);
                String finalFilename = filename;
                ClorabaseStorage.upload(MainActivity.CURRENT_PROJECT, file, new ClorabaseStorageCallback() {
                    @Override
                    public void onFailed(@NonNull Exception e) {
                        dialog.dismiss();
                        Toast.makeText(getContext(), "Failed with an exception", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(int percent) {
                        dialog.setProgress(percent);
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        var url = "https://github.com/Clorabase-databases/OpenDatabases/releases/download/" + MainActivity.CURRENT_PROJECT + "/" + finalFilename;
                        files.add(new ClorabaseStorage.File(finalFilename, url, file.length()));
                        listAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "File uploaded", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                dialog.dismiss();
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private String[] fetch(){
        String[] files = new String[50];
        for (int i = 0; i < 50; i++) {
            SystemClock.sleep(100);
            files[i] = UUID.randomUUID().toString();
        }
        return files;
    }
}

