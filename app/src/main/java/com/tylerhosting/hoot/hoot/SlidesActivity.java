package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.view.View.GONE;

public abstract class SlidesActivity extends AppCompatActivity {

    SharedPreferences shared;
    SharedPreferences.Editor prefs;

    int currentID = 0;
    int matchColor;
    int listfontsize = 24;
    String themeName;
    TextView header, tvalpha, word, definition, status, textSeconds, incorrect, answerCount;
    TextView answer; // changed by buttons
    EditText etSeconds;
    EditText entry; // for typed answers
    ListView lv;

    ImageButton first, previous, next, last, start, stop, quit;
    ImageButton next2, review, shuffle, alpha, correct, wrong;
    ImageButton aligner;

    Spinner selector;
    LinearLayout buttonlayout;
    View sv;
    AssetManager assetManager;
    DatabaseAccess databaseAccess;
    Typeface tile, listfont;

    //    Button first, previous, next, last, start, stop, quit;
    SpannableString[] wordlist;
    // String[] wordlist;
    SimpleCursorAdapter cursorAdapter;
    MatrixCursor matrixCursor;
    String[] from = new String[]{"FrontHooks","InnerFront", "Word", "InnerBack", "BackHooks"};
    int[] to = new int[] { R.id.fh, R.id.ifh, R.id.word, R.id.ibh, R.id.bh};
    String[] plainfrom = new String[]{"Word"};
    int[] plainto = new int[] { R.id.word};


    ArrayAdapter<String> adapter;
    List<String> anagramList = new ArrayList<>();
    List<String> answerList = new ArrayList<>();
    List<String> incorrectList = new ArrayList<>();

    boolean flashcards;
    boolean landscape;
    boolean dragger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = PreferenceManager.getDefaultSharedPreferences(this);
        prefs = shared.edit();

        themeName = Utils.setStartTheme(this);

        int orientation = getResources().getConfiguration().orientation;
        landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;

        if (getScreenHeight() < 500 || landscape)
            setContentView(R.layout.content_slides);
        else {
            setContentView(R.layout.activity_slides);
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            try {
                getSupportActionBar().setIcon(R.mipmap.howl);
            } catch (Exception e) {

            }
            String version = com.tylerhosting.hoot.hoot.BuildConfig.VERSION_NAME;
            String suffix = com.tylerhosting.hoot.hoot.BuildConfig.BUILD_TYPE;
            int code = com.tylerhosting.hoot.hoot.BuildConfig.VERSION_CODE;
            toolbar.setTitle("Hoot" + " " + version);
        }


        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        populateResources();



        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView wrd = view.findViewById(R.id.word);
                String selectedWord = wrd.getText().toString().toUpperCase();
                            databaseAccess.open();
                            Utils.wordDefinition(lv.getContext(), selectedWord, databaseAccess.getDefinition(selectedWord));
                            databaseAccess.close();
                    return;
                }
        });



        loadBundle();
        showSlides();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // These are things that need to be changed if returning from Settings
        if (Utils.themeChanged(themeName, this)) {
            Utils.setNewTheme(this);
            recreate();
        }
        readPreferences();
//        word.setTextColor(matchColor);
        word.setTextColor(LexData.getTileColor(this));
        etSeconds.setTextColor(matchColor);
        incorrect.setVisibility(GONE);
    }

    @Override
    protected void onPause() {

        super.onPause();
        if (running) {
            running = false;
            timer.cancel();
            start.setImageResource(R.drawable.ic_start_dark);
        }
    }

    protected void loadBundle() {

    }

    // Support custom keyboard
    KeyboardView mKeyboardView;
    public final static int CodeDelete   = -5; // Keyboard.KEYCODE_DELETE
    public final static int CodeSearch = 55007;
    public final static int CodeNewSearch = 55008;
    public final static int CodeDone = Keyboard.KEYCODE_DONE;
    //    Keyboard bKeyboard; // basic
