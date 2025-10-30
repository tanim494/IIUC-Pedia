package com.tanim.ccepedia;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebFragment extends Fragment {

    private static final String ARG_URL = "url";
    private String url;
    private WebView webView;
    private ProgressBar progressBar;

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);

        webView = view.findViewById(R.id.webViewContainer);
        progressBar = view.findViewById(R.id.progressBar);

        setupWebView();

        if (url != null && !url.isEmpty()) {
            String finalUrl = url;

            if (!finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                finalUrl = "https://" + finalUrl;
            }

            if (finalUrl.contains("drive.google.com")) {
                if (!finalUrl.contains("?")) {
                    finalUrl = finalUrl + "?usp=sharing";
                } else if (!finalUrl.contains("usp=")) {
                    finalUrl = finalUrl + "&usp=sharing";
                }
            }

            webView.loadUrl(finalUrl);
        }

        return view;
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        webSettings.setSaveFormData(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                webView.setVisibility(View.VISIBLE);
                super.onPageFinished(view, url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

                DownloadManager dm = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);

                if (dm != null) {
                    Uri uri = Uri.parse(url);
                    DownloadManager.Request request = new DownloadManager.Request(uri);

                    request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url));
                    request.addRequestHeader("User-Agent", userAgent);

                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    String suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimetype);

                    String lowerFilename = suggestedFilename.toLowerCase();

                    boolean isImage = lowerFilename.endsWith(".jpg") ||
                            lowerFilename.endsWith(".jpeg") ||
                            lowerFilename.endsWith(".png");

                    if (!isImage) {

                        String filenameFromHeader = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        if (filenameFromHeader != null && !filenameFromHeader.isEmpty()) {
                            suggestedFilename = filenameFromHeader;
                        }

                        if (!suggestedFilename.toLowerCase().endsWith(".pdf")) {

                            int lastDotIndex = suggestedFilename.lastIndexOf('.');
                            if (lastDotIndex != -1) {
                                suggestedFilename = suggestedFilename.substring(0, lastDotIndex) + ".pdf";
                            } else {
                                suggestedFilename = suggestedFilename + ".pdf";
                            }
                        }
                    }

                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, suggestedFilename);

                    dm.enqueue(request);
                    Toast.makeText(requireContext(), "Starting download: " + suggestedFilename, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Download manager not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }
}