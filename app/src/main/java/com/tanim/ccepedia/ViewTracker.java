package com.tanim.ccepedia;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewTracker {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String USERS_COLLECTION = "users";

    public static void incrementUserViewCount(String uploaderStudentId) {
        if (uploaderStudentId == null || uploaderStudentId.isEmpty()) {
            return;
        }

        db.collection(USERS_COLLECTION)
                .whereEqualTo("id", uploaderStudentId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        DocumentReference userRef = db.collection(USERS_COLLECTION).document(docId);
                        userRef.update("viewCount", FieldValue.increment(1));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewTracker", "Failed to find user to increment view count", e);
                });
    }
}