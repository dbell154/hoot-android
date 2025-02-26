package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.util.List;
import java.util.Objects;

import static com.tylerhosting.hoot.hoot.CardUtils.nextDate;

//ListName
//Category (select,add)
//Author
//Source
//Credits
//Description
//Link
//AuthorEmail


    public class ImportSubjectListActivity extends AppCompatActivity {
        Spinner listCategories;
        Button search, importlist, addcategory;
        TextView importfile, listName, listSource, listDesc, listCredits;
        DatabaseAccess databaseAccess;
        List<String> catList = new ArrayList<>();

        ArrayAdapter<String> dataAdapter;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Dialog doesn't show text
            //        Utils.setStartTheme(this);


            setContentView(R.layout.activity_import_subject_list);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


//            Toolbar toolbar = findViewById(R.id.toolbar);
//            setSupportActionBar(toolbar);
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            databaseAccess = DatabaseAccess.getInstance(getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());

            // CATEGORY
            listCategories = findViewById(R.id.listCategory);

            databaseAccess.open();
            String sql =       "SELECT Category from tblListCategories";
            Cursor cursor= databaseAccess.rawQuery(sql);
            int index = cursor.getColumnIndex("Category");

            while (cursor.moveToNext()) {
                catList.add(cursor.getString(index));
            }
            databaseAccess.close();



            dataAdapter = new ArrayAdapter<String>(this,
                    R.layout.spinmedium, catList);
            listCategories.setAdapter(dataAdapter);
//        languages.setOnItemSelectedListener(selection);

// SUBJECT
            search = (Button) findViewById(R.id.Search);
            search.setOnClickListener(findFile);

            addcategory = findViewById(R.id.btnAddCat);
            addcategory.setOnClickListener(enterCategory);

// BEGINIMPORT
            importlist = (Button) findViewById(R.id.beginImport);
            importlist.setOnClickListener(importList);

            importfile = findViewById(R.id.listFile);
            listName = findViewById(R.id.listName);
            listName.setImeOptions(EditorInfo.IME_ACTION_NEXT);

            listSource = findViewById(R.id.listSource);
            listSource.setImeOptions(EditorInfo.IME_ACTION_NEXT);

            listCredits = findViewById(R.id.listCredits);
            listDesc = findViewById(R.id.listDescription);

            setRotation();
        }
        private View.OnClickListener findFile = new View.OnClickListener() {
            public void onClick(View v) {
                selectFile();
            }
        };

        private String newCategory;
        private View.OnClickListener enterCategory = new View.OnClickListener() {
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(ImportSubjectListActivity.this);
                builder.setTitle("Enter new category");

// Set up the input
                final EditText input = new EditText(ImportSubjectListActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

// Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        newCategory = input.getText().toString();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

                // create category
                databaseAccess.open();
                databaseAccess.CreateCategory(newCategory);
                databaseAccess.close();



                // reload spinner
                dataAdapter.notifyDataSetChanged();

            }
        };
        private View.OnClickListener importList = new View.OnClickListener() {
            public void onClick(View v) {
                beginImport();
            }
        };

        //        public void selectFile() {
