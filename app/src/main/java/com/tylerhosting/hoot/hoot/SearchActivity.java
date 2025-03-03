package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.constraint.Guideline;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.Surface;
import android.view.TouchDelegate;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;


import static android.app.PendingIntent.getActivity;
import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.tylerhosting.hoot.hoot.Hoot.context;
import static com.tylerhosting.hoot.hoot.Utils.usingLegacy;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class SearchActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    @SuppressLint("ClickableViewAccessibility")

    SharedPreferences shared;
    SharedPreferences.Editor prefs;

//     Replace Search button with Image Button
    ToggleButton collapse;
    Spinner stype, predef, stems, categories, begins, ends, minimum, maximum, sortby, thenby;
    EditText etTerm, etFilter, etLimit, etOffset;
    Button clearEntry, clearBegins, clearEnds, search, blank, emptyRack, more, altSearch;
    Button slides, quizreview, quiz, clear, newsearch;
    View Underlay, scrollOverlay;
    Guideline guide1, guide2, guide3;
    ConstraintLayout lvHeader;
    TextView term, importfile, status, specStatus;
    ListView listView;
    FloatingActionButton fab;

    DatabaseAccess databaseAccess;
    List<String> words = new ArrayList<>();
    SimpleCursorAdapter cursorAdapter;
    Cursor cursor;


    // Support custom keyboard
    Keyboard bKeyboard; // basic
    Keyboard pKeyboard; // pattern
    Keyboard defKeyboard; // full
    Keyboard dKeyboard; // dialog box (basic)
    KeyboardView dKeyboardView;
    KeyboardView mKeyboardView;

    boolean unfiltered;
    int lastPrefix, lastSuffix, lastCategory, lastMin, lastMax;
    int listfontsize = 20;
    int limit;
    int offset;
    int position, skips;
    int secondary; // secondaryColor of theme
    long lStartTime;
    String thisActivity;
    String selectedWord;
    String ordering = "ORDER BY Length(Word), Word";
    String limits = "";

    String themeName;
    String message = "";
    String lastStatus = "";

    LexData.Cardbox cardBox;

    // use to pass to other activities
    StringBuilder searchParameters = new StringBuilder();

    String[] statfrom = new String[]{"FrontHooks","InnerFront", "Word", "InnerBack", "BackHooks",
            "Anagrams", "ProbFactor", "OPlayFactor", "Score"};
    int[] statto = new int[] { R.id.fh, R.id.ifh, R.id.word, R.id.ibh, R.id.bh, R.id.an, R.id.pr, R.id.pl, R.id.score };

    String[] statonlyfrom = new String[]{"InnerFront", "Word", "InnerBack",
            "Anagrams", "ProbFactor", "OPlayFactor", "Score"};
    int[] statonlyto = new int[] { R.id.ifh, R.id.word, R.id.ibh, R.id.an, R.id.pr, R.id.pl, R.id.score };

    String[] from = new String[]{"FrontHooks","InnerFront", "Word", "InnerBack", "BackHooks", "Score"};
    int[] to = new int[] { R.id.fh, R.id.ifh, R.id.word, R.id.ibh, R.id.bh, R.id.score };

    String[] plainfrom = new String[]{"Word", "Score"};
    int[] plainto = new int[] { R.id.word, R.id.score };

    String[] stemfrom = new String[]{"Word"};
    int[] stemto = new int[] { R.id.word };


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = "SearchActivity";


//        resetDatabase();

        shared = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = shared.edit();

        //        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
//        Log.d("onCre ", shared.getString("database", "") );
//        Log.d("onCre ", shared.getString("lexicon", ""));
//        Log.d("onCre ", LexData.getDatabasePath() + " - " + LexData.getDatabase());
//        Log.d("onCre ", LexData.getLexName());

        // save current theme to compare when changed
        themeName = Utils.setStartTheme(this);
        setContentView(R.layout.activity_search2);

        setDatabase();

        populateResources();

        // Support custom keyboard
        pKeyboard = new Keyboard(SearchActivity.this,R.xml.patternkeyboard);
        bKeyboard = new Keyboard(SearchActivity.this,R.xml.basickeyboard);
        defKeyboard  = new Keyboard(SearchActivity.this,R.xml.defkeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
        mKeyboardView.setKeyboard( bKeyboard );

        hideCustomKeyboard();

        ///// ?????????????
        hidekeyboard();
    }
    protected void onResume() {
        super.onResume();
//        populateResources();


        // sets display items

        setDisplay();

        // execute this after getting database, lexicon preferences
//        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
//        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

//        shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        String lexicon = shared.getString("lexicon","");
//        Structures.Lexicon lexstruct = databaseAccess.get_lexicon(lexicon);
//        LexData.setLexicon(getApplicationContext(), lexstruct.LexiconName);

        // must wait until is selected

//        showNews(5);
    }
    // called by onCreate
    protected void setDatabase() {

        // SET UP DATABASE
        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs = shared.edit();


        // get database path from prefs, or use Internal
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
            LexData.setDatabase(fullpath.substring(fullpath.lastIndexOf(File.separator)));
            LexData.setDatabasePath(getApplicationContext(), fullpath.substring(0, fullpath.lastIndexOf(File.separator)));
            Toast.makeText(this, "You are using an alternate database,\r\n" +
                    " To use the distributed database select internal database aHoot.db3 in Settings", Toast.LENGTH_LONG).show();
        }
        // set database instance based on lexdata
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

//        if (databaseAccess.getVersion() < LexData.CURRENT_VERSION)
//            Toast.makeText(this, "There is an updated database; restart Hoot to use it.", Toast.LENGTH_LONG).show();

        // makes sure database has tblLexicons
        if (!databaseAccess.isValidDatabase()) {
            Flavoring.addflavoring(getApplicationContext()); // sets database to default
            LexData.setDatabasePath(getApplicationContext(), ""); // set database path to ""
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
            prefs.putString("database", "Internal");
            prefs.apply();
        }


        // SET UP LEXICON
        String lexiconName = shared.getString("lexicon","");

        // if empty, try first lexicon
        if (lexiconName.equals(""))
            lexiconName = databaseAccess.get_firstValidLexicon();
        // if still empty, set database to default and lexicon to first, AND create new instance
        if (lexiconName.equals("")) {
            databaseAccess.defaultDBLexicon(getApplicationContext());
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        }

        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexiconName);
        LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);
        Utils.setLexiconPreference(this);


        // NOT IN RESETDATABASE
        if (!databaseAccess.lexiconExists(lexiconName)) {
            Toast.makeText(getApplicationContext(), "Lexicon " + lexicon + " doesn't exist in current database", Toast.LENGTH_LONG).show();
            Structures.Lexicon firstLexicon = databaseAccess.get_lexicon(databaseAccess.get_firstValidLexicon());

            if (!databaseAccess.checkLexicon(firstLexicon)) {
                databaseAccess.defaultDBLexicon(getApplicationContext());
                Toast.makeText(this, "Lexicons are not configured for app use. Resetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_LONG).show();
                // update prefs
                Utils.setDatabasePreference(this);
                Utils.setLexiconPreference(this);
            }
            else // get first valid lexicon
                setLexicon(databaseAccess.get_firstValidLexicon());
        }
        else { // same lexicon doesn't exist
            if (databaseAccess.checkLexicon(lexicon)) // if lexicon is valid
                setLexicon(lexiconName);
            else { // get first valid lexicon
                Utils.lexAlert(this, lexiconName);
            }
        }
    }
    // called after call to Settings
    protected void resetDatabase() {
        // called from onOptionsItemSelected
        // duplicate of initialize Database in SplashActivity
        // runs after change in Settings

        // initialiize database (?? same as in splash)
        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs = shared.edit();

        String fullpath = shared.getString("database", "");
        if (fullpath == "") {
            fullpath = "Internal";
            prefs.putString("database", "Internal");
            prefs.apply();
        }

        if (fullpath.equals("Internal") || !fullpath.contains(File.separator))  {
            Flavoring.addflavoring(getApplicationContext()); // sets database
            LexData.setDatabasePath(getApplicationContext(), "");
            if (databaseAccess.getVersion() < LexData.CURRENT_VERSION)
                Toast.makeText(this, "There is an updated database; restart Hoot to use it.", Toast.LENGTH_LONG).show();
        }
        else {
            LexData.setDatabase(fullpath.substring(fullpath.lastIndexOf(File.separator)));
            LexData.setDatabasePath(getApplicationContext(), fullpath.substring(0, fullpath.lastIndexOf(File.separator)));
        }

        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());




        // todo is this needed; compare with splashactivity
        // shouldn't settings validate lexicon
        // initialize lexicon structure

        // fixes attempt to load invalid database: app exits and resets on open
        if (!databaseAccess.isValidDatabase()) {
            Flavoring.addflavoring(getApplicationContext()); // sets database
            LexData.setDatabasePath(getApplicationContext(), "");
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
            prefs.putString("database", "Internal");
            prefs.apply();
        }

        // SET UP LEXICON
        String lexiconName = shared.getString("lexicon","");
// moved here
        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexiconName);
        LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);
        Utils.setLexiconPreference(this);

        // if empty, try first lexicon
        if (lexiconName.equals(""))
            lexiconName = databaseAccess.get_firstValidLexicon();
        if (lexiconName.equals("")) {
            databaseAccess.defaultDBLexicon(getApplicationContext());
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        }


        // NOT IN RESETDATABASE
        if (!databaseAccess.lexiconExists(lexiconName)) {
            Toast.makeText(getApplicationContext(), "Lexicon " + lexicon + " doesn't exist in current database", Toast.LENGTH_LONG).show();
            Structures.Lexicon firstLexicon = databaseAccess.get_lexicon(databaseAccess.get_firstValidLexicon());

            if (!databaseAccess.checkLexicon(firstLexicon)) {
                databaseAccess.defaultDBLexicon(getApplicationContext());
                Toast.makeText(this, "Lexicons are not configured for app use. Resetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_LONG).show();
                // update prefs
                Utils.setDatabasePreference(this);
                Utils.setLexiconPreference(this);
            }
            else // get first valid lexicon
                setLexicon(databaseAccess.get_firstValidLexicon());
        }
        else { // same lexicon doesn't exist
            if (databaseAccess.checkLexicon(lexicon)) // if lexicon is valid
                setLexicon(lexiconName);
            else { // get first valid lexicon
                Utils.lexAlert(this, lexiconName);
            }
        }


    }
    // called from onResume;
    protected void setDisplay() {
        listView.setVisibility(VISIBLE);
        if (Utils.themeChanged(themeName, this)) {
            Utils.setNewTheme(this);
            clear.performClick();
            recreate();
            collapse();
            specStatus.setText("Other Specifications");
        }

        secondary = fetchThemeColor(R.attr.colorSecondary);
        Underlay.setBackgroundColor(secondary);
        specStatus.setBackgroundColor(secondary);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setRotation();

        readPreferences(); // moved before fastscroll setting

        loadMinimum();
        loadMaximum();

//        loadPrefixes();
//        loadSuffixes();
//        loadCategoryList();

        final View parent = (View) collapse.getParent();  // button: the view you want to enlarge hit area
        parent.post( new Runnable() {
            public void run() {
                final Rect rect = new Rect();
                collapse.getHitRect(rect);
                rect.top -= 25;    // increase top hit area
                rect.left -= 25;   // increase left hit area
                rect.bottom += 50; // increase bottom hit area
                rect.right += 25;  // increase right hit area
                parent.setTouchDelegate( new TouchDelegate( rect , collapse));
            }
        });




        if (!collapse.isChecked()) {
//            group if not collapsed

            lvHeader = findViewById(R.id.imcheader);
            if (LexData.getShowStats())
                lvHeader.setVisibility(VISIBLE);
            else
                lvHeader.setVisibility(GONE);

            if (LexData.getShowCondensed()) {
                showCondensed();
            }
            else
                showExpanded();
        }

        if (LexData.getFastScroll()) {
            listView.setFastScrollEnabled(true);
            listView.setFastScrollAlwaysVisible(true);
            scrollOverlay.setVisibility(VISIBLE);
        }
        else
        {
            listView.setFastScrollEnabled(false);
            listView.setFastScrollAlwaysVisible(false);
            scrollOverlay.setVisibility(GONE);
        }

        // restoring settings after recreate
        // saved after every search
        begins.setSelection(lastPrefix);
        ends.setSelection(lastSuffix);
        minimum.setSelection(lastMin);
        maximum.setSelection(lastMax);
        categories.setSelection(lastCategory);
        if (lastStatus.isEmpty()) {
            if (LexData.getLexicon().LexiconNotice.isEmpty())
                status.setText((LexData.getLexName()));
            else
                status.setText(LexData.getLexicon().LexiconNotice);
        }
        else
            status.setText(lastStatus);



//        // Support custom keyboard
//        pKeyboard = new Keyboard(SearchActivity.this,R.xml.patternkeyboard);
//        bKeyboard = new Keyboard(SearchActivity.this,R.xml.basickeyboard);
//
//        // Lookup the KeyboardView
//        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
//        // Attach the keyboard to the view
//        mKeyboardView.setKeyboard( bKeyboard );
//
//        hideCustomKeyboard();

        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();

        if (listView.getCount() == 0)
            showkeyboard();
    }


    // called from OnCreate
    protected void readPreferences() {
        String user = shared.getString("user", "John Smith");
        LexData.setUsername(user);

        String cardLocation = shared.getString("cardlocation", "Internal");

        if (!usingLegacy()) {
            cardLocation = "Internal";
            prefs.putString("cardlocation", cardLocation);
            prefs.putBoolean("saf", true);
        }
        LexData.setCardslocation(cardLocation);

        String tileset = shared.getString("tileset",null);
        if (tileset == null)
            LexData.setTileset(0);
        else
            LexData.setTileset(Arrays.asList(LexData.tileset).indexOf(tileset));
        prefs.putString("tileset", LexData.getTilesetName());
        prefs.apply();

        String length = shared.getString("wordlength",null);
        if (!(length == null)) {
            String lenOnly = length.substring(0, length.indexOf(" "));
            Integer len  = Integer.parseInt( lenOnly );
            if (len != LexData.getMaxLength()) {
                LexData.setMaxLength(len);
            }
        }
        else {
            LexData.setMaxLength(15);
        }

        String ll = shared.getString("listlimit", "0");
        if (Utils.isParsable(ll))
            LexData.setMaxList(Integer.parseInt(shared.getString("listlimit","5000")));
        else {
            prefs.putString("listlimit", "0");
            prefs.apply();
            LexData.setMaxList(0);
        }

        String altEnding = shared.getString("altending", "Original");
        LexData.setAltOption(altEnding);

        String tapOption = shared.getString("tapping", "Definition");
        LexData.setTapOption(tapOption);

        // reset showStats value
        Boolean stats = shared.getBoolean("stats",true);
        if (stats != LexData.getShowStats())
            LexData.setShowStats(stats);

        Boolean condense = shared.getBoolean("condense",true);
        if (condense != LexData.getShowCondensed())
            LexData.setShowCondensed(condense);

        Boolean hooks = shared.getBoolean("hooks",true);
        if (hooks != LexData.getShowHooks())
            LexData.setShowHooks(hooks);

        Boolean quizHooks = shared.getBoolean("quizhooks",true);
        if (quizHooks != LexData.getShowQuixHooks())
            LexData.setShowQuizHooks(quizHooks);

        Boolean colorBlanks = shared.getBoolean("colorblank",true);
        if (colorBlanks != LexData.getColorBlanks())
            LexData.setColorBlanks(colorBlanks);

        Boolean fastScroll = shared.getBoolean("fastscroll",true);
        if (fastScroll != LexData.getFastScroll())
            LexData.setFastScroll(fastScroll);

        Boolean customKeyboard = shared.getBoolean("customkeyboard", false);
        if (customKeyboard != LexData.getCustomkeyboard()) {
            LexData.setCustomkeyboard(customKeyboard);
        }

        Boolean AutoAdvance = shared.getBoolean("autoadvance",false);
        if (AutoAdvance != LexData.getAutoAdvance())
            LexData.setAutoAdvance(AutoAdvance);

        Boolean loop = shared.getBoolean("loop",true);
        if (loop != LexData.getSlideLoop())
            LexData.setSlideLoop(loop);

        String fs = shared.getString("listfont", "24");
        if (Utils.isParsable(fs))
            listfontsize = Integer.parseInt(shared.getString("listfont","24"));
        else {
            prefs.putString("listfont", "24");
            prefs.apply();
            listfontsize = 24;
        }

        int tc = shared.getInt("tilecolor", 0xffffffff);
        if (tc != LexData.getTileColor(this))
            LexData.setTileColor(tc);

    }
    public void setLexicon(String lexname) {
        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexname);
        LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("setLex ", shared.getString("database", "") );
        Log.d("setLex ", shared.getString("lexicon", ""));
        Log.d("setLex ", LexData.getDatabasePath() + " - " + LexData.getDatabase());
        Log.d("setLex ", LexData.getLexName());

        // Don't attempt to extract in Splash
        if (!databaseAccess.checkLexicon(lexicon)) {
            Toast.makeText(getApplicationContext(), "Failed to load lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            databaseAccess.defaultDBLexicon(getApplicationContext());
            Utils.setDatabasePreference(this);
            Toast.makeText(getApplicationContext(), "Resetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
        }

        // don't want to show again from SubActivity
//        if (thisActivity.equals("SearchActivity")) {
//            Toast.makeText(this, "Using Lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
//            Toast.makeText(this, LexData.getLexicon().LexiconNotice, Toast.LENGTH_LONG).show();
//        }
        Utils.setLexiconPreference(this);

        // todo Do I need to checkScores like in Settings?
    }
    public void loadPrefixes() {
        List<String> prefixes = databaseAccess.get_prefixes();
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_spinner_item, prefixes);
                R.layout.simplespin, prefixes);
        begins.setAdapter(dataAdapter);
    }
    public void loadSuffixes() {
        List<String> suffixes = databaseAccess.get_suffixes();
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, suffixes);
        ends.setAdapter(dataAdapter);
    }
    public void loadMinimum() {
        List <String> lengths = new ArrayList<>();
        lengths.add("Minimum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, lengths); // spinmedium
        minimum.setAdapter(dataAdapter);

    }
    public void loadMaximum() {
        List <String> lengths = new ArrayList<>();
        lengths.add("Maximum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> maxAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, lengths); // R.layout.spinitem
        maximum.setAdapter(maxAdapter);
    }
    public void loadCategoryList() {
        List <String> categoryList = new ArrayList<>();
        categoryList = databaseAccess.get_categories();

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, categoryList);
        categories.setAdapter(categoryAdapter);
    }
    protected void populateResources(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setIcon(R.mipmap.howl);
        }
        catch (Exception e) {

        }
        String version = com.tylerhosting.hoot.hoot.BuildConfig.VERSION_NAME;
        String suffix = com.tylerhosting.hoot.hoot.BuildConfig.BUILD_TYPE;
        int code = com.tylerhosting.hoot.hoot.BuildConfig.VERSION_CODE;
        toolbar.setTitle("Hoot" + " " + version);

/*        if (suffix.equals("release"))
            toolbar.setTitle(getString(R.string.app_name) + " " + version);
        else
            toolbar.setTitle(getString(R.string.app_name) + " " + version + "(" + code + ")");
 */

        minimum = findViewById(R.id.MinLength);
        maximum = findViewById(R.id.MaxLength);
        begins = findViewById(R.id.BeginsWith);
        ends = findViewById(R.id.EndsWith);
        categories = findViewById(R.id.categories);

        loadPrefixes();
        loadSuffixes();
        loadCategoryList();

        sortby = findViewById(R.id.SortBy);
        thenby = findViewById(R.id.ThenBy);

        Underlay = findViewById(R.id.control_underlay);
        guide1 = findViewById(R.id.guide1);
        guide2 = findViewById(R.id.guide2);
        guide3 = findViewById(R.id.guide3);

        // setup spinners
        stype = findViewById(R.id.SearchType);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinselection, LexData.searchText);
        stype.setAdapter(dataAdapter);
        stype.setOnItemSelectedListener(selection);

        predef = findViewById(R.id.predefined);
        ArrayAdapter<String> predefAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, LexData.predefText);
        predef.setAdapter(predefAdapter);
        predef.setOnItemSelectedListener(predefined);

        stems = findViewById(R.id.stems);
        ArrayAdapter<String> stemsAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, LexData.stemsText);
        stems.setAdapter(stemsAdapter);
        stems.setOnItemSelectedListener(stemmed);

        sortby = findViewById(R.id.SortBy);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.sortby);
        sortby.setAdapter(sortAdapter);

        thenby = findViewById(R.id.ThenBy);
        ArrayAdapter<String> thenAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.thenby);
        thenby.setAdapter(thenAdapter);

        // setup other listeners
        etTerm = findViewById(R.id.etEntry);
        etTerm.addTextChangedListener(entryWatcher);
        etTerm.setOnEditorActionListener(entryAction);

        etFilter = findViewById(R.id.etFilter);
        etFilter.addTextChangedListener(filterWatcher);
        etFilter.setOnEditorActionListener(filterAction);

        if (themeName.equals("Dark Theme")) {
            etTerm.setText(null);
            etTerm.setHintTextColor(Color.GRAY);
            etFilter.setHintTextColor(Color.GRAY);
        }

        emptyRack = findViewById(R.id.EmptyRack);
        emptyRack.setOnClickListener(EmptyRack);

        more = findViewById(R.id.more);
        altSearch = findViewById(R.id.altSearch);


        clearEntry = findViewById(R.id.ClearEntry);
        clearEntry.setOnClickListener(clearTerm);
        clearBegins = findViewById(R.id.ClearBegins);
        clearBegins.setOnClickListener(clearBegin);
        clearEnds = findViewById(R.id.ClearEnds);
        clearEnds.setOnClickListener(clearEnd);

        slides = findViewById(R.id.btnSlides);
        slides.setOnClickListener(doSlides);

