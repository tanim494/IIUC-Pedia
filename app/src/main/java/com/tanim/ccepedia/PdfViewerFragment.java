package com.tanim.ccepedia;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PdfViewerFragment extends Fragment {
    private static final String ARG_URL = "fileUrl";
    private static final String ARG_FILENAME = "fileName";
    private static final String ARG_UPLOADER_STUDENT_ID = "uploaderStudentId";
    private static final int REQUEST_WRITE_PERMISSION = 1001;

    private String fileUrl, fileName, uploaderStudentId;
    private PDFView pdfView;
    private ProgressBar loadingSpinner;

    private Uri localPdfUri = null;

    public static PdfViewerFragment newInstance(String url, String fileName, String uploaderStudentId) {
        PdfViewerFragment fragment = new PdfViewerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_FILENAME, fileName);
        args.putString(ARG_UPLOADER_STUDENT_ID, uploaderStudentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pdf_viewer, container, false);

        pdfView = view.findViewById(R.id.pdfView);
        MaterialButton downloadButton = view.findViewById(R.id.downloadButton);
        MaterialButton shareButton = view.findViewById(R.id.shareButton);
        loadingSpinner = view.findViewById(R.id.loadingSpinner);

        if (getArguments() != null) {
            fileUrl = getArguments().getString(ARG_URL);
            fileName = getArguments().getString(ARG_FILENAME);
            uploaderStudentId = getArguments().getString(ARG_UPLOADER_STUDENT_ID);
            downloadAndDisplayPdf(fileUrl);
        }

        downloadButton.setOnClickListener(v -> {
            if (fileUrl != null && !fileUrl.isEmpty()) {
                checkPermissionAndDownload(fileUrl);
            } else {
                Toast.makeText(requireContext(), "No PDF URL found", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> {
            if (localPdfUri != null) {
                sharePdfFile();
            } else {
                Toast.makeText(requireContext(), "PDF not loaded yet. Please wait.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void downloadAndDisplayPdf(String urlString) {
        loadingSpinner.setVisibility(View.VISIBLE);

        new Thread(() -> {
            File file = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream input = connection.getInputStream();

                String cleanFileName = this.fileName;
                String prefixedFileName;

                if (cleanFileName == null || cleanFileName.trim().isEmpty()) {
                    cleanFileName = "temp_pdf_" + System.currentTimeMillis();
                }

                if (cleanFileName.toLowerCase().endsWith(".pdf")) {
                    cleanFileName = cleanFileName.substring(0, cleanFileName.length() - 4);
                }

                prefixedFileName = "CCE_Pedia_" + cleanFileName;

                prefixedFileName = prefixedFileName.replace(" ", "_").replace("/", "_") + ".pdf";

                file = new File(requireContext().getCacheDir(), prefixedFileName);

                FileOutputStream output = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = input.read(buffer)) != -1) {
                    output.write(buffer, 0, byteCount);
                }

                output.close();
                input.close();

                File finalFile = file;
                localPdfUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        finalFile
                );

                requireActivity().runOnUiThread(() -> pdfView.fromFile(finalFile)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .onLoad(nbPages -> loadingSpinner.setVisibility(View.GONE))
                        .onError(t -> {
                            loadingSpinner.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Failed to render PDF: " + t.getMessage(), Toast.LENGTH_LONG).show();
                        })
                        .load());

            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    loadingSpinner.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void sharePdfFile() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, localPdfUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Document: " + fileName);

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share PDF file using..."));
    }

    private void checkPermissionAndDownload(String url) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_PERMISSION);
        } else {
            downloadFileToDevice(url);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    downloadFileToDevice(fileUrl);
                }
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot download PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void downloadFileToDevice(String urlToDownload) {
        Toast.makeText(requireContext(), "Starting download...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            String savedFilePath = null;
            boolean success = false;
            try {
                URL url = new URL(urlToDownload);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server returned code " + connection.getResponseCode());
                }

                InputStream input = connection.getInputStream();

                String extension = ".pdf";
                String originalFileName = PdfViewerFragment.this.fileName;
                String fileNameToSave;

                if (originalFileName == null || originalFileName.trim().isEmpty()) {
                    fileNameToSave = "CCE_Pedia_" + System.currentTimeMillis() + extension;
                } else if (!originalFileName.toLowerCase().endsWith(".pdf")) {
                    fileNameToSave = originalFileName.replace(" ", "_") + extension;
                } else {
                    fileNameToSave = originalFileName.replace(" ", "_");
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileNameToSave);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/CCE Pedia/Resources");

                    Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    if (uri != null) {
                        try (OutputStream output = requireContext().getContentResolver().openOutputStream(uri)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = input.read(buffer)) != -1) {
                                output.write(buffer, 0, bytesRead);
                            }
                        }
                        savedFilePath = "Downloads/CCE Pedia/Resources/" + fileNameToSave;
                        success = true;
                    }

                } else {
                    File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File ccePediaFolder = new File(downloadsFolder, "CCE Pedia/Resources");
                    if (!ccePediaFolder.exists()) ccePediaFolder.mkdirs();

                    File file = new File(ccePediaFolder, fileNameToSave);

                    try (FileOutputStream output = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesRead);
                        }
                    }
                    savedFilePath = file.getAbsolutePath();
                    success = true;
                }

            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            } finally {
                String finalPath = savedFilePath;
                boolean finalSuccess = success;
                requireActivity().runOnUiThread(() -> {
                    if (finalSuccess) {
                        Toast.makeText(requireContext(), "Downloaded to: " + finalPath, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to download PDF", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (localPdfUri != null) {
            try {
                File fileToDelete = null;
                if (localPdfUri.getPathSegments().size() >= 2) {
                    String pathSegment = localPdfUri.getPathSegments().get(1);
                    fileToDelete = new File(requireContext().getCacheDir(), pathSegment);
                }

                if (fileToDelete != null && fileToDelete.exists() && fileToDelete.delete()) {
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            localPdfUri = null;
        }
    }
}