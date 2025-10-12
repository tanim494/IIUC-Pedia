package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BatchWiseFragment extends Fragment {

    private static final String ARG_GENDER = "gender";
    private String gender;
    private RecyclerView recyclerView;
    private DriveLinkAdapter adapter;
    private List<DriveLink> linkList;
    private List<DriveLink> fullLinkList;
    private androidx.appcompat.widget.SearchView searchView;

    public static BatchWiseFragment newInstance(String gender) {
        BatchWiseFragment fragment = new BatchWiseFragment();
        Bundle args = new Bundle();
        args.putString(ARG_GENDER, gender);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            gender = getArguments().getString(ARG_GENDER);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_batch_wise, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewBatchWise);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchView = view.findViewById(R.id.searchViewBatchResources);

        linkList = new ArrayList<>();
        fullLinkList = new ArrayList<>();
        adapter = new DriveLinkAdapter(linkList);
        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchBatchWiseLinks();

        return view;
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    adapter.filterList(newText);
                }
                return true;
            }
        });
    }


    private void fetchBatchWiseLinks() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("batch_wise_links")
                .whereEqualTo("gender", gender)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    fullLinkList.clear();
                    for (DocumentSnapshot doc : querySnapshots) {
                        String title = doc.getString("title");
                        String url = doc.getString("url");
                        DriveLink link = new DriveLink(title, url);
                        fullLinkList.add(link);
                    }

                    linkList.clear();
                    linkList.addAll(fullLinkList);
                    adapter.setFullList(fullLinkList);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load batch wise links", Toast.LENGTH_SHORT).show()
                );
    }
}