//anagramslides = findViewById(R.id.btnAnagramSlides);
//anagramslides.setOnClickListener(doAnagramSlides);

        quizreview = findViewById(R.id.btnQuizReview);
        quizreview.setOnClickListener(doQuizReview);

        quiz = findViewById(R.id.btnQuiz);
        quiz.setOnClickListener(doQuiz);

        clear = findViewById(R.id.btnClear);
        clear.setOnClickListener(doClear);

        newsearch = findViewById(R.id.btnNew);
        newsearch.setOnClickListener(doNewSearch);

        search = findViewById(R.id.Search);
        search.setOnClickListener(doSearch);

        collapse = findViewById(R.id.collapse);
        collapse.setOnClickListener(doCollapse);

        blank = findViewById(R.id.blank);
        blank.setOnClickListener(enterBlank);
        words.add("Empty");
        status = findViewById(R.id.lblStatus);
        specStatus = findViewById(R.id.specStatus);

        listView = findViewById(R.id.mcresults);
        scrollOverlay = findViewById(R.id.scroll_overlay);

        listView.setScrollBarFadeDuration(0);

        term = findViewById(R.id.lblTerm);
        term.setVisibility(GONE);

        etLimit = findViewById(R.id.NumWords);
        etOffset = findViewById(R.id.StartingWith);


        importfile = findViewById(R.id.tvImportFile);
        importfile.setOnClickListener(newFile);

        // fab
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                openAltSearch();

            }
        });
//        home = findViewById(R.id.gohome);
        fab.setAlpha(0.5f);
//        home.setAlpha(0.5f);



        // configure listview and listeners
        //******************* duplicate in subsearch
        final ListView lv = findViewById( R.id.mcresults);
        // Definition Display
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                TextView wrd = view.findViewById(R.id.word);
                selectedWord = wrd.getText().toString().toUpperCase();
