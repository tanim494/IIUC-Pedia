package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class DriveLinksFragment extends Fragment {

    private RecyclerView recyclerView;
    private DriveLinkAdapter adapter;
    private List<DriveLink> driveLinks;
    private TextView emptyStateTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_links, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewLinks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        emptyStateTextView = view.findViewById(R.id.emptyStateLinksText);

        driveLinks = new ArrayList<>();
        adapter = new DriveLinkAdapter(driveLinks);
        recyclerView.setAdapter(adapter);

        fetchDriveLinksFromDB();

        return view;
    }

    private void fetchDriveLinksFromDB() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String deptCode = UserData.getInstance().getDepartmentName();

        if (deptCode == null || deptCode.isEmpty()) {
            Toast.makeText(getContext(), "Department not set.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference collectionRef;

        if (deptCode.equalsIgnoreCase("CCE")) {
            collectionRef = db.collection("drive_links");
        } else {
            String deptDocumentId = "dept_" + deptCode.toLowerCase();

            collectionRef = db.collection("departments")
                    .document(deptDocumentId)
                    .collection("drive_links");
        }

        emptyStateTextView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        collectionRef
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    driveLinks.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String title = doc.getString("title");
                        String url = doc.getString("url");
                        driveLinks.add(new DriveLink(title, url));
                    }

                    adapter.notifyDataSetChanged();

                    if (driveLinks.isEmpty()) {
                        emptyStateTextView.setText("No links found for the " + deptCode + " department.");
                        emptyStateTextView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyStateTextView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load links for " + deptCode, Toast.LENGTH_SHORT).show();

                    recyclerView.setVisibility(View.GONE);
                    emptyStateTextView.setText("Failed to connect to resources for " + deptCode + ".");
                    emptyStateTextView.setVisibility(View.VISIBLE);
                });
    }
}