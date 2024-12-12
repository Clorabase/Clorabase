package com.clorabase.console;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }



    public void login(View view) {
        view.setActivated(false);
        view.setEnabled(false);
        var token = ((EditText) findViewById(R.id.token)).getText().toString();

        new Thread(() -> {
            // Trying to login
            try {
                var github = GitHub.connectUsingOAuth(token);
                var username = github.getMyself().getLogin();

                // Creating repo if not already exists
                try {
                    github.createRepository(Constants.REPO_NAME)
                            .owner(username)
                            .create()
                            .setDescription("This repo is created by clorabase console and is totally of internal use.");

                } catch (IOException ee){
                    ee.printStackTrace(); // Repo most probably already exists
                }

                setResult(RESULT_OK,new Intent().putExtra("token",token).putExtra("username",username));
                finish();
            } catch (IOException e) {
                // Login failed
                e.printStackTrace();
                runOnUiThread(() -> {
                    view.setEnabled(true);
                    if (e.getCause() instanceof UnknownHostException){
                        Toast.makeText(LoginActivity.this, "Check your internet connection!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Invalid token!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
}