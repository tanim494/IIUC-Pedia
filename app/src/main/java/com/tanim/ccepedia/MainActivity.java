package com.tanim.ccepedia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.widget.ImageView;
import android.widget.TextView;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import androidx.activity.OnBackPressedCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigation;

    private TextView userNameTextView;
    private TextView userId;
    private ImageView profileImage;

    String updateLink;
    float databaseVersion;
    float userVersion;
    String userRole;

    FirebaseFirestore firestore;
    DocumentReference configDocRef;
    ListenerRegistration configListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        checkForUpdate();
        checkForNotificationPermission();
        FirebaseMessaging.getInstance().subscribeToTopic("notification");

        initializeViews();
        setUserData();
        setupDatabaseListeners();
        setupClickListeners();
        loadFragment();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.Midcontainer);

                if (currentFragment instanceof WebFragment) {
                    WebFragment webFragment = (WebFragment) currentFragment;
                    if (webFragment.canGoBack()) {
                        webFragment.goBack();
                        return;
                    }
                }

                if (currentFragment instanceof Home || (getSupportFragmentManager().getBackStackEntryCount() == 0 && currentFragment == null)) {

                    new MaterialAlertDialogBuilder(MainActivity.this)
                            .setTitle("Exit CCEPedia?")
                            .setMessage("Are you sure you want to close the application?")
                            .setPositiveButton("Yes", (dialog, which) -> finish())
                            .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                            .show();

                }
                else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                }
                else {
                    bottomNavigation.setSelectedItemId(R.id.nv_home);

                    FragmentTransaction tran = getSupportFragmentManager().beginTransaction();
                    tran.setCustomAnimations(android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out);
                    tran.replace(R.id.Midcontainer, new Home());
                    tran.commit();
                }
            }
        });
    }

    private void checkForUpdate() {
        new Handler().postDelayed(() -> {
            if (databaseVersion > userVersion) {

                SpannableString title = new SpannableString("Update Available");
                title.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage("A new version of CCEPedia is ready to download. Update now to get the latest features and improvements.")
                        .setCancelable(false)
                        .setPositiveButton("Update", (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateLink));
                            startActivity(intent);
                        })
                        .setNegativeButton("Later", (dialog, which) -> dialog.dismiss())
                        .show();
            }
        }, 2000);
    }

    private void checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }
    }


    private void initializeViews() {
        userNameTextView = findViewById(R.id.userNameTextView);
        userId = findViewById(R.id.userId);
        profileImage = findViewById(R.id.profileImage);

        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    @SuppressLint("SetTextI18n")
    private void setUserData() {
        UserData user = UserData.getInstance();

        userNameTextView.setText(user.getName());
        userId.setText(user.getStudentId() + ", " + user.getSemester() + " Semester");
        userRole = user.getRole();
    }

    private void setupDatabaseListeners() {
        firestore = FirebaseFirestore.getInstance();
        configDocRef = firestore.collection("appConfig").document("main");

        configListener = configDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Double version = snapshot.getDouble("version");
                updateLink = snapshot.getString("updateLink");

                if (version != null) {
                    databaseVersion = version.floatValue();
                }

                userVersion = Math.round(Float.parseFloat(BuildConfig.VERSION_NAME) * 100);
            }
        });
    }

    private void setupClickListeners() {

        profileImage.setOnClickListener(view -> openProfileFragment());
    }

    @SuppressLint("SetTextI18n")
    private void openProfileFragment() {

        ProfileFragment profileFragment = new ProfileFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out)
                .replace(R.id.Midcontainer, profileFragment)
                .addToBackStack(null)
                .commit();
    }

    @SuppressLint({"SetTextI18n", "NonConstantResourceId"})
    private void loadFragment() {
        FragmentManager fgMan = getSupportFragmentManager();
        FragmentTransaction tran = fgMan.beginTransaction();
        tran.setCustomAnimations(android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        tran.replace(R.id.Midcontainer, new Home());
        tran.commit();


        bottomNavigation.setOnItemSelectedListener(item -> {

            FragmentTransaction tran1 = getSupportFragmentManager().beginTransaction();
            tran1.setCustomAnimations(android.R.anim.fade_in,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);

            switch (item.getItemId()) {
                case R.id.nv_home:
                    tran1.replace(R.id.Midcontainer, new Home());
                    break;
                case R.id.nv_faculty:
                    tran1.replace(R.id.Midcontainer, new Faculty());
                    break;
                case R.id.nv_resource:
                    tran1.replace(R.id.Midcontainer, new Resources());
                    break;
                case R.id.nv_author:
                    tran1.replace(R.id.Midcontainer, new DeveloperFragment());
                    break;
            }

            tran1.commit();
            return true;
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (configListener != null) {
            configListener.remove();
        }
    }
}