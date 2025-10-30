package com.tanim.ccepedia;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.TypedValue;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import androidx.fragment.app.Fragment;

public class DeveloperFragment extends Fragment {
    private LinearLayout moderatorListContainer;

    private static class AppUser {
        String name;
        String studentId;
        String role;

        public AppUser(String name, String studentId, String role) {
            this.name = name;
            this.studentId = studentId;
            this.role = role;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_developer, container, false);

        moderatorListContainer = view.findViewById(R.id.moderatorListContainer);
        fetchModerators();

        TextView appVersion = view.findViewById(R.id.appVersionText);
        appVersion.setText("App Version " + BuildConfig.VERSION_NAME);

        LinearLayout githubButton = view.findViewById(R.id.githubButton);
        githubButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tanim494"));
            startActivity(intent);
        });

        LinearLayout facebookButton = view.findViewById(R.id.facebookButton);
        facebookButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.facebook.com/tanim494"));
            startActivity(intent);
        });

        LinearLayout websiteButton = view.findViewById(R.id.linkedinButton);
        websiteButton.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/tanim494/"));
            startActivity(intent);
        });

        LinearLayout authorAddress = view.findViewById(R.id.authorAddress);
        authorAddress.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.app.goo.gl/KimSYJ3GMz9F6QfMA"));
            startActivity(intent);
        });

        LinearLayout authorMail = view.findViewById(R.id.authorMail);
        authorMail.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822");

            intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"Tanim494@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Request Feature or Suggestion for CCE Pedia");

            try {
                startActivity(Intent.createChooser(intent, "Send Email"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), "No email app installed", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void fetchModerators() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<AppUser> moderators = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String role = document.getString("role");

                            if (role != null && (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("moderator"))) {

                                String name = document.getString("name");
                                String studentId = document.getString("id");

                                String displayRole = role.substring(0, 1).toUpperCase() + role.substring(1).toLowerCase();

                                if (name != null) {
                                    moderators.add(new AppUser(name, studentId, displayRole));
                                }
                            }
                        }

                        displayModerators(moderators);
                    } else {
                        Toast.makeText(getContext(), "Failed to load moderator list.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayModerators(List<AppUser> users) {
        if (users.isEmpty()) {
            return;
        }

        Collections.sort(users, (u1, u2) -> {
            if (u1.role.equalsIgnoreCase("Admin") && !u2.role.equalsIgnoreCase("Admin")) return -1;
            if (!u1.role.equalsIgnoreCase("Admin") && u2.role.equalsIgnoreCase("Admin")) return 1;
            return 0;
        });

        for (AppUser user : users) {
            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setPadding(0, 8, 0, 8);

            TextView nameRoleText = new TextView(getContext());
            nameRoleText.setText(user.name + " (" + user.role + ")");
            nameRoleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            nameRoleText.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView studentIdText = new TextView(getContext());
            studentIdText.setText(user.studentId);
            studentIdText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            studentIdText.setTextColor(getResources().getColor(android.R.color.darker_gray, null));

            itemLayout.addView(nameRoleText);
            itemLayout.addView(studentIdText);

            moderatorListContainer.addView(itemLayout);
        }
    }
}