package com.tylerhosting.hoot.hoot;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static android.view.View.GONE;

public class CardAnagramQuizActivity extends SlidesActivity {

    double screenwidth, height;

    float buttonStart;
    float buttonTop;
    int buttonWidth; // in pixels
    int fontWidth; // in sp
    Button[] buttons = new Button[15];
    GestureDetector gestureDetector;
    Bundle extrasBundle;
    float dX, dY;
    int totalCorrect = 0;

    CardDatabase cardDatabase;
    private SQLiteDatabase cards;
    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.US);
    boolean updateShown = false;

    LexData.ZWord before;
    LexData.ZWord update;
    LexData.HListWord beforeListWord;
    LexData.HListWord updateListWord;
    boolean usingList = false;
    int ListID;
    String cardbox, order, count;

    SharedPreferences shared;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = PreferenceManager.getDefaultSharedPreferences(this);



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
        etSeconds.setVisibility(View.GONE);
        textSeconds.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        quit.setVisibility(View.GONE);

        first.setVisibility(View.GONE);
        next.setVisibility(View.GONE);
        previous.setVisibility(View.GONE);
        last.setVisibility(View.GONE);
        selector.setVisibility(View.GONE);

        incorrect.setText("");
        answerCount.setVisibility(View.VISIBLE);

        aligner=findViewById(R.id.btnAlign);

        gestureDetector = new GestureDetector(this, new SingleTapConfirm());

        showSlides();
    }

    @Override
    protected void onResume() {
         super.onResume();
         if (themeName.equals("Dark Theme")) {
             answer.setTextColor(Color.WHITE);
             incorrect.setTextColor(Color.WHITE);
         }
         else {
             answer.setTextColor(Color.BLACK);
             incorrect.setTextColor(Color.BLACK);
         }
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
                             Log.d("Points", Float.toString(firstX_point[0]) + "," + Float.toString(firstY_point[0]));
                             break;

                         case MotionEvent.ACTION_UP:

                             float finalX = event.getRawX();
                             float finalY = event.getRawY();
                             Log.e("Size", Double.toString(height) + "," + Double.toString(screenwidth));

                             int distanceX = (int) Math.abs(finalX - firstX_point[0]);
                             int distanceY = (int) Math.abs(finalY - firstY_point[0]);
                             Log.e("Points", Integer.toString(distanceX) + "," + Integer.toString(distanceY));

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
                                     Log.d("Test", "Up to Down swipe performed");
                                 } else {
                                     swipeCorrect();
                                     Log.d("Test", "Down to Up swipe performed");
                                 }
                             }
                             break;
                     }
                     return true;
                 }
             });
         } else {
             if (usingList) {
                 String listname = extrasBundle.getString("listname");
                 String headerText = "Quizzing " + listname + " (Anagrams)";
                 header.setText(headerText);
             } else
                 header.setText("Quizzing " + cardbox);
             sv.setOnTouchListener(null);
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

        cardbox = extrasBundle.getString("cardbox");
        order = extrasBundle.getString("order");
        count = extrasBundle.getString("wordcount");
        String filter = extrasBundle.getString("filter");

        String listname = extrasBundle.getString("listname");
        if (listname != null) {
            usingList = true;
            if (listname.length() == 0) {
                Toast.makeText(this, "A name is required for a list", Toast.LENGTH_LONG).show();
                return;
            }
        }

        header.setText("Quizzing " + cardbox);

        LexData.Cardbox cb = new LexData.Cardbox();
        cb.program = "Hoot";
        cb.lexicon = LexData.getLexName();
        cb.boxtype = cardbox;

        LexData.setCardfile(cb);
        Cursor carditems;

        if (Utils.fileExist(LexData.getCardfile())) {
            cardDatabase = new CardDatabase(this, LexData.getCardfile(), null, 2);
            cards = cardDatabase.getWritableDatabase();
            // STAYS OPEN UNTIL BACK PRESSED
            int counter = 0;

            // list anagrams
            if (usingList) {
                LexData.HList hlist = CardDatabase.getList(cards, listname);
                ListID = hlist.id;
                carditems = cardDatabase.getScheduledListCards(cards, listname, order, count);
                int cardCount = carditems.getCount();
                if (cardCount == 0) {
                    Toast.makeText(this, "Error: Card box has no entries", Toast.LENGTH_LONG).show();
                    finish();
                }

                wordlist = new SpannableString[cardCount];
                int column = carditems.getColumnIndex("question");
                while (carditems.moveToNext()) {
                    wordlist[counter] = SpannableString.valueOf(carditems.getString(column));
                    counter++;
                }
                String headerText = "Quizzing " + listname + " (Anagrams)";
                header.setText(headerText);
            }
            // global anagrams
            else {
                carditems = cardDatabase.getScheduledCards(cards, order, count, filter);
                int cardCount = carditems.getCount();
                if (cardCount == 0) {
                    Toast.makeText(this, "Error: Card box has no entries", Toast.LENGTH_LONG).show();
                    finish();
                }

                wordlist = new SpannableString[cardCount];
                int column = carditems.getColumnIndex("question");
                while (carditems.moveToNext()) {
                    wordlist[counter] = SpannableString.valueOf(carditems.getString(column));
                    counter++;
                }
            }
//also need to grade accordingly

        }
        else {
            Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
            onBackPressed();
        }

        cards.execSQL("DROP TABLE IF EXISTS `cardFilter`");

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenwidth = size.x * .95;
        height = size.y;
    }

    private void configureButton(Button btn, char letter) {
        Log.i("configure", String.valueOf(letter));
        btn.setVisibility(View.VISIBLE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                buttonWidth,
                (int) (buttonWidth*1.05)
        );
        // LinearLayout.LayoutParams.WRAP_CONTENT

        // set position
//        btn.setY(buttonTop);
        params.setMargins(0, 0, 0, 0);
        btn.setLayoutParams(params);
        btn.setPadding(0,0,0,0);

        // set font
        btn.setTypeface(tile);
        btn.setTextSize((float) (fontWidth));
        btn.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);

        // set letter id
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

        btn.setOnClickListener(append);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // set text color
