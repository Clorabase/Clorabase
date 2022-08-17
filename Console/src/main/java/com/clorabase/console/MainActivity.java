package com.clorabase.console;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.clorabase.clorastore.Clorastore;
import com.clorabase.clorastore.Collection;
import com.clorabase.clorastore.Document;
import com.clorabase.console.databinding.ActivityMainBinding;
import com.clorabase.console.fragments.DatabaseFragment;
import com.clorabase.console.fragments.HomeFragment;
import com.clorabase.console.fragments.InAppFragment;
import com.clorabase.console.fragments.PushFragment;
import com.clorabase.console.fragments.StorageFragment;
import com.clorabase.console.fragments.UpdatesFragment;
import com.google.android.material.navigation.NavigationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public Fragment fragment;
    public static String CURRENT_PROJECT;
    public static List<String> projects;
    public volatile static Collection db;
    private Document doc;
    public static NavigationView drawer;
    public static ArrayAdapter<String> adapter;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        drawer = binding.drawer;
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.app_name, R.string.app_name);
        db = Clorastore.getInstance(getFilesDir(), "main").getDatabase();
        doc = db.document("projects");
        CURRENT_PROJECT = (String) doc.get("lastProject","");
        projects = (List<String>) doc.get("names", new ArrayList<String>());
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, projects);

        binding.projects.setAdapter(adapter);
        binding.projects.setTag(false);
        binding.projects.setSelection(projects.indexOf(CURRENT_PROJECT));
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NetworkInfo info = getSystemService(ConnectivityManager.class).getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("No internet connection");
            builder.setMessage("Please check your internet connection and try again.");
            builder.setPositiveButton("retry", (dialog, which) -> recreate());
            builder.setNegativeButton("exit", (dialog, which) -> finish());
            builder.setCancelable(false);
            builder.show();
        } else {
            Utils.init();
            if (savedInstanceState == null)
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();
        }

        binding.projects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CURRENT_PROJECT = projects.get(position);
                doc.put("lastProject",CURRENT_PROJECT);
                doc.put("names",projects);
                if (binding.projects.getTag().equals(true))
                    Toast.makeText(MainActivity.this, "Restart app to take effect", Toast.LENGTH_LONG).show();
                binding.projects.setTag(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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
            binding.drawerLayout.closeDrawer(GravityCompat.START);
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
        if (item.getItemId() == R.id.help){
            String link = "";
            if (fragment == null) {
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
        } else if (item.getItemId() == R.id.delete){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Delete project");
            builder.setMessage("Are you sure you want to delete the project? It will delete all data and this action cannot be undone.");
            builder.setPositiveButton("confirm", (dialog, which) -> {
                projects.remove(CURRENT_PROJECT);
                doc.put("names",projects);
                adapter.notifyDataSetChanged();
                delete(CURRENT_PROJECT);
                Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("cancel",null);
            builder.setCancelable(false);
            builder.show();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();
    }

    public void delete(String dir) {
       new Thread(() -> {
           try {
               for (var file : Utils.repo.getDirectoryContent(dir)){
                   if (file.isFile())
                       file.delete("Deleting project");
                   else
                       delete(file.getPath());
               }
           } catch (IOException e) {
               e.printStackTrace();
           }
       }).start();
    }
}