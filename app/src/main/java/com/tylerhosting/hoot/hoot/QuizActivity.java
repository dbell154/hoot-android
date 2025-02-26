package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.constraint.Guideline;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class QuizActivity extends SlidesActivity {

    String searchType;
    Bundle extrasBundle;
    int totalCorrect = 0;
    double screenwidth;
    float dX, dY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        etSeconds.setText("60");

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                anagramList);

        word.addTextChangedListener(wordWatcher);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            definition.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        definition.setTextSize(24);
        definition.setTypeface(listfont);
        definition.setVisibility(View.GONE);

        lv.setVisibility(View.VISIBLE);

        next2.setOnClickListener(goNext);
        next2.setVisibility(View.GONE);

        review.setOnClickListener(showAnagrams);
        review.setVisibility(View.VISIBLE);

        start.setVisibility(View.GONE);
        etSeconds.setVisibility(View.GONE);
        textSeconds.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        quit.setVisibility(View.GONE);

        incorrect.setText("");

        entry.setVisibility(View.VISIBLE);
        entry.addTextChangedListener(entryWatcher);
        entry.setOnEditorActionListener(entryAction);


//        entry.setOnEditorActionListener(entryListener);
        if (Utils.getTheme(this) == "Dark Theme")
            entry.setTextColor(Color.BLUE);
        else
            entry.setTextColor(Color.BLUE);

        answer.setVisibility(View.INVISIBLE);
        answerCount.setVisibility(View.VISIBLE);

        // Support custom keyboard
        qKeyboard = new Keyboard(QuizActivity.this,R.xml.quizkeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
        mKeyboardView.setKeyboard( qKeyboard );

//        hidekeyboard();
    }

    @Override
    protected void onResume(){
        super.onResume();
        // force  use of custom keyboard
//        LexData.setCustomkeyboard(true);
//        if(LexData.getCustomkeyboard()) // setting

        if (Utils.getTheme(this) .equals("Dark Theme")) {
            incorrect.setTextColor(Color.WHITE);
        }
        else {
            incorrect.setTextColor(Color.BLACK);
        }
        incorrect.setVisibility(View.VISIBLE);

        useCustomKeyboard();

        if (flashcards) {
            if (!(header.getText().toString().endsWith("Flashcards")))
                header.append(" : Flashcards");
            final float[] firstX_point = new float[1];
            final float[] firstY_point = new float[1];
            sv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View arg0, MotionEvent event) {

                    int action = event.getAction();

                    switch (action) {

                        case MotionEvent.ACTION_DOWN:
                            firstX_point[0] = event.getRawX();
                            firstY_point[0] = event.getRawY();
                            break;

                        case MotionEvent.ACTION_UP:

                            float finalX = event.getRawX();
                            float finalY = event.getRawY();

                            int distanceX = (int) Math.abs(finalX - firstX_point[0]);
                            int distanceY = (int) Math.abs(finalY - firstY_point[0]);

                            if (distanceX < 50 && distanceY < 50) {
                                showFlashList();
                                return true;
                            }

                            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                                if ((firstX_point[0] < finalX)) {
                                    Log.d("Test", "Left to Right swipe performed");
                                } else {
                                    next2.performClick();
                                    Log.d("Test", "Right to Left swipe performed");
                                }
                                return true;
                            } else {
                                if ((firstY_point[0] < finalY)) {
                                    swipeIncorrect();
                                    // show check
//                            pause(5);
//                            next2.performClick();
                                    Log.d("Test", "Up to Down swipe performed");
                                } else {
                                    swipeCorrect();
                                    // show x
//                            pause(5);
//                            next2.performClick();
                                    Log.d("Test", "Down to Up swipe performed");
                                }
                            }
                            break;
                    }
                    return true;
                }
            });
        }

