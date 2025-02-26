package com.tylerhosting.hoot.hoot;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

public class ClockHelpActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_clockhelp);

        webpage = (WebView) findViewById(R.id.multihelp);
        webpage.setWebViewClient(new WebViewClient());
        webpage.getSettings().setSupportZoom(true);
        webpage.getSettings().setBuiltInZoomControls(true);
        webpage.getSettings().setDisplayZoomControls(true);
        webpage.getSettings().setJavaScriptEnabled(true);
        webpage.loadUrl("file:///android_asset/html/clockhelp.html");

        webpage.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webpage.setScrollbarFadingEnabled(false);
        webpage.setVerticalScrollBarEnabled(true);

        webpage.setWebViewClient(new WebViewClient(){
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.contains("tyler")) || url.startsWith("http")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse(url));
                    startActivity(intent);

                    return true;
                } else {
                    return false;
                }
            }
        });

        TextView versionInfo = findViewById(R.id.versions);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            versionInfo.setText(version + " (Code Version " + verCode + ") ");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("text/" + "help.txt")));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                text.append(mLine);
                text.append('\n');
            }
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),"Error reading file!",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }

            TextView output= (TextView) findViewById(R.id.help);
            output.setText((CharSequence) text);
            output.setMovementMethod(new ScrollingMovementMethod());
        }
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
