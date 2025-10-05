package com.tanim.ccepedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputLayout emailField;
    private TextInputEditText passwordText;
    private TextInputLayout passwordField;

    private Button loginButton, resetPasswordButton;
    private TextView registerTextView, forgotPasswordTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Bind the UI elements
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);

        emailEditText = findViewById(R.id.emailEditText);
        passwordText = findViewById(R.id.passwordText);

        loginButton = findViewById(R.id.loginButton);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        registerTextView = findViewById(R.id.registerTextView);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        // Login Button Click Listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Register Link Click Listener
        registerTextView.setOnClickListener(v -> goToRegister());

        // Forgot Password Link Click Listener (Used to toggle between Login/Reset views)
        forgotPasswordTextView.setOnClickListener(v -> toggleLoginView());

        // Reset Password Button Click Listener
        resetPasswordButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            } else {
                resetPassword(email);
            }
        });
    }

    private void handleUserDocument(DocumentSnapshot snapshot) {
        if (snapshot.exists()) {
            // Populate Singleton with user data
            UserData user = UserData.getInstance();
            user.setStudentId(snapshot.getString("id"));
            user.setName(snapshot.getString("name"));
            user.setEmail(snapshot.getString("email"));
            user.setGender(snapshot.getString("gender"));
            user.setPhone(snapshot.getString("phone"));
            user.setSemester(snapshot.getString("semester"));

            String role = snapshot.getString("role");
            if (role == null || role.isBlank()) {
                user.setRole("");
            } else {
                user.setRole(role);
            }

            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            showAlert("User data not found, contact the developer");
        }
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                String uid = user.getUid();

                                // Update "verified" field to true
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                db.collection("users")
                                        .document(uid)
                                        .update("verified", true)
                                        .addOnSuccessListener(aVoid -> {
                                            // fetch user data
                                            db.collection("users")
                                                    .document(uid)
                                                    .get()
                                                    .addOnSuccessListener(this::handleUserDocument)
                                                    .addOnFailureListener(e ->
                                                            showAlert("Failed to fetch user data"));
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(LoginActivity.this, "Failed to update verification status", Toast.LENGTH_SHORT).show());

                            } else {
                                // Resend email verification
                                user.sendEmailVerification()
                                        .addOnSuccessListener(aVoid ->
                                                showAlert("Email not verified. A verification email has been sent."))
                                        .addOnFailureListener(e ->
                                                showAlert("Failed to resend verification email. Please try again."));
                            }
                        }
                    } else {
                        showAlert("Incorrect Email/Password");
                    }
                });
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Toggles the UI between the standard Login view and the Reset Password view.
     */
    private void toggleLoginView() {
        if (resetPasswordButton.getVisibility() == View.VISIBLE) {
            showLoginView();
        } else {
            showResetPasswordView();
        }
    }

    private void showLoginView() {
        resetPasswordButton.setVisibility(View.GONE);
        loginButton.setVisibility(View.VISIBLE);

        // Show TextInputLayouts for both fields
        emailField.setVisibility(View.VISIBLE);
        passwordField.setVisibility(View.VISIBLE);

        registerTextView.setVisibility(View.VISIBLE);
        forgotPasswordTextView.setVisibility(View.VISIBLE);
        forgotPasswordTextView.setText("Forgot Password?");
    }

    // Show Reset Password UI
    private void showResetPasswordView() {
        // HIDE the Password input field and Login button
        passwordField.setVisibility(View.GONE);
        loginButton.setVisibility(View.GONE);

        // Show reset password button
        resetPasswordButton.setVisibility(View.VISIBLE);

        // Hide register link
        registerTextView.setVisibility(View.GONE);

        // FIX: Ensure email field is VISIBLE for password reset email entry
        emailField.setVisibility(View.VISIBLE);

        forgotPasswordTextView.setText("Go to Login");
    }

    // Reset Password Functionality
    private void resetPassword(String email) {
        if (email.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        // FIX: Replaced the faulty fetchSignInMethodsForEmail check with a direct call
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(resetTask -> {
                    if (resetTask.isSuccessful()) {
                        // Use a generic success message as Firebase may succeed even if the email doesn't exist (security practice)
                        Toast.makeText(LoginActivity.this, "If this email is registered, a password reset link has been sent.", Toast.LENGTH_LONG).show();

                        // Switch back to login view after sending reset email
                        showLoginView();
                    } else {
                        // This block handles general failure (network, invalid format, etc.)
                        Toast.makeText(LoginActivity.this, "Failed to send reset email. Please check the email address.", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Go to Register Activity
    private void goToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}