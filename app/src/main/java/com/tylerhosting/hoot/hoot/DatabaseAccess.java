package com.tylerhosting.hoot.hoot;

// this should open/close in all methods, unless returning Cursor
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class DatabaseAccess {
    private DatabaseOpenHelper openHelper;
    private SQLiteDatabase database;
    private static DatabaseAccess instance;

    /**
     * Private constructor to avoid object creation from outside classes.
     *
     * @param context
     */
    private DatabaseAccess(Context context, String databasePath, String database) {
        this.openHelper = new DatabaseOpenHelper(context, databasePath, database);
    }

    /**
     * Return a singleton instance of DatabaseAccess.
     *
     * @param context the Context
     * @return the instance of DatabaseAccess
     */
    public static DatabaseAccess getInstance(Context context, String databasePath, String database) {
        if (instance == null) {
            instance = new DatabaseAccess(context, databasePath, database);
        } else {
            //instance.close();
            instance = new DatabaseAccess(context, databasePath, database);
        }
        return instance;
    }
    public void open() {
        this.database = openHelper.getWritableDatabase();
//        String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
//        Log.i("Database", "Opening database in " + openHelper.returnDBPath() + " in " + caller);

Log.e("Database Path ", database.getPath());
        if (this.database == null) {
            Flavoring.addflavoring(); // sets database
            LexData.setDatabasePath(Hoot.getAppContext(), "");
            this.database = openHelper.getWritableDatabase();
        }


    }
    public void close() {
        if (database != null) {
            this.database.close();
            String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
            Log.i("Assets", "successfully CLOSED database in " + caller);
        }
    }


    // VARIABLES
    int version = 0;
    private boolean success = false;
    public boolean scored;
    private String errorMessage = ""; // used when cancelled
    public String message = "Please Wait!\r\nCalculating word scores...";
    public ProgressDialog progressDialog;
    private Runnable dialogMessages = new Runnable() {
        @Override
        public void run() {
            //Log.v(TAG, strCharacters);
            progressDialog.setMessage(message);
        }
    };


    //    public void openDirectory(){
//        int RQS_OPEN_DOCUMENT_TREE = 2;
//        Uri uriToLoad = Uri.fromFile(new File("/Documents"));
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
//        startActivityForResult(ToolsActivity, intent,
//                RQS_OPEN_DOCUMENT_TREE, null );
//        return;
//    }
//

    // STANDARD DB/Lexicon METHODS
    public Cursor rawQuery(String sql) {
        return database.rawQuery(sql, null);
    }
    public void execSQL(String sql) {
        database.execSQL(sql);
    }


    // Database checks, sets
    public boolean isValidDatabase() {
        open();

        
        Cursor cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tblLexicons';", null);
        if (cursor.getCount() == 0)
            return false;
        return true;
    }
    // This method will check if column exists in your table
    public boolean columnExists(String tableName, String fieldName) {
        boolean isExist = false;
        open();
        Cursor res = database.rawQuery("PRAGMA table_info(`"+tableName+"`)",null);
        res.moveToFirst();
        do {
            String currentColumn = res.getString(1);
            if (currentColumn.equals(fieldName)) {
                isExist = true;
            }
        } while (res.moveToNext());
        return isExist;
    }


    // Lexicon checks, sets
    public void defaultDBLexicon(Context context) {
        LexData.setDatabasePath(context, "");
        // sets the default database
        //com.tylerhosting.hoot.hoot.
        Flavoring.addflavoring(context); //
        LexData.setDefaultLexicon(context);
    }
    public ArrayList<Structures.Lexicon> get_lexicons() {
        ArrayList<Structures.Lexicon> lexicons = new ArrayList<>();
        open();
        Cursor cursor = database.rawQuery("SELECT * FROM tblLexicons ORDER BY LexiconName", null);

        while (cursor.moveToNext()) {
            Structures.Lexicon lexicon = new Structures.Lexicon();
            lexicon.LexiconID = cursor.getInt(cursor.getColumnIndex("LexiconID"));
            lexicon.LexiconName = cursor.getString(cursor.getColumnIndex("LexiconName"));
            lexicon.LexiconSource = cursor.getString(cursor.getColumnIndex("LexiconSource"));
            lexicon.LexiconStuff = cursor.getString(cursor.getColumnIndex("LexiconStuff"));
            lexicon.LexiconNotice = cursor.getString(cursor.getColumnIndex("LexiconNotice"));
            lexicon.LexLanguage = cursor.getString(cursor.getColumnIndex("LexLanguage"));
            lexicons.add(lexicon);
        }
        cursor.close();
        close();
        return lexicons;
    }
    public Structures.Lexicon get_lexicon(String lexName) {
        Structures.Lexicon lexicon = new Structures.Lexicon(); // not assigned to LexData

        if (lexName.equals(""))
            lexName = get_firstValidLexicon();

        open();
        if (!isValidDatabase())
            return null;

        Cursor cursor = database.rawQuery("SELECT * FROM tblLexicons", null);
        while (cursor.moveToNext()) {
            String LexName = cursor.getString(cursor.getColumnIndex("LexiconName"));
            if (LexName.equals(lexName)) {
                lexicon.LexiconID = cursor.getInt(cursor.getColumnIndex("LexiconID"));
                lexicon.LexiconName = LexName; // already got this
                lexicon.LexiconSource = cursor.getString(cursor.getColumnIndex("LexiconSource"));
                lexicon.LexiconStuff = cursor.getString(cursor.getColumnIndex("LexiconStuff"));
                lexicon.LexiconNotice = cursor.getString(cursor.getColumnIndex("LexiconNotice"));
                lexicon.LexLanguage = cursor.getString(cursor.getColumnIndex("LexLanguage"));
            }
        }
        cursor.close();
        close();
        return lexicon;
    }
    public String get_firstValidLexicon() { // gets first VALID lexicon
        Structures.Lexicon lexicon = new Structures.Lexicon();

        open();

        Cursor cursor = database.rawQuery("SELECT " +
                "LexiconID, LexiconName, LexiconSource, LexiconStuff, LexiconNotice, LexLanguage FROM tblLexicons ORDER BY LexiconID", null);

        cursor.moveToFirst();
        do {
            lexicon.LexiconName = cursor.getString(cursor.getColumnIndex("LexiconName"));
            if (checkLexicon(lexicon.LexiconName))
                break;
        } while (cursor.moveToNext());
        if (cursor.isAfterLast())
            return "";
        lexicon.LexiconID = cursor.getInt(cursor.getColumnIndex("LexiconID"));
        lexicon.LexiconName = cursor.getString(cursor.getColumnIndex("LexiconName"));
        lexicon.LexiconSource = cursor.getString(cursor.getColumnIndex("LexiconSource"));
        lexicon.LexiconStuff = cursor.getString(cursor.getColumnIndex("LexiconStuff"));
        lexicon.LexiconNotice = cursor.getString(cursor.getColumnIndex("LexiconNotice"));
        lexicon.LexLanguage = cursor.getString(cursor.getColumnIndex("LexLanguage"));
        cursor.close();
        close();
        return lexicon.LexiconName;
    }
    public boolean lexiconExists(String lexname) { // if exists
        int id = 0;
        try {
            open();
            Cursor cursor = database.rawQuery("SELECT LexiconID FROM tblLexicons WHERE LexiconName = '" + lexname + "'", null);
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                id = cursor.getInt(cursor.getColumnIndex("LexiconID"));
            }
            cursor.close();
            close();
            return (id > 0);
        } catch (Exception e) {
            close();
            return false;
        }
    }
    public boolean checkLexicon(Structures.Lexicon lexicon) { // if app version exists
        return checkLexicon(lexicon.LexiconName);
//        open();
//        Log.e("Database Path ", database.getPath());
//        try {
//            Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM `" + lexicon.LexiconName + "` ", null);
//            if (!cursor.moveToFirst()) {
//                cursor.close();
//                return false;
//            }
//            int count = cursor.getInt(0);
//            cursor.close();
//            close();
//            return count > 0;
//        } catch (Exception e) {
//            close();
//            return false;
//        }
    }
    public boolean checkLexicon(String LexiconName) { // if app version exists
        open();
        Log.e("Check Database Path ", database.getPath());
        try {
            Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM `" + LexiconName + "` ", null);
            if (!cursor.moveToFirst()) {
                cursor.close();
                return false;
            }
            int count = cursor.getInt(0);
            cursor.close();
            close();
            return count > 0;
        } catch (Exception e) {
            close();
            return false;
        }
    }
    public boolean MakeDummyLexicon() {

        success = true;
        // Define Lexicon Structure
        Structures.Lexicon dummy = new Structures.Lexicon("Lava", "", "", "", "en");

        // Create Lexicon
        String commandstring = "INSERT INTO tblLexicons (" +
                "LexiconName, LexiconSource, LexiconStuff, LexiconNotice, LexLanguage) " +
                "VALUES (" +
                "'" + dummy.LexiconName + "', " +
                "'" + dummy.LexiconSource + "', " +
                "'" + dummy.LexiconStuff + "', " +
                "'" + dummy.LexiconNotice + "', " +
                "'" + dummy.LexLanguage + "');";
        open();
        database.execSQL(commandstring);

        // Create Lexicon Table
        String tblSql = "CREATE TABLE `" + dummy.LexiconName + "` (" +
                " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
                " InnerFront NUM, InnerBack NUM, " +
                " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
                " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, Anagrams INTEGER NOT NULL );";
        try {
//                    open();
            database.execSQL(tblSql);
//                    close();
        } catch (Exception e) {
            Log.e("Lexicon", "could not create " + " - " + e.getMessage());
        }

        // Prepare to add a word to the lexicon
        // make sure the word exists
        database.execSQL("INSERT OR IGNORE INTO Words (Word) VALUES ('AA')");
        // then get the wordid
        Cursor identifier = database.rawQuery("SELECT WordID from Words WHERE Word = 'AA'", null);
        identifier.moveToFirst();
        int Wordid = identifier.getInt(0);

        // Add a word to the Lexicon Table
        String sqlcmd = "INSERT INTO " + dummy.LexiconName + " (WordID, Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams) " +
                "VALUES (" + Wordid + ", 'AA', " + // Word
                "'', '', " + //FrontHooks BackHooks
                "'', '', " + //InnerFront InnerBack
                " 1, " + //ProbFactor     //           " " + Convert.ToInt32(column[5]) + ", " + //ProbFactor
                " 1,  1, " + //PlayFactor OPlayFactor
                " 1); "; //Anagrams
        database.execSQL(sqlcmd);
        return success;
    }
    public String getValidLexicons(String word) {
        //List<String> lexes = new ArrayList<>();
        word = word.toUpperCase();
        String validLexicons = "";

        List<String> allLex = new ArrayList<>();
        List<String> andLex = new ArrayList<>();
        List<String> valLex = new ArrayList<>();
        // make list of all lexicons
//        Cursor all = database.rawQuery("Select `" + LexiconName + "` from tblLexicons", null);
        Cursor all = database.rawQuery("Select LexiconName from tblLexicons", null);
        while (all.moveToNext()) {
            allLex.add(all.getString(0));
        }

        // make list of all Android lexicons
        for (int i = 0; i < allLex.size(); i++) {
            Cursor andr = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name = '" +
                    allLex.get(i) + "'\n", null);

            if (andr.moveToFirst()) {
                String g = andr.getString(0);
                andLex.add(g);
            }
            else {
                continue;
            }

        }

        // make list of lexicons where valid
        for (int j = 0; j < andLex.size(); j++) {
            String lextest = andLex.get(j);
            Cursor valid = database.rawQuery("Select Word from `" + lextest + "` " + "WHERE Word = '" + word + "'", null);

            if (valid.moveToFirst()) {
                valLex.add(lextest);
            }
            else {
                continue;
            }

        }


        if (valLex.isEmpty())
            return "";

        validLexicons = "\r\nValid in: \r\n";
        for (int k = 0; k < valLex.size(); k++) {
            validLexicons += "\t" + valLex.get(k) + "\r\n";
        };

        validLexicons += "\r\n";
        return validLexicons;
    }
    public String getInValidLexicons(String word) {
        //List<String> lexes = new ArrayList<>();
        word = word.toUpperCase();
        String validLexicons = "";

        List<String> allLex = new ArrayList<>();
        List<String> andLex = new ArrayList<>();
        List<String> valLex = new ArrayList<>();
        // make list of all lexicons
//        Cursor all = database.rawQuery("Select `" + LexiconName + "` from tblLexicons", null);
        Cursor all = database.rawQuery("Select LexiconName from tblLexicons", null);
        while (all.moveToNext()) {
            allLex.add(all.getString(0));
        }

        // make list of all Android lexicons
        for (int i = 0; i < allLex.size(); i++) {
            Cursor andr = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name = '" +
                    allLex.get(i) + "'\n", null);

            if (andr.moveToFirst()) {
                String g = andr.getString(0);
                andLex.add(g);
            }
            else {
                continue;
            }

        }

        // make list of lexicons where valid
        for (int j = 0; j < andLex.size(); j++) {
            String lextest = andLex.get(j);
            Cursor valid = database.rawQuery("Select Word from `" + lextest + "` " + "WHERE Word = '" + word + "'", null);

            if (!valid.moveToFirst()) {
                valLex.add(lextest);
            }
            else {
                continue;
            }

        }


        if (valLex.isEmpty())
            return "";

        validLexicons = "\r\nValid in: \r\n";
        for (int k = 0; k < valLex.size(); k++) {
            validLexicons += "\t" + valLex.get(k) + "\r\n";
        };

        validLexicons += "\r\n";
        return validLexicons;
    }
    public void configureLexicon(Context context, String lexiconName) {
        // This is the main method to call to configure lexicons, it calls the threads in sequence

        // calling method has already determined the lexicon exists
        if (!lexiconExists(lexiconName))
            return;
//        open();
//        Toast.makeText(context, "Extracting Lexicon " + lexiconName, Toast.LENGTH_LONG).show();
        Structures.Lexicon lexicon = get_lexicon(lexiconName);
//        LexData.setLexicon(context, lexicon.LexiconName); // do on post execute
        MakeLexiconThread(context, lexicon, "");
    }


    ////// STANDARD Search Library (Cursor)
    public Cursor getCursor_ByLetterCount(int count, String filters, String ordering, String limits, boolean rack) {
//        rackfilter = rack;
//        if (limits == "")
//            limits = limitString(LexData.getMaxList());
        if (rack)
            limits = "";

        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Length(Word) = " + String.valueOf(count) +
                filters +
                " ) " + ordering + limits, null);

        return cursor;
    }
    public Cursor getCursor_BetweenLengths(int least, int most, String filters, String ordering, String limits, boolean rack) {
//        rackfilter = rack;
//        if (limits == "")
//            limits = limitString(LexData.getMaxList());
        if (rack)
            limits = "";

        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Length(Word) >= " + String.valueOf(least) +
                " AND Length(Word) <= " + String.valueOf(most) +
                filters +
                " ) " + ordering + limits, null);

        return cursor;
    }
    public Cursor getCursor_hookwords(String term, String filters, String ordering, String limits, boolean rack) {
        if (term.trim() == "")
            return null;
        if (rack)
            limits = "";

        String lenFilter = String.format("Length(Word) = %1$s", term.length() + 1);
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" +
                "(Word LIKE '%" + term + "' OR " +
                " Word LIKE '" + term + "%')" +
                " AND " + lenFilter +
                filters +
                " ) " + ordering + limits, null);
        return cursor;
    }
    // only used by SubSearch
    public Cursor getCursor_anagrams(String term, String filters, String ordering, int limit, int offset, boolean rack) {
        int skips = 0;

         //        if (limits == "")
//            limits = limitString(LexData.getMaxList());
        if (term.contains("*") || term.contains("@"))

        {
            term = term.replaceAll("[*@]", "");
            return getCursor_superanagrams(term, filters, ordering, rack);
        }
        // anagrams are all the same length
        if (term.trim().length() < 2)
            return null;
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");
        StringBuilder speedFilter = new StringBuilder();

        if (alpha.length() != 0) {
            for (int letter = 0; letter < alpha.length() && letter < 3; letter++)
                speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
        }
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String filter = String.format("Length(Word) = %1$s", term.length());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + filter +
                speedFilter +
                filters +
                " ) " + ordering , null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

//        tilefreq = new int[26];
//        tilefreq = LexData.tileFreq();
        while (cursor.moveToNext()) {

            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] anagram = word.toCharArray();
//            if (isAnagram(first, anagram, blankcount)) {
//                matrixCursor.addRow(get_CursorRow(cursor));
//            }

            // todo need to rewrite like other methods
            if (isAnagram(first, anagram, blankcount)) {
                int position = matrixCursor.getCount();
                if (position < offset - skips) {
                    skips++;
                    continue;
                }
                if (limit > 0)
                    if (position == limit)
                        break;
                matrixCursor.addRow(get_CursorRow(cursor));
            }




//            if (LexData.getMaxList() > 0)
//                if (matrixCursor.getCount() == LexData.getMaxList())
//                    break;

        }
        return matrixCursor;
    }
    // THE BLANK IS IN THE TERM, CAN USE FOR ANAGRAM WITH ?
    public Cursor getCursor_blankAnagrams(String term, String filters, String ordering, int limit, int offset, boolean rack) {
        int skips = 0;

        // use for Stems primarily
        if (term.trim().length() < 2)
            return null;
        char[] stem = term.toCharArray(); // stem

        int[] first = new int[26]; // letter count of stem
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < stem.length; c++) {
            if (stem[c] == '?') {
                blankcount++;
                continue;
            }
            first[stem[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");
        StringBuilder speedFilter = new StringBuilder();
        if (alpha.length() != 0) {
            for (int letter = 0; letter < alpha.length() && letter < 3; letter++)
                speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
        }

        String filter = String.format("Length(Word) = %1$s", term.length());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + filter +
                speedFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        //tilefreq = new int[26];
        //tilefreq = LexData.tileFreq();
        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] anagram = word.toCharArray();
            int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int letter;
            char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            int mismatchcount = 0;


            // This counts blanks
            for (int x = 0; x < 26; x++)
                blanks[x] = 0;
            for (c = 0; c < anagram.length; c++) {
                letter = ++second[anagram[c] - 'A'];
                if (letter > first[anagram[c] - 'A']) {
                    blanks[anagram[c] - 'A']++;


                    mismatchcount++;
                }
            }


            if (mismatchcount > blankcount) {
                continue;
            }


            // This adds the colored version of the string
            // always use here (BA), compare for blankcount elsewhere
//            matrixCursor.addRow(get_ColorBlankCursorRow(cursor, blanks));

//            if (LexData.getColorBlanks())
//                matrixCursor.addRow(get_BlankCursorRow(cursor, blanks));
//            else
//                matrixCursor.addRow(get_CursorRow(cursor));

//                Log.d("Limit", limit + ":" + offset);


            if (!rack) {
                if (offset > skips) {
                    skips++;
                    continue;
                }
                int position = matrixCursor.getCount();
                if (limit > 0)
                    if (position == limit)
                        break;
                if (LexData.getColorBlanks())
                    matrixCursor.addRow(get_BlankCursorRow(cursor, blanks));
                else
                    matrixCursor.addRow(get_CursorRow(cursor));
            } else
            if (LexData.getColorBlanks())
                matrixCursor.addRow(get_BlankCursorRow(cursor, blanks));
            else
                matrixCursor.addRow(get_CursorRow(cursor));



//        if (!rack) {
//                int position = matrixCursor.getCount();
//                if (position < offset - skips) {
//                    skips++;
//                    continue;
//                }
//                if (limit > 0)
//                    if (position == limit)
//                        break;
//                if (LexData.getColorBlanks())
//                    matrixCursor.addRow(get_BlankCursorRow(cursor, blanks));
//                else
//                    matrixCursor.addRow(get_CursorRow(cursor));
//            }




//            if (!rack)
//                if (LexData.getMaxList() > 0)
//                    if (matrixCursor.getCount() == LexData.getMaxList())
//                        break;

        }
        return matrixCursor;
    }
    public Cursor getCursor_contains(String term, String filters, String ordering, String limits, boolean rack) {
        if (term.trim() == "")
            return null;
        if (rack)
            limits = "";

        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " AND " + "Word LIKE '%" + term + "%' " +
                filters +
                " ) " + ordering +
                limits, null);
        return cursor;


    }
    public Cursor getCursor_contains(String term, String filters) {
        if (term.trim() == "")
            return null;

        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " AND " + "Word LIKE '%" + term + "%' " +
                filters +
                " ) " , null);
        return cursor;
    }
    public Cursor getCursor_containsAny(String letters, String filters, String ordering, int limit, int offset, boolean rack) {
        int skips = 0;
        if (letters.trim() == "")
            return null;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        char[] a = letters.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);


        while (precursor.moveToNext()) {

            String word = precursor.getString(1);
            char[] b = word.toCharArray();

            if (containsany(first, b)) {

                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;
                    matrixCursor.addRow(get_CursorRow(precursor));
                } else
                    matrixCursor.addRow(get_CursorRow(precursor));
            }
        }



        precursor.close();
        return matrixCursor;

    }
    public Cursor getCursor_subanagrams(String term, String filters, String ordering, int limit, int offset, boolean rack) {
        // NO LIMITING FOR THIS METHOD ; RESULTS ARE SHORT
        int skips = 0;
        if (term.trim() == "")
            return null;
        Cursor cursor;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) <= %2$s", LexData.getMaxLength(), term.length());

        // only difference between this and anagram is changing the length filter
        char[] a = term.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        for (c = 0; c < a.length; c++) { // initialize word to anagram
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");


        StringBuilder speedFilter = new StringBuilder();
        if (alpha.length() != 0) {
            speedFilter.append(" AND (Word LIKE '%" + alpha.substring(0, 1) + "%' "); // first letter
            for (int letter = 1; letter < alpha.length(); letter++)
                speedFilter.append(" OR Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
            speedFilter.append(") ");

        }

        cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                speedFilter +
                filters +
                " ) " + ordering, null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(1);
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram, blankcount)) {



                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;


                    if (LexData.getColorBlanks()) {
                        int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        int letter;
                        for (int x = 0; x < 26; x++)
                            blanks[x] = 0;
                        for (c = 0; c < anagram.length; c++) {
                            letter = ++second[anagram[c] - 'A'];
                            if (letter > first[anagram[c] - 'A']) {
                                blanks[anagram[c] - 'A']++;
                            }
                        }
                        matrixCursor.addRow(get_BlankCursorRow(cursor, blanks));
                    } else
                        matrixCursor.addRow(get_CursorRow(cursor));
                }
                else {
                    if (LexData.getColorBlanks()) {
                        int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        int letter;
                        for (int x = 0; x < 26; x++)
                            blanks[x] = 0;
                        for (c = 0; c < anagram.length; c++) {
                            letter = ++second[anagram[c] - 'A'];
                            if (letter > first[anagram[c] - 'A']) {
                                blanks[anagram[c] - 'A']++;
                            }
                        }
                        matrixCursor.addRow(get_BlankCursorRow(cursor, blanks));
                    } else
                        matrixCursor.addRow(get_CursorRow(cursor));

                }

            }