//                if (stems.getVisibility() == VISIBLE) {
//                    getStemWords(selectedWord);
//                    return true;
//                }
                if (categories.getVisibility() == VISIBLE) {
                    // don't ucase category
                    selectedWord = wrd.getText().toString();
                    getListWords(selectedWord);
                    return true;

                }
                return false; // true indicates the action has been completed, false lets context menu show
            }
        });

        registerForContextMenu(lv);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView wrd = view.findViewById(R.id.word);
                selectedWord = wrd.getText().toString().toUpperCase();
                if (stems.getVisibility() == VISIBLE)
                    getStemWords(selectedWord);
                else if (categories.getVisibility() == VISIBLE) {
                    // don't ucase category
                    selectedWord = wrd.getText().toString();
                    getListWords(selectedWord);
                }
                else {

                    String option = LexData.getTapOption();
                    int search = R.id.anagrams;
                    switch (option) {
                        case "Definition":
                            databaseAccess.open();
                            Utils.wordDefinition(lv.getContext(), selectedWord, databaseAccess.getDefinition(selectedWord));
                            databaseAccess.close();
                            return;
                        case "Anagrams":
                            search = R.id.anagrams;
                            break;
                        case "Hook Words":
                            search = R.id.hookwords;
                            break;
                        case "Add to Card Box...":

                    }
                    Intent intentBundle = new Intent(SearchActivity.this, SubActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putInt("search", search);
                    bundle.putString("term", selectedWord);
                    bundle.putString("ordering", getSortOrder() );

                    intentBundle.putExtras(bundle);
                    intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
                    intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
                    startActivity(intentBundle);
                    overridePendingTransition (0, 0);//
                    return;
                }
            }
        });
    }
    private void setLimits() {
        // new value passing version
        if (etLimit.getText().toString().matches(""))
            limit = LexData.getMaxList();
        else
            limit = Integer.parseInt(etLimit.getText().toString());

        if (etOffset.getText().toString().matches(""))
            offset = 0;
        else
            offset = Integer.parseInt(etOffset.getText().toString());
        if (offset > 0)
            offset--; // zero based
    }
    private int fetchThemeColor(int themeColor) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = this.obtainStyledAttributes(typedValue.data, new int[] { themeColor });
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
    }
    private void showExpanded () {
        begins.setVisibility(VISIBLE);
        clearBegins.setVisibility(VISIBLE);
        ends.setVisibility(VISIBLE);
        clearEnds.setVisibility(VISIBLE);
        sortby.setVisibility(VISIBLE);
        thenby.setVisibility(VISIBLE);

        etFilter.setVisibility(VISIBLE);
        emptyRack.setVisibility(VISIBLE);
        etLimit.setVisibility(VISIBLE);
        etOffset.setVisibility(VISIBLE);

        search.setVisibility(VISIBLE);

        more.setVisibility(GONE);
        altSearch.setVisibility(GONE);
        specStatus.setVisibility(GONE);
    }
    private void showCondensed () {
        begins.setVisibility(GONE);
        clearBegins.setVisibility(GONE);
        ends.setVisibility(GONE);
        clearEnds.setVisibility(GONE);
        sortby.setVisibility(GONE);
        thenby.setVisibility(GONE);

        etFilter.setVisibility(GONE);
        emptyRack.setVisibility(GONE);
        etLimit.setVisibility(GONE);
        etOffset.setVisibility(GONE);

        search.setVisibility(GONE);

        more.setVisibility(VISIBLE);
        altSearch.setVisibility(VISIBLE);
        specStatus.setVisibility(VISIBLE);
//        generateSpecStats();

        ConstraintSet displayed = new ConstraintSet();
        ConstraintLayout layout = findViewById(R.id.search_layout);
        displayed.clone(layout);
        displayed.connect(R.id.imcheader,ConstraintSet.TOP, R.id.specStatus, ConstraintSet.BOTTOM);
        displayed.applyTo(layout);

    }
    public void openMore(View view) {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.moredialog, null);

        final Spinner moreBegins = alertLayout.findViewById(R.id.moreBegins);
        List<String> prefixes = databaseAccess.get_prefixes();
        final ArrayAdapter<String> beginAdapter = new ArrayAdapter<String>(context,
                R.layout.darkspin, prefixes);
        moreBegins.setAdapter(beginAdapter);
        // set based on last use
        moreBegins.setSelection(begins.getSelectedItemPosition());

        final Spinner moreEnds = alertLayout.findViewById(R.id.moreEnds);
        List<String> suffixes = databaseAccess.get_suffixes();
        ArrayAdapter<String> endAdapter = new ArrayAdapter<String>(context,
                R.layout.darkspin, suffixes);
        moreEnds.setAdapter(endAdapter);
        moreEnds.setSelection(ends.getSelectedItemPosition());

        final Spinner moreSort = alertLayout.findViewById(R.id.moreSortBy);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<String>(this,
                R.layout.darkspin, LexData.sortby);
        moreSort.setAdapter(sortAdapter);
        moreSort.setSelection(sortby.getSelectedItemPosition());

        final Spinner moreThen = alertLayout.findViewById(R.id.moreThenBy);
        ArrayAdapter<String> thenAdapter = new ArrayAdapter<String>(this,
                R.layout.darkspin, LexData.thenby);
        moreThen.setAdapter(thenAdapter);
        moreThen.setSelection(thenby.getSelectedItemPosition());

        final EditText moreFilter = alertLayout.findViewById(R.id.moreFilter);
        moreFilter.setHint("Rack Filter");
        moreFilter.setText(etFilter.getText());
        moreFilter.setTextColor(Color.BLACK);

        final EditText moreNumWords = alertLayout.findViewById(R.id.moreNumWords);
        moreNumWords.setInputType(InputType.TYPE_CLASS_NUMBER);
        moreNumWords.setHint("Word Limit");
        moreNumWords.setText(etLimit.getText());
        moreNumWords.setTextColor(Color.BLACK);

        final EditText moreStartWith = alertLayout.findViewById(R.id.moreStartWith);
        moreStartWith.setInputType(InputType.TYPE_CLASS_NUMBER);
        moreStartWith.setHint("Start with");
        moreStartWith.setText(etOffset.getText());
        moreStartWith.setTextColor(Color.BLACK);


        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);
        builder.setTitle("More Specifications");
        // this is set the view from XML inside AlertDialog
        builder.setView(alertLayout);
        // disallow cancel of AlertDialog on click of back button and outside touch

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // set value in form controls
                begins.setSelection(moreBegins.getSelectedItemPosition());
                ends.setSelection(moreEnds.getSelectedItemPosition());
                sortby.setSelection(moreSort.getSelectedItemPosition());
                thenby.setSelection(moreThen.getSelectedItemPosition());
                etFilter.setText(moreFilter.getText());
                etLimit.setText(moreNumWords.getText());
                etOffset.setText(moreStartWith.getText());

                generateSpecStats();

                StringBuilder specs = new StringBuilder();
                specs.append("Specs::" );
                if (begins.getSelectedItemPosition() != 0)
                    specs.append(" Begins " + begins.getSelectedItem().toString());
                if (ends.getSelectedItemPosition() != 0)
                    specs.append(" Ends " + ends.getSelectedItem().toString());
                if (sortby.getSelectedItemPosition() != 0)
                    specs.append(" Sort By " + sortby.getSelectedItem().toString());
                if (thenby.getSelectedItemPosition() != 0)
                    specs.append(" Then By " + thenby.getSelectedItem().toString());
                if (!etFilter.getText().toString().equals(""))
                    specs.append(" Rack " + etFilter.getText().toString());
                if (!etLimit.getText().toString().equals(""))
                    specs.append(" Limit " + etLimit.getText().toString());
                if (!etOffset.getText().toString().equals(""))
                    specs.append(" Beginning " + etOffset.getText().toString());

                specStatus.setText(specs);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setNeutralButton("Clear", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                moreBegins.setSelection(0);
                moreEnds.setSelection(0);
                moreSort.setSelection(0);
                moreThen.setSelection(0);
                moreFilter.setText("");
                moreNumWords.setText("");
                moreStartWith.setText("");

                begins.setSelection(moreBegins.getSelectedItemPosition());
                ends.setSelection(moreEnds.getSelectedItemPosition());
                sortby.setSelection(moreSort.getSelectedItemPosition());
                thenby.setSelection(moreThen.getSelectedItemPosition());
                etFilter.setText(moreFilter.getText());
                etLimit.setText(moreNumWords.getText());
                etOffset.setText(moreStartWith.getText());
                specStatus.setText("Other Specifications");
            }
        });
        builder.show();
    }
    protected void collapse() {

        if (collapse.isChecked()) {
            stype.setVisibility(GONE);


//replace Item Position with Item

//            switch (stype.getSelectedItem().toString() ) {
//                case "Stems":
//                    stems.setVisibility(GONE);
//                    break;
//            }

            switch (stype.getSelectedItemPosition() ) {
                case 13:
                    stems.setVisibility(GONE);
                    break;
                case 14:
                    predef.setVisibility(GONE);
                    break;
                case 15:
                    categories.setVisibility(GONE);
                    break;
                default:
                    etTerm.setVisibility(GONE);
                    break;
            }

/*            ;stype.setVisibility(GONE);
            ;predef.setVisibility(GONE);
            ;stems.setVisibility(GONE);
            ;categories.setVisibility(GONE);*/
            guide1.setVisibility(GONE);
            guide3.setVisibility(GONE);
            Underlay.setVisibility(GONE);
            minimum.setVisibility(GONE);
            maximum.setVisibility(GONE);
            begins.setVisibility(GONE);
            clearBegins.setVisibility(GONE);
            ends.setVisibility(GONE);
            clearEnds.setVisibility(GONE);
            //etTerm.setVisibility(GONE);
            blank.setVisibility(GONE);
            clearEntry.setVisibility(GONE);
            altSearch.setVisibility(GONE);
            sortby.setVisibility(GONE);
            thenby.setVisibility(GONE);
            search.setVisibility(GONE);

            etFilter.setVisibility(GONE);
            emptyRack.setVisibility(GONE);
            etLimit.setVisibility(GONE);
            etOffset.setVisibility(GONE);

            more.setVisibility(GONE);
            altSearch.setVisibility(GONE);
            specStatus.setVisibility(GONE);


            ConstraintSet hiding = new ConstraintSet();
            ConstraintLayout layout = findViewById(R.id.search_layout);
            hiding.clone(layout);
            hiding.connect(R.id.imcheader,ConstraintSet.TOP, R.id.collapse, ConstraintSet.BOTTOM);
            hiding.applyTo(layout);


        }
        else { // not collapsed
            stype.setVisibility(VISIBLE);
            switch (stype.getSelectedItemPosition()) {
                case 13:
                    stems.setVisibility(VISIBLE);
                    break;
                case 14:
                    predef.setVisibility(VISIBLE);
                    break;
                case 15:
                    categories.setVisibility(VISIBLE);
                    break;
                default:
                    etTerm.setVisibility(VISIBLE);
                    break;
            }
            guide1.setVisibility(VISIBLE);
            guide3.setVisibility(VISIBLE);
            Underlay.setVisibility(VISIBLE);
            minimum.setVisibility(VISIBLE);
            maximum.setVisibility(VISIBLE);

            blank.setVisibility(VISIBLE);
            clearEntry.setVisibility(VISIBLE);

            if (LexData.getShowCondensed()) {
                begins.setVisibility(GONE);
                clearBegins.setVisibility(GONE);
                ends.setVisibility(GONE);
                clearEnds.setVisibility(GONE);
                sortby.setVisibility(GONE);
                thenby.setVisibility(GONE);

                search.setVisibility(GONE);

                etFilter.setVisibility(GONE);
                emptyRack.setVisibility(GONE);
                etLimit.setVisibility(GONE);
                etOffset.setVisibility(GONE);

                more.setVisibility(VISIBLE);
                altSearch.setVisibility(VISIBLE);
                specStatus.setVisibility(VISIBLE);

                ConstraintSet displayed = new ConstraintSet();
                ConstraintLayout layout = findViewById(R.id.search_layout);
                displayed.clone(layout);
                displayed.connect(R.id.imcheader,ConstraintSet.TOP, R.id.specStatus, ConstraintSet.BOTTOM);
                displayed.applyTo(layout);
            }
            else {
                begins.setVisibility(VISIBLE);
                clearBegins.setVisibility(VISIBLE);
                ends.setVisibility(VISIBLE);
                clearEnds.setVisibility(VISIBLE);
                sortby.setVisibility(VISIBLE);
                thenby.setVisibility(VISIBLE);

                search.setVisibility(VISIBLE);

                etFilter.setVisibility(VISIBLE);
                emptyRack.setVisibility(VISIBLE);
                etLimit.setVisibility(VISIBLE);
                etOffset.setVisibility(VISIBLE);

                more.setVisibility(GONE);
                altSearch.setVisibility(GONE);
                specStatus.setVisibility(GONE);

                ConstraintSet displayed = new ConstraintSet();
                ConstraintLayout layout = findViewById(R.id.search_layout);
                displayed.clone(layout);
                displayed.connect(R.id.imcheader,ConstraintSet.TOP, R.id.Search, ConstraintSet.BOTTOM);
                displayed.applyTo(layout);
            }



//            search.setVisibility(VISIBLE);

        }
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(outState);
        // Save our own state now
        outState.putAll(outState);
    }
    public void openSettings() {
        // Do something in response to button
        databaseAccess.close();
        Log.wtf("Tablet", "openSettings");
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName() );
        intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
        startActivity(intent);
    }
    public void openTools() {
        Intent intent = new Intent(this, ToolsActivity.class);
        startActivity(intent);
    }

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


    // OPTIONS MENU
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (id == R.id.activity_multi) {
            Intent intent = new Intent(this, MultiSearchActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.activity_cards) {
            if (!LexData.getCardslocation().equals("Internal"))
                if (!Utils.permission(this)) {
                    Toast.makeText(this, "Hoot doesn't have permission to write to storage", Toast.LENGTH_LONG).show();
                    return false;
                }

            Intent intent = new Intent(this, CardBoxActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.activity_timeclock) {
            Intent intent = new Intent(this, ClockActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.tile_tracker) {
            Intent intent = new Intent(this, TileTrackerActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.activity_judge) {
            Intent intent = new Intent(this, WordJudgeActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.activity_lookup) {
            Intent intent = new Intent(this, LookupActivity.class);
            startActivity(intent);
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            boolean myKeyboard = LexData.getCustomkeyboard();

            hidekeyboard();
            openSettings();

            // process changes made in settings
//            readPreferences(); // onResume
            resetDatabase();

            if (myKeyboard != LexData.getCustomkeyboard()) {
                recreate();
//                startActivity(getIntent());
//                finish();
//                overridePendingTransition(0, 0);
            }




//            setDisplay(); // onResume
        }

        if (id == R.id.activity_help) {
            getHelp("help.html");

//            Intent intent = new Intent(this, HelpActivity.class);
//            startActivity(intent);
            return true;
        }

        if (id == R.id.about_activity) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_tools) {
            openTools();
            resetDatabase();
//            String lexiconName = shared.getString("lexicon","");
//            Log.d("Lexicon", "shared: " + lexiconName + "Lexdata: " + LexData.getLexName());
//            if (!lexiconName.equals(LexData.getLexName())) {
//                resetDatabase();
//                Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexiconName);
//                LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);
//            }



            return true;
        }

        if (id == R.id.exit) {
            Utils.exitAlert(this);
        }

        return super.onOptionsItemSelected(item);
    }


    // CONTEXT MENU
    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_subsearch, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }
    @Override public boolean onContextItemSelected(MenuItem item) {

        CardDatabase cardDatabase;
        String[] words;
        int counter = 0;
        int column;

        int choice;
        choice = item.getItemId();
        if (choice < 1)
            return false;

        switch (choice) {
            case R.id.definition:
                databaseAccess.open();
                Utils.wordDefinition(this, selectedWord, databaseAccess.getDefinition(selectedWord));
                databaseAccess.close();
                return true;

            case R.id.addcards:
                return true;

            case R.id.other:
                return true;

                // MenuItem subitem = subm.getItem(R.id.addcards);
//                subchoice = subm.getItem().getItemId();

            case R.id.anagramcards:
                // Setup and get database
                if (LexData.getCardslocation().equals("Internal"));
                else
                if (!Utils.permission(this)) {
                    Toast.makeText(this, "Hoot doesn't have permission to write to storage. Maybe try again.", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardBox = new LexData.Cardbox("Hoot", LexData.getLexName(), "Anagrams");
                LexData.setCardfile(cardBox);

                if (!createCardFolder()) {
                    Toast.makeText(this, "Failed to mkdirs", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
                cards = cardDatabase.getWritableDatabase();

                // Create word list from cursor
                words = new String[cursor.getCount()];
                column = cursor.getColumnIndex("Word");

                cursor.moveToFirst();
                words[counter] = cursor.getString(column).toUpperCase();
                while (cursor.moveToNext()) {
                    counter++;
                    words[counter] = cursor.getString(column).toUpperCase();
                }

                // Create anagram list and add to table
                String[] anagrams = databaseAccess.getAnagrams(words);
                counter = CardDatabase.addCards(cards, anagrams);
                cards.close();
                Toast.makeText(this, "Added " + counter + "  words to Anagrams", Toast.LENGTH_LONG).show();
                return true;
            case R.id.hookcards:
                // Setup and get database
                if (LexData.getCardslocation().equals("Internal"));
                else
                if (!Utils.permission(this)) {
                    Toast.makeText(this, "Hoot doesn't have permission to write to storage", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardBox = new LexData.Cardbox("Hoot", LexData.getLexName(), "Hooks");
                LexData.setCardfile(cardBox);

                if (!createCardFolder()) {
                    Toast.makeText(this, "Failed to mkdirs", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
                cards = cardDatabase.getWritableDatabase();



                // Create word list from cursor
                column = cursor.getColumnIndex("Word");
                int fh = cursor.getColumnIndex("FrontHooks");
                int bh = cursor.getColumnIndex("BackHooks");

                List<String> ww = new ArrayList<>();


                cursor.moveToFirst();
                if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0)
                    ww.add(cursor.getString(column).toUpperCase());

                while (cursor.moveToNext()) {
                    if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0)
                        ww.add(cursor.getString(column).toUpperCase());
                }

                // Create anagram list and add to table
                counter = CardDatabase.addWords(cards, ww.toArray(new String[0]));

                cards.close();
                Toast.makeText(this, "Added " + counter + "  words to Hooks", Toast.LENGTH_LONG).show();
                return true;

            case R.id.blankanagramcards:
                // Setup and get database
                if (LexData.getCardslocation().equals("Internal"));
                else
                if (!Utils.permission(this)) {
                    Toast.makeText(this, "Hoot doesn't have permission to write to storage", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardBox = new LexData.Cardbox("Hoot", LexData.getLexName(), "BlankAnagrams");
                LexData.setCardfile(cardBox);

                if (!createCardFolder()) {
                    Toast.makeText(this, "Failed to mkdirs", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
                cards = cardDatabase.getWritableDatabase();

                // Create word list from cursor
                words = new String[cursor.getCount()];
                column = cursor.getColumnIndex("Word");

                cursor.moveToFirst();
                words[counter] = cursor.getString(column).toUpperCase();
                while (cursor.moveToNext()) {
                    counter++;
                    words[counter] = cursor.getString(column).toUpperCase();
                }

                // Create anagram list and add to table
                String[] blankanagrams = databaseAccess.getAnagrams(words);
                counter = CardDatabase.addCards(cards, blankanagrams);
                cards.close();
                Toast.makeText(this, "Added " + counter + "  words to Blank Anagrams", Toast.LENGTH_LONG).show();
                return true;

            case R.id.listcards:
                LexData.HList hList = new LexData.HList();

                if (LexData.getCardslocation().equals("Internal"));
                else
                if (!Utils.permission(this)) {
                    Toast.makeText(this, "Hoot doesn't have permission to write to storage", Toast.LENGTH_LONG).show();
                    return true;
                }

                cardBox = new LexData.Cardbox("Hoot", LexData.getLexName(), "Lists");
                LexData.setCardfile(cardBox);

                if (!createCardFolder()) {
                    Toast.makeText(this, "Failed to mkdirs", Toast.LENGTH_LONG).show();
                    return true;
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this, R.style.darkAlertDialog);

                builder.setTitle("Quiz Type and List Title");

                LayoutInflater inflater = this.getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.listoptions, null);
                builder.setView(dialogView);

                // set the custom dialog components - text, image and button
                final EditText listtitle = (EditText) dialogView.findViewById(R.id.listtitle);
                listtitle.setHint("List Title");
                listtitle.setTextColor(Color.WHITE);

                final String[] qtype = getResources().getStringArray(R.array.qtypes);

                Spinner quiztype = (Spinner) dialogView.findViewById(R.id.quiztype);
                ArrayAdapter<String> qtypeAdapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, LexData.quiztypes);
                        //R.layout.spinselection, LexData.quiztypes);
                quiztype.setAdapter(qtypeAdapter);
                quiztype.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        quiztypeSelection[0] = position + 1;
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // TODO Auto-generated method stub

                    }
                });


                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        cancelled[0] = true;
                    }
                });

                builder.setPositiveButton( "Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // if (listtitle.getText().length() < 1)
                        // prompt for text
                        // else dismiss, etc

                        quiztitle[0] = listtitle.getText().toString().trim();
                        LexData.HList hList = new LexData.HList();

                        hList.quiz_type = quiztypeSelection[0];
                        hList.title = listtitle.getText().toString().trim();


                        Toast.makeText(getApplicationContext(), qtype[quiztypeSelection[0]-1] + " - " + listtitle.getText().toString().trim(), Toast.LENGTH_LONG).show();
                        if (hList.title.length() < 1)
                            cancelled[0] = true;
                        else
                            addList(hList);

                        dialog.dismiss();
                    }
                });
                builder.show();


//                if (cancelled[0])
                    return true;

            case R.id.savelist:
                getFileSpec(listView);

                /*
                AlertDialog.Builder saver = new AlertDialog.Builder(SearchActivity.this, R.style.LightTheme);
                saver.setTitle("Save List to File");

                Context savecontext = SearchActivity.this;
                LinearLayout layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);

//                final EditText A = new EditText(savecontext);
//                A.setInputType(InputType.TYPE_CLASS_TEXT);
//                A.setHint("Player A");
//                layout.addView(A);

                final EditText filename = new EditText(savecontext);
                filename.setInputType(InputType.TYPE_CLASS_TEXT);
                filename.setHint("File Name");
                layout.addView(filename);

                saver.setView(layout); // Again this is a set method, not add

                saver.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (filename.length() < 1)
                            dialog.cancel(); // cancelled[0] = true;
                        else {
                            try {
                                saveList(filename.getText().toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                saver.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                saver.show();

                 */
                return true;
            case R.id.copylist:
                copyList();
                return true;


                //hList.title = "Testing2";
                //hList.quiz_type = Utils.getSearchType("Anagrams");
            default:
                Toast.makeText(this, "Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();

                // this get the position of the item in listview
                //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                //search = info.position;

                Intent intentBundle = new Intent(SearchActivity.this, SubActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("term", selectedWord);
                bundle.putInt("search", choice);
                bundle.putString("ordering", getSortOrder());

                intentBundle.putExtras(bundle);
                intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
                intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
                startActivity(intentBundle);
                overridePendingTransition(0, 0);//
                return true;
        }
    }
    private void copyList() {
// https://developer.android.com/guide/topics/text/copy-paste
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        StringBuilder clipping = new StringBuilder();
        int column = cursor.getColumnIndex("Word");
        String sep;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            sep = System.lineSeparator();
        } else sep = "\n";

        cursor.moveToFirst();
        clipping.append(cursor.getString(column).toUpperCase() + sep);
        int counter = 1;
        while (cursor.moveToNext()) {
            clipping.append(cursor.getString(column).toUpperCase() + sep);
            counter++;
        }
        Toast.makeText(getBaseContext(), "Copied " + counter + "words to clipboard ", Toast.LENGTH_LONG).show();

        // Creates a new text clip to put on the clipboard
        ClipData clip = ClipData.newPlainText("simple text", clipping.toString());

        clipboard.setPrimaryClip(clip);

    }
    private void getFileSpec(View view) {
        if (!Utils.permission(this)) {
            Toast.makeText(this, "Hoot doesn't have permission to write to storage. Maybe try again.", Toast.LENGTH_LONG).show();
            return;
        }

        AlertDialog.Builder saver = new AlertDialog.Builder(SearchActivity.this, R.style.darkAlertDialog); // R.style.LightTheme);
        saver.setTitle("Save List to File");

        Context savecontext = SearchActivity.this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);


//                final EditText A = new EditText(savecontext);
//                A.setInputType(InputType.TYPE_CLASS_TEXT);
//                A.setHint("Player A");
//                layout.addView(A);

        final EditText filename = new EditText(savecontext);
        filename.setInputType(InputType.TYPE_CLASS_TEXT);
        filename.setHint("File Name");
        filename.setTextColor(Color.WHITE);
        layout.addView(filename);

        saver.setView(layout); // Again this is a set method, not add

        saver.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (filename.length() < 1)
                    dialog.cancel(); // cancelled[0] = true;
                else {
                    try {
                        saveList(filename.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        saver.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        saver.show();
    }
    private static final int CREATE_FILE = 1;
    private static final int REQUEST_FOLDER = 9;
    private static final int PICK_TXT_FILE = 2;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
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
            }
        }

        if (requestCode == CREATE_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.

            Uri uri = null;
            if (resultData != null) {
                Utils.writeList(this, cursor, resultData.getData());

//                writeList(resultData.getData());
                // Perform operations on the document using its URI.


            }
        }

    }
    private void saveList(String filename) throws IOException {
        String DEST_PATH;
        int counter = 0;
        int column = cursor.getColumnIndex("Word");

        if (!filename.endsWith(".txt"))
            filename = filename + ".txt";

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || saf) {
        if (Utils.usingSAF()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);

            startActivityForResult(intent, CREATE_FILE);
        }
        else {

            FileWriter writer;

            try {
                DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

                File directory = new File(DEST_PATH);
                if (!directory.exists())
                    if (!directory.mkdirs()) {
                        Log.e("savelist", "Can't mkdirs()" + directory.getAbsolutePath());
                        Toast.makeText(getBaseContext(), "Can't create folder for saving", Toast.LENGTH_LONG).show();
                        return;
                    }
                filename = DEST_PATH + File.separator + filename;


                writer = new FileWriter(filename);

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


    // Searching
    public class MCListAdapter extends SimpleCursorAdapter {
        protected Context mContext;
        protected int id;
        protected List<String> items;

        public MCListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mContext = context;
            id = layout;
        }
        @Override
        public void bindView(View v, Context context, Cursor c) {
            int fh = c.getColumnIndex("FrontHooks");
            int ifh = c.getColumnIndex("InnerFront");
            int word = c.getColumnIndex("Word");
            int ibh = c.getColumnIndex("InnerBack");
            int bh = c.getColumnIndex("BackHooks");

            int pr = c.getColumnIndex("ProbFactor");
            int an = c.getColumnIndex("Anagrams");
            int pl = c.getColumnIndex("OPlayFactor");
            int sc = c.getColumnIndex("Score");

            String fhCol = c.getString(fh);
            String ifhCol = c.getString(ifh);
            String wordCol = c.getString(word);
            String ibhCol = c.getString(ibh);
            String bhCol = c.getString(bh);

            String prCol = c.getString(pr);
            String anCol = c.getString(an);
            String plCol = c.getString(pl);
            String scCol = c.getString(sc);

            int hooksfontsize = (int) (listfontsize * .9);
            TextView fhview = v.findViewById(R.id.fh);
            if (fhview != null) {
                fhview.setTextSize(hooksfontsize);
                fhview.setText(fhCol);
            }
            TextView ifhview = v.findViewById(R.id.ifh);
            if (ifhview != null) {
                ifhview.setTextSize(hooksfontsize);
                ifhview.setText(ifhCol);
            }
            TextView wordview = v.findViewById(R.id.word);
            if (wordview != null) {
                wordview.setTextSize(listfontsize);

                if (LexData.getColorBlanks()) {
                    boolean hasBlanks = false;
                    StringBuilder sb = new StringBuilder();
                    for (int index = 0; index < wordCol.length(); index++) {
                        char letter = wordCol.charAt(index);
                        if (Character.isLowerCase(letter)) {
                            hasBlanks = true;

                            switch (themeName) {
                                case "Dark Theme":
                                    sb.append("<font color='#00ff88'>" + (char) (letter - 32) + "</font>");
                                    break;
                                case "Light Theme":
                                default:
                                    sb.append("<font color='#0033ee'>" + (char) (letter - 32) + "</font>");
                                    break;
                            }

                        } else
                            sb.append(wordCol.charAt(index));
                    }
//                    Log.i("Word", wordCol + ": " + sb);

                    if (hasBlanks) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wordview.setText(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY));
                        } else
                            wordview.setText(Html.fromHtml(sb.toString()));
                    } else
                        wordview.setText(wordCol);
                }


                else
                    wordview.setText(wordCol);
            }
            TextView ibhview = v.findViewById(R.id.ibh);
            if (ibhview != null) {
                ibhview.setTextSize(hooksfontsize);
                ibhview.setText(ibhCol);
            }
            TextView bhview = v.findViewById(R.id.bh);
            if (bhview != null) {
                bhview.setTextSize(hooksfontsize);
                bhview.setText(bhCol);
            }

            TextView prview = v.findViewById(R.id.pr);
            if (prview != null) {
                prview.setText(prCol);
            }
            TextView anview = v.findViewById(R.id.an);
            if (anview != null) {
                anview.setText(anCol);
            }
            TextView plview = v.findViewById(R.id.pl);
            if (plview != null) {
                plview.setText(plCol);
            }
            TextView scview = v.findViewById(R.id.score);
            if (scview != null) {
                scview.setText(scCol);
            }
        }

    }
    public class SpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            // An item was selected. You can retrieve the selected item using
            // parent.getItemAtPosition(pos)
        }

        public void onNothingSelected(AdapterView<?> parent) {
            // Another interface callback
        }
    }
    protected final AdapterView.OnItemSelectedListener selection = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            if (stype.getSelectedItemPosition() == 20)
                mKeyboardView.setKeyboard(defKeyboard);
            else if (stype.getSelectedItemPosition() != 3) {
                String before = etTerm.getText().toString();
                String after = before.replaceAll("[\\[\\]<>cv*@0123456789.,^+-]", "");
                etTerm.setText(after);
                mKeyboardView.setKeyboard( bKeyboard );
            }
            else
                mKeyboardView.setKeyboard( pKeyboard );


//                etTerm.setKeyListener(KeyListener.getInstance("ABCDEFGHIJKLMNOPQRSTUVWXYZ?"));
//            else
//                etTerm.setKeyListener(DigitsKeyListener.getInstance("cvABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789*?.,^+[](|){}\\~-"));

            etTerm.setVisibility(VISIBLE);
            blank.setVisibility(VISIBLE);
            predef.setVisibility(GONE);
            stems.setVisibility(GONE);
            categories.setVisibility(GONE);
            importfile.setVisibility(GONE);
