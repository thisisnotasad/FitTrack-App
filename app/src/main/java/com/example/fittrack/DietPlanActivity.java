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

public class DietPlanActivity extends AppCompatActivity {

    private CardView cardEmptyState, cardFormState;
    private LinearLayout layoutHistoryState, mealsContainer;

    private TextView tvTotalMeals, tvTotalCalories, tvFormTitle;
    private EditText etMealName, etCalories;
    private Button btnFirstMeal, btnAddNewMeal, btnSaveMeal, btnCancelForm;

    private FirebaseFirestore db;
    private String userId;

    private boolean isEditMode = false;
    private String activeSelectedMealId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diet_plan);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Session Expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bind layout views
        cardEmptyState = findViewById(R.id.cardEmptyState);
        layoutHistoryState = findViewById(R.id.layoutHistoryState);
        cardFormState = findViewById(R.id.cardFormState);
        mealsContainer = findViewById(R.id.mealsContainer);

        tvTotalMeals = findViewById(R.id.tvTotalMeals);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        etMealName = findViewById(R.id.etMealName);
        etCalories = findViewById(R.id.etCalories);

        btnFirstMeal = findViewById(R.id.btnFirstMeal);
        btnAddNewMeal = findViewById(R.id.btnAddNewMeal);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);
        btnCancelForm = findViewById(R.id.btnCancelForm);

        // Map Click Behaviors
        btnFirstMeal.setOnClickListener(v -> openEntryForm(false, ""));
        btnAddNewMeal.setOnClickListener(v -> openEntryForm(false, ""));
        btnCancelForm.setOnClickListener(v -> refreshScreenDisplayState());
        btnSaveMeal.setOnClickListener(v -> commitMealToFirestore());

        // Perform Initial Read Query Execution
        fetchDietHistoryLogs();
    }

    // READ Operation: Pull meal logs out of the user's nested collection profile path
    private void fetchDietHistoryLogs() {
        db.collection("Users").document(userId).collection("Diet").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    mealsContainer.removeAllViews(); // Safe UI clear resets

                    if (queryDocumentSnapshots.isEmpty()) {
                        cardEmptyState.setVisibility(View.VISIBLE);
                        layoutHistoryState.setVisibility(View.GONE);
                        cardFormState.setVisibility(View.GONE);
                    } else {
                        cardEmptyState.setVisibility(View.GONE);
                        layoutHistoryState.setVisibility(View.VISIBLE);
                        cardFormState.setVisibility(View.GONE);

                        int totalMeals = queryDocumentSnapshots.size();
                        int totalCaloriesAccumulated = 0;

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String id = doc.getId();
                            String mealName = doc.getString("mealName");
                            Long caloriesLong = doc.getLong("calories");
                            int calories = (caloriesLong != null) ? caloriesLong.intValue() : 0;
                            totalCaloriesAccumulated += calories;

                            addMealCardToLayout(id, mealName, calories);
                        }

                        tvTotalMeals.setText(String.valueOf(totalMeals));
                        tvTotalCalories.setText(totalCaloriesAccumulated + " kcal");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error fetching diet logs: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Programmatic View Generator to inject clean rows safely into your layout view nodes
    private void addMealCardToLayout(String id, String mealName, int calories) {
        CardView itemCard = new CardView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 0, 0, 24);
        itemCard.setLayoutParams(layoutParams);
        itemCard.setRadius(16f);
        itemCard.setCardElevation(4f);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(32, 32, 32, 32);
        rowLayout.setWeightSum(3f);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f);
        textGroup.setLayoutParams(textParams);

        TextView tvMeal = new TextView(this);
        tvMeal.setText(mealName);
        tvMeal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvMeal.setTypeface(null, Typeface.BOLD);
        tvMeal.setTextColor(Color.BLACK);

        TextView tvKcal = new TextView(this);
        tvKcal.setText(calories + " kcal");
        tvKcal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvKcal.setTextColor(Color.parseColor("#2E7D32"));
        tvKcal.setPadding(0, 8, 0, 0);

        textGroup.addView(tvMeal);
        textGroup.addView(tvKcal);

        Button btnEdit = new Button(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnEdit.setLayoutParams(btnParams);
        btnEdit.setText("Edit");
        btnEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnEdit.setBackgroundColor(Color.parseColor("#757575"));
        btnEdit.setTextColor(Color.WHITE);

        btnEdit.setOnClickListener(v -> openEntryForm(true, id));

        rowLayout.addView(textGroup);
        rowLayout.addView(btnEdit);
        itemCard.addView(rowLayout);

        mealsContainer.addView(itemCard);
    }

    private void openEntryForm(boolean editModeTrigger, String selectedMealId) {
        isEditMode = editModeTrigger;
        activeSelectedMealId = selectedMealId;

        etMealName.setText("");
        etCalories.setText("");

        if (isEditMode) {
            tvFormTitle.setText("Modify Meal Entry");
            btnSaveMeal.setText("Update Meal Changes");

            db.collection("Users").document(userId).collection("Diet").document(activeSelectedMealId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            etMealName.setText(documentSnapshot.getString("mealName"));
                            etCalories.setText(String.valueOf(documentSnapshot.getLong("calories")));
                        }
                    });
        } else {
            tvFormTitle.setText("Log New Meal Details");
            btnSaveMeal.setText("Save Meal Intake");
        }

        cardEmptyState.setVisibility(View.GONE);
        layoutHistoryState.setVisibility(View.GONE);
        cardFormState.setVisibility(View.VISIBLE);
    }

    // CREATE & UPDATE execution loop engine for nutrition records
    private void commitMealToFirestore() {
        String mealInput = etMealName.getText().toString().trim();
        String calorieInput = etCalories.getText().toString().trim();

        if (mealInput.isEmpty() || calorieInput.isEmpty()) {
            Toast.makeText(this, "Please enter a meal name and calories", Toast.LENGTH_SHORT).show();
            return;
        }

        int caloriesValue = Integer.parseInt(calorieInput);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("mealName", mealInput);
        dataMap.put("calories", caloriesValue);

        if (isEditMode) {
            db.collection("Users").document(userId)
                    .collection("Diet").document(activeSelectedMealId)
                    .update(dataMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Meal entry updated!", Toast.LENGTH_SHORT).show();
                        refreshScreenDisplayState();
                    });
        } else {
            String generatedId = db.collection("Users").document(userId).collection("Diet").document().getId();
            dataMap.put("mealId", generatedId);
            dataMap.put("timestamp", System.currentTimeMillis());

            db.collection("Users").document(userId)
                    .collection("Diet").document(generatedId)
                    .set(dataMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "New meal intake logged!", Toast.LENGTH_SHORT).show();
                        refreshScreenDisplayState();
                    });
        }
    }

    private void refreshScreenDisplayState() {
        etMealName.setText("");
        etCalories.setText("");
        fetchDietHistoryLogs();
    }
}