listfontsize = (int) (listfontsize * 1.3);

    }
    protected void loadBundle() {
        Intent intentExtras = getIntent();

        extrasBundle = ((Intent) intentExtras).getExtras();
        if (extrasBundle.isEmpty())
            return;

        searchType = extrasBundle.getString("search");
        String desc = extrasBundle.getString("desc");
        header.setText("Quizzing " + searchType + ": " + desc);

        String[] inbound = extrasBundle.getStringArray("Words");
        wordlist = new SpannableString[inbound.length];
        if (inbound.length == 0)
            return;

        Log.e("Check", String.valueOf(wordlist[0]));

        // make all uppercase
        for (int wordId = 0; wordId < inbound.length; wordId++)
            wordlist[wordId] = SpannableString.valueOf(inbound[wordId].toUpperCase());

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spincenter, inbound);
        selector.setAdapter(dataAdapter);
        selector.setOnItemSelectedListener(selection);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenwidth = size.x;
        int height = size.y;
    }

    // KEYBOARD SUPPORT
    private void useCustomKeyboard() {
        // Create the Keyboard

        // Do not show the preview balloons
        mKeyboardView.setPreviewEnabled(false);
        // Install the key handler
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);


        // Hide the standard keyboard initially
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        registerEditText(R.id.entry);
        hidekeyboard();
    }
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
    @SuppressLint("ClickableViewAccessibility")
    public void registerEditText(int resid) {
        // Find the EditText 'resid'
        EditText edittext= (EditText)findViewById(resid);
        // Make the custom keyboard appear
        edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if( hasFocus ) showkeyboard(); else hidekeyboard();
            }
        });
        edittext.setOnClickListener(new View.OnClickListener() {
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
    public void showkeyboard() {
        View view = this.getCurrentFocus();
        if (LexData.getCustomkeyboard())
            showCustomKeyboard(view);
        else {
            hideCustomKeyboard();
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
            );
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                imm.showSoftInput(view,0);
            }
        }
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
//            getWindow().setSoftInputMode(
//                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
//            );
    }

    // Listeners
    protected View.OnClickListener showAnagrams = new View.OnClickListener() {
        public void onClick(View v) {
            if (incorrectList.size() > 0)
                for(int c = 0; c < incorrectList.size(); c++) {
                    incorrect.append( "   " + incorrectList.get(c));
                }
            incorrect.setVisibility(View.VISIBLE);

            if (anagramList.size() == 0) {
                next2.setVisibility(View.VISIBLE);
                return;
            }



            List missing = new ArrayList();
            for(int c = 0; c < anagramList.size(); c++)
                missing.add(anagramList.get(c));

            for(int c = 0; c < answerList.size(); c++)
                missing.remove(answerList.get(c));

            StringBuilder missed = new StringBuilder();
            missed.append("'" + missing.get(0)+ "'");
            for(int c = 1; c < missing.size(); c++) {
                missed.append(", '" + missing.get(c) + "'");
            }

            databaseAccess.open();
            Cursor cursor = databaseAccess.getCursor_getWords(missed.toString(),"", "", "");
            while (cursor.moveToNext())
                matrixCursor.addRow(databaseAccess.get_RedCursorRow(cursor));


            if (LexData.getShowQuixHooks()) {
                cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.mclistitemunscored, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            }
            else {
                cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.sclistitem, matrixCursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            }

            ListView lv = findViewById(R.id.lv);
            lv.setAdapter(cursorAdapter);
            databaseAccess.close();



            SpannableStringBuilder sb = new SpannableStringBuilder();
            for(int c = 0; c < missing.size(); c++) {
                sb.append("<font color='#ff0000'>" + missing.get(c) + "</font><br/>");
            }
            definition.append(SpannableString.valueOf(Html.fromHtml(sb.toString())));


            next2.setVisibility(View.VISIBLE);
            hidekeyboard();
            String scoreStatus = "Anagram " + (currentID + 1)  + "/" + wordlist.length + ": Solved " + answerList.size() + " out of " + anagramList.size() + " words";
            status.setText(scoreStatus);

        }
    };
    private final TextView.OnEditorActionListener entryAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
//            if (actionId == EditorInfo.IME_NULL
  //                  && event.getAction() == KeyEvent.ACTION_DOWN) {
                addAnswer(entry.getText().toString());

//                if (addAnswer(answer.getText().toString()))
                //                  answer.setTextColor(Color.GREEN);
                //            else
                //              answer.setTextColor(Color.RED);
                entry.setText("");
                return true;
            }
            return false;
        }
    };
    private final TextWatcher wordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            databaseAccess.open();
            definition.setText("");
            answer.setText("");
            answer.setVisibility(View.VISIBLE);
            answerList.clear();

            lv.setAdapter(null);

            next2.setVisibility(View.GONE);

            // prep next list
            String term = wordlist[currentID].toString();
