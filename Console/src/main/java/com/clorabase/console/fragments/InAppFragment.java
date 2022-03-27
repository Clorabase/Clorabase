package com.clorabase.console.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.clorabase.console.Utils;
import com.clorabase.console.databinding.FragmentInappBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class InAppFragment extends Fragment implements TextWatcher {
    FragmentInappBinding binding;
    String title, message, packageName, image, link;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentInappBinding.inflate(inflater);
        binding.title.addTextChangedListener(this);
        binding.message.addTextChangedListener(this);
        binding.image.addTextChangedListener(this);
        binding.packageName.addTextChangedListener(this);
        binding.send.setOnClickListener(v -> {
            link = binding.link.getText().toString();
            image = binding.image.getText().toString();

            JSONObject map = new JSONObject();
            try {
                map.put("title", title);
                map.put("message", message);
                map.put("image", image);
                map.put("link", link);
                map.put("type", "simple");
                Utils.helper.createFolderIfNotExist(packageName, Utils.clorabaseID).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        String folderId = task.getResult().getId();
                        String fileID = Utils.getFileId("messaging.json",folderId);
                        if (fileID == null) {
                            Utils.helper.createTextFile("messaging.json",map.toString(), folderId).addOnCompleteListener(fileTask -> {
                                if (fileTask.isSuccessful())
                                    Toast.makeText(getContext(), "Message sent", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(getContext(), fileTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            Utils.updateFile(fileID,map.toString()).addOnCompleteListener(fileTask -> {
                                if (fileTask.isSuccessful())
                                    Toast.makeText(getContext(), "Message sent", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(getContext(), fileTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                });
            } catch (JSONException e){
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        return binding.getRoot();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        title = binding.title.getText().toString();
        message = binding.message.getText().toString();
        packageName = binding.packageName.getText().toString();

        binding.send.setEnabled(!title.isEmpty() && !message.isEmpty() && !packageName.isEmpty());
    }

    @Override
    public void afterTextChanged(Editable s) {

    }
}