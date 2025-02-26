package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.Guideline;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
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
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RecallQuizActivity extends SlidesActivity {

    MatrixCursor matrixCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                anagramList);

        definition.setVisibility(View.GONE);

        word.setVisibility(View.GONE);
        buttonlayout.setVisibility(View.GONE);

        next2.setVisibility(View.GONE);
        next2.setOnClickListener(startOver);

        start.setVisibility(View.GONE);
        etSeconds.setVisibility(View.GONE);
        textSeconds.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        quit.setVisibility(View.GONE);

        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        first.setVisibility(View.GONE);
        last.setVisibility(View.GONE);

        lv.setVisibility(View.VISIBLE);
        review.setOnClickListener(showAnagrams);
        review.setVisibility(View.VISIBLE);


        entry.setVisibility(View.VISIBLE);
        entry.addTextChangedListener(entryWatcher);
        entry.setOnEditorActionListener(entryAction);

        entry.setTextColor(Color.BLUE);
        answer.setVisibility(View.INVISIBLE);

        // Support custom keyboard
        qKeyboard = new Keyboard(RecallQuizActivity.this,R.xml.quizkeyboard);
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        mKeyboardView.setKeyboard( qKeyboard );
//        hideCustomKeyboard();
    }

    @Override
    protected void onResume(){
        super.onResume();
//        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();
            showCustomKeyboard(entry);

    }

    protected void loadBundle() {
        Intent intentExtras = getIntent();

        Bundle extrasBundle = ((Intent) intentExtras).getExtras();
        if (extrasBundle.isEmpty())
            return;

        String desc = extrasBundle.getString("desc");
//        String search = extrasBundle.getString("search");
        String fulldesc = "Recall Quiz: " + desc;
        header.setText(fulldesc);

        String[] inbound = extrasBundle.getStringArray("Words");
        if (inbound.length == 0)
            return;

        for (int wordId = 0; wordId < inbound.length; wordId++)
            inbound[wordId] = inbound[wordId].toUpperCase();


        anagramList = new ArrayList<String>(Arrays.asList(inbound));

        matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

        String initStatus = "List Recall: Quizzing for " + anagramList.size() + " words in " + LexData.getLexName();
        status.setText(initStatus);
    }

    // Listeners
    protected View.OnClickListener showAnagrams = new View.OnClickListener() {
        public void onClick(View v) {
            SpannableStringBuilder sb = new SpannableStringBuilder();

            List missing = new ArrayList();
            for(int c = 0; c < anagramList.size(); c++)
                missing.add(anagramList.get(c));

            for(int c = 0; c < answerList.size(); c++)
                missing.remove(answerList.get(c));

            databaseAccess.open();

            if (answerList.size() < anagramList.size()) {
                StringBuilder missed = new StringBuilder();
                missed.append("'" + missing.get(0) + "'");
                for (int c = 1; c < missing.size(); c++) {
                    missed.append(", '" + missing.get(c) + "'");
                }

                Cursor cursor = databaseAccess.getCursor_getWords(missed.toString(),"", "", "");
                while (cursor.moveToNext())
                    matrixCursor.addRow(databaseAccess.get_RedCursorRow(cursor));
            }

            if (LexData.getShowQuixHooks()) {
                cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.mclistitemunscored, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            }
            else {
                cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.sclistitem, matrixCursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            }

            ListView lv = findViewById(R.id.lv);
            lv.setAdapter(cursorAdapter);


            for(int c = 0; c < missing.size(); c++) {
                sb.append("<font color='#ff0000'>" + missing.get(c) + "</font><br/>");
            }
            databaseAccess.close();

            definition.append(SpannableString.valueOf(Html.fromHtml(sb.toString())));

            hidekeyboard();
            String scoreStatus = "Recalled " + answerList.size() + " correctly out of " + anagramList.size() + " words in " + LexData.getLexName();
            status.setText(scoreStatus);
            entry.setEnabled(false);
            answerList.clear();


            for(int c = 0; c < incorrectList.size(); c++) {
                incorrect.append( "   " + incorrectList.get(c));
            }
            incorrect.setVisibility(View.VISIBLE);



            review.setVisibility(View.GONE);
            next2.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                next2.setImageDrawable(getResources().getDrawable(R.drawable.ic_first_dark, getApplicationContext().getTheme()));
            } else {
                next2.setImageDrawable(getResources().getDrawable(R.drawable.ic_first_dark));
            }
        }
    };
    protected View.OnClickListener startOver = new View.OnClickListener() {
        public void onClick(View v) {
            lv.setAdapter(null);
            matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                    "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
            entry.setEnabled(true);
            review.setVisibility(View.VISIBLE);
            next2.setVisibility(View.GONE);
            incorrect.setVisibility(View.GONE);

            String initStatus = "List Recall: Quizzing for " + anagramList.size() + " words in " + LexData.getLexName();
            status.setText(initStatus);
        }
    };
    private final TextView.OnEditorActionListener entryAction = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                addAnswer(entry.getText().toString());
                entry.setText("");
                return true;
            }
            return false;
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
                entry.setText("");
            }

            return;
        }
    };


    private boolean addAnswer(String attempt) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        attempt = attempt.toUpperCase().trim();
        Log.e("Entered", attempt);
        for(int c = 0; c < anagramList.size(); c++) {

            if (anagramList.get(c).equals(attempt)) {

                Log.e("Entered", attempt);
                databaseAccess.open();

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

                Log.e("Find", attempt);
                Cursor addition = databaseAccess.getCursor_findWord(attempt);
                if (addition.getCount() > 0) {
                    addition.moveToFirst();
                    matrixCursor.addRow(databaseAccess.get_CursorRow(addition));
                }

                Log.e("Addition", addition.getString(1));

                if (LexData.getShowQuixHooks()) {
                    cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.mclistitemunscored, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }
                else {
                    cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.sclistitem, matrixCursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                }

                ListView lv = findViewById(R.id.lv);
                lv.setAdapter(cursorAdapter);
                databaseAccess.close();

                if (equalLists(anagramList, answerList)) {
                    Log.d("Lists", anagramList.toString() + " " + answerList.toString());
                    Toast.makeText(this, "Answers Shown", Toast.LENGTH_LONG).show();
                }

                entry.setText("");


                String scoreStatus = "Recalled " + answerList.size() + " correctly out of " + anagramList.size() + " words in " + LexData.getLexName();
                status.setText(scoreStatus);


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


    // KEYBOARD SUPPORT
    private void useCustomKeyboard() {
        mKeyboardView.setPreviewEnabled(false);
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        registerEditText(R.id.entry);
        hideCustomKeyboard();
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
    private KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = RecallQuizActivity.this.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();
            // Handle key

            if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);
            } else if( primaryCode== CodeDone
//                    || primaryCode == KeyEvent.KEYCODE_ENTER
            ) {
                addAnswer(entry.getText().toString());
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

    @Override public void onBackPressed(){
        if( isCustomKeyboardVisible() ) {
            hideCustomKeyboard();
            return;
        }
        super.onBackPressed();
    }
}