//            if (!databaseAccess.permission(this))
//                return;
//            File mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//" );
//            FileDialog fileDialog = new FileDialog(this, mPath, "txt");
//            // only supports one file extension
//
//            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
//                public void fileSelected(File file) {
//                    String full = file.getAbsolutePath();
//                    importfile.setText(full);
//                    preFill();
//
//
//                    // Prefill form
//
//                }
//            });
//            fileDialog.showDialog();
//        }

        private static final int PICK_TXT_FILE = 2;
        public void selectFile() {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
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
//                        preFill();
                        importfile.setPrivateImeOptions(full);
                        try {
                            getListDetailsFromFile(full);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                fileDialog.showDialog();
            }

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
//                  preFill();
                    try {
                        getListDetailsFromFile(uri.toString());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }

        public void getListDetailsFromFile(String file) throws IOException {
            // Check source file for specifications first
            BufferedReader sr = null;

            String line;
            String entry = "";
            String keyword = "";

//            String file = importfile.getText().toString();
//            String file = importfile.getPrivateImeOptions().toString();



//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
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
                if (line == null)
                    break;

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
                    case "List Name":
                        if (TextUtils.isEmpty(listName.getText().toString().trim()))
                        {
                            listName.setText(entry);
                        }
                        break;

                    case "List Source":
                        if (TextUtils.isEmpty(listSource.getText().toString().trim()))
                            listSource.setText(entry);
                        break;
                    case "List Credits":
                        if (TextUtils.isEmpty(listCredits.getText().toString().trim()))
                            listCredits.setText(entry);
                        break;
                    case "List Description":
                        if (TextUtils.isEmpty(listDesc.getText().toString().trim()))
                            listDesc.setText(entry);
                        break;
                    case "List Category":
                        //if (languages.getSelectedItem() == null)

                        if (dataAdapter.getPosition(entry) == -1) {
                            databaseAccess.open();
                            databaseAccess.CreateCategory(entry);
                            databaseAccess.close();
                            dataAdapter.notifyDataSetChanged();
                        }

                        listCategories.setSelection(dataAdapter.getPosition(entry));
                        break;
                }

            } while (true)  ;
            // while (line != null)
                    // line.substring(0, 2).equals("//")); // Crashes on S7 here !Character.isLetter(line.charAt(0)));

            if (TextUtils.isEmpty(listName.getText().toString().trim())) {
                Toast.makeText(this, "List must have a name", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                sr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
/*           if (databaseAccess.lexiconExists(listName.getText().toString().trim())) {
                Toast.makeText(this, "List " + listName.getText().toString() + " already exists in current database", Toast.LENGTH_LONG).show();
                return;
            }

            // Now attempt to import
            Toast.makeText(this, "Begin Import of  " + listName.getText().toString() + " ", Toast.LENGTH_LONG).show();
            // create lexicon class


**            Structures.Lexicon lexicon = new Structures.Lexicon(listName.getText().toString(),
                    listSource.getText().toString(), listDesc.getText().toString(), listCredits.getText().toString(), listCategories.getSelectedItem().toString());
            try {
                databaseAccess.ImportLexiconThread(this, lexicon, importfile.getText().toString());
            }
            catch (SQLiteException e) {
                e.printStackTrace();
            }

 */

// Text: The imported lexicon will only work in the app, to import a lexicon for PC use, import in the PC and configure for lexicon use
        }

        ProgressDialog progressDialog;
        public String message = "Please Wait!\r\nCalculating word scores...";
        private Runnable dialogMessages = new Runnable() {
            @Override
            public void run() {
                //Log.v(TAG, strCharacters);
                progressDialog.setMessage(message);
            }
        };

        public void beginImport() {

            if (TextUtils.isEmpty(listName.getText().toString().trim())) {
                Toast.makeText(this, "List must have a name", Toast.LENGTH_LONG).show();
                return;
            }
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Please Wait...");
            progressDialog.show();

            new Thread() {
                public void run() {
                    // TODO ADD PROGRESS DIALOG
//            ProgressBar progressBar = new ProgressBar(this);
//            progressBar.


            // create list entry
            String sql;
            int ListID;
            int CatID;
            String Category = listCategories.getSelectedItem().toString();

            databaseAccess.open();
            sql = "SELECT CategoryID from tblListCategories WHERE Category = '" + Category + "'";
            Cursor cursor = databaseAccess.rawQuery(sql);
            cursor.moveToFirst();
            CatID = cursor.getInt(cursor.getColumnIndex("CategoryID"));

            LexData.WordList wordList = new LexData.WordList(listName.getText().toString(), "", listCredits.getText().toString(), listDesc.getText().toString(),
                    listSource.getText().toString(), " ", " ",  CatID);
            databaseAccess.ImportSubjectList(wordList);
// Adding blank

            sql = "SELECT ListID FROM WordLists WHERE ListName = '" + wordList.ListName + "'";
            cursor = databaseAccess.rawQuery(sql);
            cursor.moveToFirst();
            ListID = cursor.getInt(cursor.getColumnIndex("ListID"));



//            Toast.makeText(this,"Adding " + wordList.ListName + " to subject lists in Category " + Category, Toast.LENGTH_LONG).show();
                    message = "Adding " + wordList.ListName + " to subject lists in Category " + Category;
                    runOnUiThread(dialogMessages);
                    Log.d("Adding", message);


//            String importFile = importfile.getText().toString();
            List<String> words;
//            words = Utils.getWordsFromFile(importFile);

            if (Utils.usingSAF()) {
                try {
                    words = Utils.getWordsFromURI(getApplicationContext(), importfile.getPrivateImeOptions().toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else
                words = Utils.getWordsFromFile(importfile.getText().toString());


/*
            BufferedReader sr = null;

            String line;
            try {
                sr = new BufferedReader(new FileReader(importfile.getText().toString()));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.e("Error",  "catch bufferedreader");
            }

            // get the words first
            List<String> words = new ArrayList<>();
            String word;
            do {
                try {
                    line = sr.readLine();
                } catch (IOException oops) {
                    Log.e("Error", "readline");
                    break;
                }
                if (line == null)
                    break;

                line = line.trim().toUpperCase(); // leading spaces okay in file
                if (line.length() == 0) {
                    continue;
                }
                if ((line.substring(0, 2).equals("//"))) {
                    continue;
                }
                // add line to word list
                line = line.replaceAll("\t", " ");
                int sep = line.indexOf(' ');
                if (sep != -1) {
                    word = line.substring(0, sep); // keyword
                }
                else
                    word = line;
                Log.d("Word", word);

                words.add(word);

            } while (true);


 */



            // then get IDs and create if necessary
            List<LexData.GlobalWord> globalWords = new ArrayList<>();
            LexData.GlobalWord globalWord = new LexData.GlobalWord();

            for (int w = 0; w < words.size(); w++) {
                sql = "SELECT WordID, Word FROM Words WHERE Word = '" + words.get(w) + "';";
                cursor = databaseAccess.rawQuery(sql);
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    globalWord = new LexData.GlobalWord(cursor.getInt(cursor.getColumnIndex("WordID")),
                            cursor.getString(cursor.getColumnIndex("Word")));
                    globalWords.add(globalWord);
                    cursor.close();
                }
                else {
                    sql = "INSERT or IGNORE into Words (Word) Values ('" + words.get(w) + "')";
                    databaseAccess.execSQL(sql);

                    sql = "SELECT WordID, Word FROM Words WHERE Word = '" + words.get(w) + "';";
                    cursor = databaseAccess.rawQuery(sql);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        globalWord = new LexData.GlobalWord(cursor.getInt(0), cursor.getString(1));
                        globalWords.add(globalWord);
                    }
                    cursor.close();
                }
            }

            // finally add to Wordlist
            for (int g = 0; g < globalWords.size(); g++) {
//                Log.d("Insert", "ListID: " + ListID + " WordID: " + globalWords.get(g).WordID + " CatID: " + CatID);
                sql = "INSERT or IGNORE into WordList (ListID, WordID, CategoryID) Values ('" + ListID + "', '" +
                        globalWords.get(g).WordID + "', '" + CatID + "')";
                databaseAccess.execSQL(sql);
            }

            databaseAccess.close();

//            Toast.makeText(this,wordList.ListName + " added to subject lists in Category " + Category, Toast.LENGTH_LONG).show();

                    message = "wordList.ListName + \" added to subject lists in Category \" + Category ";
                    runOnUiThread(dialogMessages);
                    Log.d("added", wordList.ListName);

                    importfile.setText("");
                    listName.setText("");
                    listSource.setText("");
                    listDesc.setText("");
                    listCredits.setText("");

//            TextView importfile, listName, listSource, listDesc, listCredits;

/*           if (databaseAccess.lexiconExists(listName.getText().toString().trim())) {
                Toast.makeText(this, "List " + listName.getText().toString() + " already exists in current database", Toast.LENGTH_LONG).show();
                return;
            }

            // Now attempt to import
            Toast.makeText(this, "Begin Import of  " + listName.getText().toString() + " ", Toast.LENGTH_LONG).show();
            // create lexicon class


**            Structures.Lexicon lexicon = new Structures.Lexicon(listName.getText().toString(),
                    listSource.getText().toString(), listDesc.getText().toString(), listCredits.getText().toString(), listCategories.getSelectedItem().toString());
            try {
                databaseAccess.ImportLexiconThread(this, lexicon, importfile.getText().toString());
            }
            catch (SQLiteException e) {
                e.printStackTrace();
            }

 */

// Text: The imported lexicon will only work in the app, to import a lexicon for PC use, import in the PC and configure for lexicon use
                    progressDialog.dismiss();

                }
            }.start();


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

