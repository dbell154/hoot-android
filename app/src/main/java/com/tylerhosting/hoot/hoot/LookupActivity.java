package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class LookupActivity extends AppCompatActivity {
    DatabaseAccess databaseAccess;
    Button search;
    EditText etEntry;
    TextView status, results;
    Keyboard basicKeyboard; // basic
    KeyboardView mKeyboardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lookup);

        Utils.setStartTheme(this);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );


        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        populateResources();

        // Support custom keyboard
        basicKeyboard = new Keyboard(LookupActivity.this,R.xml.basickeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
        mKeyboardView.setKeyboard( basicKeyboard );

        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();
//        useCustomKeyboard();
        hideCustomKeyboard();
        showkeyboard();
//        CharSequence text = getIntent().getCharSequenceExtra((Intent.EXTRA_PROCESS_TEXT));
//        if (text.length()>1) {
//            text = text.toString().toUpperCase();
//            etEntry.setText(text);
//            executeSearch();
//        }

    }
    private View.OnClickListener doSearch = new View.OnClickListener() {
        public void onClick(View v) {
            executeSearch();
        }
    };
    private final TextView.OnEditorActionListener entryAction = new TextView.OnEditorActionListener() {
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

    private final TextWatcher entryWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        private boolean mWasEdited = false;
        private int delChars = 0;
        @Override
        public void afterTextChanged(Editable s) {
            if (mWasEdited){
                mWasEdited = false;
                return;
            }
            mWasEdited = true;
            String enteredValue  = s.toString();


            int caret = etEntry.getSelectionStart();
            enteredValue = enteredValue.toUpperCase();
            //^+[]&lt;&gt;(|){}\\~-
            String newValue = enteredValue.replaceAll("[ \n?*]", "");

            etEntry.setText(newValue);
            etEntry.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
        }
    };

    private void executeSearch() {
        status.setText(R.string.searching);
        String word = etEntry.getText().toString().toUpperCase();
        if (word.isEmpty())
            return;
        results.setMovementMethod(new ScrollingMovementMethod());
        results.setText(word + " ");
        Toast.makeText(this, "Searching. . .", Toast.LENGTH_SHORT).show();

        try {
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {

        }

        databaseAccess.open();

        if (!databaseAccess.wordJudge(word)) {
            String notice = word + " isn't a valid word in this lexicon.\r\n";
            results.setText(notice);
        }


        String def = databaseAccess.getDefinition(word);
        results.append(def);

        String lex = databaseAccess.getValidLexicons(word);
        results.append(lex);

        etEntry.setText("");
        databaseAccess.close();
        hideCustomKeyboard();
        }
    private void populateResources() {
        search = (Button) findViewById(R.id.btnLookup);
        search.setOnClickListener(doSearch);

        results = findViewById(R.id.txtResults);
        status = findViewById(R.id.lblStatus);
        etEntry = findViewById(R.id.etWord);
        etEntry.addTextChangedListener(entryWatcher);
        etEntry.setOnEditorActionListener(entryAction);
        etEntry.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        TextView lexTitle = findViewById(R.id.lexName);
        lexTitle.setText(LexData.getLexName());
        TextView note = findViewById(R.id.lexNotice);
        note.setText(LexData.getLexicon().LexiconNotice);
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

        registerEditText(R.id.etWord);
        hideCustomKeyboard();
    }
    // keyboard watcher
    protected KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = LookupActivity.this.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();

            // Handle key

            if( primaryCode==CodeSearch ) {
                if (editable != null)
                    executeSearch();
            }
            else if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);
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
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
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
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

}