//            if (!rack)
//                if (LexData.getMaxList() > 0)
//                    if (matrixCursor.getCount() == LexData.getMaxList())
//                        break;
        }

        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_begins(String term, String filters, String ordering, String limits, boolean rack) {
        if (rack)
            limits = "";
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '" + term + "%' " +
                " AND " + lenFilter +
                filters +
                " ) " + ordering +limits, null);
        return cursor;
    }
    public Cursor getCursor_begins(String term, String filters) {
        String sql = "SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '" + term + "%' " +
                filters  + ")";
 //       Log.e("sql", sql);
        Cursor cursor = database.rawQuery(sql, null);
        return cursor;
    }
    public Cursor getCursor_ends(String term, String filters, String ordering, String limits, boolean rack) {
        if (rack)
            limits = "";
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '%" + term + "' " +
                " AND " + lenFilter +
                filters +
                " ) " + ordering + limits, null);
        return cursor;
    }
    public Cursor getCursor_ends(String term, String filters) {
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '%" + term + "' " +
                filters +
                " ) " , null);
        return cursor;
    }
    public Cursor getCursor_qNotu(String filters, String ordering, int limit, int offset, boolean rack) {
        int skips = 0;
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '%" + "Q" + "%' " +
                " AND " + lenFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            if (!word.contains("U"))


                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;
                    matrixCursor.addRow(get_CursorRow(cursor));
                }
                else
                    matrixCursor.addRow(get_CursorRow(cursor));

        }
        cursor.close();
        return matrixCursor;
// TODO        results = results.Cast<HookSet>().OrderBy(item => item.Word.Length).ThenBy(item => item.Word.ToString()).ToList();
//        if (Flags.listLimited)
//            return Limits.get_LimitedResults(results);*/
    }
    public Cursor getCursor_misspells(String term, String ordering, boolean rack) {
        String lenFilter = String.format("Length(Word) = %s", term.length());

        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                " WHERE (" + lenFilter +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (cursor.moveToNext()) {
            String test = cursor.getString(cursor.getColumnIndex("Word"));
            if (isMisspell(term, test))
                matrixCursor.addRow(get_CursorRow(cursor));
            if (!rack)
                if (LexData.getMaxList() > 0)
                    if (matrixCursor.getCount() == LexData.getMaxList())
                        break;
        }
        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_transpositions(String term) {
        char shift;
        String searched;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        Cursor searcher;

        // can use double for loop
        for (int i = 0; i < term.length() - 1; i++) {
            char[] wc = term.toCharArray(); // or else must unswap before next test
            if ((wc[i] == wc[i + 1]))
                continue;// same letter swap doesn't count
            // switch letters
            shift = wc[i];
            wc[i] = wc[i + 1];
            wc[i + 1] = shift;
            searched = new String(wc);

            searcher = getCursor_findWord(searched);
            if (searcher.getCount() > 0) {
                searcher.moveToFirst();
                matrixCursor.addRow(get_CursorRow(searcher));
            }
            if (LexData.getMaxList() > 0)
                if (matrixCursor.getCount() == LexData.getMaxList())
                    break;
        }
        return matrixCursor;
    }
    public Cursor getCursor_subwords(int min, int max, String term) {
        String searched;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        Cursor searcher;
        for (int len = min; len <= max; len++) {
//            for (int i = 0; i < term.length() - len; i++) { // foreach beginning index

//        for (int len = 2; len < term.length(); len++) {
            for (int i = 0; i <= term.length() - len; i++) { // foreach beginning index
                searched = term.substring(i, i + len);
                searcher = getCursor_findWord(searched); // test word(index, index + length)
                if (searcher.getCount() > 0) {
                    searcher.moveToFirst();
                    matrixCursor.addRow(get_CursorRow(searcher));
                }
            }
        }
        return matrixCursor;
    }
    public Cursor getCursor_withDef(String term) {
        String lenFilter = String.format("Length(Word) <= %1$s ", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                " WHERE (" + lenFilter +
//                filters +
                " AND WordID IN \n" +
                "(Select WordID as id from tblWordDefinitions where tblWordDefinitions.DefinitionID  IN \n" +
                "(Select DefinitionID from tblDefinitions where UPPER(tblDefinitions.Definition) LIKE '%" + term.toUpperCase() + "%')) \n" +
                " ) " , null);

        return cursor;
    }
    public Cursor getCursor_splitters(int min, int max, String letters, String filters, String ordering, boolean rack) {

        if (max > LexData.getMaxLength())
            max = LexData.getMaxLength();
        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", max, min);

        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                " WHERE (" + lenFilter +
                filters +
                " AND Word LIKE '%" + letters + "%'" +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        String front, back;
        Log.i("Letters", letters);
        while (cursor.moveToNext()) {
            String text = cursor.getString(cursor.getColumnIndex("Word"));

//            Log.i("Insert", text );
            // LOOK FOR 0 BASED INDEX OF INSERTED LETTER
            for (int i = 2; i < (text.length() - letters.length()) ; i++)
            {
//                Log.i("Insert", text.substring(i, i + letters.length())  );
                if (text.substring(i, i + letters.length()).equals( letters))
                {
                    front = text.substring(0, i );
//                    Log.i("Front", front );
                    back = text.substring(i + letters.length());
//                    Log.i("Back", back );

                    if (wordJudge(front)) {
//                        back = text.substring(i + len + 1);
                        if (wordJudge(back)) {
                            // replace letter with lower case
                            String finding = front + letters.toLowerCase() + back;
//                            Log.i("Finding", finding );
                            String [] columnValues = new String[11];
                            for (int c = 0; c < 11; c++)
                                columnValues[c] = cursor.getString(c);
                            columnValues[cursor.getColumnIndex("Word")] = finding;
                            matrixCursor.addRow(columnValues);
                            if (!rack)
                                if (LexData.getMaxList() > 0)
                                    if (matrixCursor.getCount() == LexData.getMaxList())
                                        break;
                        }
                    }
                }
            }

        }
        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_stretches(String word, boolean rack) {
        String searched;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        Cursor searcher;

        for (int i = 1; i < word.length(); i++) {
            for (int j = 0; j < 26; j++) {
                if (!rack)
                    if (LexData.getMaxList() > 0)
                        if (matrixCursor.getCount() == LexData.getMaxList())
                            break;
                String f = word.substring(0, i);
                String b = word.substring(i);
                searched = f + (char) (j + 'A') + b;
                searcher = getCursor_findWord(searched); // test word(index, index + length)
                if (searcher.getCount() > 0) {
                    searcher.moveToFirst();
                    matrixCursor.addRow(get_CursorRow(searcher));
                }
            }
        }
        return matrixCursor;
    }
    public Cursor getCursor_HighPlays(String filters, String ordering,  String including, int length, int limit, int offset, boolean rack) {
        int skips = 0;
        String lenFilter = String.format("Length(Word) == %1$s", length);
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (cursor.moveToNext()) {
            String b = cursor.getString(cursor.getColumnIndex("Word"));
//            char first = b.charAt(0);
//            char last = b.charAt(length - 1);
//            if ( (including.indexOf(first) >= 0)
//                    || (including.indexOf(last) >= 0))
            if (including.indexOf(b.charAt(0)) >= 0
                    || including.indexOf(b.charAt(length - 1)) >= 0)

                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;
                    matrixCursor.addRow(get_CursorRow(cursor));
                }
            else
                matrixCursor.addRow(get_CursorRow(cursor));

        }
        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_gethashooks(String filters, String ordering, int limit, int offset, boolean rack) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
//        if (limit == 0)
//            limit = LexData.getMaxList();
        int skips = 0;
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +

                " AND (Trim(FrontHooks) <> '' OR Trim(BackHooks) <> '' ) " +

                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (cursor.moveToNext()) {
//            String fronthooks = cursor.getString(cursor.getColumnIndex("FrontHooks"));
//            String backhooks = cursor.getString(cursor.getColumnIndex("BackHooks"));
//
//            if (fronthooks.trim().length() > 0 || backhooks.trim().length() > 0)

                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;
                    matrixCursor.addRow(get_CursorRow(cursor));
                }


//            if (!rack) {
//                int position = matrixCursor.getCount();
//                if (position < offset - skips) {
//                    skips++;
//                    continue;
//                }
//                if (limit > 0)
//                    if (position == limit)
//                        break;
//                matrixCursor.addRow(get_CursorRow(cursor));
//            }
//

        }
        cursor.close();
        return matrixCursor;
    }



    // MOVED TO searchThread in AltSearchActivity
    public Cursor getCursor_superanagrams(String term, String filters, String ordering, boolean rack) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());

        if (term.trim().length() < 2)
            return null;
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");
        StringBuilder speedFilter = new StringBuilder();
        if (alpha.length() != 0) {
            for (int letter = 0; letter < alpha.length(); letter++)
                speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
        }
        // LIKE filters the initial search by the first letter
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                " WHERE (" + lenFilter +
                speedFilter +
                filters +
                " ) " + ordering, null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] b = word.toCharArray();
            if (containsall(first, b, blankcount)) {
                matrixCursor.addRow(get_CursorRow(cursor));
                if (!rack)
                    if (LexData.getMaxList() > 0)
                        if (matrixCursor.getCount() == LexData.getMaxList())
                            break;
            }
        }
        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_vowelheavy(String filters, String ordering, boolean rack) {
/*
            const string vowels = "AEIOU";
            List<HookSet> results = new List<HookSet>();
            string filter = String.Format("Len(Word) < {0}", Flags.lenLimit + 1);
            foreach (DataRow dr in hayfield.tblLexiconWords.Select(filter))  // traverse dataset
            {
                int vowelcount = 0;
                string b = dr["Word"].ToString();
                vowelcount = b.Count(chr => vowels.Contains(chr));
                int percentage = (100 * vowelcount) / b.Length;
                if ((percentage > 74 && b.Length > 2) || (percentage > 61 && b.Length > 3) || (percentage > 58 && b.Length > 7))
                    results.Add(Conversions.DataRowToHookSet(dr, hayfield));
            }
            results = results.Cast<HookSet>().OrderBy(item => item.Word.Length).ThenBy(item => item.Word.ToString()).ToList();
            if (Flags.listLimited)
                return Limits.get_LimitedResults(results);
            return (results);
        */
// only difference between this and anagram is removing the length filter
        //String filter = String.format("Length(Word) = %1$s", term.length());
        // anagrams are all the same length

// TODO       string filter = String.Format("Len(Word) < {0}", Flags.lenLimit + 1);
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        if (filters == "")
            filters = "1";
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (precursor.moveToNext()) {
            String word = precursor.getString(precursor.getColumnIndex("Word"));
            int vowelcount = 0;
            int size = word.length();
            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            //vowelcount = word.Count(chr => vowels.Contains(chr));
            int percentage = (100 * vowelcount) / size;
            if ((percentage > 74 && size > 2) || (percentage > 61 && size > 3) || (percentage > 58 && size > 7))
                matrixCursor.addRow(get_CursorRow(precursor));
            if (!rack)
                if (LexData.getMaxList() > 0)
                    if (matrixCursor.getCount() == LexData.getMaxList())
                        break;
        }

        precursor.close();
        return matrixCursor;
// TODO        results = results.Cast<HookSet>().OrderBy(item => item.Word.Length).ThenBy(item => item.Word.ToString()).ToList();
//        if (Flags.listLimited)
//            return Limits.get_LimitedResults(results);*/
    }
    public Cursor getCursor_consonantDumps(String filters, String ordering, boolean rack) {
// TODO       string filter = String.Format("Len(Word) < {0}", Flags.lenLimit + 1);
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        if (filters == "")
            filters = "1";
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (precursor.moveToNext()) {
            String word = precursor.getString(precursor.getColumnIndex("Word"));
            int vowelcount = 0;
            int size = word.length();

            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            int percentage = (100 * vowelcount) / size;
            if (percentage < 18)
                matrixCursor.addRow(get_CursorRow(precursor));
            if (!rack)
                if (LexData.getMaxList() > 0)
                    if (matrixCursor.getCount() == LexData.getMaxList())
                        break;

        }

        precursor.close();
        return matrixCursor;
// TODO        results = results.Cast<HookSet>().OrderBy(item => item.Word.Length).ThenBy(item => item.Word.ToString()).ToList();
    }
    public Cursor getCursor_getpalins(String filters, String ordering, boolean rack) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        boolean palin;
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (precursor.moveToNext()) {
            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
            //String word = cursor.getString(cursor.getColumnIndex("Word"));
            palin = true;
            char[] b = word.toCharArray();
            for (int i = 0; i < b.length / 2; i++) {
                if (b[i] != b[b.length - i - 1])
                    palin = false;
            }
            if (palin == true)
                matrixCursor.addRow(get_CursorRow(precursor));
            if (!rack)
                if (LexData.getMaxList() > 0)
                    if (matrixCursor.getCount() == LexData.getMaxList())
                        break;

        }
        precursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_getnohooks(String filters, String ordering, int limit, int offset, boolean rack) {
        int skips = 0;
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +

                " AND Trim(FrontHooks) = '' AND Trim(BackHooks) = '' " +

                filters +
                " ) " + ordering, null);

        if ((rack) || (offset + limit == 0))
            return cursor;

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        while (cursor.moveToNext()) {
            if (!rack) {
                if (offset > skips) {
                    skips++;
                    continue;
                }
                int position = matrixCursor.getCount();
                if (limit > 0)
                    if (position == limit)
                        break;
            }
            matrixCursor.addRow(get_CursorRow(cursor));

        }
        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_getsemos(String filters, String ordering, boolean rack) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        boolean palin;
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
        while (cursor.moveToNext()) {
            String primary = cursor.getString(1);

            StringBuilder builder = new StringBuilder();

            // append a string into StringBuilder input1
            builder.append(primary);

            // reverse StringBuilder input1
            String backward = builder.reverse().toString();

/*            String backward = "";
            char[] b = primary.toCharArray();

            for (int i = 0; i < b.length / 2; i++)
            {
                char front = b[i];
                b[i] = b[(b.length - 1) - i];
                b[(b.length - 1) - i] = front;
            }
            backward = String.valueOf(b);*/
//            Log.i("Semo", backward);
            if (wordJudge(backward))
                matrixCursor.addRow(get_CursorRow(cursor));
            if (!rack)
                if (LexData.getMaxList() > 0)
                    if (matrixCursor.getCount() == LexData.getMaxList())
                        break;

        }
        cursor.close();
        return matrixCursor;
    }
    public Cursor getCursor_Stems(int type, int count) {
/*        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
        "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                filters +
                " ) " + ordering, null);
*/

        Cursor langCursor = database.rawQuery("SELECT LexLanguage from tblLexicons WHERE tblLexicons.LexiconName = '" + LexData.getLexName() + "' ", null);
        langCursor.moveToFirst();
        String lang = langCursor.getString(langCursor.getColumnIndex("LexLanguage"));
        langCursor.close();

        Cursor cursor = database.rawQuery("SELECT StemID as _id, Stem as Word, StemID as WordID, " +
                "'' as FrontHooks, '' as BackHooks, " +
                "'' as InnerFront, '' as InnerBack, 0 as Anagrams, 0 as ProbFactor, 0 as OPlayFactor, 0 as Score " +
                " FROM tblStems " +
                " WHERE (StemType = " + String.valueOf(type) + ")" +
                " AND (StemLanguage = '" + lang + "')" +
                " AND (Length(Word) = " + String.valueOf(count) +
                ")", null);
        return cursor;
    }
    public Cursor getCursor_subjectList(String listName) {
        LexData.WordList list = new LexData.WordList(); // not assigned to LexData

        if (list.equals(""))
            return null;

        Cursor cursor = database.rawQuery("SELECT * FROM WordLists WHERE UPPER(ListName) = '" + listName.toUpperCase() + "';", null);
        return cursor;
    }
    public Cursor getCursor_Subjects(String category) {
        String sqlString = "SELECT ListID as _id, Upper(ListName) as Word, ListID as WordID, " +
                "'' as FrontHooks, '' as BackHooks, " +
                "'' as InnerFront, '' as InnerBack, 0 as Anagrams, 0 as ProbFactor, 0 as OPlayFactor, 0 as Score " +
                "FROM WordLists INNER JOIN tblListCategories on WordLists.CategoryID = tblListCategories.CategoryID " +
                "WHERE (tblListCategories.Category = '" + category + "'" +
                ")";
        Log.d("getC", sqlString);
        Cursor cursor = database.rawQuery("SELECT ListID as _id, Upper(ListName) as Word, ListID as WordID, " +
                "'' as FrontHooks, '' as BackHooks, " +
                "'' as InnerFront, '' as InnerBack, 0 as Anagrams, 0 as ProbFactor, 0 as OPlayFactor, 0 as Score " +
                "FROM WordLists INNER JOIN tblListCategories on WordLists.CategoryID = tblListCategories.CategoryID " +
                "WHERE (tblListCategories.Category = '" + category + "'" +
                ")", null);

        return cursor;
    }
    public Cursor getCursor_listwords(int ListID, String filters, String ordering) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String sql = "SELECT `" + LexData.getLexName() + "`.WordID as _id, " +
                "Word, `" + LexData.getLexName() + "`.WordID, " +
                "FrontHooks, BackHooks, InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                " FROM     `" + LexData.getLexName() + "` \n" +
                " INNER JOIN WordList on `" + LexData.getLexName() + "`.WordID = WordList.WordID " +

                " WHERE (" + lenFilter +
                filters +
                " AND WordList.ListID = " + ListID + "" +
                ")" + ordering;
        Log.i("List Words", sql);
        Cursor cursor = database.rawQuery("SELECT `" + LexData.getLexName() + "`.WordID as _id, " +
                "Word, `" + LexData.getLexName() + "`.WordID, " +
                "FrontHooks, BackHooks, InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                " FROM     `" + LexData.getLexName() + "` \n" +
                " INNER JOIN WordList on `" + LexData.getLexName() + "`.WordID = WordList.WordID " +

                " WHERE (" + lenFilter +
                filters +
                " AND WordList.ListID = " + ListID + "" +
                ")" + ordering, null);

        return cursor;
    }
    public Cursor getCursor_listwords(String listName, String filters, String ordering) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String sql = "SELECT `" + LexData.getLexName() + "`.WordID as _id, " +
                "Word, `" + LexData.getLexName() + "`.WordID, " +
                "FrontHooks, BackHooks, InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                " FROM     `" + LexData.getLexName() + "` \n" +
                " INNER JOIN WordList on `" + LexData.getLexName() + "`.WordID = WordList.WordID " +

                " WHERE (" + lenFilter +
                filters +
                " AND WordLists.ListID = '" + listName + "'" +
                ")" + ordering;
        Log.i("List Words", sql);
        Cursor cursor = database.rawQuery("SELECT `" + LexData.getLexName() + "`.WordID as _id, " +
                "Word, `" + LexData.getLexName() + "`.WordID, " +
                "FrontHooks, BackHooks, InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                " FROM     `" + LexData.getLexName() + "` \n" +
                " INNER JOIN WordList on `" + LexData.getLexName() + "`.WordID = WordList.WordID " +

                " WHERE (" + lenFilter +
                filters +
                " AND WordLists.ListName = '" + listName + "'" +
                ")" + ordering, null);

        return cursor;
    }
    public Cursor getCursor_findWord(String term) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word = '" + term +
                "' )", null);
        return cursor;
    }




    // SPECIAL SEARCH OPTIONS
    public ArrayList<LexData.WordList> get_subjectLists() {
        ArrayList<LexData.WordList> lists = new ArrayList<>();
        open();
        Cursor cursor = database.rawQuery("SELECT * FROM WordLists ORDER BY CategoryID", null);

        while (cursor.moveToNext()) {
            LexData.WordList list = new LexData.WordList();
            list.ListID = cursor.getInt(cursor.getColumnIndex("ListID"));
            list.ListName = cursor.getString(cursor.getColumnIndex("ListName"));
            list.ListAuthor = cursor.getString(cursor.getColumnIndex("ListAuthor"));
            list.ListCredits = cursor.getString(cursor.getColumnIndex("ListCredits"));
            list.ListDescription = cursor.getString(cursor.getColumnIndex("ListDescription"));
            list.ListSource = cursor.getString(cursor.getColumnIndex("ListSource"));
            list.ListLink = cursor.getString(cursor.getColumnIndex("ListLink"));
            list.ListAuthorEmail = cursor.getString(cursor.getColumnIndex("ListAuthorEmail"));
            list.CategoryID = cursor.getInt(cursor.getColumnIndex("CategoryID"));
            lists.add(list);
        }
        cursor.close();
        close();
        return lists;
    }
    public LexData.WordList get_subjectList(String listName) {
        LexData.WordList list = new LexData.WordList(); // not assigned to LexData

        if (list.equals(""))
            return null;

        Cursor cursor = database.rawQuery("SELECT * FROM WordLists", null);
        while (cursor.moveToNext()) {
            String ListName = cursor.getString(cursor.getColumnIndex("ListName"));
            if (ListName.equals(listName)) {
                list.ListID = cursor.getInt(cursor.getColumnIndex("ListID"));
                list.ListName = cursor.getString(cursor.getColumnIndex("ListName"));
                list.ListAuthor = cursor.getString(cursor.getColumnIndex("ListAuthor"));
                list.ListCredits = cursor.getString(cursor.getColumnIndex("ListCredits"));
                list.ListDescription = cursor.getString(cursor.getColumnIndex("ListDescription"));
                list.ListSource = cursor.getString(cursor.getColumnIndex("ListSource"));
                list.ListLink = cursor.getString(cursor.getColumnIndex("ListLink"));
                list.ListAuthorEmail = cursor.getString(cursor.getColumnIndex("ListAuthorEmail"));
                list.CategoryID = cursor.getInt(cursor.getColumnIndex("CategoryID"));
            }
        }
        cursor.close();
        close();
        return list;
    }
    public int getListID(String listname) {
        // called when db is open
//        Cursor cursor = database.rawQuery("Select ListID, ListName from WordLists where ListName = '" + listname + "'", null);
        Log.d("listname", listname);
        String sql = "Select ListID from WordLists where Upper(ListName) = '" + listname.toUpperCase() + "'";
        Log.d("listname", sql);
        Cursor cursor = database.rawQuery("Select ListID from WordLists where Upper(ListName) = '" + listname.toUpperCase() + "'", null);
        int listid = 0;
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            listid = cursor.getInt(cursor.getColumnIndex("ListID"));
        }
        cursor.close();
        return listid;
    }
    public String getListName(int listID) {
        // called when db is open
//        Cursor cursor = database.rawQuery("Select ListID, ListName from WordLists where ListName = '" + listname + "'", null);
        Cursor cursor = database.rawQuery("Select ListID, ListName from WordLists where ListID = '" + listID + "'", null);
        String listName = "";
        if(cursor.getCount() > 0) {
            cursor.moveToFirst();
            listName = cursor.getString(cursor.getColumnIndex("ListName"));
        }
        return listName;
    }


    // UNUSED
    public Cursor isPrefix(String letters, String filters, String ordering, int limit, int offset, boolean rack) {
        // SHOULD SIZE ADJUST TO WORDS RETURNED search size + letters

        int skips = 0;
        if (letters.trim() == "")
            return null;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '" + letters + "%' " +
                " AND " + lenFilter +
                filters +
                " ) " + ordering, null);


