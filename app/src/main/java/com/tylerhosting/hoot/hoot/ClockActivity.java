package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.Display;
import android.view.SoundEffectConstants;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class ClockActivity extends AppCompatActivity {

    // blacktime is used for display
    // totalBlack is used for copntdown (adds 1 hour addedtime)
    // blackover is used for display of overtime
    long initialTime = 25*60*1000;

    long blacktime = initialTime; // 25 minutes in milliseconds
    long whitetime = initialTime; // 25 minutes

    long totalWhite;
    long totalBlack;
    long addedtime =  59*60*1000;

    long blackover = 0;
    long whiteover = 0;

    TextView black;
    TextView white;

    TextView whitemove, blackmove;
    int whitemovetime, blackmovetime = 0;

    TextView playerA, playerB;
    ImageView pause, judge;
    CountDownTimer blackTimer;
    CountDownTimer whiteTimer;

    public boolean blackRunning = false;
    public boolean whiteRunning = false;

    AudioManager audioManager;
    MediaPlayer mediaPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);
        setRotation();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int height = size.y;


        // black button starts white
        String blackDisplay = String.format(Locale.US, "%02d:%02d",
                (blacktime / 60000), (blacktime % 60000));
        black = findViewById(R.id.Black);
        black.setText(blackDisplay);
        black.setOnClickListener(altStartWhite);

        blackmove = findViewById(R.id.BlackMove);
        blackmove.setOnClickListener(altStartWhite);

        // white button starts black
        String whiteDisplay = String.format(Locale.US, "%02d:%02d",
                (whitetime / 60000), (whitetime % 60000));
        white = findViewById(R.id.White);
        white.setText(whiteDisplay);
        white.setOnClickListener(altStartBlack);

        whitemove = findViewById(R.id.WhiteMove);
        whitemove.setOnClickListener(altStartBlack);

        totalBlack = blacktime + addedtime;
        totalWhite = whitetime + addedtime;

        pause = findViewById(R.id.btnPause);
        pause.setOnClickListener(pausePlay);
        pause.setLongClickable(true);
        pause.setOnLongClickListener(resetClock);

        playerA = findViewById(R.id.tvPlayerA);
        playerB = findViewById(R.id.tvPlayerB);
        if (height <= 480) {
            black.setTextSize(height / 8);
            blackmove.setTextSize(height / 30);
            blackmove.setHeight(height / 90);
            white.setTextSize(height / 8);
            whitemove.setTextSize(height / 30);
            whitemove.setHeight(height / 90);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
//        Boolean screenOn = shared.getBoolean("screenon", false);
//        if (screenOn)
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        else
//            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setFullScreen();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = MediaPlayer.create(this, R.raw.sample);


    }
    public void setFullScreen() {

        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
// Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
// Remember that you should never show the action bar if the
// status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if (actionBar != null)
                actionBar.hide();
        }
    }

    public void blackTimerStart(long timeLengthMilli) {
        setFullScreen();
        blackmovetime = 0;

        blackTimer = new CountDownTimer(timeLengthMilli, 1000) {

            @Override
            public void onTick(long leftTimeInMilliseconds) {
                blacktime = (leftTimeInMilliseconds - addedtime);

                if (leftTimeInMilliseconds < addedtime) {
                    blackover += 1;
                    black.setTextColor(Color.RED);
                    String formattedBlackTime = String.format(Locale.US, "-%02d:%02d",
                            (blackover % 3600) / 60, (blackover % 60));

                    black.setText(formattedBlackTime);
                }
                else {
                    long durationSeconds = blacktime / 1000;
                    String formattedBlackTime = String.format(Locale.US, "%02d:%02d",
                            (durationSeconds % 3600) / 60, (durationSeconds % 60));

                    black.setText(formattedBlackTime);
                }

                blackmovetime++;
                String formattedBlackMove;
                if (blackmovetime < 60) {
                    formattedBlackMove = String.format(Locale.US, "0:%02d",
                            (blackmovetime));
                }
                else {
                    formattedBlackMove = String.format(Locale.US, "%d:%02d",
                            (blackmovetime % 3600) / 60, (blackmovetime % 60));
                }
                blackmove.setText(formattedBlackMove);
            }

            @Override
            public void onFinish() {

            }
        };
        blackTimer.start();
    }

    public void whiteTimerStart(long timeLengthMilli) {
        setFullScreen();
        whitemovetime = 0;
        whiteTimer = new CountDownTimer(timeLengthMilli, 1000) {

            @Override
            public void onTick(long leftTimeInMilliseconds) {
                whitetime = (leftTimeInMilliseconds - addedtime);


                if (leftTimeInMilliseconds < addedtime) {
                    whiteover += 1;
                    // negative display; count up
                    white.setTextColor(Color.RED);
                    String formattedWhiteTime = String.format(Locale.US, "-%02d:%02d",
                            (whiteover % 3600) / 60, (whiteover % 60));

                    white.setText(formattedWhiteTime);
                }
                else {
                    long durationSeconds = whitetime / 1000;
                    String formattedWhiteTime =  String.format(Locale.US, "%02d:%02d",
                            (durationSeconds % 3600) / 60, (durationSeconds % 60));

                    white.setText(formattedWhiteTime);
                }
                whitemovetime++;
                String formattedWhiteMove;
                if (whitemovetime < 60) {
                    formattedWhiteMove = String.format(Locale.US, "0:%02d",
                            (whitemovetime));
                }
                else {
                    formattedWhiteMove = String.format(Locale.US, "%d:%02d",
                            (whitemovetime % 3600) / 60, (whitemovetime % 60));
                }
                whitemove.setText(formattedWhiteMove);
            }

            @Override
            public void onFinish() {

            }
        };
        whiteTimer.start();

    }

    private OnClickListener altStartBlack = new OnClickListener() { // startPause button
        @Override
        public void onClick(View v) {
//            v.playSoundEffect(SoundEffectConstants.CLICK);
//            audioManager.playSoundEffect(SoundEffectConstants.CLICK);
            if (!blackRunning) {
                mediaPlayer.start();
                if (whiteTimer != null)
                    whiteTimer.cancel();
                black.setTextColor(0xff009900);
                blackmove.setTextColor(0xff009900);
                white.setTextColor(Color.GRAY);
                whitemove.setTextColor(Color.GRAY);
                blackTimerStart(blacktime + addedtime);
                //blackTimer.start();
                blackRunning = true;
                whiteRunning = false;
            }
        }
    };

    private OnClickListener altStartWhite = new OnClickListener() {
        @Override
        public void onClick(View v) {
//            v.playSoundEffect(SoundEffectConstants.CLICK);
//            audioManager.playSoundEffect(SoundEffectConstants.CLICK);
            if (!whiteRunning) {
                mediaPlayer.start();
                if (blackTimer != null)
                    blackTimer.cancel();
                black.setTextColor(Color.GRAY);
                blackmove.setTextColor(Color.GRAY);
                white.setTextColor(0xff009900);
                whitemove.setTextColor(0xff009900);
                whiteTimerStart(whitetime + addedtime);
                blackRunning = false;
                whiteRunning = true;
            }
        }
    };

    private View.OnClickListener pausePlay = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            v.playSoundEffect(SoundEffectConstants.CLICK);