//            anagramList = databaseAccess.justAnagrams(wordlist[currentID].toString());

            // CHANGE FOR DIFFERENT SEARCH TYPES
            anagramList = databaseAccess.justblankAnagrams(wordlist[currentID].toString());



            Cursor cursor;
            switch (searchType) {
//                case "Recall" :
//                text never changes
                case "Hook Words" :
                    cursor = databaseAccess.getCursor_hookwords(wordlist[currentID].toString(), "", "", "",false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Blank Anagrams":
                    cursor = databaseAccess.getCursor_blankAnagrams(wordlist[currentID].toString() + "?", "", "", 0,0, false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;


/*                case "Word Builder":
                    cursor = databaseAccess.getCursor_subanagrams(wordlist[currentID].toString(), "", "", false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Contains":
                    cursor = databaseAccess.getCursor_contains(wordlist[currentID].toString(), "", "", false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Contains All":
                    cursor = databaseAccess.getCursor_superanagrams(wordlist[currentID].toString(), "", "", false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;

                case "Contains Any":
                    cursor = databaseAccess.getCursor_containsAny(wordlist[currentID].toString(), "", "", false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Subwords":
                    cursor = databaseAccess.getCursor_subwords(2, 9, wordlist[currentID].toString());
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Begins With":
                    cursor = databaseAccess.getCursor_begins(wordlist[currentID].toString(), "", "", false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Ends With":
                    cursor = databaseAccess.getCursor_ends(wordlist[currentID].toString(), "", "", false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
 */
                default:
                    break;
            }


            if (anagramList.size() == 0) {
                shorttoastMsg("There are no answers for '" + wordlist[currentID].toString() + "'; skipping");
                skipWord();
                return;
            }



            // calculate width based on word length
            int letterswide = word.length();
            if (letterswide < 7)
                letterswide = 7;

            AssetManager assetManager = getAssets();
            tile = Typeface.createFromAsset(assetManager, "fonts/nutiles.ttf");

            String initStatus = "Anagram " + (currentID + 1)  + "/" + wordlist.length + ": Quizzing for " + anagramList.size() + " words in " + LexData.getLexName();
            status.setText(initStatus);

            answerCount.setText("0/" + anagramList.size());

            selector.setSelection(currentID);


            // THIS SHOWS CURRENT, NEED TO SHOW WHEN ANSWERING


            databaseAccess.close();


            adapter.notifyDataSetChanged();

            matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                    "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

//            if (LexData.getCustomkeyboard())
//                showCustomKeyboard(entry);
            showkeyboard();
        }
    };
    private final TextWatcher entryWatcher = new TextWatcher() {
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
            int caret = entry.getSelectionStart();

            entry.setText(s.toString());
            entry.setSelection(Math.min(s.length(), caret));

            //enter pressed
            if (s.length()>0 && s.subSequence(s.length()-1, s.length()).toString().equalsIgnoreCase("\n")) {
                addAnswer(entry.getText().toString());

//                if (addAnswer(answer.getText().toString()))
  //                  answer.setTextColor(Color.GREEN);
    //            else
      //              answer.setTextColor(Color.RED);
                entry.setText("");
            }



            return;

        }
    };
    private KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = QuizActivity.this.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();
            // Handle key

            if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);

