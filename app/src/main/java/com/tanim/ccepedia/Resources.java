package com.tanim.ccepedia;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.card.MaterialCardView;

public class Resources extends Fragment {

    private MaterialCardView cardFacebookPage;
    private MaterialCardView cardFacebookFemPage;
    private MaterialCardView cardSemesterResourcesPage;
    private MaterialCardView cardBatchWise;
    private MaterialCardView cardBusPage;
    private MaterialCardView cardDriveLinks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_resources, container, false);

        cardFacebookPage = rootView.findViewById(R.id.cardFacebookPage);
        cardFacebookFemPage = rootView.findViewById(R.id.cardFacebookFemPage);
        cardSemesterResourcesPage = rootView.findViewById(R.id.cardSemesterResourcesPage);
        cardBatchWise = rootView.findViewById(R.id.cardBatchWise);
        cardBusPage = rootView.findViewById(R.id.cardBusPage);
        cardDriveLinks = rootView.findViewById(R.id.cardDriveLinks);

        cardFacebookPage.setOnClickListener(v -> openWebPage("https://www.facebook.com/profile.php?id=100090282199663"));

        cardFacebookFemPage.setOnClickListener(v -> openWebPage("https://www.facebook.com/profile.php?id=100091710725410"));

        cardBatchWise.setOnClickListener(v -> showGenderDialog());

        cardBusPage.setOnClickListener(v -> openFragment(new BusScheduleFragment()));

        cardSemesterResourcesPage.setOnClickListener(v -> openFragment(new SemesterResources()));

        cardDriveLinks.setOnClickListener(v -> openFragment(new DriveLinksFragment()));

        return rootView;
    }

    private void openFragment(Fragment fragment) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    private void showGenderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Section")
                .setItems(new String[]{"Male", "Female"}, (dialog, which) -> {
                    String gender = (which == 0) ? "male" : "female";
                    openBatchWiseFragment(gender);
                });
        builder.show();
    }

    private void openBatchWiseFragment(String gender) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.Midcontainer, BatchWiseFragment.newInstance(gender));
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}