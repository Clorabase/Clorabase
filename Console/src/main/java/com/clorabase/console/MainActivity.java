package com.clorabase.console;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.clorabase.console.databinding.ActivityMainBinding;
import com.clorabase.console.fragments.DatabaseFragment;
import com.clorabase.console.fragments.HomeFragment;
import com.clorabase.console.fragments.InAppFragment;
import com.clorabase.console.fragments.PushFragment;
import com.clorabase.console.fragments.StorageFragment;
import com.clorabase.console.fragments.UpdatesFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    public Fragment fragment;
    public static NavigationView drawer;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        DrawerLayout layout = findViewById(R.id.drawer_layout);
        drawer = findViewById(R.id.drawer);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, layout, toolbar, R.string.app_name, R.string.app_name);
        layout.addDrawerListener(toggle);
        toggle.syncState();
        Utils.init(this);
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();

        drawer.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_db -> {
                    getSupportActionBar().setTitle("Database");
                    fragment = new DatabaseFragment();
                }
                case R.id.nav_storage -> {
                    getSupportActionBar().setTitle("Storage");
                    fragment = new StorageFragment();
                }
                case R.id.nav_push -> {
                    getSupportActionBar().setTitle("Push notification");
                    fragment = new PushFragment();
                }
                case R.id.nav_messaging -> {
                    getSupportActionBar().setTitle("In-app messaging");
                    fragment = new InAppFragment();
                }
                case R.id.nav_update -> {
                    getSupportActionBar().setTitle("In-app update");
                    fragment = new UpdatesFragment();
                }
                case R.id.nav_tokens -> startActivity(new Intent(MainActivity.this, CredentialActivity.class));
                case R.id.nav_about -> startActivity(new Intent(MainActivity.this, AboutActivity.class));
                case R.id.nav_github -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ErrorxCode")));
                case R.id.nav_website -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://clorabase.tk")));
                case R.id.nav_clorastore -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ErrorxCode/ClorastoreDB")));
                case R.id.nav_clorem -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ErrorxCode/CloremDB")));
            }

            layout.closeDrawer(GravityCompat.START);
            if (fragment != null)
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment, fragment).commit();
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        String link = "";
        if (fragment == null){
            fragment = getSupportFragmentManager().findFragmentByTag("currentFragment");
        }
        if (fragment == null)
            link = "";
        else if (fragment instanceof InAppFragment)
            link = "/documents/inapp";
        else if (fragment instanceof PushFragment)
            link = "/documents/push";
        else if (fragment instanceof StorageFragment)
            link = "/documents/storage";
        else if (fragment instanceof DatabaseFragment)
            link = "/documents/database";


        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://errorxcode.github.io/docs/clorabase/index.html#" + link)));
        return true;
    }


    @Override
    public void onBackPressed() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Utils.db.commit();
    }
}