//                case Keyboard.KEYCODE_DONE:
                //                  ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                //                break;

            } else if( primaryCode== CodeDone
//                    || primaryCode == KeyEvent.KEYCODE_ENTER
            ) {
                addAnswer(entry.getText().toString());

                // need to set listview item instead
//                if (addAnswer(entry.getText().toString()))
//                    entry.setTextColor(Color.GREEN);
//                else
//                    entry.setTextColor(Color.RED);
                //
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

    private void skipWord() {
        if (currentID < wordlist.length - 1) {
            currentID = currentID + 1;
            word.setText(wordlist[currentID]);
        }
        else {
            longtoastMsg("End of List");
        }
    }
    private boolean addAnswer(String attempt) {
        attempt = attempt.toUpperCase().trim();
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for(int c = 0; c < anagramList.size(); c++) {

            if (anagramList.get(c).equals(attempt)) {

                Log.e("Add to Definition", attempt);

                if (Utils.getTheme(this) == "Dark Theme")
                    sb.append("<font color='#00ff00'>" + attempt + "</font><br/>");
                else
                    sb.append("<font color='#00aa00'>" + attempt + "</font><br/>");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    definition.append(SpannableString.valueOf(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)));
                }
                else {
                    definition.append(SpannableString.valueOf(Html.fromHtml(sb.toString())));
                }

                answerList.add(attempt.toString());


                databaseAccess.open();
                Cursor addition = databaseAccess.getCursor_findWord(attempt);
                if (addition.getCount() > 0) {
                    addition.moveToFirst();
                    matrixCursor.addRow(databaseAccess.get_CursorRow(addition));
                }

                if (LexData.getShowQuixHooks()) {
                    cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.mclistitemunscored, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                else {
                    cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.sclistitem, matrixCursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }

                lv.setAdapter(cursorAdapter);

                databaseAccess.close();

                List<String> correct = new ArrayList<String>(new HashSet<String>(answerList));
                answerCount.setText(correct.size() + "/" + anagramList.size());

                if (equalLists(anagramList, answerList)) {
                    if (incorrectList.size() > 0)
                        for(int i = 0; i < incorrectList.size(); i++) {
                            incorrect.append( "   " + incorrectList.get(i));
                        }
                    else
                        totalCorrect++;

                    incorrect.setVisibility(View.VISIBLE);


                    if (LexData.AutoAdvance) {
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                next2.performClick();
                            }
                        }, 1000);
                    }
                    else {
                        next2.setVisibility(View.VISIBLE);
                        hidekeyboard();
                    }
                    incorrectList.clear();
                }

                String scoreStatus = "Anagram " + (currentID + 1)  + "/" + wordlist.length + ": Solved " + answerList.size() + " out of " + anagramList.size() + " words";
                status.setText(scoreStatus);
                entry.setText("");
                return true;
            }
        }
        incorrectList.add(attempt);
        entry.setText("");
        return false;
    }
    public  boolean equalLists(List<String> a, List<String> b){
        // Check for sizes and nulls

//        if (a == null && b == null) return true;

//        if ((a == null && b!= null) || (a != null && b== null) || (a.size() != b.size()))
//        {
//            return false;
//        }

        // Sort and compare the two lists
        Collections.sort(a);
        Collections.sort(b);

        return (b.containsAll(a));
        //return a.equals(b);
    }
    private void showFlashList() {
        databaseAccess.open();

//        add all words to answerList

        StringBuilder wordlist = new StringBuilder();
        wordlist.append("'" + anagramList.get(0)+ "'");
        for(int c = 1; c < anagramList.size(); c++) {
            wordlist.append(", '" + anagramList.get(c) + "'");
        }

        // restart with empty cursor
        matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        databaseAccess.open();
        Cursor addition = databaseAccess.getCursor_getWords(wordlist.toString(),"", "", "");
        while (addition.moveToNext())
            matrixCursor.addRow(databaseAccess.get_CursorRow(addition));


/*        for(int c = 0; c < anagramList.size(); c++) {
            Log.e("Word", anagramList.get(c));


            Cursor addition = databaseAccess.getCursor_findWord(anagramList.get(c));


            if (addition.getCount() > 0) {
                addition.moveToFirst();
                matrixCursor.addRow(databaseAccess.get_CursorRow(addition));
            }
        }

 */

        if (LexData.getShowQuixHooks()) {
            cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.mclistitemunscored, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }
        else {
            cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.sclistitem, matrixCursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }

        lv.setAdapter(cursorAdapter);

        databaseAccess.close();
//        updateShown = true;

//        next2.setVisibility(View.VISIBLE);

    }
    private void swipeCorrect() {
//        if (LexData.AutoAdvance) {
//            next2.performClick();
//        }
//        else
//            next2.setVisibility(View.VISIBLE);
//
//
//        String last = "";
//        if (usingList) {
//            updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.forward.ordinal());
//            totalCorrect++;
//            if (beforeListWord.last_correct == 0) last = "Never";
//            else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));
//
//            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
//                    updateListWord.question, beforeListWord.cardbox, updateListWord.cardbox,
//                    dateFormat.format(CardUtils.dtDate(updateListWord.next_scheduled))) +
//                    String.format("Total: %s  Streak:%s  Last Correct: %s",
//                            updateListWord.correct, updateListWord.streak, last);
//            status.setText(scoreStatus);
//        }
//        else {
//            update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.forward.ordinal());
//            totalCorrect++;
//            if (before.last_correct == 0) last = "Never";
//            else last = dateFormat.format(CardUtils.dtDate(before.last_correct));
//
//            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
//                    update.question, before.cardbox, update.cardbox,
//                    dateFormat.format(CardUtils.dtDate(update.next_scheduled))) +
//                    String.format("Total: %s  Streak:%s  Last Correct: %s",
//                            update.correct, update.streak, last);
//            status.setText(scoreStatus);
//        }
//        updateShown = true;

//        if (currentID < wordlist.length - 1) {
//            currentID = currentID + 1;
//            word.setText(wordlist[currentID]);
//        }
//        else {
//            toastMsg("End of List");
//            finalExamGrade();
//        }
        totalCorrect++;

        incorrectList.clear();
        incorrect.setText("");
        entry.setText("");
        correct.setVisibility(View.VISIBLE);


        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                correct.setVisibility(View.GONE);
                if (LexData.AutoAdvance) {
                    next2.performClick();
                }
                else
                    next2.setVisibility(View.VISIBLE);
            }
        }, 1000);

