package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static android.view.View.GONE;
import static com.tylerhosting.hoot.hoot.CardDatabase.getCard;
import static com.tylerhosting.hoot.hoot.CardUtils.nextDate;
import static com.tylerhosting.hoot.hoot.CardUtils.programData;
import static com.tylerhosting.hoot.hoot.CardUtils.today;
import static com.tylerhosting.hoot.hoot.CardUtils.unixDate;

public class CardBoxActivity extends AppCompatActivity {

    CardDatabase cardDatabase;
    public Cursor cursor;
    private SQLiteDatabase cards;

    int lastList;
    int lastbox;
    TextView test, listtype, listDeleter, boxDeleter, cardboxlexicon, cbhelp, importfile;;
    DatePicker startdate;
    Spinner cardtype, cardlist, searchType, minLength, maxLength;
    RelativeLayout cardlistlayout;
    Button beginQuiz, beginReview, mergeCard;
    String themeName;
    CheckBox chkBox0, chkFilter, chkRandom, deleteWhenDone;
    EditText wordcount, etEntry;
    ListView cardboxes, cardschedule;

    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.US);
    ArrayAdapter listAdapter;

    DatabaseAccess databaseAccess;

    // Support custom keyboard
    Keyboard bKeyboard; // basic
    Keyboard pKeyboard; // pattern
    KeyboardView mKeyboardView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeName = Utils.setStartTheme(this);
        setContentView(R.layout.activity_cardbox);

        cardboxlexicon = findViewById(R.id.cardboxlexicon);

        startdate = findViewById(R.id.DateStart);

        test = findViewById(R.id.CardItems);

        chkBox0 = findViewById(R.id.chk0First);
        chkRandom = findViewById(R.id.chkRandom);

        wordcount = findViewById(R.id.wordCount);

        cardtype = findViewById(R.id.CardTypeSpinner);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                R.layout.spinselection, LexData.cardtypes);
        cardtype.setAdapter(dataAdapter);

        cardtype.setOnItemSelectedListener(boxChange);

        cardlist = findViewById(R.id.ListNameSpinner);
        cardlistlayout = findViewById(R.id.ListName);
        cardlist.setOnItemSelectedListener(listChange);

        listDeleter = findViewById(R.id.DeleteList);
        listDeleter.setOnClickListener(deleteClick);

        boxDeleter = findViewById(R.id.deleteCardbox);
        boxDeleter.setOnClickListener(deleteCBClick);

        listtype = findViewById(R.id.lqtype);

        cbhelp = findViewById(R.id.cardboxhelp);
        cbhelp.setOnClickListener(cardboxhelp);

        beginQuiz = findViewById(R.id.btnBeginQuiz);
        beginQuiz.setOnClickListener(beginClick);

        beginReview = findViewById(R.id.btnReviewCards);
        beginReview.setOnClickListener(reviewClick);

        cardboxes = findViewById(R.id.CardBoxes);
        cardschedule = findViewById(R.id.CardSchedule);

        mergeCard = findViewById(R.id.mergeIntoBox);
        mergeCard.setOnClickListener(beginMerge);

        deleteWhenDone = findViewById(R.id.deleteList);




        chkFilter = findViewById(R.id.chkFilter);
        searchType = findViewById(R.id.searchType);
        minLength = findViewById(R.id.minLength);
        maxLength = findViewById(R.id.maxLength);
        etEntry = findViewById(R.id.etEntry2);
        etEntry.addTextChangedListener(entryWatcher);
