package com.clorabase.console;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Date;
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
            binding.progress.setVisibility(View.VISIBLE);
            var name = binding.project.getText().toString();
            var doc = MainActivity.doc;
            CompletableFuture.supplyAsync(this::projectExists).thenAccept(exist -> runOnUiThread(() -> {
                if (!exist) {
                    // TODO: 16-04-2024 Increase capacity of the project
                    var lastTime = (long) doc.get("lastProjectTime", System.currentTimeMillis());
                    var timeDiff = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastTime);
                    if (timeDiff < -1 && false) {
                        Toast.makeText(AddProjectActivity.this, "You can only add 3 project per day", Toast.LENGTH_SHORT).show();
                        binding.add.setEnabled(false);
                    } else {
                        try {
                            JSONObject config = new JSONObject();
                            config.put("name", name);
                            config.put("authorization", Base64.encodeToString(Utils.encrypt(name, name), Base64.DEFAULT)); // TODO: 16-04-2024 LEss secure, need be be fixed
                            config.put("created", new Date().toString());
                            binding.progress.setVisibility(View.VISIBLE);

                            Utils.create(config.toString().getBytes(), name + "/config.json", new Utils.AsyncCallback() {
                                @Override
                                public void onComplete(String sha) {
                                    binding.progress.setVisibility(View.GONE);
                                    MainActivity.projects.add(name);
                                    doc.put("lastProjectTime", System.currentTimeMillis());
                                    doc.put("projects", MainActivity.projects);
                                    doc.put("lastProject", name);

                                    if (binding.storage.isChecked()) {
                                        binding.add.setEnabled(false);
                                        new Thread(() -> {
                                            try {
                                                Utils.repo.createRelease(name).name(name + " Storage bucket").create();
                                                runOnUiThread(() -> {
                                                    binding.add.setEnabled(true);
                                                    binding.progress.setVisibility(View.GONE);
                                                });
                                                doc.put("isStorageConfigured", true);
                                                startActivity(new Intent(AddProjectActivity.this, MainActivity.class));
                                                finish();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                                binding.progress.setVisibility(View.GONE);
                                                Toast.makeText(AddProjectActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                                Utils.reportBug(AddProjectActivity.this, e);
                                            }
                                        }).start();
                                    } else {
                                        startActivity(new Intent(AddProjectActivity.this, MainActivity.class));
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
                            e.printStackTrace();
                            Utils.reportBug(AddProjectActivity.this, e);
                        }
                    }
                } else {
                    MainActivity.projects.add(name);
                    doc.put("lastProject", name);
                    doc.put("projects", MainActivity.projects);
                    startActivity(new Intent(AddProjectActivity.this, MainActivity.class));
                    finish();
                }
            })).exceptionally(throwable -> {
                if (throwable != null) {
                    throwable.printStackTrace();
                    Utils.reportBug(AddProjectActivity.this, (Exception) throwable);
                }
                return null;
            });
        });
    }

    private boolean projectExists() {
        var projectName = binding.project.getText().toString();
        if (projectName.isEmpty()) {
            binding.project.setError("Project name cannot be empty");
            return false;
        }
        if (MainActivity.projects.contains(projectName)) {
            binding.storage.setVisibility(View.GONE);
            binding.project.setError("Project already exists");
            return true;
        }

        return Utils.exists(projectName + "/config.json");
    }
}