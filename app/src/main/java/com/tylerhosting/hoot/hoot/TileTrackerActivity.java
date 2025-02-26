package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;


public class TileTrackerActivity extends AppCompatActivity {

    CheckBox correction;
    TextView correcting;
    TextView status, used;
    String themeName;
    boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        themeName = Utils.setStartTheme(this);
        setContentView(R.layout.activity_tile_tracker);

//        setRotation();
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean screenOn = shared.getBoolean("screenon", false);
        if (screenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Button letterButton;
        TextView counter;
        int count;
        AssetManager assetManager = getAssets();
        Typeface tile = Typeface.createFromAsset(assetManager, "fonts/nutiles.ttf");

        for (int i = 65; i < 91; i++) {
            counter = findViewById(getResId("count" + Character.toString ((char) i), R.id.class));
            count = LexData.Tiles.tiles.chardist[i-65];
            counter.setText(String.valueOf(count));

            letterButton = findViewById(getResId("btn" + Character.toString ((char) i), R.id.class));
            letterButton.setTypeface(tile);
            letterButton.setTextSize(48);
            letterButton.setBackgroundColor(Color.TRANSPARENT);
            letterButton.setTextColor(LexData.getTileColor(this));
//            letterButton.setTextColor(0xFF007A8A);
            letterButton.setPadding(0, 0, 0, 0);
        }
        Button blankButton;
        TextView blankCounter;
        blankCounter = findViewById(R.id.countBlank);
        count = LexData.Tiles.tiles.chardist[26];
        blankCounter.setText(String.valueOf(count));

        blankButton = findViewById(R.id.btnBlank);
        blankButton.setTextSize(48);
        blankButton.setBackgroundColor(Color.TRANSPARENT);
        blankButton.setPadding(0, 0, 0, 0);
        blankButton.setTextColor(LexData.getTileColor(this));
//        blankButton.setTextColor(0xFF007A8A);
        blankButton.setTypeface(tile);

        correcting = findViewById(R.id.txtCorrecting);
        correction = findViewById(R.id.chkCorrection);
        if (themeName.equals("Dark Theme")) {
            correction.setTextColor(Color.WHITE);

//        int states[][] = {{android.R.attr.state_checked}, {android.R.attr.state}};
//        int colors[] = { Color.YELLOW, Color.WHITE};
//        CompoundButtonCompat.setButtonTintList(correction, new ColorStateList(states, colors));

            CompoundButtonCompat.setButtonTintList(correction, ColorStateList
                    .valueOf(getResources().getColor(R.color.whiteText)));
        }

        status = findViewById(R.id.tileset);
        status.setText("Tile Set: " + LexData.getTilesetName());
        used = findViewById(R.id.Used);
        used.setText("");

        if (savedInstanceState != null){

            String[] values = savedInstanceState.getStringArray("countValues");
            for (int i = 0; i < 26; i++) {
                TextView counted = findViewById(getResId("count" + Character.toString ((char) (i+65)), R.id.class));
                counted.setText(values[i]);

                Button resetbtn = findViewById(getResId("btn" + Character.toString ((char) (i+65)), R.id.class));
                if (counted.getText().toString().equals("0"))
                    resetbtn.setBackgroundResource(R.drawable.greybox);
                else
                    resetbtn.setBackgroundColor(Color.TRANSPARENT);
            }


            String bValue = savedInstanceState.getString("Bvalue");
            blankCounter.setText(bValue);
            //Do whatever you need with the string here, like assign it to variable.
//            Log.d("XXX", savedInstanceState.getString(STRING_CONSTANT));

            if (blankCounter.getText().toString().equals("0")) {
                blankButton.setBackgroundResource(R.drawable.greybox);
                blankButton.setBackgroundColor(Color.GRAY);
            }
            else
                blankButton.setBackgroundColor(Color.TRANSPARENT);

            String usedLetters = savedInstanceState.getString("usedLetters");
            used.setText(usedLetters);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        String[] values = new String[26];
        for (int i = 0; i < 26; i++) {
            TextView counter = findViewById(getResId("count" + Character.toString ((char) (i+65)), R.id.class));
            values[i] = counter.getText().toString();
        }
        outState.putStringArray("countValues", values);

        TextView blankCounter = findViewById(R.id.countBlank);
        outState.putString("Bvalue", blankCounter.getText().toString());

        String usedLetters = used.getText().toString();
        outState.putString("usedLetters", usedLetters);
//        TextView counter = findViewById(getResId("count" + 'B', R.id.class));
//        outState.putString("Bvalue", counter.getText().toString());

        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(outState);
        // Save our own state now
        outState.putAll(outState);
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        if (view.getId() == R.id.chkCorrection) {
            if (checked)
                correcting.setVisibility(View.VISIBLE);
            else
                correcting.setVisibility(View.GONE);
        }
    }
    // need this to avoid all values being reset
    // todo, save/reset values when rotating
    @SuppressLint("SourceLockedOrientationActivity")
    protected void setRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        switch(rotation) {
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case  Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }
    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
    public void useTile(View view ) {
        TextView counter;
        int before;
        String letter = view.getContentDescription().toString();

        if (Character.isLetter(letter.charAt(0))) {
            counter = findViewById(getResId("count" + letter, R.id.class));
            before = Integer.parseInt(counter.getText().toString());
                if (correction.isChecked()) {
                    counter.setText(String.valueOf(before + 1));
                    used.append(letter.toLowerCase());
                }
                else {
                    if (before > 0) {
                        view.setEnabled(false);
                        counter.setText(String.valueOf(before - 1));
                        view.setEnabled(true);
                        used.append(letter);
                    }
                }
                    if (counter.getText().toString().equals("0"))
                        view.setBackgroundResource(R.drawable.greybox);
                    else
                        view.setBackgroundColor(Color.TRANSPARENT);

        }
        else {
            if (letter.equals("?")) {
                counter = findViewById(R.id.countBlank);
                before = Integer.parseInt(counter.getText().toString());
                if (correction.isChecked()) {
                    counter.setText(String.valueOf(before + 1));
                    used.append(letter.toLowerCase());
//                    if (counter.getText().toString().equals("0")) {
//                        view.setBackgroundResource(R.drawable.greybox);
//                        view.setBackgroundColor(Color.GRAY);
//                    }
//                    else
//                        view.setBackgroundColor(Color.TRANSPARENT);
                }
                else {
                    if (before > 0) {
                        view.setEnabled(false);
                        counter.setText(String.valueOf(before - 1));
                        view.setEnabled(true);
                        used.append(letter);
                    }
                    // background is set, but letter is solid and it doesn't show through
//                    if (counter.getText().toString().equals("0")) {
//                        view.setBackgroundResource(R.drawable.greybox);
//                        view.setBackgroundColor(Color.GRAY);
//                    }
//                    else
//                        view.setBackgroundColor(Color.TRANSPARENT);

//                    else
//                        view.setEnabled(false); // or change color
                }
                if (counter.getText().toString().equals("0")) {
                    view.setBackgroundResource(R.drawable.greybox);
                    view.setBackgroundColor(Color.GRAY);
                }
                else
                    view.setBackgroundColor(Color.TRANSPARENT);

            } else {
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

}