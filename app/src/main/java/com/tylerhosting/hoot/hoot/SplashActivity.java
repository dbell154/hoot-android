package com.tylerhosting.hoot.hoot;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static com.tylerhosting.hoot.hoot.Utils.isFirstInstall;
import static com.tylerhosting.hoot.hoot.Utils.usingLegacy;

// use this to set LexData
public class
SplashActivity extends AppCompatActivity {
    private Handler mWaitHandler = new Handler();

    SharedPreferences shared;
    SharedPreferences.Editor prefs;
    DatabaseAccess databaseAccess;
    ProgressDialog progressDialog;
    public String message = "Please Wait!\r\nProcessing...";
    private Runnable dialogMessages = new Runnable() {
        @Override
        public void run() {
            progressDialog.setMessage(message);

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        createNotificationChannel();


        //        SharedPreferences
        shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Log.d("Splash ", shared.getString("database", "") );
        Log.d("Splash ", shared.getString("lexicon", ""));

        // initialize Database
        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        shared = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = shared.edit();

        // check version compatibility
        if (!usingLegacy()) {
            prefs.putString("database", "Internal");
            prefs.putString("cardlocation", "Internal");
            prefs.apply();
        }


        // need to save database and path, and reset when starting
        String fullpath = shared.getString("database", "");
        if (fullpath.equals("")) {
            fullpath = "Internal";
            prefs.putString("database", "Internal");
            prefs.apply();
        }


        // set database lexdata to default if internal
        if (fullpath.equals("Internal") || !fullpath.contains(File.separator))  {
            Flavoring.addflavoring(getApplicationContext()); // sets database
            LexData.setDatabasePath(getApplicationContext(), "");
        }
        // set database lexdata to saved path
        else {
            LexData.setDatabase(getApplicationContext(), fullpath.substring(fullpath.lastIndexOf(File.separator)));
            LexData.setDatabasePath(getApplicationContext(), fullpath.substring(0, fullpath.lastIndexOf(File.separator)));
        }


        // ARRESTED
//        Flavoring.addflavoring(getApplicationContext()); // sets database
//        LexData.setDatabasePath(getApplicationContext(), "");

        // Create DatabaseAccess instance
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

        Log.d("Splash ", shared.getString("database", "") );
        Log.d("Splash ", shared.getString("lexicon", ""));
        Log.d("Splash ", LexData.getDatabasePath() + " - " + LexData.getDatabase());
        Log.d("Splash", fullpath);
//        Log.d("Splash ", LexData.getLexName());

        // If internal database is missing
        String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
        File internalDatabase = new File(internalDBPath);
        if (!internalDatabase.exists())
            restoreDatabase();


        // if internal database is selected
        if (fullpath.equals("Internal") || !fullpath.contains(File.separator)) {
//            if (databaseAccess.getVersion() < LexData.getCurrentVersion(this)) {

            int installedVersion = databaseAccess.getVersion();
            // if database is outdated

//            updateDatabase();
//            Toast.makeText(this, "\nDatabase has been updated!, Select new lexicon in Settings.\n", Toast.LENGTH_LONG).show();

            if (installedVersion < LexData.CURRENT_VERSION) {
                if (installedVersion < 18) {
                    startUpdateMessage(); // when lexicons have boon removed
//                    LexData.setCurrentVersion(this, databaseAccess.getVersion());
//                    startHoot();
                }
                else {
                    updateDatabase();
                    Toast.makeText(this, "\nDatabase has been updated!, Select new lexicon in Settings.\n", Toast.LENGTH_LONG).show();
//                    LexData.setCurrentVersion(this, databaseAccess.getVersion());
//                    startHoot();
                }
//                     add other files to file folder
// Test first   addfiles();
            }
            else
                startHoot();

        }
        else startHoot();
    }

    public void startHoot() {

//        if (isFirstInstall(this))


            Toast.makeText(this, "Select lexicon in Settings", Toast.LENGTH_LONG).show();

        String lexicon = shared.getString("lexicon", "");
        setLexicon(lexicon);
        mWaitHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //The following code will execute after the 5 seconds.
                try {
                    Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }, 5000);  // Give a 5 seconds delay.

    }

    public void startUpdateMessage() {
        mWaitHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //The following code will execute after the 5 seconds.
                try {
                    Intent intent = new Intent(getApplicationContext(), UpdateMessageActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
            }
        }, 5000);  // Give a 5 seconds delay.

    }
    public void restoreDatabase() {
        Toast.makeText(this, "\nRestoring distributed version of database. \nPlease wait!\n", Toast.LENGTH_LONG).show();
        Flavoring.addflavoring(this); // sets database
        String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
        File internalDatabase = new File(internalDBPath);
        String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
        Utils.copyDatabaseFromAssets(this, assetsFilePath, internalDatabase);
    }
    public void updateDatabase() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
        new Thread() {
            public void run() {

                // Delete lexicon
                message = "\nUpdating distributed version of database. \nPlease wait!\n";
                runOnUiThread(dialogMessages);

                String internalDBPath = getApplicationContext().getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
                File internalDatabase = new File(internalDBPath);
                String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
                Utils.copyDatabaseFromAssets(getApplicationContext(), assetsFilePath, internalDatabase);
//                Flavoring.addflavoring(this); // sets database
//                Toast.makeText(this, "\nDatabase has been updated!, Select new lexicon in Settings.\n", Toast.LENGTH_LONG).show();


                progressDialog.dismiss();
            }
        }.start();


//                Toast.makeText(this, "\nUpdating distributed version of database. \nPlease wait!\n", Toast.LENGTH_LONG).show();
//                String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
//                File internalDatabase = new File(internalDBPath);
//                String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
//                Utils.copyDatabaseFromAssets(this, assetsFilePath, internalDatabase);
//                Flavoring.addflavoring(this); // sets database
//                Toast.makeText(this, "\nDatabase has been updated!, Select new lexicon in Settings.\n", Toast.LENGTH_LONG).show();
    }
    public void addfiles() {
        Toast.makeText(this, "\nAdding files to your assets. \nPlease wait!\n", Toast.LENGTH_LONG).show();
        String internalPath = this.getFilesDir().getAbsolutePath();
        File internalFiles = new File(internalPath);
        String assetsFilePath = "files";
        Utils.copyFilesFromAssets(this, assetsFilePath, internalFiles);
//        Toast.makeText(this, "\nFiles have been added to your assets.\n", Toast.LENGTH_LONG).show();
    }
    public void setLexicon(String lexname) {
        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexname);
        LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);

        // Don't attempt to extract in Splash
        if (!databaseAccess.checkLexicon(lexicon)) {
            Toast.makeText(getApplicationContext(), "Failed to load lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            databaseAccess.defaultDBLexicon(getApplicationContext());
            Utils.setDatabasePreference(this);
            Toast.makeText(getApplicationContext(), "Resetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this, "Using Lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, LexData.getLexicon().LexiconNotice, Toast.LENGTH_LONG).show();
        Utils.setLexiconPreference(this);

        // todo Do I need to checkScores like in Settings?
    }
    public void createNotificationChannel() {
        final String CHANNEL_ID = "Hoot";

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //CharSequence name = getString(R.string.channel_name);
            //String description = getString(R.string.channel_description);
            CharSequence name = "Hoot";
            String description = "Hoot Notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}