package com.tylerhosting.hoot.hoot;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.File;

import static android.os.Environment.DIRECTORY_DOCUMENTS;

public class StorageRequestActivity extends AppCompatActivity {

    Uri uriToLoad;
    int RQS_OPEN_DOCUMENT_TREE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage_request);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            openDirectory();

        }
    }

    public void openDirectory(){
        String CardPath;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            CardPath = Environment.getExternalStoragePublicDirectory("Documents").toString();
        else
            CardPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();


        uriToLoad = Uri.fromFile(new File(CardPath));
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);

        startActivityForResult(intent,
                RQS_OPEN_DOCUMENT_TREE);

        return;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == RQS_OPEN_DOCUMENT_TREE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                // Perform operations on the document using its URI.


                final int takeFlags = getIntent().getFlags() &
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                // Check for the freshest data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                }

            }
        }
    }}