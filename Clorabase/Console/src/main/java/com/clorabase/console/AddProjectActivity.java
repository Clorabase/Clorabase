package com.clorabase.console;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.clorabase.clorastore.Clorastore;
import com.clorabase.console.databinding.ActivityAddProjectBinding;
import com.clorabase.console.storage.ClorabaseStorage;
import com.clorabase.console.storage.ClorabaseStorageCallback;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AddProjectActivity extends AppCompatActivity {
    private ActivityAddProjectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.add.setOnClickListener(v -> {
            binding.add.setEnabled(false);
            binding.progress.setVisibility(View.VISIBLE);
            var name = binding.project.getText().toString().trim().replace(" ", "_");

            // Checking if project exits or not
            if (MainActivity.projects.contains(name)){
                Toast.makeText(this, "Project already exists", Toast.LENGTH_SHORT).show();
                binding.add.setEnabled(true);
            } else {
                try {
                    JSONObject config = new JSONObject();
                    config.put("name", name);
                    config.put("authorization", Base64.encodeToString(Utils.encrypt(name, name), Base64.DEFAULT)); // TODO: 16-04-2024 can be improved
                    config.put("created", new Date().toString());
                    config.put("isStorageConfigured", binding.storage.isChecked());
                    binding.progress.setVisibility(View.VISIBLE);

                    // Creating the project
                    Utils.create(config.toString().getBytes(), name + "/config.json", new Utils.AsyncCallback() {
                        @Override
                        public void onComplete(String sha) {
                            var data = new Intent();
                            data.putExtra("project", name);
                            data.putExtra("storage", binding.storage.isChecked());

                            // Creating release for storage
                            if (binding.storage.isChecked()) {
                                binding.add.setEnabled(false);
                                new Thread(() -> {
                                    try {
                                        Utils.repo.createRelease(name).name(name + " Storage bucket").create();
                                        // Creating structure.json
                                        Utils.create("{}".getBytes(),name + Constants.PATH_STORAGE_JSON, new Utils.AsyncCallback() {
                                            @Override
                                            public void onComplete(@Nullable String sha) {
                                                binding.progress.setVisibility(View.GONE);
                                                setResult(RESULT_OK, data);
                                                finish();
                                            }

                                            @Override
                                            public void onError(Exception e) {
                                                e.printStackTrace();
                                            }
                                        });
                                    } catch (IOException e) {
                                        Utils.handler.post(() -> {
                                            binding.add.setEnabled(true);
                                            binding.progress.setVisibility(View.GONE);
                                            Toast.makeText(AddProjectActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            Utils.reportBug(AddProjectActivity.this, e);
                                        });
                                    }
                                }).start();
                            } else {
                                setResult(RESULT_OK, data);
                                finish();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            binding.progress.setVisibility(View.GONE);
                            Utils.reportBug(AddProjectActivity.this, e);
                            Toast.makeText(AddProjectActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    Utils.reportBug(this,e);
                }
            }
        });

        binding.project.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
            // filter text to only contain alphabet and numbers
            for (int i = start; i < end; i++) {
                if (!Character.isLetterOrDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        }});
    }

    private boolean projectExists() {
        var projectName = binding.project.getText().toString();
        if (projectName.isEmpty()) {
            binding.project.setError("Project name cannot be empty");
            return false;
        }
        if (MainActivity.projects != null && MainActivity.projects.contains(projectName)) {
            binding.storage.setVisibility(View.GONE);
            binding.project.setError("Project already exists");
            return true;
        }

        return Utils.exists(projectName + "/config.json");
    }
}