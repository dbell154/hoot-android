package com.tylerhosting.hoot.hoot;

import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.tylerhosting.hoot.hoot.Hoot.context;
import static com.tylerhosting.hoot.hoot.Utils.usingLegacy;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
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
import android.view.Surface;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
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

public class MultiSearchActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    @SuppressLint("ClickableViewAccessibility")

    SharedPreferences shared;
    SharedPreferences.Editor prefs;

//    insert into ks select Word from NSWL18 where Word like "%ARK%"
//    create table ks as select Word from NSWL18 where Word like "%ARK%"


    // Views
    ToggleButton collapse;
    Spinner stype, predef, subjlist;
    Spinner minimum, maximum, sortby, thenby;
    EditText etTerm, etFilter, mLimit, mOffset; //, etLimit, etOffset;
    Button clearEntry, search, blank, emptyRack, more; // altSearch;
    ImageButton searchIcon;
    Button slides, quizreview, quiz, clear, newsearch;
    Button addCriteria;
    // todo    Replace Search button with Image Button
    View Underlay, scrollOverlay;
//    Guideline guide1, guide2, guide3;
    ConstraintLayout lvHeader;
    TextView term, help, importfile;

    ListView listView;
    TextView status, specStatus;
    FloatingActionButton fab;

    LinearLayout criterialayout;
    //    ConstraintLayout[] criterion = new ConstraintLayout[10];
    View[] critView = new View[10];


    // Database variables
    DatabaseAccess databaseAccess;
    List<String> words = new ArrayList<>();
    SimpleCursorAdapter cursorAdapter;
    Cursor cursor;
    LexData.Cardbox cardBox;

    // Support custom keyboard
    Keyboard bKeyboard; // basic
    Keyboard pKeyboard; // pattern
    Keyboard defKeyboard; // full
    KeyboardView dKeyboardView;
    KeyboardView mKeyboardView;
    KeyboardView defKeyboardView;
    // Keyboard dKeyboard; // dialog box (basic)


    // ListView arrays
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


    // Simple variables
    boolean unfiltered;
    String lastStatus = "";
    String selectedWord;
    String ordering = "ORDER BY Length(Word), Word";
    String limits = "";
    String themeName;
    String message = "";
    String beginning = "";
    String ending = "";
    int lastPrefix, lastSuffix, lastsubject, lastMin, lastMax;
    int listfontsize = 24;
    int limit;
    int offset;
    int position, skips;
    // if 0, use fixed search option, other wise critLine
    int numCriteria = 7; // maximum lines
    int currentCrit = 0; // line being processed
    int visibleCriteria = 0; // lines added
    int secondary; // secondaryColor of theme
    long lStartTime;

    // use to pass to other activities
    StringBuilder searchParameters = new StringBuilder();


    // Activity Startup
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = shared.edit();

        // save current theme to compare when changed
        themeName = Utils.setStartTheme(this);
        setContentView(R.layout.activity_multisearch);


        criterialayout = (LinearLayout) findViewById(R.id.criteria);
//        criterialayout

        // todo need to set layout to theme colors


        for(int i=0; i< numCriteria;i++) {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            critView[i] = inflater.inflate(R.layout.criterion, null);


//            criterion[i] = new ConstraintLayout(this);
//            criterion[i] = (ConstraintLayout) findViewById(R.id.critLine);
//
//            val inflater = LayoutInflater.from(this).inflate(R.layout.row_add_language, null)
//            binding.parentLinearLayout.addView(inflater, binding.parentLinearLayout.childCount)
            criterialayout.addView(critView[i]);
            critView[i].setPadding(0,0, 0, 1);
            critView[i].setVisibility(View.GONE);
            critView[i].setTag(Integer.toString(i));
        }


        setDatabase();

        populateResources();

        // Support custom keyboard
        pKeyboard = new Keyboard(MultiSearchActivity.this,R.xml.patternkeyboard);
        bKeyboard = new Keyboard(MultiSearchActivity.this,R.xml.basickeyboard);
        defKeyboard  = new Keyboard(MultiSearchActivity.this,R.xml.defkeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
//        defKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
//        mKeyboardView.setKeyboard( bKeyboard );

        for(int i=0; i< numCriteria;i++) {
            populateCriteria(critView[i]);
            EditText ctv = (EditText) critView[i].findViewById(R.id.critEntry);
            ctv.setTag(Integer.toString(i));
//            criterion[i].setVisibility(View.GONE);
        }
//        for(int i=0; i< numCriteria;i++) {
//            EditText ctv = (EditText) critView[i].findViewById(R.id.critEntry);
//            ctv.setTag(Integer.toString(i));
////            criterion[i].setVisibility(View.GONE);
//        }
       Spinner firstCrit = (Spinner)critView[0].findViewById(R.id.critSearchType);
        int pos = new ArrayList<String>(Arrays.asList(LexData.multiCriteriaSearchText)).indexOf("Word Builder ⌛");
        firstCrit.setSelection(pos);

//       firstCrit.setSelection(3); // show pattern as first criteria
//        mKeyboardView.setKeyboard( pKeyboard ); // because default type is pattern on open

        // first is 0
        critView[visibleCriteria].setVisibility(VISIBLE);
        // increment after population
        visibleCriteria++;

        stype.setVisibility(VISIBLE);

       // show length as initial primary search
       stype.setSelection(1);
       etTerm.setVisibility(GONE);
       blank.setVisibility(GONE);
       clearEntry.setVisibility(GONE);

        hideCustomKeyboard();
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(outState);
        // Save our own state now
        outState.putAll(outState);
    }


    // Activity Resumption
    @Override protected void onPause() {
        super.onPause();
        lastMin = minimum.getSelectedItemPosition();
        lastMax = maximum.getSelectedItemPosition();
    }
    protected void onResume() {
        super.onResume();

        setDisplay();
//        if (LexData.getMultiHelpShown() == false) {
//            showMultiHelp();
//            LexData.setMultiHelpShown(true);
//        }
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

//        loadMinimum();
//        loadMaximum();
//
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

//            if (LexData.getShowCondensed()) {
//                showCondensed();
//            }
//            else
//                showExpanded();
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
//        begins.setSelection(lastPrefix);
//        ends.setSelection(lastSuffix);
        minimum.setSelection(lastMin);
        maximum.setSelection(lastMax);
        subjlist.setSelection(lastsubject);
        if (lastStatus.isEmpty()) {
            if (LexData.getLexicon().LexiconNotice.isEmpty())
                status.setText((LexData.getLexName()));
            else
                status.setText(LexData.getLexicon().LexiconNotice);
        }

        else
            status.setText(lastStatus);

        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();

        // don't show since default selection is length
//        if (listView.getCount() == 0)
//            showkeyboard();
//        etTerm.requestFocus();

    }

    // called from onCreate
    protected void readPreferences() {
        String user = shared.getString("user", "John Smith");
        LexData.setUsername(user);

        String cardLocation = shared.getString("cardlocation", "Storage");
        if (!usingLegacy()) {
            cardLocation = "Internal";
            prefs.putString("cardlocation", cardLocation);
            prefs.putBoolean("saf", true);
            prefs.apply();
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
    protected void populateResources(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setIcon(R.mipmap.howl);
        }
        catch (Exception e) {

        }
        String version = BuildConfig.VERSION_NAME;
        String suffix = BuildConfig.BUILD_TYPE;
        int code = BuildConfig.VERSION_CODE;
        toolbar.setTitle("Hoot" + " " + version);

/*        if (suffix.equals("release"))
            toolbar.setTitle(getString(R.string.app_name) + " " + version);
        else
            toolbar.setTitle(getString(R.string.app_name) + " " + version + "(" + code + ")");
 */




        //        begins = findViewById(R.id.BeginsWith);
//        ends = findViewById(R.id.EndsWith);

//        ArrayAdapter<String> subjectsAdapter = new ArrayAdapter<String>(this,
//                R.layout.simplespin, LexData.predefText);
//        predef.setAdapter(predefAdapter);
//        predef.setOnItemSelectedListener(predefined);

//        loadPrefixes();
//        loadSuffixes();



        sortby = findViewById(R.id.SortBy);
        thenby = findViewById(R.id.ThenBy);

        Underlay = findViewById(R.id.control_underlay);
//        guide1 = findViewById(R.id.guide1);
//        guide2 = findViewById(R.id.guide2);
//        guide3 = findViewById(R.id.guide3);

        // setup spinners
        stype = findViewById(R.id.SearchType);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinselection, LexData.multiPrimarySearchText);
        stype.setAdapter(dataAdapter);
        stype.setOnItemSelectedListener(selection);

        predef = findViewById(R.id.predefined);
        ArrayAdapter<String> predefAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.predefText);
        predef.setAdapter(predefAdapter);
        predef.setOnItemSelectedListener(predefined);

        subjlist = findViewById(R.id.subjects);
        loadSubjectList(subjlist);

        minimum = findViewById(R.id.MinLength);
        maximum = findViewById(R.id.MaxLength);
        loadMinimum(minimum);
        loadMaximum(maximum);



//       ArrayAdapter<String> otherlexAdapter = new ArrayAdapter<String>(this,
//                R.layout.simplespin, LexData.predefText);
//
//        otherlex.setAdapter(otherlexAdapter);
//        otherlex.setOnItemSelectedListener(otherLexicons);

//        stems = findViewById(R.id.stems);
//        ArrayAdapter<String> stemsAdapter = new ArrayAdapter<String>(this,
//                R.layout.spinmedium, LexData.stemsText);
//        stems.setAdapter(stemsAdapter);
//        stems.setOnItemSelectedListener(stemmed);
        addCriteria = findViewById(R.id.btnAddCriteria);
        addCriteria.setOnClickListener(addLine);



        sortby = findViewById(R.id.SortBy);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.sortby);
        sortby.setAdapter(sortAdapter);

        thenby = findViewById(R.id.ThenBy);
        ArrayAdapter<String> thenAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.thenby);
        thenby.setAdapter(thenAdapter);


//        // setup other listeners
//        etTerm = findViewById(R.id.etEntry);
//        etTerm.addTextChangedListener(entryWatcher);
//        etTerm.setOnEditorActionListener(entryAction);
//
//        etFilter = findViewById(R.id.etFilter);
//        etFilter.addTextChangedListener(filterWatcher);
//        etFilter.setOnEditorActionListener(filterAction);
//
//        if (themeName == "Dark Theme") {
//            etTerm.setText(null);
//            etTerm.setHintTextColor(Color.GRAY);
//            etFilter.setHintTextColor(Color.GRAY);
//        }
//
//        emptyRack = findViewById(R.id.EmptyRack);
//        emptyRack.setOnClickListener(EmptyRack);

        more = findViewById(R.id.more);
//        altSearch = findViewById(R.id.altSearch);


//        clearEntry = findViewById(R.id.ClearEntry);
//        clearEntry.setOnClickListener(clearTerm);
//        clearBegins = findViewById(R.id.ClearBegins);
//        clearBegins.setOnClickListener(clearBegin);
//        clearEnds = findViewById(R.id.ClearEnds);
//        clearEnds.setOnClickListener(clearEnd);

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

        searchIcon = findViewById(R.id.SearchIcon);
        searchIcon.setOnClickListener(doSearch);


        collapse = findViewById(R.id.collapse);
        collapse.setOnClickListener(doCollapse);

//        blank = findViewById(R.id.blank);
//        blank.setOnClickListener(enterBlank);
        words.add("Empty");
        status = findViewById(R.id.lblStatus);
        specStatus = findViewById(R.id.specStatus);
        specStatus.setVisibility(GONE); // not using now

        listView = findViewById(R.id.mcresults);
        scrollOverlay = findViewById(R.id.scroll_overlay);

        listView.setScrollBarFadeDuration(0);

//        term = findViewById(R.id.lblTerm);
//        term.setVisibility(GONE);
//
        mLimit = findViewById(R.id.NumWords);
        mOffset = findViewById(R.id.StartingWith);



