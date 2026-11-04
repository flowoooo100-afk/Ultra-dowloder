package com.ultradownloader.app;

import android.app.Dialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
@CapacitorPlugin(name = "NativeBrowser")
public class BrowserPlugin extends Plugin {

    private WebView webView;
    private EditText addressBar;

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

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    addressBar.setText(url);
                }
            });

            webView.setWebChromeClient(new WebChromeClient());

            go.setOnClickListener(v -> loadAddress());

            addressBar.setOnEditorActionListener((v, actionId, event) -> {
                loadAddress();
                return true;
            });

            layout.addView(toolbar);

            layout.addView(webView,
                new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

            dialog.setContentView(layout);
            dialog.show();

            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

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
}
