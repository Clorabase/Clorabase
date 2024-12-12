package com.clorabase.console.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;

import com.clorabase.console.Constants;
import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.StorageListAdapter;
import com.clorabase.console.databinding.FragmentStorageBinding;
import com.clorabase.console.storage.ClorabaseStorage;
import com.clorabase.console.storage.ClorabaseStorageCallback;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
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

        boolean isStorage = MainActivity.db.document("storageConfiguration").getBoolean(MainActivity.CURRENT_PROJECT, false);
        if (!isStorage) {
            Toast.makeText(getContext(), "Storage is not configured", Toast.LENGTH_SHORT).show();
            binding.addFile.setEnabled(false);
            return binding.getRoot();
        }


        var progress = new ProgressDialog(getContext());
        progress.setTitle("Please wait");
        progress.setMessage("Fetching your storage....");
        progress.show();

        try {
            ClorabaseStorage.list(MainActivity.CURRENT_PROJECT, iterator -> {
                if (iterator == null) {
                    Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    progress.dismiss();
                } else {
                    this.iterator = iterator;
                    executor.execute(() -> {
                        var mFiles = iterator.next();
                        Utils.handler.post(() -> {
                            files.addAll(Arrays.asList(mFiles));
                            progress.dismiss();
                            listAdapter = new StorageListAdapter(requireContext(), files);
                            binding.list.setAdapter(listAdapter);
                        });
                    });
                }
            });
        } catch (RuntimeException e) {
            e.printStackTrace();
            executor.execute(() -> {
                try {
                    Utils.repo.createRelease(MainActivity.CURRENT_PROJECT)
                            .name("Storage bucket")
                            .body("This release is only for storing files for the project.")
                            .create();
                    Utils.handler.post(progress::dismiss);
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
            dialog.setCancelable(false);
            dialog.show();

            var edittext = new EditText(requireContext());
            edittext.setHint("Eg. /photos/user659");
            new AlertDialog.Builder(requireContext())
                    .setTitle("Enter file path")
                    .setView(edittext)
                    .setCancelable(false)
                    .setMessage("Enter the path of the storage folder where you wanna upload this file")
                    .setPositiveButton("Upload at this location", (d, which) -> {
                        var name = edittext.getText().toString().trim();
                        try {
                            var path = Paths.get(name).toString();
                            if (path.startsWith("/"))
                                path = path.substring(1);

                            if (path.endsWith("/"))
                                path = path.substring(0,path.length()-1);

                            if (path.isEmpty() || path.equals("/"))
                                path = "root";

                            uploadFile(data, dialog, path);
                        } catch (InvalidPathException e) {
                            Toast.makeText(getContext(), "Invalid path", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void uploadFile(@Nullable Intent data, ProgressDialog dialog, String path) {
        // Getting filename
        String filename;
        String resFileName;
        Uri uri = data.getData();
        if (uri.toString().startsWith("file"))
            filename = data.getData().getLastPathSegment();
        else {
            Cursor cursor = getContext().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                cursor.close();
            } else {
                Toast.makeText(getContext(), "Cannot retrieve filename", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        filename = filename.replace('_','-').replace(' ','-');
        resFileName = path.replace('/','_') + "_" + filename;

        // Copying file to temp directory, as ClorabaseStorage only accept file as argument
        try {
            InputStream in = getContext().getContentResolver().openInputStream(uri);
            var tempDir = System.getProperty("java.io.tmpdir");
            var file = new File(tempDir, resFileName);
            FileUtils.copyInputStreamToFile(in, file);

            // Uploading file to release assets
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
                    var user = MainActivity.db.document("config").getString("username", null);
                    var url = MessageFormat.format(Constants.RELEASE_DOWNLOAD_URL, user,MainActivity.CURRENT_PROJECT) + resFileName;
                    files.add(new ClorabaseStorage.File(resFileName, path, url, file.length()));

                    // Getting structure.json and updating it.
                    executor.execute(() -> {
                        try {
                            updateStructure(jsonObject -> {
                                try {
                                    gotoPath(jsonObject, path).put(finalFilename,url);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }, new Utils.AsyncCallback() {
                                @Override
                                public void onComplete(@Nullable String sha) {
                                    dialog.dismiss();
                                    listAdapter.notifyDataSetChanged();
                                    Toast.makeText(getContext(), "File uploaded", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), "Failed to update storage structure", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            dialog.dismiss();
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private String[] fetch() {
        String[] files = new String[50];
        for (int i = 0; i < 50; i++) {
            SystemClock.sleep(100);
            files[i] = UUID.randomUUID().toString();
        }
        return files;
    }

    public static void updateStructure(Consumer<JSONObject> callable, Utils.AsyncCallback callback) throws IOException {
        new Thread(() -> {
            try {
                // Reading the file/json
                var in = Utils.repo.getFileContent(MainActivity.CURRENT_PROJECT + Constants.PATH_STORAGE_JSON).read();
                byte[] bytes = new byte[in.available()];
                IOUtils.readFully(in, bytes);
                in.close();


                JSONObject json = new JSONObject(new String(bytes));
                callable.accept(json);
                // Pushing the changes back to the file on server
                Utils.update(json.toString().getBytes(),MainActivity.CURRENT_PROJECT + Constants.PATH_STORAGE_JSON,callback);
            } catch (JSONException | IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public static JSONObject gotoPath(@NonNull JSONObject obj, String path) throws JSONException {
        JSONObject current = obj;
        var paths = path.split("/");
        for (String path1 : paths) {
            if (!current.has(path1)){
                current.put(path1, new JSONObject());
            }
            current = current.getJSONObject(path1);

        }
        return current;
    }
}

