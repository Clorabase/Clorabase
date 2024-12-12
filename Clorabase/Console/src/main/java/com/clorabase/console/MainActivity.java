package com.clorabase.console;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    public Fragment fragment;
    public static String CURRENT_PROJECT;
    public static List<String> projects;
    public static Collection db;
    public Document config;
    public NavigationView drawer;
    public ArrayAdapter<String> adapter;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);


        // Initializing database,lists and adapter etc.
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, 0, 0);
        toggle.syncState();
        drawer = binding.drawer;
        db = Clorastore.getInstance(getFilesDir(), "main").getDatabase();
        config = db.document("config");
        projects = config.getList("projects", null);
        CURRENT_PROJECT = config.getString("activeProject", "");
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, projects);

        // Checking login status
        if (!config.getBoolean("isLogin", false)) {
            startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), 85);
            return;
        }

        // Checking network
        NetworkInfo info = getSystemService(ConnectivityManager.class).getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("No internet connection");
            builder.setMessage("Please check your internet connection and try again.");
            builder.setPositiveButton("retry", (dialog, which) -> recreate());
            builder.setNegativeButton("exit", (dialog, which) -> finish());
            builder.setCancelable(false);
            builder.show();
            return;
        }

        // Checking if projects are added or not and then initializing
        if (projects == null) {
            Toast.makeText(this, "Add a project to continue", Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(MainActivity.this, AddProjectActivity.class), Constants.REQUEST_CODE_PROJECT);
        } else {
            var username = config.getString("username", "");
            var token = config.getString("token", "");
            Utils.init(this, username, token);
            binding.projects.setAdapter(adapter);
            System.out.println("Current proj : " + CURRENT_PROJECT);
            binding.projects.setSelection(projects.indexOf(CURRENT_PROJECT));
            binding.drawerLayout.addDrawerListener(toggle);
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();
        }

        // Setting listeners
        binding.projects.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!Objects.equals(CURRENT_PROJECT, projects.get(position))) {
                    CURRENT_PROJECT = projects.get(position);
                    config.put("activeProject", CURRENT_PROJECT);
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();
                    recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        drawer.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_db) {
                getSupportActionBar().setTitle("Database");
                fragment = new DatabaseFragment();
            } else if (itemId == R.id.nav_storage) {
                getSupportActionBar().setTitle("Storage");
                fragment = new StorageFragment();
            } else if (itemId == R.id.nav_push) {
                getSupportActionBar().setTitle("Push notification");
                fragment = new PushFragment();
            } else if (itemId == R.id.nav_messaging) {
                getSupportActionBar().setTitle("In-app messaging");
                fragment = new InAppFragment();
            } else if (itemId == R.id.nav_update) {
                getSupportActionBar().setTitle("In-app update");
                fragment = new UpdatesFragment();
            } else if (itemId == R.id.nav_tokens) {
                startActivity(new Intent(MainActivity.this, QuotaActivity.class));
            } else if (itemId == R.id.nav_about) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            } else if (itemId == R.id.nav_github) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ErrorxCode")));
            } else if (itemId == R.id.nav_website) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://clorabase.netlify.app")));
            } else if (itemId == R.id.nav_clorastore) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Clorabase/ClorastoreDB")));
            } else if (itemId == R.id.nav_clorem) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Clorabase/CloremDB")));
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START);

            if (fragment != null)
                getSupportFragmentManager()
                        .beginTransaction()
                        .addToBackStack(fragment.getClass().getName())
                        .replace(R.id.fragment, fragment, Constants.FRAGMENT_TAG)
                        .commit();

            return true;
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getSupportFragmentManager().popBackStack();
            }
        });

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                var frag = getSupportFragmentManager().findFragmentByTag("currentFragment");
                if (frag == null)
                    getSupportActionBar().setTitle("Home");
                else
                    getSupportActionBar().setTitle(frag.getClass().getSimpleName().replace("Fragment", ""));
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.help) {
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
        } else if (item.getItemId() == R.id.delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Delete project");
            builder.setMessage("Are you sure you want to delete the project? It will delete all data and this action cannot be undone.");
            builder.setPositiveButton("confirm", (dialog, which) -> deleteProject(CURRENT_PROJECT));
            builder.setNegativeButton("cancel", null);
            builder.setCancelable(false);
            builder.show();
        } else if (item.getItemId() == R.id.add_project) {
            startActivityForResult(new Intent(MainActivity.this, AddProjectActivity.class), Constants.REQUEST_CODE_PROJECT);
        } else if (item.getItemId() == R.id.logout) {
            config.delete();
            recreate();
        }
        return true;
    }


    public void deleteProject(String dir) {
        new Thread(() -> {
            try {
                var files = Utils.repo.getDirectoryContent(dir);
                for (var file : files) {
                    if (file.isFile())
                        file.delete("Deleting project");
                    else
                        deleteProject(file.getPath());
                }
                var release = Utils.repo.getReleaseByTagName(dir);
                if (release != null) {
                    release.delete();
                    Utils.repo.getRef("tags/" + release.getTagName()).delete();
                }
                if (projects.isEmpty()) {
                    projects = null;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Add any project to use the console", Toast.LENGTH_SHORT).show();
                    });
                    recreate();
                } else {
                    runOnUiThread(() -> {
                        projects.remove(CURRENT_PROJECT);
                        config.removeItem("projects", projects);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(MainActivity.this, "Project deleted", Toast.LENGTH_SHORT).show();
                        recreate();
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Project deleting failed!", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            // Disabling touch events
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            new Thread(() -> {
                assert data != null;
                var token = data.getStringExtra("token");
                var username = data.getStringExtra("username");
                Utils.init(MainActivity.this, username, token); // for Utils.getProject to work
                var projects = Utils.getProjects();
                config.put("token", token);
                config.put("username", username);
                config.put("isLogin", true);
                config.put("projects",projects);

                var storage = db.document("storageConfiguration");
                projects.forEach(project -> Utils.read(project + "/config.json", bytes -> {
                    if (bytes != null){
                        try {
                            var json = new JSONObject(new String(bytes));
                            var isStorageConfigured = json.getBoolean("isStorageConfigured");
                            storage.put(project,isStorageConfigured);
                        } catch (JSONException ignored){
                            // Not possible
                        }
                    }
                }));
                runOnUiThread(this::recreate);
            }).start();
        } else if (requestCode == Constants.REQUEST_CODE_PROJECT && resultCode == RESULT_OK){
            assert data != null;
            var project = data.getStringExtra("project");
            config.addItem("projects",project);
            config.put("activeProject",project);
            db.document("storageConfiguration").put(project,data.getBooleanExtra("storage",false));
            startActivity(new Intent(this,MainActivity.class));
            finish();

            // TODO: 25-09-2024 Fix delete file from console (No path)
        }
    }
}