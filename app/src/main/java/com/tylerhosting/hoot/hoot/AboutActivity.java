package com.tylerhosting.hoot.hoot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutActivity extends AppCompatActivity {

    Button policy;
    Button feedback;
    Button facebook;
    Button rate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setStartTheme(this);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        setContentView(R.layout.activity_about);
        BufferedReader reader = null;
        StringBuilder html = new StringBuilder();
        StringBuilder html2 = new StringBuilder();

        TextView versionInfo = findViewById(R.id.versions);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            versionInfo.setText(version + " (Code Version " + verCode + ") ");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("html/" + "about.html")));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                html.append(mLine);
                html.append('\n');
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

            TextView page = (TextView) findViewById(R.id.about);
            Spanned result;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
                // we are using this flag to give a consistent behaviour
                result = Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(html.toString());
            }

//            Spanned result = Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_LEGACY);
            page.setText(result);

            page.setMovementMethod(new ScrollingMovementMethod());
            page.setMovementMethod(LinkMovementMethod.getInstance());
        }

        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("html/" + "credits.html")));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                html2.append(mLine);
                html2.append('\n');
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

            TextView page = (TextView)findViewById(R.id.lexicons);
            Spanned result;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // FROM_HTML_MODE_LEGACY is the behaviour that was used for versions below android N
                // we are using this flag to give a consistent behaviour
                result = Html.fromHtml(html2.toString(), Html.FROM_HTML_MODE_LEGACY);
            } else {
                result =  Html.fromHtml(html2.toString());
            }

            page.setText(result);
            page.setMovementMethod(new ScrollingMovementMethod());
            page.setMovementMethod(LinkMovementMethod.getInstance());
        }

        policy = findViewById(R.id.policy );
        policy.setOnClickListener(Policy);

        facebook = findViewById(R.id.facebook);
        facebook.setOnClickListener(Facebook);

        feedback = findViewById(R.id.feedback);
        feedback.setOnClickListener(feedBack);

        rate = findViewById(R.id.rate);
        rate.setOnClickListener(Rate);
    }

    private View.OnClickListener Policy = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.tylerhosting.com/hoot/policy.html"));
            startActivity(intent);
        }
    };

    private View.OnClickListener Facebook = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/groups/898118867063880"));
            startActivity(intent);
        }
    };

    private View.OnClickListener feedBack = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("mailto:hoot@tylerhosting.com"));
            startActivity(intent);
        }
    };

    private View.OnClickListener Rate = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.google_play_link)));
            startActivity(intent);
        }
    };

}
