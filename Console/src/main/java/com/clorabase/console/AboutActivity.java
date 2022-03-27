package com.clorabase.console;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.clorabase.console.databinding.ActivityAboutBinding;
import com.shashank.sony.fancyaboutpagelib.FancyAboutPage;


public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        FancyAboutPage page = findViewById(R.id.about_page_layout);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        page.setAppDescription(getResources().getString(R.string.app_description));
        page.setAppIcon(R.drawable.logo);
        page.setAppName("Clorabase console");
        page.setVersionNameAsAppSubTitle("A account-free backend for android apps");
        page.setCover(R.drawable.header);
        page.setName("Rahil khan");
        page.setDescription(getResources().getString(R.string.description));
        page.addEmailLink("inboxrahil@xcoder.tk");
        page.addGitHubLink("https://github.com/ErrorxCode");
        page.addLinkedinLink("https://www.linkedin.com/in/rahil-khan-339a24227/");
        page.addTwitterLink("https://instagram.com/x__coder__x");
    }
}