//        next2.setVisibility(View.VISIBLE);




//        android.os.SystemClock.sleep(3000);


//        new CountDownTimer(1000 * 5, 3000) {
//
//            public void onTick(long millisUntilFinished) {
//                //mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
//            }
//
//            public void onFinish() {
//                //mTextField.setText("done!");
//            }
//        }.start();





//        final Handler handler = new Handler();
//        Timer t = new Timer();
//        t.schedule(new TimerTask() {
//            public void run() {
//                handler.post(new Runnable() {
//                    public void run() {
//                        next2.setVisibility(View.VISIBLE);
//                    }
//                });
//            }
//        }, 5000);


    }
    private void swipeIncorrect() {
//        review.performClick();
//
//        String last = "";
//        if (usingList) {
//            updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());
//            if (beforeListWord.last_correct == 0) last = "Never";
//            else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));
//
//            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
//                    updateListWord.question, beforeListWord.cardbox, updateListWord.cardbox,
//                    dateFormat.format(CardUtils.dtDate(updateListWord.next_scheduled))) +
//                    String.format("Total: %s  Streak:%s  Last Correct: %s",
//                            updateListWord.correct, updateListWord.streak, last);
//            status.setText(scoreStatus);
//        }
//        else {
//            update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());
//            if (before.last_correct == 0) last = "Never";
//            else last = dateFormat.format(CardUtils.dtDate(before.last_correct));
//
//            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
//                    update.question, before.cardbox, update.cardbox,
//                    dateFormat.format(CardUtils.dtDate(update.next_scheduled))) +
//                    String.format("Total: %s  Streak:%s  Last Correct: %s",
//                            update.correct, update.streak, last);
//            status.setText(scoreStatus);
//        }
//        updateShown = true;
//        if (currentID < wordlist.length - 1) {
//            currentID = currentID + 1;
//            word.setText(wordlist[currentID]);
//        }
//        else {
//            toastMsg("End of List");
//            finalExamGrade();
//        }
        incorrectList.clear();
        incorrect.setText("");
        entry.setText("");
        wrong.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wrong.setVisibility(View.GONE);
                if (LexData.AutoAdvance) {
                    next2.performClick();
                }
                else
                    next2.setVisibility(View.VISIBLE);
            }
        }, 1000);

//        next2.setVisibility(View.VISIBLE);

    }


    @Override public void onBackPressed(){
        if( isCustomKeyboardVisible() ) {
            hidekeyboard();
            return;
        }
        exitAlert();
    }
    private boolean exitAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        StringBuilder msg = new StringBuilder();
        msg.append("You got ");
        msg.append(totalCorrect);
        msg.append(" correct out of ");
        msg.append(wordlist.length);

        builder.setMessage(msg);

        builder.setTitle("Do you want to quit this quiz ??");
        builder.setPositiveButton("Yes. Quit now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
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

}


