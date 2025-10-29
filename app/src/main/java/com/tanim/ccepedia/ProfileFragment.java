package com.tanim.ccepedia;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private TextView nameText, idText, emailText, phoneText, semesterText, viewCountText;
    private TextInputLayout nameInputLayout, idInputLayout, phoneInputLayout, semesterInputLayout;
    private EditText nameEdit, idEdit, phoneEdit;
    private AutoCompleteTextView semesterEdit;
    private Button editButton, logoutButton, saveButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        nameText = view.findViewById(R.id.nameText);
        idText = view.findViewById(R.id.idText);
        emailText = view.findViewById(R.id.emailText);
        phoneText = view.findViewById(R.id.phoneText);
        semesterText = view.findViewById(R.id.semesterText);
        viewCountText = view.findViewById(R.id.viewCountText);

        nameInputLayout = view.findViewById(R.id.nameInputLayout);
        idInputLayout = view.findViewById(R.id.idInputLayout);
        phoneInputLayout = view.findViewById(R.id.phoneInputLayout);
        semesterInputLayout = view.findViewById(R.id.semesterInputLayout);


        nameEdit = view.findViewById(R.id.nameEdit);
        idEdit = view.findViewById(R.id.idEdit);
        phoneEdit = view.findViewById(R.id.phoneEdit);
        semesterEdit = view.findViewById(R.id.semesterEdit);

        editButton = view.findViewById(R.id.editButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        saveButton = view.findViewById(R.id.saveButton);

        editButton.setOnClickListener(v -> switchToEditMode());
        saveButton.setOnClickListener(v -> saveUserData());
        logoutButton.setOnClickListener(v -> logoutUser());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserData();


        boolean canViewMetrics = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));

        if (canViewMetrics) {
            viewCountText.setVisibility(View.VISIBLE);
            loadViewCount();
        } else {
            viewCountText.setVisibility(View.GONE);
        }

        return view;
    }

    private void loadUserData() {
        UserData user = UserData.getInstance();

        nameText.setText(user.getName());
        idText.setText("ID: " + user.getStudentId());
        emailText.setText("Email: " + user.getEmail());
        phoneText.setText("Phone: " + user.getPhone());
        semesterText.setText("Semester: " + user.getSemester());

        nameEdit.setText(user.getName());
        idEdit.setText(user.getStudentId());
        phoneEdit.setText(user.getPhone());
        semesterEdit.setText(user.getSemester());
    }

    private void loadViewCount() {
        UserData user = UserData.getInstance();
        String currentStudentId = user.getStudentId();

        viewCountText.setText("Views: Loading...");

        db.collection("viewCounter")
                .document(currentStudentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Long totalViews = 0L;
                    if (documentSnapshot.exists()) {
                        Long count = documentSnapshot.getLong("viewCount");
                        if (count != null) {
                            totalViews = count;
                        }
                    }
                    viewCountText.setText("Total File Views: " + String.valueOf(totalViews));
                })
                .addOnFailureListener(e -> viewCountText.setText("Total File Views: N/A (Error)"));
    }


    private void switchToEditMode() {
        idText.setVisibility(View.GONE);
        phoneText.setVisibility(View.GONE);
        semesterText.setVisibility(View.GONE);
        viewCountText.setVisibility(View.GONE);

        nameInputLayout.setVisibility(View.VISIBLE);
        idInputLayout.setVisibility(View.VISIBLE);
        phoneInputLayout.setVisibility(View.VISIBLE);
        semesterInputLayout.setVisibility(View.VISIBLE);

        String[] SEMESTERS = new String[] {"1", "2", "3", "4", "5", "6", "7", "8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, SEMESTERS);
        semesterEdit.setAdapter(adapter);
        semesterEdit.setOnClickListener(v -> semesterEdit.showDropDown());

        editButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.VISIBLE);
    }


    private void switchToViewMode() {
        nameInputLayout.setVisibility(View.GONE);
        idInputLayout.setVisibility(View.GONE);
        phoneInputLayout.setVisibility(View.GONE);
        semesterInputLayout.setVisibility(View.GONE);

        idText.setVisibility(View.VISIBLE);
        phoneText.setVisibility(View.VISIBLE);
        semesterText.setVisibility(View.VISIBLE);

        boolean canViewMetrics = UserData.getInstance().getRole() != null &&
                (UserData.getInstance().getRole().equalsIgnoreCase("admin") ||
                        UserData.getInstance().getRole().equalsIgnoreCase("moderator"));

        if (canViewMetrics) {
            viewCountText.setVisibility(View.VISIBLE);
        }

        editButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.GONE);
    }

    private void saveUserData() {
        String newName = nameEdit.getText().toString().trim();
        String newId = idEdit.getText().toString().trim();
        String newPhone = phoneEdit.getText().toString().trim();
        String newSemester = semesterEdit.getText().toString().trim();

        if (newName.isEmpty() || newId.isEmpty() || newSemester.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .update("name", newName,
                        "id", newId,
                        "phone", newPhone,
                        "semester", newSemester)
                .addOnSuccessListener(aVoid -> {
                    UserData user = UserData.getInstance();
                    user.setName(newName);
                    user.setStudentId(newId);
                    user.setPhone(newPhone);
                    user.setSemester(newSemester);

                    loadUserData();
                    switchToViewMode();
                    Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}