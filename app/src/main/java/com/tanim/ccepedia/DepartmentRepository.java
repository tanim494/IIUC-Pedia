package com.tanim.ccepedia;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;
import java.util.List;

public class DepartmentRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String COLLECTION_NAME = "departments";

    public Task<List<String>> fetchAllDepartmentIds() {
        return db.collection(COLLECTION_NAME)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> departmentIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String deptId = document.getId();
                            departmentIds.add(deptId);
                        }
                        return departmentIds;
                    } else {
                        throw task.getException() != null ? task.getException() : new Exception("Failed to fetch department IDs.");
                    }
                });
    }
}