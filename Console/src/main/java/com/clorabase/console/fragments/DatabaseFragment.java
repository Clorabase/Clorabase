package com.clorabase.console.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.adapters.GithubFilesAdapter;
import com.clorabase.console.databinding.FragmentDatabaseBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class DatabaseFragment extends Fragment {
    public static final String BASE_PATH = MainActivity.CURRENT_PROJECT + "/db/";
    private final String BASE_URL = "https://api.github.com/repos/Clorabase-databases/OpenDatabases/git/trees/main:";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentDatabaseBinding binding = FragmentDatabaseBinding.inflate(inflater);

        var files = new ArrayList<GithubFilesAdapter.File>();
        var adapter = new GithubFilesAdapter(getContext(),files);
        binding.list.setAdapter(adapter);

        var progress = new ProgressDialog(getContext());
        progress.setTitle("Please wait");
        progress.setMessage("Fetching your database....");
        progress.show();

        new Thread(() -> {
            try {
                var in = new URL(BASE_URL + BASE_PATH).openStream();
                String str = "";
                Scanner scanner = new Scanner(in);
                while (scanner.hasNext())
                    str += scanner.nextLine();
                scanner.close();

                var root = new JSONObject(str).getJSONArray("tree");
                for (int i = 0; i < root.length(); i++) {
                    var obj = root.getJSONObject(i);
                    var isFile = obj.getString("type").equals("blob");
                    var icon = isFile ? R.drawable.ic_file : R.drawable.ic_folder;
                    var name = obj.optString("path");
                    var sha = obj.getString("sha");
                    var file = new GithubFilesAdapter.File(isFile,icon,name,sha,"");
                    files.add(file);
                }
                getActivity().runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    progress.dismiss();
                });
            } catch (IOException | JSONException e) {
               getActivity().runOnUiThread(() -> {
                   progress.dismiss();
                   if (e instanceof UnknownHostException)
                       Toast.makeText(getContext(), "Check your internet connection !", Toast.LENGTH_SHORT).show();
               });
            }
        }).start();
        return binding.getRoot();
    }
}