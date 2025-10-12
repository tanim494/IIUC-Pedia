package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class FileListFragment extends Fragment {

    private static final String ARG_SEMESTER_ID = "semesterId";
    private static final String ARG_COURSE_ID = "courseId";

    private String semesterId;
    private String courseId;

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private ProgressBar loadingSpinner;
    private SearchView searchView;

    public static FileListFragment newInstance(String semesterId, String courseId) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEMESTER_ID, semesterId);
        args.putString(ARG_COURSE_ID, courseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            semesterId = getArguments().getString(ARG_SEMESTER_ID);
            courseId = getArguments().getString(ARG_COURSE_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_file_list, container, false);

        recyclerView = view.findViewById(R.id.fileRecyclerView);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);
        searchView = view.findViewById(R.id.searchViewFiles);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FileAdapter(new ArrayList<>(), new FileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(FileItem item) {
                if (item.getUploaderStudentId() != null) {
                    ViewTracker.incrementUserViewCount(item.getUploaderStudentId());
                }

                PdfViewerFragment fragment = PdfViewerFragment.newInstance(item.getUrl(), item.getFileName(), item.getUploaderStudentId());
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.Midcontainer, fragment)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onDeleteClick(FileItem item) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete File")
                        .setMessage("Are you sure you want to delete \"" + item.getFileName() + "\"?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteFile(item))
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchFiles();
        return view;
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filterList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filterList(newText);
                return true;
            }
        });
    }

    private void fetchFiles() {
        loadingSpinner.setVisibility(View.VISIBLE);
        db.collection("semesters")
                .document(semesterId)
                .collection("courses")
                .document(courseId)
                .collection("files")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No files found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<FileItem> fetchedList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();
                        String fileName = doc.getString("fileName");
                        String url = doc.getString("url");
                        String uploader = doc.getString("uploadedBy");
                        String uploaderStudentId = doc.getString("uploaderStudentId");

                        FileItem item = new FileItem(id, fileName, url, uploader);
                        item.setUploaderStudentId(uploaderStudentId);
                        fetchedList.add(item);
                    }

                    adapter.updateData(fetchedList);
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteFile(FileItem item) {
        Toast.makeText(requireContext(), "Deleting file...", Toast.LENGTH_SHORT).show();

        StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                .child(semesterId + "/" + courseId + "/" + item.getFileName());

        fileRef.delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("semesters")
                            .document(semesterId)
                            .collection("courses")
                            .document(courseId)
                            .collection("files")
                            .document(item.getId())
                            .delete()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(requireContext(), "File deleted successfully", Toast.LENGTH_SHORT).show();
                                fetchFiles();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Failed to delete metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to delete file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}