//        int fhsize;
        String fhooked;
        Cursor searcher;
        while (precursor.moveToNext()) {

            String word = precursor.getString(1);
//            fhsize = word.length() - letters.length();
            fhooked = word.substring(letters.length()); // word without prefix

            if (wordJudge(fhooked)) {

                searcher = getCursor_findWord(fhooked); // test word(index, index + length)
                searcher.moveToFirst();


//                result.Fhooks = finding.Fhooks + "(" + letters.ToLower() + ")";

                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;
                    matrixCursor.addRow(get_CursorRow(searcher));
                } else
                    matrixCursor.addRow(get_CursorRow(searcher));
            }
        }

        precursor.close();
        return matrixCursor;
    }
    public Cursor isSuffix(String letters, String filters, String ordering, int limit, int offset, boolean rack) {
        int skips = 0;
        if (letters.trim() == "")
            return null;
        int suffixSize = letters.length();
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '%" + letters + "' " +
                " AND " + lenFilter +
                filters +
                " ) " + ordering, null);

        int bhsize;
        String bhooked;
        Cursor searcher;
        while (precursor.moveToNext()) {

            String word = precursor.getString(1);
            bhsize = word.length() - suffixSize;
            bhooked = word.substring(0, bhsize); // word without suffix

            if (wordJudge(bhooked)) {

                searcher = getCursor_findWord(bhooked); // test word(index, index + length)
                searcher.moveToFirst();
//                result.Fhooks = finding.Fhooks + "(" + letters.ToLower() + ")";

                if (!rack) {
                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                    int position = matrixCursor.getCount();
                    if (limit > 0)
                        if (position == limit)
                            break;
                    matrixCursor.addRow(get_CursorRow(searcher));
                } else
                    matrixCursor.addRow(get_CursorRow(searcher));
            }
        }

        precursor.close();
        return matrixCursor;
    }

    // FILTERING
    private boolean rackfilter = false;
    public Cursor getCursor_rackfilter(String term, Cursor source, int limit, int offset) {
        // term is rack (and term if checked)
        if (source.getCount() == 0)
            return source;
        int skips = 0;
        int position = 0;
        term = term.trim();

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        // only difference between this and anagram is changing the length filter
        char[] a = term.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        for (c = 0; c < a.length; c++) { // initialize word to anagram
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");
        StringBuilder speedFilter = new StringBuilder();
        if (alpha.length() != 0) {
            speedFilter.append(" AND (Word LIKE '%" + alpha.substring(0, 1) + "%' "); // first letter
            for (int letter = 1; letter < alpha.length(); letter++)
                speedFilter.append(" OR Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
            speedFilter.append(") ");
        }

        while (source.moveToNext()) {
            String word = source.getString(1).toUpperCase();
            char[] anagram = word.toCharArray();



            if (isAnagram(first, anagram, blankcount)) {

                    if (offset > skips) {
                        skips++;
                        continue;
                    }
                Log.d("Values", position + "p"+ offset + "o" +  skips + "s");

                    if (LexData.getColorBlanks()) {
                        int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        int letter;
                        for (int x = 0; x < 26; x++)
                            blanks[x] = 0;
                        for (c = 0; c < anagram.length; c++) {
                            letter = ++second[anagram[c] - 'A'];
                            if (letter > first[anagram[c] - 'A']) {
                                blanks[anagram[c] - 'A']++;
                            }
                        }
                        matrixCursor.addRow(get_BlankCursorRow(source, blanks));
                    } else
                        matrixCursor.addRow(get_CursorRow(source));

// write THEN compare
                position = matrixCursor.getCount();
                if (limit > 0)
                    if (position == limit)
                        break;






//                if (LexData.getColorBlanks()) {
//                    int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//                    char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//                    int letter;
//                    for (int x = 0; x < 26; x++)
//                        blanks[x] = 0;
//                    for (c = 0; c < anagram.length; c++) {
//                        letter = ++second[anagram[c] - 'A'];
//                        if (letter > first[anagram[c] - 'A']) {
//                            blanks[anagram[c] - 'A']++;
//                        }
//                    }
//                    matrixCursor.addRow(get_BlankCursorRow(source, blanks));
//                } else
//                    matrixCursor.addRow(get_CursorRow(source));


            }


//            if (LexData.getMaxList() > 0)
//                if (matrixCursor.getCount() == LexData.getMaxList())
//                    break;
        }

        source.close();
        return matrixCursor;
    }
    public Cursor get_simple_pattern(String pattern) {
        String exp = "^" + pattern.replace('?', '.') + "$";
        exp = Utils.validateString(exp); // removes reserved characters
        exp = exp.toUpperCase();

        MatrixCursor wordCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID"});

        String lenFilter = String.format("Length(Word) = %1$s", pattern.length());

        String sql = "SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) ";
        Cursor precursor = database.rawQuery(sql, null);

        try {
            Pattern.compile(exp);
        } catch (PatternSyntaxException e) {

        }

        try {
            while (precursor.moveToNext()) {
                String word = precursor.getString(1);
                if (word.matches(exp)) {
                    wordCursor.addRow(get_CursorWord(precursor));
                }
            }
        } catch (Exception e) {

        }
        precursor.close();
        return wordCursor;
    }
    private static String[] get_CursorWord (Cursor cursor) {
        String [] columnValues = new String[3];
        for (int c = 0; c < 3; c++)
            columnValues[c] = cursor.getString(c);
        return columnValues;
    }
    public String buildInnerPattern(String pattern) {
        String exp = pattern;
        if(exp.contains("<"))

        {
            Pattern p = Pattern.compile("\\<(.*?)\\>");
            Matcher m = p.matcher(exp);
            do {
                while (m.find()) {
                    //System.out.println(m.group(1));
                    String answer = m.group(1);
                    String lettersmatching = get_ReplacementLetters(answer);
                    String original = "<" + answer + ">";
                    exp = exp.replace(original, lettersmatching);
                }

            } while (exp.contains("<") & exp.contains(">"));
        }
        else exp.replace(">","");
        return exp;
    }
    public String get_ReplacementLetters(String pattern) {
        StringBuilder stringBuilder = new StringBuilder();
        String letters = "";
        Cursor results = get_simple_pattern(pattern);
Log.e("Pattern", pattern);

        try {
            while (results.moveToNext()) {
                String word = results.getString(1);
//Log.e("Word", word);
                int pos = pattern.indexOf('?');
                String letter = word.substring(pos, pos+1);
                stringBuilder.append(letter);
            }
        } catch (Exception e) {

        }
        results.close();

        letters = stringBuilder.toString();
        if (letters.length() > 0)
            return ("[" + letters + "]");
        else
            return ("XXX");
    }

    public Cursor getCursor_getWords(String list, String ordering, String limits, String filters) {
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE Word IN (" + list +
                " ) " + ordering + limits + filters, null);
//                ") order by Word " + limits, null);
        return cursor;
    }


    ////// POWER SEARCH METHODS
    // ordering only in Cursor on last search

    // also used by Card box
    public Cursor getCursorFromWords(String list, String ordering) {
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE Word IN (" + list +
                " ) " + ordering, null);
        return cursor;
    }
    public List<String> wordsFromCursor(Cursor cursor) {
        List<String> words = new ArrayList<>();
//        cursor.moveToFirst();
        int column = cursor.getColumnIndex("Word");

        while (cursor.moveToNext()) {
            String word = cursor.getString(column).toUpperCase();
            words.add(word);
        }
        return (words);
    }




    // CRITERIA SHOULD RECEIVE PREVIOUS LIST, Primary doesn't, some are criteria only
    public List<String> getList_anagrams(String term) {
        int skips = 0;

        //        if (limits == "")
//            limits = limitString(LexData.getMaxList());
        if (term.contains("*") || term.contains("@"))
        {
            term = term.replaceAll("[*@]", "");
//            return getCursor_superanagrams(term, filters, ordering, rack);
        }
        // anagrams are all the same length
        if (term.trim().length() < 2)
            return null;
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");
        StringBuilder speedFilter = new StringBuilder();

        if (alpha.length() != 0) {
            for (int letter = 0; letter < alpha.length() && letter < 3; letter++)
                speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
        }
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String filter = String.format("Length(Word) = %1$s", term.length());
        Cursor cursor = database.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + filter +
                speedFilter +
                " ) " , null);

        List<String> wordsFound = new ArrayList<>();

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram, blankcount)) {
                wordsFound.add(word);
            }
        }
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_anagrams(String term, List<String> list) {
        if (term.contains("*") || term.contains("@"))
        {
            term = term.replaceAll("[*@]", "");
// todo return getList_superanagrams when done
            //            return getCursor_superanagrams(term, filters, ordering, rack);
        }
        if (term.trim().length() < 2)
            return null;

        char[] a = term.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        List<String> wordsFound = new ArrayList<>();

        for (String word : list) {
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram, blankcount)) {
                if (term.length() == word.length()) // MUST Be SAME LENGTH
                    wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_BetweenLengths(int least, int most) {
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Length(Word) >= " + String.valueOf(least) +
                " AND Length(Word) <= " + String.valueOf(most) +
                " ) " , null);

        return (wordsFromCursor(cursor));
        // todo need to close cursor??????????????
    }
    public List<String> getList_SubjectList(String listName) {
        int listID = getListID(listName);


        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String sql = "SELECT `" + LexData.getLexName() + "`.WordID as _id, " +
                "Word, `" + LexData.getLexName() + "`.WordID, " +
                "FrontHooks, BackHooks, InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                " FROM     `" + LexData.getLexName() + "` \n" +
                " INNER JOIN WordList on `" + LexData.getLexName() + "`.WordID = WordList.WordID " +
                " WHERE WordList.ListID = " + listID + ";";
        Log.i("List Words", sql);
        Cursor cursor = database.rawQuery(sql, null);

        return (wordsFromCursor(cursor));
    }
    public List<String> getList_SubjectList(String listName, List<String> list) {
        int listID = getListID(listName);
//process list
//test
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String sql = "SELECT `" + LexData.getLexName() + "`.WordID as _id, " +
                "Word, `" + LexData.getLexName() + "`.WordID, " +
                "FrontHooks, BackHooks, InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                " FROM     `" + LexData.getLexName() + "` \n" +
                " INNER JOIN WordList on `" + LexData.getLexName() + "`.WordID = WordList.WordID " +
                " WHERE WordList.ListID = " + listID + ";";
        Log.i("List Words", sql);
        Cursor cursor = database.rawQuery(sql, null);

        List<String> first = wordsFromCursor(cursor);

        List<String> wordsFound = new ArrayList<>();

        for(String word : list) {
            if (first.contains(word))
                wordsFound.add(word);
        }
        return wordsFound;

    }

    public List<String> getList_BetweenLengths(int least, int most, List<String> list) {
        if (least < 2 || most < 2)
            return null;
        List<String> wordsFound = new ArrayList<>();
        for (String word : list) {
            if (word.length() >= least && word.length() <= most) {
                wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_gethashooks() {
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Trim(FrontHooks) <> '' OR Trim(BackHooks) <> '' ) " +
                " ) " , null);
        return (wordsFromCursor(cursor));
    }
    public List<String> getList_gethashooks(List<String> list) {
        if (list.isEmpty())
            return null;

        StringBuilder textlist = new StringBuilder();

        textlist.append("'" + list.get(0)+ "'");
        for(int c = 1; c < list.size(); c++) {
            textlist.append(", '" + list.get(c) + "'");
        }

        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE Word IN (" + textlist + " )" +
                "AND (Trim(FrontHooks) <> '' OR Trim(BackHooks) <> ''  " +
                " ) " , null);

        return wordsFromCursor(cursor);
    }
    public List<String> getList_contains(String term) {
        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " AND " + "Word LIKE '%" + term + "%' " +
                " ) " , null);
        return wordsFromCursor(cursor);
    }
    public List<String> getList_contains(String term, List<String> list) {
        if (term.isEmpty())
            return null;
        List<String> wordsFound = new ArrayList<>();

        for (String word : list) {
            if (word.contains(term))
                wordsFound.add(word);
        }
        return wordsFound;
    }
    public List<String> getList_subanagrams(String term) {
        if (term.trim().length() < 2)
            return null;
        Cursor cursor;
        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) <= %2$s", LexData.getMaxLength(), term.length());

        // only difference between this and anagram is changing the length filter
        char[] a = term.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        for (c = 0; c < a.length; c++) { // initialize word to anagram
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");


        StringBuilder speedFilter = new StringBuilder();
        if (alpha.length() != 0) {
            speedFilter.append(" AND (Word LIKE '%" + alpha.substring(0, 1) + "%' "); // first letter
            for (int letter = 1; letter < alpha.length(); letter++)
                speedFilter.append(" OR Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
            speedFilter.append(") ");

        }

        cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                speedFilter +
                " ) " , null);

        List<String> wordsFound = new ArrayList<>();

        while (cursor.moveToNext()) {
            String word = cursor.getString(0);
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram, blankcount)) {
                wordsFound.add(word);
            }
        }
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_subanagrams(String term, List<String> list) {
        if (term.trim().length() < 2)
            return null;

        char[] a = term.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        for (c = 0; c < a.length; c++) { // initialize word to anagram
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        List<String> wordsFound = new ArrayList<>();

        for (String word : list) {
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram, blankcount)) {
                wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_superanagrams(String term) {
        String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());

        if (term.trim().length() < 2)
            return null;
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String alpha = term.replaceAll("[^A-Za-z]+", "");
        StringBuilder speedFilter = new StringBuilder();
        if (alpha.length() != 0) {
            for (int letter = 0; letter < alpha.length(); letter++)
                speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");
        }
        // LIKE filters the initial search by the first letter
        Cursor cursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                " WHERE (" + lenFilter +
                speedFilter +
                " ) " , null);

        List<String> wordsFound = new ArrayList<>();

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] b = word.toCharArray();
            if (containsall(first, b, blankcount)) {
                wordsFound.add(word);
            }
        }
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_superanagrams(String term, List<String> list) {
        if (term.trim().length() < 2)
            return null;
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }


        List<String> wordsFound = new ArrayList<>();

        for (String word : list) {
            char[] b = word.toCharArray();
            if (containsall(first, b, blankcount)) {
                wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_containsAny(String letters) {
        if (letters.trim().equals(""))
            return null;

        char[] a = letters.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }

        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) ", null);

        List<String> wordsFound = new ArrayList<>();

        while (precursor.moveToNext()) {
            String word = precursor.getString(1);
            char[] b = word.toCharArray();

            if (containsany(first, b)) {
                wordsFound.add(word);
            }
        }



        precursor.close();
        return wordsFound;

    }
    public List<String> getList_containsAny(String letters, List<String> list) {
        if (letters.trim().equals(""))
            return null;

        char[] a = letters.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position
        int blankcount = 0;

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            if (a[c] == '?') {
                blankcount++;
                continue;
            }
            first[a[c] - 'A']++;
        }


        List<String> wordsFound = new ArrayList<>();

        for(String word : list) {
            char[] b = word.toCharArray();

            if (containsany(first, b)) {
                wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_begins(String term) {
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '" + term + "%' )", null);

        List<String> wordsFound = new ArrayList<>();

        while (cursor.moveToNext()) {
            String word = cursor.getString(0);
            wordsFound.add(word);
        }

        cursor.close();
        return wordsFound;
    }
    public List<String> getList_begins(String term, List<String> list) {
        if (term.isEmpty())
            return null;
        List<String> wordsFound = new ArrayList<>();

        for(String word : list) {
            if (word.startsWith(term)) {
                wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_ends(String term) {
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '%" + term + "' )", null);

        List<String> wordsFound = new ArrayList<>();

        while (cursor.moveToNext()) {
            String word = cursor.getString(0);
            wordsFound.add(word);
        }

        cursor.close();
        return wordsFound;
    }
    public List<String> getList_ends(String term, List<String> list) {
        if (term.isEmpty())
            return null;
        List<String> wordsFound = new ArrayList<>();

        for(String word : list) {
            if (word.endsWith(term)) {
                wordsFound.add(word);
            }
        }
        return wordsFound;
    }
    public List<String> getList_takesPrefix(String term) {
        if (term.trim().equals(""))
            return null;

        List<String> wordsFound = new ArrayList<>();

        // single letter just search for hooks
        Cursor precursor;
        if (term.trim().length() == 1) {
            precursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE (FrontHooks LIKE '%" + term.toLowerCase() + "%'  ) ", null);

            while (precursor.moveToNext()) {
                String word = precursor.getString(0);
                wordsFound.add(word);
            }

            precursor.close();
            return wordsFound;
        }

        else {
            precursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE (" + "Word LIKE '" + term  + "%'  ) ", null);
            Cursor searcher;
            String fhooked;

            while (precursor.moveToNext()) {
                String word = precursor.getString(0);
                fhooked = word.substring(term.length()); // word without prefix

                searcher = getCursor_findWord(fhooked); // test word(index, index + length)
                if (searcher.getCount() > 0) {
                    wordsFound.add(fhooked);
                }
            }

            precursor.close();
            return wordsFound;
        }
    }
    public List<String> getList_takesPrefix(String term, List<String> list) {
        if (term.trim().equals(""))
            return null;

        List<String> wordsFound = new ArrayList<>();
        Cursor precursor;

        // in either case, create IN list
        StringBuilder inputList = new StringBuilder();
        inputList.append("'" + list.get(0)+ "'");
        for(int c = 1; c < list.size(); c++) {
            inputList.append(", '" + list.get(c) + "'");
        }

        if (term.trim().length() == 1) {
            precursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE Word IN (" + inputList + " )" +
                    "AND (FrontHooks LIKE '%" + term.toLowerCase() + "%'  ) ", null);

            while (precursor.moveToNext()) {
                String word = precursor.getString(0);
                wordsFound.add(word);
            }

            precursor.close();
            return wordsFound;
        }
        else {

        int prefixLen = term.length() + 1;
        Cursor postcursor = database.rawQuery("SELECT Substr(Word, " + prefixLen  + ") AS Prospect "  +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '" + term  + "%'  ) " +
                " AND Prospect IN (" + inputList + " )", null);

            while (postcursor.moveToNext()) {
                String word = postcursor.getString(0);
                wordsFound.add(word);
            }
            postcursor.close();
        }
        return wordsFound;
    }
    public List<String> getList_takesSuffix(String term) {
        if (term.trim().isEmpty())
            return null;

        List<String> wordsFound = new ArrayList<>();

        // single letter just search for hooks
        Cursor precursor;
        int suffixSize = term.length();
        if (term.trim().length() == 1) {
            precursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE (BackHooks LIKE '%" + term.toLowerCase() + "%'  ) ", null);

            while (precursor.moveToNext()) {
                String word = precursor.getString(0);
                wordsFound.add(word);
            }

            precursor.close();
            return wordsFound;
        }

        else {
            precursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE (" + "Word LIKE '%" + term  + "'  ) ", null);
            Cursor searcher;
            String bhooked;
            int bhsize;

            while (precursor.moveToNext()) {
                String word = precursor.getString(0);
                bhsize = word.length() - suffixSize;
                bhooked = word.substring(0, bhsize); // word without suffix

                searcher = getCursor_findWord(bhooked); // test word(index, index + length)
                if (searcher.getCount() > 0) {
                    wordsFound.add(bhooked);
                }
            }

            precursor.close();
            return wordsFound;
        }
    }
    public List<String> getList_takesSuffix(String term, List<String> list) {

        if (term.trim().equals(""))
            return null;

        List<String> wordsFound = new ArrayList<>();
        Cursor precursor;

        // in either case, create IN list
        StringBuilder inputList = new StringBuilder();
        inputList.append("'" + list.get(0)+ "'");
        for(int c = 1; c < list.size(); c++) {
            inputList.append(", '" + list.get(c) + "'");
        }

        if (term.trim().length() == 1) {
            precursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE Word IN (" + inputList + " )" +
                    "AND (BackHooks LIKE '%" + term.toLowerCase() + "%'  ) ", null);

            while (precursor.moveToNext()) {
                String word = precursor.getString(0);
                wordsFound.add(word);
            }

            precursor.close();
            return wordsFound;
        }
        else {

            int prefixLen = term.length() - 1;
            Cursor postcursor = database.rawQuery("SELECT Substr(Word, 0, Length(Word) - " + prefixLen  + ") AS Prospect " +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE (" + "Word LIKE '%" + term  + "'  ) " +
                    " AND Prospect IN (" + inputList + " )", null);

//            SELECT substr(Word, 0, Length(Word) - 2) AS Prospect
//            FROM     `NSWL18`
//            WHERE (Word LIKE '%ING'  )
//            AND Prospect IN ('WAR', 'AIR', 'BUG', 'OOP' )

            while (postcursor.moveToNext()) {
                String word = postcursor.getString(0);
                wordsFound.add(word);
            }
            postcursor.close();
        }
        return wordsFound;



    }
    public List<String> getList_fromfile(Context context, String filespec) {

        List<String> words;

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            try {
                words = Utils.getWordsFromURI(context, filespec);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
            words = Utils.getWordsFromFile(filespec);

        Log.d("words in file", "Total " + words.size());

//        words = Utils.getWordsFromFile(filespec);
//        Log.d("words in file", "Total " + words.size());
//
        return words;

    }
    public List<String> getList_fromfile(Context context, String filespec, List<String> list) {

        List<String> words;

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            try {
                words = Utils.getWordsFromURI(context, filespec);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
            words = Utils.getWordsFromFile(filespec);
        Log.d("words in file", "Total " + words.size());

        List<String> wordsFound = new ArrayList<>();

        for(String word : list) {
            if (words.contains(word))
                wordsFound.add(word);
        }
        return wordsFound;

    }
    public List<String> getList_predefined(int searchType, Context context) {
        String incl;

        // todo get from control, method
//        databaseAccess.open(); // opened in executeSearch
        switch (searchType) {

            case 0:
                return getList_BetweenLengths(2, 2);
            case 1:
                return getList_containsAny(LexData.valueLetters(7,10), 3);
            case 2:
                return getList_BetweenLengths(3, 3);
            case 3:
                return getList_containsAny(LexData.valueLetters(7,10), 4);

            // todo replace including with tileValue
            case 4: //Top BE Fours (JQXZ)
                return getList_HighPlays(LexData.valueLetters(7,10), 4);
            case 5: //High BE Fours
                return getList_HighPlays(LexData.valueLetters(4,6), 4);
            case 6: //Top BE Fives (JQXZ)
                return getList_HighPlays(LexData.valueLetters(7,10), 5);
            case 7: //High BE Fives
                return getList_HighPlays(LexData.valueLetters(4,6), 5);
            case 8: // Vowel Heavy
                return getList_vowelheavy();
            case 9: // Consonant Dump
                return getList_consonantDumps();
            case 10: // Q not U
                return getList_qNotu();
            case 11: // Palins
                return getList_getpalins(); // thread??

            case 12: // Hookless
                return getList_getnohooks();
            case 13: // Unique Hooks
                return getList_uniqueHooks();
            case 14: // Hookless
                return getList_gethashooks();


//
//            case 8: // Vowel Heavy
//            case 9: // Consonant Dumps
//            case 11: // Palindromes
//            case 14: // Hooked
//            case 18: // Alt Ending
//                searchThread(this, "", makefilters(11));
//                return cursor;
//            default:
//                Toast.makeText(this,"Select Search Type to begin", Toast.LENGTH_SHORT).show();
////                Utils.wordDefinition(this, "", databaseAccess.getDefinition(("ERROR")));
//                break;

            default:
                Toast.makeText(context,"Not ready yet", Toast.LENGTH_SHORT).show();
                return null;
//            case 14: // Unique
//            case 15: //

            //Palindromes
            //       Semordnilaps
            //Hookless words

        }
    }
    public List<String> getList_predefined(int searchType, Context context, List<String> list) {
        String incl;

        // todo get from control, method
//        databaseAccess.open(); // opened in executeSearch
        switch (searchType) {

            case 0:
                return getList_BetweenLengths(2, 2, list);
            case 1:
                return getList_containsAny(LexData.valueLetters(7,10), 3, list);
            case 2:
                return getList_BetweenLengths(3, 3, list);
            case 3:
                return getList_containsAny(LexData.valueLetters(7,10), 4, list);

            // todo replace including with tileValue
            case 4: //Top BE Fours (JQXZ)
                return getList_HighPlays(LexData.valueLetters(7,10), 4, list);
            case 5: //High BE Fours
                return getList_HighPlays(LexData.valueLetters(4,6), 4, list);
            case 6: //Top BE Fives (JQXZ)
                return getList_HighPlays(LexData.valueLetters(7,10), 5, list);
            case 7: //High BE Fives
                return getList_HighPlays(LexData.valueLetters(4,6), 5, list);
            case 8: // Vowel Heavy
                return getList_vowelheavy(list);
            case 9:
                return getList_consonantDumps(list);

            case 10: // Q not U
                return getList_qNotu(list);
            case 11: // Palins
                return getList_getpalins(list); // thread??

            case 12: // Hookless
                return getList_getnohooks(list);
            case 13: // Unique Hooks
                return getList_uniqueHooks(list);
            case 14: // hashooks
                return getList_gethashooks(list);


//
//            case 8: // Vowel Heavy
//            case 9: // Consonant Dumps
//            case 11: // Palindromes
//            case 14: // Hooked
//            case 18: // Alt Ending
//                searchThread(this, "", makefilters(11));
//                return cursor;
//            default:
//                Toast.makeText(this,"Select Search Type to begin", Toast.LENGTH_SHORT).show();
////                Utils.wordDefinition(this, "", databaseAccess.getDefinition(("ERROR")));
//                break;

            default:
                Toast.makeText(context,"Not ready yet", Toast.LENGTH_SHORT).show();
                return null;
//            case 14: // Unique
//            case 15: //

            //Palindromes
            //       Semordnilaps
            //Hookless words

        }
    }
    public List<String> getList_inLexicon(String lexicon, Context context, List<String> list) {
            List<String> wordsFound = new ArrayList<>();
            StringBuilder textlist = new StringBuilder();

            textlist.append("'" + list.get(0)+ "'");
            for(int c = 1; c < list.size(); c++) {
                textlist.append(", '" + list.get(c) + "'");
            }

            Cursor cursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + lexicon + "` \n" +
                    "WHERE Word IN (" + textlist + " )" +
                    "  " , null);

            wordsFound = wordsFromCursor(cursor);
            cursor.close();
            return wordsFound;
    }
    public List<String> getList_containsAny(String letters, int length) {
        if (letters.trim() == "")
            return null;

        char[] a = letters.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            first[a[c] - 'A']++;
        }

        String lenFilter = "Length(Word) = " + length + " ";
        Cursor precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) ", null);

        List<String> wordsFound = new ArrayList<>();

        while (precursor.moveToNext()) {
            String word = precursor.getString(0);
            char[] b = word.toCharArray();

            if (containsany(first, b)) {
                wordsFound.add(word);
            }
        }



        precursor.close();
        return wordsFound;

    }
    public List<String> getList_containsAny(String letters, int length, List<String> list ) {
        if (letters.trim() == "")
            return null;

        char[] a = letters.toCharArray(); // anagram
        int[] first = new int[26]; // letter count of anagram
        int c; // array position

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            first[a[c] - 'A']++;
        }

