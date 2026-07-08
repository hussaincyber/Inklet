package com.inklet.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.inklet.app.R;
import com.inklet.app.utils.ProfileManager;
import com.inklet.app.utils.ThemeManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ThemeManager.isDarkMode(this)) {
            setTheme(R.style.Theme_Inklet_Dark_Splash);
        } else {
            setTheme(R.style.Theme_Inklet_Splash);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        boolean dark = ThemeManager.isDarkMode(this);
        View root = findViewById(R.id.root_splash);
        TextView logo = findViewById(R.id.tv_logo);
        TextView tagline = findViewById(R.id.tv_tagline);
        LinearLayout setupLayout = findViewById(R.id.layout_setup);
        EditText etName = findViewById(R.id.et_name);
        Button btnContinue = findViewById(R.id.btn_continue);

        if (dark) {
            root.setBackgroundColor(Color.parseColor("#0D0D0D"));
            logo.setTextColor(Color.parseColor("#F0F0F0"));
            tagline.setTextColor(Color.parseColor("#888888"));
        } else {
            root.setBackgroundColor(Color.parseColor("#FDF6EC"));
            logo.setTextColor(Color.parseColor("#1A1008"));
            tagline.setTextColor(Color.parseColor("#6B4C2A"));
        }

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(700); fadeIn.setFillAfter(true);
        logo.startAnimation(fadeIn);
        AlphaAnimation fadeInTag = new AlphaAnimation(0f, 1f);
        fadeInTag.setDuration(700); fadeInTag.setStartOffset(300); fadeInTag.setFillAfter(true);
        tagline.startAnimation(fadeInTag);

        if (ProfileManager.isFirstLaunch(this)) {
            new Handler().postDelayed(() -> {
                AlphaAnimation fade = new AlphaAnimation(0f, 1f);
                fade.setDuration(500); fade.setFillAfter(true);
                setupLayout.setVisibility(View.VISIBLE);
                setupLayout.startAnimation(fade);
            }, 1200);
            btnContinue.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                if (name.isEmpty()) name = "You";
                ProfileManager.setDisplayName(this, name);
                ProfileManager.markLaunched(this);
                ProfileManager.getProfileId(this);
                goToMain();
            });
        } else {
            new Handler().postDelayed(this::goToMain, 1400);
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
