package com.example.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class CompleteProfileActivity extends AppCompatActivity {

    private TextView tvAvatarBadge, tvProfileName, tvProfileEmail;
    private TextView tvDisplayHeight, tvDisplayWeight, tvDisplayAge;

    private EditText etHeight, etWeight, etAge;
    private LinearLayout layoutMetricsDisplay;
    private LinearLayout cardEditForm;

    private Button btnEditProfileToggle, btnSaveProfile, btnCancelEdit;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind View Elements
        tvAvatarBadge = findViewById(R.id.tvAvatarBadge);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);

        tvDisplayHeight = findViewById(R.id.tvDisplayHeight);
        tvDisplayWeight = findViewById(R.id.tvDisplayWeight);
        tvDisplayAge = findViewById(R.id.tvDisplayAge);

        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        etAge = findViewById(R.id.etAge);

        layoutMetricsDisplay = findViewById(R.id.layoutMetricsDisplay);
        cardEditForm = findViewById(R.id.cardEditForm);

        btnEditProfileToggle = findViewById(R.id.btnEditProfileToggle);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);

        // Click Controls Setup
        btnEditProfileToggle.setOnClickListener(v -> toggleEditState(true));
        btnCancelEdit.setOnClickListener(v -> toggleEditState(false));
        btnSaveProfile.setOnClickListener(v -> updateMetricsInFirestore());

        // Initial Data Fetch
        fetchUserProfileDetails();
    }

    private void fetchUserProfileDetails() {
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String email = documentSnapshot.getString("email");

                        if (name != null && !name.isEmpty()) {
                            tvProfileName.setText(name);
                            tvAvatarBadge.setText(name.substring(0, 1).toUpperCase());
                        } else {
                            tvProfileName.setText("FitTrack User");
                            tvAvatarBadge.setText("F");
                        }

                        if (email != null) {
                            tvProfileEmail.setText(email);
                        }

                        // Parse metrics using correct types matching your User model fields
                        Object ageObj = documentSnapshot.get("age");
                        Object weightObj = documentSnapshot.get("weight");
                        Object heightObj = documentSnapshot.get("height");

                        String ageStr = ageObj != null ? String.valueOf(ageObj) : "0";
                        String weightStr = weightObj != null ? String.valueOf(weightObj) : "0.0";
                        String heightStr = heightObj != null ? String.valueOf(heightObj) : "0.0";

                        tvDisplayAge.setText(ageStr + " yrs");
                        tvDisplayWeight.setText(weightStr + " kg");
                        tvDisplayHeight.setText(heightStr + " cm");

                        etAge.setText(ageStr);
                        etWeight.setText(weightStr);
                        etHeight.setText(heightStr);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateMetricsInFirestore() {
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();

        if (heightStr.isEmpty() || weightStr.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "All tracking metrics are mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Map<String, Object> medicalMetricsMap = new HashMap<>();
            medicalMetricsMap.put("height", Double.parseDouble(heightStr));
            medicalMetricsMap.put("weight", Double.parseDouble(weightStr));
            medicalMetricsMap.put("age", Integer.parseInt(ageStr));

            db.collection("Users").document(userId)
                    .update(medicalMetricsMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile biometrics updated successfully!", Toast.LENGTH_SHORT).show();
                        toggleEditState(false);
                        fetchUserProfileDetails();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Write update failure: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please check your number inputs format", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleEditState(boolean displayEditForm) {
        if (displayEditForm) {
            layoutMetricsDisplay.setVisibility(View.GONE);
            cardEditForm.setVisibility(View.VISIBLE);
        } else {
            layoutMetricsDisplay.setVisibility(View.VISIBLE);
            cardEditForm.setVisibility(View.GONE);
        }
    }
}