//        String lenFilter = "Length(Word) = " + length + " ";
//        Cursor precursor = database.rawQuery("SELECT Word \n" +
//                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + lenFilter +
//                " ) ", null);

        List<String> wordsFound = new ArrayList<>();

//        while (precursor.moveToNext()) {
//            String word = precursor.getString(0);
            for (String word : list) {
            char[] b = word.toCharArray();

            if (containsany(first, b)) {
                wordsFound.add(word);
            }
        }



//        precursor.close();
        return wordsFound;

    }
    public List<String> getList_HighPlays(String including, int length) {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) == %1$s", length);
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) " , null);

        while (cursor.moveToNext()) {
            String b = cursor.getString(0);
            if (including.indexOf(b.charAt(0)) >= 0
                    || including.indexOf(b.charAt(length - 1)) >= 0)
                wordsFound.add(b);

        }
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_HighPlays(String including, int length, List<String> list) {
        List<String> wordsFound = new ArrayList<>();
//        String lenFilter = String.format("Length(Word) == %1$s", length);
//        Cursor cursor = database.rawQuery("SELECT Word \n" +
//                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + lenFilter +
//                " ) " , null);

//        while (cursor.moveToNext()) {
//            String b = cursor.getString(0);
        for (String word : list) {
            if (word.length() != length)
                continue;
            if (including.indexOf(word.charAt(0)) >= 0
                    || including.indexOf(word.charAt(length - 1)) >= 0)
                wordsFound.add(word);

        }
//        cursor.close();
        return wordsFound;
    }
    public List<String> getList_vowelheavy() {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) " , null);

        while (precursor.moveToNext()) {
            String word = precursor.getString(0);
            int vowelcount = 0;
            int size = word.length();
            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            //vowelcount = word.Count(chr => vowels.Contains(chr));
            int percentage = (100 * vowelcount) / size;
            if ((percentage > 74 && size > 2) || (percentage > 61 && size > 3) || (percentage > 58 && size > 7))
                wordsFound.add(word);
        }

        precursor.close();
        return wordsFound;
    }
    public List<String> getList_vowelheavy(List<String> list) {
        List<String> wordsFound = new ArrayList<>();
//        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
//        Cursor precursor = database.rawQuery("SELECT Word \n" +
//                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + lenFilter +
//                " ) " , null);

//        while (precursor.moveToNext()) {
//            String word = precursor.getString(0);
        for (String word : list) {
            int vowelcount = 0;
            int size = word.length();
            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            //vowelcount = word.Count(chr => vowels.Contains(chr));
            int percentage = (100 * vowelcount) / size;
            if ((percentage > 74 && size > 2) || (percentage > 61 && size > 3) || (percentage > 58 && size > 7))
                wordsFound.add(word);
        }

//        precursor.close();
        return wordsFound;
    }
    public List<String> getList_consonantDumps() {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) " , null);

        while (precursor.moveToNext()) {
            String word = precursor.getString(precursor.getColumnIndex("Word"));
            int vowelcount = 0;
            int size = word.length();

            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            int percentage = (100 * vowelcount) / size;
            if (percentage < 18)
                wordsFound.add(word);
        }

        precursor.close();
        return wordsFound;
    }
    public List<String> getList_consonantDumps(List<String> list) {
        List<String> wordsFound = new ArrayList<>();

        for (String word : list) {
            int vowelcount = 0;
            int size = word.length();

            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            int percentage = (100 * vowelcount) / size;
            if (percentage < 18)
                wordsFound.add(word);
        }

        return wordsFound;
    }
    public List<String> getList_qNotu() {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (Word LIKE '%" + "Q" + "%' " +
                " AND " + lenFilter +
                " ) ", null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(0);
            if (!word.contains("U"))
                wordsFound.add(word);
        }
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_qNotu(List<String> list) {
        List<String> wordsFound = new ArrayList<>();
//        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
//        Cursor cursor = database.rawQuery("SELECT Word \n" +
//                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (Word LIKE '%" + "Q" + "%' " +
//                " AND " + lenFilter +
//                " ) ", null);

        for (String word : list) {
//        while (cursor.moveToNext()) {
//            String word = cursor.getString(0);
            if (word.contains("Q"))
            if (!word.contains("U"))
                wordsFound.add(word);
        }
//        cursor.close();
        return wordsFound;
    }
    public List<String> getList_getpalins() {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        boolean palin;
        Cursor precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) " , null);

        while (precursor.moveToNext()) {
            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
            //String word = cursor.getString(cursor.getColumnIndex("Word"));
            palin = true;
            char[] b = word.toCharArray();
            for (int i = 0; i < b.length / 2; i++) {
                if (b[i] != b[b.length - i - 1])
                    palin = false;
            }
            if (palin == true)
                wordsFound.add(word);
        }
        precursor.close();
        return wordsFound;
    }
    public List<String> getList_getpalins(List<String> list) {
        List<String> wordsFound = new ArrayList<>();
        boolean palin;
//        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
//        Cursor precursor = database.rawQuery("SELECT Word \n" +
//                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + lenFilter +
//                " ) " , null);

//        while (precursor.moveToNext()) {
        for (String word : list) {
//            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
            //String word = cursor.getString(cursor.getColumnIndex("Word"));
            palin = true;
            char[] b = word.toCharArray();
            for (int i = 0; i < b.length / 2; i++) {
                if (b[i] != b[b.length - i - 1])
                    palin = false;
            }
            if (palin == true)
                wordsFound.add(word);
        }
//        precursor.close();
        return wordsFound;
    }
    public List<String> getList_getnohooks() {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor cursor = database.rawQuery("SELECT Word, FrontHooks, BackHooks \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +

                " AND Trim(FrontHooks) = '' AND Trim(BackHooks) = '' " +

                " ) ", null);


        while (cursor.moveToNext()) {
            wordsFound.add(cursor.getString(0));
        }
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_getnohooks(List<String> list) {
        List<String> wordsFound = new ArrayList<>();
        StringBuilder textlist = new StringBuilder();

        textlist.append("'" + list.get(0)+ "'");
        for(int c = 1; c < list.size(); c++) {
            textlist.append(", '" + list.get(c) + "'");
        }

        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE Word IN (" + textlist + " )" +
                "AND (Trim(FrontHooks) = '' AND Trim(BackHooks) = ''  " +
                " ) " , null);

        wordsFound = wordsFromCursor(cursor);
        cursor.close();
        return wordsFound;
    }
    public List<String> getList_uniqueHooks() {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());

        Cursor precursor = rawQuery("SELECT Word, FrontHooks, BackHooks \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) " );

        while (precursor.moveToNext()) {
            //String word = cursor.getString(cursor.getColumnIndex("Word"));
            String fronthooks = precursor.getString(1);
            String backhooks = precursor.getString(2);
            if (fronthooks.trim().length() + backhooks.trim().length() == 1) {
                wordsFound.add(precursor.getString(0));
            }
        }
        precursor.close();
        return wordsFound;
    }
    public List<String> getList_uniqueHooks(List<String> list) {
        List<String> wordsFound = new ArrayList<>();
//        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        StringBuilder textlist = new StringBuilder();

        textlist.append("'" + list.get(0)+ "'");
        for(int c = 1; c < list.size(); c++) {
            textlist.append(", '" + list.get(c) + "'");
        }

        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE Word IN (" + textlist + " )" +
                "AND (Length(Trim(FrontHooks)) + Length(Trim(BackHooks)) = 1 " +
                " ) " , null);

