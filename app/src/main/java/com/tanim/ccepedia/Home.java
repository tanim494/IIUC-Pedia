package com.tanim.ccepedia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


public class Home extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseFirestore db;

    private RecyclerView noticesRecyclerView;
    private RecyclerView quickActionsRecyclerView;
    private RecyclerView latestUpdatesRecyclerView;

    private NoticeAdapter noticeAdapter;
    private LatestUpdatesAdapter latestUpdatesAdapter;
    private MaterialCardView chatBotBtn, communityBtn;

    private Button adminBtn, uploadBtn;
    private LinearLayout controlLayout;

    private final List<String> latestUpdatesList = new ArrayList<>();
    private final List<Notice> noticesList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        controlLayout = view.findViewById(R.id.controlLayout);
        adminBtn = view.findViewById(R.id.adminBtn);
        uploadBtn = view.findViewById(R.id.uploadBtn);
        chatBotBtn = view.findViewById(R.id.card_chatbot);
        communityBtn = view.findViewById(R.id.card_community);

        noticesRecyclerView = view.findViewById(R.id.noticesRecyclerView);
        quickActionsRecyclerView = view.findViewById(R.id.quickActionsRecyclerView);
        latestUpdatesRecyclerView = view.findViewById(R.id.latestUpdatesRecyclerView);

        db = FirebaseFirestore.getInstance();

        setupNotices();
        setupQuickActions();
        setupLatestUpdates();

        setupInteractiveTools();

        loadAllNotices();
        loadLatestUpdates();

        setUserData();

        return view;
    }

    private void setupInteractiveTools() {
        chatBotBtn.setOnClickListener(v -> openChatBot());
        communityBtn.setOnClickListener(v -> openCommunity());
    }

    private void setupNotices() {
        noticesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(noticesRecyclerView);
        noticeAdapter = new NoticeAdapter(noticesList, this::handleNoticeClick);
        noticesRecyclerView.setAdapter(noticeAdapter);
    }

    private void setupLatestUpdates() {
        latestUpdatesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        latestUpdatesAdapter = new LatestUpdatesAdapter(latestUpdatesList);
        latestUpdatesRecyclerView.setAdapter(latestUpdatesAdapter);
    }

    private void loadAllNotices() {
        db.collection("notices")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    noticesList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String text = doc.getString("text");
                            String link = doc.getString("link");

                            if (text != null && !text.isEmpty()) {
                                noticesList.add(new Notice(text, link));
                            }
                        }
                    }

                    if (noticesList.isEmpty()) {
                        noticesRecyclerView.setVisibility(View.GONE);
                    }

                    noticeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading announcements", e);
                    noticesList.clear();
                    noticesList.add(new Notice("Failed to load announcements.", null));
                    noticeAdapter.notifyDataSetChanged();
                });
    }

    private void loadLatestUpdates() {
        db.collection("messages")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        latestUpdatesList.clear();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String msg = doc.getString("text");
                            if (msg != null) {
                                latestUpdatesList.add(msg);
                            }
                        }
                        latestUpdatesAdapter.notifyDataSetChanged();
                    } else {
                        latestUpdatesList.clear();
                        latestUpdatesList.add("No latest updates at the moment.");
                        latestUpdatesAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading updates", e);
                    latestUpdatesList.clear();
                    latestUpdatesList.add("Failed to load updates.");
                    latestUpdatesAdapter.notifyDataSetChanged();
                });
    }

    private void setUserData() {
        String userRole = UserData.getInstance().getRole();

        if (userRole != null && userRole.equalsIgnoreCase("admin")) {
            controlLayout.setVisibility(View.VISIBLE);
            adminBtn.setVisibility(View.VISIBLE);
            adminBtn.setOnClickListener(v -> openAdminMode());

            uploadBtn.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(v -> openFileUpload());
        } else if (userRole != null && userRole.equalsIgnoreCase("moderator")) {
            controlLayout.setVisibility(View.VISIBLE);
            uploadBtn.setVisibility(View.VISIBLE);
            uploadBtn.setOnClickListener(v -> openFileUpload());
        }
    }

    private void handleNoticeClick(String link) {
        if (link != null && !link.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(link));
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "No external link available for this notice.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupQuickActions() {
        List<QuickActionItem> quickActionList = new ArrayList<>();

        quickActionList.add(new QuickActionItem(UserData.getInstance().getSemester() + " Semester", R.drawable.ic_pdf, QuickActionItem.ACTION_RESOURCES));
        quickActionList.add(new QuickActionItem("Bus Tracker", R.drawable.ic_tracker, QuickActionItem.ACTION_BUS_TRACKER));
        quickActionList.add(new QuickActionItem("Student Portal", R.drawable.ic_stportal, QuickActionItem.ACTION_STUDENT_PORTAL));
        quickActionList.add(new QuickActionItem("Bus Schedule", R.drawable.ic_bus, QuickActionItem.ACTION_BUS_SCHEDULE));
        quickActionList.add(new QuickActionItem("CP in Java", R.drawable.ic_java, QuickActionItem.ACTION_JAVACP));
        quickActionList.add(new QuickActionItem("Java Resources", R.drawable.ic_java, QuickActionItem.ACTION_JAVARes));
        quickActionList.add(new QuickActionItem("IIUC Repository", R.drawable.ic_repository, QuickActionItem.ACTION_IIUCRepo));

        QuickActionsAdapter quickActionsAdapter = new QuickActionsAdapter(quickActionList, this::handleQuickActionClick);
        quickActionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        quickActionsRecyclerView.setAdapter(quickActionsAdapter);
    }

    private void handleQuickActionClick(int actionType, String actionTitle) {
        String currentSemester = UserData.getInstance().getSemester();

        switch (actionType) {
            case QuickActionItem.ACTION_RESOURCES:
                String semesterId = "semester_" + currentSemester;
                openCourseListFragment(semesterId);
                break;

            case QuickActionItem.ACTION_BUS_TRACKER:
            openBusTracker();
                break;

            case QuickActionItem.ACTION_STUDENT_PORTAL:
                openStudentPortal();
                break;

            case QuickActionItem.ACTION_BUS_SCHEDULE:
                openBusSchedule();
                break;

            case QuickActionItem.ACTION_JAVACP:
                openWebPage("https://github.com/tanim494/CodeForces");
                break;

            case QuickActionItem.ACTION_JAVARes:
                openWebPage("https://github.com/tanim494/Java-Sessional-Resources");
                break;

            case QuickActionItem.ACTION_IIUCRepo:
                openWebPage("https://dspace.iiuc.ac.bd/home");
                break;

            default:
                Log.w(TAG, "Unhandled quick action type: " + actionType);
        }
    }

    private void openStudentPortal() {
        StudentPortalFragment portalFragment = new StudentPortalFragment();

        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, portalFragment)
                .addToBackStack(null)
                .commit();
    }

    public static class QuickActionItem {
        public static final int ACTION_RESOURCES = 1;
        public static final int ACTION_BUS_TRACKER = 2;
        public static final int ACTION_STUDENT_PORTAL = 3;
        public static final int ACTION_BUS_SCHEDULE = 4;
        public static final int ACTION_JAVACP = 5;
        public static final int ACTION_JAVARes = 6;
        public static final int ACTION_IIUCRepo = 7;

        public final String title;
        public final int iconResId;
        public final int actionType;

        public QuickActionItem(String title, int iconResId, int actionType) {
            this.title = title;
            this.iconResId = iconResId;
            this.actionType = actionType;
        }
    }

    private void openBusTracker() {
        String url = "https://transport.iiuc.ac.bd/student/home";
        openCustomTab(url);
    }

    private void openCustomTab(String url) {
        if (getContext() == null) return;
        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

            builder.setToolbarColor(ContextCompat.getColor(getContext(), R.color.Green));

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url));

        } catch (Exception e) {
            Log.e(TAG, "Custom Tabs failed, falling back to external browser.", e);
            openWebPage(url);
        }
    }

    private void openCourseListFragment(String semesterId) {
        CourseListFragment fragment = CourseListFragment.newInstance(semesterId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.Midcontainer, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openBusSchedule() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, new BusScheduleFragment());
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void openAdminMode() {
        AdminFragment adminFragment = new AdminFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, adminFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openFileUpload() {
        UploadFile uploadFragment = new UploadFile();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.Midcontainer, uploadFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openCommunity() {
        Intent intent = new Intent(requireContext(), CommunityActivity.class);
        requireActivity().startActivity(intent);
    }

    private void openChatBot() {
        Intent intent = new Intent(requireContext(), AIChatActivity.class);
        requireActivity().startActivity(intent);
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    public static class Notice {
        public final String text;
        public final String link;

        public Notice(String text, String link) {
            this.text = text;
            this.link = link;
        }
    }

    public static class NoticeAdapter extends RecyclerView.Adapter<NoticeAdapter.ViewHolder> {

        private final List<Notice> items;
        private final NoticeClickListener listener;

        public interface NoticeClickListener {
            void onNoticeClick(String link);
        }

        public NoticeAdapter(List<Notice> items, NoticeClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notice, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notice item = items.get(position);
            holder.noticeText.setText(item.text);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNoticeClick(item.link);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView noticeText;

            public ViewHolder(View itemView) {
                super(itemView);
                noticeText = itemView.findViewById(R.id.noticeContentTextView);
            }
        }
    }

    public static class QuickActionsAdapter extends RecyclerView.Adapter<QuickActionsAdapter.ViewHolder> {

        private final List<QuickActionItem> items;
        private final QuickActionClickListener listener;

        public interface QuickActionClickListener {
            void onQuickActionClick(int actionType, String actionTitle);
        }

        public QuickActionsAdapter(List<QuickActionItem> items, QuickActionClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_action, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuickActionItem item = items.get(position);
            holder.actionText.setText(item.title);
            holder.actionIcon.setImageResource(item.iconResId);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuickActionClick(item.actionType, item.title);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView actionText;
            final ImageView actionIcon;

            public ViewHolder(View itemView) {
                super(itemView);
                actionText = itemView.findViewById(R.id.actionText);
                actionIcon = itemView.findViewById(R.id.actionIcon);
            }
        }
    }

    public static class LatestUpdatesAdapter extends RecyclerView.Adapter<LatestUpdatesAdapter.ViewHolder> {
        private final List<String> data;

        public LatestUpdatesAdapter(List<String> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_latest_update, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public final TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.update_text);
            }
        }
    }

}