//            etLimit.setVisibility(GONE);
//            etOffset.setVisibility(GONE);


            if (stype.getSelectedItemPosition() == 5) // word builder
                sortby.setSelection(2); // length descending

            switch (stype.getSelectedItemPosition() ) {
                case 2:
                    // size only changed after type selected
                    if (etTerm.getText().length() > 0)
                        if (minimum.getSelectedItemPosition() - 1 < etTerm.getText().length() )
                            minimum.setSelection(etTerm.getText().length());
                    break;
                case 13:
                    etTerm.setVisibility(GONE);
                    stems.setVisibility(VISIBLE);
                    predef.setVisibility(GONE);
                    categories.setVisibility(GONE);
                    break;
                case 14:
                    etTerm.setVisibility(GONE);
                    stems.setVisibility(GONE);
                    predef.setVisibility(VISIBLE);
                    categories.setVisibility(GONE);
                    break;
                case 15:
                    etTerm.setVisibility(GONE);
                    stems.setVisibility(GONE);
                    predef.setVisibility(GONE);
                    categories.setVisibility(VISIBLE);
                    break;
                case 3:
                 case 11:
                case 12:
                case 16:
                case 17:
                case 18: // alt Ending
                case 19: // replace
                    Toast.makeText(getApplicationContext(),"\nSelect lengths for quicker results\n", Toast.LENGTH_LONG).show();
                    break;

                case 20: // With Definition
                    break;

                case 21:
//                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                    importfile.setVisibility(VISIBLE);
                    etTerm.setVisibility(INVISIBLE);
                    blank.setVisibility(INVISIBLE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        selectFile();
                    }
                    break;


                // move to Predefined
                // create method for Study List
                // setVisibilities here
                // process Search elsewhere


                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener predefined = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // cursor = getPredefined(predef.getSelectedItemPosition());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener stemmed = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // cursor = getPredefined(predef.getSelectedItemPosition());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    protected void executeSearch() {

//        when using number input, hide keyboard

        String trim = etTerm.getText().toString().trim();
        if (!stype.getSelectedItem().toString().equals("With Definition"))
            etTerm.setText(String.valueOf(trim));

        int cursorPosition = etTerm.getSelectionEnd();
//        if (etTerm.getText().toString().length() >= 0) {
//            String trim = etTerm.getText().toString().trim();
//            etTerm.setText(String.valueOf(trim));
//        }

//        if (etTerm.getText().toString().contains(" ")) {
            //status.setText("Invalid search string");
//            Toast.makeText(this,"Search string contains invalid characters", Toast.LENGTH_SHORT).show();
//            return;
//        }

        unfiltered = (etFilter.getText().length() == 0);
        lStartTime = System.nanoTime();
        status.setText(R.string.searching);
        searchParameters.setLength(0);

        ordering = getSortOrder();
        setLimits();
        limits = Utils.limitStringer(limit,offset);
        Log.d("getCursor", limits);

//// NEED TO COMPARE LAST VERSION ////
//        hideKeyboard(this);
        hidekeyboard();

        // ?? needed because resetDatabase doesn't change database
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

        databaseAccess.open();
        // if History, save search

        int least = minimum.getSelectedItemPosition() + 1;
        int most = maximum.getSelectedItemPosition() + 1;
        if (most == 1) { // If Any, assume same as least
            most = least;
            maximum.setSelection(most - 1); //
        }

        if (etTerm.getVisibility() == VISIBLE) {
        cursor = getCursor(stype.getSelectedItemPosition());
        searchParameters.append(etTerm.getText());
        searchParameters.append(makeDesc());
        }
        else {
            if (predef.getVisibility() == VISIBLE) {
                cursor = getPredefined(predef.getSelectedItemPosition());
                searchParameters.append(predef.getSelectedItem().toString());
                searchParameters.append(makeDesc());
            }
            else {
                if (stems.getVisibility() == VISIBLE) {
                    int stemtype = 2, length = 7;
                    switch (stems.getSelectedItemPosition()) {
                        case 0:
                            stemtype = 1;
                            length = 6;
                            break;
                        case 1:
                            stemtype = 2;
                            length = 6;
                            break;
                        case 2:
                            stemtype = 1;
                            length = 7;
                            break;
                        case 3:
                            stemtype = 2;
                            length = 7;
                            break;
                    }
                    cursor = databaseAccess.getCursor_Stems(stemtype, length);
                    searchParameters.append(stems.getSelectedItem().toString());
                    searchParameters.append(makeDesc());
                }
                else {
                    if (importfile.getVisibility() == VISIBLE)
                        cursor = getCursor(stype.getSelectedItemPosition());
                    else {
                        cursor = databaseAccess.getCursor_Subjects(categories.getSelectedItem().toString());
                        searchParameters.append(categories.getSelectedItem().toString());
                        searchParameters.append(makeDesc());
                    }
                }
            }
        }


        lastPrefix = begins.getSelectedItemPosition();
        lastSuffix = ends.getSelectedItemPosition();
        lastMin = minimum.getSelectedItemPosition();
        lastMax = maximum.getSelectedItemPosition();
        lastCategory = categories.getSelectedItemPosition();

        if (cursor == null) { // ?? when thread running
            return;
        }

//        if (specStatus.getVisibility() == VISIBLE)
        generateSpecStats();

        if (LexData.getMaxList() > 0) {
            if (cursor.getCount() == LexData.getMaxList())
                displayPartialResults();
            else
                displayResults();
        }
        else
            displayResults();

        etTerm.setSelection(cursorPosition);
    }
    public void doSearch2(View view) {
        executeSearch();
    }
    protected void incompleteSearch() {
        status.setText("Incomplete search parameters");
        Toast.makeText(this,"Cannot complete the search with these parameters", Toast.LENGTH_SHORT).show();
    }
    protected void incompleteSearch(String message) {
        status.setText("Incomplete search parameters");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    protected Cursor getCursor(int searchType) {
        String term = etTerm.getText().toString().trim();
        String beginning = begins.getSelectedItem().toString();
        String ending = ends.getSelectedItem().toString();

        if (!(searchType == 3)) {
            term = term.replaceAll("[\\[\\]<>cv0123456789.,^+-]", "");
            etTerm.setText(term);
        }
        if (!(searchType == 0 || searchType == 3)) {
            term = term.replaceAll("[*@]", "");
            etTerm.setText(term);
        }

        String filters = makefilters(searchType);

        int least = minimum.getSelectedItemPosition() + 1;
        int most = maximum.getSelectedItemPosition() + 1;

        if (least > most && most != 1) {
            incompleteSearch("Maximum length can't be less than minimum.");
            return null;
        }

        if (most == 1 && least > 1) { // If Any, assume same as least
            most = least;
            maximum.setSelection(most - 1); //
        }


        // todo get from control, method
        switch (searchType) {
            case 0: // anagrams and blank anagrams
                if (term.trim().length() > 1) {
                    if (term.contains("*") || term.contains("@")) {
                        String letters = term.replaceAll("[*@]", "");
                        searchThread(this, letters, filters);
                        return cursor;
                    } else //
                        return databaseAccess.getCursor_blankAnagrams(term, filters, ordering, limit, offset,etFilter.getText().length()  > 0);
                    //return databaseAccess.getCursor_anagrams(term, filters, ordering);
                }
                else
                    incompleteSearch();
                break;
            case 1:
                etTerm.setText("");


                if (least > 1) {

                    if (etFilter.getText().length() > 0) {
                        filters = makefilters(5);
                        return databaseAccess.getCursor_subanagrams(etFilter.getText().toString(), filters, ordering, limit, offset, etFilter.getText().length()  > 0);
                    }


                    if (least == most)
                        return databaseAccess.getCursor_ByLetterCount(least, filters, ordering, limits, etFilter.getText().length()  > 0);
                    else {
                        if (most == 1) { // If Any, assume same as least
                            most = least;
                            maximum.setSelection(most - 1); //
                            return databaseAccess.getCursor_ByLetterCount(least, filters, ordering, limits,etFilter.getText().length()  > 0);
                        }
                        else
                            return databaseAccess.getCursor_BetweenLengths(least, most, filters, ordering, limits, etFilter.getText().length()  > 0);
                    }
                }
                else
                    incompleteSearch("Please select a length.");
                break;
            case 2:
                if (term.trim().length() > 0) {
                    return databaseAccess.getCursor_hookwords(term, filters, ordering, limits, etFilter.getText().length() > 0);
                }
                else
                    // need to set min/max like in lettercount ???
                    if (least > 1)
                        return databaseAccess.getCursor_gethashooks(filters, ordering, limit, offset,etFilter.getText().length()  > 0);
                    else
                        incompleteSearch("Please select a length.");
                break;
            case 3:
                if (term.trim().length() > 1) {
                    // if no size specified, set using
                    getpatternThread(this, term, filters, ordering);
                    return null; // Thread displays results
                }
                else
                    incompleteSearch();
                break;
            case 4:
                if (term.trim().length() > 0)
                    return databaseAccess.getCursor_contains(term, filters, ordering, limits,etFilter.getText().length()  > 0);
                else
                    incompleteSearch();
                break;
            case 5: // word builder
                if (term.trim().length() > 1) {
                    if (term.trim().length() > 6) {
                        searchThread(this, term, filters);
                        return cursor;
                    }
                    else
                        return databaseAccess.getCursor_subanagrams(term, filters, ordering, limit, offset,etFilter.getText().length()  > 0); // short words
                }
                else
                    incompleteSearch();
                break;
            case 6:
            case 7:
            case 16:
            case 17:
                if (term.trim().length() > 0) {
                    searchThread(this, term, filters);
                    return cursor;
                    //return databaseAccess.getCursor_superanagrams(term, filters, ordering);
                }
                else
                    incompleteSearch();
                break;

//            case 7:
//                if (term.trim().length() > 0) {
//                    searchThread(this, term, filters);
//                    return cursor;
//                    //return databaseAccess.getCursor_containsAny(term, filters, ordering);
//                }
//                else
//                    incompleteSearch();
//                break;

            case 8:
                if (begins.getSelectedItemPosition() == 0)
                    beginning = "";
//                if (!"".equals(term.trim())) {
                if (term.trim().length() != 0) {
                    if (begins.getSelectedItemPosition() != 0)
                        begins.setSelection(0);
                    beginning = term;
                }
 //               Log.i("Begins", "-" + term + "-");
//                Log.i("Begins", "-" + beginning + "-");

//                if (!"".equals(begins.trim()))
                if (beginning.trim().length() != 0)
                    return databaseAccess.getCursor_begins(beginning, filters, ordering, limits, etFilter.getText().length()  > 0);
                else
                    incompleteSearch();
                break;
            case 9: // ends
                if (ends.getSelectedItemPosition() == 0)
                    ending = "";
                if (term.trim().length() != 0) {
                    if (ends.getSelectedItemPosition() != 0)
                        ends.setSelection(0);
                    ending = term;
                }
                if (ending.trim().length() != 0)
                    return databaseAccess.getCursor_ends(ending, filters, ordering, limits, etFilter.getText().length()  > 0);
                else
                    incompleteSearch();
                break;

            //  SUBWORDS
            case 10: // subwords
                if (term.trim().length() > 1) {
                    if (least == 1 || most == 1) {
                        least = 2;
                        most = term.length();
                        minimum.setSelection(least-1);
                        maximum.setSelection(most-1);
                    }
                    if (most > term.length()) {
                        most = term.length();
                        maximum.setSelection(most - 1);
                    }
                    return databaseAccess.getCursor_subwords(least, most, term);
                }
                else
                    incompleteSearch();
                break;

            case 11: // parallel
                String validate = term.replaceAll("[^A-Za-z]+", "");
                if (validate.trim().length() > 1) {
                    getparallelThread(this, term, makeParallelFilters(11), ordering);
                    return null; // Thread displays results
                }
                else
                    incompleteSearch();
                break;

            case 12: // splitters
                // move to thread
                if (term.trim().length() > 0) {
                    if (least >= (term.trim().length()) + 4 || most >= (term.trim().length()) + 4) {
                        Toast.makeText(this,"Searching... Please wait...", Toast.LENGTH_LONG).show();

//                        return databaseAccess.getCursor_splitters(least, most, term, filters, ordering, etFilter.getText().length()  > 0);
                        searchThread(this, term, filters);
                        return cursor;
                    }
                    else {
                        int required = (term.length() + 4);
                        incompleteSearch("For " + term + ", length must be at least " + required + " letters");
                    }
                }
                else
                    incompleteSearch();
                break;

            case 13:
            case 14:
            case 15:
                break;
            case 18:
            if (term.trim().length() > 0) {
                if ((!term.contains("?")) && (ends.getSelectedItemPosition() == 0))
                    Toast.makeText(getApplicationContext(), "\nMust select or enter original and alternate ending\n", Toast.LENGTH_LONG).show();
                else {
                    searchThread(this, term, filters);
                    return cursor;
                }
            }
            else
                incompleteSearch();
            break;

            case 19:
                if (term.trim().length() > 0) {
                    if ((!term.contains("?")))
                        Toast.makeText(getApplicationContext(), "\nMust select or enter original and alternate ending\n", Toast.LENGTH_LONG).show();
                    else {
                        searchThread(this, term, filters);
                        return cursor;
                    }
                }
                else
                    incompleteSearch();
                break;

            // Stems
            // Predefined
            // Subject Lists


//            case 16: // isPrefix (process with case 6:
//                if (term.trim().length() > 1) {
//                    searchThread(this, term, filters);
//                    return cursor;
//                }
//                else
//                    incompleteSearch();
//                break;

//            case 160:
//                Toast.makeText(this,"Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();
//                if (term.trim().length() > 0)
//                    return databaseAccess.isPrefix(term, adjustedfilters(searchType), ordering, limit, offset,etFilter.getText().length()  > 0);
//                else
//                    incompleteSearch();
//                break;
//            case 170:
//                Toast.makeText(this,"Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();
//                if (term.trim().length() > 0)
//                    return databaseAccess.isSuffix(term, adjustedfilters(searchType), ordering, limit, offset,etFilter.getText().length()  > 0);
//                else
//                    incompleteSearch();
//                break;


            case 20:
                if (!term.isEmpty()) {
                    cursor = databaseAccess.getCursor_withDef(etTerm.getText().toString());
                    return cursor;
                }
                break;



            case 21: // From File
                if (importfile.getText().toString().equals("File specification") ||
                        importfile.getText().toString().isEmpty()) {
                    incompleteSearch("File must be selected");
                }
                else {

//                    String words = Utils.GetTextFile(this, importfile.getText().toString());
                    String words = Utils.GetTextFile(this, importfile.getPrivateImeOptions().toString());


                    cursor = databaseAccess.getCursor_getWords(words, ordering, limits, filters);
                    Log.d("words in file", "cursor " + cursor.getCount());

                    return cursor;
                }

            default:
                Toast.makeText(this,"Select Search Type to begin", Toast.LENGTH_SHORT).show();
//                Utils.wordDefinition(this, term, databaseAccess.getDefinition((term)));
                break;
        }
        // make sure keyboard is closed
        hidekeyboard();
//        hideKeyboard(this);
/*        try {
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {

        }
        */
        return null;
    }
    protected Cursor getPredefined(int searchType) {
        String incl;
        ordering = getSortOrder();

        // todo get from control, method
//        databaseAccess.open(); // opened in executeSearch
        switch (searchType) {

            case 0:
                minimum.setSelection(2-1); // 2 letter
                maximum.setSelection(2-1); // 2 letter
                return databaseAccess.getCursor_ByLetterCount(2, makefilters(11), ordering, limits,etFilter.getText().length()  > 0);
            case 1:
                minimum.setSelection(3-1); // 3 letter
                maximum.setSelection(3-1); // 3 letter
                return databaseAccess.getCursor_containsAny(LexData.valueLetters(7,10), makefilters(11), ordering, limit, offset,etFilter.getText().length()  > 0);
            case 2:
                minimum.setSelection(3-1); // 3 letter
                maximum.setSelection(3-1); // 3 letter
                return databaseAccess.getCursor_ByLetterCount(3, makefilters(11), ordering, limits,etFilter.getText().length()  > 0);
            case 3:
                minimum.setSelection(4-1); // 4 letter
                maximum.setSelection(4-1); // 4 letter
                return databaseAccess.getCursor_containsAny(LexData.valueLetters(7,10), makefilters(11), ordering, limit, offset,etFilter.getText().length()  > 0);

            // todo replace including with tileValue
            case 4: //Top BE Fours (JQXZ)
                minimum.setSelection(4-1); // 4 letters
                maximum.setSelection(4-1); // 4 letter
                incl = LexData.valueSet(7,10);
                return databaseAccess.getCursor_HighPlays(makefilters(11), ordering, incl, 4, limit, offset,etFilter.getText().length()  > 0);
            case 5: //High BE Fours
                minimum.setSelection(4-1); // 4 letter"FHKVWY"
                maximum.setSelection(4-1); // 4 letter
                incl = LexData.valueSet(4,6);
                return databaseAccess.getCursor_HighPlays(makefilters(11), ordering, incl, 4, limit, offset,etFilter.getText().length()  > 0);
            case 6: //Top BE Fives (JQXZ)
                minimum.setSelection(5-1); // 5 letter
                maximum.setSelection(5-1); // 5 letter
                incl = LexData.valueSet(7,10);
                return databaseAccess.getCursor_HighPlays(makefilters(11), ordering, incl, 5, limit, offset,etFilter.getText().length()  > 0);
            case 7: //High BE Fives
                minimum.setSelection(5-1); // 5 letter
                maximum.setSelection(5-1); // 5 letter
                incl = LexData.valueSet(4,6);
                return databaseAccess.getCursor_HighPlays(makefilters(11), ordering, incl, 5, limit, offset,etFilter.getText().length()  > 0);
            case 10: // Q not U
                return databaseAccess.getCursor_qNotu(makefilters(11), ordering, limit, offset,etFilter.getText().length()  > 0);   //                 11qnotu,
            case 12: // Hookless
                return databaseAccess.getCursor_getnohooks(makefilters(11), ordering, limit, offset,etFilter.getText().length()  > 0);

            case 8: // Vowel Heavy
            case 9: // Consonant Dumps
            case 11: // Palindromes
            case 13: // Unique Hooks
            case 14: // Hooked
            case 18: // Alt Ending
                searchThread(this, "", makefilters(11));
                return cursor;

//            case 14: // Unique
//            case 15: //

            //Palindromes
            //       Semordnilaps
            //Hookless words

            default:
                Toast.makeText(this,"Select Search Type to begin", Toast.LENGTH_SHORT).show();
//                Utils.wordDefinition(this, "", databaseAccess.getDefinition(("ERROR")));
                break;
        }
        // make sure keyboard is closed
        hidekeyboard();
//        hideKeyboard(this);
/*        try {
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {

        }*/
        return null;
    }
    public void getStemWords(String word) {
        Toast.makeText(this,"Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();

        Intent intentBundle = new Intent(SearchActivity.this, SubActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("term", word); // subsearch adds blank

        int subsearchtype = R.id.blankanagrams;
        bundle.putInt("search", subsearchtype);
        //        bundle.putString("filters", makefilters(subsearchtype));
        bundle.putString("ordering", getSortOrder() );

        intentBundle.putExtras(bundle);

        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//

        startActivity(intentBundle);

        overridePendingTransition (0, 0);//
    }
    public void getListWords(String word) {
        //        Toast.makeText(this,"Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();

        Intent intentBundle = new Intent(SearchActivity.this, SubActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("term", word); // subsearch adds blank

        int subsearchtype = R.id.categories;
        bundle.putInt("search", subsearchtype);
        //        bundle.putString("filters", makefilters(subsearchtype));
        bundle.putString("ordering", getSortOrder() );
        intentBundle.putExtras(bundle);

        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//

        startActivity(intentBundle);

        overridePendingTransition (0, 0);//

    }
    protected void executeFilter() {
//        if (etTerm.getVisibility() == VISIBLE)

        cursor = databaseAccess.getCursor_rackfilter(etFilter.getText().toString(), cursor, limit, offset);
        // don't filter stems, predefined, stems, subject lists

    }


    // Threads
    public Cursor searchThread(final Context context, final String term, final String filters){
        @SuppressLint("StaticFieldLeak")
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            protected ProgressDialog dialog;
            //            protected Cursor tcursor;
            protected MatrixCursor matrixCursor;
            String ordering = getSortOrder();
            int searchType;

            @Override
            protected void onPreExecute()
            {
                databaseAccess.open();


                if (etTerm.getVisibility() == VISIBLE)
                    searchType = stype.getSelectedItemPosition();
                if (predef.getVisibility() == VISIBLE)
                    searchType = predef.getSelectedItemPosition();
                //               Log.i("Search Type", Integer.toString(searchType));
//                etTerm.getVisibility();

                this.dialog = Utils.themeDialog(context);
/*                switch (themeName) {
                    case "Dark Theme":
                        this.dialog = new ProgressDialog(context,R.style.darkProgressBar);
                        break;
                    case "Light Theme":
                    default:
                        this.dialog = new ProgressDialog(context);
                        break;
                }

 */



                this.dialog.setMessage("Please Wait!\r\nThis search may take a minute or two...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //dialog.dismiss();
                    }
                });                this.dialog.setCancelable(true);
                this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        // cancel AsyncTask
                        cancel(false);
                    }
                });
                this.dialog.setCanceledOnTouchOutside(false);
                this.dialog.show();
            }

            @Override
            protected Void doInBackground(Void... params)
            {
                skips = 0;
                Cursor precursor;
                Cursor altcursor;
                List<String> alternates;
                matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                        "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
                String lenFilter; // = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                // String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) <= %2$s", LexData.getMaxLength(), term.length());
                char[] a = term.toCharArray(); // anagram
                int[] first = new int[26]; // letter count of anagram
                int c; // array position
                int blankcount = 0;
//                databaseAccess.open();
                StringBuilder speedFilter;
                String alpha;
                // threads from searches
                switch (searchType) {
                    case 5:

                        // contains only (subanagrams, word builder)
                        if (term.trim() == "")
                            return null;
                        lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) <= %2$s", LexData.getMaxLength(), term.length());

                        for (c = 0; c < a.length; c++) { // initialize word to anagram
                            if (a[c] == '?') {
                                blankcount++;
                                continue;
                            }
                            first[a[c] - 'A']++;
                        }


                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                //speedFilter +
                                filters +
                                " ) " + ordering);
                        while (precursor.moveToNext()) {
//                            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
                            String word = precursor.getString(1);
                            char[] anagram = word.toCharArray();

                            if (databaseAccess.isAnagram(first, anagram, blankcount)) {


                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                    int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                    int letter;
                                    for (int x = 0; x < 26; x++)
                                        blanks[x] = 0;
                                    for (c = 0; c < anagram.length; c++) {
                                        letter = ++second[anagram[c] - 'A'];
                                        if (letter > first[anagram[c] - 'A']) {
                                            blanks[anagram[c] - 'A']++;
                                        }
                                    }

                                    if (LexData.getColorBlanks())
                                        matrixCursor.addRow(get_BlankCursorRow(precursor, blanks));
                                    else
                                        matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);


                            }

                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
                        //tcursor = databaseAccess.getCursor_subanagrams(term, filters, ordering);
                        break;
                    case 0:
                    case 6:
                        // superanagrams, contains all
