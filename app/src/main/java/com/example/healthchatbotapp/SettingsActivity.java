package com.example.healthchatbotapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Spinner themeSpinner, languageSpinner;
    private SharedPreferences prefs;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            view.setPadding(0, insets.getInsets(android.view.WindowInsets.Type.systemBars()).top, 0, 0);
            return insets;
        });

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        themeSpinner = findViewById(R.id.spinnerTheme);
        languageSpinner = findViewById(R.id.spinnerLanguage);

        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(this,
                R.array.theme_options, android.R.layout.simple_spinner_item);
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(themeAdapter);

        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(this,
                R.array.language_options, android.R.layout.simple_spinner_item);
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);

        // Restore saved settings
        themeSpinner.setSelection(prefs.getInt("themeIndex", 0));
        languageSpinner.setSelection(prefs.getInt("langIndex", 0));

        findViewById(R.id.btnApply).setOnClickListener(v -> applySettings());
    }

    private void applySettings() {
        int themeIndex = themeSpinner.getSelectedItemPosition();
        int langIndex = languageSpinner.getSelectedItemPosition();
        String langCode = (langIndex == 0 ? "en" : "tr");

        prefs.edit()
                .putInt("themeIndex", themeIndex)
                .putInt("langIndex", langIndex)
                .putString("language", langCode)
                .apply();

        switch (themeIndex) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }

        // Restart only MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