//        Cursor precursor = rawQuery("SELECT Word, FrontHooks, BackHooks \n" +
//                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + lenFilter +
//                " ) " );
//
//        while (precursor.moveToNext()) {
//            //String word = cursor.getString(cursor.getColumnIndex("Word"));
//            String fronthooks = precursor.getString(1);
//            String backhooks = precursor.getString(2);
//            if (fronthooks.trim().length() + backhooks.trim().length() == 1) {
//                wordsFound.add(precursor.getString(0));
//            }
//        }
//        precursor.close();
//        return wordsFound;
        wordsFound = wordsFromCursor(cursor);
        cursor.close();
        return wordsFound;
    }

    // new methods
    public List<String> getList_vowelCount(int count) {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        Cursor precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) " , null);

        while (precursor.moveToNext()) {
            String word = precursor.getString(0);
            int size = word.length();
            int vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            //vowelcount = word.Count(chr => vowels.Contains(chr));
            if (vowelcount == count)
                wordsFound.add(word);
        }

        precursor.close();
        return wordsFound;
    }
    public List<String> getList_vowelCount(int count, List<String> list) {
        List<String> wordsFound = new ArrayList<>();
        for (String word : list) {
            int size = word.length();
            int vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
            //vowelcount = word.Count(chr => vowels.Contains(chr));
            if (vowelcount == count)
                wordsFound.add(word);
        }

//        precursor.close();
        return wordsFound;
    }
    public List<String> getList_withDef(String term) {
        List<String> wordsFound = new ArrayList<>();
        String lenFilter = String.format("Length(Word) <= %1$s ", LexData.getMaxLength());
//        Cursor precursor = getCursor_withDef(term);
        Cursor precursor = database.rawQuery("" +
                "SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " AND WordID IN \n" +
                "(Select WordID from tblWordDefinitions where tblWordDefinitions.DefinitionID  IN \n" +
                "(Select DefinitionID from tblDefinitions where tblDefinitions.Definition LIKE '%" + term + "%')) \n" +
                " ) " , null);

        wordsFound = wordsFromCursor(precursor);
        precursor.close();
        return wordsFound;
    }
    public List<String> getList_withDef(String term, List<String> list) {
    List<String> wordsFound = new ArrayList<>();
    String lenFilter = String.format("Length(Word) <= %1$s ", LexData.getMaxLength());
//        StringBuilder inputList = new StringBuilder();
//        inputList.append("'" + list.get(0)+ "'");
//        for(int c = 1; c < list.size(); c++) {
//            inputList.append(", '" + list.get(c) + "'");
//        }

        Cursor precursor = database.rawQuery("" +
            "SELECT Word " +
            "FROM     `" + LexData.getLexName() + "` \n" +
            "WHERE (" + lenFilter +
            " AND WordID IN \n" +
            "(Select WordID from tblWordDefinitions where tblWordDefinitions.DefinitionID  IN \n" +
            "(Select DefinitionID from tblDefinitions where tblDefinitions.Definition LIKE '%" + term + "%')) \n" +
//            " ) AND Word IN (" + inputList + " )\" +\n " , null);
            " ) AND Word IN (" + getInputList(list) + " )" + "\n " , null);

    wordsFound = wordsFromCursor(precursor);
        precursor.close();
        return wordsFound;
    }
    public String getInputList(List<String> list) {
        StringBuilder inputList = new StringBuilder();
        inputList.append("'" + list.get(0)+ "'");
        for(int c = 1; c < list.size(); c++) {
            inputList.append(", '" + list.get(c) + "'");
        }
        return inputList.toString();
    }

    // now included in threads
    public List<String> getList_pattern(String pattern) {
        if (pattern.length() < 2)
            return null;
        String prep = buildInnerPattern(pattern);
        Log.e("AfterSimple", prep);
        String exp = Utils.buildPattern(prep);
        Log.e("AfterBuild", exp);
        //Log.i("Expression", exp);

        String frontfilter = "";
        String backfilter = "";

        if (Character.isUpperCase(exp.charAt(1)))
            frontfilter = " AND Word LIKE '" + exp.charAt(1) + "%' ";
        if (Character.isUpperCase(exp.charAt(exp.length() - 2)))
            backfilter = " AND Word LIKE '%" + exp.charAt(exp.length() - 2) + "' ";

        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());

        String sql = "SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                frontfilter + backfilter +
                " ) " ;
        Cursor precursor = rawQuery(sql);

//                Log.i("Exp", exp);
        try {
            Pattern.compile(exp);
        } catch (PatternSyntaxException e) {
//                    Log.i("Exp", exp);

        }

        List<String> wordsFound = new ArrayList<>();
        while (precursor.moveToNext()) {
//                        String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
            String word = precursor.getString(0);

            if (word.matches(exp)) {
                wordsFound.add(word);
            }
        }
        precursor.close();
        return wordsFound;
    }
    public List<String> getList_pattern(String pattern, List<String> list) {
        if (pattern.length() < 2)
            return null;
        String prep = buildInnerPattern(pattern);
        Log.e("AfterSimple", prep);
        String exp = Utils.buildPattern(prep);
        Log.e("AfterBuild", exp);
        //Log.i("Expression", exp);

//                Log.i("Exp", exp);
        try {
            Pattern.compile(exp);
        } catch (PatternSyntaxException e) {
//                    Log.i("Exp", exp);

        }

        List<String> wordsFound = new ArrayList<>();
        for (String word : list) {
            if (word.matches(exp))
                wordsFound.add(word);
        }

        return wordsFound;
    }
    public List<String> getList_altEnding(String term) {
        if (term.trim().isEmpty())
            return null;

        List<String> wordsFound = new ArrayList<>();
        String altEnding = "";
        String orgEnding = "";

        if (!term.contains("?"))
            return null;
        String altSet = term.trim().replace("?", ">");
        String column[] = altSet.split(">");
        orgEnding = column[0];
        altEnding = column[1];

        String lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());

        Cursor precursor;
        Cursor searcher;
        Cursor altcursor;
        precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '%" + orgEnding + "' " +
                lenFilter + " ) ", null);

        altcursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '%" + altEnding + "' " +
                lenFilter + " ) ", null);


        List<String> alternates = new ArrayList<>();
        while (altcursor.moveToNext()) {
            String word2find = altcursor.getString(0);
            alternates.add(word2find);
            //Log.e("Alt", word2find);
        }
        altcursor.close();

        while (precursor.moveToNext()) {
            String orgWord = precursor.getString(0);
            String altWord = Utils.replaceLast(orgWord, orgEnding, altEnding);

            if (!alternates.contains(altWord))
                continue;
            //                            Log.e("Alt", altWord);

            searcher = getCursor_findWord(altWord); // test word(index, index + length)
            if (searcher.getCount() > 0) {
                searcher.moveToFirst();
                wordsFound.add(orgWord);
            }
            searcher.close();

        }
        precursor.close();
        return wordsFound;
    }
    public List<String> getList_altEnding(String term, List<String> list) {
        if (term.trim().isEmpty())
            return null;

        List<String> wordsFound = new ArrayList<>();
        String altEnding = "";
        String orgEnding = "";

        if (!term.contains("?"))
            return null;
        String altSet = term.trim().replace("?", ">");
        String column[] = altSet.split(">");
        orgEnding = column[0];
        altEnding = column[1];

        String lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());

        Cursor searcher;
        Cursor altcursor;

        altcursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '%" + altEnding + "' " +
                lenFilter + " ) ", null);


        List<String> alternates = new ArrayList<>();
        while (altcursor.moveToNext()) {
            String word2find = altcursor.getString(0);
            alternates.add(word2find);
            //Log.e("Alt", word2find);
        }
        altcursor.close();

        for(String orgWord : list) {
            if (!orgWord.endsWith(orgEnding))
                continue;

            String altWord = Utils.replaceLast(orgWord, orgEnding, altEnding);

            if (!alternates.contains(altWord))
                continue;
            //                            Log.e("Alt", altWord);

            searcher = getCursor_findWord(altWord); // test word(index, index + length)
            if (searcher.getCount() > 0) {
                searcher.moveToFirst();
                wordsFound.add(orgWord);
            }
            searcher.close();

        }
        return wordsFound;
    }
    public List<String> getList_replace(String term) {
        if (term.trim().isEmpty())
            return null;
        if (!term.contains("?"))
            return null;

        List<String> wordsFound = new ArrayList<>();
        String altString = "";
        String orgString = "";

        String altSet = term.trim().replace("?", ">");
        String column[] = altSet.split(">");
        orgString = column[0];
        altString = column[1];

        String lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());

        Cursor precursor;
        Cursor searcher;
        Cursor altcursor;
        precursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + "Word LIKE '%" + orgEnding + "' " +
                "WHERE (" + "Word LIKE '%" + orgString + "%' " +
                lenFilter + " ) ", null);

        altcursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
//                "WHERE (" + "Word LIKE '%" + altEnding + "' " +
                "WHERE (" + "Word LIKE '%" + altString + "%' " +
                lenFilter + " ) ", null);


        List<String> alternates = new ArrayList<>();
        while (altcursor.moveToNext()) {
            String word2find = altcursor.getString(0);
            alternates.add(word2find);
            //Log.e("Alt", word2find);
        }
        altcursor.close();

        while (precursor.moveToNext()) {
            String orgWord = precursor.getString(0);
            String altWord = orgWord.replaceFirst(orgString, altString);

            if (!alternates.contains(altWord))
                continue;
            //                            Log.e("Alt", altWord);

            searcher = getCursor_findWord(altWord); // test word(index, index + length)
            if (searcher.getCount() > 0) {
                searcher.moveToFirst();
                wordsFound.add(orgWord);
            }
            searcher.close();

        }
        precursor.close();
        return wordsFound;
    }
    public List<String> getList_replace(String term, List<String> list) {
        if (term.trim().isEmpty())
            return null;

        List<String> wordsFound = new ArrayList<>();
        String altString = "";
        String orgString = "";

        if (!term.contains("?"))
            return null;
        String altSet = term.trim().replace("?", ">");
        String column[] = altSet.split(">");
        orgString = column[0];
        altString = column[1];

        String lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());