//        etEntry.setOnEditorActionListener(entryAction);

        importfile = findViewById(R.id.tvImportFile);
        importfile.setOnClickListener(newFile);



        ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.cardFilterText);

        searchType.setAdapter(searchAdapter);
        searchType.setOnItemSelectedListener(typeConfig);

        pKeyboard = new Keyboard(CardBoxActivity.this,R.xml.patternkeyboard);
        bKeyboard = new Keyboard(CardBoxActivity.this,R.xml.filterkeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
        mKeyboardView.setKeyboard( bKeyboard );

        hidekeyboard();

        hideCustomKeyboard();

//        if (BuildConfig.BUILD_TYPE == "beta") {
            showFilter();
//        }


        loadMinimum();
        loadMaximum();

        // used by filter
        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

        databaseAccess.open();
        wordcount.requestFocus();

    }

    @Override
    protected void onResume(){
        super.onResume();

        // Enable fast scroll only if preference set
        if (LexData.getFastScroll()) {
            cardboxes.setFastScrollAlwaysVisible(true);
            cardboxes.setFastScrollEnabled(true);
            cardschedule.setFastScrollAlwaysVisible(true);
            cardschedule.setFastScrollEnabled(true);
        }
        else {
            cardboxes.setFastScrollAlwaysVisible(false);
            cardboxes.setFastScrollEnabled(false);
            cardschedule.setFastScrollAlwaysVisible(false);
            cardschedule.setFastScrollEnabled(false);
        }

        if (Utils.themeChanged(themeName, this)) {
            Utils.setNewTheme(this);
            recreate();
        }
        if (cardtype.getSelectedItem().toString().equals("Lists")) {
            populateLists();
            cardlist.setSelection(lastList);
        }
        else {
            populateCardTables();
            cardboxes.setSelection(lastbox);
        }


        if (themeName == "Dark Theme") {
            cardtype.setBackgroundColor(Color.BLACK);
            cardlist.setBackgroundColor(Color.BLACK);
            cardboxes.setBackground(getResources().getDrawable(R.drawable.ic_arrow_dropdown_18));
            listDeleter.setTextColor(Color.WHITE);
            boxDeleter.setTextColor(Color.WHITE);
        }

        cardboxlexicon.setText(LexData.getLexName());
        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();
    }

    protected void readPreferences() {
//        Boolean customKeyboard = shared.getBoolean("customkeyboard", false);
//        if (customKeyboard != LexData.getCustomkeyboard()) {
//            LexData.setCustomkeyboard(customKeyboard);
//        }
    }
    private void showFilter() {
        chkFilter.setVisibility(View.VISIBLE);
        searchType.setVisibility(View.VISIBLE);
        minLength.setVisibility(View.VISIBLE);
        maxLength.setVisibility(View.VISIBLE);
        etEntry.setVisibility(View.VISIBLE);
    }
    private void hideFilter() {
        chkFilter.setVisibility(GONE);
        searchType.setVisibility(GONE);
        minLength.setVisibility(GONE);
        maxLength.setVisibility(GONE);
        etEntry.setVisibility(GONE);
    }
    public void loadMinimum() {
        List <String> lengths = new ArrayList<>();
        lengths.add("Minimum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, lengths); // spinmedium
        minLength.setAdapter(dataAdapter);

    }
    public void loadMaximum() {
        List <String> lengths = new ArrayList<>();
        lengths.add("Maximum");
        for (int c = 2; c <= LexData.getMaxLength(); c++)
            lengths.add(Integer.toString(c) + " letters");

        ArrayAdapter<String> maxAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, lengths); // R.layout.spinitem
        maxLength.setAdapter(maxAdapter);
    }
    protected String createWordFilter() {
        Log.e("createWordFilter", "CWF");

        String subquery = "";
        String[] words;
        int counter = 0;
        int column;

        if (etEntry.getText().toString().contains(" ")) {
            String trim = etEntry.getText().toString().trim();
            etEntry.setText(String.valueOf(trim));
            //status.setText("Invalid search string");
            Toast.makeText(this,"Search filter contains invalid characters", Toast.LENGTH_SHORT).show();
            return "";
        }

        hidekeyboard();
//        databaseAccess.open();

        int least = minLength.getSelectedItemPosition() + 1;
        int most = maxLength.getSelectedItemPosition() + 1;

        cards = cardDatabase.getWritableDatabase();

        String term = etEntry.getText().toString();

        boolean hascards;

        if (searchType.getSelectedItemPosition() != 1 && searchType.getSelectedItemPosition() != 8)
            if (term.trim().length() == 0) {
                cards.close();
                return "";
            }

        switch (searchType.getSelectedItemPosition()) {
            //(anagrams, lengths, word build)

            // Anagrams
            case 0:
//                if (most == 1) { // If Any, assume same as least
//                    most = least;
//                    maxLength.setSelection(most - 1); //
//                }
                hascards = CardDatabase.addCardFilter(cards, getQuestionAnagramCursor(term), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                            makecardfilters(searchType.getSelectedItemPosition());
                else subquery = "";
                break;

            // Length
            case 1:
                // no cardFilter, just query statament
//                etEntry.setText("");
                // no need for makefilters since the only option is length
                if (least > 1) {
                    if (least == most)
                        subquery = " WHERE (Length(question) = " + least + ")";
                    else {
//                        if (most < least) { // should be handled before
//                            most = least;
//                            maxLength.setSelection(most - 1); //
//                            subquery = " WHERE (Length(question) = " + least + ")";
//                        } else
                            subquery = " WHERE (Length(question) >= " + least +
                                    " AND Length(question) <= " + most + ")";
                    }
                }
                else
                    subquery = "";
                break;

//                // Pattern
//            case 2:
//                if (term.trim().length() == 0)
//                    return "";
//                hascards = CardDatabase.addCardFilter(cards, getQuestionPatternCursor(term));
//
//                if (hascards)
//                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
//                            makecardfilters(searchType.getSelectedItemPosition());
//                else
//                    subquery = "";
//                Log.e("Filter SQL", subquery);
//                return (subquery);
//
            // Pattern Cursor
            case 2:

                // getQuestionPatternCursorThread(term);
                // finish
                // if CardDatabase.addCardFilter(cards, matrixcursor)
//                subquery = " WHERE question IN (SELECT card from cardFilter) " +
//                        makecardfilters(searchType.getSelectedItemPosition());
//                else
//                subquery = "";


                hascards = CardDatabase.addCardFilter(cards, getQuestionPatternCursor(term), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                            makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Contains
            case 3:
//                if (least == 1)
//                    minLength.setSelection(1);
//                if (most < least) { // If less, set to length of term
//                    maxLength.setSelection(14); //
//                }
                hascards = CardDatabase.addCardFilter(cards, getQuestionContainsCursor(term), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                            makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Contains All Word Build
            case 4:
//                if (least == 1)
//                    minLength.setSelection(1);
//                if (most < least) { // If less, set to length of term
//                    maxLength.setSelection(term.length() - 1); //
//                }
                hascards = CardDatabase.addCardFilter(cards, getQuestionBuildCursor(term), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                            makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Contains Any
            case 5:
                hascards = CardDatabase.addCardFilter(cards, getQuestionContainsAnyCursor(term), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) "+
                            makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Begins with
            case 6:
                hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursor_begins(term, makefilters(6)), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;

            // Ends with
            case 7:
                hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursor_ends(term, makefilters(6)), false);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;

            case 8: // From File
                if (importfile.getText().toString().equals("File specification") ||
                        importfile.getText().toString().isEmpty()) {

                    incompleteSearch("File must be selected");
                    hascards = false;
                }
                else {
                    hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursorFromWords(Utils.GetTextFile(this, importfile.getText().toString()), makefilters(6)), false);
                }

//                if (importfile.getText().toString().equals("File specification") ||
//                        importfile.getText().toString().isEmpty()) {
//
//                    incompleteSearch("File must be selected");
//                    break;
//                }
//                else

                // need to get list of words.


                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;


        }
        cards.close();
        Log.e("subquery", subquery);
        return (subquery);
    }
    protected String createAnagramFilter() {
Log.e("createAnagramFilter", "CAF");
        String subquery = "";
        String[] words;
        int counter = 0;
        int column;

        if (etEntry.getText().toString().contains(" ")) {
            String trim = etEntry.getText().toString().trim();
            etEntry.setText(String.valueOf(trim));
            //status.setText("Invalid search string");
            Toast.makeText(this,"Search filter contains invalid characters", Toast.LENGTH_SHORT).show();
            return "";
        }

        hidekeyboard();
//        databaseAccess.open();

        int least = minLength.getSelectedItemPosition() + 1;
        int most = maxLength.getSelectedItemPosition() + 1;

        cards = cardDatabase.getWritableDatabase();

        String term = etEntry.getText().toString();

        boolean hascards;


        if (searchType.getSelectedItemPosition() != 1 && searchType.getSelectedItemPosition() != 8)
            if (term.trim().length() == 0) {
                cards.close();
                return "";
            }

        switch (searchType.getSelectedItemPosition()) {
            //(anagrams, lengths, word build)

            // Anagrams
            case 0:
//                if (most == 1) { // If Any, assume same as least
//                    most = least;
//                    maxLength.setSelection(most - 1); //
//                }
                hascards = CardDatabase.addCardFilter(cards, getQuestionAnagramCursor(term), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                        makecardfilters(searchType.getSelectedItemPosition());
                else subquery = "";
                break;

            // Length
            case 1:
                // no cardFilter, just query statament
//                etEntry.setText("");
                // no need for makefilters since the only option is length
                if (least > 1) {
                    if (least == most)
                        subquery = " WHERE (Length(question) = " + least + ")";
                    else {
//                        if (most == 1) { // If Any, assume same as least
//                            most = least;
//                            maxLength.setSelection(most - 1); //
//                            subquery = " WHERE (Length(question) = " + least + ")";
//                        } else
                            subquery = " WHERE (Length(question) >= " + least +
                                    " AND Length(question) <= " + most + ")";
                    }
                }
                else
                    subquery = "";
                break;

//                // Pattern
//            case 2:
//                if (term.trim().length() == 0)
//                    return "";
//                hascards = CardDatabase.addCardFilter(cards, getQuestionPatternCursor(term));
//
//                if (hascards)
//                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
//                            makecardfilters(searchType.getSelectedItemPosition());
//                else
//                    subquery = "";
//                Log.e("Filter SQL", subquery);
//                return (subquery);
//
            // Pattern Cursor
            case 2:

                // getQuestionPatternCursorThread(term);
                // finish
                // if CardDatabase.addCardFilter(cards, matrixcursor)
//                subquery = " WHERE question IN (SELECT card from cardFilter) " +
//                        makecardfilters(searchType.getSelectedItemPosition());
//                else
//                subquery = "";


                hascards = CardDatabase.addCardFilter(cards, getQuestionPatternCursor(term), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                            makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Contains
            case 3:
                hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursor_contains(term, makefilters(3)), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;

            // Contains All Word Build
            case 4:
//                if (least == 1)
//                    minLength.setSelection(1);
//                if (most < least) { // If less, set to length of term
//                    maxLength.setSelection(term.length() - 1); //
//                }
                hascards = CardDatabase.addCardFilter(cards, getQuestionBuildCursor(term), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) " +
                        makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Contains Any
            case 5:
                hascards = CardDatabase.addCardFilter(cards, getQuestionContainsAnyCursor(term), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) "+
                        makecardfilters(searchType.getSelectedItemPosition());
                else
                    subquery = "";
                break;

            // Begins with
            case 6:
                hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursor_begins(term, makefilters(6)), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;

            // Ends with
            case 7:
                hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursor_ends(term, makefilters(6)), true);

                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;
            case 8: // From File
                Log.e("c", importfile.getText().toString());
                if (importfile.getText().toString().equals("File specification") ||
                        importfile.getText().toString().isEmpty()) {

                    incompleteSearch("File must be selected");
                    hascards = false;
                }
                else {
                    // hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursorFromWords(importfile.getText().toString(), ""), false);
                    hascards = CardDatabase.addCardFilter(cards, databaseAccess.getCursorFromWords(Utils.GetTextFile(this, importfile.getText().toString()), makefilters(6)), true);
                }
                if (hascards)
                    subquery = " WHERE question IN (SELECT card from cardFilter) ";
                else
                    subquery = "";
                break;
        }
        cards.close();
        Log.e("subquery", subquery);
        return (subquery);
    }
    public String makefilters(int searchType) {
        List<String> filters = new ArrayList<String>();

        int min = minLength.getSelectedItemPosition() + 1;
        int max = maxLength.getSelectedItemPosition() + 1;

        if (min == max && min > 1)
            filters.add(" Length(Word) = " + String.valueOf(min));
        else {
            String e = " Length(Word) >= " + String.valueOf(min) + " " +
                    " AND Length(Word) <= " + String.valueOf(max) + " ";
            if (searchType != 1 && searchType != 3)
                if (minLength.getSelectedItemPosition() != 0)
                    filters.add(e);

            if (searchType == 3)
                if (minLength.getSelectedItemPosition() != 0)
                    filters.add(e);
                else
                    filters.add(" Length(Word) >= " + Utils.analyzeMin(etEntry.getText().toString()) +
                            " AND Length(Word) <= " + Utils.analyzeMax(etEntry.getText().toString()));
        }

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
//        Log.i("Filter", SQLfilter);
        return SQLfilter;
    }
    public String makecardfilters(int searchType) {
        List<String> filters = new ArrayList<String>();

        int min = minLength.getSelectedItemPosition() + 1;
        int max = maxLength.getSelectedItemPosition() + 1;

        if (min == max && min > 1)
            filters.add(" Length(question) = " + String.valueOf(min));
        else {
            String e = " Length(question) >= " + String.valueOf(min) + " " +
                    " AND Length(question) <= " + String.valueOf(max) + " ";
            if (searchType != 1 && searchType != 3)
                if (minLength.getSelectedItemPosition() != 0)
                    filters.add(e);

            if (searchType == 3)
                if (minLength.getSelectedItemPosition() != 0)
                    filters.add(e);
                else
                    filters.add(" Length(question) >= " + Utils.analyzeMin(etEntry.getText().toString()) +
                            " AND Length(question) <= " + Utils.analyzeMax(etEntry.getText().toString()));
        }

        StringBuilder builder = new StringBuilder();
        for(String s : filters) {
            builder.append(" AND " + s);
        }

        String SQLfilter = builder.toString();
//        Log.i("Filter", SQLfilter);
        return SQLfilter;
    }
    // this populates the spinner with list names
    public void populateCardTables() {
        boxDeleter.setVisibility(View.VISIBLE);
        listDeleter.setVisibility(View.GONE);
        cardlistlayout.setVisibility(View.INVISIBLE);
        listtype.setVisibility(View.GONE);
        mergeCard.setVisibility(View.GONE);
        deleteWhenDone.setVisibility(View.GONE);

//        if (BuildConfig.BUILD_TYPE == "beta") {
//            showFilter();
//        }

        String sql;

        String [] header = new String[3];
        SimpleCursorAdapter boxCursorAdapter;
        SimpleCursorAdapter schCursorAdapter;
        MatrixCursor matrixBoxCursor;
        MatrixCursor matrixSchCursor;

        String boxtype = cardtype.getSelectedItem().toString();
        boxtype = Utils.deSpaceString(boxtype);

        LexData.Cardbox cb = new LexData.Cardbox();
        cb.program = "Hoot";
        cb.lexicon = LexData.getLexName();
        cb.boxtype = boxtype;

        LexData.setCardfile(cb);

        matrixBoxCursor = new MatrixCursor(new String[]{"_id", "cardbox", "total"});
        matrixSchCursor = new MatrixCursor(new String[]{"_id", "nextday", "total"});



        if (Utils.fileExist(LexData.getCardfile())) {
            cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
            cards = cardDatabase.getWritableDatabase();

            // Show list of active Card boxes
            String[] Bfrom = new String[]{"cardbox","total"};
            int[] Bto = new int[] { R.id.box, R.id.boxcount};

            List<String> activeBoxes = new ArrayList<>();
            sql = "Select DISTINCT cardbox from questions order by cardbox;";
            Cursor activeCursor = cards.rawQuery(sql, null);

            if (activeCursor.getCount() == 0) {
                cardboxes.setAdapter(null);
                cardschedule.setAdapter(null);
                boxDeleter.setVisibility(View.INVISIBLE);
                wordcount.setText("0");
                Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
                return;
            }
            boxDeleter.setVisibility(View.VISIBLE);

            while(activeCursor.moveToNext()) {
                activeBoxes.add(activeCursor.getString(0));
            }

            Cursor boxCursor;
            boxCursorAdapter = new BoxListAdapter(getBaseContext(), R.layout.cardboxlist, matrixBoxCursor, Bfrom, Bto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            cardboxes.setAdapter(boxCursorAdapter);

            header[0] = String.valueOf(idNumber++);
            header[1] = "Card Box";
            header[2] = "Count";
            matrixBoxCursor.addRow(header);
            for (int n = 0; n < activeBoxes.size(); n++) {
                sql = "Select question as _id, cardbox, Count(cardbox) as total from questions WHERE cardbox = " + activeBoxes.get(n) + " order by cardbox;";
                boxCursor = cards.rawQuery(sql, null);
                while(boxCursor.moveToNext()) {
                    // todo: modify to only add if cards in box
                    matrixBoxCursor.addRow(get_BoxRow(boxCursor));
                }
            }




            // Show list of scheduled items
            String[] Sfrom = new String[]{"nextday","total"};
            int[] Sto = new int[] { R.id.schedule, R.id.schedulecount};

            List<String> activeSchedule = new ArrayList<>();
            sql = "Select DISTINCT (next_scheduled / 86400) * 86400 as nextday from questions order by nextday;";
            Cursor scheduleCursor = cards.rawQuery(sql, null);
            while(scheduleCursor.moveToNext()) {
                activeSchedule.add(scheduleCursor.getString(0));
            }




            Cursor schCursor;
            schCursorAdapter = new SchListAdapter(getBaseContext(), R.layout.schedulelist, matrixSchCursor, Sfrom, Sto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            cardschedule.setAdapter(schCursorAdapter);

            header[0] = String.valueOf(idNumber++);
            header[1] = "Scheduled";
            header[2] = "Count";
            matrixSchCursor.addRow(header);
            for (int n = 0; n < activeSchedule.size(); n++) {
                sql = "Select question as _id, (next_scheduled / 86400) * 86400 as nextday, Count((next_scheduled / 86400) * 86400) as total from questions WHERE nextday = " + activeSchedule.get(n) + " order by nextday;";
                schCursor = cards.rawQuery(sql, null);
                while(schCursor.moveToNext()) {
                    // todo: modify to only add if cards in box
                    matrixSchCursor.addRow(get_SchRow(schCursor));
                }
            }

            SharedPreferences shared;
            shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String wc = shared.getString("cardcount", "100");
            if (!Utils.isParsable(wc))
                wc = "50";
            if (wc.equals("0") || wc.equals("")) {
                sql = "Select Count(*) from questions WHERE next_scheduled <= " + unixDate(today()) + "  ;";
                Cursor countCursor = cards.rawQuery(sql, null);
                countCursor.moveToFirst();
                wc = countCursor.getString(0);
            }
            wordcount.setText(wc);
            cards.close();
        }
        else {
            cardboxes.setAdapter(null);
            cardschedule.setAdapter(null);
            wordcount.setText("0");
            boxDeleter.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
        }
    }
    public void populateLists() {
        String sql;

        boxDeleter.setVisibility(View.INVISIBLE);

        String boxtype = cardtype.getSelectedItem().toString();
        boxtype = Utils.deSpaceString(boxtype);

        LexData.Cardbox cb = new LexData.Cardbox();
        cb.program = "Hoot";
        cb.lexicon = LexData.getLexName();
        cb.boxtype = boxtype;

        LexData.setCardfile(cb);

        if (Utils.fileExist(LexData.getCardfile())) {
            List<String> listTitles = new ArrayList<>();

            cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
            cards = cardDatabase.getWritableDatabase();

            // sort by order added to database
            sql =       "SELECT ListCardTitle from tblList order by ListCardID";
            Cursor cursor= cards.rawQuery(sql, null);
            int index = cursor.getColumnIndex("ListCardTitle");

            if (cursor.getCount() == 0) {
                listDeleter.setVisibility(View.GONE);
                cardboxes.setAdapter(null);
                cardschedule.setAdapter(null);
                return;
            }
            boxDeleter.setVisibility(View.INVISIBLE);
            listDeleter.setVisibility(View.VISIBLE);

            while (cursor.moveToNext()) {
                listTitles.add(cursor.getString(index));
            }

            listAdapter = new ArrayAdapter<String>(this, R.layout.spinselection, listTitles);
            //listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listTitles);
            // listAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cardlist.setAdapter(listAdapter);
            cardlistlayout.setVisibility(View.VISIBLE);
            cards.close();
            // after list is determined, populate tables
            populateListTables();
        }
        else {
            cardboxes.setAdapter(null);
            cardschedule.setAdapter(null);

            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
        }

    }
    public void populateListTables() {

        String sql;
        String [] header = new String[3];
        SimpleCursorAdapter boxCursorAdapter;
        SimpleCursorAdapter schCursorAdapter;

        MatrixCursor matrixBoxCursor;
        MatrixCursor matrixSchCursor;
        String boxtype = cardtype.getSelectedItem().toString();
        boxtype = Utils.deSpaceString(boxtype);

        LexData.Cardbox cb = new LexData.Cardbox();
        cb.program = "Hoot";
        cb.lexicon = LexData.getLexName();
        cb.boxtype = boxtype;

        LexData.setCardfile(cb);

        matrixBoxCursor = new MatrixCursor(new String[]{"_id", "cardbox", "total"});
        matrixSchCursor = new MatrixCursor(new String[]{"_id", "nextday", "total"});



        if (Utils.fileExist(LexData.getCardfile())) {
            cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
            cards = cardDatabase.getWritableDatabase();


            // show list quiz type
            sql =       "SELECT quiz_type from tblList WHERE ListCardTitle = '" + cardlist.getSelectedItem().toString() + "';";
            Cursor qtCursor = cards.rawQuery(sql, null);

            if (qtCursor.getCount() == 0) {
                cardboxes.setAdapter(null);
                cardschedule.setAdapter(null);
                return;
            }

            qtCursor.moveToFirst();
            int qt = qtCursor.getInt(qtCursor.getColumnIndex("quiz_type"));
            listtype.setText(LexData.quiztypes[qt-1]);
            listtype.setVisibility(View.VISIBLE);

            String mergeText = "Merge list cards into " + LexData.quiztypes[qt-1];
            mergeCard.setText(mergeText );
            mergeCard.setVisibility(View.VISIBLE);
            deleteWhenDone.setVisibility(View.VISIBLE);

            hideFilter();

            qtCursor.close();


            // Show list of active Card boxes
            String[] Bfrom = new String[]{"cardbox","total"};
            int[] Bto = new int[] { R.id.box, R.id.boxcount};

            List<String> activeBoxes = new ArrayList<>();

            // get the listID
            sql =       "SELECT ListCardID from tblList WHERE ListCardTitle = '" + cardlist.getSelectedItem().toString() + "';";
            Cursor cursor= cards.rawQuery(sql, null);

            if (cursor.getCount() == 0) {
                cardboxes.setAdapter(null);
                cardschedule.setAdapter(null);
                return;
            }

            cursor.moveToFirst();
            String listID = cursor.getString(cursor.getColumnIndex("ListCardID"));

            // get the distinct cardboxes
            sql = "Select DISTINCT cardbox from tblListWords where ListCardID = " + listID +
                    " order by cardbox;";
            Cursor activeCursor = cards.rawQuery(sql, null);
            while(activeCursor.moveToNext()) {
                activeBoxes.add(activeCursor.getString(0));
            }

            // get the boxes and word count
            Cursor boxCursor;
            boxCursorAdapter = new BoxListAdapter(getBaseContext(), R.layout.cardboxlist, matrixBoxCursor, Bfrom, Bto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            cardboxes.setAdapter(boxCursorAdapter);

            header[0] = String.valueOf(idNumber++);
            header[1] = "Card Box";
            header[2] = "Count";
            matrixBoxCursor.addRow(header);
            for (int n = 0; n < activeBoxes.size(); n++) {
                sql = " Select question as _id, cardbox, Count(cardbox) as total from tblListWords WHERE ListCardID = '" +
                        listID + "' AND  cardbox = " + activeBoxes.get(n) + " order by cardbox;";
                boxCursor = cards.rawQuery(sql, null);
                while(boxCursor.moveToNext()) {
                    // todo: modify to only add if cards in box
                    matrixBoxCursor.addRow(get_BoxRow(boxCursor));
                }
            }




            // Show list of scheduled items
            String[] Sfrom = new String[]{"nextday","total"};
            int[] Sto = new int[] { R.id.schedule, R.id.schedulecount};

            List<String> activeSchedule = new ArrayList<>();
            sql = "Select DISTINCT (next_scheduled / 86400) * 86400 as nextday from tblListWords where ListCardID = " + listID + " order by nextday;";
            Cursor scheduleCursor = cards.rawQuery(sql, null);
            while(scheduleCursor.moveToNext()) {
                activeSchedule.add(scheduleCursor.getString(0));
            }

            Cursor schCursor;
            schCursorAdapter = new SchListAdapter(getBaseContext(), R.layout.schedulelist, matrixSchCursor, Sfrom, Sto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            cardschedule.setAdapter(schCursorAdapter);

            header[0] = String.valueOf(idNumber++);
            header[1] = "Scheduled";
            header[2] = "Count";
            matrixSchCursor.addRow(header);
            for (int n = 0; n < activeSchedule.size(); n++) {
                sql = "Select question as _id, (next_scheduled / 86400) * 86400 as nextday, Count((next_scheduled / 86400) * 86400) as total " +
                        " from tblListWords WHERE ListCardID = " + listID +
                        " AND   nextday = " + activeSchedule.get(n) + " order by nextday;";
                schCursor = cards.rawQuery(sql, null);

                while(schCursor.moveToNext()) {
                    // todo: modify to only add if cards in box
                    matrixSchCursor.addRow(get_SchRow(schCursor));
                }
            }

            SharedPreferences shared;
            shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String wc = shared.getString("cardcount", "100");
            if (!Utils.isParsable(wc))
                wc = "50";
            if (wc.equals("0") || wc.equals("")) {
                sql = "Select Count(*) from tblListWords WHERE ListCardID = " + listID +
                        " AND next_scheduled <= " + unixDate(today()) + "  ;";
                Cursor countCursor = cards.rawQuery(sql, null);
                countCursor.moveToFirst();
                wc = countCursor.getString(0);
            }
            wordcount.setText(wc);
            cards.close();
        }
        else {
            cardboxes.setAdapter(null);
            cardschedule.setAdapter(null);
            wordcount.setText("0");
            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
        }
    }
    private void setAutoLengths() {
        // anagrams
        if (searchType.getSelectedItemPosition() == 0)
            if (etEntry.getText().length() > 0) {
                minLength.setSelection(etEntry.getText().length() - 1);
                maxLength.setSelection(etEntry.getText().length() - 1);
            }
            else
                return;

        // length
        if (searchType.getSelectedItemPosition() == 1) {
            etEntry.setText("");
            if (minLength.getSelectedItemPosition() == 0)
                minLength.setSelection(etEntry.getText().length() - 1);
        }

        // pattern (2)

        if (Arrays.asList(3,6,7).contains(searchType.getSelectedItemPosition())) {
            if (minLength.getSelectedItemPosition() == 0)
                minLength.setSelection(etEntry.getText().length() - 1);
            if (maxLength.getSelectedItemPosition() == 0)
                maxLength.setSelection(LexData.getMaxLength() - 1);
        }


        // word build
        if (searchType.getSelectedItemPosition() == 4) {
            if (minLength.getSelectedItemPosition() == 0)
                minLength.setSelection(1);
            if (maxLength.getSelectedItemPosition() == 0)
                maxLength.setSelection(etEntry.getText().length() - 1);
        }

        // contains any (5)

        // any search
        int minp = minLength.getSelectedItemPosition();
        int maxp = maxLength.getSelectedItemPosition();
        if (minp > maxp) {
            maxLength.setSelection(minp);
        }

    }


    // this populates the tables when list is selected
    int idNumber = 1;
    public String[] get_BoxRow (Cursor cursor) {
        String [] columnValues = new String[3];
        columnValues[0] = String.valueOf(idNumber++);
        for (int c = 1; c < 3; c++) {
//            Log.d("Row ", cursor.getString(c));
            columnValues[c] = cursor.getString(c);
        }
        return columnValues;
    }
    public String[] get_SchRow (Cursor cursor) {
        String [] columnValues = new String[3];
        columnValues[0] = String.valueOf(idNumber++);
        for (int c = 1; c < 3; c++) {
//            Log.d("Row ", cursor.getString(c));
            columnValues[c] = cursor.getString(c);
        }

//        columnValues[2] = dateFormat.format(CardUtils.dtDate(Integer.parseInt(columnValues[2])));

        int intDate = Integer.parseInt(columnValues[1]);
        Date date = CardUtils.dtDate(intDate);
        String strDate = dateFormat.format(date);
//        Log.e("IntDate ", strDate);

        if (columnValues[1] == "0")
            columnValues[1] = "New";
        else
            columnValues[1] = strDate;
//            columnValues[1] = dateFormat.format(CardUtils.dtDate( Integer.parseInt(columnValues[2]) ));


        return columnValues;
    }
    protected String[] get_CursorRow (Cursor cursor) {
        String [] columnValues = new String[11];
        for (int c = 0; c < 11; c++)
            columnValues[c] = cursor.getString(c);
        return columnValues;
    }
    protected String[] get_CursorValue (Cursor cursor) {
        String [] columnValues = new String[1];
//        for (int c = 0; c < 11; c++)
        columnValues[0] = cursor.getString(0);
        return columnValues;
    }

    // card database is already open when these are called
    private Cursor getQuestionContainsCursor(String term) {
        // make it compatible with Lexicon queries
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"Word"});

        String filter = String.format("Length(question) >= %1$s", term.length());
        Cursor cursor = cards.rawQuery("SELECT question \n" +
                "FROM questions \n" +
                "WHERE ( question LIKE '%" + term + "%' " +
                " AND " + filter + " )", null);

        while (cursor.moveToNext()) {

//            String word = cursor.getString(0);
//            if (word.contains(term))
                matrixCursor.addRow(get_CursorValue(cursor));
        }
        return matrixCursor;
    }
    private Cursor getQuestionContainsAnyCursor(String term) {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"Word"});

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

        Cursor precursor = cards.rawQuery("SELECT question \n" +
                "FROM questions ", null );

        while (precursor.moveToNext()) {

            String word = precursor.getString(0);
            char[] b = word.toCharArray();

            if (databaseAccess.containsany(first, b)) {
                matrixCursor.addRow(get_CursorValue(precursor));
            }
        }

        precursor.close();
        return matrixCursor;

    }
    private Cursor getQuestionBuildCursor(String term) {
        Cursor cursor;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"Word"});
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

//        databaseAccess.open();
        cursor = databaseAccess.rawQuery("SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                " ) ");

        while (cursor.moveToNext()) {
            String word = cursor.getString(0);
            char[] anagram = word.toCharArray();
            if (databaseAccess.isAnagram(first, anagram, blankcount)) {
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
                matrixCursor.addRow(get_CursorValue(cursor));
            }
        }

        cursor.close();
//        databaseAccess.close();
        return matrixCursor;
    }
    private Cursor getQuestionAnagramCursor(String term) {


        // doesn't handle "*"


        // use for Stems primarily
        char[] stem = term.toCharArray(); // stem

        int[] first = new int[26]; // letter count of stem
        int c; // array position
        int blankcount = 0;
        int mismatchcount = 0;
        String word;
        char[] anagram;
        int letter;

        // initialize word to anagram
        for (c = 0; c < stem.length; c++) {
            if (stem[c] == '?') {
                blankcount++;
                continue;
            }
            first[stem[c] - 'A']++;
        }

        String filter = String.format("Length(question) = %1$s", term.length());
        Cursor cursor = cards.rawQuery("SELECT question \n" +
                "FROM questions \n" +
                "WHERE ( " + filter + " )", null);

        // make it compatible with Lexicon queries
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"Word"});

        int column = cursor.getColumnIndex("question");
        while (cursor.moveToNext()) {
            word = cursor.getString(column);

 //           Log.e("wo", word);
            //char[]
            anagram = word.toCharArray();
            int[] second = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            char[] blanks = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            mismatchcount = 0;

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

            matrixCursor.addRow(get_CursorValue(cursor));
        }
        return matrixCursor;

    }
    private Cursor getQuestionPatternCursor(String pattern) {
        String prep = databaseAccess.buildInnerPattern(pattern);
        String exp = Utils.buildPattern(prep);
        Log.e("AfterBuild", exp);
        String frontfilter = "";
        String backfilter = "";

        if (Character.isUpperCase(exp.charAt(1)))
            frontfilter = " AND Word LIKE '" + exp.charAt(1) + "%' ";
        if (Character.isUpperCase(exp.charAt(exp.length() - 2)))
            backfilter = " AND Word LIKE '%" + exp.charAt(exp.length() - 2) + "' ";

        String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"Word"});

        String sql = "SELECT Word \n" +
                "FROM     `" + LexData.getLexName() + "` \n" +
                "WHERE (" + lenFilter +
                frontfilter + backfilter +
                " ) " ;
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
                String word = precursor.getString(0);

                if (word.matches(exp)) {
                    matrixCursor.addRow(get_CursorValue(precursor));
                }

            }
        } catch (Exception e) {

        }
        precursor.close();
