package com.example.barangaybulletin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.example.barangaybulletin.databinding.ActivityAdminRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;

public class AdminRegisterActivity extends AppCompatActivity {

    private ActivityAdminRegisterBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Back button click listener
        binding.btnBack.setOnClickListener(v -> finish());

        // Login text click listener
        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(AdminRegisterActivity.this, AdminLoginActivity.class));
            finish();
        });

        // Register button click listener
        binding.btnRegister.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            binding.tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        binding.btnRegister.setEnabled(false);
        binding.btnRegister.setText("Registering...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration success
                        Toast.makeText(AdminRegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AdminRegisterActivity.this, AdminHomeActivity.class));
                        finish();
                    } else {
                        // Registration failed
                        Toast.makeText(AdminRegisterActivity.this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        binding.btnRegister.setEnabled(true);
                        binding.btnRegister.setText("Register");
                    }
                });
    }
}