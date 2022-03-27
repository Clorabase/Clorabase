package com.clorabase.console;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.clorabase.console.databinding.ActivityCredentialBinding;

public class CredentialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCredentialBinding binding = ActivityCredentialBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EditText editText = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Enter app package name")
                .setMessage("Enter the package name of the app you want to get the credential of")
                .setView(editText)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String packageName = editText.getText().toString();
                    if (packageName.isEmpty()) {
                        Toast.makeText(this, "Please enter a package name", Toast.LENGTH_SHORT).show();
                    } else {
                        String project = Utils.getFileId(packageName,Utils.clorabaseID);
                        if (project == null) {
                            Toast.makeText(this, "No project found for the package " + packageName, Toast.LENGTH_LONG).show();
                        } else {
                            String storage = Utils.getFileId("Storage",project);
                            if (storage == null) {
                                Toast.makeText(this, "No storage found for the project " + project, Toast.LENGTH_LONG).show();
                                binding.storage.setText("STORAGE_BUCKET_NOT_CONFIGURED");
                                binding.storage.setTextColor(Color.RED);
                            } else {
                                binding.storage.setText(storage);
                            }
                            binding.token.setText(Utils.TOKEN);
                            binding.project.setText(project);
                        }
                    }
                }).setNegativeButton("Cancel", (x,y) -> finish()).show();
    }

    public void copy(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("credential", ((TextView) view).getText().toString());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
    }
}