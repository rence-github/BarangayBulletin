package com.example.barangaybulletin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.widget.Toast;

import com.example.barangaybulletin.databinding.ActivityAdminHomeBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class AdminHomeActivity extends AppCompatActivity {

    private ActivityAdminHomeBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Display welcome message
        if (mAuth.getCurrentUser() != null) {
            String welcomeMessage = "Welcome, " + mAuth.getCurrentUser().getEmail();
            Toast.makeText(this, welcomeMessage, Toast.LENGTH_SHORT).show();
        }

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = binding.adminBottomNav;
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.nav_home:
                    selectedFragment = new AdminHomeFragment();
                    break;
                case R.id.nav_volunteer:
                    selectedFragment = new AdminVolunteerFragment();
                    break;
                case R.id.nav_feedback:
                    selectedFragment = new AdminFeedbackFragment();
                    break;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.admin_fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.admin_fragment_container, new AdminHomeFragment())
                    .commit();
        }
    }
}