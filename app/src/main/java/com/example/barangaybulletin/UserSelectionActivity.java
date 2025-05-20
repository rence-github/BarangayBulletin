package com.example.barangaybulletin;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.barangaybulletin.databinding.ActivityUserSelectionBinding;

public class UserSelectionActivity extends AppCompatActivity {

    private ActivityUserSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set click listeners
        binding.cardAdmin.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminLoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        binding.cardGuest.setOnClickListener(v -> {
            startActivity(new Intent(this, UserHomeActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}