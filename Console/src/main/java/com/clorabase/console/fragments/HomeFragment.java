package com.clorabase.console.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.clorabase.console.AddProjectActivity;
import com.clorabase.console.Constants;
import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.databinding.FragmentHomeBinding;
import com.google.android.material.navigation.NavigationView;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        var manager = getParentFragmentManager();

        var listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                var transaction = manager.beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                transaction.addToBackStack(Constants.FRAGMENT_TAG);
                NavigationView drawer = ((MainActivity) getActivity()).drawer;
                ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                if (v == binding.database){
                    drawer.setCheckedItem(R.id.nav_db);
                    actionBar.setTitle("Clorabase database");
                    transaction.replace(R.id.fragment,new DatabaseFragment(),Constants.FRAGMENT_TAG).commit();
                } else if (v == binding.storage){
                    drawer.setCheckedItem(R.id.nav_storage);
                    actionBar.setTitle("Clorabase storage");
                    transaction.replace(R.id.fragment,new StorageFragment(),Constants.FRAGMENT_TAG).commit();
                } else if (v == binding.messaging){
                    drawer.setCheckedItem(R.id.nav_messaging);
                    actionBar.setTitle("In-app messaging");
                    transaction.replace(R.id.fragment,new InAppFragment(), Constants.FRAGMENT_TAG).commit();
                } else if (v == binding.push){
                    drawer.setCheckedItem(R.id.nav_push);
                    actionBar.setTitle("Push messaging");
                    transaction.replace(R.id.fragment,new PushFragment(),Constants.FRAGMENT_TAG).commit();
                } else if (v == binding.updates){
                    drawer.setCheckedItem(R.id.nav_update);
                    actionBar.setTitle("In-app updates");
                    transaction.replace(R.id.fragment,new UpdatesFragment(),Constants.FRAGMENT_TAG).commit();
                } else
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://errorxcode.github.io/docs/clorabase/index.html#")));
            }
        };


        manager.addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                var frag = manager.findFragmentByTag("currentFragment");
                if (getActivity() != null){
                    if (frag == null)
                        getActivity().setTitle("Home");
                    else
                        getActivity().setTitle(frag.getClass().getSimpleName().replace("Fragment", ""));
                }
            }
        });

        binding.documentation.setOnClickListener(listener);
        binding.database.setOnClickListener(listener);
        binding.storage.setOnClickListener(listener);
        binding.messaging.setOnClickListener(listener);
        binding.push.setOnClickListener(listener);
        binding.updates.setOnClickListener(listener);

        if (MainActivity.projects == null || MainActivity.projects.isEmpty()){
            startActivity(new Intent(getActivity(), AddProjectActivity.class));
        }

        return binding.getRoot();
    }
}