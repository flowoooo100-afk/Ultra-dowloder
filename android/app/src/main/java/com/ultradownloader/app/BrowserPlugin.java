package com.ultradownloader.app;

import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.JavascriptInterface;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.widget.FrameLayout;

@CapacitorPlugin(name = "NativeBrowser")
public class BrowserPlugin extends Plugin {

    private WebView webView;
    private EditText addressBar;
    private Button downloadButton;
    private String detectedVideoUrl;

    private class VideoBridge {

        @JavascriptInterface
        public void onVideoDetected(String status, String url) {

            getActivity().runOnUiThread(() -> {

                if (url == null ||
                    !(url.startsWith("https://") ||
                      url.startsWith("http://"))) return;

                detectedVideoUrl = url;

                downloadButton.setVisibility(View.VISIBLE);
                downloadButton.setEnabled(true);
                downloadButton.setText("↓");

                downloadButton.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        status.equals("playing")
                            ? Color.rgb(20, 190, 105)
                            : Color.rgb(35, 110, 220)
                    )
                );
            });
        }
    }

    @PluginMethod
    public void openBrowser(PluginCall call) {

        String initialUrl = call.getString(
            "url", "https://www.google.com"
        );

        getActivity().runOnUiThread(() -> {

            Dialog dialog = new Dialog(getActivity());

            LinearLayout layout = new LinearLayout(getActivity());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setBackgroundColor(Color.rgb(12, 17, 30));

            LinearLayout toolbar = new LinearLayout(getActivity());
            toolbar.setPadding(8, 8, 8, 8);
            toolbar.setGravity(Gravity.CENTER_VERTICAL);

            addressBar = new EditText(getActivity());
            addressBar.setSingleLine(true);
            addressBar.setTextColor(Color.WHITE);
            addressBar.setHintTextColor(Color.GRAY);
            addressBar.setHint("Enter website or search");
            addressBar.setText(initialUrl);

            Button go = new Button(getActivity());
            go.setText("GO");

            toolbar.addView(addressBar,
                new LinearLayout.LayoutParams(0, 55, 1));

            toolbar.addView(go);

            webView = new WebView(getActivity());

            webView.addJavascriptInterface(
                new VideoBridge(), "UltraVideoBridge"
            );

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            webView.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageStarted(
                    WebView view, String url,
                    android.graphics.Bitmap favicon
                ) {
                    detectedVideoUrl = null;
                    downloadButton.setVisibility(View.GONE);
                }

                @Override
                public void onPageFinished(WebView view, String url) {

                    addressBar.setText(url);

                    String script =
    "(function() {" +
    "function report(u,status) {" +
    "if(!u)return;" +
    "try {" +
    "u=new URL(u,location.href).href;" +
    "if(!/^https?:/.test(u))return;" +
    "if(!/\\.(mp4|webm|m4v|mov)(?:$|[?#])/i.test(u))return;" +
    "UltraVideoBridge.onVideoDetected(status,u);" +
    "}catch(e){}" +
    "}" +
    "function scan() {" +
    "document.querySelectorAll('video').forEach(function(v) {" +
    "report(v.currentSrc||v.src,'found');" +
    "v.querySelectorAll('source').forEach(function(s) {" +
    "report(s.src,'found');" +
    "});" +
    "if(!v.dataset.ultraTracked) {" +
    "v.dataset.ultraTracked='1';" +
    "v.addEventListener('playing',function() {" +
    "report(v.currentSrc||v.src,'playing');" +
    "});" +
    "}" +
    "});" +
    "}" +
    "scan();" +
    "setInterval(scan,1500);" +
    "})();";

view.evaluateJavascript(script, null);
                }
            });

            webView.setWebChromeClient(new WebChromeClient());

            go.setOnClickListener(v -> loadAddress());

            addressBar.setOnEditorActionListener((v, actionId, event) -> {
                loadAddress();
                return true;
            });

            // Browser content and floating button
            FrameLayout browserFrame = new FrameLayout(getActivity());

            browserFrame.addView(webView,
                new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ));

            downloadButton = new Button(getActivity());
            downloadButton.setText("↓");
            downloadButton.setTextSize(28);
            downloadButton.setTextColor(Color.WHITE);
            downloadButton.setAllCaps(false);
            downloadButton.setVisibility(View.GONE);
            downloadButton.setEnabled(false);
            downloadButton.setElevation(12);

            FrameLayout.LayoutParams buttonParams =
                new FrameLayout.LayoutParams(
                    58, 58, Gravity.BOTTOM | Gravity.END
                );

            buttonParams.setMargins(0, 0, 20, 28);

            browserFrame.addView(downloadButton, buttonParams);

            downloadButton.setOnClickListener(v ->
                downloadDetectedVideo()
            );

            layout.addView(toolbar);

            layout.addView(browserFrame,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
                ));

            dialog.setContentView(layout);
            dialog.show();

            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );

            loadAddress();
            call.resolve();
        });
    }

    private void loadAddress() {

        String text = addressBar.getText().toString().trim();
        if (text.isEmpty()) return;

        String url;

        if (text.contains(" ") || !text.contains(".")) {
            url = "https://www.google.com/search?q=" +
                android.net.Uri.encode(text);
        } else if (!text.startsWith("http://") &&
                   !text.startsWith("https://")) {
            url = "https://" + text;
        } else {
            url = text;
        }

        webView.loadUrl(url);
    }

    private void downloadDetectedVideo() {

        if (detectedVideoUrl == null) {
            Toast.makeText(getActivity(),
                "No supported video detected",
                Toast.LENGTH_SHORT).show();
            return;
        }

        try {

            DownloadManager manager =
                (DownloadManager) getActivity().getSystemService(
                    Context.DOWNLOAD_SERVICE
                );

            String filename = URLUtil.guessFileName(
                detectedVideoUrl, null, "video/mp4"
            );

            DownloadManager.Request request =
                new DownloadManager.Request(
                    android.net.Uri.parse(detectedVideoUrl)
                );

            request.setTitle(filename);
            request.setDescription("Ultra Downloader");
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            String cookies = CookieManager.getInstance()
                .getCookie(detectedVideoUrl);

            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
            }

            request.addRequestHeader("Referer", webView.getUrl());
            request.addRequestHeader(
                "User-Agent",
                webView.getSettings().getUserAgentString()
            );

            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, filename
            );

            manager.enqueue(request);

            Toast.makeText(getActivity(),
                "Download started!",
                Toast.LENGTH_SHORT).show();

        } catch (Exception e) {

            Toast.makeText(getActivity(),
                "Download failed: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
}
