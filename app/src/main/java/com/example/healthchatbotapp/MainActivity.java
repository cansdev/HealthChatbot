package com.example.healthchatbotapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved locale first
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "en");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar and Drawer
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // 🔁 Force drawer menu to re-inflate under current locale
        navView.getMenu().clear();
        navView.inflateMenu(R.menu.drawer_menu);

        // Edge-to-edge and insets
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                    view.setPadding(sys.left, sys.top, sys.right, sys.bottom + ime.bottom);
                    return insets;
                });

        RecyclerView rvChat = findViewById(R.id.rvChat);
        ChatAdapter adapter = new ChatAdapter(new ArrayList<>());
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(adapter);

        TextInputEditText etMessage = findViewById(R.id.etMessage);
        MaterialButton btnSend = findViewById(R.id.btnSend);
        CircularProgressIndicator loading = findViewById(R.id.loading);
        GeminiClient gemini = new GeminiClient();

        btnSend.setOnClickListener(v -> {
            String text = Objects.requireNonNull(etMessage.getText()).toString().trim();
            if (text.isEmpty()) return;

            adapter.addMessage(new ChatMessage(text, true));
            rvChat.scrollToPosition(adapter.getItemCount() - 1);
            etMessage.setText("");
            btnSend.setEnabled(false);
            loading.setVisibility(View.VISIBLE);

            // Use localized prompt
            String fullPrompt = (lang.equals("tr") ?
                    "Sen HealthChatBotApp’in tıbbi asistanısın. " +
                            "Kısa, dostça sağlık tavsiyesi ver.\n" +
                            "Hasta: " + text + "\nDoktor:" :
                    "You are HealthChatBotApp’s medical assistant. " +
                            "Provide concise, friendly health advice.\n" +
                            "Patient: " + text + "\nDoctor:");

            if (!NetworkUtils.isNetworkAvailable(this)) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.no_network_title))
                        .setMessage(getString(R.string.no_network_message))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            gemini.generateContent(fullPrompt, new GeminiClient.Listener() {
                @Override
                public void onSuccess(String reply) {
                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        adapter.addMessage(new ChatMessage(reply, false));
                        rvChat.scrollToPosition(adapter.getItemCount() - 1);
                    });
                }

                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        loading.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                        new MaterialAlertDialogBuilder(MainActivity.this)
                                .setTitle("Gemini Error")
                                .setMessage(e.getMessage())
                                .setPositiveButton("OK", null)
                                .show();
                    });
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
