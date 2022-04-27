package com.clorabase.console.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.adapters.DatabaseListAdapter;
import com.clorabase.console.databinding.FragmentDatabaseBinding;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import apis.xcoder.easydrive.AsyncTask;
import db.clorabase.clorem.Node;

public class DatabaseFragment extends Fragment {
    public static Node db;
    DatabaseListAdapter adapter;
    List<String> names;
    List<String> packageNames;
    public static List<String> ids;
    List<Integer> sizes = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentDatabaseBinding binding = FragmentDatabaseBinding.inflate(inflater);

        ImageView image = new ImageView(getContext());
        image.setImageResource(R.drawable.empty_list);
        db = MainActivity.db.node("Database");
        names = db.getListString("names");
        packageNames = db.getListString("packages");
        ids = db.getListString("ids");
        adapter = new DatabaseListAdapter(requireContext(), names, packageNames, sizes);

        populateList();
        ((ViewGroup) binding.appsList.getParent()).addView(image);
        binding.appsList.setEmptyView(image);
        binding.appsList.setAdapter(adapter);

        binding.add.setOnClickListener(v -> {
            if (packageNames.size() > 10){
                binding.add.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Max apps limit reached.", Toast.LENGTH_LONG).show();
                return;
            }

            View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_common, null);
            EditText mName = view.findViewById(R.id.app_name);
            EditText mPackage = view.findViewById(R.id.app_package);
            new AlertDialog.Builder(requireContext())
                    .setTitle("Add a new app")
                    .setView(view)
                    .setPositiveButton("Add", (dialog, which) -> {
                        String name = mName.getText().toString();
                        String packageName = mPackage.getText().toString();
                        if (name.isEmpty() || packageName.isEmpty())
                            Toast.makeText(getContext(), "Please enter app details", Toast.LENGTH_SHORT).show();
                        else if (names.contains(name) || packageNames.contains(packageName)) {
                            Toast.makeText(getContext(), "App already exist", Toast.LENGTH_SHORT).show();
                        } else {
                            Snackbar.make(getActivity().findViewById(android.R.id.content),"Adding database...", BaseTransientBottomBar.LENGTH_LONG).show();
                            MainActivity.configureFeature(getContext(), packageName + "/clorabase.db", call -> {
                                if (call.isSuccessful){
                                    names.add(name);
                                    packageNames.add(packageName);
                                    ids.add(call.result);
                                    sizes.add(0);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    call.exception.printStackTrace();
                                    Toast.makeText(getContext(), "Failed to create database", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }).setNegativeButton("Cancel", null).show();
        });
        return binding.getRoot();
    }


    private void populateList() {
        for (String id : ids){
            MainActivity.drive.getFileSize(id).setOnSuccessCallback(result -> {
                sizes.add((int) (result / 1024 * 1024));
                getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            });
        }
    }
}