//                        if (term.trim().length() < 2)
                        //                          return null;
                        lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());

                        // initialize word to anagram
                        for (c = 0; c < a.length; c++) {
                            if (a[c] == '?') {
                                blankcount++;
                                continue;
                            }
                            first[a[c] - 'A']++;
                        }

                        alpha = term.replaceAll("[^A-Za-z]+", "");
                        speedFilter = new StringBuilder();
//                        for (int letter = 0; letter < alpha.length() && letter < 3; letter++)
                        for (int letter = 0; letter < alpha.length(); letter++)
                            speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter, letter + 1) + "%' ");

                        // LIKE filters the initial search by the first letter
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                " WHERE (" + lenFilter +
                                speedFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(cache.getColumnIndex(precursor, "Word"));
//                            String word = precursor.getString(precursor.getColumnIndex("Word"));
                            char[] b = word.toCharArray();
                            if (databaseAccess.containsall(first, b, blankcount)) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                }


                                else
                                    cancel(false);
                            }

//                            if (LexData.getMaxList() > 0 && unfiltered)
//                                if (matrixCursor.getCount() == LexData.getMaxList())
//                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
                        break;
                    case 7:
                        //tcursor = databaseAccess.getCursor_containsAny(term, filters, ordering);
                        if (term.trim() == "")
                            return null;

                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        // initialize word to anagram
                        for (c = 0; c < a.length; c++) {
                            if (a[c] == '?') {
                                blankcount++;
                                continue;
                            }
                            first[a[c] - 'A']++;
                        }

                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(cache.getColumnIndex(precursor, "Word"));
//                            String word = precursor.getString(1);
                            char[] b = word.toCharArray();

                            if (databaseAccess.containsany(first, b)) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);
                            }
//
//
//
//
//
//
//                                matrixCursor.addRow(get_CursorRow(precursor));
//
//
//                            }
//                            if (LexData.getMaxList() > 0 && unfiltered)
//                                if (matrixCursor.getCount() == LexData.getMaxList())
//                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }

                        precursor.close();
                        cursor = matrixCursor;
                        break;

                    // threads from predefined
                    case 8:
//                        tcursor = databaseAccess.getCursor_vowelheavy(filters, ordering);         //   9vowels,
                        String sizeFilter = filters;
                        //                       Log.i("Size", sizeFilter + ":" + filters);
                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());

                        if (sizeFilter == "")
                            sizeFilter = "1";
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                sizeFilter +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(precursor.getColumnIndex("Word"));
                            //String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
                            int size = word.length();
                            int vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
                            //vowelcount = word.Count(chr => vowels.Contains(chr));
                            int percentage = (100 * vowelcount) / size;
                            if ((percentage > 74 && size > 2) || (percentage > 61 && size > 3) || (percentage > 58 && size > 7)) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);
                            }

//
//
//                                matrixCursor.addRow(get_CursorRow(precursor));
//                            if (LexData.getMaxList() > 0 && unfiltered)
//                                if (matrixCursor.getCount() == LexData.getMaxList())
//                                    cancel(false);






                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }

                        precursor.close();
                        cursor = matrixCursor;
                        break;
                    case 9:
//                        tcursor = databaseAccess.getCursor_consonantDumps(filters, ordering);                    //10novowels,
                        sizeFilter = filters;
                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        if (sizeFilter == "")
                            sizeFilter = "1";
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                sizeFilter +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
                            int vowelcount = 0;
                            int size = word.length();

                            vowelcount = size - word.replaceAll("A|E|I|O|U|", "").length();
                            int percentage = (100 * vowelcount) / size;
                            if (percentage < 18) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);

                            }




//                                matrixCursor.addRow(get_CursorRow(precursor));
//                            if (LexData.getMaxList() > 0 && unfiltered)
//                                if (matrixCursor.getCount() == LexData.getMaxList())
//                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }

                        precursor.close();
                        cursor = matrixCursor;
                        break;
                    case 11:
//                        tcursor = databaseAccess.getCursor_getpalins(filters, ordering);
                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        boolean palin;
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
                            //String word = cursor.getString(cursor.getColumnIndex("Word"));
                            palin = true;
                            char[] b = word.toCharArray();
                            for (int i = 0; i < b.length / 2; i++) {
                                if (b[i] != b[b.length - i - 1])
                                    palin = false;
                            }
                            if (palin == true) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);

                            }




//                                matrixCursor.addRow(get_CursorRow(precursor));
//                            if (LexData.getMaxList() > 0 && unfiltered)
//                                if (matrixCursor.getCount() == LexData.getMaxList())
//                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
                        break;
                    case 12:
                        // moving joins/splitters here
//                        public Cursor getCursor_splitters(int min, int max, String letters, String filters, String ordering, boolean rack) {
                        int max = maximum.getSelectedItemPosition() + 1;
                        int min = minimum.getSelectedItemPosition() + 1;
                        if (max > LexData.getMaxLength())
                            max = LexData.getMaxLength();
                        lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", max, min);

                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                " WHERE (" + lenFilter +
                                filters +
                                " AND Word LIKE '__%" + term + "%__'" +
                                " ) " + ordering);

//                        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
//                                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

                        String front, back;
                        while (precursor.moveToNext()) {
                            String text = precursor.getString(precursor.getColumnIndex("Word"));
                            // LOOK FOR 0 BASED INDEX OF INSERTED LETTER
                            for (int i = 2; i < (text.length() - term.length()) ; i++)
                            {
                                if (text.substring(i, i + term.length()).equals( term))
                                {
                                    front = text.substring(0, i );
                                    back = text.substring(i + term.length());

                                    if (databaseAccess.wordJudge(front)) {
                                        if (databaseAccess.wordJudge(back)) {
                                            // replace letter with lower case
                                            String finding = front + term.toLowerCase() + back;
                                            String [] columnValues = new String[11];
                                            for (int j = 0; j < 11; j++)
                                                columnValues[j] = precursor.getString(j);
                                            columnValues[precursor.getColumnIndex("Word")] = finding;
                                            matrixCursor.addRow(columnValues);
                                            if (unfiltered)
                                                if (LexData.getMaxList() > 0)
                                                    if (matrixCursor.getCount() == LexData.getMaxList())
                                                        break;
                                        }
                                    }
                                }
                            }
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }

                        }
                        precursor.close();
                        cursor = matrixCursor;



/*
// Semordnilaps dropped from predefined
//                        public Cursor getCursor_getsemos(String filters, String ordering) {
                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String primary = precursor.getString(1);
                            StringBuilder builder = new StringBuilder();
                            // append a string into StringBuilder input1
                            builder.append(primary);
                            // reverse StringBuilder input1
                            String backward = builder.reverse().toString();
                            if (databaseAccess.wordJudge(backward))
                                matrixCursor.addRow(get_CursorRow(precursor));
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
                        break;

 */
//                        cursor =  databaseAccess.getCursor_getnohooks(filters, ordering);
/*                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        String sql = "SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +

                                " AND Trim(FrontHooks) = '' AND Trim(BackHooks) = '' " +

                                filters +
                                " ) " + ordering;
                        Log.e("sql", sql);

                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +

                                " AND Trim(FrontHooks) = '' AND Trim(BackHooks) = '' " +

                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                if (limit == 0 || !unfiltered || limit > position) {
                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
*/
//                        while (precursor.moveToNext()) {
//                            //String word = cursor.getString(cursor.getColumnIndex("Word"));
//                            String fronthooks = precursor.getString(precursor.getColumnIndex("FrontHooks"));
//                            String backhooks = precursor.getString(precursor.getColumnIndex("BackHooks"));
//                            if (fronthooks.trim().length() + backhooks.trim().length() == 0) {
//
//                                if (unfiltered)
//                                    if (offset > skips) {
//                                        skips++;
//                                        continue;
//                                    }
//                                position = matrixCursor.getCount();
//                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
//                                if (limit == 0 || !unfiltered || limit > position) {
//
//
//                                    matrixCursor.addRow(get_CursorRow(precursor));
//                                } else
//                                    cancel(false);
//
//                            }
//
//                            if (isCancelled()) {
//                                precursor.close();
//                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
//                                cursor = matrixCursor;
//                                break;
//                            }
//                        }
//                        precursor.close();
//                        cursor = matrixCursor;
                        break;

                    case 13:
                        // Unique Hooks
                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            //String word = cursor.getString(cursor.getColumnIndex("Word"));
                            String fronthooks = precursor.getString(precursor.getColumnIndex("FrontHooks"));
                            String backhooks = precursor.getString(precursor.getColumnIndex("BackHooks"));
                            if (fronthooks.trim().length() + backhooks.trim().length() == 1) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);

                            }




//                                matrixCursor.addRow(get_CursorRow(precursor));
//                            if (LexData.getMaxList() > 0 && unfiltered)
//                                if (matrixCursor.getCount() == LexData.getMaxList())
//                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
                        break;

//TEST THIS
                    case 14:

                        // Hooked Words (i.e. has internal hook)

                        lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {

                            String ifh = precursor.getString(precursor.getColumnIndex("InnerFront"));
                            String ibh = precursor.getString(precursor.getColumnIndex("InnerBack"));
                            if (ifh.trim().length() + ibh.trim().length() != 0) {

                                if (unfiltered)
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                position = matrixCursor.getCount();
                                Log.d("Values", position + "p" + offset + "o" + skips + "s");
                                if (limit == 0 || !unfiltered || limit > position) {


                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    cancel(false);

                            }

                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        cursor = matrixCursor;
                        break;

                    case 16:
                        skips = 0;
                        Cursor searcher;

                        if (term.trim() == "")
                            return null;

                        beginning = "";
                        ending = "";

                        if (begins.getSelectedItemPosition() != 0)
                            beginning = begins.getSelectedItem().toString();
                        if (ends.getSelectedItemPosition() != 0)
                            ending = ends.getSelectedItem().toString();

                        if (maximum.getSelectedItemPosition() == 0)
                            lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());
                        else
                            lenFilter = "";

                        if (term.trim().length() == 1) {
                            precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                    "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                    "FROM     `" + LexData.getLexName() + "` \n" +
                                    "WHERE (" + "Word LIKE '" + beginning + "%" + ending + "' " +
                                    "AND FrontHooks LIKE '%" + term.toLowerCase() + "%' " +
                                    lenFilter +
                                    adjustedfilters(searchType) +
                                    " ) " + ordering);

                            while (precursor.moveToNext()) {
                                if (unfiltered) {
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                    position = matrixCursor.getCount();
                                    if (limit > 0)
                                        if (position == limit)
                                            break;
                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    matrixCursor.addRow(get_CursorRow(precursor));
                            }
                        }

                        else {
                            int prefixLen = term.length() + 1;

                            // get prospects list
                            precursor = databaseAccess.rawQuery("SELECT Substr(Word, " + prefixLen + ") " +
                                    "FROM     `" + LexData.getLexName() + "` \n" +
                                    "WHERE (" + "Word LIKE '" + term + "%' " +
                                    " )");

                            StringBuilder inputList = new StringBuilder();
                            if (!precursor.moveToFirst()) {
                                precursor.close();
                                break;
                            }
                            inputList.append("'" + precursor.getString(0) + "'");

                            while (precursor.moveToNext()) {
                                inputList.append(", '" + precursor.getString(0) + "'");
                            }

                            // do search with IN list restriction
                            Cursor postcursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                    "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                    "FROM     `" + LexData.getLexName() + "` \n" +
                                    "WHERE Word IN (" + inputList + " )" +
                                    lenFilter +
                                    adjustedfilters(searchType) +
                                    "  " + ordering);


                            while (postcursor.moveToNext()) {
                                if (unfiltered) {
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                    position = matrixCursor.getCount();
                                    if (limit > 0)
                                        if (position == limit)
                                            break;
                                }
                                matrixCursor.addRow(get_CursorRow(postcursor));
                                if (isCancelled()) {
                                    postcursor.close();
                                    //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                    cursor = matrixCursor;
                                    break;
                                }

                            }
                            postcursor.close();
                        }
//                        else                         }{
//
//
//
//
//                            // looking up longer words so adjust length filter
//                            int adj = term.length();
//                            max = maximum.getSelectedItemPosition() + 1 + adj;
//                            min = minimum.getSelectedItemPosition() + 1 + adj;
//                            if (max > LexData.getMaxLength())
//                                max = LexData.getMaxLength();
//                            lenFilter = String.format(" AND Length(Word) <= %1$s AND Length(Word) >= %2$s", max, min);
//
//                            precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
//                                    "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
//                                    "FROM     `" + LexData.getLexName() + "` \n" +
//                                    "WHERE (" + "Word LIKE '" + term + beginning + "%" + ending + "' " +
//                                    lenFilter +
////                                    adjustedfilters(searchType) +
//                                    " ) " + ordering);
//
//
//
//                            String fhooked;
//                            while (precursor.moveToNext()) {
//
//                                String word = precursor.getString(1);
//                                fhooked = word.substring(adj); // word without prefix
//
//                                searcher = databaseAccess.getCursor_findWord(fhooked); // test word(index, index + length)
//                                if (searcher.getCount() > 0) {
//                                    searcher.moveToFirst();
//
//                                    if (unfiltered) {
//                                        if (offset > skips) {
//                                            skips++;
//                                            continue;
//                                        }
//                                        position = matrixCursor.getCount();
//                                        if (limit > 0)
//                                            if (position == limit)
//                                                break;
//                                        matrixCursor.addRow(get_CursorRow(searcher));
//                                    } else
//                                        matrixCursor.addRow(get_CursorRow(searcher));
//                                }
//                            }
//                            if (isCancelled()) {
//                                precursor.close();
//                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
//                                cursor = matrixCursor;
//                                break;
//                            }
//
//                        }

                        cursor = matrixCursor;
                        break;

                    case 17:
                        skips = 0;
                        if (term.trim().equals(""))
                            return null;
                        beginning = "";
                        ending = "";

                        if (begins.getSelectedItemPosition() != 0)
                            beginning = begins.getSelectedItem().toString();
                        if (ends.getSelectedItemPosition() != 0)
                            ending = ends.getSelectedItem().toString();

                        int suffixSize = term.length();

                        if (maximum.getSelectedItemPosition() == 0)
                            lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());
                        else
                            lenFilter = "";

                        if (term.trim().length() == 1) {
                            precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                    "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                    "FROM     `" + LexData.getLexName() + "` \n" +
                                    "WHERE (" + "Word LIKE '" + beginning + "%" + ending + "' " +
                                    "AND BackHooks LIKE '%" + term.toLowerCase() + "%' " +
                                    lenFilter +
                                    adjustedfilters(searchType) +
                                    " ) " + ordering);

                            while (precursor.moveToNext()) {
                                if (unfiltered) {
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                    position = matrixCursor.getCount();
                                    if (limit > 0)
                                        if (position == limit)
                                            break;
                                    matrixCursor.addRow(get_CursorRow(precursor));
                                } else
                                    matrixCursor.addRow(get_CursorRow(precursor));
                            }
                        }
                        else {
                            int prefixLen = term.length() - 1;

                            // get prospects list
                            precursor = databaseAccess.rawQuery("SELECT Substr(Word, 0, Length(Word) - " + prefixLen  + ") " +
                                    "FROM     `" + LexData.getLexName() + "` \n" +
                                    "WHERE (" + "Word LIKE '%" + term + "' " +
                                    " )");

                            StringBuilder inputList = new StringBuilder();
                            if (!precursor.moveToFirst()) {
                                precursor.close();
                                break;
                            }
                            inputList.append("'" + precursor.getString(0) + "'");

                            while (precursor.moveToNext()) {
                                inputList.append(", '" + precursor.getString(0) + "'");
                            }

                            // do search with IN list restriction
                            Cursor postcursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                    "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                    "FROM     `" + LexData.getLexName() + "` \n" +
                                    "WHERE Word IN (" + inputList + " )" +
                                    lenFilter +
                                    adjustedfilters(searchType) +
                                    "  " + ordering);


                            while (postcursor.moveToNext()) {
                                if (unfiltered) {
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                    position = matrixCursor.getCount();
                                    if (limit > 0)
                                        if (position == limit)
                                            break;
                                }
                                matrixCursor.addRow(get_CursorRow(postcursor));
                                if (isCancelled()) {
                                    postcursor.close();
                                    //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                    cursor = matrixCursor;
                                    break;
                                }

                            }
                            postcursor.close();
                        }

//                        else {
//                            int adj = term.length();
//                            max = maximum.getSelectedItemPosition() + 1 + adj;
//                            min = minimum.getSelectedItemPosition() + 1 + adj;
//                            if (max > LexData.getMaxLength())
//                                max = LexData.getMaxLength();
//                            lenFilter = String.format(" AND Length(Word) <= %1$s AND Length(Word) >= %2$s", max, min);
//
//                            precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
//                                    "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
//                                    "FROM     `" + LexData.getLexName() + "` \n" +
////                                "WHERE (" + "Word LIKE '%" + term + "' " +
//                                    "WHERE (" + "Word LIKE '" + beginning + "%" + ending + term + "' " +
//                                    lenFilter +
//                                    " ) " + ordering);
//
//
//                            int bhsize;
//                            String bhooked;
//                            //Cursor searcher;
//                            while (precursor.moveToNext()) {
//
//                                String word = precursor.getString(1);
//                                bhsize = word.length() - suffixSize;
//                                bhooked = word.substring(0, bhsize); // word without suffix
//
//
////                            if (databaseAccess.wordJudge(bhooked)) {
//
//                                searcher = databaseAccess.getCursor_findWord(bhooked); // test word(index, index + length)
//                                if (searcher.getCount() > 0) {
//                                    searcher.moveToFirst();
//
//                                    if (unfiltered) {
//                                        if (offset > skips) {
//                                            skips++;
//                                            continue;
//                                        }
//                                        position = matrixCursor.getCount();
//                                        if (limit > 0)
//                                            if (position == limit)
//                                                break;
//                                        matrixCursor.addRow(get_CursorRow(searcher));
//                                    } else
//                                        matrixCursor.addRow(get_CursorRow(searcher));
//                                }
//                            }
//                            if (isCancelled()) {
//                                precursor.close();
//                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
//                                cursor = matrixCursor;
//                                break;
//                            }
//
//                        }
                        precursor.close();
                        cursor = matrixCursor;
                        break;


                    case 18:
                        // alternate ending
                        skips = 0;
                        if (term.trim().equals(""))
                            return null;
                        beginning = "";
                        ending = "";
                        String altEnding = "";
                        String orgEnding = "";
                        String altOption = LexData.getAltOption();


                        if (begins.getSelectedItemPosition() != 0)
                            beginning = begins.getSelectedItem().toString();
                        if (ends.getSelectedItemPosition() != 0)
                            ending = ends.getSelectedItem().toString();

//                        customKeyboard doesn't allow '>', so using '?'
//                        replacing needed for processing of split'
                        if (term.contains("?")) {
                            String altSet = term.trim().replace("?", ">");
                            String column[] = altSet.split(">");
                            orgEnding = column[0];
                            altEnding = column[1];
//                            Log.e("col", orgEnding);
                        }
                        else {
                            altEnding = term;
                            orgEnding = ending;
                        }


                        if (maximum.getSelectedItemPosition() == 0)
                            lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());
                        else
                            lenFilter = "";

                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
