package com.tylerhosting.hoot.hoot;

import static com.tylerhosting.hoot.hoot.CardUtils.programData;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;

public class UpdateMessageActivity extends AppCompatActivity {
    private Handler mWaitHandler = new Handler();
    DatabaseAccess databaseAccess;
    private static final int STORAGE_PERMISSION_CODE = 101;
    String internalDBPath;
    SharedPreferences shared;
    SharedPreferences.Editor prefs;
    CheckBox noBackup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_message);
        shared = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = shared.edit();


        internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();

        Button Yes = findViewById(R.id.btnYes);
        Yes.setOnClickListener(yes);

        Button No = findViewById(R.id.btnNo);
        No.setOnClickListener(no);

        Button Wait = findViewById(R.id.btnWait);
        Wait.setOnClickListener(wait);

        noBackup = findViewById(R.id.chkNoBackup);

        //
//        Flavoring.addflavoring(getApplicationContext()); // sets database
//        LexData.setDatabasePath(getApplicationContext(), "");
//        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());


    }

    ProgressDialog progressDialog;
    public String message = "Please Wait!\r\nProcessing...";
    private Runnable dialogMessages = new Runnable() {
        @Override
        public void run() {
            progressDialog.setMessage(message);

        }
    };

    public void updateDB(View view) {


        doUpdate();
//        if (!Utils.usingLegacy()) {
//            prefs.putString("cardlocation", "Internal");
//            prefs.putBoolean("saf", true);
//            prefs.apply();
//            continueUpdate("First Installed on Android 11+");
//            return;
//        }
//
//        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, STORAGE_PERMISSION_CODE);
        // goes to onRequestPermissionResult

    }
    private void doUpdate() {
//        Toast.makeText(this, "Backup old version of database", Toast.LENGTH_LONG).show();
        if (noBackup.isChecked()) {
            continueUpdate("No backup!");
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
        Log.d("backup", internalDBPath);
        new Thread() {
            public void run() {

                // Delete lexicon
                message = "Please Wait!\r\nBacking up old database";
                runOnUiThread(dialogMessages);

                if (!Utils.BackupDatabase(getApplicationContext(), internalDBPath)) {
                    message = "Backup failed.";
                    runOnUiThread(dialogMessages);
                }
                else {

                    message = "Please Wait!\r\nUpdating database";
                    runOnUiThread(dialogMessages);
                    File internalDatabase = new File(internalDBPath);
                    String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
                    Utils.copyDatabaseFromAssets(getApplicationContext(), assetsFilePath, internalDatabase);

                }
                message = "All done";
                runOnUiThread(dialogMessages);
                progressDialog.dismiss();
            }
        }.start();


        //        calls utils, not Tools
//        if (!Utils.BackupDatabase(this, internalDBPath))
//            continueUpdate("Backup Failed!");
//        else {
//            Toast.makeText(this, "\nUpdating distributed version of database. \nPlease wait!\n", Toast.LENGTH_LONG).show();
//            File internalDatabase = new File(internalDBPath);
//            String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
//            Utils.copyDatabaseFromAssets(this, assetsFilePath, internalDatabase);
//            Flavoring.addflavoring(this); // sets database
//        }



        startHoot();

    }
    public void noUpdate(View view) {

        String sql = "INSERT or IGNORE into tblVersions (VersionID, VersionNumber) Values ('" + LexData.CURRENT_VERSION + "', '" + LexData.CURRENT_VERSION + "')";
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();
        databaseAccess.execSQL(sql);
        databaseAccess.close();
        startHoot();
    }
    public void waitForUpdate(View view) {
        startHoot();
    }
    private boolean continueUpdate(String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle(title);

        String msg = "Do you want to continue to update database without saving a backup?\n";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Update Database.!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                Toast.makeText(getApplicationContext(), "\nUpdating distributed version of database. \nPlease wait!\n", Toast.LENGTH_LONG).show();
                File internalDatabase = new File(internalDBPath);
                String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
                Utils.copyDatabaseFromAssets(getApplicationContext(), assetsFilePath, internalDatabase);
                Flavoring.addflavoring(getApplicationContext()); // sets database

                startHoot();

            }
        });
        builder.setNegativeButton("Not now", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                startHoot();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;

    }
    private void startHoot() {

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

    // Listeners
    private View.OnClickListener yes = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            updateDB(v);
//            msg = "Copied " + sourceFolder + " to " + destinationFolder;
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };

    private View.OnClickListener no = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            noUpdate(v);
//            msg = "Copied " + sourceFolder + " to " + destinationFolder;
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };

    private View.OnClickListener wait = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            waitForUpdate(v);
//            msg = "Copied " + sourceFolder + " to " + destinationFolder;
//            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
                doUpdate();


            } else {

                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                continueUpdate("Backup Failed!");

            }
        }
    }

    @Override
    public void onBackPressed() {
        startHoot();
    }
}


