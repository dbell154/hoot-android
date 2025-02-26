package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v4.provider.DocumentFile;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.PendingIntent.getActivity;
import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.view.View.VISIBLE;
import static com.tylerhosting.hoot.hoot.CardUtils.programData;

import com.tylerhosting.hoot.EncryptActivity;

public class ToolsActivity extends AppCompatActivity {
    SharedPreferences shared;
    DatabaseAccess databaseAccess;
    Button spec;
    EditText anal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.setStartTheme(this);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_tools2col);

        Button Copy1924Cards = findViewById(R.id.btnCopy1924Cards);
        Copy1924Cards.setOnClickListener(copy1924Cards);

        Button Copy2224Cards = findViewById(R.id.btnCopy2224Cards);
        Copy2224Cards.setOnClickListener(copy2224Cards);

        Button Copy2324Cards = findViewById(R.id.btnCopy2324Cards);
        Copy2324Cards.setOnClickListener(copy2324Cards);

        Button CopyNWLCards = findViewById(R.id.btnCopyNWLCards);
        CopyNWLCards.setOnClickListener(copyNWLCards);

        Button CopyWOWCards = findViewById(R.id.btnCopyWOWCards);
        CopyWOWCards.setOnClickListener(copyWOWCards);

        Button DeleteList = findViewById(R.id.btnDeleteList);
        DeleteList.setOnClickListener(deleteList);;

        Button ExportList = findViewById(R.id.btnExportList);
        ExportList.setOnClickListener(exportList);;

        Button updater = findViewById(R.id.btnDBUpdate);
        updater.setOnClickListener(updateDB);

        Button CopyDB = findViewById(R.id.btnCopyAssetsDB);
        CopyDB.setOnClickListener(copyAssetsDB);

        Button CopyInternal = findViewById(R.id.btnCopyInternalDB);
        CopyInternal.setOnClickListener(copyInternal);

        Button CopySelected = findViewById(R.id.btnCopySelectedFiles);
        CopySelected.setOnClickListener(copySelected);

        Button ImportDB = findViewById(R.id.btnImportDatabase);
        ImportDB.setOnClickListener(importDataBase);;

        Button ImportCardbox = findViewById(R.id.btnImportCardbox);
        ImportCardbox.setOnClickListener(importCardbox);;
//        if (LexData.getUsername().equals("Testing"))
            ImportCardbox.setVisibility(VISIBLE);

        Button DeleteDB = findViewById(R.id.btnDeleteInternalFiles);
        DeleteDB.setOnClickListener(deleteInternalFiles);;
        // using Copy instead
        Button BackupDB = findViewById(R.id.btnBackupDB);
        BackupDB.setOnClickListener(backupDB);

        Button DeleteLex = findViewById(R.id.btnDeleteLexicon);
        DeleteLex.setOnClickListener(deleteLexicon);;

        Button CompactDB = findViewById(R.id.btnCompactDatabase);
        CompactDB.setOnClickListener(compactDatabase);;


        shared = PreferenceManager.getDefaultSharedPreferences(this);
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

        spec = findViewById(R.id.btnAnalyze);
        anal = findViewById(R.id.analystlog);

//        Button multiBtn = findViewById(R.id.multisearch);
//        multiBtn.setVisibility(View.VISIBLE);

        //        if (LexData.getUsername().equals("Beta Tester"))
////        if (BuildConfig.VERSION_NAME.contains("beta"))
//            multiBtn.setVisibility(View.VISIBLE);
//        else
//            multiBtn.setVisibility(View.GONE);


//        Button Encrypt = findViewById(R.id.btnEncryption);
//        if (LexData.getUsername().equals("Beta Tester")) {
//            Encrypt.setVisibility(View.VISIBLE);
//        }


//        if (BuildConfig.VERSION_NAME.contains("beta")) {
//            spec.setVisibility(View.VISIBLE);
//            anal.setVisibility(View.VISIBLE);
//        }
//        anal.setText("noBA ");


//        String user = shared.getString("user", "New User");
//        Button clock = findViewById(R.id.btnTimer);
//        if (!user.equals("DanaB")) {
//            clock.setVisibility(View.GONE);
//        }