//
        // fab
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showHelp();

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


//                if (subjlist.getVisibility() == VISIBLE) {
//                    // don't ucase category
//                    selectedWord = wrd.getText().toString();
//                    getListWords(selectedWord);
//                    return true;
//
//                }
                return false; // true indicates the action has been completed, false lets context menu show
            }
        });

        registerForContextMenu(lv);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView wrd = view.findViewById(R.id.word);
                selectedWord = wrd.getText().toString().toUpperCase();
//                if (stems.getVisibility() == VISIBLE)
//                    getStemWords(selectedWord);
                if (subjlist.getVisibility() == VISIBLE) {
                    // don't ucase category
                    selectedWord = wrd.getText().toString();
                    databaseAccess.get_subjectList(selectedWord);
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
                Intent intentBundle = new Intent(MultiSearchActivity.this, SubActivity.class);
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


        // fIRST lINE
        help = findViewById(R.id.HelpGuide);
        help.setOnClickListener(showHelpGuide);


        term = findViewById(R.id.lblTerm);
        term.setVisibility(GONE);


        // setup other listeners
        etTerm = findViewById(R.id.etEntry);
        etTerm.addTextChangedListener(entryWatcher);
        etTerm.setOnEditorActionListener(entryAction);

        etFilter = findViewById(R.id.etFilter);
        etFilter.addTextChangedListener(filterWatcher);
        etFilter.setOnEditorActionListener(filterAction);

        if (themeName == "Dark Theme") {
            etTerm.setText(null);
            etTerm.setHintTextColor(Color.GRAY);
            etFilter.setHintTextColor(Color.GRAY);
        }

        emptyRack = findViewById(R.id.EmptyRack);
        emptyRack.setOnClickListener(EmptyRack);

        clearEntry = findViewById(R.id.ClearEntry);
        clearEntry.setOnClickListener(clearTerm);

        blank = findViewById(R.id.blank);
        blank.setOnClickListener(enterBlank);

        importfile = findViewById(R.id.tvImportFile);
        importfile.setOnClickListener(newFile);
        importfile.setVisibility(GONE);

    }
    protected void setDatabase() {

        // can i put some of this back in splash
        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        prefs = shared.edit();

        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());




        // todo is this needed; compare with splashactivity
        // shouldn't settings validate lexicon
        // initialize lexicon structure
        String lexiconName = shared.getString("lexicon","");

