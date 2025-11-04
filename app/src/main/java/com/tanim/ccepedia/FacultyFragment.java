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
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FacultyFragment extends Fragment {

    private static final String TAG = "FacultyFragment";

    private RecyclerView facultyRecyclerView;
    private FacultyAdapter facultyAdapter;
    private List<FacultyModel> facultyList;
    private List<FacultyModel> fullFacultyList;
    private ProgressBar loadingSpinner;
    private TextView errorText;
    private FirebaseFirestore db;
    private SearchView facultySearchView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_faculty, container, false);

        facultyRecyclerView = view.findViewById(R.id.facultyRecyclerView);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        errorText = view.findViewById(R.id.errorText);
        facultySearchView = view.findViewById(R.id.facultySearchView);

        db = FirebaseFirestore.getInstance();
        facultyList = new ArrayList<>();
        fullFacultyList = new ArrayList<>();

        facultyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        facultyAdapter = new FacultyAdapter(getContext(), facultyList);
        facultyRecyclerView.setAdapter(facultyAdapter);

        setupSearch();
        loadFacultyData();

        return view;
    }

    private void setupSearch() {
        facultySearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                facultyAdapter.filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                facultyAdapter.filterList(newText);
                return true;
            }
        });
    }

    private void loadFacultyData() {
        String deptCode = UserData.getInstance().getDepartmentName();

        if (deptCode == null || deptCode.isEmpty()) {
            errorText.setText("Department not set.");
            errorText.setVisibility(View.VISIBLE);
            return;
        }

        CollectionReference collectionRef;

        if (deptCode.equalsIgnoreCase("CCE")) {
            collectionRef = db.collection("faculties");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();

            collectionRef = db.collection("departments")
                    .document(deptDocumentId)
                    .collection("faculties");
        }

        loadingSpinner.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);

        collectionRef
                .orderBy("order")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        facultyList.clear();
                        fullFacultyList.clear();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            try {
                                FacultyModel faculty = doc.toObject(FacultyModel.class);
                                facultyList.add(faculty);
                                fullFacultyList.add(faculty);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to parse faculty document: " + doc.getId(), e);
                            }
                        }
                        facultyAdapter.setFullList(fullFacultyList);
                        facultyAdapter.notifyDataSetChanged();
                    } else {
                        errorText.setText("No faculty data available for " + deptCode + ".");
                        errorText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    errorText.setText("Failed to load faculty data for " + deptCode + ". Check internet.");
                    errorText.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading faculty data for " + deptCode, e);
                });
    }
}