//            audioManager.playSoundEffect(SoundEffectConstants.CLICK);
            mediaPlayer.start();

            if (whiteTimer != null)
                whiteTimer.cancel();
            if (blackTimer != null)
                blackTimer.cancel();
            black.setTextColor(Color.GRAY);
            white.setTextColor(Color.GRAY);
            blackmove.setTextColor(Color.GRAY);
            whitemove.setTextColor(Color.GRAY);
            blackRunning = false;
            whiteRunning = false;
        }
    };

    public void openJudge(View view) {
        if (whiteTimer != null)
            whiteTimer.cancel();
        if (blackTimer != null)
            blackTimer.cancel();
        black.setTextColor(Color.GRAY);
        white.setTextColor(Color.GRAY);
        blackmove.setTextColor(Color.GRAY);
        whitemove.setTextColor(Color.GRAY);
        blackRunning = false;
        whiteRunning = false;
        Intent intent = new Intent(this, WordJudgeActivity.class);
        startActivity(intent);
    }

    public void openSettings(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ClockActivity.this);
        builder.setTitle("Time Clock Settings");

        Context context = ClockActivity.this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText A = new EditText(ClockActivity.this);
        A.setInputType(InputType.TYPE_CLASS_TEXT);
        A.setHint("Player A");
        layout.addView(A);

        final EditText B = new EditText(ClockActivity.this);
        B.setInputType(InputType.TYPE_CLASS_TEXT);
        B.setHint("Player B");
        layout.addView(B);

        final EditText setTime = new EditText(ClockActivity.this);
        setTime.setInputType(InputType.TYPE_CLASS_NUMBER);
        setTime.setHint("Time Limit");
        layout.addView(setTime);

        builder.setView(layout); // Again this is a set method, not add

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (A.getText().length() > 0)
                    playerA.setText(A.getText().toString());
                if (B.getText().length() > 0)
                    playerB.setText(B.getText().toString());
                if (setTime.getText().length() > 0)
                    initialTime = 60000 * Integer.parseInt(setTime.getText().toString());
                Toast.makeText(getBaseContext(), "Player names changed; Press reset to use new time limit", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void showHelp(View view) {
        Intent intent = new Intent(this, ClockHelpActivity.class);
        startActivity(intent);
    }

    private View.OnLongClickListener resetClock = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(ClockActivity.this);
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(ClockActivity.this, LexData.getDatabasePath(), LexData.getDatabase());

            builder.setTitle("Do you want to reset the Time Clock ??");
            builder.setNegativeButton("No! Don't reset!", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.setPositiveButton("Yes. Reset Clock.", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (whiteTimer != null)
                        whiteTimer.cancel();
                    if (blackTimer != null)
                        blackTimer.cancel();
                    blackRunning = false;
                    whiteRunning = false;

                    blacktime = initialTime; // 25 minutes in milliseconds
                    totalBlack = blacktime + addedtime;
                    blackover = 0;
                    whitetime = initialTime; // 25 minutes
                    totalWhite = whitetime + addedtime;
                    whiteover = 0;

                    black.setTextColor(Color.GRAY);
                    String blackDisplay = String.format(Locale.US, "%02d:%02d",
                            (blacktime / 60000), (blacktime % 60000));
                    black.setText(blackDisplay);
                    blackmove.setText("00");
                    blackmove.setTextColor(Color.GRAY);

                    // white button starts black
                    white.setTextColor(Color.GRAY);
                    String whiteDisplay = String.format(Locale.US, "%02d:%02d",
                            (whitetime / 60000), (whitetime % 60000));
                    white.setText(whiteDisplay);
                    whitemove.setText("00");
                    whitemove.setTextColor(Color.GRAY);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        }

    };

    @Override public void onBackPressed(){
        exitAlert();




//        if (isTaskRoot()) {
//            exitAlert();
//        }
//        else {
//            if (doubleBackToExitPressedOnce) {
//                super.onBackPressed();
//                return;
//            }
//
//            this.doubleBackToExitPressedOnce = true;
//            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
//
//            new Handler().postDelayed(new Runnable() {
//
//                @Override
//                public void run() {
//                    doubleBackToExitPressedOnce = false;
//
//                }
//            }, 2000);
//        }



    }

    private boolean exitAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this, LexData.getDatabasePath(), LexData.getDatabase());

        builder.setTitle("Do you want to exit the Time Clock ??");
        builder.setPositiveButton("Yes. Quit now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (isTaskRoot())
                    finishAffinity();
                else {

                    if (Build.VERSION.SDK_INT < 16) {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }
                    else {
                        View decorView = getWindow().getDecorView();
// Hide the status bar.
                        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
                        decorView.setSystemUiVisibility(uiOptions);
// Remember that you should never show the action bar if the
// status bar is hidden, so hide that too if necessary.
                        ActionBar actionBar = getActionBar();
                        if (actionBar != null)
                            actionBar.show();
                    }
                    finish();
                }
            }
        });
        builder.setNegativeButton("Not now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
    }

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

}