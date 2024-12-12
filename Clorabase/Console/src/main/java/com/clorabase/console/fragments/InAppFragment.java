package com.clorabase.console.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.Utils;
import com.clorabase.console.databinding.FragmentInappBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;
import org.kohsuke.github.HttpException;

import java.io.IOException;

public class InAppFragment extends Fragment {
    FragmentInappBinding binding;
    String title, message, channel, image, link;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        var watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                title = binding.title.getText().toString();
                message = binding.message.getText().toString();
                channel = binding.channel.getText().toString();

                binding.send.setEnabled(!title.isEmpty() && !message.isEmpty() && !channel.isEmpty());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        binding = FragmentInappBinding.inflate(inflater);
        binding.title.addTextChangedListener(watcher);
        binding.message.addTextChangedListener(watcher);
        binding.image.addTextChangedListener(watcher);
        binding.channel.addTextChangedListener(watcher);

        binding.send.setOnClickListener(v -> {
            var path = MainActivity.CURRENT_PROJECT + "/messages/" + channel + ".json";
            link = binding.link.getText().toString();
            try {
                JSONObject map = new JSONObject();
                map.put("title", title);
                map.put("message", message);
                map.put("image", image);
                map.put("link", link);
                map.put("type", "simple");

                Utils.create(map.toString().getBytes(), path, new Utils.AsyncCallback() {
                    @Override
                    public void onComplete(String sha) {
                        Snackbar.make(binding.getRoot(), "Message sent!", Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (e instanceof HttpException && e.getMessage().contains("already")){
                            Utils.delete(path, new Utils.AsyncCallback() {
                                @Override
                                public void onComplete(String sha) {
                                    binding.send.performClick();
                                }

                                @Override
                                public void onError(Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        else
                            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (JSONException e) {
                Toast.makeText(getContext(), "Horrible glitch !", Toast.LENGTH_SHORT).show();
            }
        });

        binding.image.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),0);
        });
        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            var in = getContext().getContentResolver().openInputStream(data.getData());
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            image = Base64.encodeToString(bytes, Base64.DEFAULT);
            Toast.makeText(getContext(), "Image chosen", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to pick image. IOError", Toast.LENGTH_LONG).show();
        }
    }
}