package com.clorabase.console;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.clorabase.console.databinding.ActivityQuotaBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class QuotaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityQuotaBinding binding = ActivityQuotaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        new Thread(() -> {
            JSONObject json;
            try {
                var core = Utils.gitHub.getRateLimit().getCore();
                json = new JSONObject();
                json.put("limit", core.getLimit());
                json.put("remaining", core.getRemaining());
                json.put("reset", core.getResetDate().toString());
                var string = json.toString(4);
                runOnUiThread(() -> binding.quota.setText(string));
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(QuotaActivity.this, "An error occurred!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}