//                                "WHERE (" + "Word LIKE '" + beginning + "%" + ending + "' " +
                                "WHERE (" + "Word LIKE '" + beginning + "%" + orgEnding + "' " +
                                lenFilter +
                                orgfilters() +
                                " ) " + ordering);

                        altcursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
//                                "WHERE (" + "Word LIKE '" + beginning + "%" + ending + "' " +
                                "WHERE (" + "Word LIKE '" + beginning + "%" + altEnding + "' " +
                                lenFilter +
                                altfilters(altEnding.length() - orgEnding.length()) +
                                " ) " + ordering);



                        alternates = new ArrayList<>();
                        while (altcursor.moveToNext()) {
                            String word2find = altcursor.getString(1);
                            alternates.add(word2find);
                            //Log.e("Alt", word2find);
                        }

//                        String[] alternates = new String[altcursor.getCount()];
//                        int counter = 0;
//                        altcursor.moveToFirst();
//                        alternates[counter] = altcursor.getString(1).toUpperCase();
//                        while (altcursor.moveToNext()) {
//                            counter++;
//                            alternates[counter] = altcursor.getString(1).toUpperCase();
//                        }



                        while (precursor.moveToNext()) {
                            String orgWord = precursor.getString(1);
                            String altWord = replaceLast(orgWord, orgEnding, altEnding);

                            if (!alternates.contains(altWord))
                                continue;
//                            Log.e("Alt", altWord);

                            searcher = databaseAccess.getCursor_findWord(altWord); // test word(index, index + length)
                            if (searcher.getCount() > 0) {
                                searcher.moveToFirst();

                                if (unfiltered) {
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                    position = matrixCursor.getCount();
                                    if (limit > 0)
                                        if (position == limit)
                                            break;
                                }
                                switch (altOption) {
                                    default:
                                    case "Original":
                                        matrixCursor.addRow(get_CursorRow(precursor));
                                        break;
                                    case "Alternate":
                                        matrixCursor.addRow(get_CursorRow(searcher));
                                        break;
                                    case "Both":
                                        matrixCursor.addRow(get_CursorRow(precursor));
                                        matrixCursor.addRow(get_CursorRow(searcher));
                                        break;
                                }
                            }
                            searcher.close();
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }


                        }

                        precursor.close();
                        cursor = matrixCursor;
                        break;

                    case 19:
                        // replace
                        skips = 0;
                        if (term.trim().equals(""))
                            return null;
                        if (!term.contains("?"))
                            return null;
                        beginning = "";
                        ending = "";
                        String altString = "";
                        String orgString = "";
                        altOption = LexData.getAltOption();


                        if (begins.getSelectedItemPosition() != 0)
                            beginning = begins.getSelectedItem().toString();
                        if (ends.getSelectedItemPosition() != 0)
                            ending = ends.getSelectedItem().toString();

                        String altSet = term.trim().replace("?", ">");
                        String column[] = altSet.split(">");
                        orgString = column[0];
                        altString = column[1];
//                            Log.e("col", orgEnding);
//                        }
//                        else {
//                            altEnding = term;
//                            orgEnding = ending;
//                        }


                        if (maximum.getSelectedItemPosition() == 0)
                            lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());
                        else
                            lenFilter = "";

                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
//                                "WHERE (" + "Word LIKE '" + beginning + "%" + ending + "' " +
                                "WHERE (" + "Word LIKE '" + beginning + "%" + orgString + "%" + ending + "' " +

//                                " AND " + "Word LIKE '%" + term + "%' " +

                                lenFilter +
                                orgfilters() +
                                " ) " + ordering);

                        altcursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
//                                "WHERE (" + "Word LIKE '" + beginning + "%" + ending + "' " +
                                "WHERE (" + "Word LIKE '" + beginning + "%" + altString + "%" + ending + "' " +
                                lenFilter +
                                altfilters(altString.length() - orgString.length()) +
                                " ) " + ordering);



                        alternates = new ArrayList<>();

                        while (altcursor.moveToNext()) {
                            String word2find = altcursor.getString(1);
                            alternates.add(word2find);
                            Log.e("Alt", word2find);
                        }
                        altcursor.close();

                        while (precursor.moveToNext()) {
                            String orgWord = precursor.getString(1);
                            String altWord = orgWord.replace(orgString, altString);

                            if (!alternates.contains(altWord))
                                continue;
                            Log.e("Alt", altWord);

                            searcher = databaseAccess.getCursor_findWord(altWord); // test word(index, index + length)
                            if (searcher.getCount() > 0) {
                                searcher.moveToFirst();

                                if (unfiltered) {
                                    if (offset > skips) {
                                        skips++;
                                        continue;
                                    }
                                    position = matrixCursor.getCount();
                                    if (limit > 0)
                                        if (position == limit)
                                            break;
                                }
                                switch (altOption) {
                                    default:
                                    case "Original":
                                        matrixCursor.addRow(get_CursorRow(precursor));
                                        break;
                                    case "Alternate":
                                        matrixCursor.addRow(get_CursorRow(searcher));
                                        break;
                                    case "Both":
                                        matrixCursor.addRow(get_CursorRow(precursor));
                                        matrixCursor.addRow(get_CursorRow(searcher));
                                        break;
                                }
                            }
                            searcher.close();
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }


                        }

                        precursor.close();
                        cursor = matrixCursor;
                        break;




                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                //called on ui thread
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                cursor = matrixCursor;
//                databaseAccess.close();
                displayResults();
                return;
            }
            @Override
            protected void onCancelled()//called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                dialog.cancel();
                cursor = matrixCursor; // un-uncommented
//                databaseAccess.close();
                displayPartialResults();
            }

        };
        task.execute();
        return null;
    }
    public Cursor getpatternThread(final Context context, final String pattern, final String filters, final String ordering){
        @SuppressLint("StaticFieldLeak")
        final AsyncTask<Void, Void, Cursor> task = new AsyncTask<Void, Void, Cursor>() {
            protected MatrixCursor matrixCursor;
            protected ProgressDialog dialog;

            @Override
            protected void onPreExecute()
            {
//                databaseAccess.open(); // opened in executeSearch
//                Log.i("Pattern", pattern);
                this.dialog = Utils.themeDialog(context);
/*                switch (themeName) {
                    case "Dark Theme":
                        this.dialog = new ProgressDialog(context,R.style.darkProgressBar);
                        break;
                    case "Light Theme":
                    default:
                        this.dialog = new ProgressDialog(context);
                        break;
                }

 */

                this.dialog.setMessage("Please Wait!\r\nThis search may take a minute or two...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //dialog.dismiss();
                    }
                });

                this.dialog.setCancelable(false);
                this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        // cancel AsyncTask
                        cancel(false);
                    }
                });
                this.dialog.setCanceledOnTouchOutside(false);
                this.dialog.show();
            }

            @Override
            protected Cursor doInBackground(Void... params)
            {
                skips = 0;
                String prep = databaseAccess.buildInnerPattern(pattern);
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
                matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                        "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

                String sql = "SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                        "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                        "FROM     `" + LexData.getLexName() + "` \n" +
                        "WHERE (" + lenFilter +
                        frontfilter + backfilter +
                        filters +
                        " ) " + ordering ;
                Cursor precursor = databaseAccess.rawQuery(sql);

//                Log.i("Exp", exp);
                try {
                    Pattern.compile(exp);
                } catch (PatternSyntaxException e) {
//                    Log.i("Exp", exp);

                }

                // enable cancel of search
                try {
                    while (precursor.moveToNext()) {
//                        String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
                        String word = precursor.getString(1);




//                        if (word.matches(exp)) {
//
//                            if (offset > 0) {
//                                if (position < offset - skips) {
//                                    skips++;
//                                } else
//                                    matrixCursor.addRow(get_CursorRow(cursor));
//                            }
//                            else matrixCursor.addRow(get_CursorRow(cursor));
//
//                            position = matrixCursor.getCount();
//                            Log.d("Values", position + "p"+offset +"o" + "s"+skips);
//
//                            if (limit > 0)
//                                if (position == limit)
//                                    cancel(false);
////                            matrixCursor.addRow(get_CursorRow(cursor));
//                        }



                        if (word.matches(exp)) {
                            Log.d("Values", position + "p"+ offset + "o" +  skips + "s");
                            if (unfiltered)
                                if ( offset > skips) {
                                    skips++;
                                    continue;
                                }

                            position = matrixCursor.getCount();
                            Log.d("Values", position + "p"+ offset + "o" +  skips + "s");
                            if (limit == 0 || !unfiltered || limit > position)
//                                if (limit == 0 || (limit > position && unfiltered))
                                matrixCursor.addRow(get_CursorRow(precursor));
                            else
                                cancel(false);
                        }

//                        if (LexData.getMaxList() > 0 && unfiltered)
//                            if (matrixCursor.getCount() == LexData.getMaxList())
//                                cancel(false);





//                        if (word.matches(exp)) {
//                            matrixCursor.addRow(get_CursorRow(precursor));
//                        }
//                        if (LexData.getMaxList() > 0 && unfiltered)
//                            if (matrixCursor.getCount() == LexData.getMaxList())
//                                cancel(false);
                        if (isCancelled()) {
                            precursor.close();
                            //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                            cursor = matrixCursor;
                            break;
                        }
                    }
                } catch (Exception e) {

                }
                precursor.close();
                return matrixCursor;
            }

            protected Cursor getThreadCursor()
            {
                return matrixCursor;
            }

            @Override
            protected void onPostExecute(Cursor result) //called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                cursor = matrixCursor;
//                databaseAccess.close();
                displayResults();
            }

            @Override
            protected void onCancelled() //called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                dialog.cancel();
                cursor = matrixCursor;
//                databaseAccess.close();
                displayPartialResults();
            }
        };
        task.execute();
        return cursor;
    }
    public Cursor getparallelThread(final Context context, final String termite, final String filters, final String ordering){
        @SuppressLint("StaticFieldLeak")
        final AsyncTask<Void, Void, Cursor> task = new AsyncTask<Void, Void, Cursor>() {
            protected MatrixCursor matrixCursor;
            protected MatrixCursor matrixCursor2;
            protected ProgressDialog dialog;
            MergeCursor mergeCursor;

            @Override
            protected void onPreExecute()
            {
                databaseAccess.open();
                Log.i("Pattern", ":" + termite);
                matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                        "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
                matrixCursor2 = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                        "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

                this.dialog = Utils.themeDialog(context);
/*                switch (themeName) {
                    case "Dark Theme":
                        this.dialog = new ProgressDialog(context,R.style.darkProgressBar);
                        break;
                    case "Light Theme":
                    default:
                        this.dialog = new ProgressDialog(context);
                        break;
                }

 */

                this.dialog.setMessage("Please Wait!\r\nThis search may take a minute or two...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //dialog.dismiss();
                    }
                });

                this.dialog.setCancelable(false);
                this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        // cancel AsyncTask
                        cancel(false);
                    }
                });
                this.dialog.setCanceledOnTouchOutside(false);
                this.dialog.show();
            }

            @Override
            protected Cursor doInBackground(Void... params)
            {
                String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                String exp;
                String frontfilter = "";
                String backfilter = "";
                String sql = "";
                String frontpattern = databaseAccess.get_frontparallel(termite);
                String backpattern = databaseAccess.get_backparallel(termite);

                // FRONT PARALLEL
                if (frontpattern != "") {
                    exp = Utils.buildPattern(frontpattern);
                    Log.i("Front:", ":" + exp);
                    try {
                        Pattern.compile(exp);
                    } catch (PatternSyntaxException e) {
                        return null;

                    } finally {
                    }

                    if (Character.isUpperCase(exp.charAt(1)))
                        frontfilter = " AND Word LIKE '" + exp.charAt(1) + "%' ";
                    if (Character.isUpperCase(exp.charAt(exp.length() - 2)))
                        backfilter = " AND Word LIKE '%" + exp.charAt(exp.length() - 2) + "' ";


                    sql = "SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                            "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                            "FROM     `" + LexData.getLexName() + "` \n" +
                            "WHERE (" + lenFilter +
                            frontfilter + backfilter +
                            filters +
                            " ) " + ordering;
                    databaseAccess.open();
                    Cursor precursor = databaseAccess.rawQuery(sql);

                    try {
                        while (precursor.moveToNext()) {
                            String word = precursor.getString(1);
                            if (word.matches(exp)) {
                                matrixCursor.addRow(get_CursorRow(precursor));
                                // TODO for arrows use altered get_CursorRow ..
                            }
                            if (LexData.getMaxList() > 0 && unfiltered)
                                if (matrixCursor.getCount() == LexData.getMaxList())
                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();

                                Cursor[] cursors = {
                                        matrixCursor,
                                        matrixCursor2
                                };
                                mergeCursor = new MergeCursor(cursors);
                                cursor = mergeCursor;

                                break;
                            }
                        }
                    } catch (Exception e) {

                    }
                    precursor.close();
                }


                // BACK PARALLEL
                if (backpattern != "") {
                    exp = Utils.buildPattern(backpattern);
                    Log.i("Back:", exp);
                    try {
                        Pattern.compile(exp);
                    } catch (PatternSyntaxException e) {

                    } finally {

                    }

                    if (Character.isUpperCase(exp.charAt(1)))
                        frontfilter = " AND Word LIKE '" + exp.charAt(1) + "%' ";
                    if (Character.isUpperCase(exp.charAt(exp.length() - 2)))
                        backfilter = " AND Word LIKE '%" + exp.charAt(exp.length() - 2) + "' ";


                    sql = "SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                            "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                            "FROM     `" + LexData.getLexName() + "` \n" +
                            "WHERE (" + lenFilter +
                            frontfilter + backfilter +
                            filters +
                            " ) " + ordering;
                    databaseAccess.open();
                    Cursor postcursor = databaseAccess.rawQuery(sql);

                    try {
                        while (postcursor.moveToNext()) {
                            String word = postcursor.getString(1);
                            if (word.matches(exp)) {
                                matrixCursor2.addRow(get_CursorRow(postcursor));
                            }
                            if (isCancelled()) {
                                postcursor.close();

                                Cursor[] cursors = {
                                        matrixCursor,
                                        matrixCursor2
                                };
                                MergeCursor mergeCursor = new MergeCursor(cursors);
                                cursor = mergeCursor;

                                break;
                            }
                        }
                    } catch (Exception e) {

                    }
                    postcursor.close();
                }

                Cursor[] cursors = {
                        matrixCursor,
                        matrixCursor2
                };
                mergeCursor = new MergeCursor(cursors);
                cursor = mergeCursor;
                return mergeCursor;
            }

            protected Cursor getThreadCursor()
            {
                return matrixCursor;
            }

            @Override
            protected void onPostExecute(Cursor result) //called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                cursor = mergeCursor;
                displayResults();
//                databaseAccess.close();
            }

            @Override
            protected void onCancelled() //called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                dialog.cancel();
                cursor = matrixCursor;
