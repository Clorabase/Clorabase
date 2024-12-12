package com.clorabase.console.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.GithubFilesAdapter;
import com.clorabase.console.databinding.FragmentDatabaseBinding;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseFragment extends Fragment {
    public final List<GithubFilesAdapter.File> files = new ArrayList<>();
    public GithubFilesAdapter adapter;
    public final String BASE_PATH = MainActivity.CURRENT_PROJECT + "/db/";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentDatabaseBinding binding = FragmentDatabaseBinding.inflate(inflater);


        adapter = new GithubFilesAdapter(this, files);
        binding.list.setAdapter(adapter);

        var progress = new ProgressDialog(getContext());
        progress.setTitle("Please wait");
        progress.setMessage("Fetching your database....");
        progress.show();


        new Thread(() -> {
            try {
                files.addAll(Utils.listFiles(BASE_PATH));
                Utils.handler.post(() -> {
                    adapter.notifyDataSetChanged();
                    progress.dismiss();
                });
            } catch (IOException | NullPointerException e) {
                if (e instanceof IOException){
                    Utils.handler.post(() -> {
                        progress.dismiss();
                        if (e instanceof UnknownHostException)
                            Toast.makeText(binding.getRoot().getContext(), "Check your internet connection !", Toast.LENGTH_SHORT).show();
                        else if (e instanceof FileNotFoundException)
                            Toast.makeText(binding.getRoot().getContext(), "Start by creating a document", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(binding.getRoot().getContext(), "An error occurred. Please report on the github", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
        return binding.getRoot();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 11 && resultCode == Activity.RESULT_OK) {
            var file = ((GithubFilesAdapter.File) data.getSerializableExtra("file"));
            if (file != null) {
                files.add(file);
                adapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Doc created", Toast.LENGTH_SHORT).show();
            }
        }
    }
}