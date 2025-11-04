package com.tanim.ccepedia;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class BusScheduleFragment extends Fragment {

    private ImageView scheduleImage;
    private ProgressBar loadingSpinner;
    private LinearLayout contactsLayout;
    private MaterialButton downloadButton;

    private FirebaseFirestore db;
    private String imageUrl;

    private static final int REQUEST_WRITE_STORAGE = 112;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus_schedule, container, false);

        scheduleImage = rootView.findViewById(R.id.scheduleImage);
        contactsLayout = rootView.findViewById(R.id.contactsLayout);
        downloadButton = rootView.findViewById(R.id.downloadButton);
        loadingSpinner = rootView.findViewById(R.id.loadingSpinner);

        db = FirebaseFirestore.getInstance();

        loadingSpinner.setVisibility(View.VISIBLE);
        loadBusScheduleFromFirestore();

        downloadButton.setOnClickListener(v -> {
            String fileUrl = imageUrl;

            if (fileUrl != null && !fileUrl.isEmpty()) {
                checkPermissionAndDownload(fileUrl);
            } else {
                Toast.makeText(requireContext(), "Download link not available", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    private void loadBusScheduleFromFirestore() {
        db.collection("resources").document("bus_schedule")
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        imageUrl = documentSnapshot.getString("url");
                        List<Map<String, Object>> contacts = (List<Map<String, Object>>) documentSnapshot.get("contacts");

                        if (imageUrl != null && !imageUrl.isEmpty()) {

                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .listener(new RequestListener<Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                            loadingSpinner.setVisibility(View.GONE);
                                            Toast.makeText(requireContext(), "Failed to load schedule image", Toast.LENGTH_SHORT).show();
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                            loadingSpinner.setVisibility(View.GONE);
                                            return false;
                                        }
                                    })
                                    .into(scheduleImage);
                        } else {
                            loadingSpinner.setVisibility(View.GONE);
                            scheduleImage.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "No image URL found", Toast.LENGTH_SHORT).show();
                        }

                        if (contacts != null && !contacts.isEmpty()) {
                            addContactsToLayout(contacts);
                        } else {
                            TextView noContacts = new TextView(requireContext());
                            noContacts.setText("No important contacts found");
                            contactsLayout.addView(noContacts);
                        }
                    } else {
                        loadingSpinner.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "No bus schedule found in database", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Failed to load bus schedule", Toast.LENGTH_SHORT).show();
                });
    }

    private void addContactsToLayout(List<Map<String, Object>> contacts) {
        contactsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Map<String, Object> contact : contacts) {
            String name = (String) contact.get("name");
            String phone = (String) contact.get("phone");

            if (name != null && phone != null) {
                View contactView = inflater.inflate(R.layout.item_contact, contactsLayout, false);
                TextView nameView = contactView.findViewById(R.id.contactNameTextView);
                TextView numberView = contactView.findViewById(R.id.contactNumberTextView);

                nameView.setText(name);
                numberView.setText(phone);

                contactView.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phone));
                    Toast.makeText(requireContext(), "Dialing " + name, Toast.LENGTH_SHORT).show();
                    startActivity(callIntent);
                });

                contactsLayout.addView(contactView);
            }
        }
    }


    private void checkPermissionAndDownload(String fileUrl) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            this.imageUrl = fileUrl;
        } else {
            new DownloadImageTask().execute(fileUrl);
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (imageUrl != null) {
                    new DownloadImageTask().execute(imageUrl);
                }
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot download file.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class DownloadImageTask extends AsyncTask<String, Void, Boolean> {

        private String savedFilePath = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(requireContext(), "Starting download...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            String urlToDownload = urls[0];

            try {
                URL url = new URL(urlToDownload);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return false;
                }

                InputStream input = connection.getInputStream();

                String extension = ".jpg";
                if (urlToDownload.contains(".")) {
                    extension = urlToDownload.substring(urlToDownload.lastIndexOf("."));
                }

                String mimeType = "image/" + extension.replace(".", "");
                if (extension.equalsIgnoreCase(".pdf")) {
                    mimeType = "application/pdf";
                }

                String fileName = "bus_schedule_" + System.currentTimeMillis() + extension;

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/IIUC Pedia/Bus Schedule");

                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    if (uri != null) {
                        OutputStream output = requireContext().getContentResolver().openOutputStream(uri);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                        output.close();
                        input.close();
                        savedFilePath = "Downloads/IIUC Pedia/Bus Schedule/" + fileName;
                        return true;
                    } else {
                        return false;
                    }

                } else {
                    File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File iiucPediaFolder = new File(downloadsFolder, "IIUC Pedia/Bus Schedule");
                    if (!iiucPediaFolder.exists()) iiucPediaFolder.mkdirs();

                    File file = new File(iiucPediaFolder, fileName);
                    savedFilePath = file.getAbsolutePath();

                    FileOutputStream output = new FileOutputStream(file);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }

                    output.close();
                    input.close();

                    if (file.exists() && file.length() > 0) {
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(Uri.fromFile(file));
                        requireContext().sendBroadcast(mediaScanIntent);
                    }

                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }


        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(requireContext(), "Downloaded to: " + savedFilePath, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Failed to download bus schedule. Check connection or URL.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}