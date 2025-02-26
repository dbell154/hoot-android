package com.tylerhosting.hoot.hoot;

import static android.view.View.GONE;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CardMgmtActivity extends AppCompatActivity {

    CardDatabase cardDatabase;
    private SQLiteDatabase cards;
    TextView listtype, cardboxlexicon;
    Spinner cardtype, cardlist;
    RelativeLayout cardlistlayout;
    EditText etCards;
    Button delete, add, deletebox;
    String themeName;
    ArrayAdapter listAdapter;

    DatabaseAccess databaseAccess;

    // Support custom keyboard
    Keyboard bKeyboard; // basic
    KeyboardView mKeyboardView;

    int lastList;
    int lastbox;
    public Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        themeName = Utils.setStartTheme(this);
        setContentView(R.layout.activity_cardmgmt);

        cardboxlexicon = findViewById(R.id.cardboxlexicon);

        cardtype = findViewById(R.id.CardTypeSpinner);

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this,
                R.layout.spinselection, LexData.cardtypes);
        cardtype.setAdapter(dataAdapter);

        cardtype.setOnItemSelectedListener(boxChange);

        cardlist = findViewById(R.id.ListNameSpinner);
        cardlistlayout = findViewById(R.id.ListName);
        cardlist.setOnItemSelectedListener(listChange);

        listtype = findViewById(R.id.lqtype);

        ArrayAdapter<String> searchAdapter = new ArrayAdapter<String>(this,
                R.layout.simplespin, LexData.cardFilterText);

        etCards = findViewById(R.id.etCardList);
        delete = findViewById(R.id.deletecards);
        add = findViewById(R.id.add2cardbox);
        deletebox = findViewById(R.id.deletecardbox);

        etCards.addTextChangedListener(entryWatcher);
        delete.setOnClickListener(deleteClick);


        bKeyboard = new Keyboard(CardMgmtActivity.this,R.xml.judgekeyboard);

        // Lookup the KeyboardView
        mKeyboardView = (KeyboardView)findViewById(R.id.keyboardview);
        // Attach the keyboard to the view
        mKeyboardView.setKeyboard( bKeyboard );

        hidekeyboard();

        hideCustomKeyboard();

        // used by filter
        LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");
        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());

        databaseAccess.open();

    }

    protected View.OnClickListener deleteClick = new View.OnClickListener() {
        public void onClick(View v) {
            deleteAlert();
        }
    };

    private boolean deleteAlert() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);

        builder.setTitle("Are you sure?" );

        String xxxx = "xxxx";
        String msg = "Are you sure you want to delete these cards from " + xxxx + "?\n";
        builder.setMessage(msg + "Cards in the list will be reset to box 0.");

        builder.setPositiveButton("Yes. Merge now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                beginDeletions();
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
    private void beginDeletions() {
/*
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

*/

    }




    @Override
    protected void onResume(){
        super.onResume();

        if (Utils.themeChanged(themeName, this)) {
            Utils.setNewTheme(this);
            recreate();
        }
        if (cardtype.getSelectedItem().toString().equals("Lists")) {
            populateLists();
            cardlist.setSelection(lastList);
        }

        if (themeName == "Dark Theme") {
            cardtype.setBackgroundColor(Color.BLACK);
            cardlist.setBackgroundColor(Color.BLACK);
        }

        cardboxlexicon.setText(LexData.getLexName());
        if(LexData.getCustomkeyboard()) // setting
            useCustomKeyboard();
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

    protected final AdapterView.OnItemSelectedListener boxChange = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (cardtype.getSelectedItem().toString().equals("Lists")) {
                populateLists();
            }
            else {
                if (listAdapter != null) {
                    listAdapter.clear();
                    listAdapter.notifyDataSetChanged();
                }
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };
    protected final AdapterView.OnItemSelectedListener listChange = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    // this populates the spinner with list names
    public void populateLists() {
        String sql;

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

            while (cursor.moveToNext()) {
                listTitles.add(cursor.getString(index));
            }

            listAdapter = new ArrayAdapter<String>(this, R.layout.spinselection, listTitles);
            //listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listTitles);
            // listAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            cardlist.setAdapter(listAdapter);
            cards.close();
            // after list is determined, populate tables
        }
        else {
//            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
        }

    }

    // this populates the tables when list is selected
    int idNumber = 1;

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

        registerEditText(R.id.etCardList);
        hideCustomKeyboard();
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

            int caret = etCards.getSelectionStart();

                enteredValue = enteredValue.toUpperCase();
                //^+[]&lt;&gt;(|){}\\~-
                String newValue = enteredValue.replaceAll("[^A-Z \n]", "");
//                String newValue = enteredValue.replaceAll("[\\[\\]<>cv0123456789.,^+~-]", "");

                etCards.setText(newValue);
                etCards.setSelection(Math.min(newValue.length(), caret)); // if first char is invalid
                return;
        }
    };


    // keyboard watcher
    protected KeyboardView.OnKeyboardActionListener mOnKeyboardActionListener = new KeyboardView.OnKeyboardActionListener() {
        @Override public void onKey(int primaryCode, int[] keyCodes) {
            // Get the EditText and its Editable
            // Log.e("Key", Character.toString((char) primaryCode));

            View focusCurrent = CardMgmtActivity.this.getWindow().getCurrentFocus();
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

    @Override public void onBackPressed(){
        if( isCustomKeyboardVisible() ) {
            hideCustomKeyboard();
            return;
        }

        super.onBackPressed();
    }

}