//        Button ilist = findViewById(R.id.btnImportList);
//        if (!user.equals("DanaB")) {
//            ilist.setVisibility(View.GONE);
//        }

    }


    ////////// VARIABLES //////////
    ProgressDialog progressDialog;
    public String message = "Please Wait!\r\nProcessing...";
    private Runnable dialogMessages = new Runnable() {
        @Override
        public void run() {
            progressDialog.setMessage(message);

        }
    };
    DatabaseAccess otherDatabase, otherLexicon;
    String otherDBPath, otherDBFile;
    String DB_NAME = Flavoring.getflavoring();
    // Need to allow user to select which internal database to copy
    String internalDBPath = LexData.internalFilesDir + File.separator + Flavoring.getflavoring();
    String getInternalCardboxPath = LexData.internalFilesDir + "/Cards/" + LexData.getLexName() + "/";
    String selectedDBPath = "";
    public String ExportListName = "";



    ////////// GUIDE //////////
    public void getHelp(String html) {

        Intent intentBundle = new Intent(this, HelpFileActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("html", html);
        intentBundle.putExtras(bundle);

        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//

        startActivity(intentBundle);

        overridePendingTransition (0, 0);//
    }

    public void toolguide(View view) {
        getHelp("toolguide.html");
    }



    //////////// CARD BOXES //////////
    public void cardmgmt(View view) {
        Intent intent = new Intent(this, CardMgmtActivity.class);
        startActivity(intent);
    }

    private View.OnClickListener copy1924Cards = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String sourceFolder = programData("Hoot") + File.separator + "CSW19";
            String destinationFolder = programData("Hoot") + File.separator + "CSW24";

            File fSource = new File(sourceFolder);
            File fDest = new File(destinationFolder);

            String msg = "Copying " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            if (LexData.getCardslocation().equals("Internal") || permission())
                copyCardFolderThread(fSource, fDest);

            msg = "Copied " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };
    private View.OnClickListener copy2224Cards = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String sourceFolder = programData("Hoot") + File.separator + "CSW22";
            String destinationFolder = programData("Hoot") + File.separator + "CSW24";

            File fSource = new File(sourceFolder);
            File fDest = new File(destinationFolder);

            String msg = "Copying " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            if (LexData.getCardslocation().equals("Internal") || permission())
                copyCardFolderThread(fSource, fDest);

            msg = "Copied " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };
    private View.OnClickListener copy2324Cards = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String sourceFolder = programData("Hoot") + File.separator + "NWL23";
            String destinationFolder = programData("Hoot") + File.separator + "CSW24";

            File fSource = new File(sourceFolder);
            File fDest = new File(destinationFolder);

            String msg = "Copying " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            if (LexData.getCardslocation().equals("Internal") || permission())
                copyCardFolderThread(fSource, fDest);

            msg = "Copied " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };
    private View.OnClickListener copyNWLCards = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String sourceFolder = programData("Hoot") + File.separator + "NWL20";
            String destinationFolder = programData("Hoot") + File.separator + "NWL23";

            File fSource = new File(sourceFolder);
            File fDest = new File(destinationFolder);

            String msg = "Copying " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            if (LexData.getCardslocation().equals("Internal") || permission())
                copyCardFolderThread(fSource, fDest);

            msg = "Copied " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };
    private View.OnClickListener copyWOWCards = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String sourceFolder = programData("Hoot") + File.separator + "NWL18";
            String destinationFolder = programData("Hoot") + File.separator + "WOW24";

            File fSource = new File(sourceFolder);
            File fDest = new File(destinationFolder);

            String msg = "Copying " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

            if (LexData.getCardslocation().equals("Internal") || permission())
                copyCardFolder(fSource, fDest);

            msg = "Copied " + sourceFolder + " to " + destinationFolder;
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    };
    private void copyCardFolder(File fSource, File fDest) {

//        String msg = "Copying " + fSource.toString() + " to " + fDest.toString();
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        try {
//            while (fSource.isDirectory()) {
            if (fSource.isDirectory()) {
                // A simple validation, if the destination is not exist then create it
                if (!fDest.exists()) {
                    fDest.mkdirs();
                }

                // Create list of files and directories on the current source
                // Note: with the recursion 'fSource' changed accordingly
                String[] fList = fSource.list();

                for (int index = 0; index < fList.length; index++) {
                    File dest = new File(fDest, fList[index]);
                    File source = new File(fSource, fList[index]);

                    // Recursion call take place here
                    copyCardFolder(source, dest);
                }
            } else {
                // Found a file. Copy it into the destination, which is already created in 'if' condition above

                // Open a file for read and write (copy)
                FileInputStream fInStream = new FileInputStream(fSource);
                FileOutputStream fOutStream = new FileOutputStream(fDest);

                // Read 2K at a time from the file
                byte[] buffer = new byte[2048];
                int iBytesReads;

                // In each successful read, write back to the source
                while ((iBytesReads = fInStream.read(buffer)) >= 0) {
                    fOutStream.write(buffer, 0, iBytesReads);
                }

                // Safe exit
                if (fInStream != null) {
                    fInStream.close();
                }

                if (fOutStream != null) {
                    fOutStream.close();
                }
            }
        } catch (Exception ex) {
            // Please handle all the relevant exceptions here
        }
//        msg = "Copied " + fSource.toString() + " to " + fDest.toString();
//        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();


    }
    private void copyCardFolderThread(File fSource, File fDest) {
        String msg = "Copying " + fSource.toString() + " to " + fDest.toString();
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        new Thread() {
            public void run() {

                message = "Please Wait!\r\nCopying card folders ";
                runOnUiThread(dialogMessages);
                Log.d("cards", "copying");

                message = "Copying " + fSource.getName() + "... ";
                runOnUiThread(dialogMessages);
                Log.d("cards", "copying");


                    try {
            //            while (fSource.isDirectory()) {
                        if (fSource.isDirectory()) {
                            // A simple validation, if the destination is not exist then create it
                            if (!fDest.exists()) {
                                fDest.mkdirs();
                            }

                            // Create list of files and directories on the current source
                            // Note: with the recursion 'fSource' changed accordingly
                            String[] fList = fSource.list();

                            for (int index = 0; index < fList.length; index++) {
                                File dest = new File(fDest, fList[index]);
                                File source = new File(fSource, fList[index]);


                                message = "Copying " + source.getName() + "... ";
                                runOnUiThread(dialogMessages);
                                Log.d("cards", message);


                                // Recursion call take place here
                                copyCardFolder(source, dest);
                            }
                        } else {
                            // Found a file. Copy it into the destination, which is already created in 'if' condition above

                            // Open a file for read and write (copy)
                            FileInputStream fInStream = new FileInputStream(fSource);
                            FileOutputStream fOutStream = new FileOutputStream(fDest);

                            message = "Copying " + fSource.getName() + "... ";
                            runOnUiThread(dialogMessages);
                            Log.d("cards", message);


                            // Read 2K at a time from the file
                            byte[] buffer = new byte[2048];
                            int iBytesReads;

                            // In each successful read, write back to the source
                            while ((iBytesReads = fInStream.read(buffer)) >= 0) {
                                fOutStream.write(buffer, 0, iBytesReads);
                            }

                            // Safe exit
                            if (fInStream != null) {
                                fInStream.close();
                            }

                            if (fOutStream != null) {
                                fOutStream.close();
                            }
                        }
                    } catch (Exception ex) {
                        Log.d("Exception ex", ex.toString());

                        // Please handle all the relevant exceptions here
                    }


                message = "All done";
                runOnUiThread(dialogMessages);
                Log.d("Copied cards", "All done");
                progressDialog.dismiss();

            }
        }.start();

        msg = "Copied " + fSource.toString() + " to " + fDest.toString();
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();


    }

    private View.OnClickListener importCardbox = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            importCardbox();
//            CharSequence msg = "Importing database";
//            Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
        }
    };
    public void importCardbox() {
//        String DEST_PATH = this.getFilesDir().getAbsolutePath().replace("files", "databases")

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("application/vnd_sqlite3");
        intent.setType("application/octet-stream");
//        intent.putExtra(Intent.EXTRA_TITLE, filename);
        Log.d("import", "importCardbox");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
        startActivityForResult(intent, IMPORT_CARDBOX);
    }
    public void copyUriCardboxToInternalThread(Context context, Uri input, String filename) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        new Thread() {
            public void run() {


                message = "Please Wait!\r\nImporting cardbox ";
                runOnUiThread(dialogMessages);
                Log.d("Update", "updating");


                InputStream inputStream = null;
                File internalCardbox;

                String internalCardboxPath = getInternalCardboxPath + File.separator + filename;

                //String internalCardboxPath = LexData.internalFilesDir + File.separator + filename;
                ; // + File.separator + Flavoring.getflavoring();
                Log.d("import", "CogyUriCardboxToInternal" + internalCardboxPath);

                Log.d("split", filename);
                internalCardbox = new File(internalCardboxPath);


                File cardfolder = new File(getInternalCardboxPath);

                if (!cardfolder.exists()) {
                    boolean success = cardfolder.mkdirs();
                    if (!success) {
                        Log.e("mkdirs", "failed to mkdirs");
                        return;
                    }
                }

                if (internalCardbox.exists()) {
                    boolean replace = ImportCardboxAlert(filename);
                    if (!replace)
                        return;
                }
// internalDatabase is the file
                // filename is the file
//                int num=0;
//                while (internalCardbox.exists()) {
//                    num++;
//                    String[] alt = filename.split("\\.(?=[^\\.]+$)");  // CHANGE
////                String[] alt = DB_NAME.split("\\.(?=[^\\.]+$)");
////                    internalCardbox = new File(LexData.internalFilesDir + "/Cards" + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
//                    internalCardbox = new File(getInternalCardboxPath + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
//                }


                try {
                    inputStream = context.getContentResolver().openInputStream(input);
//            Log.e("CopyDB", inputStream.toString());
                    Utils.copyStreamTo(inputStream, internalCardbox);
                } catch (IOException e) {
                    //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        Log.e("CopyCardbox", "Failed to close the destination");
                    }
                }
                message = "\nCardbox Imported\n ";
                runOnUiThread(dialogMessages);
                Log.d("Import", "imported");
                progressDialog.dismiss();


            }
        }.start();
    }
    private boolean ImportCardboxAlert(String filename) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Replace Cardbox");

        String msg = "Are you sure you want to replace " + filename + "?";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Replace Now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                CharSequence msg = "Deleting database";
//                Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
                deleteFile("filename");
