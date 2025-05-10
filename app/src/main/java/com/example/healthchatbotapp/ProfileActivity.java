package com.example.healthchatbotapp;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private MaterialTextView tvUsername, tvDob;
    private MaterialButton btnPickDob;
    private FirebaseFirestore db;
    private DocumentReference userDoc;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Edge-to-edge padding
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, insets) -> {
                    Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(sys.left, sys.top, sys.right, sys.bottom);
                    return insets;
                });

        // Toolbar with back arrow
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Views
        tvUsername = findViewById(R.id.tvUsername);
        tvDob      = findViewById(R.id.tvDob);
        btnPickDob = findViewById(R.id.btnPickDob);

        // Firebase setup
        db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userDoc = db.collection("users").document(uid);

        // Show email as username
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        tvUsername.setText(getString(R.string.username_label, email));

        // Load DOB if set
        userDoc.get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("dateOfBirth")) {
                String dob = doc.getString("dateOfBirth");
                tvDob.setText(getString(R.string.dob_label, dob));
            }
        });

        // DatePicker
        btnPickDob.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(
                    this,
                    (picker, year, month, day) -> {
                        // format DD/MM/YYYY
                        String dob = String.format(Locale.getDefault(),
                                "%02d/%02d/%04d", day, month + 1, year);
                        // save to Firestore
                        Map<String,Object> data = new HashMap<>();
                        data.put("dateOfBirth", dob);
                        userDoc.update(data)
                                .addOnSuccessListener(a -> tvDob.setText(
                                        getString(R.string.dob_label, dob)))
                                .addOnFailureListener(e -> {
                                    // optionally show error
                                });
                    },
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }
}
