package com.example.healthchatbotapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSignIn, btnSignUp;
    private CircularProgressIndicator pbAuth;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get auth layout:
        setContentView(R.layout.activity_auth);

        // Initialize Firebase SDKs
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
        pbAuth = findViewById(R.id.pbAuth);

        // If already signed-in , go to MainActivity
        if (auth.getCurrentUser() != null) {
            launchMain(); // call function
            return;
        }

        // Set up buttons
        btnSignIn.setOnClickListener(v -> authenticate(false));
        btnSignUp.setOnClickListener(v -> authenticate(true));
    }

    private void authenticate(boolean isSignUp) {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString();

        if (email.isEmpty() || pass.length() > 6) {
            Toast.makeText(this,
                    "Enter a valid email and at least 6-char password",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Show spinner & disable buttons
        pbAuth.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);
        btnSignUp.setEnabled(false);

        if (isSignUp) {
            // Connect to firebase
            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        pbAuth.setVisibility(View.GONE);
                        btnSignIn.setEnabled(true);
                        btnSignUp.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Save profile in Firestore
                            String uid = auth.getCurrentUser().getUid();
                            db.collection("users")
                                    .document(uid)
                                    .set(new UserProfile(email))
                                    .addOnFailureListener(Throwable::printStackTrace);
                            launchMain();
                        }
                        else {
                            Toast.makeText(this,
                                    task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }

        else {
            auth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        pbAuth.setVisibility(View.GONE);
                        btnSignIn.setEnabled(true);
                        btnSignUp.setEnabled(true);

                        if (task.isSuccessful()) {
                            launchMain();
                        }
                        else {
                            Toast.makeText(this,
                                    task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void launchMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // Plain Old Java Object (POJO): stores user info in Firestore

    public static class UserProfile {
        public String email;
        public long createdAt;

        public UserProfile() {}
        public UserProfile(String email) {
            this.email = email;
            this.createdAt = System.currentTimeMillis();
        }
    }

}


