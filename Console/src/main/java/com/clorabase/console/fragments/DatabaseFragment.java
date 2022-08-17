package com.clorabase.console.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.Utils;
import com.clorabase.console.databinding.FragmentDatabaseBinding;

import org.json.JSONException;
import org.json.JSONObject;

public class DatabaseFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentDatabaseBinding binding = FragmentDatabaseBinding.inflate(inflater);

        ImageView image = new ImageView(getContext());
        image.setImageResource(R.drawable.empty_list);
        binding.databaseView.setMovementMethod(new ScrollingMovementMethod());

        var dialog = new ProgressDialog(getContext());
        dialog.setTitle("Fetching database");
        dialog.setMessage("Please wait will be render your database preview");
        dialog.show();
        if (Utils.exists(MainActivity.CURRENT_PROJECT + "/clorem.db")) {
            AndroidNetworking.get("https://clorabase.herokuapp.com/clorem/" + MainActivity.CURRENT_PROJECT + "/preview")
                    .build()
                    .getAsJSONObject(new JSONObjectRequestListener() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            try {
                                binding.databaseView.setText(jsonObject.toString(3));
                                dialog.dismiss();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ANError anError) {
                            System.out.println(anError.getErrorBody());
                            dialog.dismiss();
                            Toast.makeText(getContext(), "Error fetching database : " + anError.getErrorBody(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            binding.databaseView.setText("Database is just created..... Please put some data and see again");
            Utils.create(new byte[0], MainActivity.CURRENT_PROJECT + "/clorem.db", new Utils.AsyncCallback() {
                @Override
                public void onComplete() {
                    Toast.makeText(getContext(), "Database is created", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Error creating database", Toast.LENGTH_SHORT).show();
                }
            });
        }
        return binding.getRoot();
    }
}