//        Cursor searcher;
        Cursor altcursor;

        altcursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + "Word LIKE '%" + altString + "%' " +
                lenFilter + " ) ", null);


        List<String> alternates = new ArrayList<>();
        while (altcursor.moveToNext()) {
            String word2find = altcursor.getString(0);
            alternates.add(word2find);
            //Log.e("Alt", word2find);
        }
        altcursor.close();

        for(String orgWord : list) {
            String altWord;
            if (orgWord.contains(orgString)) {
                altWord = orgWord.replaceFirst(orgString, altString);
                if (alternates.contains(altWord))
                    wordsFound.add(orgWord);
            }

//            if (!alternates.contains(altWord))
//                continue;
//            //                            Log.e("Alt", altWord);
//
//            searcher = getCursor_findWord(altWord); // test word(index, index + length)
//            if (searcher.getCount() > 0) {
//                searcher.moveToFirst();
//                wordsFound.add(orgWord);
//            }
//            searcher.close();

        }
        return wordsFound;
    }
    public List<String> hookAnagrams(String term) {
        List<String> anagramList = new ArrayList<>();
        term = term.toUpperCase();
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            first[a[c] - 'A']++;
        }

        String filter = String.format("Length(Word) = %1$s", term.length());
        Cursor cursor = database.rawQuery("SELECT Word, FrontHooks, BackHooks \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + filter +
                " ) ", null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            String fhooks = "";
            String bhooks = "";
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram)) {
                if (LexData.getShowHooks()) {
                    fhooks = cursor.getString(cursor.getColumnIndex("FrontHooks")).trim();
                    bhooks = cursor.getString(cursor.getColumnIndex("BackHooks")).trim();

                    fhooks = padLeft(fhooks, 7 - fhooks.length());
                    bhooks = padRight(bhooks, 7 - bhooks.length());
                }
                //               Log.e("padding", ">" + fhooks + "  " + word + "  " + bhooks + "<");
                anagramList.add(fhooks + "  " + word + "  " + bhooks );
            }
        }
        return anagramList;
    }
    static int hookWidth = 9;
    public String[] getAnagrams(String[] words) {

        List<String> alphawords = new ArrayList<>();
        for (int id = 0; id < words.length; id++) {
            String sorted  = sortString(words[id]);
            alphawords.add(sorted);
        }
        Set<String> alphagrams = new LinkedHashSet<>(alphawords);

        /*        Set<String> alphagrams = new LinkedHashSet<>();
        for (int id = 0; id < words.length; id++) {
            String sorted  = sortString(words[id]);
            alphagrams.add(sorted);
        }
*/
        String[] alpha = alphagrams.toArray(new String[0]);
//        Arrays.sort(alpha);

//        Arrays.sort(alpha);
        return alpha;

    }


    // SLIDES AND QUIZZES
    public List<String> justblankAnagrams(String term) {
        List<String> anagramList = new ArrayList<>();
        term = term.toUpperCase();
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            first[a[c] - 'A']++;
        }

        String filter = String.format("Length(Word) = %1$s", term.length() + 1);
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + filter +
                " ) ", null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram, 1)) {
                anagramList.add(word);
            }
        }
        return anagramList;
    }
    // used by AnagramSlides; should only have letters
    public List<String> justAnagrams(String term) {
        List<String> anagramList = new ArrayList<>();
        term = term.toUpperCase();
        char[] a = term.toCharArray(); // anagram

        int[] first = new int[26]; // letter count of anagram
        int c; // array position

        // initialize word to anagram
        for (c = 0; c < a.length; c++) {
            first[a[c] - 'A']++;
        }

        String filter = String.format("Length(Word) = %1$s", term.length());
            Cursor cursor = database.rawQuery("SELECT Word \n" +
                    "FROM     `" + LexData.getLexName() + "` \n" +
                    "WHERE (" + filter +
                    " ) ", null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            char[] anagram = word.toCharArray();
            if (isAnagram(first, anagram)) {
                anagramList.add(word);
            }
        }
        return anagramList;
    }
    // used by AnagramSlides; should only have letters


    // UTILITIES
    public boolean permission(Context context) {
        if (ContextCompat.checkSelfPermission(context,
                WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            int okay = 1;
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{WRITE_EXTERNAL_STORAGE},
                    okay);

            if (ContextCompat.checkSelfPermission(context,
                    WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)
                return true;

        } else {
            return true;
            // Permission has already been granted
        }
        return false;
    }
    public int getVersion() {
        open();
        Cursor cursor = database.rawQuery("Select Max(VersionNumber) as Version FROM tblVersions", null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            version = cursor.getInt(cursor.getColumnIndex("Version"));
        }
        close();
        return version;
    }
    public String getDefinition(String word) {
        String definition = "";
//        open();
        // next line causes crash, removed, unused
//        String lexName = LexData.getLexName();

        Cursor cursor = database.rawQuery("SELECT Definition, Dictionary FROM tblWordDefinitions \n" +
                "INNER JOIN tblDictionaries ON tblWordDefinitions.DictionaryID = tblDictionaries.DictionaryID  \n" +
                "INNER JOIN tblDefinitions ON tblDefinitions.DefinitionID = tblWordDefinitions.DefinitionID \n" +
                "INNER JOIN Words ON Words.WordId = tblWordDefinitions.WordID\n" +
                "WHERE Words.Word = '" + word + "'", null);

/*        Cursor cursor = database.rawQuery("SELECT Definition, Dictionary FROM (`" + lexName + "` " +
                " INNER JOIN tblWordDefinitions ON `" + lexName + "`.WordID = tblWordDefinitions.WordID) " +
                " INNER JOIN tblDefinitions ON tblDefinitions.DefinitionID = tblWordDefinitions.DefinitionID " +
                " INNER JOIN tblDictionaries ON tblWordDefinitions.DictionaryID = tblDictionaries.DictionaryID  " +
                " WHERE (`" + lexName + "`.Word='" + word + "'" +
                ")", null);*/
/*        Cursor cursor = database.rawQuery("SELECT Definition, Dictionary FROM (`" + LexData.getLexName() + "` " +
                " INNER JOIN tblWordDefinitions ON `" + LexData.getLexName() + "`.WordID = tblWordDefinitions.WordID) " +
                " INNER JOIN tblDefinitions ON tblDefinitions.DefinitionID = tblWordDefinitions.DefinitionID " +
                " INNER JOIN tblDictionaries ON tblWordDefinitions.DictionaryID = tblDictionaries.DictionaryID  " +
                " WHERE (`" + LexData.getLexName() + "`.Word='" + word + "'" +
                ")", null);*/
/*        Cursor cursor = database.rawQuery("SELECT Definition, Dictionary, tblDictionaries.Lang FROM (`" + LexData.getLexName() + "` " +
                " INNER JOIN tblWordDefinitions ON `" + LexData.getLexName() + "`.WordID = tblWordDefinitions.WordID) " +
                " INNER JOIN tblDefinitions ON tblDefinitions.DefinitionID = tblWordDefinitions.DefinitionID " +
                " INNER JOIN tblDictionaries ON tblWordDefinitions.DictionaryID = tblDictionaries.DictionaryID  " +
                " WHERE (`" + LexData.getLexName() + "`.Word='" + word + "'" +
                " AND tblDictionaries.Lang = '" + LexData.getLexLanguage() + "' " +
                ")", null);*/

        // TODO update for Language of Lexicon
        while(cursor.moveToNext())
        {
            String def = cursor.getString(cursor.getColumnIndex("Definition"));
            String dict = cursor.getString(cursor.getColumnIndex("Dictionary"));
            if (!(definition == ""))
                definition += "\r\n";
            definition += def + "\r\n\t\t- " + dict;
        }
        cursor.close();
        if (definition == "")
            definition = "No definition for " + word;
//        close();
        return  definition;
    }
    public List<String> get_prefixes() {

        List<String> list = new ArrayList<>();
        open();
        String lexLanguage = LexData.getLexLanguage();
        Cursor cursor = database.rawQuery("SELECT Prefix, PrefixLanguage FROM tblPrefixes WHERE Trim(PrefixLanguage) = '" + lexLanguage +
                "' ORDER BY Prefix", null);

        list.add("Begins with:");
        while(cursor.moveToNext())
        {
            list.add(cursor.getString(cursor.getColumnIndex("Prefix")));
        }
        cursor.close();
        close();
        return list;
    }
    public List<String> get_suffixes() {

        List<String> list = new ArrayList<>();
        open();
        String lexLanguage = LexData.getLexLanguage();
        Cursor cursor = database.rawQuery("SELECT Suffix, SuffixLanguage FROM tblSuffixes WHERE Trim(SuffixLanguage) = '" + lexLanguage +
                "' ORDER BY Suffix", null);

        // TODO update for Language of Lexicon
        list.add("Ends with:");
        while(cursor.moveToNext())
        {
            list.add(cursor.getString(cursor.getColumnIndex("Suffix")));
        }
        cursor.close();
        close();
        return list;
    }
    public List<String> get_categories() {
        List<String> list = new ArrayList<>();
        open();
        Cursor cursor = database.rawQuery("Select Category from tblListCategories" +
                " ORDER BY Category", null);

        while(cursor.moveToNext())
        {
            list.add(cursor.getString(cursor.getColumnIndex("Category")));
        }
        cursor.close();
        close();
        return list;

    }
    public List<String> get_subjects() {
        List<String> list = new ArrayList<>();
        open();

        String sqlString = "SELECT WordLists.ListName as Subject " +
                "FROM WordLists INNER JOIN tblListCategories on WordLists.CategoryID = tblListCategories.CategoryID " +
                "order by tblListCategories.Category, WordLists.ListName";
//        String sqlString = "SELECT substr(tblListCategories.Category,1,6) || \": \" || WordLists.ListName as Subject " +
//                "FROM WordLists INNER JOIN tblListCategories on WordLists.CategoryID = tblListCategories.CategoryID " +
//                "order by tblListCategories.Category, WordLists.ListName";
        Log.d("getSubjects", sqlString);

        Cursor cursor = database.rawQuery(sqlString, null);

        while(cursor.moveToNext())
        {
            list.add(cursor.getString(cursor.getColumnIndex("Subject")));
        }
        cursor.close();
        close();

        return list;
    }
    public Cursor getCursor_Subjects() {
        String sqlString = "SELECT substr(tblListCategories.Category,1,6) || \": \" || WordLists.ListName as Subject " +
                "FROM WordLists INNER JOIN tblListCategories on WordLists.CategoryID = tblListCategories.CategoryID " +
                "order by tblListCategories.Category, WordLists.ListName";
        Log.d("getSubjects", sqlString);
        return database.rawQuery(sqlString, null);
    }
    public List<String> get_lexiconList() {
        List<String> list = new ArrayList<>();
        open();
        Cursor cursor = database.rawQuery("Select LexiconName from tblLexicons" +
        " WHERE LexiconName != '" + LexData.getLexName() + "' " +
                " ORDER BY LexiconName", null);

        while(cursor.moveToNext())
        {
            list.add(cursor.getString(cursor.getColumnIndex("LexiconName")));
        }
        cursor.close();
        close();
        return list;

    }


    // Formatting Utilities
    public String sortString (String word) {
        String sortedword = word.toUpperCase();
        char[] chars = sortedword.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
    public static String padRight(String s, int n) {

//        Log.e("padright", ">" + s + "<");

        if(s.length() > hookWidth - 1)
            return s;
        StringBuilder sb = new StringBuilder(s);
        for (int c = hookWidth - s.length(); c > 0; c--)
            sb.append(" ");
//        Log.e("padded", ">" + sb.toString() + "<");
        return sb.toString();
    }
    public static String padLeft(String s, int n) {
        //       Log.e("padleft", ">" + s + "<");

        if(s.length() > hookWidth - 1)
            return s;
        StringBuilder sb = new StringBuilder();
        for (int c = hookWidth - s.length(); c > 0; c--)
            sb.append(" ");
        sb.append(s);
        //       Log.e("padded", ">" + sb.toString() + "<");
        return sb.toString();
    }
    public String getWordDefined(String term) {
        // this gets words with term in definition ; need to add to menu and allow scrolling in results
        String definition = "";
//        open();
        String lexName = LexData.getLexName();

        Cursor cursor = database.rawQuery("SELECT Words.Word, tblDefinitions.Definition " +
                " FROM (Words INNER JOIN (tblDefinitions " +
                " INNER JOIN tblWordDefinitions ON tblDefinitions.DefinitionID = tblWordDefinitions.DefinitionID) " +
                " ON Words.WordID = tblWordDefinitions.WordID) " +
                " INNER JOIN `" + lexName + "` ON Words.WordID = `" + lexName + "`.WordID " +
                " WHERE (((tblDefinitions.Definition) Like '%" + term + "%' ))", null);

        // TODO update for Language of Lexicon
        while(cursor.moveToNext())
        {
            String word = cursor.getString(0);
            String defined = cursor.getString(1);
            if (!(definition == ""))
                definition += "\r\n";
            definition += word + "\r\n\t\t" + defined;
        }
        cursor.close();
        if (definition == "")
            definition = "No definition for " + term;
//        close();
        return  definition;
    }
    public boolean wordJudge(String word) {
//        open();
        //    String length = "Length(Word) = " + word.length();
        Cursor cursor = database.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "`\n" +
//            "WHERE (" + length +
//            " AND " + "Word = '" + word +
                "WHERE (" + "Word = '" + word +
                "')", null);
        boolean valid = (cursor.getCount() > 0);
        cursor.close();
//        close();
        return valid;
    }


    // SEARCH SUPPORT, Used by search threads ; some called from Search, uses searchThread
    public String get_frontparallel(String word) {
        // called when db is open
        // create String array size of word
        String[] builder = new String[word.length()];
        String pattern = "";

        for (int c = 0; c < word.length(); c++) {
            builder[c] = get_FrontHookLetters(word.charAt(c));
            if (builder[c].length() > 1)
                builder[c] = "[" + builder[c] + "]";
            else // it's okay to have a blank at the beginning or the end; but nowhere else
            {
                if ((builder[c].length() == 0) && (c > 0) && (c < word.length() - 1))
                    return (""); // return (pattern);
            }
            pattern += builder[c];
        }

        if (pattern == "")
            return (pattern); // return (results); // empty list
        else {
            if (builder[0].length() > 0)
                pattern = "*" + pattern;
            if (builder[word.length() - 1].length() > 0)
                pattern += "*";
        }

        String frontal = "";
        String backside = "";
        String PatternJoin = "(";
        if (builder[0].length() > 0) // if match on first character
        {
            for (int c = 0; c < word.length(); c++)
                frontal += builder[c];
            PatternJoin += "*" + frontal;
            if (builder[word.length() - 1].length() > 0) // if match on last character
                PatternJoin += "~"; // last character is optional
        }
        // pattern is only included if both frontal and backside are there; only one or all three
        if (builder[0].length() > 0 && builder[word.length() - 1].length() > 0)
            PatternJoin += "|" + pattern + "|";
        if (builder[word.length() - 1].length() > 0) {
            if (builder[0].length() > 0) // if match on first character
                backside = builder[0] + "~"; // first character is optional
            for (int c = 1; c < word.length(); c++)
                backside += builder[c];
            PatternJoin = PatternJoin + backside + "*";
        }
        PatternJoin += ")";
        return (PatternJoin);
    }
    public String get_FrontHookLetters(Character character) {
        String lenFilter = String.format("Length(Word) = 2");
        String results = "";
        open();
        Cursor cursor = database.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE " + lenFilter, null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            if (character.toString().equals(word.substring(1))) // if character is second letter
            {
                results += word.substring(0, 1);
            }
        }
        close();
//        Log.i("Pattern", results);
        return (results);
    }
    public String get_FrontHookLetters(String word) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String results = "";
//        String filter = String.Format("Len(Word) < {0}", Flags.lenLimit + 1);
        Cursor cursor = database.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE " + lenFilter, null);

        while (cursor.moveToNext()) {
            String b = cursor.getString(cursor.getColumnIndex("Word"));
            if (b.length() == word.length() + 1) {
                if (word.equals(b.substring(1))) // Front Hooks
                {
                    results += b.substring(0, 1);
                }
            }
        }
        return (results);
    }
    public String get_backparallel(String word) {
        // create String array size of word
        String[] builder = new String[word.length()];
        String pattern = "";

        for (int c = 0; c < word.length(); c++)
        {
            builder[c] = get_BackHookLetters(word.charAt(c));
            if (builder[c].length() > 1)
                builder[c] = "[" + builder[c] + "]";
            else // it's okay to have a blank at the beginning or the end; but nowhere else
            {
                if ((builder[c].length() == 0) && (c > 0) && (c < word.length() - 1))
                    return (""); // return (pattern);
            }
            pattern += builder[c];
        }

        if (pattern == "")
            return (pattern); // empty list
        else
        {
            if (builder[0].length() > 0)
                pattern = "*" + pattern;
            if (builder[word.length() - 1].length() > 0)
                pattern += "*";
        }
        String frontal = "";
        String backside = "";
        String PatternJoin = "(";
        if (builder[0].length() > 0) // if match on first character
        {
            for (int c = 0; c < word.length(); c++)
                frontal += builder[c];
            PatternJoin += "*" + frontal;
            if (builder[word.length() - 1].length() > 0) // if match on last character
                PatternJoin += "~"; // last character is optional
        }
        // pattern is only included if both frontal and backside are there; only one or all three
        if (builder[0].length() > 0 && builder[word.length() - 1].length() > 0)
            PatternJoin += "|" + pattern + "|";
        if (builder[word.length() - 1].length() > 0)
        {
            if (builder[0].length() > 0) // if match on first character
                backside = builder[0] + "~"; // first character is optional
            for (int c = 1; c < word.length(); c++)
                backside += builder[c];
            PatternJoin = PatternJoin + backside + "*";
        }
        PatternJoin += ")";

        return (PatternJoin);
    }
    public String get_BackHookLetters(Character character) {
        String lenFilter = String.format("Length(Word) = 2");
        String results = "";
        open();
        Cursor cursor = database.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE " + lenFilter, null);

        while (cursor.moveToNext()) {
            String word = cursor.getString(cursor.getColumnIndex("Word"));
            if (character.toString().equals(word.substring(0, 1))) // Back Hooks
            {
                results += word.substring(1);
            }
        }
        close();
        //       Log.i("Pattern", results);
        return (results);
    }
    public String get_BackHookLetters(String word) {
        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        String results = "";
        Cursor cursor = database.rawQuery("SELECT Word " +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE " + lenFilter, null);

        while (cursor.moveToNext()) {
            String b = cursor.getString(cursor.getColumnIndex("Word"));
            if (b.length() == word.length() + 1) {
                if (word.equals(b.substring(0, word.length()))) // Back Hooks
                {
                    results += b.substring(word.length(), 1);
                }
            }
        }
        return (results);
    }


    // THESE ARE SUBROUTINES OF GET_CURSOR....
    public boolean containsall(int[] letters, char[] word, int blankcount) {
        //int matchcount = 0;
        int matchcount = blankcount;
        // IGNORE, CONTAINS ALL HAS UNLIMITED BLANKS
        // use blankcount to specify word length
        int[] second = new int[26]; // letter count of test word
        int c, length = 0;
        // reset array
        for (c = 0; c < 26; c++)
            second[c] = 0;

        for (c = 0; c < word.length; c++) // initialize word prospect
            second[word[c] - 'A']++;

        for (c = 0; c < 26; c++) // count actual letters in search phrase
            length += letters[c];

        for (c = 0; c < 26; c++) // count matches
            matchcount += (letters[c]<second[c]) ? letters[c]:second[c];

        if (matchcount >= length + blankcount)
            return true;
        return false;
    }
    public boolean containsany(int[] letters, char[] word) {
        int matchcount = 0;
        int c;
        int[] second = {0,0,0,0,0, 0,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0, 0};

        for (c = 0; c < word.length; c++) {
            second[word[c] - 'A']++;
        }

        for (c = 0; c < 26; c++)
        {
            matchcount += Math.min(letters[c], second[c]);
        }

        if (matchcount > 0)
            return true;
        return false;
    }
    private boolean isMisspell(String word, String misspell) {
        int matchcount = 0;
        char[] a = word.toCharArray();
        char[] b = misspell.toCharArray();

        for (int c = 0; c < a.length; c++)
            if (a[c] == b[c])
                matchcount++;
        if (matchcount == a.length - 1)
            return true;
        return false;
    }
    public boolean isAnagram(int[] anagram, char[] word, int blankcount) {
        // anagram is letters in term
        int mismatchcount = 0;
        int c;
        int[] second = {0,0,0,0,0, 0,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0, 0};
        int letter;

        // separate tests
        if (blankcount > 0) {
            for (c = 0; c < word.length; c++) {
                letter = ++second[word[c] - 'A'];
                mismatchcount += (letter > anagram[word[c] - 'A'] ? 1 : 0); //Math.abs(second[c]-anagram[c]);

                //if (!(anagram[c] == second[c]))
                //  second[c] = Convert.ToChar(c + 'A');

                if (mismatchcount > blankcount) /// should be LENGTH??
                    return false;
            }
            return true;
        }
        else {
            for (c = 0; c < word.length; c++) {
                letter = ++second[word[c] - 'A'];
                if (letter > anagram[word[c] - 'A'])
                    return false;
            }
            return true;
        }
    }
    public boolean isAnagram(int[] anagram, char[] word) {     // when no blanks
        int c;
        int[] second = {0,0,0,0,0, 0,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0,  0,0,0,0,0, 0};
        int letter;

        for (c = 0; c < word.length; c++) {
            letter = ++second[word[c] - 'A'];
            if (letter > anagram[word[c] - 'A'])
                return false;
        }
        return true;
    }
    private static char getInnerCode(String stored) {
        if (stored.equals("False"))
            return ' ';
        if (stored.equals("True"))
            return '∙';
        if (stored.equals(""))
            return ' ';
        else return ('∙');

    }
    public String[] get_RedCursorRow (Cursor cursor) {
        String [] columnValues = new String[11];
        for (int c = 0; c < 11; c++)
            columnValues[c] = cursor.getString(c);
        columnValues[1] = columnValues[1].toLowerCase();
        return columnValues;
    }
    public String[] get_CursorRow (Cursor cursor) {
        String [] columnValues = new String[11];
        for (int c = 0; c < 11; c++)
            columnValues[c] = cursor.getString(c);
        return columnValues;
    }
    // converts blanks to lowercase ; calling method must calculate blank letters
    public String[] get_BlankCursorRow (Cursor cursor, char[] blanks) {
        String [] columnValues = new String[11];
        for (int c = 0; c < 11; c++)
            columnValues[c] = cursor.getString(c);

        String word = cursor.getString(cursor.getColumnIndex("Word"));
        for (int c = 0; c < 26; c++)
            while (blanks[c] > 0) {
                //char letter = Character.forDigit(c + 'A',10);
                char letter = (char)(c + 'A');
                char sub = (char)(c + 'a');
//                Log.i("Replace", Character.toString(letter) + Character.toString(sub));
                word = word.replaceFirst(Character.toString(letter), Character.toString(sub));
                blanks[c]--;
            }
        columnValues[cursor.getColumnIndex("Word")] = word;
        return columnValues;
    }
    public static int getBoolValue(String stored)     {
        if (stored == "False")
            return 0;
        if (stored == "True")
            return 1;
        if (stored == null || stored == "")
            return 0;
        else return (1);
    }
    private ColumnIndexCache cache = new ColumnIndexCache();
    public class ColumnIndexCache {
        private ArrayMap<String, Integer> mMap = new ArrayMap<>();
        public int getColumnIndex(Cursor cursor, String columnName) {
            if (!mMap.containsKey(columnName))
                mMap.put(columnName, cursor.getColumnIndex(columnName));
            return mMap.get(columnName);
        }
        public void clear() {
            mMap.clear();
        }
    }


    // nonSearch Threads
    public boolean MakeLexiconThread(final Context context, final Structures.Lexicon lexicon, final String huh) {
        // not found, configuring lexicon (background), try to select later
        // at end, lexicon created, you can now select the database/lexicon
        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            final String temptable = "TEMP";
            String droptable = "DROP TABLE IF EXISTS `" + temptable + "`";
            NotificationCompat.Builder builder;

            @Override
            protected void onPreExecute() {
                // Special Intents used for Notifications
                /*Intent intent = new Intent(context, AltSearchActivity.class );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);*/

                // regular intent
                // Create an Intent for the activity you want to start
                Intent intent = new Intent(context, SearchActivity.class);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(intent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent pendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                // Create an explicit intent for an Activity in your app
                builder = new NotificationCompat.Builder(context, "Hoot")
                        .setSmallIcon(R.mipmap.howl)
                        .setContentTitle("Lexicon configured")
                        .setContentText("Lexicon " + lexicon.LexiconName + " has been configured")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                Log.i("MakeLex", "onPreExecute");
                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage("Please Wait!\r\nExtracting lexicon for Android use...");
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        // progressDialog.dismiss(); // uncommented
                    }
                });
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(false);
                    }
                });
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
            }

            @Override
            protected Void doInBackground(Void... params) {

                // check if cancelled between sections
// for insert or update, use insert, update instead of execSQL
                Log.i("MakeLex", "doInBackground");

                open();
                database.execSQL("DROP TABLE IF EXISTS `" + lexicon.LexiconName + "`");

//                open();
                database.execSQL(droptable);
//                close();

                String tblSql = "CREATE TABLE `" + temptable + "` (" +
                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
                        " InnerFront NUM, InnerBack NUM, " +
                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, Anagrams INTEGER NOT NULL );";
                try {
//                    open();
                    database.execSQL(tblSql);
//                    close();
                } catch (Exception e) {
                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
                    cancel(true);
                }
                try {
                    open();
                    database.execSQL("CREATE INDEX WordIndex ON `" + temptable + "` ( Word COLLATE NOCASE);");
                    close();
                } catch (Exception e) {
                    // ignore
                }

                String sql = "INSERT INTO `" + temptable + "` " +
                        " SELECT Words.WordID, Words.Word, tblLexiconWords.FrontHooks, tblLexiconWords.BackHooks, " +
                        " tblLexiconWords.InnerFront, tblLexiconWords.InnerBack," +
                        " tblLexiconWords.ProbFactor, tblLexiconWords.tempPlay," +
                        " tblLexiconWords.PlayFactor, tblLexiconWords.OPlayFactor, tblLexiconWords.Anagrams" +
