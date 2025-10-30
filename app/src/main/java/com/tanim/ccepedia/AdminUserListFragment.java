package com.tanim.ccepedia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.appcompat.widget.SearchView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminUserListFragment extends Fragment {

    private Spinner spinnerGender, spinnerSemester, spinnerRole, spinnerVerified;
    private SearchView searchView;
    private RecyclerView recyclerViewUsers;
    private UserListAdapter userAdapter;
    private List<UserListModel> userList;
    private List<UserListModel> allUsers;

    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_user_list, container, false);

        db = FirebaseFirestore.getInstance();

        spinnerGender = view.findViewById(R.id.spinnerGender);
        spinnerSemester = view.findViewById(R.id.spinnerSemester);
        spinnerRole = view.findViewById(R.id.spinnerRole);
        spinnerVerified = view.findViewById(R.id.spinnerVerified);
        searchView = view.findViewById(R.id.searchView);

        searchView.setIconified(false);

        recyclerViewUsers = view.findViewById(R.id.recyclerViewUsers);

        userList = new ArrayList<>();
        allUsers = new ArrayList<>();
        userAdapter = new UserListAdapter(getContext(), userList);

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewUsers.setAdapter(userAdapter);

        setupSpinners();

        loadUsers(null, null, null, null);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBySearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBySearch(newText);
                return false;
            }
        });

        return view;
    }

    private void setupSpinners() {
        int itemLayout = android.R.layout.simple_spinner_dropdown_item;

        String[] genders = {"All", "male", "female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(getContext(), itemLayout, genders);
        spinnerGender.setAdapter(genderAdapter);

        String[] semesters = new String[9];
        semesters[0] = "All";
        for (int i = 1; i <= 8; i++) {
            semesters[i] = String.valueOf(i);
        }
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(getContext(), itemLayout, semesters);
        spinnerSemester.setAdapter(semesterAdapter);

        String[] roles = {"All", "admin", "moderator", "user"};
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), itemLayout, roles);
        spinnerRole.setAdapter(roleAdapter);

        String[] verifiedOptions = {"All", "Verified", "Not Verified"};
        ArrayAdapter<String> verifiedAdapter = new ArrayAdapter<>(getContext(), itemLayout, verifiedOptions);
        spinnerVerified.setAdapter(verifiedAdapter);

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFiltersAndSearch();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerGender.setOnItemSelectedListener(filterListener);
        spinnerSemester.setOnItemSelectedListener(filterListener);
        spinnerRole.setOnItemSelectedListener(filterListener);
        spinnerVerified.setOnItemSelectedListener(filterListener);
    }

    private void applyFiltersAndSearch() {
        String gender = spinnerGender.getSelectedItem().toString();
        if (gender.equals("All")) gender = null;

        String semester = spinnerSemester.getSelectedItem().toString();
        if (semester.equals("All")) semester = null;

        String role = spinnerRole.getSelectedItem().toString();
        if (role.equals("All")) role = null;

        String verifiedString = spinnerVerified.getSelectedItem().toString();
        Boolean verifiedFilter = null;
        if (verifiedString.equals("Verified")) {
            verifiedFilter = true;
        } else if (verifiedString.equals("Not Verified")) {
            verifiedFilter = false;
        }

        loadUsers(gender, semester, role, verifiedFilter);

        filterBySearch(searchView.getQuery().toString());
    }

    private void loadUsers(String genderFilter, String semesterFilter, String roleFilter, Boolean verifiedFilter) {

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<UserListModel> fetchedUsers = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        UserListModel user = doc.toObject(UserListModel.class);
                        fetchedUsers.add(user);
                    }

                    Collections.sort(fetchedUsers, (u1, u2) -> {
                        if (u1.getLastLoggedIn() == null && u2.getLastLoggedIn() == null) return 0;
                        if (u1.getLastLoggedIn() == null) return 1;
                        if (u2.getLastLoggedIn() == null) return -1;
                        return u2.getLastLoggedIn().compareTo(u1.getLastLoggedIn());
                    });

                    userList.clear();
                    allUsers.clear();

                    for (UserListModel user : fetchedUsers) {
                        boolean genderMatch = (genderFilter == null || (user.getGender() != null && user.getGender().equalsIgnoreCase(genderFilter)));

                        boolean semesterMatch = (semesterFilter == null || (user.getSemester() != null && user.getSemester().equalsIgnoreCase(semesterFilter)));

                        boolean roleMatch;
                        if (roleFilter == null) {
                            roleMatch = true;
                        } else if (roleFilter.equals("user")) {
                            roleMatch = (user.getRole() == null || user.getRole().isEmpty() || user.getRole().equalsIgnoreCase("user"));
                        } else {
                            roleMatch = roleFilter.equalsIgnoreCase(user.getRole());
                        }

                        boolean verifiedMatch = true;
                        if (verifiedFilter != null) {
                            Boolean userVerified = user.isVerified();
                            verifiedMatch = userVerified.equals(verifiedFilter);
                        }

                        if (genderMatch && semesterMatch && roleMatch && verifiedMatch) {
                            allUsers.add(user);
                        }
                    }

                    userList.addAll(allUsers);
                    userAdapter.notifyDataSetChanged();
                });
    }

    private void filterBySearch(String query) {
        if (query == null) query = "";
        query = query.toLowerCase();

        List<UserListModel> filteredList = new ArrayList<>();

        for (UserListModel user : allUsers) {
            if ((user.getName() != null && user.getName().toLowerCase().contains(query)) ||
                    (user.getStudentId() != null && user.getStudentId().toLowerCase().contains(query))) {
                filteredList.add(user);
            }
        }

        userList.clear();
        userList.addAll(filteredList);
        userAdapter.notifyDataSetChanged();
    }
}