//                        ??myContext.deleteFile(fileName);
//                file.delete();
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



    ////////// LEXICONS //////////
    // Import Lexicon
    public void importAct(View view) {
        Intent intent = new Intent(this, ImportLexiconActivity.class);
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
//            if (LexData.getCardslocation() == "Internal")
//                Toast.makeText(this, "This may not function in Android 11", Toast.LENGTH_LONG).show();
        startActivity(intent);
    }

    // Delete Lexicon
    private View.OnClickListener deleteLexicon = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectLexiconForDeletion();
        }
    };
    public void selectLexiconForDeletion(){
        // only called from Select options, not when loading
        databaseAccess = DatabaseAccess.getInstance(this, LexData.getDatabasePath(), LexData.getDatabase());
        ArrayList<Structures.Lexicon> lexiconList = databaseAccess.get_lexicons();

        if (!lexiconList.isEmpty()) {
            ArrayList<String> options= new ArrayList<>();
            for (int c = 0; c < lexiconList.size(); c++) {
                options.add(lexiconList.get(c).LexiconName);
            }
            final String[] opts;
            opts = options.toArray(new String[]{});
            Log.d("opts", String.valueOf(opts.length));

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select");
            builder.setItems(opts, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    // opts[item] is the name of the lexicon

                    dialog.dismiss();
                    deleteLexiconAlert(opts[item]);

                }
            });
            builder.show();
        }
    }
    private boolean deleteLexiconAlert(String lexicon) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Delete Lexicon");

        String msg = "Are you sure you want to delete " + lexicon + "?";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Delete Now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                deleteLexicon(lexicon);
                deleteLexiconThread(lexicon);
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
    private void deleteLexiconThread(String lexicon) {
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

//        ProgressBar progressBar = new ProgressBar(this);
//        progressBar.start
        CharSequence msg = "Deleting lexicon";
        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();

        new Thread() {
            public void run() {

                // Delete lexicon
                message = "Please Wait!\r\nDeleting " + lexicon;
                runOnUiThread(dialogMessages);

                String sql = "DROP TABLE IF EXISTS " + lexicon + "; ";
                Log.d("Dropping", lexicon);
                databaseAccess.execSQL(sql);

                // Delete lexicon entry
                message = "Please Wait!\r\nDeleting lexicon entry for " + lexicon;
                runOnUiThread(dialogMessages);

                sql = "DELETE FROM tblLexicons WHERE LexiconName = '" + lexicon + "'; ";
                Log.d("Delete Entry", sql);
                databaseAccess.execSQL(sql);

// todo move to separate tool
//                // Compacting
//                message = "Please Wait!\r\nCompacting";
//                runOnUiThread(dialogMessages);
//
//                sql = "Vacuum;";
//                Log.i("Compacting", lexicon);
//                databaseAccess.execSQL(sql);

                message = "All done";
                runOnUiThread(dialogMessages);
                progressDialog.dismiss();
            }
        }.start();
        msg = "Deleted lexicon " + lexicon;
        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
    }
    public void deleteLexicon(String lexicon) {
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();

        progressDialog = new ProgressDialog(getApplicationContext());
        progressDialog.setMessage("Please Wait!\r\nDeleting lexicon for Android use...");


        String sql = "DROP TABLE IF EXISTS " + lexicon + "; ";
        Log.d("DelLex", lexicon);
        databaseAccess.execSQL(sql);

        sql = "DELETE FROM tblLexicons WHERE LexiconName = '" + lexicon + "'; ";
        Log.d("sql", sql);
        databaseAccess.execSQL(sql);

        message = "Please Wait!\r\nCompacting\r\n";
        runOnUiThread(dialogMessages);


        sql = "Vacuum;";
        databaseAccess.execSQL(sql);


        Log.i("Delete Lexicon", lexicon);


    }


    public void useLexicon(View view){
        // only called from Select options, not when loading
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());
        ArrayList<Structures.Lexicon> lexiconList = databaseAccess.get_lexicons();

        if (!lexiconList.isEmpty()) {
            ArrayList<String> options= new ArrayList<>();
            for (int c = 0; c < lexiconList.size(); c++) {
                options.add(lexiconList.get(c).LexiconName);
            }
            final String[] opts;
            opts = options.toArray(new String[]{});
            Log.d("opts", String.valueOf(opts.length));


            final android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
            builder.setTitle("Select");
            builder.setItems(opts, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    // opts[item] is the name of the lexicon

                    dialog.dismiss();
                    setLexicon(opts[item]);

                }
            });
            builder.show();
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            Log.i("SetSelectLexiconPrefs", shared.getString("lexicon", "x"));

        }
        else {
            Toast.makeText(this, "Empty lexicon. \r\nIgnoring " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            if (true) return;

//5                Toast.makeText(getActivity(), "Empty lexicon. \r\nResetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            databaseAccess.defaultDBLexicon(this.getApplicationContext());
            Utils.setDatabasePreference(this.getApplicationContext());
            setLexicon(LexData.getLexName());

        }
    }
    public void setLexicon(String lexname) { // sets LexData and prefs; this handles whether or not valid; don't set before calling
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//            SharedPreferences.Editor prefs = shared.edit();

        databaseAccess = DatabaseAccess.getInstance(getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());
        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexname);
        // need to check lexicon before changing
        if (!databaseAccess.checkLexicon(lexicon))
            Utils.lexAlert(this,lexname); // not getApplicationContext
        else {
            LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);
            Toast.makeText(this, "Using Lexicon " + lexicon.LexiconName, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, lexicon.LexiconNotice, Toast.LENGTH_LONG).show();
            Utils.setLexiconPreference(getApplicationContext());
            Log.i("SettingsSetLexiconPrefs", shared.getString("lexicon", "x"));

        }
    }



    ////////// SUBJECT LISTS //////////
    // Import Subject List
    public void importList(View view) {
        Intent intent = new Intent(this, ImportSubjectListActivity.class);
        startActivity(intent);
    }

    // Delete Subject List
    private View.OnClickListener deleteList = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectListForDeletion();
        }
    };
    public void selectListForDeletion(){
        // only called from Select options, not when loading
        databaseAccess = DatabaseAccess.getInstance(this, LexData.getDatabasePath(), LexData.getDatabase());
        ArrayList<LexData.WordList> wordListsList = databaseAccess.get_subjectLists();

        if (!wordListsList.isEmpty()) {
            ArrayList<String> options= new ArrayList<>();
            for (int c = 0; c < wordListsList.size(); c++) {
                options.add(wordListsList.get(c).ListName);
            }
            final String[] opts;
            opts = options.toArray(new String[]{});
            Log.d("opts", String.valueOf(opts.length));

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select");
            builder.setItems(opts, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    // opts[item] is the name of the list

                    dialog.dismiss();
                    deleteListAlert(opts[item]);

                }
            });
            builder.show();
        }
    }
    private boolean deleteListAlert(String list) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Delete Subject List");

        String msg = "Are you sure you want to delete " + list + "?";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Delete Now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                deleteLexicon(lexicon);
                deleteListThread(list);
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
    private void deleteListThread(String list) {
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

//        ProgressBar progressBar = new ProgressBar(this);
//        progressBar.start
        CharSequence msg = "Deleting list";
        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();

        new Thread() {
            public void run() {

                // Delete list items
                message = "Please Wait!\r\nDeleting subject list entries for " + list;
                runOnUiThread(dialogMessages);

                String sql = "DELETE FROM WordList WHERE ListID = '" + databaseAccess.getListID(list) + "'; ";
                Log.d("Delete Entry", sql);
                databaseAccess.execSQL(sql);


                // Delete list entry
                message = "Please Wait!\r\nDeleting subject list " + list;
                runOnUiThread(dialogMessages);

                sql = "DELETE FROM WordLists WHERE ListName = '" + list + "'; ";
                Log.d("Delete Entry", sql);
                databaseAccess.execSQL(sql);

// todo move to separate tool
//                // Compacting
//                message = "Please Wait!\r\nCompacting";
//                runOnUiThread(dialogMessages);
//
//                sql = "Vacuum;";
//                Log.i("Compacting", lexicon);
//                databaseAccess.execSQL(sql);

                message = "All done";
                runOnUiThread(dialogMessages);
                progressDialog.dismiss();
            }
        }.start();
        msg = "Deleted list " + list;
        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
    }

    // Export Subject List
    private View.OnClickListener exportList = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            selectListForExport();
        }
    };
    public void selectListForExport(){
        // only called from Select options, not when loading
        databaseAccess = DatabaseAccess.getInstance(this, LexData.getDatabasePath(), LexData.getDatabase());
        ArrayList<LexData.WordList> wordListsList = databaseAccess.get_subjectLists();

        if (!wordListsList.isEmpty()) {
            ArrayList<String> options= new ArrayList<>();
            for (int c = 0; c < wordListsList.size(); c++) {
                options.add(wordListsList.get(c).ListName);
            }
            final String[] opts;
            opts = options.toArray(new String[]{});
            Log.d("opts", String.valueOf(opts.length));

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select");
            builder.setItems(opts, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    // opts[item] is the name of the list

                    dialog.dismiss();

                    try {
                        exportList(opts[item]);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            });
            builder.show();
        }
    }
    private void exportList(String listName) throws IOException {
        // expose for SAF method
        ExportListName = listName;

        String DEST_PATH;
        int counter = 0;

        Log.d("listname", listName);
        databaseAccess = DatabaseAccess.getInstance(this, LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();
//        ListID = databaseAccess.getListID(listName);
//        int listID = databaseAccess.getListID(listName);

        String sql = "Select ListID from WordLists where Upper(ListName) = '" + listName.toUpperCase() + "'";
        Log.d("sql", sql);
        Cursor cursor = databaseAccess.rawQuery(sql);
cursor.moveToFirst();
//        Cursor cursor = databaseAccess.rawQuery("Select ListID from WordLists where Upper(ListName) = '" + listName.toUpperCase() + "'");
        int listID = cursor.getInt(cursor.getColumnIndex("ListID"));
        cursor.close();


        cursor = databaseAccess.getCursor_listwords(listID, "", "");

        int column = cursor.getColumnIndex("Word");
        String filename=listName;

        if (!filename.endsWith(".txt"))
            filename = filename + ".txt";

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || saf) {
        if (Utils.usingSAF()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);

            startActivityForResult(intent, EXPORT_LIST);
        }
        else {

            DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

            File directory = new File(DEST_PATH);
            if (!directory.exists())
                if (!directory.mkdirs()) {
                    Log.e("savelist", "Can't mkdirs()" + directory.getAbsolutePath());
                    Toast.makeText(getBaseContext(), "Can't create folder for saving", Toast.LENGTH_LONG).show();
                    return;
                }
            filename = DEST_PATH + File.separator + filename;




                Cursor detailCursor = databaseAccess.getCursor_subjectList(listName);
                Log.d("listname", listName);

                detailCursor.moveToFirst();
                String lN = detailCursor.getString(detailCursor.getColumnIndex("ListName"));
                String lA = detailCursor.getString(detailCursor.getColumnIndex("ListAuthor"));
                String lC = detailCursor.getString(detailCursor.getColumnIndex("ListCredits"));
                String lD = detailCursor.getString(detailCursor.getColumnIndex("ListDescription"));
                String lS = detailCursor.getString(detailCursor.getColumnIndex("ListSource"));
                String lL = detailCursor.getString(detailCursor.getColumnIndex("ListLink"));
                String lAE = detailCursor.getString(detailCursor.getColumnIndex("ListAuthorEmail"));
                String catID = detailCursor.getString(detailCursor.getColumnIndex("CategoryID"));
                detailCursor.close();

                sql = "SELECT Category from tblListCategories WHERE CategoryID = " + catID + ";";
                Cursor catCursor = databaseAccess.rawQuery(sql);
                catCursor.moveToFirst();
                String cat = catCursor.getString(catCursor.getColumnIndex("Category"));
                catCursor.close();

                try {

                    FileWriter writer = new FileWriter(filename);


                if (lN != null && !lN.isEmpty())
                    writer.append("//List Name: " + lN + "\r\n");
                if (lA != null && !lA.isEmpty())
                    writer.append("//List Author: " + lA + "\r\n");
                if (lC != null && !lC.isEmpty())
                    writer.append("//List Credits: " + lC + "\r\n");
                if (lD != null && !lD.isEmpty())
                    writer.append("//List Description: " + lD + "\r\n");
                if (lS != null && !lS.isEmpty())
                    writer.append("//List Source: " + lS + "\r\n");
                if (lL != null && !lL.isEmpty())
                    writer.append("//List Link: " + lL + "\r\n");
                if (lAE != null && !lAE.isEmpty())
                    writer.append("//List Email: " + lAE + "\r\n");
                if (cat != null && !cat.isEmpty())
                    writer.append("//List Category: " + cat + "\r\n");

                String sep;
                sep = System.lineSeparator();

                cursor.moveToFirst();
                counter++;
                writer.append(cursor.getString(column).toUpperCase() + sep);
                while (cursor.moveToNext()) {
                    counter++;
                    writer.append(cursor.getString(column).toUpperCase() + sep);
                }
                writer.close();
                Toast.makeText(getBaseContext(), "Saving " + counter + " words to " + filename, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }



    ////////// DATABASES //////////
    // Update Database
    private View.OnClickListener updateDB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            updateAlert();

        }
    };
    private void updateAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Update Database");

        String msg = "Are you sure you want to update the Internal database without making a backup?";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Update Now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                updateDBThread();
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
    }
    private void updateDBThread() {

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        new Thread() {
            public void run() {

                message = "Please Wait!\r\nUpdating internal database ";
                runOnUiThread(dialogMessages);
                Log.d("Update", "updating");

                // background work
//                Toast.makeText(ToolsActivity.this, "\nUpdating distributed version of database. \nPlease wait!\n", Toast.LENGTH_LONG).show();

                String internalDBPath = ToolsActivity.this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
                File internalDatabase = new File(internalDBPath);
                String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
                Utils.copyDatabaseFromAssets(ToolsActivity.this, assetsFilePath, internalDatabase);

                message = "\nDatabase Updated\n ";
                runOnUiThread(dialogMessages);
                Log.d("Update", "updated");

//                Toast.makeText(ToolsActivity.this, "\nDatabase updated\n", Toast.LENGTH_LONG).show();
                Flavoring.addflavoring(ToolsActivity.this); // sets database

                message = "All done";
                runOnUiThread(dialogMessages);
                Log.d("Update", "All done");
                progressDialog.dismiss();

            }
        }.start();
    }
    private void origupdateDB() {
//        boolean changeLexicon = false;
//        if (LexData.getLexName().equals("CSW19"))
//            changeLexicon = true;
        String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
        Toast.makeText(this, "\nUpdating distributed version of database. \nPlease wait!\n", Toast.LENGTH_LONG).show();
        File internalDatabase = new File(internalDBPath);
        String assetsFilePath = "databases" + File.separator + LexData.getDatabase();
        Utils.copyDatabaseFromAssets(this, assetsFilePath, internalDatabase);
        Toast.makeText(this, "\nDatabase updated\n", Toast.LENGTH_LONG).show();
        Flavoring.addflavoring(this); // sets database

//        if (changeLexicon) {
//            setLexicon("CSW22");
//        }
//            String lexiconName = shared.getString("lexicon","");
//            Log.d("Lexicon", "shared: " + lexiconName + "Lexdata: " + LexData.getLexName());
    }

    // Import Database
    private View.OnClickListener importDataBase = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            importDataBase();