//                " " + Utils.getValue()
                        " FROM     Words INNER JOIN tblLexiconWords" +
                        " ON Words.WordID = tblLexiconWords.WordID" +
                        " WHERE  (tblLexiconWords.LexiconID = " + lexicon.LexiconID + ")";
                try {
                    open();
                    database.execSQL(sql);
                    close();
                } catch (Exception e) {
                    Log.e("Lexicon", "could not insert " + " - " + e.getMessage());
                    cancel(true);
                }

                open();
                try {
                    open();
                    database.execSQL("UPDATE `" + temptable + "` SET InnerFront = ' ' WHERE (InnerFront = 0);");
                    database.execSQL("UPDATE `" + temptable + "` SET InnerBack = ' ' WHERE (InnerBack = 0);");
                    database.execSQL("UPDATE `" + temptable + "` SET InnerFront = '∙' WHERE (InnerFront = 1);");
                    database.execSQL("UPDATE `" + temptable + "` SET InnerBack = '∙' WHERE (InnerBack = 1);");
                    close();
                } catch (Exception e) {
                    Log.e("Lexicon", "could not update " + " - " + e.getMessage());
                    cancel(true);
                }

                try {
                    open();
                    database.execSQL("ALTER TABLE `" + temptable + "` RENAME TO `" + lexicon.LexiconName + "`");
                    close();
                } catch (Exception e) {
                    Log.e("Lexicon", "could not drop/rename " + " - " + e.getMessage());
                    cancel(true);
                }

                // uses default message
                ((Activity) context).runOnUiThread(dialogMessages);

                // NOW DO SCORES
                Log.i("AddScores", "doInBackground");
                open();
//        Log.e("AddScores", "database " + " - " + database.getPath());


