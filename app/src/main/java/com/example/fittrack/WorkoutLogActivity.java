package com.example.fittrack;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WorkoutLogActivity extends AppCompatActivity {

    // View Containers
    private CardView cardEmptyState, cardFormState;
    private LinearLayout layoutHistoryState, listContainer;

    // Summary Metrics Text Fields
    private TextView tvTotalWorkouts, tvTotalMinutes, tvFormTitle;

    // Form Inputs & Processing Buttons
    private EditText etWorkoutType, etDuration;
    private Button btnFirstWorkout, btnAddNewWorkout, btnSaveWorkout, btnCancelForm;

    // Firebase Core Instances
    private FirebaseFirestore db;
    private String userId;

    // Operational Tracking Variables to distinguish between Create vs Update actions
    private boolean isEditMode = false;
    private String activeSelectedWorkoutId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_log);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Link Container Architectures
        cardEmptyState = findViewById(R.id.cardEmptyState);
        layoutHistoryState = findViewById(R.id.layoutHistoryState);
        cardFormState = findViewById(R.id.cardFormState);
        listContainer = findViewById(R.id.listContainer);

        // Link Labels and Inputs
        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvTotalMinutes = findViewById(R.id.tvTotalMinutes);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        etWorkoutType = findViewById(R.id.etWorkoutType);
        etDuration = findViewById(R.id.etDuration);

        // Link Component System Controls
        btnFirstWorkout = findViewById(R.id.btnFirstWorkout);
        btnAddNewWorkout = findViewById(R.id.btnAddNewWorkout);
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        btnCancelForm = findViewById(R.id.btnCancelForm);

        // Map UI Control Actions Safely
        btnFirstWorkout.setOnClickListener(v -> openEntryForm(false, ""));
        btnAddNewWorkout.setOnClickListener(v -> openEntryForm(false, ""));
        btnCancelForm.setOnClickListener(v -> refreshScreenDisplayState());

        btnSaveWorkout.setOnClickListener(v -> commitFormToFirestore());

        // Perform Initial Read Query Execution
        fetchWorkoutHistoryLogs();
    }

    // 1. READ Operation: Pulls logs down from the nested sub-collection pipeline
    private void fetchWorkoutHistoryLogs() {
        db.collection("Users").document(userId).collection("Workouts").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listContainer.removeAllViews(); // Reset display row list safely

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Display Motivational state if database is empty
                        cardEmptyState.setVisibility(View.VISIBLE);
                        layoutHistoryState.setVisibility(View.GONE);
                        cardFormState.setVisibility(View.GONE);
                    } else {
                        cardEmptyState.setVisibility(View.GONE);
                        layoutHistoryState.setVisibility(View.VISIBLE);
                        cardFormState.setVisibility(View.GONE);

                        int totalSessions = queryDocumentSnapshots.size();
                        int totalMinutesAccumulated = 0;

                        // Programmatically build modern clean item cards for each workout log found
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String id = doc.getId();
                            String type = doc.getString("workoutType");
                            Long durationLong = doc.getLong("duration");
                            int duration = (durationLong != null) ? durationLong.intValue() : 0;
                            totalMinutesAccumulated += duration;

                            addWorkoutCardToLayout(id, type, duration);
                        }

                        // Sync Summary Metrics fields directly
                        tvTotalWorkouts.setText(String.valueOf(totalSessions));
                        tvTotalMinutes.setText(totalMinutesAccumulated + " mins");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching records: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Fixed Dynamic View Compiler Engine with standard programmatic Android layout rules
    private void addWorkoutCardToLayout(String id, String type, int duration) {
        CardView itemCard = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 24); // Clean margin spacing between cards
        itemCard.setLayoutParams(layoutParams);
        itemCard.setRadius(16f);
        itemCard.setCardElevation(4f);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(32, 32, 32, 32);
        rowLayout.setWeightSum(4f); // Expanded sum weight to fit Edit and Delete comfortably

        // Texts Details Column Group
        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
        textGroup.setLayoutParams(textParams);

        TextView tvType = new TextView(this);
        tvType.setText(type);
        tvType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Fixed SP assignment rule
        tvType.setTypeface(null, Typeface.BOLD); // Fixed typographic bold rule
        tvType.setTextColor(Color.BLACK);

        TextView tvTime = new TextView(this);
        tvTime.setText("Duration: " + duration + " mins");
        tvTime.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Fixed SP assignment rule
        tvTime.setTextColor(Color.GRAY);
        tvTime.setPadding(0, 8, 0, 0);

        textGroup.addView(tvType);
        textGroup.addView(tvTime);

        // Action Edit Trigger Button Column Group
        Button btnEdit = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnEdit.setLayoutParams(btnParams);
        btnEdit.setText("Edit");
        btnEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnEdit.setBackgroundColor(Color.parseColor("#757575"));
        btnEdit.setTextColor(Color.WHITE);
        btnEdit.setOnClickListener(v -> openEntryForm(true, id));

        // Action Delete Trigger Button Column Group
        Button btnDelete = new Button(this);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        deleteParams.setMargins(8, 0, 0, 0); // Give button tiny breathing room from edit button
        btnDelete.setLayoutParams(deleteParams);
        btnDelete.setText("Delete");
        btnDelete.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnDelete.setBackgroundColor(Color.parseColor("#C62828")); // Strong visible red accent
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setOnClickListener(v -> deleteWorkoutEntry(id));

        rowLayout.addView(textGroup);
        rowLayout.addView(btnEdit);
        rowLayout.addView(btnDelete);
        itemCard.addView(rowLayout);

        listContainer.addView(itemCard);
    }

    // 3. DELETE Operation: Cleans specific workout out of the user pipeline completely
    private void deleteWorkoutEntry(String recordId) {
        db.collection("Users").document(userId)
                .collection("Workouts").document(recordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(WorkoutLogActivity.this, "Workout deleted successfully!", Toast.LENGTH_SHORT).show();
                    fetchWorkoutHistoryLogs(); // Instantly refresh data representation metrics
                })
                .addOnFailureListener(e -> Toast.makeText(WorkoutLogActivity.this, "Error deleting record: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Controls Layout Visibility States smoothly
    private void openEntryForm(boolean editModeTrigger, String selectedRecordId) {
        isEditMode = editModeTrigger;
        activeSelectedWorkoutId = selectedRecordId;

        // Clear or preload inputs cleanly
        etWorkoutType.setText("");
        etDuration.setText("");

        if (isEditMode) {
            tvFormTitle.setText("Modify Workout Entry");
            btnSaveWorkout.setText("Update Session Changes");

            // Pre-fill fields out of the existing view layout labels
            db.collection("Users").document(userId).collection("Workouts").document(activeSelectedWorkoutId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etWorkoutType.setText(documentSnapshot.getString("workoutType"));
                            etDuration.setText(String.valueOf(documentSnapshot.getLong("duration")));
                        }
                    });
        } else {
            tvFormTitle.setText("Log New Routine Details");
            btnSaveWorkout.setText("Save Session Details");
        }

        // Adjust visibility toggles safely
        cardEmptyState.setVisibility(View.GONE);
        layoutHistoryState.setVisibility(View.GONE);
        cardFormState.setVisibility(View.VISIBLE);
    }

    // 4. CREATE & UPDATE Operation Commit Engine
    private void commitFormToFirestore() {
        String typeInput = etWorkoutType.getText().toString().trim();
        String durationInput = etDuration.getText().toString().trim();

        if (typeInput.isEmpty() || durationInput.isEmpty()) {
            Toast.makeText(this, "Please fulfill all data parameters", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationValue = Integer.parseInt(durationInput);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("workoutType", typeInput);
        dataMap.put("duration", durationValue);

        if (isEditMode) {
            // Execute UPDATE branch targeting existing identifier
            db.collection("Users").document(userId)
                    .collection("Workouts").document(activeSelectedWorkoutId)
                    .update(dataMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Session entry updated successfully!", Toast.LENGTH_SHORT).show();
                        refreshScreenDisplayState();
                    });
        } else {
            // Execute CREATE branch generating fresh auto ID token path entry
            String generatedId = db.collection("Users").document(userId).collection("Workouts").document().getId();
            dataMap.put("workoutId", generatedId);
            dataMap.put("timestamp", System.currentTimeMillis());

            db.collection("Users").document(userId)
                    .collection("Workouts").document(generatedId)
                    .set(dataMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "New workout entry logged!", Toast.LENGTH_SHORT).show();
                        refreshScreenDisplayState();
                    });
        }
    }

    private void refreshScreenDisplayState() {
        etWorkoutType.setText("");
        etDuration.setText("");
        fetchWorkoutHistoryLogs(); // Automatically triggers a clean database read refresh
    }
}