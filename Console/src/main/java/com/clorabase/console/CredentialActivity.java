package com.clorabase.console;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
                        try {
//                            String project = MainActivity.drive.getFileId(packageName,MainActivity.clorabaseID).getResult(50);
                            if ("project" == null) {
                                Toast.makeText(this, "No project found for the package " + packageName, Toast.LENGTH_LONG).show();
                            } else {
//                                var storage = MainActivity.drive.getFileId("Storage",project).getResult(5);
//                                var updates = MainActivity.drive.getFileId("versions.json",project).getResult(5);
//                                var messaging = MainActivity.drive.getFileId("messaging.json",project).getResult(5);
//                                var database = MainActivity.drive.getFileId("clorabase.db",project).getResult(5);
//                                if (storage != null)
//                                    binding.storage.setText(storage);
//                                if (database != null)
//                                    binding.database.setText(database);
//                                if (messaging != null)
//                                    binding.messaging.setText(messaging);
//                                if (updates != null)
//                                    binding.updates.setText(updates);
//
//                                binding.token.setText(MainActivity.TOKEN);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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