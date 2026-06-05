package com.example.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Configure Google Sign-In options for secure session cancellation
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Using custom styling layout
        }

        // Connect Layout Architecture Widgets
        CardView workoutCard = findViewById(R.id.workoutCard);
        CardView dietCard = findViewById(R.id.dietCard);
        CardView profileCard = findViewById(R.id.profileCard);
        ImageView ivLogout = findViewById(R.id.ivLogout);

        // 1. Navigation Flow: Workouts Tracker Section
        workoutCard.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, WorkoutLogActivity.class));
        });

        // 2. Navigation Flow: Diet Plan Calories Section
        dietCard.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, DietPlanActivity.class));
        });

        // 3. Navigation Flow: Body Biometric Profile Section
        profileCard.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CompleteProfileActivity.class));
        });

        // 4. Clear Sessions & Force Sign-out Event
        ivLogout.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(HomeActivity.this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}