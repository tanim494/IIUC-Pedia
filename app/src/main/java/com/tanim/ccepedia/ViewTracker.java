package com.tanim.ccepedia;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class ViewTracker {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String COLLECTION_NAME = "viewCounter";

    public static void incrementUserViewCount(String uploaderStudentId) {
        DocumentReference counterRef = db.collection(COLLECTION_NAME).document(uploaderStudentId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("viewCount", FieldValue.increment(1));
        counterRef.set(updates, SetOptions.merge())
                .addOnFailureListener(e -> {});
    }
}