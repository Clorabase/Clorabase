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
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clorabase.console.ClorabaseStorage;
import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.StorageListAdapter;
import com.clorabase.console.databinding.FragmentStorageBinding;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StorageFragment extends Fragment {
    private FragmentStorageBinding binding;
    private StorageListAdapter listAdapter;
    private List<String> fileNames = new ArrayList<>();
    private final List<String> sizes = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStorageBinding.inflate(inflater);

        listAdapter = new StorageListAdapter(requireContext(), fileNames, sizes);
        ImageView imageView = new ImageView(getContext());

        binding.list.setAdapter(listAdapter);
        imageView.setImageResource(R.drawable.empty_list);
        ((ViewGroup) binding.list.getParent()).addView(imageView);
        binding.list.setEmptyView(imageView);
        populateList();

        if (listAdapter.getCount() > 100)
            binding.addFile.setVisibility(View.GONE);

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
                var file = File.createTempFile(filename, ".file");
                file.renameTo(new File(file.getParentFile(), filename));
                FileUtils.copyInputStreamToFile(in, file);
                String finalFilename = filename;
                ClorabaseStorage.upload(MainActivity.CURRENT_PROJECT, file, new ClorabaseStorage.ClorabaseStorageCallback() {
                    @Override
                    public void onFailed(@NonNull Exception e) {
                        dialog.dismiss();
                        e.printStackTrace();
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onProgress(int prcnt) {
                        dialog.setProgress(prcnt);
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                        fileNames.add(finalFilename);
                        sizes.add(file.length()/(1024*1024) + " MB");
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


    public void populateList() {
        Utils.read(MainActivity.CURRENT_PROJECT + "/storage.prop", bytes -> {
            if (bytes == null || bytes.length == 0)
                return;

            var lines = new String(bytes).split("\n");
            new Thread(() -> {
                for (var line : lines) {
                    var name = line.split("=")[0];
                    var value = line.split("=")[1];
                    var size = Utils.getFileSize(value);
                    fileNames.add(name);
                    sizes.add(size);
                }
                ((Activity) getContext()).runOnUiThread(() -> listAdapter.notifyDataSetChanged());
            }).start();
        });
    }


    public interface ProgressListener {
        void onProgress(int percent);

        void onComplete(String fileId);

        void onFailed(Exception e);
    }
}