//    Keyboard pKeyboard; // pattern
    public Keyboard qKeyboard; // quiz
    boolean keyboardChanged = false;
    Boolean running = false;
    public void hidekeyboard() {
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private void populateResources() {


        header = findViewById(R.id.header);
        buttonlayout = (LinearLayout) findViewById(R.id.letterbuttons);

        word = findViewById(R.id.slideTextView);
        assetManager = getAssets();
        tile = Typeface.createFromAsset(assetManager, "fonts/nutiles.ttf");
        word.setTypeface(tile);
        listfont = Typeface.createFromAsset(assetManager, "fonts/ubuntumono.ttf");

        lv = findViewById(R.id.lv);
        sv = findViewById(R.id.swipepanel);

        definition = findViewById(R.id.tvDefinitions);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            definition.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }


        definition.setTextSize(16);
        definition.setMovementMethod(new ScrollingMovementMethod());


        definition.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                next.performClick();
                // Whatever
            }

            @Override
            public void onSwipeRight() {
                previous.performClick();
            }
        });


        incorrect = findViewById(R.id.incorrect);

        selector = findViewById(R.id.selector);

        first = findViewById(R.id.btnFirst);
        first.setOnClickListener(goFirst);
        previous = findViewById(R.id.btnPrevious);
        previous.setOnClickListener(goPrevious);
        next = findViewById(R.id.btnNext);
        next.setOnClickListener(goNext);
        last = findViewById(R.id.btnLast);
        last.setOnClickListener(goLast);

        start = findViewById(R.id.btnStart);
        start.setOnClickListener(startTimer);
        stop = findViewById(R.id.btnStop);
        stop.setOnClickListener(stopTimer);

        textSeconds = findViewById(R.id.tvseconds);

        etSeconds = findViewById(R.id.etSeconds);
        etSeconds.addTextChangedListener(secondsWatcher);
        etSeconds.setOnEditorActionListener(secondsAction);



        quit = findViewById(R.id.btnQuit);
        quit.setOnClickListener(goBack);

        status = findViewById(R.id.lblStatus);
        answerCount = findViewById(R.id.answerCount);


        answer = findViewById(R.id.answer);
        next2 = findViewById(R.id.btnNext2);
        review = findViewById(R.id.btnReview);
        shuffle = findViewById(R.id.btnShuffle);
        alpha = findViewById(R.id.btnAlpha);
        entry = findViewById(R.id.entry);

        correct = findViewById(R.id.btnRight);
        correct.setVisibility(GONE);
        wrong = findViewById(R.id.btnWrong);
        wrong.setVisibility(GONE);


        // hide controls used by quiz
        next2.setVisibility(View.GONE);
        review.setVisibility(GONE);
        shuffle.setVisibility(GONE);
        alpha.setVisibility(GONE);