//                databaseAccess.close();
                displayPartialResults();
            }
        };
        task.execute();
        return cursor;
    }


    // Search Utilities
    String beginning = "";
    String ending = "";
    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }
    public void displayResults() {
        lvHeader = findViewById(R.id.imcheader);

        if (etFilter.getText().length() > 0)
            executeFilter();

        if (stems.getVisibility() == VISIBLE || categories.getVisibility() == VISIBLE) {
            cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.sclistitem, cursor, stemfrom, stemto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            lvHeader.setVisibility(GONE);
//            mcView = inflater.inflate(R.layout.sclistitem, null);
        }
        else{
            if (LexData.getShowStats()) {
                if (LexData.getShowHooks()) {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.mcstats, cursor, statfrom, statto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                else {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.mcstatsonly, cursor, statonlyfrom, statonlyto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                lvHeader.setVisibility(VISIBLE);
            } else {
                if (LexData.getShowHooks()) {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.mclistitem, cursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                else {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.plainlistitem, cursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                lvHeader.setVisibility(GONE);
            }
        }

        ListView lv = findViewById(R.id.mcresults);
        lv.setAdapter(cursorAdapter);

        long lEndTime = System.nanoTime();
        double output = (lEndTime - lStartTime) / 1000000;

        status.setText("Found " + Integer.toString(cursor.getCount()) + " words from " + LexData.getLexName());
        status.append("   " + String.format("%.2f seconds",output/1000));
        lastStatus = status.getText().toString();

//        hideKeyboard(this);
        hidekeyboard();
    }
    public void displayPartialResults() {
        //ConstraintLayout
        lvHeader = findViewById(R.id.imcheader);

        if (etFilter.getText().length() > 0)
            executeFilter();

        if (stems.getVisibility() == VISIBLE || categories.getVisibility() == VISIBLE) {
            cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.sclistitem, cursor, stemfrom, stemto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            lvHeader.setVisibility(GONE);
        }
        else{
            if (LexData.getShowStats()) {
                if (LexData.getShowHooks()) {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.mcstats, cursor, statfrom, statto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                else {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.mcstatsonly, cursor, statonlyfrom, statonlyto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                lvHeader.setVisibility(VISIBLE);
            } else {
                if (LexData.getShowHooks()) {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.mclistitem, cursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                else {
                    cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.plainlistitem, cursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                lvHeader.setVisibility(GONE);
            }
        }

        ListView lv = findViewById(R.id.mcresults);
        lv.setAdapter(cursorAdapter);

        long lEndTime = System.nanoTime();
        double output = (lEndTime - lStartTime) / 1000000;

        status.setText("Found " + Integer.toString(cursor.getCount()) + " words (partial) from " + LexData.getLexName());
        status.append("   " + String.format("%.2f seconds",output/1000));

//        hideKeyboard(this);
        hidekeyboard();
    }
    public String makefilters(int searchType) {
        List<String> filters = new ArrayList<String>();
        // TODO handle maximum

        if (searchType == 2)
            if (etTerm.getText().length() > 0) { // hookwords
                minimum.setSelection(etTerm.getText().length());
                maximum.setSelection(etTerm.getText().length());
            }

//        Spinner minimum = findViewById(R.id.MinLength);
        int min = minimum.getSelectedItemPosition() + 1;

//        Spinner maximum = findViewById(R.id.MaxLength);
        int max = maximum.getSelectedItemPosition() + 1;

        // String term = etTerm.getText().toString();
        String beginning = begins.getSelectedItem().toString();
        String ending = ends.getSelectedItem().toString();


        if (min == max && min > 1)
            filters.add(" Length(Word) = " + String.valueOf(min));
        else {
            String e = " Length(Word) >= " + String.valueOf(min) + " " +
                    " AND Length(Word) <= " + String.valueOf(max) + " ";
            if (searchType != 1 && searchType != 3)
                if (minimum.getSelectedItemPosition() != 0)
                    filters.add(e);

            if (searchType == 3)
                if (minimum.getSelectedItemPosition() != 0)
                    filters.add(e);
                else
                    filters.add(" Length(Word) >= " + Utils.analyzeMin(etTerm.getText().toString()) +
                            " AND Length(Word) <= " + Utils.analyzeMax(etTerm.getText().toString()));
        }

        if (searchType != 8)
            if (begins.getSelectedItemPosition() != 0)
                filters.add(" Word LIKE '" + beginning + "%' ");
        if (searchType != 9)
            if (ends.getSelectedItemPosition() != 0)
                filters.add(" Word LIKE '%" + ending + "' ");

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
        Log.i("Filter", SQLfilter);
        return SQLfilter;
    }
    public String adjustedfilters(int searchType) { // DON'T extends length to include the -fix
        List<String> filters = new ArrayList<String>();
        // TODO handle maximum

        int min = minimum.getSelectedItemPosition() + 1;
        int max = maximum.getSelectedItemPosition() + 1;

        if (minimum.getSelectedItemPosition() != 0)
            if (min == max && min > 1)
                filters.add(" Length(Word) = " + String.valueOf(min));
//                filters.add(" Length(Word) = " + String.valueOf(min  + etTerm.getText().length()));
            else {
                filters.add(" Length(Word) >= " + String.valueOf(min) + " " +
                        " AND Length(Word) <= " + String.valueOf(max));
//                filters.add(" Length(Word) >= " + String.valueOf(min + etTerm.getText().length()) + " " +
//                        " AND Length(Word) <= " + String.valueOf(max + etTerm.getText().length()));
            }

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
//        Log.i("Filter", SQLfilter);
        return SQLfilter;
    }
    public String orgfilters() {
        List<String> filters = new ArrayList<String>();
        // TODO handle maximum

        int min = minimum.getSelectedItemPosition() + 1;
        int max = maximum.getSelectedItemPosition() + 1;

        if (min == max && min > 1)
            filters.add(" Length(Word) = " + String.valueOf(min));
        else {
            String e = " Length(Word) >= " + String.valueOf(min) + " " +
                    " AND Length(Word) <= " + String.valueOf(max) + " ";
            if (minimum.getSelectedItemPosition() != 0)
                filters.add(e);
        }

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
        return SQLfilter;
    }
    public String altfilters(int lenVariance) {
        List<String> filters = new ArrayList<String>();
        // TODO handle maximum

        int min = (minimum.getSelectedItemPosition() + 1);
        int max = (maximum.getSelectedItemPosition() + 1);

        if (min == 2 && max == 2)
            lenVariance = 0;
        if (min == max && min > 1)
            filters.add(" Length(Word) = " + String.valueOf(min  + lenVariance));
        else {
            String e = " Length(Word) >= " + String.valueOf(min + lenVariance) + " " +
                    " AND Length(Word) <= " + String.valueOf(max + lenVariance) + " ";
            if (minimum.getSelectedItemPosition() != 0)
                filters.add(e);
        }

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
        return SQLfilter;
    }
    public String makeDesc() {
        StringBuilder sb = new StringBuilder();
        int min = minimum.getSelectedItemPosition() + 1;

//        Spinner maximum = findViewById(R.id.MaxLength);
        int max = maximum.getSelectedItemPosition() + 1;

        // String term = etTerm.getText().toString();
        String beginning = begins.getSelectedItem().toString();
        String ending = ends.getSelectedItem().toString();
        if (minimum.getSelectedItemPosition() > 0 ||
        maximum.getSelectedItemPosition() > 0)
            sb.append(" Len:" + min + "-" + max + " ");
        if (begins.getSelectedItemPosition() > 0)
            sb.append("Beg:" + beginning + " ");
        if (ends.getSelectedItemPosition() > 0)
            sb.append("End:" + ending + " ");
        return sb.toString();
    }
    public String makeParallelFilters(int searchType) {
        List<String> filters = new ArrayList<String>();

        Spinner sp = findViewById(R.id.MinLength);
        int minimum = sp.getSelectedItemPosition() + 1;

        Spinner max = findViewById(R.id.MaxLength);
        int maximum = max.getSelectedItemPosition() + 1;

        // String term = etTerm.getText().toString();
        String beginning = begins.getSelectedItem().toString();
        String ending = ends.getSelectedItem().toString();

        if (sp.getSelectedItemPosition() != 0)
            filters.add(" Length(Word) >= " + String.valueOf(minimum) + " " +
                    " AND Length(Word) <= " + String.valueOf(maximum) + " ");
        else
            filters.add(" (Length(Word) >= " + (etTerm.length() - 1) +
                    " AND Length(Word) <= " + (etTerm.length() + 1) + ") ");
        if (begins.getSelectedItemPosition() != 0)
            filters.add(" Word LIKE '" + beginning + "%' ");
        if (ends.getSelectedItemPosition() != 0)
            filters.add(" Word LIKE '%" + ending + "' ");

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
        return SQLfilter;
    }
    protected String[] get_CursorRow (Cursor cursor) {
        String [] columnValues = new String[11];
        for (int c = 0; c < 11; c++)
            columnValues[c] = cursor.getString(c);
        return columnValues;
    }
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
    protected String getSortOrder() {
        //"ORDER BY Length(Word), Word"
        int firstsort = sortby.getSelectedItemPosition();
        int secondsort = thenby.getSelectedItemPosition();
        StringBuilder builder = new StringBuilder();


        if (firstsort == 16)
            return " ";

        builder.append("ORDER BY ");
        switch (firstsort) {
            case 0:
                sortby.setSelection(1);
            case 1:
                builder.append("Length(Word)");
                break;
            case 2:
                builder.append("Length(Word) DESC");
                break;
            case 3:
                builder.append("Word");
                break;
            case 4:
                builder.append("Word DESC");
                break;
            case 5:
                builder.append("Score");
                break;
            case 6:
                builder.append("Score DESC");
                break;
            case 7:
                builder.append("ProbFactor");
                break;
            case 8:
                builder.append("ProbFactor DESC");
                break;
            case 9:
                builder.append("OPlayFactor");
                break;
            case 10:
                builder.append("OPlayFactor DESC");
                break;
            case 11:
                builder.append("Anagrams");
                break;
            case 12:
                builder.append("Anagrams DESC");
                break;
            case 13:
                if (databaseAccess.columnExists(LexData.getLexName(), "Alphagram"))
                    builder.append("Alphagram");
                else
                    builder.append("Word ");
                break;
            case 14:
                if (databaseAccess.columnExists(LexData.getLexName(), "Alphagram"))
                    builder.append("Alphagram DESC");
                else
                    builder.append("Word DESC ");
                break;
            case 15:
                builder.append("Random()");
                break;
            case 16: // unsorted
                break;
        }
        builder.append(", ");

//        if (firstsort == 15)
//            secondsort = 15);

        switch (secondsort) {
            case 0:
                thenby.setSelection(1);
            case 1:
                builder.append("Word ");
                break;
            case 2:
                builder.append("Word DESC ");
                break;
            case 3:
                builder.append("Score");
                break;
            case 4:
                builder.append("Score DESC");
                break;
            case 5:
                builder.append("ProbFactor ");
                break;
            case 6:
                builder.append("ProbFactor DESC ");
                break;
            case 7:
                builder.append("OPlayFactor ");
                break;
            case 8:
                builder.append("OPlayFactor DESC ");
                break;
            case 9:
                builder.append("Anagrams ");
                break;
            case 10:
                builder.append("Anagrams DESC ");
                break;
            case 11:
                builder.append("Length(Word) ");
                break;
            case 12:
                builder.append("Length(Word) DESC ");
                break;
            case 13:
                if (databaseAccess.columnExists(LexData.getLexName(), "Alphagram"))
                    builder.append("Alphagram ");
                else
                    builder.append("Word ");
                break;
            case 14:
                if (databaseAccess.columnExists(LexData.getLexName(), "Alphagram"))
                builder.append("Alphagram DESC ");
                else
                    builder.append("Word DESC ");
                break;
            case 15:
                builder.append("Random()");
                break;
        }
        return builder.toString();
    }
    public void selectFile() {
//        Boolean saf = shared.getBoolean("saf",false);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || saf) {
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
            FileDialog fileDialog = new FileDialog(SearchActivity.this, mPath, "txt");
            // only supports one file extension

            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    String full = file.getAbsolutePath();
                    importfile.setPrivateImeOptions(full);
                    importfile.setText(full);
                }
            });
            fileDialog.showDialog();
        }
    }
    private void generateSpecStats() {
        StringBuilder specs = new StringBuilder();
        specs.append("Specs::" );
        if (begins.getSelectedItemPosition() != 0)
            specs.append(" Begins " + begins.getSelectedItem().toString());
        if (ends.getSelectedItemPosition() != 0)
            specs.append(" Ends " + ends.getSelectedItem().toString());
        if (sortby.getSelectedItemPosition() != 0)
            specs.append(" Sort By " + sortby.getSelectedItem().toString());
        if (thenby.getSelectedItemPosition() != 0)
            specs.append(" Then By " + thenby.getSelectedItem().toString());
        if (!etFilter.getText().toString().equals(""))
            specs.append(" Rack " + etFilter.getText().toString());
        if (!etLimit.getText().toString().equals(""))
            specs.append(" Limit " + etLimit.getText().toString());
        if (!etOffset.getText().toString().equals(""))
            specs.append(" Beginning " + etOffset.getText().toString());

        specStatus.setText(specs);

    }


    // Activity Utilities
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
    @Override public void onBackPressed(){
        if( isCustomKeyboardVisible() ) {
            hideCustomKeyboard();
            return;
        }

        if (isTaskRoot())
            Utils.exitAlert(this);
        else
            super.onBackPressed();
    }
    public void registerEditText(int resid) {
        // Find the EditText 'resid'
        EditText edittext= (EditText)findViewById(resid);
        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
//                if( hasFocus ) showCustomKeyboard(v); else hideCustomKeyboard();
                if( hasFocus ) showkeyboard(); else hidekeyboard();
            }
        });
        edittext.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
//                showCustomKeyboard(v);
                showkeyboard();
            }
        });

        // Disable standard keyboard hard way
        edittext.setOnTouchListener(new View.OnTouchListener() {

            @Override public boolean onTouch(View v, MotionEvent event) {
                EditText edittext = (EditText) v;


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    edittext.setShowSoftInputOnFocus(false); // disable android keyboard
                    return false;
                } else {
                    int inType = edittext.getInputType();       // Backup the input type
                    edittext.setInputType(InputType.TYPE_NULL); // Disable standard keyboard
                    edittext.onTouchEvent(event);               // Call native handler
                    edittext.setInputType(inType);              // Restore input type
                    return true; // Consume touch event
                }
            }
        });

        // Disable spell check (hex strings look like words to Android)
        edittext.setInputType( edittext.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS );
    }
    ColumnIndexCache cache = new ColumnIndexCache();
    public class ColumnIndexCache {
        protected ArrayMap<String, Integer> mMap = new ArrayMap<>();
        public int getColumnIndex(Cursor cursor, String columnName) {
            if (!mMap.containsKey(columnName))
                mMap.put(columnName, cursor.getColumnIndex(columnName));
            return mMap.get(columnName);
        }
        public void clear() {
            mMap.clear();
        }
    }


    // KEYBOARD SUPPORT
    public final static int CodeDelete   = -5; // Keyboard.KEYCODE_DELETE
    public final static int CodeSearch = 55007;
    public final static int CodeNewSearch = 55008;
    protected void useCustomKeyboard() {
        // Create the Keyboard

        // Do not show the preview balloons
        mKeyboardView.setPreviewEnabled(false);
        // Install the key handler
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);


        // Hide the standard keyboard initially
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        registerEditText(R.id.etEntry);
        registerEditText(R.id.etFilter);
        hideCustomKeyboard();
    }
    public void hidekeyboard() {
        if (LexData.getCustomkeyboard())
            hideCustomKeyboard();
        else {
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
        if (etLimit.getVisibility() != GONE) {
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

    }
    public void showkeyboard() {
        View view = this.getCurrentFocus();
        if (LexData.getCustomkeyboard())
            showCustomKeyboard(view);
        else {
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
        }
    }
    public void hideCustomKeyboard() {
        if (mKeyboardView != null) {
            mKeyboardView.setVisibility(View.GONE);
            mKeyboardView.setEnabled(false);
        }
    }
    public void showCustomKeyboard( View v ) {

        // todo trap error here



        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if( v!=null ) ((InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }


    // Slides and Quizzes
    public Bundle makeSlideBundle() {
        Bundle bundle =new Bundle();


//        if (etTerm.getVisibility() == VISIBLE)
//            bundle.putString("term", etTerm.getText().toString());
//        else
//            bundle.putString("term", " ");

//        bundle.putString("search", stype.getSelectedItem().toString());

        if (collapse.getVisibility() == VISIBLE) {// indicates SearchActivity
//            bundle.putString("search", stype.getSelectedItem().toString());
            bundle.putString("desc", stype.getSelectedItem().toString() + " " + searchParameters.toString());
        }
        else {
//            bundle.putString("search", " ");
            bundle.putString("desc", message);
        }
//        bundle.putString("desc", searchParameters.toString());
        return bundle;
    }
    public void startSlides() {
        if (listView.getCount() < 1) {
            Toast.makeText(this, "Complete a search before showing slides", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] words = new String[cursor.getCount()];
        cursor.moveToFirst();
        int counter = 0;
        int column = cursor.getColumnIndex("Word");
        words[counter] = cursor.getString(column);
        while(cursor.moveToNext()){
            counter++;
            words[counter] = cursor.getString(column);
        }
/*
        Bundle bundle =new Bundle();

        bundle.putStringArray("Words",words);

        if (etTerm.getVisibility() == VISIBLE)
            bundle.putString("term", etTerm.getText().toString());
        else
            bundle.putString("term", " ");

        // this time use the form search type
        bundle.putString("search", stype.getSelectedItem().toString());
        // Create description for Slide/Quiz
        // based on search option
        bundle.putString("desc", searchParameters.toString());
*/
        Bundle bundle = makeSlideBundle();
        bundle.putStringArray("Words",words);

        Intent intentBundle = new Intent(SearchActivity.this, ListSlidesActivity.class);
        intentBundle.putExtras(bundle);
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);
    }
    public void startReview(String rtype) {
        if (listView.getCount() < 1) {
            Toast.makeText(this, "Complete a search before showing slides", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] words = new String[cursor.getCount()];

        // extract words from cursor
        int column = cursor.getColumnIndex("Word");

        cursor.moveToFirst();
        int counter = 0;
        words[counter] = cursor.getString(column);
        while(cursor.moveToNext()){
            counter++;
            words[counter] = cursor.getString(column);
        }

        Bundle bundle =new Bundle();

        switch (rtype) {
            case "Anagrams":
            case "Blank Anagrams":
                String[] anagrams = databaseAccess.getAnagrams(words);
                bundle.putStringArray("Words", anagrams);
                break;
            case "Hook Words":
                counter = 0;
                cursor.moveToFirst();
                List<String> wordlist = new ArrayList<>();
                int fh = cursor.getColumnIndex("FrontHooks");
                int bh = cursor.getColumnIndex("BackHooks");
//                if (cursor.getCount() > 10000) {
//                    Toast.makeText(this, "Over the limit!\r\nPlease limit slides to 5000 words...", Toast.LENGTH_LONG).show();
//                    return;
//                }

                if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0) {
                    wordlist.add(cursor.getString(column));
                    counter++;
                }

                cursor.moveToFirst();
                if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0) {
                    wordlist.add(cursor.getString(column));
                    counter++;
                }

                while (cursor.moveToNext()) {
                    if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0) {
                        wordlist.add(cursor.getString(column));
                        counter++;
                    }
                }
                String[] hookwords = wordlist.toArray(new String[0]);
                bundle.putStringArray("Words", hookwords);
                break;
            default:
                bundle.putStringArray("Words",words);
                break;
        }

//        if (etTerm.getVisibility() == VISIBLE)
//            bundle.putString("term", etTerm.getText().toString());
//        else
//            bundle.putString("term", " ");

        bundle.putString("search", rtype);

        if (collapse.getVisibility() == VISIBLE) {// indicates SearchActivity
//            bundle.putString("search", stype.getSelectedItem().toString());
            bundle.putString("desc", stype.getSelectedItem().toString() + " " + searchParameters.toString());
        }
        else {
//            bundle.putString("search", " ");
            bundle.putString("desc", message);
        }

//        bundle.putString("desc", stype.getSelectedItem().toString() + " " + searchParameters.toString());

        Intent intentBundle = new Intent(SearchActivity.this, ReviewActivity.class);
        intentBundle.putExtras(bundle);
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);
    }
    public void startAnagramQuiz() {
        if (listView.getCount() < 1) {
            Toast.makeText(this, "Complete a search before beginning a quiz", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] words = new String[cursor.getCount()];
//        if (cursor.getCount() > 10000) {
//            Toast.makeText(this, "Over the limit!\r\nPlease limit slides to 5000 words...", Toast.LENGTH_LONG).show();
//            return;
//        }

        // extract words from cursor
        int counter = 0;
        int column = cursor.getColumnIndex("Word");

        cursor.moveToFirst();
        words[counter] = cursor.getString(column);
        while(cursor.moveToNext()){
            counter++;
            words[counter] = cursor.getString(column);
        }

        Bundle bundle = makeSlideBundle();
/*        // extract anagrams from words
        Bundle bundle =new Bundle();

        if (etTerm.getVisibility() == VISIBLE)
            bundle.putString("term", etTerm.getText().toString());
        else
            bundle.putString("term", " ");

        bundle.putString("search", stype.getSelectedItem().toString());
        bundle.putString("desc", searchParameters.toString());
 */
        String[] anagrams = databaseAccess.getAnagrams(words);
        bundle.putStringArray("Words",anagrams);

        Intent intentBundle = new Intent(SearchActivity.this, AnagramQuizActivity.class);
        intentBundle.putExtras(bundle);
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);
    }
    public void startRecallQuiz() {
        if (listView.getCount() < 1) {
            listView.setVisibility(GONE);
            search.performClick();
            if (listView.getCount() < 1) {
                Toast.makeText(this, "No results from current search parameters", Toast.LENGTH_LONG).show();
                listView.setVisibility(VISIBLE);
                return;
            }
        }

/*        if (listView.getCount() < 1) {
            Toast.makeText(this, "Complete a search before beginning a quiz", Toast.LENGTH_SHORT).show();
            return;
        }
 */

//        if (cursor.getCount() > 10000) {
//            Toast.makeText(this, "Over the limit!\r\nPlease limit slides to 5000 words...", Toast.LENGTH_LONG).show();
//            return;
//        }
        String[] words = new String[cursor.getCount()];

        // extract words from cursor
        int counter = 0;
        int column = cursor.getColumnIndex("Word");

        cursor.moveToFirst();
        words[counter] = cursor.getString(column);
        while(cursor.moveToNext()){
            counter++;
            words[counter] = cursor.getString(column);
        }

        Bundle bundle = makeSlideBundle();
/*        Bundle bundle =new Bundle();
        if (etTerm.getVisibility() == VISIBLE)
            bundle.putString("term", etTerm.getText().toString());
        else
            bundle.putString("term", " ");

        // this time use the form search type
        bundle.putString("search", stype.getSelectedItem().toString());
        bundle.putString("desc",  searchParameters.toString());
//        bundle.putString("desc", stype.getSelectedItem().toString() + " " + searchParameters.toString());

 */
        bundle.putStringArray("Words",words);

        Intent intentBundle = new Intent(SearchActivity.this, RecallQuizActivity.class);
        intentBundle.putExtras(bundle);
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);
    }
    public void startQuiz(String type) {
        if (listView.getCount() < 1) {
            Toast.makeText(this, "Complete a search before beginning a quiz", Toast.LENGTH_SHORT).show();
            return;
        }
//        if (cursor.getCount() > 10000) {
//            Toast.makeText(this, "Over the limit!\r\nPlease limit slides to 5000 words...", Toast.LENGTH_LONG).show();
//            return;
//        }
        String[] words = new String[cursor.getCount()];
        Intent intentBundle = new Intent(SearchActivity.this, QuizActivity.class);

        // extract words from cursor
        int column = cursor.getColumnIndex("Word");

        // create list of words
        int counter = 0;
        cursor.moveToFirst();
        words[counter] = cursor.getString(column);
        while(cursor.moveToNext()){
            counter++;
            words[counter] = cursor.getString(column);
        }

        Bundle bundle =new Bundle();

        switch (type) {
            case "Blank Anagrams":
                String[] anagrams = databaseAccess.getAnagrams(words);
                bundle.putStringArray("Words", anagrams);
                break;
            case "Hook Words":
                counter = 0;
                cursor.moveToFirst();
                List<String> wordlist = new ArrayList<>();
                int fh = cursor.getColumnIndex("FrontHooks");
                int bh = cursor.getColumnIndex("BackHooks");

                if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0) {
                    wordlist.add(cursor.getString(column));
                    counter++;
                }

                while (cursor.moveToNext()) {
                    if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0) {
                        wordlist.add(cursor.getString(column));
                        counter++;
                    }
                }
                String[] hookwords = wordlist.toArray(new String[0]);
                bundle.putStringArray("Words", hookwords);
                break;
            default:
                bundle.putStringArray("Words",words);
                break;
        }

        // only in SearchActivity
