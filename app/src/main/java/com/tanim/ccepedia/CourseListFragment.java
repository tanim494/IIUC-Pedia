package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CourseListFragment extends Fragment implements CourseAdapter.OnCourseClickListener {

    private static final String ARG_SEMESTER_ID = "semesterId";
    private String semesterId;

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private CourseAdapter adapter;
    private SearchView searchView;

    public static CourseListFragment newInstance(String semesterId) {
        CourseListFragment fragment = new CourseListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEMESTER_ID, semesterId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            semesterId = getArguments().getString(ARG_SEMESTER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_list, container, false);

        recyclerView = view.findViewById(R.id.courseRecyclerView);
        searchView = view.findViewById(R.id.searchViewCourses);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CourseAdapter(new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        setupSearch();
        fetchCourses();
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

    private void fetchCourses() {
        db.collection("semesters")
                .document(semesterId)
                .collection("courses")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(requireContext(), "No courses found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Course> newCourseList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String id = doc.getId();

                        String title = id;

                        newCourseList.add(new Course(id, title));
                    }
                    adapter.updateData(newCourseList);
                })
                .addOnFailureListener(e -> Toast.makeText(requireContext(), "Failed to load courses: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onCourseClick(String courseId) {
        openFileListFragment(courseId);
    }

    private void openFileListFragment(String courseId) {
        FileListFragment fragment = FileListFragment.newInstance(semesterId, courseId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }
}