//        answer.setVisibility(View.INVISIBLE);
        entry.setVisibility(GONE);

    }
    private void readPreferences() {
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefs = shared.edit();

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


        Boolean hooks = shared.getBoolean("hooks",true);
        if (hooks != LexData.getShowHooks())
            LexData.setShowHooks(hooks);

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

        String fs = shared.getString("listfont", "24");
        if (Utils.isParsable(fs))
            listfontsize = Integer.parseInt(shared.getString("listfont","24"));
        else {
            prefs.putString("listfont", "24");
            prefs.apply();
            listfontsize = 24;
        }

        flashcards = shared.getBoolean("flashcards", false);

        Boolean AutoAdvance = shared.getBoolean("autoadvance",false);
        if (AutoAdvance != LexData.getAutoAdvance())
            LexData.setAutoAdvance(AutoAdvance);

        Boolean loop = shared.getBoolean("loop",true);
        if (loop != LexData.getSlideLoop())
            LexData.setSlideLoop(loop);

        String themeName = shared.getString("theme", "Light Theme");
        switch (themeName) {
            case "Dark Theme":
                matchColor = Color.WHITE; // Color.YELLOW;
                break;
            default:
            case "Light Theme":
                matchColor = Color.BLACK; // Color.BLUE;
                break;
        }

        Boolean screenOn = shared.getBoolean("screenon", false);
        if (screenOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int tc = shared.getInt("tilecolor", 0xffffffff);
        LexData.setTileColor(tc);

    }
    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }
    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }
    public void showSlides() {

    }
    public void shorttoastMsg(String msg) {

        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();

    }
    public void longtoastMsg(String msg) {

        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.show();

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
            if (!Utils.permission(this)) {
                Toast.makeText(this, "Hoot doesn't have permission to write to storage", Toast.LENGTH_LONG).show();
                return false;
            }

            Intent intent = new Intent(this, CardBoxActivity.class);
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

            // hidekeyboard();
            openSettings();
            readPreferences();

            if (myKeyboard != LexData.getCustomkeyboard())
                recreate();

        }


        if (id == R.id.activity_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.about_activity) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_tools) {
            openTools();
            return true;
        }

        if (id == R.id.exit) {
            Utils.exitAlert(this);
        }

        return super.onOptionsItemSelected(item);
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


    // LISTENERS
    private View.OnClickListener goFirst = new View.OnClickListener() {
        public void onClick(View v) {
            currentID = 0;
            word.setText(wordlist[currentID]);
        }
    };
    private View.OnClickListener goPrevious = new View.OnClickListener() {
        public void onClick(View v) {
            if (currentID != 0)
                currentID = currentID - 1;
            word.setText(wordlist[currentID]);
        }
    };
    protected View.OnClickListener goNext = new View.OnClickListener() {
        public void onClick(View v) {
            if (currentID < wordlist.length - 1)
                currentID = currentID + 1;
            else {
                if (LexData.slideLoop)
                    currentID = 0; /// if rotating
                longtoastMsg("End of List");
            }
            word.setText(wordlist[currentID]);
            incorrect.setText("");
        }
    };
    private View.OnClickListener goLast = new View.OnClickListener() {
        public void onClick(View v) {
            currentID = wordlist.length - 1;
            word.setText(wordlist[currentID]);
        }
    };

    private View.OnClickListener startTimer = new View.OnClickListener() {
        public void onClick(View v) {

            hidekeyboard();

//            currentID++;
            if (running) {
                running = false;
                timer.cancel();
                start.setImageResource(R.drawable.ic_start_dark);
                return;
            }
            running = true;
            if (currentID == wordlist.length -1) {
                currentID = 0;
                word.setText(wordlist[currentID]);
            }

            start.setImageResource(R.drawable.ic_stop_dark);

            if (etSeconds.length() < 1)
                etSeconds.setText("1");

            // adding one millisecond to handle requests with 0 pause
            timer = new CountDownTimer(10000 * 10000 + 1, (1000 * Integer.parseInt(etSeconds.getText().toString())) + 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (currentID < wordlist.length - 1)
                            next.performClick();
                    else {
                        if (LexData.slideLoop)  next.performClick(); // Loops
                        else {
                            timer.cancel();
                            running = false;
                            start.setImageResource(R.drawable.ic_start_dark);
                        }
                    }
                }

                @Override
                public void onFinish() {
                    running = false;
                    start.setImageResource(R.drawable.ic_start_dark);
                }
            };
            timer.start();
        }
    };
    private View.OnClickListener stopTimer = new View.OnClickListener() {
        public void onClick(View v) {
            timer.cancel();
            start.setImageResource(R.drawable.ic_start_dark);
        }
    };
    private View.OnClickListener goBack = new View.OnClickListener() {
        public void onClick(View v) {
            onBackPressed();
        }
    };
    protected final AdapterView.OnItemSelectedListener selection = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            currentID = selector.getSelectedItemPosition();
            word.setText(wordlist[currentID]);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    private final TextWatcher secondsWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private boolean mWasEdited = false;
        @Override
        public void afterTextChanged(Editable s) {
            if (mWasEdited) {
                mWasEdited = false;
                return;
            }
            mWasEdited = true;

            int caret = etSeconds.getSelectionStart();

            String enteredValue  = s.toString();
            StringBuilder modifiedValue = new StringBuilder(enteredValue);
            etSeconds.setText(modifiedValue);
            etSeconds.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid

            return;
        }
    };
    protected final TextWatcher entryWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private boolean mWasEdited = false;
        @Override
        public void afterTextChanged(Editable s) {
            if (mWasEdited) {
                mWasEdited = false;
                return;
            }
            mWasEdited = true;
            entry.setText(s.toString());
            if (s.length()>0 && s.subSequence(s.length()-1, s.length()).toString().equalsIgnoreCase("\n")) {

                //enter pressed
            }



            return;

        }
    };
    private final TextView.OnEditorActionListener secondsAction = new TextView.OnEditorActionListener() {

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            return false;
        }
    };
    /**
     * Detects left and right swipes across a view.
     */
    public class OnSwipeTouchListener implements View.OnTouchListener {

        private final GestureDetector gestureDetector;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }
    public class SlidesListAdapter extends SimpleCursorAdapter {
        private Context mContext;
        private int id;
        private List<String> items;

        public SlidesListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
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


            // IF LOWER SET TO RED, ELSE GREEN
            TextView wordview = v.findViewById(R.id.word);
            if (wordview != null) {
                wordview.setTextSize(listfontsize);

                boolean hasBlanks = false;
                StringBuilder red = new StringBuilder();
                for (int index = 0; index < wordCol.length(); index++) {
                    char letter = wordCol.charAt(index);
                    if (Character.isLowerCase(letter)) {
                        hasBlanks = true;

                        switch (themeName) {
                            case "Dark Theme":
                                red.append("<font color='#ff0000'>" + (char) (letter - 32) + "</font>");
                                break;
                            case "Light Theme":
                            default:
                                red.append("<font color='#cc4444'>" + (char) (letter - 32) + "</font>");
                                break;
                        }

                    } else
                        red.append(wordCol.charAt(index));
                }
                Log.i("Word", wordCol + ": " + red);

                if (hasBlanks) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wordview.setText(Html.fromHtml(red.toString(), Html.FROM_HTML_MODE_LEGACY));
                    } else
                        wordview.setText(Html.fromHtml(red.toString()));
                } else {
                    StringBuilder green = new StringBuilder();
                    switch (themeName) {
                        case "Dark Theme":
                            green.append("<font color='#00ff00'>" + wordCol + "</font>");
                            break;
                        case "Light Theme":
                        default:
                            green.append("<font color='#22aa22'>" + wordCol + "</font>"); //44CC44
                            break;

                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wordview.setText(Html.fromHtml(green.toString(), Html.FROM_HTML_MODE_LEGACY));
                    } else
                        wordview.setText(Html.fromHtml(green.toString()));
//                        wordview.setText(wordCol);
                }
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

    CountDownTimer timer =  new CountDownTimer(60*60000, 5000) {

        public void onTick(long millisUntilFinished) {
            if (currentID < wordlist.length - 1)
                next.performClick();
            else {
                if (LexData.slideLoop)  next.performClick(); // Loops
                else  timer.cancel();
            }
        }

        public void onFinish() {

        }
    };

    @Override
    public void onBackPressed() {
        finish();
        timer.cancel();
        super.onBackPressed();
    }
}