// OUT ON 1/6/22
        // back in 2/28/22
        // fixes attempt to load invalid database: app exits and resets on open
        if (!databaseAccess.isValidDatabase()) {
            Flavoring.addflavoring(getApplicationContext()); // sets database
            LexData.setDatabasePath(getApplicationContext(), "");
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
            prefs.putString("database", "Internal");
            prefs.apply();
        }

        // OUT 1/6/22
        if (lexiconName.equals(""))
            lexiconName = databaseAccess.get_firstValidLexicon();
        if (lexiconName.equals(""))
            databaseAccess.defaultDBLexicon(getApplicationContext());

        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexiconName);
        LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);
        Utils.setLexiconPreference(this);


        // omitted from setDatabase
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
    public void loadLengths(Spinner spinLength) {
        List <String> lengths = new ArrayList<>();
        lengths.add("Minimum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, lengths); // spinmedium
        spinLength.setAdapter(dataAdapter);

    }
    public void loadMinimum(Spinner least) {
        List <String> lengths = new ArrayList<>();
        lengths.add("Minimum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, lengths); // spinmedium
        least.setAdapter(dataAdapter);

    }
    public void loadMaximum(Spinner most) {
        List <String> lengths = new ArrayList<>();
        lengths.add("Maximum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> maxAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, lengths); // R.layout.spinitem
        most.setAdapter(maxAdapter);
    }
    public void loadSubjectList(Spinner sub) {
        List <String> subjectList = new ArrayList<>();
        subjectList = databaseAccess.get_subjects();

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<String>(this,
                R.layout.spinmedium, subjectList);
        sub.setAdapter(subjectAdapter);
    }

    public void critLoadMinimum(Spinner least) {
        List <String> lengths = new ArrayList<>();
        lengths.add("Minimum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, lengths); // spinmedium
        least.setAdapter(dataAdapter);

    }
    public void critLoadMaximum(Spinner most) {
        List <String> lengths = new ArrayList<>();
        lengths.add("Maximum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> maxAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, lengths); // R.layout.spinitem
        most.setAdapter(maxAdapter);
    }
    public void critloadSubjectList(Spinner subj) {
        List <String> subjectList = new ArrayList<>();
        subjectList = databaseAccess.get_subjects();

        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, subjectList);
        subj.setAdapter(subjectAdapter);
    }
    public void critLoadLexicons(Spinner otherlex) {

        List <String> lexicons = new ArrayList<>();
//        lexicons.add("Select Lexicon");
        lexicons = databaseAccess.get_lexiconList();

        if (lexicons.isEmpty())
            lexicons.add("No Other Lexicons");

        ArrayAdapter<String> lexAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, lexicons); // spinmedium
        otherlex.setAdapter(lexAdapter);
    }
//    public void loadCategoryList() {
//        List <String> categoryList = new ArrayList<>();
//        categoryList = databaseAccess.get_categories();
//
//        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<String>(this,
//                R.layout.spinmedium, categoryList);
//        categories.setAdapter(categoryAdapter);
//    }

    protected void clearCriteria(View crit) {

        Spinner srch = (Spinner)crit.findViewById(R.id.critSearchType);
        srch.setSelection(0);

        Spinner min = (Spinner)crit.findViewById(R.id.critMin);
        Spinner max = (Spinner)crit.findViewById(R.id.critMax);

        min.setSelection(0);
        max.setSelection(0);

        EditText entry = crit.findViewById(R.id.critEntry);
        entry.setText("");

        CheckBox notbox = crit.findViewById(R.id.critNotBox);
        notbox.setChecked(false);
    }
    protected void populateCriteria(View crit) {

//        crit.setTag(Integer.toString(visibleCriteria));
//        crit.setVisibility(VISIBLE);



        Spinner srch = (Spinner)crit.findViewById(R.id.critSearchType);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.multiCriteriaSearchText);
        srch.setAdapter(dataAdapter);
        srch.setOnItemSelectedListener(critSelection);
//        android:background="@color/whiteBackground"
//        android:textColor="@color/blackText"



        Spinner min = (Spinner)crit.findViewById(R.id.critMin);
        Spinner max = (Spinner)crit.findViewById(R.id.critMax);
        Spinner subj = crit.findViewById(R.id.critSubject);
        Spinner otherlex = (Spinner)crit.findViewById(R.id.critLexicon);
        critLoadMinimum(min);
        critLoadMaximum(max);
        critloadSubjectList(subj);
        critLoadLexicons(otherlex);
        subj.setOnItemSelectedListener(critSubject);

        EditText entry = crit.findViewById(R.id.critEntry);
        entry.addTextChangedListener(new GenericTextWatcher(entry));

        Spinner critPredef = crit.findViewById(R.id.critPredefined);
        ArrayAdapter<String> predefAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.predefText);

        critPredef.setAdapter(predefAdapter);
        critPredef.setOnItemSelectedListener(critPredefined);

        if (themeName.equals("Dark Theme")) {
            srch.setBackgroundColor(Color.BLACK);
            critPredef.setBackgroundColor(Color.BLACK);
            otherlex.setBackgroundColor(Color.BLACK);
        }
        else {
            srch.setBackgroundColor(Color.WHITE);
            critPredef.setBackgroundColor(Color.WHITE);
            otherlex.setBackgroundColor(Color.WHITE);
        }


        Button clearEntry = crit.findViewById(R.id.critClear);
        clearEntry.setOnClickListener(critClearTerm);

        Button blank = crit.findViewById(R.id.critBlank);
        blank.setOnClickListener(critBlank);

        Button delete = crit.findViewById(R.id.critDelete);
        delete.setOnClickListener(critDelete);

        CheckBox notbox = crit.findViewById(R.id.critNotBox);


        TextView importfile = crit.findViewById(R.id.critFile);
        importfile.setOnClickListener(critNewFile);

//        entry.addTextChangedListener(entryWatcher);
        entry.setOnEditorActionListener(entryAction);

        if (themeName.equals( "Dark Theme") ) {
            srch.setBackgroundColor(Color.BLACK);
            min.setBackgroundColor(Color.BLACK);
            max.setBackgroundColor(Color.BLACK);
        }

        // only used in Sub
//        term = findViewById(R.id.lblTerm);
//        term.setVisibility(GONE);

//        if (themeName == "Dark Theme") {
//            etTerm.setText(null);
//            etTerm.setHintTextColor(Color.GRAY);
//            etFilter.setHintTextColor(Color.GRAY);
//        }

    }

    protected void showHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    private int fetchThemeColor(int themeColor) {
        TypedValue typedValue = new TypedValue();

        TypedArray a = this.obtainStyledAttributes(typedValue.data, new int[] { themeColor });
        int color = a.getColor(0, 0);

        a.recycle();

        return color;
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
            final AlertDialog.Builder builder = new AlertDialog.Builder(MultiSearchActivity.this);

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
    public void openMore(View view) {
        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.limitdialog, null);

//        final Spinner moreSort = alertLayout.findViewById(R.id.moreSortBy);
//        ArrayAdapter<String> sortAdapter = new ArrayAdapter<String>(this,
//                R.layout.darkspin, LexData.sortby);
//        moreSort.setAdapter(sortAdapter);
//        moreSort.setSelection(sortby.getSelectedItemPosition());
//
//        final Spinner moreThen = alertLayout.findViewById(R.id.moreThenBy);
//        ArrayAdapter<String> thenAdapter = new ArrayAdapter<String>(this,
//                R.layout.darkspin, LexData.thenby);
//        moreThen.setAdapter(thenAdapter);
//        moreThen.setSelection(thenby.getSelectedItemPosition());
//
//        final EditText moreFilter = alertLayout.findViewById(R.id.moreFilter);
//        moreFilter.setHint("Rack Filter");
//        moreFilter.setText(etFilter.getText());
//        moreFilter.setTextColor(Color.BLACK);

        final EditText moreNumWords = alertLayout.findViewById(R.id.moreNumWords);
        moreNumWords.setInputType(InputType.TYPE_CLASS_NUMBER);
        moreNumWords.setHint("Word Limit");
        moreNumWords.setText(mLimit.getText());
        moreNumWords.setTextColor(Color.BLACK);

        final EditText moreStartWith = alertLayout.findViewById(R.id.moreStartWith);
        moreStartWith.setInputType(InputType.TYPE_CLASS_NUMBER);
        moreStartWith.setHint("Start with");
        moreStartWith.setText(mOffset.getText());
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
                mLimit.setText(moreNumWords.getText());
                mOffset.setText(moreStartWith.getText());
//                begins.setSelection(moreBegins.getSelectedItemPosition());
//                ends.setSelection(moreEnds.getSelectedItemPosition());
//                sortby.setSelection(moreSort.getSelectedItemPosition());
//                thenby.setSelection(moreThen.getSelectedItemPosition());
//                etFilter.setText(moreFilter.getText());

//                generateSpecStats();

//                StringBuilder specs = new StringBuilder();
//                specs.append("Specs::" );
//                if (begins.getSelectedItemPosition() != 0)
//                    specs.append(" Begins " + begins.getSelectedItem().toString());
//                if (ends.getSelectedItemPosition() != 0)
//                    specs.append(" Ends " + ends.getSelectedItem().toString());
//                if (sortby.getSelectedItemPosition() != 0)
//                    specs.append(" Sort By " + sortby.getSelectedItem().toString());
//                if (thenby.getSelectedItemPosition() != 0)
//                    specs.append(" Then By " + thenby.getSelectedItem().toString());
//                if (!etFilter.getText().toString().equals(""))
//                    specs.append(" Rack " + etFilter.getText().toString());
//                if (!etLimit.getText().toString().equals(""))
//                    specs.append(" Limit " + etLimit.getText().toString());
//                if (!etOffset.getText().toString().equals(""))
//                    specs.append(" Beginning " + etOffset.getText().toString());

//                specStatus.setText(specs);
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
//                moreBegins.setSelection(0);
//                moreEnds.setSelection(0);
//                moreSort.setSelection(0);
//                moreThen.setSelection(0);
//                moreFilter.setText("");
                moreNumWords.setText("");
                moreStartWith.setText("");

//                begins.setSelection(moreBegins.getSelectedItemPosition());
//                ends.setSelection(moreEnds.getSelectedItemPosition());
//                sortby.setSelection(moreSort.getSelectedItemPosition());
//                thenby.setSelection(moreThen.getSelectedItemPosition());
//                etFilter.setText(moreFilter.getText());
                mLimit.setText(moreNumWords.getText());
                mOffset.setText(moreStartWith.getText());
//                specStatus.setText("Other Specifications");
            }
        });
        builder.show();
    }
    protected void collapse() {

        if (collapse.isChecked()) {
            stype.setVisibility(GONE);
            predef.setVisibility(GONE);
            subjlist.setVisibility(GONE);
            etTerm.setVisibility(GONE);

//                    stems.setVisibility(GONE);
            subjlist.setVisibility(GONE);

            Underlay.setVisibility(GONE);
            minimum.setVisibility(GONE);
            maximum.setVisibility(GONE);
            blank.setVisibility(GONE);
            clearEntry.setVisibility(GONE);
            sortby.setVisibility(GONE);
            thenby.setVisibility(GONE);
            search.setVisibility(GONE);
            searchIcon.setVisibility(GONE);

            etFilter.setVisibility(GONE);
            emptyRack.setVisibility(GONE);
//            etLimit.setVisibility(GONE);
//            etOffset.setVisibility(GONE);
//
            more.setVisibility(GONE);
//            altSearch.setVisibility(GONE);
            specStatus.setVisibility(GONE);

            ConstraintSet hiding = new ConstraintSet();
            ConstraintLayout layout = findViewById(R.id.multisearch_layout);
            hiding.clone(layout);
            hiding.connect(R.id.imcheader,ConstraintSet.TOP, R.id.collapse, ConstraintSet.BOTTOM);
            hiding.applyTo(layout);

            addCriteria.setVisibility(GONE);
            criterialayout.setVisibility(GONE);

        }
        else { // not collapsed
            stype.setVisibility(VISIBLE);
            switch(stype.getSelectedItem().toString()) {
//            switch (stype.getSelectedItemPosition()) {
                case "Length":
                    minimum.setVisibility(VISIBLE);
                    maximum.setVisibility(VISIBLE);
                    break;
                case "From File":
                    importfile.setVisibility(VISIBLE);
                    break;
//                case 13:
//                    stems.setVisibility(VISIBLE);
//                    break;
                case "Predefined":
                    predef.setVisibility(VISIBLE);
                    break;
                case "Subject Lists":
                    subjlist.setVisibility(VISIBLE);
                    break;
                default:
                    etTerm.setVisibility(VISIBLE);
                    blank.setVisibility(VISIBLE);
                    clearEntry.setVisibility(VISIBLE);
                    break;
            }
//            guide1.setVisibility(VISIBLE);
//            guide3.setVisibility(VISIBLE);
            Underlay.setVisibility(VISIBLE);

            criterialayout.setVisibility(VISIBLE);
            addCriteria.setVisibility(VISIBLE);

//            if (LexData.getShowCondensed()) {
////                begins.setVisibility(GONE);
////                clearBegins.setVisibility(GONE);
////                ends.setVisibility(GONE);
////                clearEnds.setVisibility(GONE);
//                sortby.setVisibility(GONE);
//                thenby.setVisibility(GONE);
//
//                search.setVisibility(GONE);
//
//                etFilter.setVisibility(GONE);
//                emptyRack.setVisibility(GONE);
////                etLimit.setVisibility(GONE);
////                etOffset.setVisibility(GONE);
////
//                more.setVisibility(VISIBLE);
////                altSearch.setVisibility(VISIBLE);
//                specStatus.setVisibility(VISIBLE);
//
//                ConstraintSet displayed = new ConstraintSet();
//                ConstraintLayout layout = findViewById(R.id.search_layout);
//                displayed.clone(layout);
//                displayed.connect(R.id.imcheader,ConstraintSet.TOP, R.id.specStatus, ConstraintSet.BOTTOM);
//                displayed.applyTo(layout);
//            }
//            else {
////                begins.setVisibility(VISIBLE);
////                clearBegins.setVisibility(VISIBLE);
////                ends.setVisibility(VISIBLE);
////                clearEnds.setVisibility(VISIBLE);
//                sortby.setVisibility(VISIBLE);
//                thenby.setVisibility(VISIBLE);
//
//                search.setVisibility(VISIBLE);
//
////                etFilter.setVisibility(VISIBLE);
////                emptyRack.setVisibility(VISIBLE);
////                etLimit.setVisibility(VISIBLE);
////                etOffset.setVisibility(VISIBLE);
//
//                more.setVisibility(GONE);
////                altSearch.setVisibility(GONE);
//                specStatus.setVisibility(GONE);
//
//                ConstraintSet displayed = new ConstraintSet();
//                ConstraintLayout layout = findViewById(R.id.multisearch_layout);
//                displayed.clone(layout);
//                displayed.connect(R.id.imcheader,ConstraintSet.TOP, R.id.Search, ConstraintSet.BOTTOM);
//                displayed.applyTo(layout);
//            }

            more.setVisibility(VISIBLE);
            sortby.setVisibility(VISIBLE);
            thenby.setVisibility(VISIBLE);

//            search.setVisibility(VISIBLE);
            searchIcon.setVisibility(VISIBLE);
            ConstraintSet displayed = new ConstraintSet();
            ConstraintLayout layout = findViewById(R.id.multisearch_layout);
            displayed.clone(layout);
            displayed.connect(R.id.imcheader,ConstraintSet.TOP, R.id.SearchIcon, ConstraintSet.BOTTOM);
            displayed.applyTo(layout);

//            search.setVisibility(VISIBLE);

        }
    }




    // OPTIONS MENU
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.multi_main_menu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.activity_cards) {
            if (LexData.getCardslocation().equals("Internal"));
            else
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

        if (id == R.id.activity_search) {
            Intent intent = new Intent(this,SearchActivity.class);
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
            resetDatabase();

//            need to reload lexicons for otherlex
            if (myKeyboard != LexData.getCustomkeyboard())
                recreate();

            if (Utils.themeChanged(themeName, this)) {
                // in multi, tried to recreate to handle imported criteria layout ??
                recreate();

                switch (themeName) {
                    case "Dark Theme":
                        for (int i = 0; i < numCriteria; i++) {
                            Spinner search = critView[i].findViewById(R.id.critSearchType);
                            Spinner minimum = critView[i].findViewById(R.id.critMin);
                            Spinner maximum = critView[i].findViewById(R.id.critMax);
                            search.setBackgroundColor(Color.BLACK);
                            minimum.setBackgroundColor(Color.BLACK);
                            maximum.setBackgroundColor(Color.BLACK);
                        }
                        break;
                    case "Light Theme":
                    default:
                        for (int i = 0; i < numCriteria; i++) {
                            Spinner search = critView[i].findViewById(R.id.critSearchType);
                            Spinner minimum = critView[i].findViewById(R.id.critMin);
                            Spinner maximum = critView[i].findViewById(R.id.critMax);
                            search.setBackgroundColor(Color.WHITE);
                            minimum.setBackgroundColor(Color.WHITE);
                            maximum.setBackgroundColor(Color.WHITE);
                        }
                        break;
                }
            }
        }

        if (id == R.id.activity_help) {
            getHelp("help.html");
//            change
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
            return true;
        }

        if (id == R.id.exit) {
            Utils.exitAlert(this);
        }

        return super.onOptionsItemSelected(item);
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

        // don't want to show again from SubActivity
//        if (thisActivity.equals("SearchActivity")) {
//            Toast.makeText(this, "Using Lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
//            Toast.makeText(this, LexData.getLexicon().LexiconNotice, Toast.LENGTH_LONG).show();
//        }
        Utils.setLexiconPreference(this);

        // todo Do I need to checkScores like in Settings?
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
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

            if (databaseAccess.getVersion() < LexData.getCurrentVersion(this))
//            {
////                startUpdateMessage();
//                updateDatabase();
//                LexData.setCurrentVersion(this, databaseAccess.getVersion());
//
//
//            if (databaseAccess.getVersion() < LexData.CURRENT_VERSION)
                Toast.makeText(this, "There is an updated database; restart Hoot to use it.", Toast.LENGTH_LONG).show();
        }
        else {
            LexData.setDatabase(getApplicationContext(), fullpath.substring(fullpath.lastIndexOf(File.separator)));
            LexData.setDatabasePath(getApplicationContext(), fullpath.substring(0, fullpath.lastIndexOf(File.separator)));
        }
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());




        // todo is this needed; compare with splashactivity
        // shouldn't settings validate lexicon
        // initialize lexicon structure
        String lexiconName = shared.getString("lexicon","");

        // fixes attempt to load invalid database: app exits and resets on open
        if (!databaseAccess.isValidDatabase()) {
            Flavoring.addflavoring(getApplicationContext()); // sets database
            LexData.setDatabasePath(getApplicationContext(), "");
            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
            prefs.putString("database", "Internal");
            prefs.apply();
        }

        if (lexiconName.equals(""))
            lexiconName = databaseAccess.get_firstValidLexicon();
        if (lexiconName.equals(""))
            databaseAccess.defaultDBLexicon(getApplicationContext());

        Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexiconName);
        LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);
        Utils.setLexiconPreference(this);

    }
    public void openTools() {
        Intent intent = new Intent(this, ToolsActivity.class);
        startActivity(intent);
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
                if (!LexData.getCardslocation().equals("Internal"))
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

                final AlertDialog.Builder builder = new AlertDialog.Builder(MultiSearchActivity.this, R.style.darkAlertDialog);

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

                Intent intentBundle = new Intent(MultiSearchActivity.this, SubActivity.class);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
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




        AlertDialog.Builder saver = new AlertDialog.Builder(MultiSearchActivity.this, R.style.darkAlertDialog); // R.style.LightTheme);
        saver.setTitle("Save List to File");

        Context savecontext = MultiSearchActivity.this;
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
    private void saveListO(String filename) throws IOException {
        String DEST_PATH;
        int counter = 0;
        int column = cursor.getColumnIndex("Word");

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            DEST_PATH = Environment.getExternalStoragePublicDirectory("Documents").toString();
        else
            DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        File directory = new File(DEST_PATH);
        if (!directory.exists())
            if (!directory.mkdirs()) {
                Log.e("BackupDB", "Can't mkdirs()" + directory.getAbsolutePath());
                Toast.makeText(getBaseContext(), "Can't create folder for saving", Toast.LENGTH_LONG).show();
                return;
            }

        if (!filename.endsWith(".txt"))
                filename = filename + ".txt";

        filename = DEST_PATH + File.separator + filename;

        FileWriter writer;

        try {
            writer = new FileWriter(filename);

            String sep;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                sep = System.lineSeparator();
            } else sep = "\n";

            cursor.moveToFirst();
            counter++;
            writer.append(cursor.getString(column).toUpperCase() + sep);
            while (cursor.moveToNext()) {
                counter++;
                writer.append(cursor.getString(column).toUpperCase() + sep);
            }
            writer.close();
            Toast.makeText(getBaseContext(), "Saving " + counter + "words to " + filename, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }



    private static final int CREATE_FILE = 1;
    private static final int REQUEST_FOLDER = 9;
    private static final int PICK_TXT_FILE = 3;
    private static final int PICK_CRIT_FILE = 2;
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
                String fileUrl = String.valueOf(Uri.parse(uri.toString()));
                importfile.setPrivateImeOptions(uri.toString());
                File fn = new File("" + Uri.parse(uri.toString()));
                importfile.setText(Uri.decode(fn.getAbsolutePath()));
//                importfile.setText(uri.toString());
            }
        }

        if (requestCode == PICK_CRIT_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                // Perform operations on the document using its URI.
                String fileUrl = String.valueOf(Uri.parse(uri.toString()));
                TextView ffile = critView[currentCrit].findViewById(R.id.critFile);

                ffile.setPrivateImeOptions(uri.toString());
                File fn = new File("" + Uri.parse(uri.toString()));
                ffile.setText(Uri.decode(fn.getAbsolutePath()));
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

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, filename);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);

            startActivityForResult(intent, CREATE_FILE);
        } else {

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
                Toast.makeText(getBaseContext(), "Saving " + counter + "words to " + filename, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    protected final AdapterView.OnItemSelectedListener selection = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {


            switch (stype.getSelectedItem().toString()) {
                case "Pattern ⌛":
                    mKeyboardView.setKeyboard(pKeyboard);
                    break;
                case "With Definition":
                    mKeyboardView.setKeyboard(defKeyboard);
                    break;
                default:
                    String before = etTerm.getText().toString();
                    String after = before.replaceAll("[\\[\\]<>cv*@0123456789.,^+-]", "");
                    etTerm.setText(after);
                    mKeyboardView.setKeyboard(bKeyboard);
            }


//            if (!stype.getSelectedItem().toString().equals("Pattern ⌛")) {
////            if (stype.getSelectedItemPosition() != 3) {
//                String before = etTerm.getText().toString();
//                String after = before.replaceAll("[\\[\\]<>cv*@0123456789.,^+-]", "");
//                etTerm.setText(after);
//                mKeyboardView.setKeyboard( bKeyboard );
//            }
//            else
//                mKeyboardView.setKeyboard( pKeyboard );
//
//            if (stype.getSelectedItem().toString().equals("With Definition"))
//                mKeyboardView.setKeyboard(defKeyboard);

            // hide elements that are special
            // show elements that are common
            etTerm.setVisibility(VISIBLE); // common
            blank.setVisibility(VISIBLE);
            clearEntry.setVisibility(VISIBLE);

            importfile.setVisibility(GONE); // only with from file
            minimum.setVisibility(GONE); // only with length
            maximum.setVisibility(GONE);

            // removed from multisearch
            predef.setVisibility(GONE);
//            stems.setVisibility(GONE);
            subjlist.setVisibility(GONE);
//            etLimit.setVisibility(GONE);
//            etOffset.setVisibility(GONE);
            etTerm.requestFocus();


            if (stype.getSelectedItem().toString().equals("Word Builder ⌛"))
//            if (stype.getSelectedItemPosition() == 5) // word builder
                sortby.setSelection(2); // length descending


            showkeyboard();
            // todo set visible items for each
            switch (stype.getSelectedItem().toString()) {
//            switch (stype.getSelectedItemPosition() ) {
                case "Anagrams":
                    break;
                case "Length":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    minimum.setVisibility(VISIBLE);
                    maximum.setVisibility(VISIBLE);
                    hidekeyboard();

                    // size only changed after type selected
//                    if (etTerm.getText().length() > 0)
//                        if (minimum.getSelectedItemPosition() - 1 < etTerm.getText().length() )
//                            minimum.setSelection(etTerm.getText().length());
                    break;

                // adjust case number

                case "From File":
                    importfile.setVisibility(VISIBLE);
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    hidekeyboard();
                    selectFile();
                    break;


                case "Predefined":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    predef.setVisibility(VISIBLE);
                    hidekeyboard();
                    subjlist.setVisibility(GONE);
                    break;
                case "Subject Lists":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    predef.setVisibility(GONE);
                    subjlist.setVisibility(VISIBLE);
                    hidekeyboard();
                    break;
//                case 15:
//                    etTerm.setVisibility(GONE);
//                    stems.setVisibility(GONE);
//                    predef.setVisibility(GONE);
//                    categories.setVisibility(VISIBLE);
//                    break;
//            "Anagrams", // anagrams,
//                    "Length", // "Letter Count", //length,
//                    "Hook Words", //hooks
//                    "Pattern ⌛",
//                    "Contains", // extensions, // contains
//
//                    "Word Builder ⌛", // subanagrams, // word builder (contains only)
//                    "Contains All", // superanagrams, // contains all letters
//                    "Contains Any",
//                    "Begins With", //begins,
//                    "Ends With", // ends,
                case "Pattern ⌛": // pattern
                case "Parallel ⌛":
                    Toast.makeText(getApplicationContext(),"\nSelect lengths for quicker results\n", Toast.LENGTH_LONG).show();
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
    protected final AdapterView.OnItemSelectedListener critSelection = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            ConstraintLayout line = (ConstraintLayout) ((ViewGroup) view.getParent()).getParent();
//            currentCrit = (int) line.getTag();
            currentCrit = (int) Integer.parseInt(line.getTag().toString());
            Log.e("currentCrit", "Tag" + currentCrit);

            // create LOCAL handles for line elements
            Spinner stype = critView[currentCrit].findViewById(R.id.searchType);
            EditText etTerm = critView[currentCrit].findViewById(R.id.critEntry);
            Button blank = critView[currentCrit].findViewById(R.id.critBlank);
            Button clearEntry = critView[currentCrit].findViewById(R.id.critClear);
            TextView importfile = critView[currentCrit].findViewById(R.id.critFile);
            Spinner minimum = critView[currentCrit].findViewById(R.id.critMin);
            Spinner maximum = critView[currentCrit].findViewById(R.id.critMax);
            Spinner predef = critView[currentCrit].findViewById(R.id.critPredefined);
            Spinner subj = critView[currentCrit].findViewById(R.id.critSubject);
            Spinner otherlex = critView[currentCrit].findViewById(R.id.critLexicon);

            String critType = LexData.multiCriteriaSearchText[position];

            // trying to enable custom keyboard
//            if (LexData.getCustomkeyboard())
//                hideCustomKeyboard();


//            if (LexData.getCustomkeyboard())

            switch (critType) {
                case "Pattern ⌛":
                    mKeyboardView.setKeyboard(pKeyboard);
                    break;
                case "With Definition":
                    mKeyboardView.setKeyboard(defKeyboard);
                    break;
                default:
                    String before = etTerm.getText().toString();
                    String after = before.replaceAll("[\\[\\]<>cv*@0123456789.,^+-]", "");
                    etTerm.setText(after);
                    mKeyboardView.setKeyboard(bKeyboard);
            }

//            if (!critType.equals("Pattern ⌛")) {
//                String before = etTerm.getText().toString();
//                String after = before.replaceAll("[\\[\\]<>cv*@0123456789.,^+-]", "");
//
//                etTerm.setText(after);
//                mKeyboardView.setKeyboard(bKeyboard);
//            } else {
//                mKeyboardView.setKeyboard(pKeyboard);
//            }
//            if (critType.equals("With Definition"))
//                mKeyboardView.setKeyboard(defKeyboard);

            // hide elements that are special
            // show elements that are common
            etTerm.setVisibility(VISIBLE); // common
            blank.setVisibility(VISIBLE);
            clearEntry.setVisibility(VISIBLE);

            predef.setVisibility(GONE);
            subj.setVisibility(GONE);
            otherlex.setVisibility(GONE);
            importfile.setVisibility(GONE); // only with from file
            minimum.setVisibility(GONE); // only with length
            maximum.setVisibility(GONE);
            etTerm.requestFocus();

            // removed from multisearch
//            predef.setVisibility(GONE);
//            stems.setVisibility(GONE);
//            etLimit.setVisibility(GONE);
//            etOffset.setVisibility(GONE);


//            if (position == 5) // word builder
//                sortby.setSelection(2); // length descending

            // todo set visible items for each
            switch (critType ) {
                case "Anagrams":
                    break;
                case "Length":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);

                    minimum.setVisibility(VISIBLE);
                    maximum.setVisibility(VISIBLE);
                    break;

                case "Hook Words":
                    etTerm.setVisibility(INVISIBLE);
                    blank.setVisibility(INVISIBLE);
                    clearEntry.setVisibility(INVISIBLE);
                    break;

                case "Pattern ⌛": // pattern
                    break;

                case "Predefined":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    predef.setVisibility(VISIBLE);
                    subj.setVisibility(GONE);
                    otherlex.setVisibility(GONE);
                    break;

                case "Subject Lists":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    predef.setVisibility(GONE);
                    subj.setVisibility(VISIBLE);
                    otherlex.setVisibility(GONE);
                    break;

                case "In Lexicon":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    predef.setVisibility(GONE);
                    otherlex.setVisibility(VISIBLE);
                    break;

                case "Takes Prefix":
                case "Takes Suffix":
                case "Alt Ending ⌛":
                case "Replace ⌛":
                    Toast.makeText(getApplicationContext(),"\nSelect lengths for quicker results\n", Toast.LENGTH_LONG).show();
                    break;

                // different position in multisearch (not 16, 17, 18, 19)
                case "From File":
                    etTerm.setVisibility(GONE);
                    blank.setVisibility(GONE);
                    clearEntry.setVisibility(GONE);
                    importfile.setVisibility(VISIBLE);
                    selectcritFile(importfile);
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    // Searching: This responds to Search click
    int cursorPosition;
    protected void executeSearch() {
        // prep search
        listView.setAdapter(null);
        hidekeyboard();
///trim        unless definition
        String trim = etTerm.getText().toString().trim();

        if (!stype.getSelectedItem().toString().equals("With Definition"))
            etTerm.setText(String.valueOf(trim));

//        unfiltered = (etFilter.getText().length() == 0);
        lStartTime = System.nanoTime();
        status.setText(R.string.searching);
        searchParameters.setLength(0);

        ordering = getSortOrder();
        setLimits();
        limits = Utils.limitStringer(limit,offset);
        Log.d("getCursor", limits);

        // if History, save search
        cursorPosition = etTerm.getSelectionEnd();

        int least = minimum.getSelectedItemPosition() + 1;
        int most = maximum.getSelectedItemPosition() + 1;
        if (most == 1) { // If Any, assume same as least
            most = least;
            maximum.setSelection(most - 1); //
        }
        else if (least == 1) { // If Any, assume same as least
            least = most;
            minimum.setSelection(least - 1); //
        }

        // TODO SHOW PARAMETER BASED ON SEARCH TYPE
        
        executeSearchThread(this);
        
        
    }

    protected ProgressDialog dialog;
    public String alertMessage = "Beginning Search...";
    private Runnable dialogMessages = new Runnable() {
        @Override
        public void run() {
            //Log.v(TAG, strCharacters);
            dialog.setMessage(alertMessage);
        }
    };
    protected void  executeSearchThread(final Context context) {
        @SuppressLint("StaticFieldLeak")
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            public void onPreExecute() {
                dialog = Utils.themeDialog(context);
                dialog.setMessage("Please Wait!\r\nThis search may take a minute or two...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //dialog.dismiss();
                    }
                });
                dialog.setCancelable(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        // cancel AsyncTask
                        cancel(false);
                    }
                });
                dialog.setCanceledOnTouchOutside(false);
                dialog.show();

                databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

                databaseAccess.open();

            }
            
            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                ((Activity) context).runOnUiThread(dialogMessages);

                List<String> wordsFound = new ArrayList<>();
                List<String> primary = new ArrayList<>();
                List<String> critList = new ArrayList<>();
                List<String> current;
                List<String> complete;
                List<String> empty = new ArrayList<>();
//                String alertMessage;
                int least = minimum.getSelectedItemPosition() + 1;
                int most = maximum.getSelectedItemPosition() + 1;
                if (most == 1) { // If Any, assume same as least
                    most = least;
                    maximum.setSelection(most - 1); //
                }
                else if (least == 1) { // If Any, assume same as least
                    least = most;
                    minimum.setSelection(least - 1); //
                }
                if (most == least && most == 1) {
                    least = 2;
                    most = LexData.getMaxLength();
                    minimum.setSelection(least - 1); //
                    maximum.setSelection(most - 1); //
                }

                // primary search

                // check for valid input
                // todo this is where do go through each criteria line

                String searchType = stype.getSelectedItem().toString();
                String entry = etTerm.getText().toString();
                switch (searchType) {
                    case "HookWords":
                        alertMessage = "Primary search " + stype.getSelectedItem().toString();
                        break;
                    case "Length":
//                        if (least == 1 || most == 1) {
//                            Toast.makeText(context, "You must specify lengths to continue this search", Toast.LENGTH_LONG).show();
//                            return null;
//                        }
                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + least + " - " + most + " letters";
                        break;
                    case "Predefined":
                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + predef.getSelectedItem().toString();
                        break;
                    case "Subject Lists":
                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + subjlist.getSelectedItem().toString();
                        break;
//                    case "With Definition":
//                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + predef.getSelectedItem().toString();
//
//                            break;
                    case "From File":
                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + importfile.getText();
                        break;
                    case "Alt Ending ⌛":
                    case "Replace ⌛":
                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + entry;
                        if (!entry.contains("?"))
                            return null;
                        if (entry.length()<3)
                            return null;
                        if (entry.charAt(0) == '?')
                            return null;
                        if (entry.charAt(entry.length()-1) == '?')
                            return null;
                        break;
                    default:
                        alertMessage = "Primary search " + stype.getSelectedItem().toString() + " " + etTerm.getText();
                        if (etTerm.getText().toString().isEmpty())
                            return null;
                }
                Log.e("Searching", alertMessage);
//                Toast.makeText(context,alertMessage, Toast.LENGTH_LONG).show();
//                message = "Please Wait!\r\nCalculating word scores...\r\n" + size + " letters";
                ((Activity) context).runOnUiThread(dialogMessages);


                switch (stype.getSelectedItem().toString()) {
                    case "Anagrams": // Anagrams
                        if (entry.trim().length() > 1) {
                            if (entry.contains("*") || entry.contains("@")) {
                                String letters = entry.replaceAll("[*@]", "");
                                primary = databaseAccess.getList_superanagrams(letters);
                            } else //
                                primary = databaseAccess.getList_anagrams(etTerm.getText().toString());
                        }
                        else {
                            incompleteSearch();
//                            return null;
                        }
                        break;
                    case "Length": // Length
//                        if (least == 1 || most == 1) {
//                            Toast.makeText(context, "You must specify lengths to continue this search", Toast.LENGTH_LONG).show();
//                            return null;
//                        }

                        primary = databaseAccess.getList_BetweenLengths(least, most);
                        break;
//                    case "Hook Words":
//                        primary = databaseAccess.getList_gethashooks();
//                        break;
                    case "Pattern ⌛":
                        // primary = databaseAccess.getList_pattern(etTerm.getText().toString());


                        String pattern = etTerm.getText().toString();
                        if (pattern.length() < 2) {
                                primary = empty;
                                break;
                            }
                        String prep = databaseAccess.buildInnerPattern(pattern);
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
                        Log.d("pt", sql);
                        Cursor precursor = databaseAccess.rawQuery(sql);

                        while (precursor.moveToNext()) {
//                        String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
                            String word = precursor.getString(0);

                            if (word.matches(exp)) {
                                wordsFound.add(word);
                                Log.d("pt", word);
                            }
                            if (isCancelled()) {
                                precursor.close();
                                break;
                            }
                        }
                        precursor.close();
                        primary = wordsFound;


                    break;
                    case "Contains": // Contains
                        primary = databaseAccess.getList_contains(etTerm.getText().toString());
                        break;

                    case "Word Builder ⌛": // Build
//                        primary = databaseAccess.getList_subanagrams(etTerm.getText().toString());



                        String term = etTerm.getText().toString();
                        if (term.trim().length() < 2) {
                            primary = empty;
                            break;
                        }

                        Cursor cursor;
                        lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) <= %2$s", LexData.getMaxLength(), term.length());

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

                        String st = "SELECT Word \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                speedFilter +
                                " ) ";

                        cursor = databaseAccess.rawQuery("SELECT Word \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                speedFilter +
                                " ) ");
                        Log.d("sol", st);

                        while (cursor.moveToNext()) {
                            String word = cursor.getString(0);
                            char[] anagram = word.toCharArray();
                            if (databaseAccess.isAnagram(first, anagram, blankcount)) {
                                wordsFound.add(word);
                            }
                            if (isCancelled()) {
                                cursor.close();
                                break;
                            }
                        }
                        cursor.close();
                        primary = wordsFound;



                        break;
                    case "Contains All": // Contains all (Superanagrams)
                        primary = databaseAccess.getList_superanagrams(etTerm.getText().toString());
                        break;
                    case "Contains Any": // Contains any
                        primary = databaseAccess.getList_containsAny(etTerm.getText().toString());
                        break;
                    case "Begins With":
                        primary = databaseAccess.getList_begins(etTerm.getText().toString());
                        break;
                    case "Ends With": // Contains any
                        primary = databaseAccess.getList_ends(etTerm.getText().toString());
                        break;

                    case "Predefined":
                        primary = databaseAccess.getList_predefined(predef.getSelectedItemPosition(), context);
                        break;
                    case "Subject Lists":
                        primary = databaseAccess.getList_SubjectList(subjlist.getSelectedItem().toString());
                        break;


//                    case "Takes Prefix":
//                        primary = databaseAccess.getList_takesPrefix(etTerm.getText().toString());
//                        break;
//                    case "Takes Suffix":
//                        primary = databaseAccess.getList_takesSuffix(etTerm.getText().toString());
//                        break;
//                    case "Alt Ending ⌛":
//                        primary = databaseAccess.getList_altEnding(etTerm.getText().toString());
//                        break;
//                    case "Replace ⌛":
//                        primary = databaseAccess.getList_replace(etTerm.getText().toString());
//                        break;

                    case "With Definition":
                        primary = databaseAccess.getList_withDef(etTerm.getText().toString());
                        break;
                    case "From File": // From File
                        if (importfile.getText().toString().equals("File specification") ||
                                importfile.getText().toString().isEmpty()) {
                            Toast.makeText(context, "File must be selected", Toast.LENGTH_LONG).show();
                            return null;
                        }

//                        primary = databaseAccess.getList_fromfile(getApplicationContext(), importfile.getText().toString());
                        primary = databaseAccess.getList_fromfile(getApplicationContext(), importfile.getPrivateImeOptions().toString());


                        break;
                    default:
                        return null;
                }
                current = primary;


                // criteria searches
                for(int i=0; i< numCriteria;i++) {
                    if (current == null) {
                        current = empty;
                        break;
                    }
                    if (current.isEmpty()) {
//                        Toast.makeText(context, "Search returned 0 words, remaining searches aborted.", Toast.LENGTH_LONG).show();
                        break;
                    }

                    if (critView[i].getVisibility() == VISIBLE) {
                        Spinner search = critView[i].findViewById(R.id.critSearchType);
                        EditText term = critView[i].findViewById(R.id.critEntry);
                        TextView importfile = critView[i].findViewById(R.id.critFile);

                        Spinner minimum = critView[i].findViewById(R.id.critMin);
                        Spinner maximum = critView[i].findViewById(R.id.critMax);
                        CheckBox notbox = critView[i].findViewById(R.id.critNotBox);
                        Spinner predef = critView[i].findViewById(R.id.critPredefined);
                        Spinner subj = critView[i].findViewById(R.id.critSubject);
                        Spinner otherlex = critView[i].findViewById(R.id.critLexicon);

                        alertMessage = "Criteria search " + search.getSelectedItem().toString() + " " + term.getText();
                        Log.e("Searching", alertMessage);
//                        Toast.makeText(context,alertMessage, Toast.LENGTH_LONG).show();
                        ((Activity) context).runOnUiThread(dialogMessages);



                        int min = minimum.getSelectedItemPosition() + 1;
                        int max = maximum.getSelectedItemPosition() + 1;
                        if (max == 1) { // If Any, assume same as least
                            max = min;
//                            maximum.setSelection(max - 1); //
                        }
                        Log.d("MM", "min-max" + min + max);

                        String critSearchType = search.getSelectedItem().toString();
                        String critEntry = term.getText().toString();

                        // todo this is where do go through each criteria line
                        // TODO TOAST MESSAGES

//                String critsearchType = search.getSelectedItem().toString();
//                switch (critsearchType) {
//                    case "HookWords":
//                    case "Length":
//                    case "Predefined":
//                    case "From File":
//                        break;
//                    case "Alt Ending ⌛":
//                    case "Replace ⌛":
//                        if (!term.getText().toString().contains("?"))
//                            return;
//                    default:
//                        if (term.getText().toString().isEmpty())
//                            return;
//                }
//
                        if (critList != null)
                            critList.clear();

//                        wordsFound.clear();
                        Cursor searcher;
                        String altSet;
                        String column[];
                        Cursor altcursor;
                        List<String> alternates = new ArrayList<>();


                        //                        wordsFound.clear();
                        entry = term.getText().toString();
                        switch (critSearchType) {
//                switch (search.getSelectedItemPosition()) {
                            case "Anagrams": // Anagrams
                                if (entry.trim().length() < 1) {
                                    critList = null;
                                    break;
                                }
                                if (entry.trim().length() > 1) {
                                    if (entry.contains("*") || entry.contains("@")) {
                                        String letters = entry.replaceAll("[*@]", "");
                                        critList = databaseAccess.getList_superanagrams(letters, current);
                                    } else //
                                        critList = databaseAccess.getList_anagrams(entry, current);
                                }
//                                critList = databaseAccess.getList_anagrams(term.getText().toString(), current);
                                break;
                            case "Length": // Length
                                alertMessage = "Criteria search " + critSearchType + " " + min + " - " + max + " letters";
                                critList = databaseAccess.getList_BetweenLengths(min, max, current);
                                break;
                            case "Hook Words":
                                alertMessage = "Criteria search " + critSearchType;
                                critList = databaseAccess.getList_gethashooks(current);
                                break;
                            case "Pattern ⌛":
//                                critList = databaseAccess.getList_pattern(term.getText().toString(), current);
                                String pattern = term.getText().toString();

                                if (pattern.length() < 2) {
                                        critList = null;
                                        break;
                                    }
                                String prep = databaseAccess.buildInnerPattern(pattern);
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

                                for (String word : current) {
                                    if (word.matches(exp))
                                        wordsFound.add(word);
                                    if (isCancelled()) {
                                        break;
                                    }
                                }

                                critList = wordsFound;





                                break;
                            case "Contains": // Contains
                                critList = databaseAccess.getList_contains(term.getText().toString(), current);
                                break;
                            case "Word Builder ⌛": // Build
//                                critList = databaseAccess.getList_subanagrams(term.getText().toString(), current);

                                String strTerm = term.getText().toString();

                                if (strTerm.trim().length() < 2) {
                                        critList = null;
                                        break;
                                    }

                                char[] a = strTerm.toCharArray(); // anagram
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

                                for (String word : current) {
                                    char[] anagram = word.toCharArray();
                                    if (databaseAccess.isAnagram(first, anagram, blankcount)) {
                                        wordsFound.add(word);
                                    }
                                    if (isCancelled()) {
                                        break;
                                    }

                                }
                                critList = wordsFound;
                                break;

                            case "Contains All": // Contains all (Superanagrams)
                                critList = databaseAccess.getList_superanagrams(term.getText().toString(), current);
                                break;
                            case "Contains Any": // Contains any
                                critList = databaseAccess.getList_containsAny(term.getText().toString(), current);
                                break;
                            case "Begins With": // Begins
                                critList = databaseAccess.getList_begins(term.getText().toString(), current);
                                break;
                            case "Ends With": // Ends
                                critList = databaseAccess.getList_ends(term.getText().toString(), current);
                                break;
                            case "Predefined":
                                alertMessage = "Criteria search " + critSearchType + " " + predef.getSelectedItem().toString();
                                critList = databaseAccess.getList_predefined(predef.getSelectedItemPosition(), context, current);
                                break;
                            case "Subject Lists":
                                alertMessage = "Criteria search " + critSearchType + " " + subj.getSelectedItem().toString();
                                critList = databaseAccess.getList_SubjectList(subj.getSelectedItem().toString(), current);
                                break;
                            case "In Lexicon":
                                if (otherlex.getSelectedItem().toString().equals("No Other Lexicons")) {
                                    alertMessage = "No Other Lexicons exist in the database";
                                    critList = null;
                                    break;
                                }

                                alertMessage = "Criteria search " + critSearchType + " " + otherlex.getSelectedItem().toString();
                                critList = databaseAccess.getList_inLexicon(otherlex.getSelectedItem().toString(), context, current);
                                break;

                            case "Takes Prefix":
                                critList = databaseAccess.getList_takesPrefix(term.getText().toString(), current);


//                                String pref = term.getText().toString();
////                                todo modify term before this
////                                term.setText(pref.replace("?", ""));
//                                pref.replace("?", "");
////                                critList = databaseAccess.getList_takesPrefix(term.getText().toString(),current);
//                                if (pref.trim().equals("")) {
//                                        critList = null;
//                                        break;
//                                    }
//
//                                // single letter just search for hooks
//                                if (pref.trim().length() == 1) {
//                                    String letter = pref.toLowerCase();
//                                    String hooks;
//                                    for(String word : current) {
//                                        searcher = databaseAccess.getCursor_findWord(word); // test word(index, index + length)
//                                        if (searcher.getCount() < 1){
//                                            searcher.close();
//                                            continue;
//                                        }
//                                        searcher.moveToFirst();
//                                        hooks = searcher.getString(searcher.getColumnIndex("FrontHooks"));
////                Log.d("Prefix", letter + "? " + hooks + " " + word);
//                                        if (hooks.contains(letter)) {
//                                            wordsFound.add(word);
//                                        }
//                                        searcher.close();
//                                        if (isCancelled()) {
//                                            break;
//                                        }
//
//                                    }
//                                }
//
//                                else {
//                                    String fhooked;
//                                    for(String word : current) {
//                                        fhooked = pref + word; // word without prefix
//                                        searcher = databaseAccess.getCursor_findWord(fhooked); // test word(index, index + length)
////                                        if (searcher.getCount() < 1) {
////                                            searcher.close();
////                                            continue;
////                                        }
////                                        Log.d("Prefix", fhooked + " " + word);
//                                        if (searcher.getCount() > 0) {
//                                            wordsFound.add(word);
//                                        }
//                                        searcher.close();
//                                        if (isCancelled()) {
//                                            break;
//                                        }
//                                    }
//                                }
//                                critList = wordsFound;


                                break;
                            case "Takes Suffix":

        critList = databaseAccess.getList_takesSuffix(term.getText().toString(), current);
//
//                                String suff = term.getText().toString();
////                                term.setText(suff.replace("?", ""));
//                                suff.replace("?", "");
////                                critList = databaseAccess.getList_takesSuffix(term.getText().toString(),current);
//
//
//                                if (suff.trim().equals("")) {
//                                        critList = null;
//                                        break;
//                                    }
//
//                                // single letter just search for hooks
//                                if (suff.trim().length() == 1) {
//                                    String letter = suff.toLowerCase();
//                                    String hooks;
//                                    for(String word : current) {
//                                        searcher = databaseAccess.getCursor_findWord(word); // test word(index, index + length)
//
//                                        // do i need this check (all fix methods)
//                                        if (searcher.getCount() < 1){
//                                            searcher.close();
//                                            continue;
//                                        }
//
//                                        searcher.moveToFirst();
//                                        hooks = searcher.getString(searcher.getColumnIndex("BackHooks"));
//
//                                        if (hooks.contains(letter)) {
//                                            wordsFound.add(word);
//                                        }
//                                        searcher.close();
//                                        if (isCancelled()) {
//                                            break;
//                                        }
//                                    }
//                                }
//
//                                else {
//                                    String bhooked;
//                                    int suffixSize = suff.length();
//                                    for(String word : current) {
//                                        bhooked = word + suff; // word with suffix
//
//                                        searcher = databaseAccess.getCursor_findWord(bhooked); // test word(index, index + length)
//                                        if (searcher.getCount() > 0) {
//                                            wordsFound.add(word);
//                                        }
//                                        if (isCancelled()) break;
//
//                                        searcher.close();
//                                    }
//                                }
//                                critList = wordsFound;
//

                                break;
                            case "Alt Ending ⌛":


                                // todo continue importing source code from databaseAccess
//                                critList = databaseAccess.getList_altEnding(term.getText().toString(),current);
                                // todo check that term includes ?


                                strTerm = term.getText().toString();
                                if (strTerm.trim().isEmpty()) {
                                    critList = null;
                                    break;
                                }

                                String altEnding = "";
                                String orgEnding = "";

                                if (!strTerm.contains("?"))
                                    return null;
                                altSet = strTerm.trim().replace("?", ">");
                                column = altSet.split(">");
                                orgEnding = column[0];
                                altEnding = column[1];

                                String lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());


                                altcursor = databaseAccess.rawQuery("SELECT Word \n" +
                                        "FROM     `" + LexData.getLexName() + "` \n" +
                                        "WHERE (" + "Word LIKE '%" + altEnding + "' " +
                                        lenFilter + " ) ");

                                alternates = new ArrayList<>();

                                while (altcursor.moveToNext()) {
                                    String word2find = altcursor.getString(0);
                                    alternates.add(word2find);
                                    //Log.e("Alt", word2find);
                                }
                                altcursor.close();

                                for(String orgWord : current) {
                                    if (!orgWord.endsWith(orgEnding))
                                        continue;

                                    String altWord = Utils.replaceLast(orgWord, orgEnding, altEnding);

                                    if (!alternates.contains(altWord))
                                        continue;
                                    //                            Log.e("Alt", altWord);

                                    searcher = databaseAccess.getCursor_findWord(altWord); // test word(index, index + length)
                                    if (searcher.getCount() > 0) {
                                        searcher.moveToFirst();
                                        wordsFound.add(orgWord);
                                    }
                                    searcher.close();

                                }
                                critList = wordsFound;



                                break;
                            case "Replace ⌛":
//                        if (!entry.contains("?"))
//                            return;
//                        if (entry.length()<3)
//                            return;F
//                        if (entry.charAt(0) == '?')
//                            return;
//                        if (entry.charAt(entry.length()-1) == '?')
//                            return;
//                                critList = databaseAccess.getList_replace(term.getText().toString(),current);


                                strTerm = term.getText().toString();
                                if (strTerm.trim().isEmpty()) {
                                    critList = null;
                                    break;
                                }

                                String altString = "";
                                String orgString = "";

                                if (!strTerm.contains("?"))
                                    return null;
                                altSet = strTerm.trim().replace("?", ">");
                                column = altSet.split(">");
                                orgString = column[0];
                                altString = column[1];

                                lenFilter = String.format(" AND Length(Word) <= %1$s", LexData.getMaxLength());

//        Cursor searcher;
//                                Cursor altcursor;

                                altcursor = databaseAccess.rawQuery("SELECT Word \n" +
                                        "FROM     `" + LexData.getLexName() + "` \n" +
                                        "WHERE (" + "Word LIKE '%" + altString + "%' " +
                                        lenFilter + " ) ");


                                alternates = new ArrayList<>();
                                while (altcursor.moveToNext()) {
                                    String word2find = altcursor.getString(0);
                                    alternates.add(word2find);
                                    //Log.e("Alt", word2find);
                                }
                                altcursor.close();

                                for(String orgWord : current) {
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
                                critList = wordsFound;



                            break;

                            // adjust case number
                            case "With Definition":
                                critList = databaseAccess.getList_withDef(term.getText().toString(), current);
                                break;


                            case "From File": // From File
                                alertMessage = "Criteria search " + critSearchType + " " + importfile.getText();
                                if (importfile.getText().toString().equals("File specification") ||
                                        importfile.getText().toString().isEmpty()) {
                                    critList = null;
                                    break;
                                }
                                // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF"))
                                if (Utils.usingSAF())
                                    critList = databaseAccess.getList_fromfile(getApplicationContext(), importfile.getPrivateImeOptions().toString(), current);
                                else
                                    critList = databaseAccess.getList_fromfile(getApplicationContext(), importfile.getText().toString(), current);
                                break;

                            default:
                                break;
                        }

                        if (current != null && critList != null)
                            if (notbox.isChecked())
                                current.removeAll(critList);
                            else
                                current.retainAll(critList);


                    }
                    if (current.isEmpty()) {
//                        Toast.makeText(context, "Search returned 0 words, remaining searches aborted.", Toast.LENGTH_LONG).show();
                        break;
                    }
                }



                // finished all searches
                complete = current;

                alertMessage = "Compiling final results";
                StringBuilder lookup = new StringBuilder();
                for(String s : complete)
                    lookup.append("'" + s + "', ");
                lookup.append( "'HYZERHYZER'"); // DUMMY WORD AFTER LAST COMMA
                cursor = databaseAccess.getCursor_getWords(lookup.toString(), ordering, limits, "");

               return null;
            }
            
            @Override
            protected void onPostExecute(Void result) {
                MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                        "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
                    displayResults();
                hidekeyboard();

                etTerm.setSelection(cursorPosition);
                lastMin = minimum.getSelectedItemPosition();
                lastMax = maximum.getSelectedItemPosition();
//                databaseAccess.close();

                if (dialog != null) {
                    dialog.dismiss();
                }
//        Cursor searcher;
//        for(String s : complete) {
//            //            Log.e("s", "-" + s + ".");
//            searcher = databaseAccess.getCursor_findWord(s); // test word(index, index + length)
//            if (searcher.getCount() > 0) {
//                searcher.moveToFirst();
//                matrixCursor.addRow(get_CursorRow(searcher));
//            }
//            searcher.close();
//        }
//        cursor = matrixCursor;


                // todo to sort, add to table and select using order
                // TRY TO CREATE "IN" TABLE AND IMPORT WITH SORT

            }

            @Override
            protected void onCancelled() {
                cancel(true);
                if (dialog != null) {
                    dialog.dismiss();
                }
//                cursor = matrixCursor; // un-uncommented
//                displayPartialResults();
                dialog.cancel();
            }
        };
        task.execute();

        
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


    public void getListWords(String word) {
        //        Toast.makeText(this,"Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();

        Intent intentBundle = new Intent(MultiSearchActivity.this, SubActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("term", word); // subsearch adds blank

        int subsearchtype = R.id.subjects;
        bundle.putInt("search", subsearchtype);
        //        bundle.putString("filters", makefilters(subsearchtype));
        bundle.putString("ordering", getSortOrder() );
        intentBundle.putExtras(bundle);

        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//

        startActivity(intentBundle);

        overridePendingTransition (0, 0);//

    }
    // started by selecting the item from the searchType
    //
    // importfile is a hidden control on screen
    public void displayResults() {
        lvHeader = findViewById(R.id.imcheader);

        if (etFilter.getText().length() > 0)
            executeFilter();

//        if (stems.getVisibility() == VISIBLE || subjects.getVisibility() == VISIBLE) {
//            cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.sclistitem, cursor, stemfrom, stemto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
//            lvHeader.setVisibility(GONE);
////            mcView = inflater.inflate(R.layout.sclistitem, null);
//        }

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
//        }

        ListView lv = findViewById(R.id.mcresults);
        lv.setAdapter(cursorAdapter);

        long lEndTime = System.nanoTime();
        double output = (lEndTime - lStartTime) / 1000000;

            if (cursor != null)
                status.setText("Found " + Integer.toString(cursor.getCount()) + " words from " + LexData.getLexName());
            else
                incompleteSearch();
        status.append("   " + String.format("%.2f seconds",output/1000));
        lastStatus = status.getText().toString();

//        hideKeyboard(this);
        hidekeyboard();
    }


    // Search Utilities
    private void setLimits() {
        // new value passing version
        if (mLimit.getText().toString().matches(""))
            limit = LexData.getMaxList();
        else
            limit = Integer.parseInt(mLimit.getText().toString());

        if (mOffset.getText().toString().matches(""))
            offset = 0;
        else
            offset = Integer.parseInt(mOffset.getText().toString());
        if (offset > 0)
            offset--; // zero based
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
            return "";

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
            case 15:
                builder.append("Random()");
                break;
            case 16: // unsorted
                break;
        }
        builder.append(", ");
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
    protected void executeFilter() {
//        if (etTerm.getVisibility() == VISIBLE)

        cursor = databaseAccess.getCursor_rackfilter(etFilter.getText().toString(), cursor, limit, offset);
        // don't filter stems, predefined, stems, subject lists

    }

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
            FileDialog fileDialog = new FileDialog(MultiSearchActivity.this, mPath, "txt");
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
    public void selectcritFile(TextView ffile) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOCUMENTS);
            startActivityForResult(intent, PICK_CRIT_FILE);

        } else {
            if (!databaseAccess.permission(this))
                return;
            File mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
            FileDialog fileDialog = new FileDialog(MultiSearchActivity.this, mPath, "txt");
            // only supports one file extension

            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    String full = file.getAbsolutePath();
                    ffile.setText(full);
                }
            });
            fileDialog.showDialog();
        }
    }


    public void registerEditText(int resid) {
        // Find the EditText 'resid'

        EditText edittext= (EditText)findViewById(resid);

        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (LexData.getCustomkeyboard()) {

                    switch (stype.getSelectedItem().toString()) {
                        case "Pattern ⌛":
                            mKeyboardView.setKeyboard(pKeyboard);
                            break;
                        case "With Definition":
                            mKeyboardView.setKeyboard(defKeyboard);
                            break;
                        default:
                            mKeyboardView.setKeyboard(bKeyboard);
                    }



//                    if (stype.getSelectedItem().equals("Pattern ⌛"))
//                        mKeyboardView.setKeyboard(pKeyboard); // because default type is pattern on open
//                    else
//                        mKeyboardView.setKeyboard(bKeyboard); // because default type is pattern on open
//                    if (stype.getSelectedItem().equals("With Definition"))
//                        mKeyboardView.setKeyboard(defKeyboard);
                }
                if( hasFocus ) showkeyboard(); else hidekeyboard();
            }
        });
        edittext.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
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
    @SuppressLint("ClickableViewAccessibility")
    public void registerCritEditText(EditText edittext) {

        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (LexData.getCustomkeyboard()) {
                    int currentCrit =  (int) Integer.parseInt(edittext.getTag().toString());
                    Spinner st = critView[currentCrit].findViewById(R.id.critSearchType);

                    switch (st.getSelectedItem().toString()) {
                        case "Pattern ⌛":
                            mKeyboardView.setKeyboard(pKeyboard);
                            break;
                        case "With Definition":
                            mKeyboardView.setKeyboard(defKeyboard);
                            break;
                        default:
                            mKeyboardView.setKeyboard(bKeyboard);
                            break;
                    }




//                    if (st.getSelectedItem().equals("Pattern ⌛"))
//                        mKeyboardView.setKeyboard( pKeyboard ); // because default type is pattern on open
//                    else
//                        mKeyboardView.setKeyboard( bKeyboard ); // because default type is pattern on open
//                    if (st.getSelectedItem().equals("With Definition"))
//                        mKeyboardView.setKeyboard(defKeyboard);
                }

                if( hasFocus ) showkeyboard(); else hidekeyboard();
            }
        });
        edittext.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
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

        Intent intentBundle = new Intent(MultiSearchActivity.this, ListSlidesActivity.class);
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

        Intent intentBundle = new Intent(MultiSearchActivity.this, ReviewActivity.class);
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

        Intent intentBundle = new Intent(MultiSearchActivity.this, AnagramQuizActivity.class);
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

        Intent intentBundle = new Intent(MultiSearchActivity.this, RecallQuizActivity.class);
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
        Intent intentBundle = new Intent(MultiSearchActivity.this, QuizActivity.class);

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
        Intent intent = new Intent(this, MultiSearchActivity.class);
        //        finishAffinity(); // deletes the stack
        startActivity(intent);
    }
    //******* END DUPLICATED IN SUBSEARCH *********


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
    public void hidekeyboard() {
        if (LexData.getCustomkeyboard())
            hideCustomKeyboard();
//        else {
        // always hide normal keyboard
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            );
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
//        }


