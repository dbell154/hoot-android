package com.tylerhosting.hoot.hoot;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TileColorActivity extends AppCompatActivity {

    String themeName;
    Button standard;
    Button current;

//    /Pink c43c68
///Green 179a76
///Red f44e3e
///Ivory fdf5ed
//
///Light Green bcde91
///Yellow edea6d
///Purple 69386c
//    Redder f52f25
//
//    Ivory ffde9d
///Pale Blue 6d8ca8
////Teal 41a28f
////Navy 0d2770
////Green 2b8f44



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeName = Utils.setStartTheme(this);

        setContentView(R.layout.activity_tile_color);

//        for all buttons
        ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.tile_colors);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof Button) {
                v.setOnClickListener(getButtonColor);
            } //etc. If it fails anywhere, just return false.
        }
        standard = findViewById(R.id.color);
        if (themeName.equals("Dark Theme"))
            standard.setTextColor(Color.WHITE);
        current = findViewById(R.id.color19);
        current.setTextColor(LexData.getTileColor(this));

    }

    private View.OnClickListener getButtonColor = new View.OnClickListener() {
        public void onClick(View v) {
            Button btn = (Button) v;
            setButtonColor(btn);
            btn.setText(R.string.h);
        }
    };

    private void setButtonColor(Button button) {
        int color = button.getCurrentTextColor();

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefs = shared.edit();

        prefs.putInt("tilecolor", color);
        prefs.apply();
        LexData.setTileColor(color);
        finish();

    }
}