//        databaseAccess.close();
        return matrixCursor;

    }
    protected void incompleteSearch() {
        Toast.makeText(this,"Cannot complete the search with these parameters", Toast.LENGTH_SHORT).show();
    }
    protected void incompleteSearch(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Keyboard support
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
    public final static int CodeDelete   = -5; // Keyboard.KEYCODE_DELETE
    public final static int CodeSearch = 55007;
    protected void useCustomKeyboard() {
        // Create the Keyboard

        // Do not show the preview balloons
        mKeyboardView.setPreviewEnabled(false);
        // Install the key handler
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);


        // Hide the standard keyboard initially
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        registerEditText(R.id.etEntry2);
        hideCustomKeyboard();
    }
    // keyboard watcher
    protected KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = CardBoxActivity.this.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();

            // Handle key

            if( primaryCode==CodeSearch ) {
                if( editable!=null )
                    hidekeyboard();
            } else
            if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);
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
        mKeyboardView.setVisibility(View.GONE);
        mKeyboardView.setEnabled(false);
    }
    public void showCustomKeyboard( View v ) {
        mKeyboardView.setVisibility(View.VISIBLE);
        mKeyboardView.setEnabled(true);
        if( v!=null ) ((InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    public boolean isCustomKeyboardVisible() {
        return mKeyboardView.getVisibility() == View.VISIBLE;
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
        edittext.setOnClickListener(new View.OnClickListener() {
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


    // Listeners and calls
    protected View.OnClickListener cardboxhelp = new View.OnClickListener() {
        public void onClick(View v) {
//            showHelp();
            getHelp("cardboxhelp.html");
        }
    };
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


//    public void showHelp() {
//        Intent intent = new Intent(this, CardBoxHelpActivity.class);
//        startActivity(intent);
//    }

    protected View.OnClickListener beginClick = new View.OnClickListener() {
        public void onClick(View v) {
            beginCardQuiz(cardtype.getSelectedItem().toString());
        }
    };
    public void beginCardQuiz(String boxtype) {
        hidekeyboard();
        if (chkFilter.isChecked()) {
            setAutoLengths();
            if (searchType.getSelectedItemPosition() != 8)
            if (minLength.getSelectedItemPosition() == 0 || maxLength.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "This search requires you to select word lengths", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!Utils.fileExist(LexData.getCardfile())) {
            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
            return;
        }

        if (chkFilter.isChecked())
            Toast.makeText(this, "Filtering . . . ", Toast.LENGTH_SHORT).show();


        boxtype = Utils.deSpaceString(boxtype);

        // prep bundle
        Bundle bundle =new Bundle();
        Intent intentBundle;
        bundle.putString("cardbox", boxtype);

        StringBuilder ordering = new StringBuilder();

        if (chkBox0.isChecked()) {
            if (chkRandom.isChecked())
                bundle.putString("order", "boxrandom");
            else bundle.putString("order", "box");
        }
        else {
            if (chkRandom.isChecked())
                bundle.putString("order", "schrandom");
            else
                bundle.putString("order", "schedule");
        }

        // List needs to populate list titles, and selected list title populates cards, schedule before beginQuiz
        if (boxtype.equals("Lists")) {
            if (cardlist.getCount() == 0) {
                Toast.makeText(this, "There are no lists", Toast.LENGTH_LONG).show();
                return;
            }
            else
                bundle.putString("listname", cardlist.getSelectedItem().toString());
        }

        String count = wordcount.getText().toString();
        bundle.putString("wordcount", count);


        String filterQuery;
        switch (boxtype) {
            case "Anagrams":
                if (chkFilter.isChecked()) {
//                    if (threading)
//                        thread (include through opon activity (below) in finish)
//                                return

//                    Toast.makeText(this, "Filtering . . . ", Toast.LENGTH_LONG).show();
                    filterQuery = createAnagramFilter();
                    if (filterQuery.equals("")) {
                        Toast.makeText(this, "No words in the card box meet the filter criteria", Toast.LENGTH_LONG).show();
                        return;
                    }
                    bundle.putString("filter", filterQuery);
                }

                intentBundle = new Intent(CardBoxActivity.this, CardAnagramQuizActivity.class);
                break;
            case "Hooks":
            case "BlankAnagrams":
                if (chkFilter.isChecked()) {
//                    if (threading)
//                        thread (include through opon activity (below) in finish)
//                                return

//                    Toast.makeText(this, "Filtering . . . ", Toast.LENGTH_LONG).show();
                    filterQuery = createWordFilter();
                    if (filterQuery.equals("")) {
                        Toast.makeText(this, "No words in the card box meet the filter criteria", Toast.LENGTH_LONG).show();
                        return;
                    }
                    bundle.putString("filter", filterQuery);
                }

                intentBundle = new Intent(CardBoxActivity.this, CardQuizActivity.class);
                break;

            case "Lists":

                if (cardlist.getSelectedItem().toString() == "")
                    return;
                cards = cardDatabase.getWritableDatabase();
                String sql = "SELECT quiz_type from tblList WHERE ListCardTitle = '" + cardlist.getSelectedItem().toString() + "';";
                Cursor cursor= cards.rawQuery(sql, null);
                cursor.moveToFirst();
                int qtype = cursor.getInt(cursor.getColumnIndex("quiz_type"));

                cards.close();

                if (qtype == 1)
                    intentBundle = new Intent(CardBoxActivity.this, CardAnagramQuizActivity.class);
                else
                    intentBundle = new Intent(CardBoxActivity.this, CardQuizActivity.class);
                break;

            default:
                return;
        }

//        filterToast.cancel();

        // open activity
        intentBundle.putExtras(bundle);
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);

        lastbox = cardboxes.getSelectedItemPosition();
        lastList = cardlist.getSelectedItemPosition();

    }

    protected View.OnClickListener reviewClick = new View.OnClickListener() {
        public void onClick(View v) {
            beginCardReview(cardtype.getSelectedItem().toString());
        }
    };
    public void beginCardReview(String boxtype) {
        hidekeyboard();
        if (chkFilter.isChecked()) {
            setAutoLengths();
            if (searchType.getSelectedItemPosition() != 8)
            if (minLength.getSelectedItemPosition() == 0 || maxLength.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "This search requires you to select word lengths", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (!Utils.fileExist(LexData.getCardfile())) {
            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
            return;
        }

        if (chkFilter.isChecked())
            Toast.makeText(this, "Filtering . . . ", Toast.LENGTH_SHORT).show();

        boxtype = Utils.deSpaceString(boxtype);

        // prep bundle
        Bundle bundle =new Bundle();
        Intent intentBundle;
        bundle.putString("cardbox", boxtype);


        if (chkBox0.isChecked()) {
            if (chkRandom.isChecked())
                bundle.putString("order", "boxrandom");
            else bundle.putString("order", "box");
        }
        else {
            if (chkRandom.isChecked())
                bundle.putString("order", "schrandom");
            else
                bundle.putString("order", "schedule");
        }

//
//
//        if (chkBox0.isChecked())
//            bundle.putString("order", "box");
//        else
//            bundle.putString("order", "schedule");

        // List needs to populate list titles, and selected list title populates cards, schedule before beginQuiz

        String count = wordcount.getText().toString();
        bundle.putString("wordcount", count);

        String filterQuery;
        switch (boxtype) {
            case "Anagrams":
                if (chkFilter.isChecked()) {
//                    Toast.makeText(this, "Filtering . . . ", Toast.LENGTH_LONG).show();
                    // createAnagramFilter returns query based on filter
                    filterQuery = createAnagramFilter();

                    if (filterQuery.trim().equals("")) {
//                        filterToast.cancel();
                        Toast.makeText(this, "No words in the card box meet the filter criteria", Toast.LENGTH_LONG).show();
                        return;
                    }
                    bundle.putString("filter", filterQuery);
                }

                bundle.putString("search", boxtype);
                break;

            case "Hooks":

            case "BlankAnagrams":
                if (chkFilter.isChecked()) {
//                    Toast.makeText(this, "Filtering . . . ", Toast.LENGTH_LONG).show();
                    // createAnagramFilter returns query based on filter
                    filterQuery = createWordFilter();

                    if (filterQuery.trim().equals("")) {
//                        filterToast.cancel();
                        Toast.makeText(this, "No words in the card box meet the filter criteria", Toast.LENGTH_LONG).show();
                        return;
                    }
                    bundle.putString("filter", filterQuery);
                }

                bundle.putString("search", boxtype);
                break;

            case "Lists":

                    if (cardlist.getCount() == 0) {
                        Toast.makeText(this, "There are no lists", Toast.LENGTH_LONG).show();
                        return;
                    }
                    else
                        bundle.putString("listname", cardlist.getSelectedItem().toString());




//                bundle.putString("listname", cardlist.getSelectedItem().toString());
                cards = cardDatabase.getWritableDatabase();
                String sql = "SELECT quiz_type from tblList WHERE ListCardTitle = '" + cardlist.getSelectedItem().toString() + "';";
                Cursor cursor= cards.rawQuery(sql, null);
                cursor.moveToFirst();
                int qtype = cursor.getInt(cursor.getColumnIndex("quiz_type"));
                String[] qtypes = getResources().getStringArray(R.array.qtypes);

                bundle.putString("search", qtypes[qtype-1]);
                cards.close();
                break;

            default:
                return;
        }

//        filterToast.cancel();

        // unlike quiz, same bundle for all reviews
        intentBundle = new Intent(CardBoxActivity.this, ReviewActivity.class);

        // open activity
        intentBundle.putExtras(bundle);
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);//
        intentBundle.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//
        startActivity(intentBundle);

        lastbox = cardboxes.getSelectedItemPosition();
        lastList = cardlist.getSelectedItemPosition();
    }

    protected View.OnClickListener beginMerge = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mergeAlert(cardlist.getSelectedItem().toString());
        }
    };
    private boolean mergeAlert(String ListName) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Are you sure?" );

        String msg = "Are you sure you want to merge the list " + ListName + " with \n" + listtype.getText() +  "?\n";
        if (deleteWhenDone.isChecked()) {
            builder.setMessage(msg + "List will be deleted afterwards.");
        }
        else
            builder.setMessage(msg + "Cards in the list will be reset to box 0.");

        builder.setPositiveButton("Yes. Merge now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                beginListMerge(ListName);
//                if (cardlist.getCount() == 0) {
//                    listAdapter.clear();
//                    listAdapter.notifyDataSetChanged();
//                    cardlist.setAdapter(null);
//                    cardboxes.setAdapter(null);
//                    cardschedule.setAdapter(null);
//                }
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
    public void beginListMerge(String listname) {
// like so
//        cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
//        cards = cardDatabase.getWritableDatabase();
        lastList = cardlist.getSelectedItemPosition();

        LexData.HListWord listWord;
        LexData.HListWord updateListWord;

        if (listname == "")
            return;
        CardDatabase cardsDatabase, listcardsDatabase;
        SQLiteDatabase cards, listcards;

//        need to setCardfile manually
        LexData.Cardbox listbox = new LexData.Cardbox();
        listbox.program = "Hoot";
        listbox.lexicon = LexData.getLexName();
        listbox.boxtype = "Lists";

        String listcardfile = programData(listbox.program) + File.separator + listbox.lexicon + File.separator + listbox.boxtype + ".db";

        // open list cards database
        if (Utils.fileExist(listcardfile)) {
            Toast.makeText( this, "Merging.. Please wait..", Toast.LENGTH_LONG ).show();

            listcardsDatabase = new CardDatabase(this, listcardfile, null, 2);
            listcards = listcardsDatabase.getWritableDatabase();

            // get list card cursor
            LexData.HList hlist = CardDatabase.getList(listcards, listname);
            int ListID = hlist.id; // needed for genListWord

            Cursor carditems = cardDatabase.getScheduledListCards(listcards, listname, "box", null);
            int cardCount = carditems.getCount();
            if (cardCount == 0) {
                Toast.makeText(this, "Error: Card box has no entries", Toast.LENGTH_LONG).show();
                listcards.close();
            }

            // open, or create quiztype database
            LexData.Cardbox cardsbox = new LexData.Cardbox();
            cardsbox.program = "Hoot";
            cardsbox.lexicon = LexData.getLexName();
            cardsbox.boxtype = LexData.quiztypes[hlist.quiz_type - 1];
 //           Log.e("box type", cardsbox.boxtype);

            String cardfile = programData(cardsbox.program) + File.separator + cardsbox.lexicon + File.separator + cardsbox.boxtype + ".db";

            cardsDatabase = new CardDatabase(this, cardfile, null, 2);
            cards = cardsDatabase.getWritableDatabase();


            // read list cards
            while (carditems.moveToNext()) {
                String sql;
                // get word from list
                listWord = CardDatabase.getListWord(listcards, ListID, carditems.getString(carditems.getColumnIndex("question")));

                // find word in standard card
                LexData.ZWord selection = getCard(cards, listWord.question);

                // merge cards
                if (selection != null) {
                    int newCard = selection.cardbox + listWord.cardbox;
                    int nextSch = Math.max(selection.next_scheduled, listWord.next_scheduled);
                    int lastCorrect = Math.max(selection.last_correct, listWord.last_correct);
                    int newCorrect = selection.correct + listWord.correct;
                    int newStreak = selection.streak + listWord.streak;
                    sql = "UPDATE questions SET cardbox = " + newCard + ", " +
                            "correct = " + newCorrect + ", " +
                            "streak = " + newStreak + ", " +
                            "last_correct = " + lastCorrect + ", " +
                            "next_scheduled = " + nextSch + " " +
                            "WHERE question = '" + listWord.question + "';";
                    cards.execSQL(sql);
                } else { // add new
                    cards.execSQL("INSERT OR IGNORE INTO questions (question, " +
                            "correct, incorrect, streak, last_correct, difficulty," +
                            "cardbox, next_scheduled) " +
                            "VALUES ('" + listWord.question + "', " +
                            listWord.correct + ", " +
                            listWord.incorrect + ", " +
                            listWord.streak + ", " +
                            listWord.last_correct + ", " +
                            listWord.difficulty + ", " +
                            listWord.cardbox + ", " +
                            listWord.next_scheduled + ");"
                    );
                }

            }
            // reset listcards OR delete list
            if (deleteWhenDone.isChecked()) {
                String sqlcmd;
                sqlcmd = "DELETE from tblListWords where ListCardID = " + ListID + ";";
                listcards.execSQL(sqlcmd);

                sqlcmd = "DELETE from tblList where ListCardID = " + ListID + ";";
                listcards.execSQL(sqlcmd);

            } else {
                String sql = "UPDATE tblListWords SET cardbox = 0, " +
                        "correct = 0, " +
                        "streak = 0, " +
                        "last_correct = 0, " +
                        "next_scheduled = " + nextDate(0) + ";";
                listcards.execSQL(sql);
                if (cardtype.getSelectedItem().toString().equals("Lists")) {
                    populateLists();
                    cardlist.setSelection(lastList);
                }

            }

            Toast.makeText( this, "Merge Complete", Toast.LENGTH_LONG ).show();

            carditems.close();
            listcards.close();
            cards.close();
            String msg = "All cards have been merged with " + cardsbox.boxtype;
            Toast.makeText(this, msg, Toast.LENGTH_LONG );
            populateLists();
        }


    }

    protected View.OnClickListener deleteClick = new View.OnClickListener() {
        public void onClick(View v) {
            deleteAlert(cardlist.getSelectedItem().toString());

        }
    };
    public void deleteList(String listname) {

        if (listname == "")
            return;
        // called by deleteAlert
        cards = cardDatabase.getWritableDatabase();

        String sqlcmd = "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listname + "';";
        Cursor cursor= cards.rawQuery(sqlcmd, null);
        cursor.moveToFirst();
        int id = cursor.getInt(cursor.getColumnIndex("ListCardID"));

        sqlcmd = "DELETE from tblListWords where ListCardID = " + id + ";";
        cards.execSQL(sqlcmd);

        sqlcmd = "DELETE from tblList where ListCardID = " + id + ";";
        cards.execSQL(sqlcmd);

        cards.close();
        populateLists();

    }
    private boolean deleteAlert(String ListName) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        StringBuilder msg = new StringBuilder();

        builder.setTitle("Are you sure you want to delete the list " + ListName + "?");
        builder.setPositiveButton("Yes. Delete now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteList(ListName);
                populateLists();
                if (cardlist.getCount() == 0) {
                    listAdapter.clear();
                    listAdapter.notifyDataSetChanged();
                    cardlist.setAdapter(null);
                    cardlistlayout.setVisibility(View.INVISIBLE);
                    cardboxes.setAdapter(null);
                    cardschedule.setAdapter(null);
                }
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

    protected View.OnClickListener deleteCBClick = new View.OnClickListener() {
        public void onClick(View v) {
            deleteCBAlert(cardtype.getSelectedItem().toString());

        }
    };
    public void deleteCB(String boxtype) {
        CardDatabase cardsDatabase;
        SQLiteDatabase cards;

        // open, or create quiztype database
        LexData.Cardbox cardsbox = new LexData.Cardbox();
        cardsbox.program = "Hoot";
        cardsbox.lexicon = LexData.getLexName();
        cardsbox.boxtype = boxtype;

        String cardfile = programData(cardsbox.program) + File.separator + cardsbox.lexicon + File.separator + cardsbox.boxtype + ".db";

        cardsDatabase = new CardDatabase(this, cardfile, null, 2);
        cards = cardsDatabase.getWritableDatabase();

        String sqlcmd;
        sqlcmd = "DELETE from questions;";
        cards.execSQL(sqlcmd);

        cards.close();
        populateCardTables();

    }
    private boolean deleteCBAlert(String boxtype) {
        String cardfile = programData("Hoot") + File.separator + LexData.getLexName() + File.separator + boxtype + ".db";
        if (!Utils.fileExist(cardfile))
            return true;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        StringBuilder msg = new StringBuilder();

        builder.setTitle("Are you sure you want to delete ALL cards in  " + boxtype + "?");
        builder.setPositiveButton("Yes. Delete now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteCB(boxtype);
                populateCardTables();
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

    protected View.OnClickListener newFile = new View.OnClickListener() {
        public void onClick(View v) {
            selectFile();
        }
    };
    public void selectFile() {
//        if (LexData.getUsername().equals("SAF")) {
        if (Utils.usingSAF()) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");

//            // Optionally, specify a URI for the file that should appear in the
//            // system file picker when it loads.
//            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

            startActivityForResult(intent, PICK_TXT_FILE);

        } else {


            if (!databaseAccess.permission(this))
                return;
            File mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
            FileDialog fileDialog = new FileDialog(CardBoxActivity.this, mPath, "txt");
            // only supports one file extension

            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    String full = file.getAbsolutePath();
                    importfile.setText(full);
                }
            });
            fileDialog.showDialog();
        }
    }

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

            int caret = etEntry.getSelectionStart();

            if (searchType.getSelectedItemPosition() != 2) { // not pattern
                enteredValue = enteredValue.toUpperCase();
                //^+[]&lt;&gt;(|){}\\~-
                String newValue = enteredValue.replaceAll("[\\[\\]/\\\\<>(|){}cv0123456789.,^+~-]", "");
//                String newValue = enteredValue.replaceAll("[\\[\\]<>cv0123456789.,^+~-]", "");

                if ( // searchType.getSelectedItemPosition() != 0 && // not anagram
                        searchType.getSelectedItemPosition() != 2) { // and not pattern
                    newValue = newValue.replaceAll("[*@]", "");
                }
                if (Arrays.asList(3,5,6,7).contains(searchType.getSelectedItemPosition())) {// contains, contains any, begins, ends
                    newValue = newValue.replaceAll("[?]", ""); // okay for anagrams
                }
                etEntry.setText(newValue);
                etEntry.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
                return;
            }

            else {
                StringBuilder modifiedValue = new StringBuilder(enteredValue);
                for (int i = 0; i < modifiedValue.length(); i++) {
                    if (Character.isLowerCase(modifiedValue.charAt(i)))
                        if (!(modifiedValue.charAt(i) == 'c' || modifiedValue.charAt(i) == 'v'))

                            modifiedValue.setCharAt(i, Character.toUpperCase(modifiedValue.charAt(i)));
                }
                etEntry.setText(modifiedValue);
                etEntry.setSelection(Math.min(modifiedValue.length(), caret)); // if first char is invalid
            }
        }
    };
    protected final AdapterView.OnItemSelectedListener typeConfig = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            // if not pattern
            if (searchType.getSelectedItemPosition() != 2) {
                String before = etEntry.getText().toString();
                String after = before.replaceAll("[\\[\\]<>cv*0123456789.,^+-]", "");
                etEntry.setText(after);

                mKeyboardView.setKeyboard( bKeyboard );
                if (searchType.getSelectedItemPosition() == 8) {
                    selectFile();
                    importfile.setVisibility(View.VISIBLE);
                    etEntry.setVisibility(View.INVISIBLE);
                }
                else {
                    importfile.setVisibility(View.INVISIBLE);
                    etEntry.setVisibility(View.VISIBLE);
                }
            }
            else {
                mKeyboardView.setKeyboard(pKeyboard);
                Toast.makeText(getApplicationContext(), "This search may take several minutes", Toast.LENGTH_LONG).show();
            }

            switch (searchType.getSelectedItemPosition() ) {
                case 0: // anagrams
                    break;
                case 1:
                    etEntry.setText("");
                    break;
                case 2:
                case 4: // build
                    Toast.makeText(getApplicationContext(),"Select lengths for quicker results", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener boxChange = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (cardtype.getSelectedItem().toString().equals("Lists")) {
                hideFilter();
                populateLists();
            }
            else {
                if (listAdapter != null) {
                    listAdapter.clear();
                    listAdapter.notifyDataSetChanged();
                }

//                if (!cardtype.getSelectedItem().toString().equals("Blank Anagrams"))
                showFilter();
//                else
//                    hideFilter();

                populateCardTables();
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener listChange = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            populateListTables();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    public static class BoxListAdapter extends SimpleCursorAdapter {
        protected Context mContext;
        protected int id;
        protected List<String> items;

        public BoxListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mContext = context;
            id = layout;
        }
        public void bindView(View v, Context context, Cursor c) {
            int box = c.getColumnIndex("cardbox");
            int count = c.getColumnIndex("total");

            String boxCol = c.getString(box);
            String countCol = c.getString(count);

            TextView boxView = v.findViewById(R.id.box);
                boxView.setText(boxCol);
            TextView countView = v.findViewById(R.id.boxcount);
                countView.setText(countCol);
        }

    }
    public static class SchListAdapter extends SimpleCursorAdapter {
        protected Context mContext;
        protected int id;
        protected List<String> items;

        public SchListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
            mContext = context;
            id = layout;
        }
        public void bindView(View v, Context context, Cursor c) {
            int sch = c.getColumnIndex("nextday");
            int count = c.getColumnIndex("total");

            String schCol = c.getString(sch);
            String countCol = c.getString(count);

            TextView boxView = v.findViewById(R.id.schedule);
            boxView.setText(schCol);
            TextView countView = v.findViewById(R.id.schedulecount);
            countView.setText(countCol);
        }

    }



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
                String fileUrl = String.valueOf(Uri.parse(uri.toString()));
//            String path = FileUtils(context).getPath(fileUrl);
//            Log.i("uri", uri.getPath());
//            Log.i("uri", uri.getEncodedPath());
//            Log.i("uri", uri.getPathSegments().toString());
                importfile.setText(uri.toString());
            }
        }
    }



    public Cursor getpatternThread(final Context context, final String pattern, final String filters, final String ordering){
        @SuppressLint("StaticFieldLeak")
        final AsyncTask<Void, Void, Cursor> task = new AsyncTask<Void, Void, Cursor>() {
            protected MatrixCursor matrixCursor;
            protected ProgressDialog dialog;

            @Override
            protected void onPreExecute()
            {
                this.dialog = Utils.themeDialog(context);
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
                        cancel(false);
                    }
                });
                this.dialog.setCanceledOnTouchOutside(false);
                this.dialog.show();
            }

            @Override
            protected Cursor doInBackground(Void... params)
            {
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

                        if (word.matches(exp)) {
                            matrixCursor.addRow(get_CursorRow(precursor));
                        }

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

            @Override
            protected void onPostExecute(Cursor result) //called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                cursor = matrixCursor;
            }

            @Override
            protected void onCancelled() //called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                dialog.cancel();
                cursor = matrixCursor;
            }
        };
        task.execute();
        return cursor;
    }
    public void addResults (Cursor results) {
        test.append("\n");
        while (results.moveToNext()) {
            String word = results.getString(results.getColumnIndex("question"));
            test.append(word + " ");
        }
        results.close();
    }
    public Cursor getWordBuild(final String term, final String filters, final String ordering) {
        Cursor precursor;
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
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

//                    if (LexData.getColorBlanks())
//                        matrixCursor.addRow(get_BlankCursorRow(precursor, blanks));
//                    else
                matrixCursor.addRow(get_CursorRow(precursor));
            }

        }
        precursor.close();
        return matrixCursor;
        //tcursor = databaseAccess.getCursor_subanagrams(term, filters, ordering);
    }
    protected String createQuizFilter(boolean anagrams) {
        String subquery = "";

        // vvvvvvv

        return subquery;
    }
    public String GetTextFile(String filespec) {

        List<String> words;
//        words = Utils.getWordsFromFile(importfile.getText().toString());
        words = Utils.getWordsFromFile(filespec);
        Log.d("words in file", "Total " + words.size());

        StringBuilder textlist = new StringBuilder();

        textlist.append("'" + words.get(0)+ "'");
        for(int c = 1; c < words.size(); c++) {
            textlist.append(", '" + words.get(c) + "'");
        }

        return textlist.toString();
//        cursor = databaseAccess.getCursor_getWords(textlist.toString(),"", "");
//        Log.d("words in file", "cursor " + cursor.getCount());

//        Create comma separated list as in AnagramQuizActivity and call getCursor_getWords(String list)

    }

    @Override public void onBackPressed(){
        if( isCustomKeyboardVisible() ) {
            hideCustomKeyboard();
            return;
        }

        super.onBackPressed();
    }
}