//        if (etLimit.getVisibility() != GONE) {
//            getWindow().setSoftInputMode(
//                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
//            );
//            View view = this.getCurrentFocus();
//            if (view != null) {
//                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
//            }
//        }

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


    // KEYBOARD SUPPORT  called from onCreate, onResume, readPreferences, onOptionItemSelected
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

        // shows keyboard but doesn't accept input
        for(int i=0; i < numCriteria;i++) {
            registerCritEditText(critView[i].findViewById(R.id.critEntry));
//            registerEditText(critView[i].findViewById(R.id.critEntry).getId());
        }

        hideCustomKeyboard();
    }
    // keyboard watcher
    protected KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = MultiSearchActivity.this.getWindow().getCurrentFocus();
//            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
//            if( focusCurrent==null || focusCurrent.getClass()!= android.widget.EditText.class) return;
            if (focusCurrent == null) return;
            if (focusCurrent.getClass()!=android.widget.EditText.class)
                if (focusCurrent.getClass()!=AppCompatEditText.class)
                return;
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
    public void hideCustomKeyboard() {
        if (mKeyboardView != null) {
            mKeyboardView.setVisibility(View.GONE);
            mKeyboardView.setEnabled(false);
        }
    }
    public void showCustomKeyboard( View v ) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if( v!=null ) ((InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
    }
    protected void movelines() {

        // only check visibleCriteria
        // only one line should be moved
        for (int i = 0; i < numCriteria-1; i++) {
            if (critView[i].getVisibility() != VISIBLE)
                if (critView[i + 1].getVisibility() == VISIBLE) {
                    // copy i+1 to i
                    // make i+1 gone
                    // make i visible
                    copyCriteria(critView[i + 1], critView[i]);
                    critView[i].setVisibility(VISIBLE);
                    critView[i + 1].setVisibility(GONE);
                }
                else
                    clearCriteria(critView[i]);
        }
    }

    protected void copyCriteria(View from, View to){

        Spinner toSearch = (Spinner)to.findViewById(R.id.critSearchType);
        Spinner fromSearch = (Spinner)from.findViewById(R.id.critSearchType);
        toSearch.setSelection(fromSearch.getSelectedItemPosition());

        Spinner toMin = (Spinner)to.findViewById(R.id.critMin);
        Spinner fromMin = (Spinner)from.findViewById(R.id.critMin);
        toMin.setSelection(fromMin.getSelectedItemPosition());

        Spinner toMax = (Spinner)to.findViewById(R.id.critMax);
        Spinner fromMax = (Spinner)from.findViewById(R.id.critMax);
        toMax.setSelection(fromMax.getSelectedItemPosition());

        EditText toEntry = to.findViewById(R.id.critEntry);
        EditText fromEntry = from.findViewById(R.id.critEntry);
        toEntry.setText(fromEntry.getText());

        CheckBox toNotbox = to.findViewById(R.id.critNotBox);
        CheckBox fromNotbox = from.findViewById(R.id.critNotBox);
        toNotbox.setChecked(fromNotbox.isChecked());

    }


    // Adapters
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



    // Listeners
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
    private class GenericTextWatcher implements TextWatcher{

        private View view;
        private GenericTextWatcher(View view) {
            this.view = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        protected boolean mWasEdited = false;
        public void afterTextChanged(Editable editable) {

            if (mWasEdited){
                mWasEdited = false;
                return;
            }
            mWasEdited = true;
            String enteredValue  = editable.toString();
            EditText et;
            Spinner st;

            // this sets the control to watch
            switch(view.getId()){
                case R.id.etEntry:
                    // process normally
                    et = findViewById(R.id.etEntry);
                    st = findViewById(R.id.searchType);
                    break;
                default:
//                    ConstraintLayout line = (ConstraintLayout) ((ViewGroup) view.getParent()).getParent();
                    ConstraintLayout line = (ConstraintLayout) ((ViewGroup) view.getParent());
                    currentCrit = (int) Integer.parseInt(line.getTag().toString());

                    et = critView[currentCrit].findViewById(R.id.critEntry);
                    st = critView[currentCrit].findViewById(R.id.critSearchType);

                    break;
            }

            int caret = et.getSelectionStart();
//            if (st.getSelectedItemPosition() != 3) { // not pattern


            switch (st.getSelectedItem().toString()) {
                case "Pattern ⌛":
                    StringBuilder modifiedValue = new StringBuilder(enteredValue);
                    for (int i = 0; i < modifiedValue.length(); i++) {
                        if (Character.isLowerCase(modifiedValue.charAt(i)))
                            if (!(modifiedValue.charAt(i) == 'c' || modifiedValue.charAt(i) == 'v'))
                                modifiedValue.setCharAt(i, Character.toUpperCase(modifiedValue.charAt(i)));
                    }
                    et.setText(modifiedValue);
                    et.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid
                    break;
                case "With Definition":
                    break;
                default:
                    enteredValue = enteredValue.toUpperCase();
                    String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}cv0123456789.,^+~-]", "");
                    if (!st.getSelectedItem().toString().equals("Anagrams")) {
                        newValue = newValue.replaceAll("[*@]", "");
                    }
                    if (Arrays.asList("Hook Words", "Contains", "Contains All", "Contains Any", "Begins With", "Ends With").contains(stype.getSelectedItem().toString())) {// hooks, begins, ends
                        newValue = newValue.replaceAll("[?]", "");
                    }
                    et.setText(newValue);
                    et.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
                    break;
            }



//            if (!st.getSelectedItem().toString().equals("Pattern ⌛")) {
//                enteredValue = enteredValue.toUpperCase();
//                //^+[]&lt;&gt;(|){}\\~-
//                String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}cv0123456789.,^+~-]", "");
////                String newValue = enteredValue.replaceAll("[\\[\\]<>cv0123456789.,^+~-]", "");
//
//                if (!st.getSelectedItem().toString().equals("Anagrams")) {
////                if (stype.getSelectedItemPosition() != 0 && // not anagram
////                        stype.getSelectedItemPosition() != 3) { // and not pattern
//                    newValue = newValue.replaceAll("[*@]", "");
//                }
//
//
////                if (st.getSelectedItemPosition() != 0 && // not anagram
////                        st.getSelectedItemPosition() != 3) { // and not pattern
////                    newValue = newValue.replaceAll("[*@]", "");
////                }
//
//
////                if (Arrays.asList(2,7,8).contains(st.getSelectedItemPosition())) {// hooks, begins, ends
////                    newValue = newValue.replaceAll("[?]", "");
////                }
//
//                if (Arrays.asList("Hook Words","Contains", "Contains All","Contains Any","Begins With","Ends With").contains(st.getSelectedItem().toString())) {// hooks, begins, ends
//                    newValue = newValue.replaceAll("[?]", "");
//                }
//
//                et.setText(newValue);
//                et.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
//                return;
//            }
//            else {
//                StringBuilder modifiedValue = new StringBuilder(enteredValue);
//                for (int i = 0; i < modifiedValue.length(); i++) {
//                    if (Character.isLowerCase(modifiedValue.charAt(i)))
//                        if (!(modifiedValue.charAt(i) == 'c' || modifiedValue.charAt(i) == 'v'))
//
//                            modifiedValue.setCharAt(i, Character.toUpperCase(modifiedValue.charAt(i)));
//                }
//                et.setText(modifiedValue);
//                et.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid
//            }


//            String text = editable.toString();
//            switch(view.getId()){
//                case R.id.name:
//                    model.setName(text);
//                    break;
//                case R.id.email:
//                    model.setEmail(text);
//                    break;
//                case R.id.phone:
//                    model.setPhone(text);
//                    break;
//            }
        }
    }
