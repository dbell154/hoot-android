package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ImportLexiconActivity extends AppCompatActivity {
    Spinner languages, tilesets;
    Button search, importlex;
    TextView importfile, lexName, lexSource, lexDesc, lexCopyright;
    DatabaseAccess databaseAccess;
    ArrayAdapter<String> dataAdapter;
    ArrayAdapter<String> tiledataAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dialog doesn't show text
        //        Utils.setStartTheme(this);


        setContentView(R.layout.activity_import_lexicon);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        languages = findViewById(R.id.lexLanguage);
        dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinsmall, LexData.languages);
        languages.setAdapter(dataAdapter);
//        languages.setOnItemSelectedListener(selection);

        tilesets = findViewById(R.id.lexTileSet);
        tiledataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinsmall, LexData.tileset);
        tilesets.setAdapter(tiledataAdapter);

        search = (Button) findViewById(R.id.Search);
        search.setOnClickListener(findFile);

        importlex = (Button) findViewById(R.id.beginImport);
        importlex.setOnClickListener(importLexicon);

        importfile = findViewById(R.id.lexFile);
        lexName = findViewById(R.id.lexName);
        lexName.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        lexSource = findViewById(R.id.lexSource);
        lexSource.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        lexCopyright = findViewById(R.id.lexNotice);
        lexDesc = findViewById(R.id.lexDescription);

        databaseAccess = DatabaseAccess.getInstance(getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());

        setRotation();
    }
    private View.OnClickListener findFile = new View.OnClickListener() {
        public void onClick(View v) {
            selectFile();
        }
    };
    private View.OnClickListener importLexicon = new View.OnClickListener() {
        public void onClick(View v) {
//            getLexiconFromFile(importfile.getText().toString());
            getLexiconFromFile(importfile.getPrivateImeOptions().toString());
        }
    };


    private static final int PICK_TXT_FILE = 2;
    public void selectFile() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
            startActivityForResult(intent, PICK_TXT_FILE);

        } else {

            if (!databaseAccess.permission(this))
                return;
            File mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
            FileDialog fileDialog = new FileDialog(this, mPath, "txt");
            // only supports one file extension

            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    String full = file.getAbsolutePath();
                    importfile.setText(full);
                    importfile.setPrivateImeOptions(full);
                    try {
                        getLexiconDetailsFromFile(full);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            fileDialog.showDialog();
        }
    }

    public void getLexiconDetailsFromFile(String file) throws IOException {

        // Check source file for specifications first
//        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context , LexData.getDatabasePath(), LexData.getDatabase());
        BufferedReader sr = null;
//        InputStream inputStream;

        String line;
        String entry = "";
        String keyword = "";
//        Log.i("Key:Entry",  keyword + ":" + entry);


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {

            Uri uri = Uri.parse(file);
            // SAF
            try {
                InputStream inputStream =
                        this.getContentResolver().openInputStream(uri);
                sr = new BufferedReader(
                        //                         BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Objects.requireNonNull(inputStream)));
            } catch (FileNotFoundException e) {
                // No content provider: content:/com.android.externalstorage.documents/document/home%3Aa24.txt
                throw new RuntimeException(e);
            }
        }
        else
            try {
                sr = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("Error",  "catch bufferedreader");
            }


        do {
            try {
                line = sr.readLine();
//                Log.i("Import", "read " + line);
            } catch (IOException oops) {
                Log.e("Error", "readline");
                break;
            }


            // line = StringUtils.RemoveDiacritics(line);
//            line = Normalizer.normalize(line, Normalizer.Form.NFD);
//            line = line.replaceAll("[^\\p{ASCII}]", "");
//
//            if (line.charAt(0) == '/')
//                continue;
//            if (!Character.isLetter(line.charAt(0))) // this handles all lines not beginning with a character, including comments.
//                continue;

            if (line.charAt(0) != '/') {
                String column[] = line.split("\t");

                // check number of columns !!!!!!!!!!!
                if (column.length < 9) {
                    Toast.makeText(this, "This is not a valid Hoot version 3 lexicon.", Toast.LENGTH_LONG).show();
                    return;
                }
            }



            line = line.trim(); // leading spaces okay in file
            if (line.length() == 0) {
//                Log.i("Import", "empty");
                continue;
            }
            if (!(line.substring(0, 2).equals("//"))) {
//                Log.i("Import", "not comment");
                break;
            }
            int sep = line.indexOf(':');
            if (sep == -1) {
//                Log.i("Import", "no colon");
                continue;
            }

//            Log.i("Key:Entry",  keyword + ":" + entry);

            // int space = line.IndexOf(' '); //
            entry = line.substring(sep + 1); // entry
            entry = entry.trim();
            keyword = line.substring(2, sep); // keyword
            keyword = keyword.trim();
//                Log.i("Key:Entry",  keyword + ":" + entry);
            switch (keyword)
            {
                case "Lexicon Name":
                    if (TextUtils.isEmpty(lexName.getText().toString().trim()))
                    {
                        lexName.setText(entry);
                    }
                    break;
                case "Lexicon Source":
                    if (TextUtils.isEmpty(lexSource.getText().toString().trim()))
                        lexSource.setText(entry);
//                    Log.i("Lex", "Name: " + lexSource.getText().toString().trim());
                    break;
                case "Lexicon Notice":
                    if (TextUtils.isEmpty(lexCopyright.getText().toString().trim()))
                        lexCopyright.setText(entry);
                    break;
                case "Lexicon Description":
                    if (TextUtils.isEmpty(lexDesc.getText().toString().trim()))
                        lexDesc.setText(entry);
                    break;
                case "Language":
                    //if (languages.getSelectedItem() == null)
                    languages.setSelection(dataAdapter.getPosition(entry));
                    break;
            }

        } while (!Character.isLetter(line.charAt(0)));

        if (databaseAccess.lexiconExists(lexName.getText().toString().trim())) {
            Toast.makeText(this, "Lexicon " + lexName.getText().toString() + " already exists in current database", Toast.LENGTH_LONG).show();
            return;
        }
        sr.close();

        // Now attempt to import
// Text: The imported lexicon will only work in the app, to import a lexicon for PC use, import in the PC and configure for lexicon use

    }
    public void getLexiconFromFile(String file) {

        // File or URi ???



        // Check source file for specifications first
//        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context , LexData.getDatabasePath(), LexData.getDatabase());
        BufferedReader sr = null;
        InputStream inputStream;

        String line;
        String entry = "";
        String keyword = "";
//        Log.i("Key:Entry",  keyword + ":" + entry);


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {

            Uri uri = Uri.parse(file);
            // SAF
            try {
                inputStream =
                        this.getContentResolver().openInputStream(uri);
                sr = new BufferedReader(
                        //                         BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Objects.requireNonNull(inputStream)));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        else
            try {
                sr = new BufferedReader(new FileReader(file));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("Error",  "catch bufferedreader");
            }




        do {
            try {
                line = sr.readLine();
//                Log.i("Import", "read " + line);
            } catch (IOException oops) {
                Log.e("Error", "readline");
                break;
            }

            line = line.trim(); // leading spaces okay in file
            if (line.length() == 0) {
//                Log.i("Import", "empty");
                continue;
            }
            if (!(line.substring(0, 2).equals("//"))) {
//                Log.i("Import", "not comment");
                continue;
            }
            int sep = line.indexOf(':');
            if (sep == -1) {
//                Log.i("Import", "no colon");
                continue;
            }

//            Log.i("Key:Entry",  keyword + ":" + entry);

            // int space = line.IndexOf(' '); //
            entry = line.substring(sep + 1); // entry
            entry = entry.trim();
            keyword = line.substring(2, sep); // keyword
            keyword = keyword.trim();
//                Log.i("Key:Entry",  keyword + ":" + entry);
            switch (keyword)
            {
                case "Lexicon Name":
                    if (TextUtils.isEmpty(lexName.getText().toString().trim()))
                    {
                        lexName.setText(entry);
                    }
                    break;
                case "Lexicon Source":
                    if (TextUtils.isEmpty(lexSource.getText().toString().trim()))
                        lexSource.setText(entry);
//                    Log.i("Lex", "Name: " + lexSource.getText().toString().trim());
                    break;
                case "Lexicon Notice":
                    if (TextUtils.isEmpty(lexCopyright.getText().toString().trim()))
                        lexCopyright.setText(entry);
                    break;
                case "Lexicon Description":
                    if (TextUtils.isEmpty(lexDesc.getText().toString().trim()))
                        lexDesc.setText(entry);
                    break;
                case "Language":
                    //if (languages.getSelectedItem() == null)
                    languages.setSelection(dataAdapter.getPosition(entry));
                    break;
            }

        } while (!Character.isLetter(line.charAt(0)));

        if (TextUtils.isEmpty(lexName.getText().toString().trim())) {
            Toast.makeText(this, "Lexicon must have a name", Toast.LENGTH_LONG).show();
            return;
        }
        if (databaseAccess.lexiconExists(lexName.getText().toString().trim())) {
            Toast.makeText(this, "Lexicon " + lexName.getText().toString() + " already exists in current database", Toast.LENGTH_LONG).show();
            return;
        }

        // Now attempt to import
        Toast.makeText(this, "Begin Import of  " + lexName.getText().toString() + " ", Toast.LENGTH_LONG).show();
        // create lexicon class
        Structures.Lexicon lexicon = new Structures.Lexicon(lexName.getText().toString(),
                lexSource.getText().toString(), lexDesc.getText().toString(), lexCopyright.getText().toString(), languages.getSelectedItem().toString());
        try {
            databaseAccess.ImportLexiconThread(this, lexicon, tilesets.getSelectedItem().toString(), file);
        }
        catch (SQLiteException e) {
            e.printStackTrace();
        }
// Text: The imported lexicon will only work in the app, to import a lexicon for PC use, import in the PC and configure for lexicon use
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        // This only gets the filename

        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == PICK_TXT_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                // Perform operations on the document using its URI.
                Log.d("fs", String.valueOf(Uri.parse(uri.toString())));
                File fn = new File("" + Uri.parse(uri.toString()));
                Log.d("fp", fn.getPath());
                Log.d("fn", fn.getName());

                importfile.setText(Uri.decode(fn.getAbsolutePath()));
                importfile.setPrivateImeOptions(uri.toString());
                try {
                    getLexiconDetailsFromFile(uri.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }
    }

//    ProgressDialog progressDialog;
//    public String message = "Please Wait!\r\nProcessing...";
//    private Runnable dialogMessages = new Runnable() {
//        @Override
//        public void run() {
//            progressDialog.setMessage(message);
//
//        }
//    };
//
//    public boolean ImportLexiconThread(final Context context, final Structures.Lexicon lexicon, final String tileset, final String filespec) {
//        // not found, configuring lexicon (background), try to select later
//        // at end, lexicon created, you can now select the database/lexicon
//        Log.e("TileSet", tileset);
//
//        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
//            String LexTable = lexicon.LexiconName;
//            String TempTable = "IMPORTER", line, sqlcmd;
//            ArrayList<String> importList = new ArrayList<>();
//            NotificationCompat.Builder builder;
//            int rowCount = 0;
//
//            @Override
//            protected void onPreExecute() {
//                // Special Intents used for Notifications
//                /*Intent intent = new Intent(context, AltSearchActivity.class );
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);*/
//
//                // regular intent
//                // Create an Intent for the activity you want to start
//                Intent intent = new Intent(context, SearchActivity.class);
//                // Create the TaskStackBuilder and add the intent, which inflates the back stack
//                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//                stackBuilder.addNextIntentWithParentStack(intent);
//                // Get the PendingIntent containing the entire back stack
//                PendingIntent pendingIntent =
//                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
//
//                // Create an explicit intent for an Activity in your app
//                builder = new NotificationCompat.Builder(context, "Hoot")
//                        .setSmallIcon(R.mipmap.howl)
//                        .setContentTitle("Lexicon " + lexicon.LexiconName + " imported")
//                        .setContentText("Go to Settings to select this new lexicon")
//                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                        // Set the intent that will fire when the user taps the notification
//                        .setContentIntent(pendingIntent)
//                        .setAutoCancel(true);
//
//                progressDialog = new ProgressDialog(context);
//                progressDialog.setMessage("Please Wait!\r\nImporting lexicon...");
//                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
//                progressDialog.setCancelable(true);
//                progressDialog.setCanceledOnTouchOutside(false);
//                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//                    @Override
//                    public void onCancel(DialogInterface dialog) {
//                        cancel(true);
//                    }
//                });
//                progressDialog.show();
//
//            }
//
//            @Override
//            protected Void doInBackground(Void... params) {
//                // todo check if cancelled between sections
//                final DatabaseAccess database = DatabaseAccess.getInstance(context, LexData.getDatabasePath(), LexData.getDatabase());
////                databaseAccess.open();
//
//                // CREATE TEMPORARY TABLE
//                database.execSQL("DROP TABLE IF EXISTS `" + TempTable + "`");
//                String tblSql = "CREATE TABLE `" + TempTable + "` (" +
//                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
//                        " InnerFront NUM, InnerBack NUM, " +
//                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
//                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, Anagrams INTEGER NOT NULL DEFAULT 1, Score INTEGER NOT NULL DEFAULT 0);";
//                try {
//                    database.execSQL(tblSql);
//                } catch (Exception e) {
//                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
//                }
//                try {
//                    database.execSQL("CREATE INDEX WordIndex ON `" + TempTable + "` ( Word COLLATE NOCASE);");
//                } catch (Exception e) {
//                    // ignore
//                }
//                Log.i("IMPORT", "Created Temp Table");
//
//
//                // IMPORT FROM TEXT FILE INTO TEMPORARY TABLE
//                message = "Please Wait!\r\nReading import file";
//                ((Activity) context).runOnUiThread(dialogMessages);
//
//
//                BufferedReader sr = null;
//                InputStream inputStream;
//
////                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
//                if (Utils.usingSAF()) {
//
//                    Uri uri = Uri.parse(filespec);
//                    // SAF
//                    try {
//                        inputStream =
//                                context.getContentResolver().openInputStream(uri);
//                        sr = new BufferedReader(
//                                //                         BufferedReader reader = new BufferedReader(
//                                new InputStreamReader(Objects.requireNonNull(inputStream)));
//                    } catch (FileNotFoundException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//                else
//                    try {
//                        sr = new BufferedReader(new FileReader(filespec));
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                        Log.e("Error",  "catch bufferedreader");
//                    }
//
//
//
//
//
//                database.beginTransaction();
//                boolean processing = true;
//                while (processing) {
//                    try {
//                        line = sr.readLine();
//
//
//
//
//
//                        if (line == null)
//                            break;
//                        else
//                            Log.d("lines", line);
//
//                    } catch (IOException oops) {
//                        Log.e("IO", line);
//                        break;
//                    }
//
//                    // line = StringUtils.RemoveDiacritics(line);
//                    line = Normalizer.normalize(line, Normalizer.Form.NFD);
//                    line = line.replaceAll("[^\\p{ASCII}]", "");
//
//                    if (line.charAt(0) == '/')
//                        continue;
//                    if (!Character.isLetter(line.charAt(0))) // this handles all lines not beginning with a character, including comments.
//                        continue;
//
//                    String column[] = line.split("\t");
//
//
//                    // check number of columns !!!!!!!!!!!
//                    if (column.length < 9) {
//                        processing = false;
//                        errorMessage = "This is not a valid Hoot version 3 lexicon.";
////                        progressDialog.cancel();
//                        cancel(true);
//                    }
//
//                    if (column[0].length() < 22) {
//                        // GET WORDS IN IMPORTED LIST
//                        importList.add(column[0]);
//                        // then add to table
//                        try {
//                            sqlcmd = "INSERT INTO " + TempTable + " (Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams) " +
//                                    "VALUES ('" + column[0] + "', " + // Word
//                                    "'" + column[1] + "', '" + column[2] + "', " + //FrontHooks BackHooks
//                                    "'" + getInnerCode(column[3]) + "', '" + getInnerCode(column[4]) + "', " + //InnerFront InnerBack
//                                    " " + column[5] + ", " + //ProbFactor     //           " " + Convert.ToInt32(column[5]) + ", " + //ProbFactor
//                                    " " + column[6] + ",  " + column[7] + ", " + //PlayFactor OPlayFactor
//                                    " " + column[8] + "); "; //Anagrams
//                            database.execSQL(sqlcmd);
//                            rowCount++;
//                        } catch (Exception ex) {
//                            Log.e("Values", column[0] + column[1] + column[2] + column[3]);
//                            Log.e("IMPORT ERROR", ex.getMessage());
//                        }
//                        if (isCancelled()) {
//                            processing = false;
//                            database.endTransaction();
//                            try {
//                                sr.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                    }
//                }
//
//                try {
//                    sr.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//
//
////                Toast.makeText(context, String.format("Entered %s rows into table", Integer.toString(rowCount)), Toast.LENGTH_LONG).show();
//                database.setTransactionSuccessful();
//                database.endTransaction();
//                Log.i("IMPORT", "Finished processing " + Integer.toString(rowCount) + "lexicon words");
//
//                Cursor counter = database.rawQuery("SELECT COUNT(*) FROM " + TempTable, null);
//                counter.moveToFirst();
//                int count = counter.getInt(0);
//                Log.i("IMPORT", Integer.toString(count) + " records are in" + TempTable);
//
//                // ADD NEW WORDS TO WORDS TABLE
//                message = "Please Wait!\r\nAdding new words to table";
//                ((Activity) context).runOnUiThread(dialogMessages);
//
//                database.beginTransaction();
//                for (int i = 0; i < importList.size(); i++)
//                    database.execSQL("INSERT OR IGNORE INTO Words (Word) VALUES ('" + importList.get(i) + "')");
//                database.setTransactionSuccessful();
//                database.endTransaction();
//                Log.i("IMPORT", "Added new words to Words table");
//
//                // CREATE ANDROID LEXICON
//                database.execSQL("DROP TABLE IF EXISTS `" + LexTable + "`");
//                tblSql = "CREATE TABLE `" + LexTable + "` (" +
//                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
//                        " InnerFront NUM, InnerBack NUM, " +
//                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
//                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, " +
//                        " Anagrams INTEGER NOT NULL DEFAULT 1, Score INTEGER NOT NULL DEFAULT 0, Alphagram TEXT);";
//                try {
//                    database.execSQL(tblSql);
//                } catch (Exception e) {
//                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
//                    cancel(true);
//                }
//                try {
//                    open();
//                    database.execSQL("CREATE INDEX WordIndex ON `" + TempTable + "` ( Word COLLATE NOCASE);");
//                    close();
//                } catch (Exception e) {
//                    // ignore
//                }
//                Log.i("IMPORT", "Created Lexicon Table");
//
//                // CREATE LEXICON ENTRY
//                String commandstring = "INSERT INTO tblLexicons (" +
//                        "LexiconName, LexiconSource, LexiconStuff, LexiconNotice, LexLanguage) " +
//                        "VALUES (" +
//                        "'" + lexicon.LexiconName + "', " +
//                        "'" + lexicon.LexiconSource + "', " +
//                        "'" + lexicon.LexiconStuff + "', " +
//                        "'" + lexicon.LexiconNotice + "', " +
//                        "'" + lexicon.LexLanguage + "');";
//                open();
//                database.execSQL(commandstring);
//                Log.i("IMPORT", "Created Lexicon in tblLexicons");
//
//
//                message = "Please Wait!\r\nCreating new lexicon " + LexTable;
//                ((Activity) context).runOnUiThread(dialogMessages);
//
//                commandstring = "INSERT INTO `" + LexTable + "` ( WordID, Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams ) ";
//                commandstring += "SELECT Words.WordID, ";
//                commandstring += TempTable + ".Word, ";
//                commandstring += TempTable + ".FrontHooks, ";
//                commandstring += TempTable + ".BackHooks, ";
//                commandstring += TempTable + ".InnerFront, ";
//                commandstring += TempTable + ".InnerBack, ";
//                commandstring += TempTable + ".ProbFactor, ";
//                commandstring += TempTable + ".PlayFactor, ";
//                commandstring += TempTable + ".OPlayFactor, ";
//                commandstring += TempTable + ".Anagrams ";
//                commandstring += "FROM Words INNER JOIN " + TempTable + " ON Words.Word = " + TempTable + ".Word ";
//                commandstring += "WHERE (((Length(Words.Word))>1)) ";
//
//                database.execSQL(commandstring);
//                Log.i("IMPORT", "Added all words to table");
//
//
//                // ADD SCORES TO LEXICON
////                ((Activity)context).runOnUiThread(dialogMessages);
//
//                // SAVE CURRENT TILE SET (RESET below)
//                String current = LexData.getTilesetName();
//
//                // SET TILE SET FOR IMPORT
//                if (tileset == null)
//                    LexData.setTileset(0);
//                else
//                    LexData.setTileset(Arrays.asList(LexData.tileset).indexOf(tileset));
//
//                // ADD SCORES AND ALPHAGRAMS
//                for (int size = 2; size < 22; size++) {
//                    Log.i("ScoreAdder: ", String.valueOf(size) + " letters");
//                    message = "Please Wait!\r\nCalculating word scores...\r\n" + size + " letters";
//                    ((Activity) context).runOnUiThread(dialogMessages);
//
//                    database.beginTransaction();
//                    // each length
//                    try {
//                        //               Log.i("Scores", "Calculating scores for " + String.valueOf(size) + " letter words");
////                Toast.makeText(this, String.valueOf(size), Toast.LENGTH_SHORT).show();
////                        Cursor cursor = database.rawQuery("SELECT Word FROM `" + TempTable + "` " +
////                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);
//                        Cursor cursor = database.rawQuery("SELECT WordID, Word FROM `" + LexTable + "` " +
//                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);
//
//                        while (cursor.moveToNext()) {
//                            String word = cursor.getString(1); //cursor.getColumnIndex("Word"));
//                            int wordid = cursor.getInt(0); //cursor.getColumnIndex("WordID"));
//                            int score = Utils.getValue(word);
//                            String alphagram = Utils.sortString(word);
//
//                            //Log.e("SQL","UPDATE `" + lexicon.LexiconName + "` SET Score = " + score +
//                            //" WHERE Word = " + word );
//
//                            database.execSQL("UPDATE `" + lexicon.LexiconName + "` SET Score = " + score + ", " +
//                                    " Alphagram = '" + alphagram + "'" +
//                                    " WHERE WordID = '" + wordid + "'");
//                        }
//                        database.setTransactionSuccessful();
//                    } catch (Exception e) {
//                        //database.endTransaction();
//                        Log.e("Scores", "could not add  scores for " + String.valueOf(size) + " letter words");
//                        cancel(true);
//                    } finally {
//                        database.endTransaction();
//                    }
//                    //database.setTransactionSuccessful();
//                    //database.endTransaction();
//                    if (isCancelled()) {
//                        database.endTransaction();
//                        close();
//                        progressDialog.dismiss();
//                        return null;
//                    }
//                }
//                Log.i("IMPORT", "Added Scores");
//
//                // RESET TO CURRENT TILE SET
//                LexData.setTileset(Arrays.asList(LexData.tileset).indexOf(current));
//
//                close();
//
//
//                // delete TempTableTi
//                progressDialog.dismiss();
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void result) {
//                Log.i("ImportLex", "onPostExecute");
//                success = true;
//                if (progressDialog != null) {
//                    progressDialog.dismiss();
//                }
//
//                Toast.makeText(context, "Finished importing  " + lexicon.LexiconName, Toast.LENGTH_SHORT).show();
//                Toast.makeText(context, lexicon.LexiconNotice, Toast.LENGTH_LONG).show();
//                // not setting lexicon to current as in MakeLexicon
//
//                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//// notificationId is a unique int for each notification that you must define
//                notificationManager.notify(LexData.getNotification_id(), builder.build());
//
//                Toast.makeText(context, "Go to Settings to change to this lexicon ", Toast.LENGTH_LONG).show();
//                ((Activity) context).finish();
//            }
//
//            @Override
//            protected void onCancelled() {
//                open();
//                // remove lexicon entry
//                try {
//                    database.execSQL("DELETE FROM tblLexicons WHERE LexiconName = `" + LexTable + "`");
//                } catch (SQLiteException e) {
//
//                }
//                // remove lexicon table
//                database.execSQL("DROP TABLE IF EXISTS `" + LexTable + "`");
//                close();
//                success = false;
//                if (progressDialog != null) {
//                    progressDialog.dismiss();
//                }
//                progressDialog.cancel();
//
//                Log.i("ImportLex", "Failed to Import");
//                if (!errorMessage.equals(""))
//                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
//                else
//
//                    Toast.makeText(context, "Failed to import lexicon " + LexTable, Toast.LENGTH_SHORT).show();
//            }
//        };
//        task.execute();
//        return success;
//    }
//
//
//    private static char getInnerCode(String stored) {
//        if (stored.equals("False"))
//            return ' ';
//        if (stored.equals("True"))
//            return '∙';
//        if (stored.equals(""))
//            return ' ';
//        else return ('∙');
//
//    }


    public void beginImport() {
        // Check source file for specifications first
        BufferedReader sr = null;

        String line;
        String entry = "";
        String keyword = "";
//        Log.i("Key:Entry",  keyword + ":" + entry);
        try {
            sr = new BufferedReader(new FileReader(importfile.getText().toString()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("Error",  "catch bufferedreader");
        }

        do {
            try {
                line = sr.readLine();
//                Log.i("Import", "read " + line);
            } catch (IOException oops) {
                Log.e("Error", "readline");
                break;
            }

            line = line.trim(); // leading spaces okay in file
            if (line.length() == 0) {
//                Log.i("Import", "empty");
                continue;
            }
            if (!(line.substring(0, 2).equals("//"))) {
//                Log.i("Import", "not comment");
                continue;
            }
            int sep = line.indexOf(':');
            if (sep == -1) {
//                Log.i("Import", "no colon");
                continue;
            }

//            Log.i("Key:Entry",  keyword + ":" + entry);

            // int space = line.IndexOf(' '); //
            entry = line.substring(sep + 1); // entry
            entry = entry.trim();
            keyword = line.substring(2, sep); // keyword
            keyword = keyword.trim();
//                Log.i("Key:Entry",  keyword + ":" + entry);
            switch (keyword)
            {
                case "Lexicon Name":
                    if (TextUtils.isEmpty(lexName.getText().toString().trim()))
                    {
                        lexName.setText(entry);
                    }
                    break;
                case "Lexicon Source":
                    if (TextUtils.isEmpty(lexSource.getText().toString().trim()))
                        lexSource.setText(entry);
//                    Log.i("Lex", "Name: " + lexSource.getText().toString().trim());
                    break;
                case "Lexicon Notice":
                    if (TextUtils.isEmpty(lexCopyright.getText().toString().trim()))
                        lexCopyright.setText(entry);
                    break;
                case "Lexicon Description":
                    if (TextUtils.isEmpty(lexDesc.getText().toString().trim()))
                        lexDesc.setText(entry);
                    break;
                case "Language":
                    //if (languages.getSelectedItem() == null)
                    languages.setSelection(dataAdapter.getPosition(entry));
                    break;
            }

        } while (!Character.isLetter(line.charAt(0)));

        if (TextUtils.isEmpty(lexName.getText().toString().trim())) {
            Toast.makeText(this, "Lexicon must have a name", Toast.LENGTH_LONG).show();
            return;
        }
        if (databaseAccess.lexiconExists(lexName.getText().toString().trim())) {
            Toast.makeText(this, "Lexicon " + lexName.getText().toString() + " already exists in current database", Toast.LENGTH_LONG).show();
            return;
        }

        // Now attempt to import
        Toast.makeText(this, "Begin Import of  " + lexName.getText().toString() + " ", Toast.LENGTH_LONG).show();
        // create lexicon class
        Structures.Lexicon lexicon = new Structures.Lexicon(lexName.getText().toString(),
                lexSource.getText().toString(), lexDesc.getText().toString(), lexCopyright.getText().toString(), languages.getSelectedItem().toString());
        try {
            databaseAccess.ImportLexiconThread(this, lexicon, tilesets.getSelectedItem().toString(), importfile.getText().toString());
        }
        catch (SQLiteException e) {
            e.printStackTrace();
        }
// Text: The imported lexicon will only work in the app, to import a lexicon for PC use, import in the PC and configure for lexicon use
    }
    // non async saved in version 75

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
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
