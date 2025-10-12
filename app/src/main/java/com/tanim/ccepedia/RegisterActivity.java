package com.tanim.ccepedia;

import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.AutoCompleteTextView;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText nameEditText, idEditText, phoneEditText, emailEditText, passwordEditText;
    private AutoCompleteTextView genderAutoComplete, semesterAutoComplete;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        idEditText = findViewById(R.id.idEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        genderAutoComplete = findViewById(R.id.genderSpinner);
        semesterAutoComplete = findViewById(R.id.semesterSpinner);

        registerButton = findViewById(R.id.registerButton);

        String[] semesters = new String[9];
        semesters[0] = "Select Semester";
        for (int i = 1; i <= 8; i++) {
            semesters[i] = String.valueOf(i);
        }
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, semesters);
        semesterAutoComplete.setAdapter(semesterAdapter);

        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, genders);
        genderAutoComplete.setAdapter(genderAdapter);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String id = idEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        String gender = genderAutoComplete.getText().toString();
        String semester = semesterAutoComplete.getText().toString();

        if (name.isEmpty() || id.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Please fill in all fields");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showAlert("Invalid email format");
            return;
        }

        if (!isStrongPassword(password)) {
            showAlert("Password must be at least 6 characters long.");
            return;
        }

        if (gender.equals("Select Gender") || gender.equals("") || gender.equals("Gender") || semester.equals("Semester") || semester.equals("Select Semester") || semester.equals("")) {
            showAlert("Please select both gender and semester");
            return;
        }

        registerButton.setEnabled(false);

        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean emailExists = task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty();
                if (emailExists) {
                    showAlert("Email already in use");
                    registerButton.setEnabled(true);
                } else {
                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, authTask -> {
                                if (authTask.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    if (user != null) {
                                        user.sendEmailVerification()
                                                .addOnCompleteListener(emailTask -> {
                                                    if (emailTask.isSuccessful()) {
                                                        User newUser = new User(name, id, phone, email, gender, semester);
                                                        db.collection("users")
                                                                .document(user.getUid())
                                                                .set(newUser)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    showAlert("Registration successful. Please verify your email.");
                                                                    mAuth.signOut();
                                                                    new Handler().postDelayed(this::finish, 1500);
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    showAlert("Error saving user data: " + e.getMessage());
                                                                    registerButton.setEnabled(true);
                                                                });
                                                    } else {
                                                        showAlert("Failed to send verification email.");
                                                        registerButton.setEnabled(true);
                                                    }
                                                });
                                    }
                                } else {
                                    showAlert(authTask.getException().getMessage());
                                    registerButton.setEnabled(true);
                                }
                            });
                }
            } else {
                Toast.makeText(this, "Failed to check email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                registerButton.setEnabled(true);
            }
        });
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 6;
    }

    private void showAlert(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Alert!")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static class User {
        private String name;
        private String id;
        private String phone;
        private String email;
        private String gender;
        private String semester;
        private boolean verified;

        public User() {
        }

        public User(String name, String id, String phone, String email, String gender, String semester) {
            this.name = name;
            this.id = id;
            this.phone = phone;
            this.email = email;
            this.gender = gender;
            this.semester = semester;
            this.verified = false;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getSemester() { return semester; }
        public void setSemester(String semester) { this.semester = semester; }

        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }

    }
}