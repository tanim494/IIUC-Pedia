package com.tanim.ccepedia;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class Faculty extends Fragment {

    private static final String TAG = "FacultyFragment";

    private RecyclerView facultyRecyclerView;
    private FacultyAdapter facultyAdapter;
    private List<FacultyModel> facultyList;
    private ProgressBar loadingSpinner;
    private TextView errorText;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty, container, false);

        facultyRecyclerView = view.findViewById(R.id.facultyRecyclerView);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        errorText = view.findViewById(R.id.errorText);

        db = FirebaseFirestore.getInstance();
        facultyList = new ArrayList<>();

        facultyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        facultyAdapter = new FacultyAdapter(getContext(), facultyList);
        facultyRecyclerView.setAdapter(facultyAdapter);

        loadFacultyData();

        return view;
    }

    private void loadFacultyData() {
        loadingSpinner.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);

        db.collection("faculties")
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        facultyList.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                FacultyModel faculty = doc.toObject(FacultyModel.class);
                                facultyList.add(faculty);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse faculty document: " + doc.getId(), e);
                            }
                        }
                        facultyAdapter.notifyDataSetChanged();
                    } else {
                        errorText.setText("No faculty data available.");
                        errorText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    errorText.setText("Failed to load faculty data. Please check your internet connection.");
                    errorText.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading faculty data", e);
                });
    }
}