// create temp
                database.execSQL(droptable);

                try {
                    database.execSQL("CREATE TABLE `" + temptable + "` (" +
                            " WordID INTEGER, Word TEXT)");
                } catch (Exception e) {
                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
                    cancel(true);
                }

                try {
                    database.execSQL("INSERT INTO `" + temptable + "` " +
                            " SELECT WordID, Word FROM `" + lexicon.LexiconName + "`");
                } catch (Exception e) {
                    Log.e("Lexicon", "could not insert " + " - " + e.getMessage());
                    cancel(true);
                }

                try {
                    database.execSQL("ALTER TABLE `" + lexicon.LexiconName + "` ADD COLUMN Score INTEGER NOT NULL DEFAULT 0");
                } catch (Exception e) {
                    Log.e("Scores", "could not create table Score " + e.getMessage());
                    cancel(true);
                }

                for (int size = 2; size < 22; size++) {
                    Log.i("ScoreAdder: ", String.valueOf(size) + " letters");
                    message = "Please Wait!\r\nCalculating word scores...\r\n" + size + " letters";
                    ((Activity) context).runOnUiThread(dialogMessages);

                    database.beginTransaction();
                    try {
                        //               Log.i("Scores", "Calculating scores for " + String.valueOf(size) + " letter words");
//                Toast.makeText(this, String.valueOf(size), Toast.LENGTH_SHORT).show();
                        Cursor cursor = database.rawQuery("SELECT WordID, Word FROM `" + temptable + "` " +
                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);

                        while (cursor.moveToNext()) {
                            String word = cursor.getString(cursor.getColumnIndex("Word"));
                            int wordid = cursor.getInt(cursor.getColumnIndex("WordID"));
                            int score = Utils.getValue(word);

                            database.execSQL("UPDATE `" + lexicon.LexiconName + "` SET Score = " + score +
                                    " WHERE WordID = " + wordid);
                        }
                        database.setTransactionSuccessful();

                    } catch (Exception e) {
                        //database.endTransaction();
                        Log.e("Scores", "could not add  scores for " + String.valueOf(size) + " letter words");
                        cancel(true);
                    } finally {
                        database.endTransaction();
                    }
                }

                database.execSQL(droptable);
                scored = true;
                progressDialog.dismiss();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.i("MakeLex", "onPostExecute");
                success = true;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                LexData.setLexicon(context, lexicon.LexiconName);
                Toast.makeText(context, "Using Lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
                Toast.makeText(context, LexData.getLexicon().LexiconNotice, Toast.LENGTH_LONG).show();

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor prefs = settings.edit();
                prefs.putString("lexicon", LexData.getLexName());
                prefs.apply();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
// notificationId is a unique int for each notification that you must define
                notificationManager.notify(LexData.getNotification_id(), builder.build());
                //return null;
            }

            @Override
            protected void onCancelled() {
                database.endTransaction();
                close();
                open();
                database.execSQL(droptable);
                close();
                success = false;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                progressDialog.cancel();

                Log.i("configureLexicon", "Failed to Extract");
                Toast.makeText(context, "Failed to extract lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
                defaultDBLexicon(context);
                Utils.setDatabasePreference(context);

 /*               SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor prefs = settings.edit();

                prefs.putString("database", LexData.getDatabasePath() + "/" + LexData.getDatabase());
                prefs.apply();*/
                Toast.makeText(context, "Resetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            }
        };
        task.execute();
        return success;
    }
    public boolean ImportLexiconThread(final Context context, final Structures.Lexicon lexicon, final String tileset, final String filespec) {
        // not found, configuring lexicon (background), try to select later
        // at end, lexicon created, you can now select the database/lexicon
        Log.e("TileSet", tileset);

        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            String LexTable = lexicon.LexiconName;
            String TempTable = "IMPORTER", line, sqlcmd;
            ArrayList<String> importList = new ArrayList<>();
            NotificationCompat.Builder builder;
            int rowCount = 0;

            @Override
            protected void onPreExecute() {
                // Special Intents used for Notifications
                /*Intent intent = new Intent(context, AltSearchActivity.class );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);*/

                // regular intent
                // Create an Intent for the activity you want to start
                Intent intent = new Intent(context, SearchActivity.class);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(intent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent pendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                // Create an explicit intent for an Activity in your app
                builder = new NotificationCompat.Builder(context, "Hoot")
                        .setSmallIcon(R.mipmap.howl)
                        .setContentTitle("Lexicon " + lexicon.LexiconName + " imported")
                        .setContentText("Go to Settings to select this new lexicon")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage("Please Wait!\r\nImporting lexicon...");
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });
                progressDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                // todo check if cancelled between sections
                open();

                // CREATE TEMPORARY TABLE
                database.execSQL("DROP TABLE IF EXISTS `" + TempTable + "`");
                String tblSql = "CREATE TABLE `" + TempTable + "` (" +
                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
                        " InnerFront NUM, InnerBack NUM, " +
                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, Anagrams INTEGER NOT NULL DEFAULT 1, Score INTEGER NOT NULL DEFAULT 0);";
                try {
                    database.execSQL(tblSql);
                } catch (Exception e) {
                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
                }
                try {
                    database.execSQL("CREATE INDEX WordIndex ON `" + TempTable + "` ( Word COLLATE NOCASE);");
                } catch (Exception e) {
                    // ignore
                }
                Log.i("IMPORT", "Created Temp Table");


                // IMPORT FROM TEXT FILE INTO TEMPORARY TABLE
                message = "Please Wait!\r\nReading import file";
                ((Activity) context).runOnUiThread(dialogMessages);


                BufferedReader sr = null;
                InputStream inputStream;

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
                if (Utils.usingSAF()) {

                    Uri uri = Uri.parse(filespec);
                    // SAF
                    try {
                        inputStream =
                                context.getContentResolver().openInputStream(uri);
                        sr = new BufferedReader(
                                //                         BufferedReader reader = new BufferedReader(
                                new InputStreamReader(Objects.requireNonNull(inputStream)));
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                else
                    try {
                        sr = new BufferedReader(new FileReader(filespec));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e("Error",  "catch bufferedreader");
                    }





                database.beginTransaction();
                boolean processing = true;
                while (processing) {
                    try {
                        line = sr.readLine();





                        if (line == null)
                            break;
//                        else
//                            Log.d("lines", line);

                    } catch (IOException oops) {
                        Log.e("IO", line);
                        break;
                    }

                    // line = StringUtils.RemoveDiacritics(line);
                    line = Normalizer.normalize(line, Normalizer.Form.NFD);
                    line = line.replaceAll("[^\\p{ASCII}]", "");

                    if (line.charAt(0) == '/')
                        continue;
                    if (!Character.isLetter(line.charAt(0))) // this handles all lines not beginning with a character, including comments.
                        continue;

                    String column[] = line.split("\t");


                    // check number of columns !!!!!!!!!!!
                    if (column.length < 9) {
                        processing = false;
                        errorMessage = "This is not a valid Hoot version 3 lexicon. It maybe corrupted.";
//                        progressDialog.cancel();
                        cancel(true);
                    }

                    if (column[0].length() < 22) {
                        // GET WORDS IN IMPORTED LIST
                        importList.add(column[0]);
                        // then add to table
                        try {
                            sqlcmd = "INSERT INTO " + TempTable + " (Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams) " +
                                    "VALUES ('" + column[0] + "', " + // Word
                                    "'" + column[1] + "', '" + column[2] + "', " + //FrontHooks BackHooks
                                    "'" + getInnerCode(column[3]) + "', '" + getInnerCode(column[4]) + "', " + //InnerFront InnerBack
                                    " " + column[5] + ", " + //ProbFactor     //           " " + Convert.ToInt32(column[5]) + ", " + //ProbFactor
                                    " " + column[6] + ",  " + column[7] + ", " + //PlayFactor OPlayFactor
                                    " " + column[8] + "); "; //Anagrams
                            database.execSQL(sqlcmd);
                            rowCount++;
                        } catch (Exception ex) {
                            Log.e("Values", column[0] + column[1] + column[2] + column[3]);
                            Log.e("IMPORT ERROR", ex.getMessage());
                        }
                        if (isCancelled()) {
                            processing = false;
                            database.endTransaction();
                            try {
                                sr.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }

                try {
                    sr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }



//                Toast.makeText(context, String.format("Entered %s rows into table", Integer.toString(rowCount)), Toast.LENGTH_LONG).show();
                database.setTransactionSuccessful();
                database.endTransaction();
                Log.i("IMPORT", "Finished processing " + Integer.toString(rowCount) + "lexicon words");

                Cursor counter = database.rawQuery("SELECT COUNT(*) FROM " + TempTable, null);
                counter.moveToFirst();
                int count = counter.getInt(0);
                Log.i("IMPORT", Integer.toString(count) + " records are in" + TempTable);

                // ADD NEW WORDS TO WORDS TABLE
                message = "Please Wait!\r\nAdding new words to table";
                ((Activity) context).runOnUiThread(dialogMessages);

                database.beginTransaction();
                for (int i = 0; i < importList.size(); i++)
                    database.execSQL("INSERT OR IGNORE INTO Words (Word) VALUES ('" + importList.get(i) + "')");
                database.setTransactionSuccessful();
                database.endTransaction();
                Log.i("IMPORT", "Added new words to Words table");

                // CREATE ANDROID LEXICON
                database.execSQL("DROP TABLE IF EXISTS `" + LexTable + "`");
                tblSql = "CREATE TABLE `" + LexTable + "` (" +
                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
                        " InnerFront NUM, InnerBack NUM, " +
                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, " +
                        " Anagrams INTEGER NOT NULL DEFAULT 1, Score INTEGER NOT NULL DEFAULT 0, Alphagram TEXT);";
                try {
                    database.execSQL(tblSql);
                } catch (Exception e) {
                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
                    cancel(true);
                }
                try {
                    open();
                    database.execSQL("CREATE INDEX WordIndex ON `" + TempTable + "` ( Word COLLATE NOCASE);");
                    close();
                } catch (Exception e) {
                    // ignore
                }
                Log.i("IMPORT", "Created Lexicon Table");

                // CREATE LEXICON ENTRY
                String commandstring = "INSERT INTO tblLexicons (" +
                        "LexiconName, LexiconSource, LexiconStuff, LexiconNotice, LexLanguage) " +
                        "VALUES (" +
                        "'" + lexicon.LexiconName + "', " +
                        "'" + lexicon.LexiconSource + "', " +
                        "'" + lexicon.LexiconStuff + "', " +
                        "'" + lexicon.LexiconNotice + "', " +
                        "'" + lexicon.LexLanguage + "');";
                open();
                database.execSQL(commandstring);
                Log.i("IMPORT", "Created Lexicon in tblLexicons");


                message = "Please Wait!\r\nCreating new lexicon " + LexTable;
                ((Activity) context).runOnUiThread(dialogMessages);

                commandstring = "INSERT INTO `" + LexTable + "` ( WordID, Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams ) ";
                commandstring += "SELECT Words.WordID, ";
                commandstring += TempTable + ".Word, ";
                commandstring += TempTable + ".FrontHooks, ";
                commandstring += TempTable + ".BackHooks, ";
                commandstring += TempTable + ".InnerFront, ";
                commandstring += TempTable + ".InnerBack, ";
                commandstring += TempTable + ".ProbFactor, ";
                commandstring += TempTable + ".PlayFactor, ";
                commandstring += TempTable + ".OPlayFactor, ";
                commandstring += TempTable + ".Anagrams ";
                commandstring += "FROM Words INNER JOIN " + TempTable + " ON Words.Word = " + TempTable + ".Word ";
                commandstring += "WHERE (((Length(Words.Word))>1)) ";

                database.execSQL(commandstring);
                Log.i("IMPORT", "Added all words to table");


                // ADD SCORES TO LEXICON
//                ((Activity)context).runOnUiThread(dialogMessages);

                // SAVE CURRENT TILE SET (RESET below)
                String current = LexData.getTilesetName();

                // SET TILE SET FOR IMPORT
                if (tileset == null)
                    LexData.setTileset(0);
                else
                    LexData.setTileset(Arrays.asList(LexData.tileset).indexOf(tileset));

                // ADD SCORES AND ALPHAGRAMS
                for (int size = 2; size < 22; size++) {
                    Log.i("ScoreAdder: ", String.valueOf(size) + " letters");
                    message = "Please Wait!\r\nCalculating word scores...\r\n" + size + " letters";
                    ((Activity) context).runOnUiThread(dialogMessages);

                    database.beginTransaction();
                    // each length
                    try {
                        //               Log.i("Scores", "Calculating scores for " + String.valueOf(size) + " letter words");
//                Toast.makeText(this, String.valueOf(size), Toast.LENGTH_SHORT).show();
//                        Cursor cursor = database.rawQuery("SELECT Word FROM `" + TempTable + "` " +
//                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);
                        Cursor cursor = database.rawQuery("SELECT WordID, Word FROM `" + LexTable + "` " +
                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);

                        while (cursor.moveToNext()) {
                            String word = cursor.getString(1); //cursor.getColumnIndex("Word"));
                            int wordid = cursor.getInt(0); //cursor.getColumnIndex("WordID"));
                            int score = Utils.getValue(word);
                            String alphagram = Utils.sortString(word);

                            //Log.e("SQL","UPDATE `" + lexicon.LexiconName + "` SET Score = " + score +
                            //" WHERE Word = " + word );

                            database.execSQL("UPDATE `" + lexicon.LexiconName + "` SET Score = " + score + ", " +
                                    " Alphagram = '" + alphagram + "'" +
                                    " WHERE WordID = '" + wordid + "'");
                        }
                        database.setTransactionSuccessful();
                    } catch (Exception e) {
                        //database.endTransaction();
                        Log.e("Scores", "could not add  scores for " + String.valueOf(size) + " letter words");
                        cancel(true);
                    } finally {
                        database.endTransaction();
                    }
                    //database.setTransactionSuccessful();
                    //database.endTransaction();
                    if (isCancelled()) {
                        database.endTransaction();
                        close();
                        progressDialog.dismiss();
                        return null;
                    }
                }
                Log.i("IMPORT", "Added Scores");

                // RESET TO CURRENT TILE SET
                LexData.setTileset(Arrays.asList(LexData.tileset).indexOf(current));

                close();


                // delete TempTableTi
                progressDialog.dismiss();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.i("ImportLex", "onPostExecute");
                success = true;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                Toast.makeText(context, "Finished importing  " + lexicon.LexiconName, Toast.LENGTH_SHORT).show();
                Toast.makeText(context, lexicon.LexiconNotice, Toast.LENGTH_LONG).show();
                // not setting lexicon to current as in MakeLexicon

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
// notificationId is a unique int for each notification that you must define
                notificationManager.notify(LexData.getNotification_id(), builder.build());

                Toast.makeText(context, "Go to Settings to change to this lexicon ", Toast.LENGTH_LONG).show();
                ((Activity) context).finish();
            }

            @Override
            protected void onCancelled() {
                open();
                // remove lexicon entry
                try {
                    database.execSQL("DELETE FROM tblLexicons WHERE LexiconName = `" + LexTable + "`");
                } catch (SQLiteException e) {

                }
                // remove lexicon table
                database.execSQL("DROP TABLE IF EXISTS `" + LexTable + "`");
                close();
                success = false;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                progressDialog.cancel();

                Log.i("ImportLex", "Failed to Import");
                if (!errorMessage.equals(""))
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                else

                    Toast.makeText(context, "Failed to import lexicon " + LexTable, Toast.LENGTH_SHORT).show();
            }
        };
        task.execute();
        return success;
    }
    public boolean ImportSubjectList(LexData.WordList wordList) {
        String sql = "INSERT OR IGNORE INTO WordLists (ListName, " +
                "ListCredits, ListDescription, ListSource, CategoryID) " +
                "VALUES ( '" + wordList.ListName + "', '" + wordList.ListCredits + "', '" + wordList.ListDescription +
                "', '" + wordList.ListSource + "', '" + wordList.CategoryID + "');";
        database.execSQL(sql);

        return true;
    }
    public boolean CreateCategory(String category) {
        String sql = "INSERT OR IGNORE INTO tblListCategories (Category) " +
                "VALUES ( '" + category + "');";
        database.execSQL(sql);
        return true;
    }
    public boolean addAlphagramsThread(final Context context) {
        // not found, configuring lexicon (background), try to select later
        // at end, lexicon created, you can now select the database/lexicon
        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            String LexTable = LexData.getLexName(); // current
            NotificationCompat.Builder builder;
            int rowCount = 0;

            @Override
            protected void onPreExecute() {
                // Special Intents used for Notifications
                /*Intent intent = new Intent(context, AltSearchActivity.class );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);*/

                // regular intent
                // Create an Intent for the activity you want to start
                Intent intent = new Intent(context, SearchActivity.class);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(intent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent pendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                // Create an explicit intent for an Activity in your app
                builder = new NotificationCompat.Builder(context, "Hoot")
                        .setSmallIcon(R.mipmap.howl)
                        .setContentTitle("Alphagrams created for " + LexTable)
                        .setContentText("Now try sorting anagrams by alphagram")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                progressDialog = new ProgressDialog(context);



                // ADDED THIS, NOT CHECKED
                progressDialog = Utils.themeDialog(context);


                progressDialog.setMessage("Please Wait!\r\nAlphagramming lexicon...");
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });
                progressDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                // todo check if cancelled between sections
                open();

                // todo a crash report refers to this command 1/4/20
                database.execSQL("ALTER TABLE `" + LexTable + "` ADD Alphagram TEXT");


                // ADD SCORES AND ALPHAGRAMS
                for (int size = 2; size < 22; size++) {
                    Log.i("Alphagrams: ", String.valueOf(size) + " letters");

                    message = "Please Wait!\r\nCreating alphagrams for...\r\n" + size + " letters";
                    ((Activity) context).runOnUiThread(dialogMessages);

                    database.beginTransaction();
                    // each length
                    try {
                        Cursor cursor = database.rawQuery("SELECT WordID, Word FROM `" + LexTable + "` " +
                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);

                        while (cursor.moveToNext()) {
                            String word = cursor.getString(1); //cursor.getColumnIndex("Word"));
                            int wordid = cursor.getInt(0); //cursor.getColumnIndex("WordID"));
                            String alphagram = Utils.sortString(word);

                            //Log.e("SQL","UPDATE `" + lexicon.LexiconName + "` SET Score = " + score +
                            //" WHERE Word = " + word );

                            database.execSQL("UPDATE `" + LexTable + "` SET Alphagram = '" + alphagram + "'" +
                                    " WHERE WordID = '" + wordid + "'");
                        }
                        database.setTransactionSuccessful();
                    } catch (Exception e) {
                        //database.endTransaction();
                        Log.e("Alphagrams", "could not add alphagrams for " + String.valueOf(size) + " letter words");
                        cancel(true);
                    } finally {
                        database.endTransaction();
                    }
                    //database.setTransactionSuccessful();
                    //database.endTransaction();
                    if (isCancelled()) {
                        database.endTransaction();
                        close();
                        progressDialog.dismiss();
                        return null;
                    }
                }
                Log.i("Alphagrams", "Added Alphagrams");

                close();

                // delete TempTableTi
                progressDialog.dismiss();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.i("Alphagrams", "onPostExecute");
                success = true;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                Toast.makeText(context, "Finished adding alphagrams  " + LexTable, Toast.LENGTH_SHORT).show();
                // not setting lexicon to current as in MakeLexicon

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                notificationManager.notify(LexData.getNotification_id(), builder.build());
                ((Activity) context).finish();
            }

            @Override
            protected void onCancelled() {
                success = false;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                progressDialog.cancel();

                Log.i("Alphagrams", "Failed to add alphagrams");
                if (!errorMessage.equals(""))
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                else

                    Toast.makeText(context, "Failed to add alphagrams " + LexTable, Toast.LENGTH_SHORT).show();
            }
        };
        task.execute();
        return success;
    }


    // used by importLexicon
    public boolean orgImportLexiconThread(final Context context, final Structures.Lexicon lexicon, final String filespec) {
        // not found, configuring lexicon (background), try to select later
        // at end, lexicon created, you can now select the database/lexicon
        @SuppressLint("StaticFieldLeak") final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            String LexTable = lexicon.LexiconName;
            String TempTable = "IMPORTER", line, sqlcmd;
            ArrayList<String> importList = new ArrayList<>();
            NotificationCompat.Builder builder;
            int rowCount = 0;

            @Override
            protected void onPreExecute() {
                // Special Intents used for Notifications
                /*Intent intent = new Intent(context, AltSearchActivity.class );
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);*/

                // regular intent
                // Create an Intent for the activity you want to start
                Intent intent = new Intent(context, SearchActivity.class);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(intent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent pendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                // Create an explicit intent for an Activity in your app
                builder = new NotificationCompat.Builder(context, "Hoot")
                        .setSmallIcon(R.mipmap.howl)
                        .setContentTitle("Lexicon " + lexicon.LexiconName + " imported")
                        .setContentText("Go to Settings to select this new lexicon")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        // Set the intent that will fire when the user taps the notification
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                progressDialog = new ProgressDialog(context);
                progressDialog.setMessage("Please Wait!\r\nImporting lexicon...");
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cancel(true);
                    }
                });
                progressDialog.show();

            }

            @Override
            protected Void doInBackground(Void... params) {
                // todo check if cancelled between sections
                open();

                // CREATE TEMPORARY TABLE
                database.execSQL("DROP TABLE IF EXISTS `" + TempTable + "`");
                String tblSql = "CREATE TABLE `" + TempTable + "` (" +
                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
                        " InnerFront NUM, InnerBack NUM, " +
                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, Anagrams INTEGER NOT NULL DEFAULT 1, Score INTEGER NOT NULL DEFAULT 0);";
                try {
                    database.execSQL(tblSql);
                } catch (Exception e) {
                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
                }
                try {
                    database.execSQL("CREATE INDEX WordIndex ON `" + TempTable + "` ( Word COLLATE NOCASE);");
                } catch (Exception e) {
                    // ignore
                }
                Log.i("IMPORT", "Created Temp Table");


                // IMPORT FROM TEXT FILE INTO TEMPORARY TABLE
                message = "Please Wait!\r\nReading import file";
                ((Activity) context).runOnUiThread(dialogMessages);


                BufferedReader sr = null;
                try {
                    sr = new BufferedReader(new FileReader(filespec));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                database.beginTransaction();
                boolean processing = true;
                while (processing) {
                    try {
                        line = sr.readLine();
                        if (line == null)
                            break;
                    } catch (IOException oops) {
                        break;
                    }

                    // line = StringUtils.RemoveDiacritics(line);
                    line = Normalizer.normalize(line, Normalizer.Form.NFD);
                    line = line.replaceAll("[^\\p{ASCII}]", "");

                    if (line.charAt(0) == '/')
                        continue;
                    if (!Character.isLetter(line.charAt(0))) // this handles all lines not beginning with a character, including comments.
                        continue;

                    String column[] = line.split("\t");


                    // check number of columns !!!!!!!!!!!
                    if (column.length < 9) {
                        processing = false;
                        errorMessage = "This is not a valid Hoot version 3 lexicon.";
//                        progressDialog.cancel();
                        cancel(true);
                    }

                    if (column[0].length() < 22) {
                        // GET WORDS IN IMPORTED LIST
                        importList.add(column[0]);
                        // then add to table
                        try {
                            sqlcmd = "INSERT INTO " + TempTable + " (Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams) " +
                                    "VALUES ('" + column[0] + "', " + // Word
                                    "'" + column[1] + "', '" + column[2] + "', " + //FrontHooks BackHooks
                                    "'" + getInnerCode(column[3]) + "', '" + getInnerCode(column[4]) + "', " + //InnerFront InnerBack
                                    " " + column[5] + ", " + //ProbFactor     //           " " + Convert.ToInt32(column[5]) + ", " + //ProbFactor
                                    " " + column[6] + ",  " + column[7] + ", " + //PlayFactor OPlayFactor
                                    " " + column[8] + "); "; //Anagrams
                            database.execSQL(sqlcmd);
                            rowCount++;
                        } catch (Exception ex) {
                            Log.e("Values", column[0] + column[1] + column[2] + column[3]);
                            Log.e("IMPORT ERROR", ex.getMessage());
                        }
                        if (isCancelled()) {
                            processing = false;
                            database.endTransaction();
                            try {
                                sr.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }

                try {
                    sr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                Toast.makeText(context, String.format("Entered %s rows into table", Integer.toString(rowCount)), Toast.LENGTH_LONG).show();
                database.setTransactionSuccessful();
                database.endTransaction();
                Log.i("IMPORT", "Finished processing " + Integer.toString(rowCount) + "lexicon words");

                Cursor counter = database.rawQuery("SELECT COUNT(*) FROM " + TempTable, null);
                counter.moveToFirst();
                int count = counter.getInt(0);
                Log.i("IMPORT", Integer.toString(count) + " records are in" + TempTable);

                // ADD NEW WORDS TO WORDS TABLE
                message = "Please Wait!\r\nAdding new words to table";
                ((Activity) context).runOnUiThread(dialogMessages);

                database.beginTransaction();
                for (int i = 0; i < importList.size(); i++)
                    database.execSQL("INSERT OR IGNORE INTO Words (Word) VALUES ('" + importList.get(i) + "')");
                database.setTransactionSuccessful();
                database.endTransaction();
                Log.i("IMPORT", "Added new words to Words table");

                // CREATE ANDROID LEXICON
                database.execSQL("DROP TABLE IF EXISTS `" + LexTable + "`");
                tblSql = "CREATE TABLE `" + LexTable + "` (" +
                        " WordID INTEGER PRIMARY KEY , Word TEXT, FrontHooks TEXT, BackHooks  TEXT, " +
                        " InnerFront NUM, InnerBack NUM, " +
                        " ProbFactor INTEGER DEFAULT 0, tempPlay INTEGER NOT NULL DEFAULT 0, " +
                        " PlayFactor INTEGER NOT NULL DEFAULT 0, OPlayFactor INTEGER NOT NULL DEFAULT 0, " +
                        " Anagrams INTEGER NOT NULL DEFAULT 1, Score INTEGER NOT NULL DEFAULT 0);";
                try {
                    database.execSQL(tblSql);
                } catch (Exception e) {
                    Log.e("Lexicon", "could not create " + " - " + e.getMessage());
                    cancel(true);
                }
                try {
                    open();
                    database.execSQL("CREATE INDEX WordIndex ON `" + TempTable + "` ( Word COLLATE NOCASE);");
                    close();
                } catch (Exception e) {
                    // ignore
                }
                Log.i("IMPORT", "Created Lexicon Table");

                // CREATE LEXICON ENTRY
                String commandstring = "INSERT INTO tblLexicons (" +
                        "LexiconName, LexiconSource, LexiconStuff, LexiconNotice, LexLanguage) " +
                        "VALUES (" +
                        "'" + lexicon.LexiconName + "', " +
                        "'" + lexicon.LexiconSource + "', " +
                        "'" + lexicon.LexiconStuff + "', " +
                        "'" + lexicon.LexiconNotice + "', " +
                        "'" + lexicon.LexLanguage + "');";
                open();
                database.execSQL(commandstring);
                Log.i("IMPORT", "Created Lexicon in tblLexicons");


                message = "Please Wait!\r\nCreating new lexicon " + LexTable;
                ((Activity) context).runOnUiThread(dialogMessages);

                commandstring = "INSERT INTO `" + LexTable + "` ( WordID, Word, FrontHooks, BackHooks, InnerFront, InnerBack, ProbFactor, PlayFactor, OPlayFactor, Anagrams ) ";
                commandstring += "SELECT Words.WordID, ";
                commandstring += TempTable + ".Word, ";
                commandstring += TempTable + ".FrontHooks, ";
                commandstring += TempTable + ".BackHooks, ";
                commandstring += TempTable + ".InnerFront, ";
                commandstring += TempTable + ".InnerBack, ";
                commandstring += TempTable + ".ProbFactor, ";
                commandstring += TempTable + ".PlayFactor, ";
                commandstring += TempTable + ".OPlayFactor, ";
                commandstring += TempTable + ".Anagrams ";
                commandstring += "FROM Words INNER JOIN " + TempTable + " ON Words.Word = " + TempTable + ".Word ";
                commandstring += "WHERE (((Length(Words.Word))>1)) ";

                database.execSQL(commandstring);
                Log.i("IMPORT", "Added all words to table");


                // ADD SCORES TO LEXICON
//                ((Activity)context).runOnUiThread(dialogMessages);

                // ADD SCORES
                for (int size = 2; size < 22; size++) {
                    Log.i("ScoreAdder: ", String.valueOf(size) + " letters");
                    message = "Please Wait!\r\nCalculating word scores...\r\n" + size + " letters";
                    ((Activity) context).runOnUiThread(dialogMessages);

                    database.beginTransaction();
                    // each length
                    try {
                        //               Log.i("Scores", "Calculating scores for " + String.valueOf(size) + " letter words");
//                Toast.makeText(this, String.valueOf(size), Toast.LENGTH_SHORT).show();
//                        Cursor cursor = database.rawQuery("SELECT Word FROM `" + TempTable + "` " +
//                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);
                        Cursor cursor = database.rawQuery("SELECT WordID, Word FROM `" + LexTable + "` " +
                                "WHERE (Length(Word) = " + String.valueOf(size) + " ) ", null);

                        while (cursor.moveToNext()) {
                            String word = cursor.getString(1); //cursor.getColumnIndex("Word"));
                            int wordid = cursor.getInt(0); //cursor.getColumnIndex("WordID"));
                            int score = Utils.getValue(word);

                            //Log.e("SQL","UPDATE `" + lexicon.LexiconName + "` SET Score = " + score +
                            //" WHERE Word = " + word );

                            database.execSQL("UPDATE `" + lexicon.LexiconName + "` SET Score = " + score +
                                    " WHERE WordID = '" + wordid + "'");
                        }
                        database.setTransactionSuccessful();
                    } catch (Exception e) {
                        //database.endTransaction();
                        Log.e("Scores", "could not add  scores for " + String.valueOf(size) + " letter words");
                        cancel(true);
                    } finally {
                        database.endTransaction();
                    }
                    //database.setTransactionSuccessful();
                    //database.endTransaction();
                    if (isCancelled()) {
                        database.endTransaction();
                        close();
                        progressDialog.dismiss();
                        return null;
                    }
                }
                Log.i("IMPORT", "Added Scores");

                close();

                // delete TempTableTi
                progressDialog.dismiss();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                Log.i("ImportLex", "onPostExecute");
                success = true;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                Toast.makeText(context, "Finished importing  " + lexicon.LexiconName, Toast.LENGTH_SHORT).show();
                Toast.makeText(context, lexicon.LexiconNotice, Toast.LENGTH_LONG).show();
                // not setting lexicon to current as in MakeLexicon

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
// notificationId is a unique int for each notification that you must define
                notificationManager.notify(LexData.getNotification_id(), builder.build());

                Toast.makeText(context, "Go to Settings to change to this lexicon ", Toast.LENGTH_LONG).show();
                ((Activity) context).finish();
            }

            @Override
            protected void onCancelled() {
                open();
                // remove lexicon entry
                try {
                    database.execSQL("DELETE FROM tblLexicons WHERE LexiconName = `" + LexTable + "`");
                } catch (SQLiteException e) {

                }
                // remove lexicon table
                database.execSQL("DROP TABLE IF EXISTS `" + LexTable + "`");
                close();
                success = false;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                progressDialog.cancel();

                Log.i("ImportLex", "Failed to Import");
                if (!errorMessage.equals(""))
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                else

                    Toast.makeText(context, "Failed to import lexicon " + LexTable, Toast.LENGTH_SHORT).show();
            }
        };
        task.execute();
        return success;
    }

}




