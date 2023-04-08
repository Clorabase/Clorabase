package com.clorabase.console;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import android.content.Intent;
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
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AddProjectActivity extends AppCompatActivity {
    private JSONObject config;
    private boolean isProjectChecked;
    private final ActivityAddProjectBinding binding = ActivityAddProjectBinding.inflate(getLayoutInflater());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);



        EditText project = findViewById(R.id.project);
        EditText key = findViewById(R.id.key);
        Button add = findViewById(R.id.add);
        ProgressBar progressBar = findViewById(R.id.progress);
        CheckBox storage = findViewById(R.id.storage);

        project.setOnEditorActionListener((v, actionId, event) -> {
            isProjectChecked = true;
            checkProject();
            return true;
        });

        key.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                add.setEnabled(s.toString().length() == 16);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        add.setOnClickListener(v -> {
            if (!isProjectChecked)
                checkProject();

            try {
                var doc = Clorastore.getInstance(getFilesDir(), "main").getDatabase().document("projects");
                var name = project.getText().toString();
                var decodeKey = key.getText().toString();
                if (config == null){
                    var lastTime = (long) doc.get("lastProjectTime",System.currentTimeMillis());
                    var timeDiff = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastTime);
                    if (timeDiff < -1){
                        Toast.makeText(this, "You can only add one project per day", Toast.LENGTH_SHORT).show();
                        add.setEnabled(false);
                    } else {
                        config = new JSONObject();
                        config.put("name",name);
                        config.put("authorization",encrypt(name,decodeKey));
                        config.put("created",new Date().toString());
                        progressBar.setVisibility(View.VISIBLE);
                        Utils.create(config.toString().getBytes(), name + "/config.json", new Utils.AsyncCallback() {
                            @Override
                            public void onComplete() {
                                progressBar.setVisibility(View.GONE);
                                MainActivity.projects.add(name);
                                doc.put("lastProjectTime",System.currentTimeMillis());
                                doc.put("names",MainActivity.projects);
                                doc.put("lastProject",name);
                                if (storage.isChecked()){
                                    add.setEnabled(false);
                                    new Thread(() -> {
                                        try {
                                            Utils.repo.createRelease(name).name(name + " Storage bucket").create();
                                            add.setEnabled(true);
                                            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                                            startActivity(new Intent(AddProjectActivity.this,MainActivity.class));
                                            finish();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Toast.makeText(AddProjectActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                            Utils.reportBug(AddProjectActivity.this,e);
                                        }
                                    }).start();
                                } else {
                                    startActivity(new Intent(AddProjectActivity.this,MainActivity.class));
                                    finish();
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                                Utils.reportBug(AddProjectActivity.this,e);
                                Toast.makeText(AddProjectActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    var token = config.getString("authorization");
                    if (decrypt(token,decodeKey)){
                        MainActivity.projects.add(name);
                        doc.put("lastProject",name);
                        doc.put("names",MainActivity.projects);
                        startActivity(new Intent(AddProjectActivity.this,MainActivity.class));
                        finish();
                    } else
                        Toast.makeText(this, "Wrong key", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utils.reportBug(this,e);
            }
        });
    }

    private void checkProject() {
        var projectName = binding.project.getText().toString();
        if (projectName.isEmpty()) {
            binding.project.setError("Project name cannot be empty");
        }
        if (MainActivity.projects.contains(projectName)) {
            binding.storage.setVisibility(View.GONE);
            binding.project.setError("Project already exists");
        }
        if (Utils.exists(projectName + "/config.json")) {
            binding.storage.setVisibility(View.GONE);
            Utils.read(projectName + "/config.json", bytes -> {
                try {
                    if (bytes == null){
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        finish();
                    } else
                        config = new JSONObject(new String(bytes));
                } catch (JSONException e) {
                    Utils.reportBug(AddProjectActivity.this,e);
                }
            });

            binding.add.setText("Add project");
        } else {
            Toast.makeText(AddProjectActivity.this, "Click on right button from the keyboard", Toast.LENGTH_SHORT).show();
        }
    }

    private static String encrypt(String str, String key) throws Exception {
        Key keySpec = new SecretKeySpec(key.getBytes(),"AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE,keySpec);
        var encrypted = cipher.doFinal(str.getBytes());
        return Base64.encodeToString(encrypted,Base64.DEFAULT);
    }

    public static boolean decrypt(String str, String key) throws Exception {
        try {
            Key keySpec = new SecretKeySpec(key.getBytes(),"AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,keySpec);
            cipher.doFinal(Base64.decode(str,Base64.DEFAULT));
            return true;
        } catch (InvalidKeyException | BadPaddingException e){
            return false;
        }
    }
}