package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class WordJudgeActivity extends AppCompatActivity {

    // todo scroll in entry
    DatabaseAccess databaseAccess;
    EditText etWords;
    Button judge;
    TextView judged, jtitle;
    Button clear;
    SharedPreferences shared;
    Keyboard jKeyboard; // basic
    KeyboardView mKeyboardView;
    public final static int CodeDelete   = -5; // Keyboard.KEYCODE_DELETE
    public final static int CodeEnter = 55009;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Utils.setStartTheme(this);

        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );

        setContentView(R.layout.activity_word_judge);

        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");

        // preferences
        shared = PreferenceManager.getDefaultSharedPreferences(this);
//need to save database and path, and reset when starting

        String full = shared.getString("database", "Internal");
        String db = "";
        if (full == null)
            full = "Internal";

        if (full == "Internal" || !full.contains(File.separator))  {
            Flavoring.addflavoring(getApplicationContext()); // sets database
            LexData.setDatabasePath(getApplicationContext(), null);
        }
        else {
//            db = full.substring(full.lastIndexOf(File.separator));
//            Log.i("ExtDatabase", db);
            LexData.setDatabase(full.substring(full.lastIndexOf(File.separator)));
            LexData.setDatabasePath(getApplicationContext(), full.substring(0, full.lastIndexOf(File.separator)));
        }
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();

        String lexicon = shared.getString("lexicon","");
        LexData.setLexicon(this, lexicon);

        populateResources();

        // Support custom keyboard
        jKeyboard = new Keyboard(WordJudgeActivity.this,R.xml.judgekeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
        mKeyboardView.setKeyboard( jKeyboard );

        hideCustomKeyboard();
        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();
//        useCustomKeyboard();
        hideCustomKeyboard();
        showkeyboard();
    }


    private View.OnClickListener wordJudge = new View.OnClickListener() {
        public void onClick(View v) {
            hideCustomKeyboard();
            adjudicate();
        }
    };
    private View.OnClickListener clearEdit = new View.OnClickListener() {
        public void onClick(View v) {
            clear();
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
            if (mWasEdited){
                mWasEdited = false;
                return;
            }
            mWasEdited = true;
            String enteredValue  = s.toString();

            int caret = etWords.getSelectionStart();
            enteredValue = enteredValue.toUpperCase();
            etWords.setText(enteredValue);
            etWords.setSelection(Math.min(enteredValue.length(), caret)); // if first char is invalid
            }
    };


    public void populateResources(){
        etWords = findViewById(R.id.txtWords);
        etWords.setBackgroundResource(R.drawable.edittext_border);
        etWords.addTextChangedListener(entryWatcher);
        etWords.setFilters(new InputFilter[]{new InputFilter.AllCaps()});

        judge = findViewById(R.id.btnJudge);
        judge.setOnClickListener(wordJudge);

        jtitle = findViewById(R.id.judgeTitle);
        jtitle.setTextColor(Color.WHITE);
        String title = "Hoot Word Judge " + com.tylerhosting.hoot.hoot.BuildConfig.VERSION_NAME;
        jtitle.setText(title);

        clear = findViewById(R.id.btnClear);
        clear.setOnClickListener(clearEdit);
        judged = findViewById(R.id.lblJudged);
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        etWords.setEnabled(true);

        TextView lexTitle = findViewById(R.id.lexName);
        lexTitle.setText(LexData.getLexName());
        TextView note = findViewById(R.id.lexNotice);
        note.setText(LexData.getLexicon().LexiconNotice);

        // Create a border programmatically
//        ShapeDrawable shape = new ShapeDrawable(new RectShape());
//        shape.getPaint().setColor(Color.RED);
//        shape.getPaint().setStyle(Paint.Style.STROKE);
//        shape.getPaint().setStrokeWidth(3);

        // Assign the created border to EditText widget
//        et2.setBackground(shape);

    }
    public void adjudicate() {
        boolean validWord = false;
        String txtWords = etWords.getText().toString().toUpperCase();
        Log.d("wds", txtWords);
        txtWords = txtWords.replaceAll(" ", "\r\n");
        Log.d("awds", txtWords);

        String[] userText = txtWords.split("\r?\n");
        databaseAccess.open();
        for (String s: userText) {
            if (s.length() == 0)
                    continue;
            s = s.trim();
            validWord = databaseAccess.wordJudge(s);
            if (validWord == false)
                break;
        }
        databaseAccess.close();

        if (validWord == true) {
            judged.setTextColor(Color.GREEN);
            judged.setText(String.format("%s %s", "Play is Acceptable in ", LexData.getLexName()));
        } else {
            judged.setTextColor(Color.RED);
            judged.setText(String.format("%s %s", "Play is NOT Acceptable in ", LexData.getLexName()));
        }
        etWords.setEnabled(false);
    }
    public void clear(){
        etWords.setText("");
        etWords.setEnabled(true);
        judged.setText("");
    }


    protected void useCustomKeyboard() {
        // Create the Keyboard

        // Do not show the preview balloons
        mKeyboardView.setPreviewEnabled(false);
        // Install the key handler
        mKeyboardView.setOnKeyboardActionListener(mOnKeyboardActionListener);


        // Hide the standard keyboard initially
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        registerEditText(R.id.txtWords);
        hideCustomKeyboard();
    }
    protected KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = WordJudgeActivity.this.getWindow().getCurrentFocus();
            if( focusCurrent==null || focusCurrent.getClass()!= AppCompatEditText.class ) return;
            EditText edittext = (EditText) focusCurrent;
            Editable editable = edittext.getText();
            int start = edittext.getSelectionStart();

            // Handle key

            if( primaryCode==CodeDelete ) {
                if( editable!=null && start>0 ) editable.delete(start - 1, start);
            } else if (primaryCode == CodeEnter) {
                editable.append('\n');
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

    boolean doubleBackToExitPressedOnce = false;
    @Override public void onBackPressed(){
        if( isCustomKeyboardVisible() ) {
            hideCustomKeyboard();
            return;
        }
        if (isTaskRoot()) {
            if (etWords.getText().toString().length() > 0) {
                clear();
                Toast.makeText(this, "Press Back Again to Exit", Toast.LENGTH_LONG).show();
            } else
                exitAlert();
        }
        else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;

                }
            }, 2000);
        }
//            super.onBackPressed();
    }

    private boolean exitAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);
//        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this, LexData.getDatabasePath(), LexData.getDatabase());

        builder.setTitle("Do you want to quit Word Judge ??");
        builder.setPositiveButton("Yes. Quit now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                databaseAccess.close();
                finishAffinity();
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
