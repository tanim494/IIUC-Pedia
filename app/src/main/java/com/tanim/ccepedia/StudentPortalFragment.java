package com.tanim.ccepedia;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class StudentPortalFragment extends Fragment {

    private WebView webView;
    private ProgressBar progressBar;
    private final String PORTAL_BASE_URL = "https://portal.iiuc.ac.bd/";
    private final String PORTAL_DASHBOARD_PATH = "index.php/dashboard/";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_portal, container, false);


        webView = view.findViewById(R.id.portal_webview);
        progressBar = view.findViewById(R.id.portal_progress_bar);

        setupWebView();
        webView.loadUrl(PORTAL_BASE_URL + PORTAL_DASHBOARD_PATH);

        setupBackNavigation(view);

        return view;
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile; rv:100.0) Gecko/100.0 Firefox/100.0");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

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

                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, suggestedFilename);

                    dm.enqueue(request);
                    Toast.makeText(requireContext(), "Starting download: " + suggestedFilename, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "Download manager not available.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                CookieManager.getInstance().flush();

                String newBackgroundColor = "#FFFFFF";
                String js = "javascript:" +
                        "document.body.style.backgroundColor = '" + newBackgroundColor + "';" +
                        "document.documentElement.style.backgroundColor = '" + newBackgroundColor + "';";
                view.evaluateJavascript(js, null);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
    }

    private void setupBackNavigation(View view) {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                String currentUrl = webView.getUrl();

                if (webView.canGoBack()) {
                    webView.goBack();
                }
                else if (currentUrl != null && currentUrl.contains(PORTAL_DASHBOARD_PATH)) {
                    getParentFragmentManager().popBackStack();
                }
                else {
                    getParentFragmentManager().popBackStack();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush();
        }
    }
}