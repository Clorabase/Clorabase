package com.clorabase.console.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.clorabase.console.MainActivity;
import com.clorabase.console.R;
import com.clorabase.console.databinding.FragmentHomeBinding;
import com.google.android.material.navigation.NavigationView;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.documentation.setOnClickListener(this);
        binding.database.setOnClickListener(this);
        binding.storage.setOnClickListener(this);
        binding.messaging.setOnClickListener(this);
        binding.push.setOnClickListener(this);
        binding.updates.setOnClickListener(this);

        return binding.getRoot();
    }

    @Override
    public void onClick(View v) {
        FragmentTransaction manager = getParentFragmentManager().beginTransaction();
        manager.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        NavigationView drawer = MainActivity.drawer;
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (v == binding.database){
            drawer.setCheckedItem(R.id.nav_db);
            actionBar.setTitle("Clorabase database");
            manager.replace(R.id.fragment,new DatabaseFragment(),"currentFragment").commit();
        } else if (v == binding.storage){
            drawer.setCheckedItem(R.id.nav_storage);
            actionBar.setTitle("Clorabase storage");
            manager.replace(R.id.fragment,new StorageFragment(),"currentFragment").commit();
        } else if (v == binding.messaging){
            drawer.setCheckedItem(R.id.nav_messaging);
            actionBar.setTitle("In-app messaging");
            manager.replace(R.id.fragment,new InAppFragment(),"currentFragment").commit();
        } else if (v == binding.push){
            drawer.setCheckedItem(R.id.nav_push);
            actionBar.setTitle("Push messaging");
            manager.replace(R.id.fragment,new PushFragment(),"currentFragment").commit();
        } else if (v == binding.updates){
            drawer.setCheckedItem(R.id.nav_update);
            actionBar.setTitle("In-app updates");
            manager.replace(R.id.fragment,new UpdatesFragment(),"currentFragment").commit();
        } else
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://errorxcode.github.io/docs/clorabase/index.html#")));
    }
}