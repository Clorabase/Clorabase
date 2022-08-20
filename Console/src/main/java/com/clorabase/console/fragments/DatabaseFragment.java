package com.clorabase.console.fragments;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.databinding.FragmentDatabaseBinding;

public class DatabaseFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentDatabaseBinding binding = FragmentDatabaseBinding.inflate(inflater);

        ImageView image = new ImageView(getContext());
        image.setImageResource(R.drawable.empty_list);
        binding.databaseView.setMovementMethod(new ScrollingMovementMethod());

        var path = MainActivity.CURRENT_PROJECT + "/db/config.json";
        if (!Utils.exists(path)) {
            Utils.create(new byte[0], path, new Utils.AsyncCallback() {
                @Override
                public void onComplete() {
                    Toast.makeText(getContext(), "Database created", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error creating database", Toast.LENGTH_SHORT).show();
                }
            });
        }

        binding.fetch.setOnClickListener(v -> {
            var address = MainActivity.CURRENT_PROJECT + "/db/" + binding.address.getText().toString();
            if (address.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a path", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Fetching document", Toast.LENGTH_SHORT).show();
                Utils.read(address, bytes -> {
                    if (bytes == null || bytes.length == 0)
                        Toast.makeText(getContext(), "Document does not exist", Toast.LENGTH_SHORT).show();
                    else
                        binding.databaseView.setText(new String(bytes));
                });
            }
        });
        return binding.getRoot();
    }
}