//todo
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


            switch (stype.getSelectedItem().toString()) {
                case "Pattern ⌛":
                    StringBuilder modifiedValue = new StringBuilder(enteredValue);
                    for (int i = 0; i < modifiedValue.length(); i++) {
                        if (Character.isLowerCase(modifiedValue.charAt(i)))
                            if (!(modifiedValue.charAt(i) == 'c' || modifiedValue.charAt(i) == 'v'))
                                modifiedValue.setCharAt(i, Character.toUpperCase(modifiedValue.charAt(i)));
                    }
                    etTerm.setText(modifiedValue);
                    etTerm.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid
                    break;
                case "With Definition":
                    break;
                default:
                    enteredValue = enteredValue.toUpperCase();
                    String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}cv0123456789.,^+~-]", "");
                    if (!stype.getSelectedItem().toString().equals("Anagrams")) {
                        newValue = newValue.replaceAll("[*@]", "");
                    }
                    if (Arrays.asList("Hook Words", "Contains", "Contains All", "Contains Any", "Begins With", "Ends With").contains(stype.getSelectedItem().toString())) {// hooks, begins, ends
                        newValue = newValue.replaceAll("[?]", "");
                    }
                    etTerm.setText(newValue);
                    etTerm.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
                    break;
            }


//            if (!stype.getSelectedItem().toString().equals("Pattern ⌛")) {
//                enteredValue = enteredValue.toUpperCase();
//                String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}cv0123456789.,^+~-]", "");
//
//                if (!stype.getSelectedItem().toString().equals("Anagrams")) {
//                    newValue = newValue.replaceAll("[*@]", "");
//                }
//
//                if (Arrays.asList("Hook Words","Contains", "Contains All","Contains Any","Begins With","Ends With").contains(stype.getSelectedItem().toString())) {// hooks, begins, ends
//                    newValue = newValue.replaceAll("[?]", "");
//                }
//
//                etTerm.setText(newValue);
//                etTerm.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
//                return;
//            }
//            else {
//                StringBuilder modifiedValue = new StringBuilder(enteredValue);
//                for (int i = 0; i < modifiedValue.length(); i++) {
//                    if (Character.isLowerCase(modifiedValue.charAt(i)))
//                        if (!(modifiedValue.charAt(i) == 'c' || modifiedValue.charAt(i) == 'v'))
//
//                            modifiedValue.setCharAt(i, Character.toUpperCase(modifiedValue.charAt(i)));
//                }
//                etTerm.setText(modifiedValue);
//                etTerm.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid
//            }


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

    protected final AdapterView.OnItemSelectedListener predefined = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // cursor = getPredefined(predef.getSelectedItemPosition());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener critPredefined = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // cursor = getPredefined(predef.getSelectedItemPosition());
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener critSubject = new AdapterView.OnItemSelectedListener() {
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
    protected OnClickListener critBlank = new OnClickListener() {
        public void onClick(View v) {
            ConstraintLayout line = (ConstraintLayout) ((ViewGroup) v.getParent());
            currentCrit = (int) Integer.parseInt(line.getTag().toString());

            EditText et = critView[currentCrit].findViewById(R.id.critEntry);

            int start = Math.max(et.getSelectionStart(), 0);
            int end = Math.max(et.getSelectionEnd(), 0);
            et.getText().replace(Math.min(start, end), Math.max(start, end),
                    "?", 0, 1);    // "?".length()
        }
    };
    protected OnClickListener addLine = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (visibleCriteria < numCriteria) {
                for (int i = 0; i < numCriteria; i++) {
                    if (critView[i].getVisibility() != VISIBLE) {
//                        populateCriteria(critView[i]);
                        critView[i].setVisibility(VISIBLE);
                        break;
                    }
                }

//                populateCriteria(critView[visibleCriteria]);
//                critView[visibleCriteria].setTag(0, visibleCriteria);
                visibleCriteria++;
                if (visibleCriteria == 7)
                    addCriteria.setVisibility(GONE);
            }
        }
    };
    protected OnClickListener critDelete = new OnClickListener() {
        public void onClick(View v) {

//            return;

            ConstraintLayout line = (ConstraintLayout) ((ViewGroup) v.getParent());
            line.setVisibility(GONE);
            visibleCriteria--;
            addCriteria.setVisibility(VISIBLE);
            if (visibleCriteria > 0)
                movelines();
        }
    };
    protected OnClickListener critClearTerm = new OnClickListener() {
        public void onClick(View v) {
            ConstraintLayout line = (ConstraintLayout) ((ViewGroup) v.getParent());
            currentCrit = (int) Integer.parseInt(line.getTag().toString());

            EditText editText = critView[currentCrit].findViewById(R.id.critEntry);

            editText.setText("");
            editText.requestFocus();
            showkeyboard();
        }
    };
    protected OnClickListener critNewFile = new OnClickListener() {
        public void onClick(View v) {

            ConstraintLayout line = (ConstraintLayout) ((ViewGroup) v.getParent());
            currentCrit = (int) Integer.parseInt(line.getTag().toString());

            TextView filespec = critView[currentCrit].findViewById(R.id.critFile);

            selectcritFile(filespec);
        }
    };

    protected OnClickListener showHelpGuide = new OnClickListener() {
        public void onClick(View v) {


        }
    };
    protected OnClickListener doSearch = new OnClickListener() {
        public void onClick(View v) {
            executeSearch();
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
            selectFile();
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
            PopupMenu popup = new PopupMenu(MultiSearchActivity.this, v);
            popup.setOnMenuItemClickListener(MultiSearchActivity.this);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_quiz, popup.getMenu());
            popup.show();
        }
    };
    protected OnClickListener doQuizReview = new OnClickListener() {
        @Override
        public void onClick(View v) {
            PopupMenu popup = new PopupMenu(MultiSearchActivity.this, v);
            popup.setOnMenuItemClickListener(MultiSearchActivity.this);
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

            for (int i = 0; i < numCriteria; i++)
                clearCriteria(critView[i]);

//            begins.setSelection(0);
//            ends.setSelection(0);
            sortby.setSelection(0);
            thenby.setSelection(0);
            etFilter.setText("");
            mLimit.setText("");
            mOffset.setText("");
            lastStatus = "";

            if (LexData.getLexicon().LexiconNotice.isEmpty())
                status.setText((LexData.getLexName()));
            else
                status.setText(LexData.getLexicon().LexiconNotice);

//            specStatus.setText("Other Specifications");
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
    public void getStemWords(String word) {
        Toast.makeText(this,"Please Wait!\r\nThis may take a minute...", Toast.LENGTH_LONG).show();

        Intent intentBundle = new Intent(MultiSearchActivity.this, SubActivity.class);
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
    public void displayPartialResults() {
        //ConstraintLayout
        lvHeader = findViewById(R.id.imcheader);

        if (etFilter.getText().length() > 0)
            executeFilter();

//        if (stems.getVisibility() == VISIBLE || subjects.getVisibility() == VISIBLE) {
//            cursorAdapter = new MCListAdapter(getBaseContext(), R.layout.sclistitem, cursor, stemfrom, stemto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
//            lvHeader.setVisibility(GONE);
//        }
//        else{
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
//        }

        ListView lv = findViewById(R.id.mcresults);
        lv.setAdapter(cursorAdapter);

        long lEndTime = System.nanoTime();
        double output = (double) (lEndTime - lStartTime) / 1000000;

        status.setText("Found " + Integer.toString(cursor.getCount()) + " words (partial) from " + LexData.getLexName());
        status.append("   " + String.format("%.2f seconds",output/1000));

//        hideKeyboard(this);
        hidekeyboard();
    }
    public String makeDesc() {
        StringBuilder sb = new StringBuilder();
        int min = minimum.getSelectedItemPosition() + 1;

//        Spinner maximum = findViewById(R.id.MaxLength);
        int max = maximum.getSelectedItemPosition() + 1;

        // String term = etTerm.getText().toString();
//        String beginning = begins.getSelectedItem().toString();
//        String ending = ends.getSelectedItem().toString();
        if (minimum.getSelectedItemPosition() > 0 ||
                maximum.getSelectedItemPosition() > 0)
            sb.append(" Len:" + min + "-" + max + " ");
//        if (begins.getSelectedItemPosition() > 0)
//            sb.append("Beg:" + beginning + " ");
//        if (ends.getSelectedItemPosition() > 0)
//            sb.append("End:" + ending + " ");
        return sb.toString();
    }
    public String makeParallelFilters(int searchType) {
        List<String> filters = new ArrayList<String>();

        Spinner sp = findViewById(R.id.MinLength);
        int minimum = sp.getSelectedItemPosition() + 1;

        Spinner max = findViewById(R.id.MaxLength);
        int maximum = max.getSelectedItemPosition() + 1;

        // String term = etTerm.getText().toString();
//        String beginning = begins.getSelectedItem().toString();
//        String ending = ends.getSelectedItem().toString();

        if (sp.getSelectedItemPosition() != 0)
            filters.add(" Length(Word) >= " + String.valueOf(minimum) + " " +
                    " AND Length(Word) <= " + String.valueOf(maximum) + " ");
        else
            filters.add(" (Length(Word) >= " + (etTerm.length() - 1) +
                    " AND Length(Word) <= " + (etTerm.length() + 1) + ") ");
//        if (begins.getSelectedItemPosition() != 0)
//            filters.add(" Word LIKE '" + beginning + "%' ");
//        if (ends.getSelectedItemPosition() != 0)
//            filters.add(" Word LIKE '%" + ending + "' ");

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
        return SQLfilter;
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
/*
    protected OnClickListener doAnagramSlides = new OnClickListener() {
        @Override
        public void onClick(View v) {
            startAnagramSlides();
        }
    };
    public void startAnagramSlides() {
        if (listView.getCount() < 1) {
            Toast.makeText(this, "Complete a search before showing slides", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intentBundle = new Intent(SearchActivity.this, ReviewActivity.class);

        String[] words = new String[cursor.getCount()];
        if (cursor.getCount() > 10000) {
            Toast.makeText(this, "Over the limit!\r\nPlease limit slides to 5000 words...", Toast.LENGTH_LONG).show();
            return;
        }

        // extract words from cursor
        cursor.moveToFirst();
        int counter = 0;
        int column = cursor.getColumnIndex("Word");
        words[counter] = cursor.getString(column);
        while(cursor.moveToNext()){
            counter++;
            words[counter] = cursor.getString(column);
        }

        // extract anagrams from words
        String[] anagrams = databaseAccess.getAnagrams(words);

        Bundle bundle =new Bundle();
        bundle.putStringArray("Words",anagrams);

        if (etTerm.getVisibility() == VISIBLE)
            bundle.putString("term", etTerm.getText().toString());
        else
            bundle.putString("term", " ");

        // this time use the form search type
        bundle.putString("search", stype.getSelectedItem().toString());
        bundle.putString("desc", searchParameters.toString());
        intentBundle.putExtras(bundle);

        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);
    }

 */