//        if (etTerm.getVisibility() == VISIBLE)
//            bundle.putString("term", etTerm.getText().toString());
//        else
//            bundle.putString("term", " ");

        // this time use the form search type
        bundle.putString("search", type);

        if (collapse.getVisibility() == VISIBLE) // indicates SearchActivity
            bundle.putString("desc", stype.getSelectedItem().toString() + " " + searchParameters.toString());
        else
            bundle.putString("desc", message);


        intentBundle.putExtras(bundle);

        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);
    }
    @Override
    public boolean onMenuItemClick(MenuItem item) {
//        Toast.makeText(this, "Selected Item: " +item.getTitle(), Toast.LENGTH_SHORT).show();
        switch (item.getItemId()) {

            // Review popup
            case R.id.anagram_review:
            case R.id.hooks_review:
            case R.id.stem_review:
                startReview(item.getTitle().toString());
                return true;

            // Quiz  popup
            case R.id.recall:
                startRecallQuiz();
                return true;
            case R.id.anagram_quiz:
                startAnagramQuiz();
                // do your code
                return true;
            case R.id.hooks_quiz:
            case R.id.stem_quiz:
                //   startStemQuiz();
                // do your code
                //   return true;
//            case R.id.build_quiz:
//            case R.id.contains_quiz:
//            case R.id.containsall_quiz:
//            case R.id.containsAny_quiz:
//            case R.id.subwords_quiz:
                startQuiz(item.getTitle().toString());
                // do your code

                return true;

            // Save words to Cardbox popup
/*            case R.id.anagrams:
                cardBox.boxtype = "Anagrams";
                return true;
            case R.id.blankanagrams:
                cardBox.boxtype = "BlankAnagrams";
                return true;
            case R.id.hookwords:
                cardBox.boxtype = "Hooks";
                return true;
            case R.id.list:
                cardBox.boxtype = "Lists";
                return true;
*/

            default:
                return false;
        }
    }
    public void openAltSearch() {
        // Do something in response to button
        Intent intent = new Intent(this, SearchActivity.class);
        //        finishAffinity(); // deletes the stack
        startActivity(intent);
    }


    // Card Boxes
    private SQLiteDatabase cards;
    public boolean createCardFolder() {
        // create folders
        File full = new File(LexData.getCardfile()); // includes database name
        File directory = new File(full.getParent()); // path only

        if (directory.exists())
            return true;

        boolean success = directory.mkdirs();
        if (!success) {
            Log.e("mkdirs", "failed to mkdirs");
            return false;
        }
        return true;
    }
    // DIALOG FOR LIST OPTIONS
    final boolean[] cancelled = {false};
    final int[] quiztypeSelection = new int[1];
    final String[] quiztitle = new String[1];
    private boolean addList(LexData.HList hList) {
        CardDatabase cardDatabase;
        String[] words;
        int counter = 0;
        int column;


        cardBox = new LexData.Cardbox("Hoot", LexData.getLexName(), "Lists");
        LexData.setCardfile(cardBox);

        if (!createCardFolder()) {
            Toast.makeText(this, "Failed to mkdirs", Toast.LENGTH_LONG).show();
            return true;
        }

        cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
        cards = cardDatabase.getWritableDatabase();
        words = new String[cursor.getCount()];

        // extract words from cursor
        column = cursor.getColumnIndex("Word");

        cursor.moveToFirst();
        words[counter] = cursor.getString(column).toUpperCase();
        while (cursor.moveToNext()) {
            counter++;
            words[counter] = cursor.getString(column).toUpperCase();

// order mixed
//            Log.e("S:addList", words[counter]);
        }

        if (hList.quiz_type != 2) {
            // add words to list

            List<String> listOfWords = new ArrayList<>();
            for (int id = 0; id < words.length; id++) {
                listOfWords.add(words[id]);
            }
            counter = CardDatabase.addList(cards, hList, listOfWords);

//            counter = CardDatabase.addList(cards, hList, Arrays.asList(words));
            cards.close();
            Toast.makeText(this, "Added " + counter + " words to List", Toast.LENGTH_LONG).show();
            return true;
        }
        else {
            // Create word list from cursor
            column = cursor.getColumnIndex("Word");
            int fh = cursor.getColumnIndex("FrontHooks");
            int bh = cursor.getColumnIndex("BackHooks");

            List<String> ww = new ArrayList<>();

            cursor.moveToFirst();
            if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0)
                ww.add(cursor.getString(column).toUpperCase());

            while (cursor.moveToNext()) {
                if (cursor.getString(fh).trim().length() + cursor.getString(bh).trim().length() > 0)
                    ww.add(cursor.getString(column).toUpperCase());
            }

            counter = CardDatabase.addList(cards, hList, Arrays.asList(words));
            cards.close();
            Toast.makeText(this, "Added " + counter + "  words to Hooks", Toast.LENGTH_LONG).show();
            return true;
        }

    }


    // Listeners
    protected KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = SearchActivity.this.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();

            // Handle key

            if( primaryCode==CodeSearch ) {
                if( editable!=null )
                    executeSearch();
            } else if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);
            } else if( primaryCode==CodeNewSearch ) {
                openAltSearch();
//            } else if (primaryCode == CodeEnter) {
//                editable.append('\n');
            } else {// Insert character
                editable.insert(start, Character.toString((char) primaryCode));
            }
        }
        @Override public void onPress(int arg0) {
        }

        @Override public void onRelease(int primaryCode) {
        }

        @Override public void onText(CharSequence text) {
        }

        @Override public void swipeDown() {
        }

        @Override public void swipeLeft() {
        }

        @Override public void swipeRight() {
        }

        @Override public void swipeUp() {
        }
    };
    protected final TextView.OnEditorActionListener entryAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                executeSearch();//match this behavior to your 'Send' (or Confirm) button
                return true;
            }
            return false;
        }
    };
    protected final TextView.OnEditorActionListener filterAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL
                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                executeSearch();//match this behavior to your 'Send' (or Confirm) button
                return true;
            }
            return false;
        }
    };
    protected final TextWatcher entryWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        protected boolean mWasEdited = false;
        @Override
        public void afterTextChanged(Editable s) {
            if (mWasEdited){
                mWasEdited = false;
                return;
            }
            mWasEdited = true;
            String enteredValue  = s.toString();

            int caret = etTerm.getSelectionStart();
            if (stype.getSelectedItemPosition() != 3) { // not pattern
                enteredValue = enteredValue.toUpperCase();
                //^+[]&lt;&gt;(|){}\\~-
                String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}cv0123456789.,^+~-]", "");
//                String newValue = enteredValue.replaceAll("[\\[\\]<>cv0123456789.,^+~-]", "");

                if (stype.getSelectedItemPosition() != 0 && // not anagram
                        stype.getSelectedItemPosition() != 3) { // and not pattern
                    newValue = newValue.replaceAll("[*@]", "");
                }
                if (Arrays.asList(2,7,8,9).contains(stype.getSelectedItemPosition())) {// hooks, contains any, begins, ends
                    newValue = newValue.replaceAll("[?]", "");
                }
                etTerm.setText(newValue);
                etTerm.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
                return;
            }
            else {
                StringBuilder modifiedValue = new StringBuilder(enteredValue);
                for (int i = 0; i < modifiedValue.length(); i++) {
                    if (Character.isLowerCase(modifiedValue.charAt(i)))
                        if (!(modifiedValue.charAt(i) == 'c' || modifiedValue.charAt(i) == 'v'))

                            modifiedValue.setCharAt(i, Character.toUpperCase(modifiedValue.charAt(i)));
                }
                etTerm.setText(modifiedValue);
                etTerm.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid
            }
        }
    };
    protected final TextWatcher filterWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        protected boolean mWasEdited = false;
        @Override
        public void afterTextChanged(Editable s) {
            if (mWasEdited){
                mWasEdited = false;
                return;
            }
            mWasEdited = true;
            String enteredValue  = s.toString();

            int caret = etFilter.getSelectionStart();

            enteredValue = enteredValue.toUpperCase();

            String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}a-z*0123456789.,^+~-]", "");

            etFilter.setText(newValue);
            etFilter.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
            return;
        }
    };
    protected OnClickListener doSearch = new OnClickListener() {
        public void onClick(View v) {
            executeSearch();
        }
    };
    protected OnClickListener clearBegin = new OnClickListener() {
        public void onClick(View v) {
            begins.setSelection(0);
        }
    };
    protected OnClickListener clearEnd = new OnClickListener() {
        public void onClick(View v) {
            ends.setSelection(0);
        }
    };
    protected OnClickListener enterBlank = new OnClickListener() {
        public void onClick(View v) {
            int start = Math.max(etTerm.getSelectionStart(), 0);
            int end = Math.max(etTerm.getSelectionEnd(), 0);
            etTerm.getText().replace(Math.min(start, end), Math.max(start, end),
                    "?", 0, 1);    // "?".length()
        }
    };
    protected OnClickListener clearTerm = new OnClickListener() {
        public void onClick(View v) {
            EditText editText = findViewById(R.id.etEntry);
            editText.setText("");
            editText.requestFocus();
            showkeyboard();
        }
    };
    protected OnClickListener newFile = new OnClickListener() {
        public void onClick(View v) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                selectFile();
            }
        }
    };
    protected OnClickListener EmptyRack = new OnClickListener() {
        public void onClick(View v) {
            EditText editText = findViewById(R.id.etFilter);
            editText.setText("");
        }
    };
    protected OnClickListener doSlides = new OnClickListener() {
        @Override
        public void onClick(View v) {
            startSlides();
        }
    };
    protected OnClickListener doQuiz = new OnClickListener() {
        @Override
        public void onClick(View v) {
            PopupMenu popup = new PopupMenu(SearchActivity.this, v);
            popup.setOnMenuItemClickListener(SearchActivity.this);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_quiz, popup.getMenu());
            popup.show();
        }
    };
    protected OnClickListener doQuizReview = new OnClickListener() {
        @Override
        public void onClick(View v) {
            PopupMenu popup = new PopupMenu(SearchActivity.this, v);
            popup.setOnMenuItemClickListener(SearchActivity.this);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_review, popup.getMenu());
            popup.show();
        }
    };
    protected OnClickListener doClear = new OnClickListener() {
        public void onClick(View v) {
            stype.setSelection(0);
            minimum.setSelection(0);
            maximum.setSelection(0);
            etTerm.setText("");
            begins.setSelection(0);
            ends.setSelection(0);
            sortby.setSelection(0);
            thenby.setSelection(0);
            etFilter.setText("");
            etLimit.setText("");
            etOffset.setText("");
            lastStatus = "";

            if (LexData.getLexicon().LexiconNotice.isEmpty())
                status.setText((LexData.getLexName()));
            else
                status.setText(LexData.getLexicon().LexiconNotice);

            specStatus.setText("Other Specifications");
            listView.setAdapter(null);
            showkeyboard();
        }
    };
    protected OnClickListener doNewSearch = new OnClickListener() {
        @Override
        public void onClick(View v) {
            openAltSearch();
        }
    };
    protected OnClickListener doCollapse = new OnClickListener() {
        public void onClick(View v) {
            collapse();
        }
    };

    private void writeList(Uri uri) {


        int counter = 0;
        int column = cursor.getColumnIndex("Word");


        counter++;
        String sep = System.lineSeparator();



        try {

            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());


            fileOutputStream.write((cursor.getString(column).toUpperCase() + sep).getBytes());
            while (cursor.moveToNext()) {
                counter++;
                fileOutputStream.write((cursor.getString(column).toUpperCase() + sep).getBytes());
            }
            fileOutputStream.close();
            Toast.makeText(getBaseContext(), "Saving " + counter + " words to " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();



            fileOutputStream.write(("Overwritten at " + System.currentTimeMillis() +
                    "\n").getBytes());
            // Let the document provider know you're done by closing the stream.
            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }




//        ContentResolver contentResolver;
//        val descriptor = contentResolver.openFileDescriptor(uri, "w")!!.fileDescriptor
//        val fileOut = FileOutputStream(descriptor)


//        writer.append(cursor.getString(column).toUpperCase() + sep);
//        while (cursor.moveToNext()) {
//            counter++;
//            writer.append(cursor.getString(column).toUpperCase() + sep);
//        }
//        writer.close();
//        Toast.makeText(getBaseContext(), "Saving " + counter + "words to " + filename, Toast.LENGTH_LONG).show();
//
//
//
//        fileOut.write(message.toByteArray())
//        fileOut.close()
    }
    private void askPermission() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, "Documents");
        startActivityForResult(intent, REQUEST_FOLDER);
    }
    private int fetchSocondaryColor() {
        TypedValue typedValue = new TypedValue();

        TypedArray a = this.obtainStyledAttributes(typedValue.data, new int[] { R.attr.colorSecondary });
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
    }
    public void showNews(int number) {
// use for feature/tip tracking
// Can store feature version with blind preference
// Get from the SharedPreferences

/*        int homeScore = shared.getInt("homeScore", 5);
        if (homeScore < 8) {
            // show tips
            // update feature version number
            homeScore = 8;
        }

        prefs.putInt("homeScore", homeScore);
        prefs.apply();

        Log.e("Hidden Pref", Integer.toString(homeScore));

*/
        int news = shared.getInt("news",1);
        if (news  < number) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(SearchActivity.this);

            builder.setTitle("Application News");
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.app_news, null);

            BufferedReader reader = null;
            StringBuilder html = new StringBuilder();
            try {
                reader = new BufferedReader(
                        new InputStreamReader(getAssets().open("html/" + "news.html")));

                // do reading, usually loop until end of file reading
                String mLine;
                while ((mLine = reader.readLine()) != null) {
                    html.append(mLine);
                    html.append('\n');
                }
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(),"Error reading file!",Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //log the exception
                    }
                }


                TextView appNews =  dialogView.findViewById(R.id.textNews);
                Spanned result;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    result = Html.fromHtml(html.toString(), Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(html.toString());
                }

                appNews.setText(result);
                appNews.setMovementMethod(new ScrollingMovementMethod());
                appNews.setMovementMethod(LinkMovementMethod.getInstance());
            }

            builder.setView(dialogView);
            builder.setPositiveButton( "Okay", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {


                    dialog.dismiss();
                }
            });
            builder.show();
//            prefs.putInt("news", number);
//            prefs.putInt("news", 4);
        }

    }
    protected String getLimitsOld() {
        int limit, offset;
        StringBuilder builder = new StringBuilder();

        if (etLimit.getText().toString().matches("0"))
            etLimit.setText("");
        if (etOffset.getText().toString().matches("0"))
            etOffset.setText("");

        if (!etLimit.getText().toString().matches("")) {
            limit = Integer.parseInt(etLimit.getText().toString());

//            if (!etOffset.getText().toString().matches(""))
            builder.append(" LIMIT " + limit);

            if (etOffset.getText().toString().matches(""))
                offset = 1;
            else
                offset = Integer.parseInt(etOffset.getText().toString()) - 1;
            builder.append(" OFFSET " + (offset));

        }

        // No Limit
        else {
            if (!etOffset.getText().toString().matches("")) {
                builder.append(" LIMIT -1 ");
                offset = Integer.parseInt(etOffset.getText().toString()) - 1;
                builder.append(" OFFSET " + (offset));

            } else {
                return "";
            }
        }
        return builder.toString();
    }
    public String minmakefilters(int searchType) {
        List<String> filters = new ArrayList<String>();
        // TODO handle maximum

        Spinner sp = findViewById(R.id.MinLength);
        int minimum = sp.getSelectedItemPosition() + 1;
        // String term = etTerm.getText().toString();
        String beginning = begins.getSelectedItem().toString();
        String ending = ends.getSelectedItem().toString();

        if (searchType != 1 && searchType != 3)
            if (sp.getSelectedItemPosition() != 0)
                filters.add(" Length(Word) = " + String.valueOf(minimum) + " ");



        if (searchType == 3)
            if (sp.getSelectedItemPosition() != 0)
                filters.add(" Length(Word) = " + String.valueOf(minimum) + " ");
            else
                filters.add(" Length(Word) >= " + Utils.analyzeMin(etTerm.getText().toString()) +
                        " AND Length(Word) <= " + Utils.analyzeMax(etTerm.getText().toString())   );

        if (searchType != 8)
            if (begins.getSelectedItemPosition() != 0)
                filters.add(" Word LIKE '" + beginning + "%' ");
        if (searchType != 9)
            if (ends.getSelectedItemPosition() != 0)
                filters.add(" Word LIKE '%" + ending + "' ");

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
//        Log.i("Filter", SQLfilter);
        return SQLfilter;
    }
    protected void executeHistory(int SearchID) {
        lStartTime = System.nanoTime();
        status.setText(R.string.searching);

//        hideKeyboard(this);
        hidekeyboard();
        databaseAccess.open();
//        load parameters
//        set etTerm visibilit, hide others
//        set controls from parameters
//        cursor = getCursor(stype.getSelectedItemPosition());

        if (cursor == null) { // ?? when thread running
            return;
        }

        displayResults();
//leaked
//        x//        databaseAccess.close(); // closing database while async was still running caused connection pool closed
    }
    public String GetTextFile(String filespec)  {

        List<String> words;

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            try {
                words = Utils.getWordsFromURI(this, filespec);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
            words = Utils.getWordsFromFile(filespec);

        Log.d("words in file", "Total " + words.size());

        StringBuilder textlist = new StringBuilder();

        textlist.append("'" + words.get(0)+ "'");
        for(int c = 1; c < words.size(); c++) {
            textlist.append(", '" + words.get(c) + "'");
        }

        return textlist.toString();
//        cursor = databaseAccess.getCursor_getWords(textlist.toString(), ordering, limits, filters);
//        Log.d("words in file", "cursor " + cursor.getCount());

//        Create comma separated list as in AnagramQuizActivity and call getCursor_getWords(String list)

    }
    public void oldselectFile() {
        if (!databaseAccess.permission(this))
            return;
        File mPath = new File(Environment.getExternalStorageDirectory() +  "//Documents//" );
        FileDialog fileDialog = new FileDialog(SearchActivity.this, mPath, "txt");
        // only supports one file extension

        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                String full = file.getAbsolutePath();
                importfile.setText(full);
            }
        });
        fileDialog.showDialog();
    }
    public void hidedialogKeyboard() {
        dKeyboardView.setVisibility(View.GONE);
        dKeyboardView.setEnabled(false);
    }
    public void showdialogKeyboard( View v ) {
        dKeyboardView.setVisibility(View.VISIBLE);
        dKeyboardView.setEnabled(true);
        if( v!=null ) ((InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}
