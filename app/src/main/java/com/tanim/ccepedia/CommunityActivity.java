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
import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.tanim.ccepedia.CommunityChatAdapter.MessageInteractionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CommunityActivity extends AppCompatActivity implements MessageInteractionListener {
    private static final String TAG = "CommunityActivity";

    private FirebaseFirestore db;
    private CollectionReference chatRef;

    private RecyclerView recyclerView;
    private CommunityChatAdapter chatAdapter;
    private final List<CommunityMessage> messageList = new ArrayList<>();
    private LinearLayoutManager layoutManager;

    private EditText messageEditText;
    private FloatingActionButton sendButton;

    private final String currentUserEmail = UserData.getInstance().getEmail();
    private final String currentStudentId = UserData.getInstance().getStudentId();
    private final String currentUserName = UserData.getInstance().getName();
    private final String currentUserDepartment = UserData.getInstance().getDepartmentName();

    private boolean isLoading = false;
    private boolean moreMessagesAvailable = true;
    private DocumentSnapshot oldestMessageSnapshot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        Toolbar toolbar = findViewById(R.id.toolbar_community);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Community Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.communityChatRecyclerView);
        messageEditText = findViewById(R.id.communityMessageEditText);
        sendButton = findViewById(R.id.communitySendButton);

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        chatAdapter = new CommunityChatAdapter(messageList, currentStudentId, this);
        recyclerView.setAdapter(chatAdapter);

        db = FirebaseFirestore.getInstance();
        chatRef = db.collection("community_messages");

        sendButton.setOnClickListener(v -> sendMessage());
        messageEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0 && dy < 0) {
                    if (!isLoading && moreMessagesAvailable) {
                        loadMoreMessages();
                    }
                }
            }
        });

        loadMessages();
    }

    @Override
    public void onDeleteMessage(CommunityMessage message) {
        showDeleteDialog(message);
    }

    private boolean canCurrentUserDelete(CommunityMessage message) {
        if (UserData.getInstance() == null) return false;
        String currentUserRole = UserData.getInstance().getRole();
        if ("admin".equals(currentUserRole)) {
            return true;
        }
        return currentStudentId != null && currentStudentId.equalsIgnoreCase(message.getUserStudentId());
    }

    public void showDeleteDialog(CommunityMessage message) {
        if (canCurrentUserDelete(message)) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Message")
                    .setMessage("Are you sure you want to delete this message? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

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
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Message deleted.", Toast.LENGTH_SHORT).show())
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

    private void loadMessages() {
        isLoading = true;
        chatRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        isLoading = false;
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        oldestMessageSnapshot = snapshots.getDocuments().get(snapshots.size() - 1);

                        List<CommunityMessage> newMessages = new ArrayList<>();
                        for (CommunityMessage message : snapshots.toObjects(CommunityMessage.class)) {
                            newMessages.add(message);
                        }
                        Collections.reverse(newMessages);

                        messageList.clear();
                        messageList.addAll(newMessages);

                        chatAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messageList.size() - 1);
                    }
                    isLoading = false;
                    moreMessagesAvailable = snapshots.size() >= 50;
                });
    }

    private void loadMoreMessages() {
        if (oldestMessageSnapshot == null) return;

        isLoading = true;
        moreMessagesAvailable = false;

        chatRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(oldestMessageSnapshot)
                .limit(50)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots != null && !snapshots.isEmpty()) {
                        oldestMessageSnapshot = snapshots.getDocuments().get(snapshots.size() - 1);

                        List<CommunityMessage> olderMessages = new ArrayList<>();
                        for (CommunityMessage message : snapshots.toObjects(CommunityMessage.class)) {
                            olderMessages.add(message);
                        }
                        Collections.reverse(olderMessages);

                        messageList.addAll(0, olderMessages);
                        chatAdapter.notifyItemRangeInserted(0, olderMessages.size());

                    }
                    isLoading = false;
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    Toast.makeText(this, "Failed to load older messages.", Toast.LENGTH_SHORT).show();
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
        message.put("userDepartment", currentUserDepartment);
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