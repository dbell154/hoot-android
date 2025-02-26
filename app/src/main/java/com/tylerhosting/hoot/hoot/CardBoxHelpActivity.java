package com.tylerhosting.hoot.hoot;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CardBoxHelpActivity extends AppCompatActivity {

    private TextView textView;
    private StringBuilder text = new StringBuilder();
    private WebView webpage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setStartTheme(this);


        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        setContentView(R.layout.activity_card_box_help);

        webpage = (WebView) findViewById(R.id.cbhelp);
        webpage.setWebViewClient(new WebViewClient());
        webpage.getSettings().setSupportZoom(true);
        webpage.getSettings().setBuiltInZoomControls(true);
        webpage.getSettings().setDisplayZoomControls(true);
        webpage.getSettings().setJavaScriptEnabled(true);
        webpage.loadUrl("file:///android_asset/html/cardboxhelp.html");

        webpage.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webpage.setScrollbarFadingEnabled(false);
        webpage.setVerticalScrollBarEnabled(true);

        webpage.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.contains("tyler")) || url.startsWith("http")) {


                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(url));
                    startActivity(intent);

//                    view.getContext().startActivity(
                    //                           new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                } else {
                    return false;
                }
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webpage.canGoBack()) {
                        webpage.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }
}