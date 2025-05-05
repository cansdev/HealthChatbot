package com.example.healthchatbotapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    // get status+nav bar insets
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    // get IME (keyboard) inset
                    Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                    // set padding: left, top, right as before, bottom = navBar + keyboard
                    view.setPadding(
                            sys.left,
                            sys.top,
                            sys.right,
                            sys.bottom + ime.bottom
                    );

                    // return the insets untouched if you have other listeners down the tree
                    return insets;
                }
        );

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

            String fullPrompt =
                    "You are HealthChatBotApp’s medical assistant. " +
                            "Provide concise, friendly health advice.\n" +
                            "Patient: " + text + "\nDoctor:";

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
                        // Show the entire error in a scrollable dialog:
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