//        if (themeName.equals("Dark Theme"))
//            btn.setTextColor(Color.WHITE);
//        else
//            btn.setTextColor(Color.BLUE);
        setButtonColor(btn);

        // set background, fitting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btn.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            btn.setBackgroundColor(Color.TRANSPARENT);
        }
        else { // < 19
            if (themeName.equals("Dark Theme"))
                btn.setBackgroundColor(0xff464C70);
            else {
                btn.setBackgroundColor(0xff99C1E9); // black
            }
        }
        setButtonColor(btn);

    }
    // only called by onCreate (combine??)
    public void showSlides() {
        if (!Utils.fileExist(LexData.getCardfile()))
            return;
        if(wordlist!=null && wordlist.length>0) {
            word.setText(wordlist[0]);
//            if (dragger)
//                alignButtons();
        }
        else {
            Toast.makeText(this, "Error: Card box has no entries", Toast.LENGTH_LONG).show();
            return;
        }
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

    private void addLetter(View v) {
        // adds letter based on user input
        // if already selected, deselect and adjust answer
        if (v.getTag() == "X") {
            v.setTag("");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                v.setBackgroundColor(Color.TRANSPARENT);
            else { // < 19
                if (themeName.equals("Dark Theme"))
                    v.setBackgroundColor(0xff464C70);
                else
                    v.setBackgroundColor(0xff99C1E9);
            }

            String str = answer.getText().toString();
            int index = str.lastIndexOf(v.getContentDescription().toString());

//            String logmsg = "String: " + str + " Letter: " + v.getContentDescription().toString() +
//                    "Text: " + v.toString() + " Tag: " + v.getTag() + "Visibility: " + v.getVisibility();
//            Log.d("add delete letter", logmsg );

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

            new CountDownTimer(2000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    // do something after 1s
                }

                @Override
                public void onFinish() {
                    // do something end times 5s
                }

            }.start();

//            for (int i = 0; i < answer.length(); i++) {
            for (int i = 0; i < 15; i++) {
                buttons[i].setTag("");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    buttons[i].setBackgroundColor(Color.TRANSPARENT);
                else { // < 19
                    if (themeName.equals("Dark Theme"))
                        buttons[i].setBackgroundColor(0xff464C70);
                    else
                        buttons[i].setBackgroundColor(0xff99C1E9);
                }
            }
            answer.setText("");
        }

        // check to see if all answers provided
    }
    // this also grades after each call
    private boolean addAnswer(String attempt) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for(int c = 0; c < anagramList.size(); c++) {

            if (anagramList.get(c).equals(attempt)) {
                if (answerList.contains(attempt))
                    return true;

                if (themeName.equals( "Dark Theme"))
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
                    updateCard();

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
    private void updateCard() {
        ;
//        before = CardDatabase.getCard(cards, wordlist[currentID].toString());

        if (incorrectList.size() > 0)
            for(int c = 0; c < incorrectList.size(); c++) {
                incorrect.append( "   " + incorrectList.get(c));
            }
        incorrect.setVisibility(View.VISIBLE);

        String last = "";
        if (usingList){
            if (incorrect.length() > 0)
                updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());
            else {
                updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.forward.ordinal());
                totalCorrect++;
            }
            if (beforeListWord.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    updateListWord.question, beforeListWord.cardbox, updateListWord.cardbox,
                    dateFormat.format(CardUtils.dtDate(updateListWord.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            updateListWord.correct, updateListWord.streak, last);
            status.setText(scoreStatus);

        }
        else {
            if (incorrect.length() > 0)
                update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());
            else {
                update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.forward.ordinal());
                totalCorrect++;
            }
            if (before.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(before.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    update.question, before.cardbox, update.cardbox,
                    dateFormat.format(CardUtils.dtDate(update.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            update.correct, update.streak, last);
            status.setText(scoreStatus);
        }


        updateShown  = true;
    }
    private void missedCard() {
        if (incorrectList.size() > 0)
            for(int c = 0; c < incorrectList.size(); c++) {
                incorrect.append( "   " + incorrectList.get(c));
            }
        incorrect.setVisibility(View.VISIBLE);


        if (usingList) {
            updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());

            String last = "";
            if (beforeListWord.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    updateListWord.question, beforeListWord.cardbox, updateListWord.cardbox,
                    dateFormat.format(CardUtils.dtDate(updateListWord.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            updateListWord.correct, updateListWord.streak, last);
            status.setText(scoreStatus);
        }
        else {
            update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());

            String last = "";
            if (before.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(before.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    update.question, before.cardbox, update.cardbox,
                    dateFormat.format(CardUtils.dtDate(update.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            update.correct, update.streak, last);
            status.setText(scoreStatus);
        }
        updateShown = true;
    }
    private void skipWord() {
        if (currentID < wordlist.length - 1) {
            currentID = currentID + 1;
            word.setText(wordlist[currentID]);
        }
        else {
            longtoastMsg("End of List");
            finalExamGrade();
        }
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
    public  boolean finalExamGrade() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.darkAlertDialog);
        StringBuilder msg = new StringBuilder();
        msg.append("You got ");
        msg.append(totalCorrect);
        msg.append(" correct out of ");
        msg.append(wordlist.length);

        builder.setMessage(msg);
        builder.setTitle("Return to Cardbox quiz menu");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onBackPressed();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
        return true;
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
        updateShown = true;

//        next2.setVisibility(View.VISIBLE);

    }
    private void swipeCorrect() {
        String last = "";
        if (usingList) {
            updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.forward.ordinal());
            totalCorrect++;
            if (beforeListWord.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    updateListWord.question, beforeListWord.cardbox, updateListWord.cardbox,
                    dateFormat.format(CardUtils.dtDate(updateListWord.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            updateListWord.correct, updateListWord.streak, last);
            status.setText(scoreStatus);
        }
        else {
            update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.forward.ordinal());
            totalCorrect++;
            if (before.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(before.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    update.question, before.cardbox, update.cardbox,
                    dateFormat.format(CardUtils.dtDate(update.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            update.correct, update.streak, last);
            status.setText(scoreStatus);
        }
        updateShown = true;

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
        String last = "";
        if (usingList) {
            updateListWord = CardDatabase.moveListWord(cards, ListID, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());
            if (beforeListWord.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    updateListWord.question, beforeListWord.cardbox, updateListWord.cardbox,
                    dateFormat.format(CardUtils.dtDate(updateListWord.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            updateListWord.correct, updateListWord.streak, last);
            status.setText(scoreStatus);
        }
        else {
            update = CardDatabase.moveCard(cards, wordlist[currentID].toString(), CardUtils.boxmove.back.ordinal());
            if (before.last_correct == 0) last = "Never";
            else last = dateFormat.format(CardUtils.dtDate(before.last_correct));

            String scoreStatus = String.format(Locale.US, "Moved %s from box %s to box %s     Due: %s\r\n",
                    update.question, before.cardbox, update.cardbox,
                    dateFormat.format(CardUtils.dtDate(update.next_scheduled))) +
                    String.format("Total: %s  Streak:%s  Last Correct: %s",
                            update.correct, update.streak, last);
            status.setText(scoreStatus);
        }
        updateShown = true;
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
    private View.OnClickListener append = new View.OnClickListener() {
        public void onClick(View v) {
            addLetter(v);

        }
    };
    protected View.OnClickListener goNext = new View.OnClickListener() {
        public void onClick(View v) {

            if (!updateShown) {
                if (equalLists(anagramList, answerList)) {
                    updateCard();
                }
                else
                    missedCard();
                updateShown = true;
                return;
            }

            if (currentID < wordlist.length - 1) {
                currentID = currentID + 1;
                word.setText(wordlist[currentID]);
            }
            else {
                longtoastMsg("End of List");
                finalExamGrade();
            }
            incorrectList.clear();
            incorrect.setText("");
            updateShown = false;
//            if (dragger)
//                alignButtons();

        }
    };
    private final TextWatcher wordWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//            if (dragger)
//                alignButtons();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
//            if (dragger)
//                alignButtons();
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
                        buttons[i].setBackgroundColor(0xff464C70);
                    else
                        buttons[i].setBackgroundColor(0xff99C1E9);
                }

            }
            answer.setText("");

            // button cleanup
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

            // calculate width based on each word length
            int letterswide = word.length();
            if (letterswide < 7)
                letterswide = 7;
            if (landscape)
                if (letterswide < 10)
                    letterswide = 10;

            buttonWidth = (int) ((screenwidth) / letterswide);
            fontWidth = (buttonWidth/(int)getResources().getDisplayMetrics().scaledDensity)/2;
            buttonStart = ((float) (screenwidth - (term.length() * buttonWidth)) / 2);
//            buttonTop = buttons[0].getY();

            Log.d("Stats", screenwidth + " screen, " + getResources().getDisplayMetrics().density + " density");


// RESET BUTTONS, CLEAR ENTRY
            for (int c = 0; c < term.length(); c++) {
                char letter = term.charAt(c);
                configureButton(buttons[c], letter);
            }
//            if (dragger)
//                alignButtons();


            if (usingList) {
                beforeListWord = CardDatabase.getListWord(cards, ListID, wordlist[currentID].toString());
                String last = "";
                if (beforeListWord.last_correct == 0) last = "Never";
                else last = dateFormat.format(CardUtils.dtDate(beforeListWord.last_correct));

                String scoreStatus = "Anagram " + (currentID + 1) + "/" + wordlist.length + ": Quizzing for " + anagramList.size() + " words in " + LexData.getLexName() +
                        String.format(Locale.US, "\r\nIn box %s, scheduled for %s  Last Correct: %s ",
                                beforeListWord.cardbox,
                                dateFormat.format(CardUtils.dtDate(beforeListWord.next_scheduled)),
                                last);
                status.setText(scoreStatus);
            }
            else {
                before = CardDatabase.getCard(cards, wordlist[currentID].toString());

                String last = "";
                if (before.last_correct == 0) last = "Never";
                else last = dateFormat.format(CardUtils.dtDate(before.last_correct));

                String scoreStatus = "Anagram " + (currentID + 1) + "/" + wordlist.length + ": Quizzing for " + anagramList.size() + " words in " + LexData.getLexName() +
                        String.format(Locale.US, "\r\nIn box %s, scheduled for %s  Last Correct: %s ",
                                before.cardbox,
                                dateFormat.format(CardUtils.dtDate(before.next_scheduled)),
                                last);
                status.setText(scoreStatus);
            }

            answerCount.setText("0/" + anagramList.size());

            //            on each word
//            store XY of first button
//                    after drag, reset the position of buttons using XY + buttonWidth

            selector.setSelection(currentID);
            databaseAccess.close();
            matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                    "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
            adapter.notifyDataSetChanged();

//            if (dragger)
//                alignButtons();

        }
    };
    protected View.OnClickListener showAnagrams = new View.OnClickListener() {
        public void onClick(View v) {
            if (flashcards) {
                showFlashList();
                return;
            }

            if (anagramList.size() == 0) {
                next2.setVisibility(View.VISIBLE);
                return;
            }

            List missing = new ArrayList();
            for(int c = 0; c < anagramList.size(); c++)
                missing.add(anagramList.get(c));

            for(int c = 0; c < answerList.size(); c++)
                missing.remove(answerList.get(c));

            Collections.sort(missing);

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

            if (equalLists(anagramList, answerList)) {
                updateCard();
            }
            else
                missedCard();
            updateShown = true;

            next2.setVisibility(View.VISIBLE);
        }
    };


    // DRAGGING ROUTINES
    private void alignButtons() {
        int seq = 0;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].getVisibility() == View.VISIBLE) {
                float pos = buttonStart + (seq * buttonWidth);
                buttons[i].setX(pos);
                seq++;
                Log.i(buttons[i].getText().toString(), Float.toString(buttons[i].getX()));
            }
        }
    }
    private void setButtonColor(Button button) {

//        if (LexData.getTileColor(this) == 1) { // Default

//            if (themeName.equals("Light Theme") && LexData.getTileColor(this) == Color.BLACK)
//                LexData.setTileColor(Color.WHITE);
//            else
//                button.setTextColor(Color.BLUE);
//        }
//        else
        button.setTextColor(LexData.getTileColor(this));

    }
    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }

    }
    View.OnTouchListener mover = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            StringBuilder sb;

            // single tap adds selected button letter to answer
//            if (gestureDetector.onTouchEvent(event)) {
//                addLetter(v);
//                return true;
//            }
//            else
//                Log.i("false", String.valueOf(event.getAction()));

            // dragging
            Log.e("MotionEvent", String.valueOf(event.getAction()));
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_BUTTON_PRESS: //
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
                default:
                    Arrays.sort(buttons, new Comparator<Button>() {
                        @Override
                        public int compare(Button b1, Button b2) {
                                if (b1.getX() >= b2.getX()) return 1;  // >= gives b1 preference; 2 buttons cannot be in same place
                                else return -1;
                        }
                    });
                    alignButtons();
                    break;
            }
//            alignButtons();

            // single tap adds selected button letter to answer
            if (gestureDetector.onTouchEvent(event)) {
                addLetter(v);
                if (dragger)
                    alignButtons();
            }
            return true;
        }
    };
    private View.OnClickListener align = new View.OnClickListener() {
        public void onClick(View v) {
            if (dragger)
                alignButtons();
        }
    };

    private void alignButtons2(String term) {
        // button cleanup
        for (int i = 0; i < buttons.length; i++)
            buttons[i].setVisibility(View.GONE);

        // calculate width based on each word length
        int letterswide = term.length();
        if (letterswide < 7)
            letterswide = 7;
        if (landscape)
            if (letterswide < 10)
                letterswide = 10;

        buttonWidth = (int) ((screenwidth) / letterswide);
        fontWidth = (buttonWidth / (int) getResources().getDisplayMetrics().scaledDensity) / 2;
        buttonStart = ((float) (screenwidth - (term.length() * buttonWidth)) / 2);

        Log.d("Stats", screenwidth + " screen, " + getResources().getDisplayMetrics().density + " density");


// RESET BUTTONS, CLEAR ENTRY
        int seq = 0;
        for (int c = 0; c < term.length(); c++) {
            char letter = term.charAt(c);
            configureButton(buttons[c], letter);
            float pos = buttonStart + (seq * buttonWidth);
            buttons[c].setX(pos);
            seq++;
            Log.i(buttons[c].getText().toString(), Float.toString(buttons[c].getX()));
        }


// remark selected letters
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
//                            buttons[c].setTextColor(Color.WHITE);
                        }
                    }
                    buttons[c].setTag("");
                    setButtonColor(buttons[c]);

                }
            }
        }
    }

    @Override public void onBackPressed(){
        if (answer.length() > 0) {
            answer.setText("");
            for (int i = 0; i < buttons.length; i++) {
                buttons[i].setTag("");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    buttons[i].setBackgroundColor(Color.TRANSPARENT);
                else { // < 19
                    if (themeName.equals("Dark Theme"))
                        buttons[i].setBackgroundColor(0xff464C70);
                    else
                        buttons[i].setBackgroundColor(0xff99C1E9);
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

        builder.setTitle("Do you want to quit this Card box quiz ??");
        builder.setPositiveButton("Yes. Quit now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (cards != null)
                    if (cards.isOpen())
                        cards.close();
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
