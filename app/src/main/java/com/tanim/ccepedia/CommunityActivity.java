package com.tanim.ccepedia;

import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.FieldValue;
import com.tanim.ccepedia.CommunityMessage;
// FIX: Correct Import Path for the interface now defined inside CommunityChatAdapter
import com.tanim.ccepedia.CommunityChatAdapter.MessageInteractionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// FIX: CommunityActivity now correctly implements the imported interface
public class CommunityActivity extends AppCompatActivity implements MessageInteractionListener {
    private static final String TAG = "CommunityActivity";

    private FirebaseFirestore db;
    private CollectionReference chatRef;

    private RecyclerView recyclerView;
    private CommunityChatAdapter chatAdapter;
    private final List<CommunityMessage> messageList = new ArrayList<>();

    private EditText messageEditText;
    private FloatingActionButton sendButton;

    // Current User Data (Using placeholders, replace with UserData.getInstance())
    private final String currentUserEmail = UserData.getInstance().getEmail();
    private final String currentStudentId = UserData.getInstance().getStudentId();
    private final String currentUserName = UserData.getInstance().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // --- 1. Toolbar Setup ---
        Toolbar toolbar = findViewById(R.id.toolbar_community);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Community Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- 2. UI Initialization ---
        recyclerView = findViewById(R.id.communityChatRecyclerView);
        messageEditText = findViewById(R.id.communityMessageEditText);
        sendButton = findViewById(R.id.communitySendButton);

        // --- 3. RecyclerView & Adapter Setup ---
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        // Pass 'this' as the MessageInteractionListener
        chatAdapter = new CommunityChatAdapter(messageList, currentStudentId, this);
        recyclerView.setAdapter(chatAdapter);

        // --- 4. Firestore Setup ---
        db = FirebaseFirestore.getInstance();
        chatRef = db.collection("community_messages");

        // --- 5. Event Listeners ---
        sendButton.setOnClickListener(v -> sendMessage());
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // --- 6. Load Messages (Real-time Listener) ---
        loadMessages();
    }

    // ===============================================
    //               DELETION LOGIC
    // ===============================================

    // Implementation of the interface method (triggered by single-click in adapter)
    @Override
    public void onDeleteMessage(CommunityMessage message) {
        showDeleteDialog(message);
    }

    /**
     * Checks if the current user has permission to delete the message.
     */
    private boolean canCurrentUserDelete(CommunityMessage message) {
        // Check if UserData is initialized and role is available
        if (UserData.getInstance() == null) return false;

        String currentUserRole = UserData.getInstance().getRole();

        // 1. Check if user is an admin
        if ("admin".equals(currentUserRole)) {
            return true;
        }

        // 2. Check if the message was sent by the current user
        return currentStudentId != null && currentStudentId.equals(message.getUserStudentId());
    }

    /**
     * Handles the display of the delete confirmation dialog on single click.
     */
    public void showDeleteDialog(CommunityMessage message) {
        if (canCurrentUserDelete(message)) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Message")
                    .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteMessage(message);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    /**
     * Deletes the message from Firestore.
     */
    private void deleteMessage(CommunityMessage message) {
        if (message.getTimestamp() == null) {
            Toast.makeText(this, "Cannot delete message without a valid timestamp.", Toast.LENGTH_SHORT).show();
            return;
        }

        chatRef.whereEqualTo("timestamp", message.getTimestamp())
                .whereEqualTo("userStudentId", message.getUserStudentId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        String documentId = task.getResult().getDocuments().get(0).getId();
                        chatRef.document(documentId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting message", e);
                                    Toast.makeText(this, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Log.w(TAG, "Message not found for deletion query.", task.getException());
                        Toast.makeText(this, "Message not found or failed to query.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ===============================================
    //                  CHAT LOGIC
    // ===============================================

    private void loadMessages() {
        chatRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .limitToLast(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        messageList.clear();
                        for (CommunityMessage message : snapshots.toObjects(CommunityMessage.class)) {
                            messageList.add(message);
                        }

                        chatAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("userStudentId", currentStudentId);
        message.put("userEmail", currentUserEmail);
        message.put("userName", currentUserName);
        message.put("messageText", messageText);
        message.put("timestamp", FieldValue.serverTimestamp());

        chatRef.add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message sent: " + documentReference.getId());
                    messageEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                });
    }
}