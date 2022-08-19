package com.clorabase.console;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
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
import com.xcoder.tasks.AsyncTask;

import java.io.IOException;
import java.security.GeneralSecurityException;

import apis.xcoder.easydrive.EasyDrive;
import db.clorabase.clorem.Clorem;
import db.clorabase.clorem.Node;

public class MainActivity extends AppCompatActivity {
    public static final String TOKEN = "****************************";
    public Fragment fragment;
    public static EasyDrive drive;
    public volatile static Node db;
    public static NavigationView drawer;
    public volatile static String clorabaseID;

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
        initClorabase();
        if (savedInstanceState == null)
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment, new HomeFragment()).commit();

        NetworkInfo info = getSystemService(ConnectivityManager.class).getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("No internet connection");
            builder.setMessage("Please check your internet connection and try again.");
            builder.setPositiveButton("retry", (dialog, which) -> recreate());
            builder.show();
            return;
        }

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

    private void initClorabase(){
        try {
            db = Clorem.getInstance(getFilesDir(), "main").getDatabase();
            System.out.println(db.getString("clorabaseRoot", "There is no id in database"));
            drive = new EasyDrive("402416439097-j01jvkbrkjttqb1ugoopi4hu7bi94a3o.apps.googleusercontent.com", "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", TOKEN);
            if (db.getString("clorabaseRoot", null) == null) {
                drive.createFolder("Clorabase", null).setOnCompleteCallback(call -> {
                    if (call.isSuccessful) {
                        clorabaseID = call.result;
                        db.put("clorabaseRoot", clorabaseID);
                    } else {
                        call.exception.printStackTrace();
                    }
                });
            } else
                clorabaseID = db.getString("clorabaseRoot", null);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show();
        }
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
        db.commit();
    }

    public static void configureFeature(Context context,String relativePath, AsyncTask.OnCompleteCallback<String> callback) {
        if (relativePath.endsWith(".db") || relativePath.endsWith(".json"))
            drive.createFileRecursively("clorabase/" + relativePath).setOnCompleteCallback(task -> ((Activity) context).runOnUiThread(() -> {
                callback.onComplete(task);
            }));
        else
            drive.createFolderRecursively("clorabase/" + relativePath).setOnCompleteCallback(task -> ((Activity) context).runOnUiThread(() -> {
                callback.onComplete(task);
            }));
    }

    public static void delete(Context context, String fileId, AsyncTask.OnCompleteCallback callback) {
        drive.delete(fileId).setOnCompleteCallback(task -> ((Activity) context).runOnUiThread(() -> {
            callback.onComplete(task);
        }));
    }

    public static void updateFile(Context context,String fileId,String content, AsyncTask.OnCompleteCallback callback) {
        drive.updateFile(fileId, content).setOnCompleteCallback(task -> ((Activity) context).runOnUiThread(() -> {
            callback.onComplete(task);
        }));
    }
}