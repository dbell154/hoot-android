package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import static android.view.View.GONE;

public class AnagramQuizActivity extends SlidesActivity {

// hmmm doesn't keep score
// still need incorrect display
    // where to add to incorrectList
    // how to use updateShown
// hook words doesn't clear incorrect list

    int buttonWidth; // in pixels
    int fontWidth; // in sp
    int totalCorrect = 0;
    double screenwidth;
    float buttonStart;
    float buttonBottom;
    float dX, dY;
    Button[] buttons = new Button[15];
    GestureDetector gestureDetector;
    Bundle extrasBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                anagramList);

        word.setVisibility(View.INVISIBLE);
        word.addTextChangedListener(wordWatcher);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            definition.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        definition.setTextSize(24);
        definition.setTypeface(listfont);
        definition.setVisibility(View.GONE);

        dragger = shared.getBoolean("testdrag",false);

        // create buttons and add to layout; all hidden until word changes
        for(int i=0; i< buttons.length;i++) {
            buttons[i] = new Button(this);
            buttonlayout.addView(buttons[i]);
            buttons[i].setVisibility(View.GONE);
        }

        shuffle.setOnClickListener(doShuffle);
        shuffle.setVisibility(View.VISIBLE);

        alpha.setOnClickListener(doAlphagram);
        alpha.setVisibility(View.VISIBLE);

        next2.setOnClickListener(goNext);
        next2.setVisibility(View.GONE);

        review.setOnClickListener(showAnagrams);
        review.setVisibility(View.VISIBLE);

        start.setVisibility(View.GONE);
        etSeconds.setText("60");
        etSeconds.setVisibility(View.GONE);
        textSeconds.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        quit.setVisibility(View.GONE);

        incorrect.setText("");
        answerCount.setVisibility(View.VISIBLE);

        aligner=findViewById(R.id.btnAlign);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());
    }

    @Override
    protected void onResume(){
        super.onResume();
//        if (Utils.getTheme(this) .equals("Dark Theme")) {
//            answer.setTextColor(Color.WHITE);
//            incorrect.setTextColor(Color.WHITE);
//        }
//        else {
//            answer.setTextColor(Color.BLACK);
//            incorrect.setTextColor(Color.BLACK);
//        }
        incorrect.setVisibility(View.VISIBLE);

        for (int c = 0; c < 15; c++)
            setButtonColor(buttons[c]);

        if (screenwidth < 1200)
            status.setTextSize(12);

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
//                            Log.d("Points", Float.toString(firstX_point[0]) + "," + Float.toString(firstY_point[0]));
                            break;

                        case MotionEvent.ACTION_UP:

                            float finalX = event.getRawX();
                            float finalY = event.getRawY();
//                            Log.d("Points", Float.toString(finalX) + "," + Float.toString(finalY));

                            int distanceX = (int) Math.abs(finalX - firstX_point[0]);
                            int distanceY = (int) Math.abs(finalY - firstY_point[0]);
//                            Log.d("Points", Integer.toString(distanceX) + "," + Integer.toString(distanceY));

                            if (distanceX < 50 && distanceY < 50) {
                                showFlashList();
                                return true;
                            }

                            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                                if ((firstX_point[0] < finalX)) {
                                    Log.d("Test", "Left to Right swipe performed");
                                } else {
                                    next2.performClick();
//                                    Log.d("Test", "Right to Left swipe performed");
                                }
                                return true;
                            } else {
                                if ((firstY_point[0] < finalY)) {
                                    swipeIncorrect();
//                                    Log.d("Test", "Up to Down swipe performed");
                                } else {
                                    swipeCorrect();
//                                    Log.d("Test", "Down to Up swipe performed");
                                }
                            }
                            break;
                    }
                    return true;
                }
            });
        }
        listfontsize = (int) (listfontsize * 1.3);

        /////// is this necessary??? - AnagramQuiz doesn't have it
        /// need it to calculate alignment values
        tile = Typeface.createFromAsset(assetManager, "fonts/nutiles.ttf");

        // calculate width based on word length
        int letterswide = word.length();
        if (letterswide < 7)
            letterswide = 7;
        if (landscape)
            if (letterswide < 10)
                letterswide = 10;

        String term = wordlist[currentID].toString();

        buttonWidth = (int) ((screenwidth) / letterswide);
        fontWidth = (buttonWidth/(int)getResources().getDisplayMetrics().scaledDensity)/2;
        buttonStart = ((float) (screenwidth - (term.length() * buttonWidth)) / 2);

        dragger = shared.getBoolean("testdrag",false);

    }

    protected void loadBundle() {
        Intent intentExtras = getIntent();

        extrasBundle = ((Intent) intentExtras).getExtras();
        if (extrasBundle.isEmpty())
            return;

        String desc = extrasBundle.getString("desc");
        header.setText("Quizzing Anagrams: " + " " + desc);

        String[] inbound = extrasBundle.getStringArray("Words");
        wordlist = new SpannableString[inbound.length];
        if (inbound.length == 0)
            return;

        for (int wordId = 0; wordId < inbound.length; wordId++)
            wordlist[wordId] = SpannableString.valueOf(inbound[wordId]);

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
    private void addLetter(View v) {
        // if already selected, deselect and adjust answer
        if (v.getTag() == "X") {
            v.setTag("");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.setBackgroundColor(Color.TRANSPARENT);
            else { // < 19
                if (themeName.equals("Dark Theme"))
                    v.setBackgroundColor(Color.BLUE);
                else
                    v.setBackgroundColor(Color.BLACK);
            }

            String str = answer.getText().toString();
            int index = str.lastIndexOf(v.getContentDescription().toString());

//            Log.d("delete letter", "String: " + str + " letter: " + v.getContentDescription().toString() + "(" + index + ")" );


            if (index >= 0) {
                String fix = str.substring(0, index) + str.substring(index + 1);
                answer.setText(fix);
            }
            return;
        }

        // add letter to answer
        answer.append(v.getContentDescription());
        v.setBackgroundResource(R.drawable.greybox);

        v.setTag("X");

        if (answer.length() == word.length()) {
                addAnswer(answer.getText().toString());

//            for (int i = 0; i < answer.length(); i++) {
            for (int i = 0; i < 15; i++) {
                buttons[i].setTag("");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    buttons[i].setBackgroundColor(Color.TRANSPARENT);
                else { // < 19
                    if (themeName.equals("Dark Theme"))
                        buttons[i].setBackgroundColor(Color.BLUE);
                    else
                        buttons[i].setBackgroundColor(Color.BLACK);
                }

            }
            answer.setText("");
        }
    }
    // this also grades after each call
    private boolean addAnswer(String attempt) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for(int c = 0; c < anagramList.size(); c++) {

            if (anagramList.get(c).equals(attempt)) {
                if (answerList.contains(attempt))
                    return true;

                if (themeName.equals("Dark Theme"))
                    sb.append("<font color='#00ff00'>" + attempt + "</font><br/>");
                else
                    sb.append("<font color='#00aa00'>" + attempt + "</font><br/>");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    definition.append(SpannableString.valueOf(Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)));
                }
                else {
                    definition.append(SpannableString.valueOf(Html.fromHtml(sb.toString())));
                }

                if (!(answerList.contains(attempt)))
                    answerList.add(attempt);

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

//                ListView lv = findViewById(R.id.lv);
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
                    else
                        next2.setVisibility(View.VISIBLE);
                }
                return true;
            }
        }
        incorrectList.add(attempt);
        return false;
    }
    private void skipWord() {
        if (currentID < wordlist.length - 1) {
            currentID = currentID + 1;
            word.setText(wordlist[currentID]);
        }
        else {
            longtoastMsg("End of List");
        }
    }
    public boolean equalLists(List<String> a, List<String> b){
        // Sort and compare the two lists
        Collections.sort(a);
        Collections.sort(b);

        return (b.containsAll(a));
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

        if (LexData.getShowQuixHooks()) {
            cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.mclistitemunscored, matrixCursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }
        else {
            cursorAdapter = new SlidesListAdapter(getBaseContext(), R.layout.sclistitem, matrixCursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        }

        lv.setAdapter(cursorAdapter);

        databaseAccess.close();
//        updateShown = true;
    }
    private void swipeCorrect() {
        totalCorrect++;

        incorrectList.clear();
        incorrect.setText("");
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

    }
    private void swipeIncorrect() {
        incorrectList.clear();
        incorrect.setText("");
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
    @SuppressLint("ClickableViewAccessibility")
    private void configureButton(Button btn, char letter) {
        btn.setVisibility(View.VISIBLE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                buttonWidth,
                (int) (buttonWidth * 1.05) // * 1.1
        );
        // LinearLayout.LayoutParams.WRAP_CONTENT
        params.setMargins(0, 0, 0, 0);
        btn.setLayoutParams(params);
        btn.setPadding(0,0,0,0);

        btn.setTypeface(tile);
        btn.setTextSize((float) (fontWidth));
        btn.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

//copy to cardquiz
        btn.setText(Character.toString(letter));
        btn.setId(letter);
        btn.setContentDescription(Character.toString( letter));
        btn.setTag("");

        // USE INSTEAD OF ON CLICK FOR DRAGGING (under development)
        if (dragger) {
            btn.setOnTouchListener(mover);
            aligner.setVisibility(View.VISIBLE);
            aligner.setOnClickListener(align);;
        }
        else {
            btn.setOnTouchListener(null);
            aligner.setVisibility(GONE);
        }

        //add button to the layout
        btn.setOnClickListener(append);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // set text color
//        if (themeName.equals("Dark Theme"))
//            btn.setTextColor(Color.WHITE);
//        else
//            btn.setTextColor(Color.BLUE);
        btn.setTextColor(LexData.getTileColor(this));


        // set background, fitting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btn.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            btn.setBackgroundColor(Color.TRANSPARENT);
        }
        else { // < 19
            if (themeName.equals("Dark Theme"))
                btn.setBackgroundColor(Color.BLUE);
            else {
                btn.setBackgroundColor(Color.BLACK); // black
//                btn.setTextColor(Color.WHITE);
            }
        }
        btn.setTextColor(LexData.getTileColor(this));

    }
    public String shuffle(String input){
        List<Character> characters = new ArrayList<Character>();
        for(char c:input.toCharArray()){
            characters.add(c);
        }
        StringBuilder output = new StringBuilder(input.length());
        while(characters.size()!=0){
            int randPicker = (int)(Math.random()*characters.size());
            output.append(characters.remove(randPicker));
        }
        return output.toString();
    }

    private View.OnClickListener append = new View.OnClickListener() {
        public void onClick(View v) {
            addLetter(v);
//            if (v.getTag() == "X") {
//                v.setTag("");
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                    v.setBackgroundColor(Color.TRANSPARENT);
//                else { // < 19
//                    if (themeName.equals("Dark Theme"))
//                        v.setBackgroundColor(Color.BLUE);
//                    else
//                        v.setBackgroundColor(Color.BLACK);
//                }
//
//                String str = answer.getText().toString();
//                int index = str.lastIndexOf(v.getContentDescription().toString());
//                String fix = str.substring(0, index) + str.substring(index+1);
//
//                answer.setText(fix);
//                return;
//            }
//
//            answer.append(v.getContentDescription());
//
////            v.setBackgroundColor(Color.GRAY);
////            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////                v.setBackgroundColor(getColor(R.color.lightgray));
////            }
//            v.setBackgroundResource(R.drawable.greybox);
//
////            v.setBackgroundColor(Color.BLACK);
//            v.setTag("X");
//
//            if (answer.length() == word.length()) {
//                addAnswer(answer.getText().toString());
//
//                new CountDownTimer(2000, 1000) {
//
//                    @Override
//                    public void onTick(long millisUntilFinished) {
//                        // do something after 1s
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        // do something end times 5s
//                    }
//
//                }.start();
//
//
//                for (int i = 0; i < answer.length(); i++) {
//                    buttons[i].setTag("");
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                        buttons[i].setBackgroundColor(Color.TRANSPARENT);
//                    else { // < 19
//                        if (themeName.equals("Dark Theme"))
//                            buttons[i].setBackgroundColor(Color.BLUE);
//                        else
//                            buttons[i].setBackgroundColor(Color.BLACK);
//                    }
//
//                }
//                answer.setText("");
//            }
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
            incorrectList.clear();
            incorrect.setText("");
        } };
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
            Cursor cursor = databaseAccess.getCursor_getWords(missed.toString(), "", "", "");
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
        }
    };
    protected View.OnClickListener doShuffle = new View.OnClickListener() {
        public void onClick(View v) {

            String term = shuffle(wordlist[currentID].toString());
            for (int cl = 0; cl < 15; cl++)
                buttons[cl].setVisibility(View.GONE);

            for (int c = 0; c < term.length(); c++) {
                char letter = term.charAt(c);
                configureButton(buttons[c], letter);
                // sets visibility
            }


            String letters = answer.getText().toString();
            for (int e = 0; e < letters.length(); e++) {
                char seeking = letters.charAt(e);

                // using  the first term.length buttons
                for (int c = 0; c < term.length(); c++) {
//                    if (buttons[c].getVisibility() == View.VISIBLE) {
//                        if (buttons[c].getTag() == "")
                    // if in answer
                    if (buttons[c].getText().toString().contains(Character.toString(seeking))) {
                        buttons[c].setBackgroundResource(R.drawable.greybox);
                        buttons[c].setId(seeking);
                        buttons[c].setContentDescription(Character.toString(seeking));
                        buttons[c].setTag("X");
                        break;
                    } else {
                        // set background, fitting
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            buttons[c].setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                            buttons[c].setBackgroundColor(Color.TRANSPARENT);
                        } else { // < 19
                            if (themeName.equals("Dark Theme"))
                                buttons[c].setBackgroundColor(Color.BLUE);
                            else {
                                buttons[c].setBackgroundColor(Color.BLACK); // black
                                buttons[c].setTextColor(Color.WHITE);
                            }
                        }
                        buttons[c].setTag("");
                        setButtonColor(buttons[c]);
                    }
                }
            }
//            }
            if (dragger)
                alignButtons();

        }
    };
    protected View.OnClickListener doAlphagram = new View.OnClickListener() {
        public void onClick(View v) {

            String term = databaseAccess.sortString(wordlist[currentID].toString());
            for (int cl = 0; cl < 15; cl++)
                buttons[cl].setVisibility(View.GONE);

            for (int c = 0; c < term.length(); c++) {
                char letter = term.charAt(c);
                configureButton(buttons[c], letter);
                // sets visibility
            }

            //get text, set button tag, bg
            String letters = answer.getText().toString();
            for (int e = 0; e < letters.length(); e++) {
                char seeking = letters.charAt(e);

                // using  the first term.length buttons
                for (int c = 0; c < term.length(); c++) {
//                    if (buttons[c].getVisibility() == View.VISIBLE) {
//                        if (buttons[c].getTag() == "")
                    // if in answer
                    if (buttons[c].getText().toString().contains(Character.toString(seeking))) {
                        buttons[c].setBackgroundResource(R.drawable.greybox);
                        buttons[c].setId(seeking);
                        buttons[c].setContentDescription(Character.toString(seeking));
                        buttons[c].setTag("X");
                        break;
                    } else {
                        // set background, fitting
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            buttons[c].setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                            buttons[c].setBackgroundColor(Color.TRANSPARENT);
                        } else { // < 19
                            if (themeName.equals("Dark Theme"))
                                buttons[c].setBackgroundColor(0xff464C70);
                            else {
                                buttons[c].setBackgroundColor(0xff99C1E9); // black
//                                        buttons[c].setTextColor(Color.WHITE);
                            }
                        }
                        buttons[c].setTag("");
                        setButtonColor(buttons[c]);

                    }
                }
            }
//            }
            if (dragger)
                alignButtons();
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

            for (int i = 0; i < answer.length(); i++) {
                buttons[i].setTag("");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    buttons[i].setBackgroundColor(Color.TRANSPARENT);
                else { // < 19
                    if (themeName.equals("Dark Theme"))
                        buttons[i].setBackgroundColor(Color.BLUE);
                    else
                        buttons[i].setBackgroundColor(Color.BLACK);
                }

            }
            answer.setText("");


            //cleanup
            for(int i=0; i< buttons.length;i++)
                buttons[i].setVisibility(View.GONE);
            definition.setText("");
            lv.setAdapter(null);

            answer.setText("");
            answer.setVisibility(View.VISIBLE);
            answerList.clear();

            next2.setVisibility(View.GONE);

            // prep next list
            String term = wordlist[currentID].toString();
            anagramList = databaseAccess.justAnagrams(wordlist[currentID].toString());

            if (anagramList.size() == 0) {
                shorttoastMsg("There are no answers for '" + wordlist[currentID].toString() + "'; skipping");
                skipWord();
                return;
            }

            // calculate width based on word length
            int letterswide = word.length();
            if (letterswide < 7)
                letterswide = 7;
            if (landscape)
                if (letterswide < 10)
                    letterswide = 10;

            buttonWidth = (int) ((screenwidth) / letterswide);
//            fontWidth = buttonWidth/(int)getResources().getDisplayMetrics().scaledDensity;
//            fontWidth = fontWidth / 2;
            fontWidth = (buttonWidth/(int)getResources().getDisplayMetrics().scaledDensity)/2;
            buttonStart = ((float) (screenwidth - (term.length() * buttonWidth)) / 2);

            Log.d("Stats", screenwidth + " screen, " + getResources().getDisplayMetrics().density + " density");

//            AssetManager assetManager = getAssets();
//            tile = Typeface.createFromAsset(assetManager, "fonts/nutiles.ttf");

//            int r = 0;
            for (int c = 0; c < term.length(); c++) {
                char letter = term.charAt(c);
                configureButton(buttons[c], letter);
            }

            status.setText("Anagram " + (currentID + 1)  + "/" + wordlist.length + ": Quizzing for " + anagramList.size() + " words in " + LexData.getLexName());

            answerCount.setText("0/" + anagramList.size());

//            String sizeLog;
//            sizeLog = "ScreenWidth:" + screenwidth + " buttonWidth" + buttonWidth + " fondWidth:" + fontWidth;
//            Log.e("Sizes", sizeLog);
//
//            int position = currentID + 1;

            selector.setSelection(currentID);
            databaseAccess.close();

            matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                    "Anagrams", "ProbFactor", "OPlayFactor", "Score"});

            adapter.notifyDataSetChanged();
        }
    };

    // DRAGGING ROUTINES
    View.OnTouchListener mover = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // adds selected letter to answer (from onClickListener)
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_BUTTON_PRESS:
                    dX = v.getX() - event.getRawX();
                    break;

                case MotionEvent.ACTION_MOVE:
                    v.animate()
                            .x(event.getRawX() + dX)
                            .setDuration(0)
                            .start();
                    break;

                case MotionEvent.ACTION_OUTSIDE:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_BUTTON_RELEASE: //
                case MotionEvent.ACTION_CANCEL: //
                    Arrays.sort(buttons, new Comparator<Button>() {
                        @Override
                        public int compare(Button b1, Button b2) {
                                if (b1.getX() >= b2.getX()) return 1;
                                else return -1;
                        }
                    });

                    alignButtons();
                    break;

            }
            if (gestureDetector.onTouchEvent(event)) {
                addLetter(v);
            }
            return true;
        }
    };
    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }
    private void setButtonColor(Button button) {

//        if (LexData.getTileColor(this) == 1) { // Default
//            if (themeName.equals("Dark Theme"))
//                button.setTextColor(Color.WHITE);
//            else
//                button.setTextColor(Color.BLUE);
//        }
//        else
        button.setTextColor(LexData.getTileColor(this));

    }
    private void alignButtons() {

        Log.i("ButtonStart", Float.toString(buttonStart) );
        int seq = 0;
        for(int i=0; i< buttons.length;i++) {
            if (buttons[i].getVisibility() == View.VISIBLE) {
//                buttons[i].setX(buttonStart + (seq++ * buttonWidth));
                float pos = buttonStart + (seq * buttonWidth);
                buttons[i].setX(pos);
                seq++;
                Log.i(buttons[i].getText().toString(), Float.toString(buttons[i].getX()));
            }
        }
    }
    private View.OnClickListener align = new View.OnClickListener() {
        public void onClick(View v) {
            alignButtons();
        }
    };

    @Override public void onBackPressed(){
        if (answer.length() > 0) {
            answer.setText("");
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setTag("");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    buttons[i].setBackgroundColor(Color.TRANSPARENT);
                else { // < 19
                    if (themeName.equals("Dark Theme"))
                        buttons[i].setBackgroundColor(Color.BLUE);
                    else
                        buttons[i].setBackgroundColor(Color.BLACK);
                }
            }
        }
        else
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
