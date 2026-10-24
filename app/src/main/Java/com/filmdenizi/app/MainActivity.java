package com.filmdenizi.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    static final String DEF = "http://localhost:8080";
    WebView web;
    FrameLayout root;
    View custom;
    WebChromeClient.CustomViewCallback customCb;
    boolean dialogOpen = false;

    SharedPreferences prefs() { return getSharedPreferences("fd", MODE_PRIVATE); }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(0xFF120D24);
        getWindow().setNavigationBarColor(0xFF120D24);
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF120D24);
        web = new WebView(this);
        web.setBackgroundColor(0xFF120D24);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) askUrl("Sunucuya bağlanılamadı.\nTermux'ta server.py açık mı?");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View v, CustomViewCallback cb) {
                custom = v; customCb = cb;
                root.addView(v, new FrameLayout.LayoutParams(-1, -1));
                web.setVisibility(View.GONE);
                immersive(true);
            }
            @Override
            public void onHideCustomView() {
                if (custom == null) return;
                root.removeView(custom);
                custom = null;
                customCb.onCustomViewHidden();
                web.setVisibility(View.VISIBLE);
                immersive(false);
            }
        });
        web.loadUrl(prefs().getString("url", DEF));
    }

    @SuppressWarnings("deprecation")
    void immersive(boolean on) {
        getWindow().getDecorView().setSystemUiVisibility(on
            ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            : 0);
    }

    void askUrl(String msg) {
        if (dialogOpen) return;
        dialogOpen = true;
        final EditText et = new EditText(this);
        et.setText(prefs().getString("url", DEF));
        new AlertDialog.Builder(this)
            .setTitle("Sunucu adresi")
            .setMessage(msg)
            .setView(et)
            .setCancelable(false)
            .setPositiveButton("Bağlan", (d, w) -> {
                dialogOpen = false;
                String u = et.getText().toString().trim();
                if (!u.startsWith("http")) u = "http://" + u;
                prefs().edit().putString("url", u).apply();
                web.loadUrl(u);
            })
            .setNegativeButton("Kapat", (d, w) -> finish())
            .show();
    }

    @Override
    public void onBackPressed() {
        if (custom != null) {
            // tam ekran videodan çık
            web.evaluateJavascript("document.exitFullscreen&&document.exitFullscreen()", null);
            return;
        }
        web.evaluateJavascript("window.__back?window.__back():false", v -> {
            if (!"true".equals(v)) finish();
        });
    }
}