//            CharSequence msg = "Importing database";
//            Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
        }
    };
    public void importDataBase() {
//        String DEST_PATH = this.getFilesDir().getAbsolutePath().replace("files", "databases")

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
//        intent.setType("*/*");
//        intent.putExtra(Intent.EXTRA_TITLE, filename);
        Log.d("import", "importDatabase");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
        startActivityForResult(intent, IMPORT_INTERNAL);
    }
    public void copyUriDatabaseToInternalThread(Context context, Uri input, String filename) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        new Thread() {
            public void run() {

                message = "Please Wait!\r\nImporting database ";
                runOnUiThread(dialogMessages);
                Log.d("Update", "updating");

                InputStream inputStream = null;
                File internalDatabase;

                String internalDBPath = LexData.internalFilesDir + File.separator + filename;
                ; // + File.separator + Flavoring.getflavoring();
                Log.d("import", "CogyUriDatabaseToInternal" + internalDBPath);

                Log.d("split", filename);
               internalDatabase = new File(internalDBPath);

// internalDatabase is the file
                // filename is the file
                int num=0;
                while (internalDatabase.exists()) {
                    num++;
                    String[] alt = filename.split("\\.(?=[^\\.]+$)");  // CHANGE
//                String[] alt = DB_NAME.split("\\.(?=[^\\.]+$)");
                    internalDatabase = new File(LexData.internalFilesDir + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
                }


                try {
                    inputStream = context.getContentResolver().openInputStream(input);
//            Log.e("CopyDB", inputStream.toString());
                    Utils.copyStreamTo(inputStream, internalDatabase);
                } catch (IOException e) {
                    //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
                } finally {
                    try {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    } catch (IOException e) {
                        Log.e("CopyDB", "Failed to close the destination");
                    }
                }
                message = "\nDatabase Imported\n ";
                runOnUiThread(dialogMessages);
                Log.d("Import", "imported");
                progressDialog.dismiss();


            }
        }.start();
    }

    // Copy Assets Database
    private View.OnClickListener copyAssetsDB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CharSequence msg = "Copying Assets database";
            Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
            if (permission()) copyAssetsDataBase();
        }
    };

    // Copy Assets Database to Documents
    public void copyAssetsDataBase() {
        String DB_NAME = Flavoring.getflavoring();
        String DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        if (Utils.usingSAF()) {
            Toast.makeText(this, "URI Copying database to " + DEST_PATH, Toast.LENGTH_LONG).show();
            copyUriFile(DB_NAME, COPY_ASSETS);
//            copyUriAssetsDatabase();
            Toast.makeText(this, "URI Copied database to " + DEST_PATH, Toast.LENGTH_LONG).show();
        } else {
            // setup input
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Please Wait...");
            progressDialog.show();

            new Thread() {
                public void run() {

                    message = "Please Wait!\r\nCopying database ";
                    runOnUiThread(dialogMessages);
                    Log.d("Copying", message);

                    String assetsFilePath = "databases" + File.separator + DB_NAME;

                    if (!isExternalStorageWritable()) {
                        message = "Access to external storage denied!\r\nCan't copy database now. ";
                        runOnUiThread(dialogMessages);
                        Log.d("Access Denied", message);
                        return;
                    }

                    boolean truth = true;

                   File directory = new File(DEST_PATH);
                    if (!directory.exists())
                        truth = directory.mkdirs();
                    if (!truth) {
                        message = "Can't create Destination folder!\r\nCan't copy database now. ";
                        runOnUiThread(dialogMessages);
                        Log.d("Access Denied", message);
                        Log.e("CopyDB", "Can't mkdirs()" + directory.getAbsolutePath());
                        return;
                    }
Log.d("SPLIT", DB_NAME);
                    File destination = new File(DEST_PATH + File.separator + DB_NAME);
                    int num = 0;
                    while (destination.exists()) {
                        num++;
                        String[] alt = DB_NAME.split("\\.(?=[^\\.]+$)");  // CHANG
//         nalDatabase = new File(LexData.internalFilesDir + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
                        destination = new File(DEST_PATH + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
                    }

                    Utils.copyDatabaseFromAssets(getApplicationContext(), assetsFilePath, destination);

                    message = "\nDatabase Copied\n ";
                    runOnUiThread(dialogMessages);
                    Log.d("Copied", message);
                    progressDialog.dismiss();

                }

            }.start();
        }
    }

    // Copy Internal Database
    private View.OnClickListener copyInternal = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            CharSequence msg = "Copying Internal database";
//            Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();

            if (permission()) copyInternalDataBase();
        }
    };
    public void copyInternalDataBase() {
        String DB_NAME = Flavoring.getflavoring();
//        String DB_NAME = LexData.getDatabase();
        String DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        if (Utils.usingSAF()) {
//            Toast.makeText(this, "URI Copying database to " + DEST_PATH, Toast.LENGTH_LONG).show();
            copyUriFile(DB_NAME, COPY_INTERNAL);
//            copyUriInternaDatabase();
//            Toast.makeText(this, "URI Copied database to " + DEST_PATH, Toast.LENGTH_LONG).show();
        } else {

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Please Wait...");
            progressDialog.show();

            new Thread() {
                public void run() {

                    message = "Please Wait!\r\nCopying database ";
                    runOnUiThread(dialogMessages);
                    Log.d("Copying", message);

                    if (!isExternalStorageWritable()) {
//                Toast.makeText(this, "Can't write to external storage", Toast.LENGTH_LONG).show();
                        message = "Access to external storage denied!\r\nCan't copy database now. ";
                        runOnUiThread(dialogMessages);
                        Log.d("Access Denied", message);
                        return;
                    }

                    boolean truth = true;
                    File directory = new File(DEST_PATH);
                    if (!directory.exists())
                        truth = directory.mkdirs();
                    if (!truth) {
                        message = "Can't create Destination folder!\r\nCan't copy database now. ";
                        runOnUiThread(dialogMessages);
                        Log.d("Access Denied", message);
                        Log.e("CopyDB", "Can't mkdirs()" + directory.getAbsolutePath());
                        return;
                    }
                    Log.d("SPLIT", DB_NAME);

                    File destination = new File(DEST_PATH + File.separator + DB_NAME);
                    int num = 0;
                    while (destination.exists()) {
                        num++;
                        String[] alt = DB_NAME.split("\\.(?=[^\\.]+$)");  // CHANGE
                        destination = new File(DEST_PATH + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
                    }

                    Utils.copyInternalDatabase(getApplicationContext(), internalDBPath, destination);

                    message = "\nDatabase Copied\n ";
                    runOnUiThread(dialogMessages);
                    Log.d("Copied", message);
                    progressDialog.dismiss();

                }
            }.start();
        }

    }

    // Backup Database
    private View.OnClickListener backupDB = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (permission()) BackupDataBase();
        }
    };
    public void BackupDataBase() {
        String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();
        Log.d("backup", internalDBPath);
        new Thread() {
            public void run() {

                // Delete lexicon
                message = "Please Wait!\r\nBacking up database";
                runOnUiThread(dialogMessages);

                Utils.BackupDatabase(getApplicationContext(), internalDBPath);
                progressDialog.dismiss();
            }
        }.start();
        Toast.makeText(this, "Database backed up to " + internalDBPath, Toast.LENGTH_LONG).show();
    }

    // Compact Database
    private View.OnClickListener compactDatabase = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            compactDatabaseAlert();
            CharSequence msg = "Compacting database";
            Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
        }
    };
    private boolean compactDatabaseAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Compact Database");

        String msg = "Are you sure you want to compact the database? \nThis may take several minutes... ";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Compact Now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                compactDatabaseThread();
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
    private void compactDatabaseThread() {
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

//        ProgressBar progressBar = new ProgressBar(this);
//        progressBar.start
//        CharSequence msg = "Compacting Database";
//        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();

        new Thread() {
            public void run() {

                // Delete lexicon
                message = "Please Wait!\r\nCompacting Database ";
                runOnUiThread(dialogMessages);

                String sql = "Vacuum;";
                Log.i("Compacting", "compacting");
                databaseAccess.execSQL(sql);

                message = "All done";
                runOnUiThread(dialogMessages);
                progressDialog.dismiss();
            }
        }.start();
//        msg = "All Done. \nDatabase should be smaller now";
//        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
    }



    ////////// FILE MANAGEMENT //////////
    // Copy Selected Database
    private View.OnClickListener copySelected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            CharSequence msg = "Copying Selected database";
            Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();

            if (permission()) copySelectedFile();
        }
    };
    public void copySelectedFileThread(String Selected) {
        String DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        if (Utils.usingSAF()) {
//            Toast.makeText(this, "URI Copying database to " + DEST_PATH, Toast.LENGTH_LONG).show();
            copyUriFile(Selected, COPY_SELECTED);
//            copyUriInternaDatabase();
//            Toast.makeText(this, "URI Copied database to " + DEST_PATH, Toast.LENGTH_LONG).show();
        } else {

            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Please Wait...");
            progressDialog.show();

            new Thread() {
                public void run() {

                    message = "Please Wait!\r\nCopying file " + Selected;
                    runOnUiThread(dialogMessages);
                    Log.d("Copying", message);

                    if (!isExternalStorageWritable()) {
//                Toast.makeText(this, "Can't write to external storage", Toast.LENGTH_LONG).show();
                        message = "Access to external storage denied!\r\nCan't copy database now. ";
                        runOnUiThread(dialogMessages);
                        Log.d("Access Denied", message);
                        return;
                    }

                    boolean truth = true;
                    File directory = new File(DEST_PATH);
                    if (!directory.exists())
                        truth = directory.mkdirs();
                    if (!truth) {
                        message = "Can't create Destination folder!\r\nCan't copy file now. ";
                        runOnUiThread(dialogMessages);
                        Log.d("Access Denied", message);
                        Log.e("CopyDB", "Can't mkdirs()" + directory.getAbsolutePath());
                        return;
                    }

                    File destination = new File(DEST_PATH + File.separator + Selected);
                    int num = 0;
                    while (destination.exists()) {
                        num++;
                        String[] alt = Selected.split("\\.(?=[^\\.]+$)");  // CHANGE
                        destination = new File(DEST_PATH + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
                    }

                    Utils.copyInternalDatabase(getApplicationContext(), internalDBPath, destination);

                    message = "\nDatabase Copied\n ";
                    runOnUiThread(dialogMessages);
                    Log.d("Copied", message);
                    progressDialog.dismiss();

                }
            }.start();
        }

    }
    public void copySelectedFile() {
        String location = this.getFilesDir().getAbsolutePath().replace("files", "");

        File file = new File(location);

        FileDialog fileDialog = new FileDialog(this, file, "*");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                Log.d("Name", file.getName());
                Log.d("path", file.getAbsolutePath());
                // source is in selectedDBPath, file.getName is for supp
                selectedDBPath = file.getAbsolutePath();

                copySelectedFileThread(file.getName());
            }
        });
        fileDialog.showDialog();
    }

    // Delete Database
    private View.OnClickListener deleteInternalFiles = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            deleteInternalFiles();
        }
    };
    private boolean deleteAlert(File file) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Delete Database");

        String msg = "Are you sure you want to delete " + file.getName() + "?";
        builder.setMessage(msg);

        builder.setPositiveButton("Yes. Delete Now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                CharSequence msg = "Deleting database";
//                Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
                deleteFile("filename");
//                        ??myContext.deleteFile(fileName);
                file.delete();
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
    public void deleteInternalFiles() {
        String location;
        if (Utils.usingSAF())
            location = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        else
            location = DIRECTORY_DOCUMENTS;

        File file = new File(location);

        FileDialog fileDialog;
//        if (LexData.getUsername().equals("Testing")) {
        fileDialog = new FileDialog(this, file, "*");
//        } else {
//            fileDialog = new FileDialog(this, file, "db3");
//        }
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                String full = file.getAbsolutePath();
//                if (!file.getName().equals (Flavoring.getFlavor())
                deleteAlert(file);
                //                  LexData.setDatabase(getActivity().getApplicationContext(),file.getName());
                //                LexData.setDatabasePath(getActivity().getApplicationContext(),full.substring(0,full.lastIndexOf(File.separator)));
            }
        });
        fileDialog.showDialog();
    }



    ////////// Generic methods //////////
    // URI database copying
    public void copyUriFile(String filename, int source) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
        startActivityForResult(intent, source);
    }

    // Generic copy to Documents (URI) from onActivityResult
    public void copyUriThread(InputStream in, Uri destinationUri) {
//////// Only called if SAF

        //        public void copyUriFile(Uri SourceUri, Uri destinationUri) {

//    DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
//    String extension = inputFile.substring(inputFile.lastIndexOf(".") + 1);
//    DocumentFile newFile = pickedDir.createFile("*/" + extension, inputFile);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

        new Thread() {
            public void run() {

                message = "Please Wait!\r\nCopying database ";
                runOnUiThread(dialogMessages);
                Log.d("Copying", message);


                try {
//            in = this.getContentResolver().openInputStream(SourceUri);

                    OutputStream out = null;
                    out = getContentResolver().openOutputStream(destinationUri);

                    byte[] buffer = new byte[1024];
                    int read;
                    Log.d("destination", destinationUri.toString());

                    try {
                        while ((in.read(buffer)) > 0) {
                            if (out != null) {
                                out.write(buffer);
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    if (out != null) {
                        out.flush();

                        out.close();
                    }
                    in.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                message = "\nDatabase Copied\n ";
                runOnUiThread(dialogMessages);
                Log.d("Copied", message);
                progressDialog.dismiss();

            }
        }.start();

    }



    ////////// STORAGE ACCESS FRAMEWORK //////////
    private static final int CREATE_FILE = 1;
    private static final int PICK_TXT_FILE = 2;
    private static final int COPY_DB = 3;
    private static final int COPY_INTERNAL = 4;
    private static final int COPY_ASSETS = 5;
    private static final int COPY_SELECTED = 15;
    private static final int BACKUP_INTERNAL = 6;
    private static final int IMPORT_INTERNAL = 7;
    private static final int DELETE_DATABASE = 8;
    private static final int REQUEST_FOLDER = 9;
    private static final int EXPORT_LIST = 11;
    private static final int IMPORT_CARDBOX = 12;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        // The result data contains a URI for the document or directory that
        // the user selected.

        if (resultData != null) {

            if (requestCode == COPY_INTERNAL
                    && resultCode == Activity.RESULT_OK) {
                Log.d("uri", internalDBPath);
                Uri destinationUri = resultData.getData();

                InputStream source;  // 2nd line
                try {
                    source = new FileInputStream(internalDBPath);
                } catch (FileNotFoundException e) {
                    String message = internalDBPath + " doesn't exist.";
                    Toast.makeText(this, message , Toast.LENGTH_LONG).show();
                    Log.d("uri", message);
                    return;
                }
                copyUriThread(source, destinationUri);
                return;
            }

            if (requestCode == COPY_ASSETS
                    && resultCode == Activity.RESULT_OK) {
                String assetsFilePath = "databases" + File.separator+ DB_NAME;
                Log.d("uri", assetsFilePath);
                Uri destinationUri = resultData.getData();

                InputStream source;
                try {
                    source = getApplicationContext().getAssets().open(assetsFilePath);
                } catch (IOException e) {
                    String message = assetsFilePath + " doesn't exist.";
                    Toast.makeText(this, message , Toast.LENGTH_LONG).show();
                    Log.d("uri", message);
                    return;
                }

                copyUriThread(source, destinationUri);
                return;
            }

            if (requestCode == COPY_SELECTED
                    && resultCode == Activity.RESULT_OK) {
                Log.d("uri", selectedDBPath);
                Uri destinationUri = resultData.getData();

                InputStream source;  // 2nd line
                try {
                    source = new FileInputStream(selectedDBPath);
                } catch (FileNotFoundException e) {
                    String message = selectedDBPath + "source doesn't exist.";
                    Toast.makeText(this, message , Toast.LENGTH_LONG).show();
                    Log.d("uri", message);
                    return;
                }
                copyUriThread(source, destinationUri);
                return;
            }

            if (requestCode == IMPORT_INTERNAL
                    && resultCode == Activity.RESULT_OK) {

                Uri source = resultData.getData(); // path selected
                String filename = getUriFileName(source);
Log.d("import", "path: " + internalDBPath);

//                Utils.copyUriDatabaseToInternal(this, source, filename);
                copyUriDatabaseToInternalThread(this, source, filename);
            }

            if (requestCode == IMPORT_CARDBOX
                    && resultCode == Activity.RESULT_OK) {

                Uri source = resultData.getData(); // path selected
                String filename = getUriFileName(source);
                Log.d("import", "path: " + getInternalCardboxPath);

//                Utils.copyUriDatabaseToInternal(this, source, filename);
                copyUriCardboxToInternalThread(this, source, filename);
            }

            if (requestCode == EXPORT_LIST
                    && resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    Utils.exportList(this, ExportListName, resultData.getData());

//                writeList(resultData.getData());
                    // Perform operations on the document using its URI.


                }
            }
        }
    }






    //////// UTILITIES //////////
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{WRITE_EXTERNAL_STORAGE}, 1);
            if (Environment.MEDIA_MOUNTED.equals(state))
                return true;
        }
        return false;
    }
    private boolean permission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            int okay = 1;
            ActivityCompat.requestPermissions(this,
                    new String[]{WRITE_EXTERNAL_STORAGE},
                    okay);

            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
                return true;

        } else {
            return true;
            // Permission has already been granted
        }
        return false;
    }
    @SuppressLint("Range")
    public String getUriFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }



    ///// Unused
    ////////// OLD TOOLS //////////
    public void openLookup(View view) {
        Intent intent = new Intent(this, LookupActivity.class);
        startActivity(intent);
    }
    public void openJudge(View view) {
        Intent intent = new Intent(this, WordJudgeActivity.class);
        startActivity(intent);
    }
    public void tileTracker(View view) {
        Intent intent = new Intent(this, TileTrackerActivity.class);
        startActivity(intent);
    }
    public void clock(View view) {
        Intent intent = new Intent(this, ClockActivity.class);
        startActivity(intent);
    }
    public void doMulti(View view) {
        Intent intent = new Intent(this, MultiSearchActivity.class);
        startActivity(intent);
    }

    ///////// Special
    @SuppressLint("Range")
    public void selectLexicon(){
        // only called from Select options, not when loading
        otherDatabase = DatabaseAccess.getInstance(getApplicationContext() ,
                otherDBPath, otherDBFile);

        ArrayList <Structures.Lexicon> lexiconList = new ArrayList<>();
        otherDatabase.open();
        Cursor cursor = otherDatabase.rawQuery("SELECT * FROM tblLexicons ORDER BY LexiconName");

        while (cursor.moveToNext()) {
            Structures.Lexicon lexicon = new Structures.Lexicon();
            lexicon.LexiconID = cursor.getInt(cursor.getColumnIndex("LexiconID"));
            lexicon.LexiconName = cursor.getString(cursor.getColumnIndex("LexiconName"));
            lexicon.LexiconSource = cursor.getString(cursor.getColumnIndex("LexiconSource"));
            lexicon.LexiconStuff = cursor.getString(cursor.getColumnIndex("LexiconStuff"));
            lexicon.LexiconNotice = cursor.getString(cursor.getColumnIndex("LexiconNotice"));
            lexicon.LexLanguage = cursor.getString(cursor.getColumnIndex("LexLanguage"));
            lexiconList.add(lexicon);
        }
        cursor.close();
        otherDatabase.close();
    }
    public void selectDatabase() {
        if (!permission()) // requests permission
            if (ContextCompat.checkSelfPermission(this,
                    WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) // tests permission
                return;


        File mPath;
        if(Utils.usingSAF()) {
            String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
            mPath = new File(internalDBPath);
        }
        else {
            mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
        }
        Log.d("mPath", mPath.getPath());

//        mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
        FileDialog fileDialog = new FileDialog(this, mPath, "db3");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                String full = file.getAbsolutePath();
                otherDBFile = file.getName();
                otherDBPath = full.substring(0,full.lastIndexOf(File.separator));
            }
        });
        fileDialog.showDialog();
    }
    private boolean swappable(List<String> list, String word, String prospect) {
        List<String> set = new ArrayList<>();
        set.add(word);
        set.add(prospect);

        for (int t = 0; t < 4; t++) {
            StringBuilder sb = new StringBuilder(word);
            StringBuilder sp = new StringBuilder(prospect);
            if (sb.charAt(t) == sp.charAt(t))
                return false;

            sb.setCharAt(t, prospect.charAt(t));
            sp.setCharAt(t, word.charAt(t));
            if (!list.contains(sb.toString()))
                return false;
            set.add(sb.toString());
            if (!list.contains(sp.toString()))
                return false;
            set.add(sp.toString());

            if (t > 0) {
                sb = new StringBuilder(word);
                sp = new StringBuilder(prospect);

                sb.setCharAt(0, prospect.charAt(0));
                sb.setCharAt(t, prospect.charAt(t));
                sp.setCharAt(0, word.charAt(0));
                sp.setCharAt(t, word.charAt(t));
                if (!list.contains(sb.toString()))
                    return false;
                set.add(sb.toString());
                if (!list.contains(sp.toString()))
                    return false;
                set.add(sp.toString());
            }
        }
        StringBuilder pr = new StringBuilder();
        for(int p=0; p < 16; p++) {
            pr.append(set.get(p) + " ");
        }
        Log.e("Results",pr.toString());

        return true;

    }
    public void noBA(View view) {


        List<String> list = new ArrayList<>();
        databaseAccess.open();
        Cursor cursor = databaseAccess.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Length(Word) = 7" +
                " AND Trim(FrontHooks) = '' AND Trim(BackHooks) = '') ;");

        while (cursor.moveToNext()) {
            String addition = cursor.getString(0);
            list.add(addition);
//            Log.e("A", addition);
        }
        cursor.close();
        Log.e("Count", Integer.toString(list.size()));

        int noBAcounter = 0;
        Cursor checker;
        for (int n = 0; n < list.size(); n++) {
            String word = list.get(n);
            checker = databaseAccess.getCursor_blankAnagrams(word + "?", "", "", 0, 0, false);
            if (checker.getCount() == 0) {
                noBAcounter++;
                Log.e("Found", Integer.toString(noBAcounter) + ":" + word);

                anal.append(word + " ");

//                if (noBAcounter > 10)
//                    return;
            }
        }
    }
    public void addAlphagrams(View view ) {
        databaseAccess.addAlphagramsThread(this);
    }
    public void goEncryption(View view) {
        Intent intent = new Intent(this, EncryptActivity.class);
        startActivity(intent);
    }

    public void copyUriFile(InputStream in, Uri destinationUri) {
//        public void copyUriFile(Uri SourceUri, Uri destinationUri) {

//    DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
//    String extension = inputFile.substring(inputFile.lastIndexOf(".") + 1);
//    DocumentFile newFile = pickedDir.createFile("*/" + extension, inputFile);

        try {
//            in = this.getContentResolver().openInputStream(SourceUri);

            OutputStream out = null;
            out = this.getContentResolver().openOutputStream(destinationUri);

            byte[] buffer = new byte[1024];
            int read;
            Log.d("uri", "copy");

            try {
                while ((in.read(buffer)) > 0) {
                    if (out != null) {
                        out.write(buffer);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (out != null) {
                out.flush();

                out.close();
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void copyUriFile(Uri sourceUri, OutputStream out) {
//        public void copyUriFile(Uri SourceUri, Uri destinationUri) {

//    DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
//    String extension = inputFile.substring(inputFile.lastIndexOf(".") + 1);
//    DocumentFile newFile = pickedDir.createFile("*/" + extension, inputFile);

        try {
//            in = this.getContentResolver().openInputStream(SourceUri);

            InputStream in = null;
            in = this.getContentResolver().openInputStream(sourceUri);

            byte[] buffer = new byte[1024];
            int read;
            Log.d("uri", "copy");

            try {
                while ((in.read(buffer)) > 0) {
                    if (out != null) {
                        out.write(buffer);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (out != null) {
                out.flush();

                out.close();
            }
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void exportListThread(String list) {
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please Wait...");
        progressDialog.show();

//        ProgressBar progressBar = new ProgressBar(this);
//        progressBar.start
        CharSequence msg = "Deleting list";
        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();

        new Thread() {
            public void run() {

                // Delete list items
                message = "Please Wait!\r\nDeleting subject list entries for " + list;
                runOnUiThread(dialogMessages);

                String sql = "DELETE FROM WordList WHERE ListID = '" + databaseAccess.getListID(list) + "'; ";
                Log.d("Delete Entry", sql);
                databaseAccess.execSQL(sql);


                // Delete list entry
                message = "Please Wait!\r\nDeleting subject list " + list;
                runOnUiThread(dialogMessages);

                sql = "DELETE FROM WordLists WHERE ListName = '" + list + "'; ";
                Log.d("Delete Entry", sql);
                databaseAccess.execSQL(sql);

// todo move to separate tool
//                // Compacting
//                message = "Please Wait!\r\nCompacting";
//                runOnUiThread(dialogMessages);
//
//                sql = "Vacuum;";
//                Log.i("Compacting", lexicon);
//                databaseAccess.execSQL(sql);

                message = "All done";
                runOnUiThread(dialogMessages);
                progressDialog.dismiss();
            }
        }.start();
        msg = "Deleted list " + list;
        Toast.makeText(ToolsActivity.this, msg , Toast.LENGTH_LONG).show();
    }
    public void b4copyInternalDataBase() {
        String DB_NAME = Flavoring.getflavoring();
//        String DB_NAME = LexData.getDatabase();
        String DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        if (Utils.usingSAF()) {
            Toast.makeText(this, "URI Copying database to " + DEST_PATH, Toast.LENGTH_LONG).show();
            copyUriFile(DB_NAME, COPY_INTERNAL);
//            copyUriInternaDatabase();
            Toast.makeText(this, "URI Copied database to " + DEST_PATH, Toast.LENGTH_LONG).show();
        } else {

            // setup input
//            String internalDBPath = this.getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();

            if (!isExternalStorageWritable()) {
                Toast.makeText(this, "Can't write to external storage", Toast.LENGTH_LONG).show();
                return;
            }

            // setup output handle
            boolean truth = true;
            File directory = new File(DEST_PATH);
            if (!directory.exists())
                truth = directory.mkdirs();

            if (!truth)
                Log.e("CopyDB", "Can't mkdirs()" + directory.getAbsolutePath());

            String[] nameParts = DB_NAME.split("\\.(?=[^\\.]+$)");  // ADD



            File destination = new File(DEST_PATH + File.separator + DB_NAME);
            int num = 0;
            while (destination.exists()) {
                num++;
                String[] alt = DB_NAME.split("\\.(?=[^\\.]+$)");  // CHANGE
//                String[] alt = DB_NAME.split("\\.(?=[^\\.]+$)");
                destination = new File(DEST_PATH + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
            }

            Toast.makeText(this, "Copying database to " + destination.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Utils.copyInternalDatabase(this, internalDBPath, destination);
            Toast.makeText(this, "Copied database to " + destination.getAbsolutePath(), Toast.LENGTH_LONG).show();

        }

    }
    public void importFromDB(String dbPath) {

        selectDatabase();

        otherDatabase = DatabaseAccess.getInstance(getApplicationContext() ,
                otherDBPath, otherDBFile);

        if (!otherDatabase.isValidDatabase())
            Toast.makeText(this, otherDBFile + " is not a valid Hoot database. " , Toast.LENGTH_LONG).show();
        else
            selectLexicon();

    }
    private String copyStreamTo(InputStream inputStream, String inputFile, Uri treeUri) {
        //    private static void copyStreamTo(InputStream inputStream, uri destination) {
        // copyFile
//        https://stackoverflow.com/questions/36023334/android-how-to-use-new-storage-access-framework-to-copy-files-to-external-sd-c#43051555
//        InputStream inputStream = null;
        OutputStream out = null;
        String error = null;
        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
        String extension = inputFile.substring(inputFile.lastIndexOf(".") + 1);

        try {
            assert pickedDir != null;
            DocumentFile newFile = pickedDir.createFile("audio/" + extension, inputFile);
            assert newFile != null;
            out = this.getContentResolver().openOutputStream(newFile.getUri());
//            inputStream = new FileInputStream(inputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                assert out != null;
                out.write(buffer, 0, read);
            }
            inputStream.close();
            // write the output file (You have now copied the file)
            assert out != null;
            out.flush();
            out.close();

        } catch (Exception e) {
            error = e.getMessage();
        }
        return error;
    }
    public void findmatches(View view ) {
        int matchcounter = 0;
        List<String> list = new ArrayList<>();
        databaseAccess.open();
        Cursor cursor = databaseAccess.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Length(Word) = 4) ;");

        while(cursor.moveToNext())
        {
            String addition = cursor.getString(0);
            list.add(addition);
//            Log.e("A", addition);
        }
        cursor.close();

        // for each word in list


        List<String> prospects = new ArrayList<>(list);

        Log.e("StartCount", Integer.toString(list.size()));

        matchcounter = 0;

        for (Iterator<String> iterator = list.iterator(); iterator.hasNext(); ) {
//        for (int n = 0; n < list.size(); n++) {
//            String word = list.get(n);
            String word = iterator.next();
//            Log.d("Checking", word);
            for (int p = 0; p < prospects.size(); p++) {
                String pr = prospects.get(p);
                if (swappable(list, word, pr)) {
                    Log.e("Found", word + ":" + pr);
                    anal.append(word + "-" + pr);
                }
                else {
//                    Log.d("Removed", word);
//                    iterator.remove();
                }
            }

        }
        Log.e("EndCount", Integer.toString(list.size()));
        databaseAccess.close();


    }
    private void importDBLexicon(){

    }

}