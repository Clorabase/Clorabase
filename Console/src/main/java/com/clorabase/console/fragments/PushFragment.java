package com.clorabase.console.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.TextKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.clorabase.console.R;
import com.clorabase.console.databinding.FragmentPushBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class PushFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentPushBinding binding = FragmentPushBinding.inflate(inflater);

        TextWatcher listener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.send.setEnabled(!(binding.title.toString().isEmpty() || binding.message.getText().toString().isEmpty() ||
                        binding.action.getText().toString().isEmpty() || binding.channel.getText().toString().isEmpty()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        binding.message.addTextChangedListener(listener);
        binding.title.addTextChangedListener(listener);
        binding.action.addTextChangedListener(listener);
        binding.action.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.actions)));

        binding.action.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0){
                binding.action.setKeyListener(TextKeyListener.getInstance());
                binding.action.getText().clear();
                binding.spinner.setHint("Url to open");
                binding.action.setInputType(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS);
            } else {
                binding.spinner.setHint("Click action");
                binding.action.setKeyListener(null);
            }
        });

        binding.send.setOnClickListener(v -> {
            try {
                JSONObject json = new JSONObject();
                json.put("app_id","xxxxxxxxxxxxxxxxxxxxxxxxx");
                json.put("isAndroid",true);
                json.put("channel_for_external_user_ids","push");
                json.put("include_external_user_ids",new JSONArray(String.format("[\"%s\"]",binding.channel.getText().toString())));
                json.put("headings",new JSONObject().put("en",binding.title.getText().toString()));
                json.put("contents",new JSONObject().put("en",binding.message.getText().toString()));
                if (binding.spinner.getHint().toString().contains("Url")){
                    if (binding.action.getText().toString().contains("https://")){
                        json.put("app_url",binding.action.getText().toString());
                    } else {
                        binding.action.setError("URL must start with 'http://' or 'https://'");
                        return;
                    }
                }
                URL url = new URL("https://onesignal.com/api/v1/notifications");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setUseCaches(false);
                con.setDoOutput(true);
                con.setDoInput(true);
                con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                con.setRequestProperty("Authorization", "Basic MjZlYTYwMTctOGU0ZC00ZWU2LTkxNzctZjA4OTk1MmU4OTdm");
                con.setRequestMethod("POST");
                con.getOutputStream().write(json.toString().getBytes());

                if (con.getResponseCode() == 200)
                    Snackbar.make(binding.getRoot(),"Push notification sent !",Snackbar.LENGTH_SHORT).show();
                else
                    Toast.makeText(getContext(), "Push not sent, Error " + con.getResponseMessage(), Toast.LENGTH_LONG).show();
            } catch (JSONException | IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "An IO exception occurred", Toast.LENGTH_SHORT).show();
            }

        });
        return binding.getRoot();
    }
}