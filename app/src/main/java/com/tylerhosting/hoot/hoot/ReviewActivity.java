package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ReviewActivity extends SlidesActivity {

    ArrayAdapter<String> adapter;
    List<String> anagramList = new ArrayList<>();
    String searchType;
    SQLiteDatabase cards;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adapter=new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                anagramList);

        word.addTextChangedListener(wordWatcher);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            definition.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
        definition.setTextSize(24);
        definition.setTypeface(listfont);
        lv.setVisibility(View.VISIBLE);
        definition.setVisibility(View.GONE);

        answer.setVisibility(View.GONE);
        answerCount.setVisibility(View.VISIBLE);
        answerCount.setTypeface(null, Typeface.NORMAL);

        entry.setVisibility(View.GONE);

/*        String fs = shared.getString("listfont", "24");
        if (Utils.isParsable(fs))
            listfontsize = Integer.parseInt(shared.getString("listfont","24"));
        else {
            prefs.putString("listfont", "24");
            prefs.apply();
            listfontsize = 24;
        }
*/
        lv.setOnTouchListener(new OnSwipeTouchListener(this) {
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


    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    protected void loadBundle() {
        Intent intentExtras = getIntent();
        Bundle extrasBundle = ((Intent) intentExtras).getExtras();
        if (extrasBundle.isEmpty())
            return;

        searchType = extrasBundle.getString("search");
        String desc = extrasBundle.getString("desc");
        header.setText("Review " + searchType + ": " + desc);
        String filter = extrasBundle.getString("filter");

        String[] inbound = null; // only used by words passed ??

        // If cardbox passed
        if (extrasBundle.getStringArray("Words") == null) {
            CardDatabase cardDatabase;
            boolean usingList = false;

            String cardbox = extrasBundle.getString("cardbox");
            searchType = extrasBundle.getString("cardbox");
            String order = extrasBundle.getString("order");
            String count = extrasBundle.getString("wordcount");
            String listname = extrasBundle.getString("listname");
            if (listname != null) {
                usingList = true;
                if (listname.length() == 0) {
                    Toast.makeText(this, "A name is required for a list", Toast.LENGTH_LONG).show();
                    return;
                }
                searchType = extrasBundle.getString("search");
            }

            header.setText("Reviewing " + cardbox);

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
                if (usingList)
                    carditems = cardDatabase.getScheduledListCards(cards, listname, order, count);
                else
                    carditems = cardDatabase.getScheduledCards(cards, order, count, filter);

                // global anagrams
                int cardCount = carditems.getCount();
                if (cardCount == 0) {
                    Toast.makeText(this, "Error: Card box has no entries", Toast.LENGTH_LONG).show();
                    finish();
                }

                wordlist = new SpannableString[cardCount];
                inbound = new String[cardCount];
                int column = carditems.getColumnIndex("question");
                while (carditems.moveToNext()) {

// words changed here
//                    Log.e("ReviewActloadbundle", carditems.getString(column));

                    wordlist[counter] = SpannableString.valueOf(carditems.getString(column));
                    inbound[counter] = carditems.getString(column);
                    counter++;
                }

            }
            else {
                Toast.makeText(this, "Card box doesn't have any cards in it", Toast.LENGTH_LONG).show();
                onBackPressed();
            }
            cards.execSQL("DROP TABLE IF EXISTS `cardFilter`");

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
        }


        // If list passed
        else {
            inbound = extrasBundle.getStringArray("Words");
            wordlist = new SpannableString[inbound.length];
            if (inbound.length == 0)
                return;
//        }

            if (LexData.getColorBlanks()) {
                // for each word
                for (int wordId = 0; wordId < inbound.length; wordId++) {
                    boolean hasBlanks = false;

                    SpannableStringBuilder sb = new SpannableStringBuilder();
                    // for each letter
                    for (int index = 0; index < inbound[wordId].length(); index++) {
                        char letter = inbound[wordId].charAt(index);
                        if (Character.isLowerCase(letter)) {
                            hasBlanks = true;

                            switch (themeName) {
                                case "Dark Theme":
                                    sb.append("<font color='#00ff88'>" + (char) (letter - 32) + "</font>"); // #00ff88
                                    break;
                                case "Light Theme":
                                default:
                                    sb.append("<font color='#0033ee'>" + (char) (letter - 32) + "</font>"); // #0033ee
                                    break;
                            }

                        } else
                            sb.append(inbound[wordId].charAt(index));
                    }
                    if (hasBlanks) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wordlist[wordId] = SpannableString.valueOf((Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_LEGACY)));
                        } else
                            wordlist[wordId] = SpannableString.valueOf((Html.fromHtml(sb.toString())));
                    } else
                        wordlist[wordId] = SpannableString.valueOf(inbound[wordId]);
                }
            } else {
                for (int wordId = 0; wordId < inbound.length; wordId++)
                    wordlist[wordId] = SpannableString.valueOf(inbound[wordId]);
            }
        }

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                R.layout.spincenter, inbound);
        selector.setAdapter(dataAdapter);
        selector.setOnItemSelectedListener(selection);


    }


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

            // return String with hooks/Word
            //anagramList = databaseAccess.hookAnagrams(wordlist[currentID].toString());
            definition.setText("");
            if (s.length() < 3)
                definition.setTextSize(18);
            else if (s.length() < 5)
                definition.setTextSize(22);
            else
                definition.setTextSize(24);

            lv.setAdapter(null);


            for(int c = 0; c < anagramList.size(); c++)
                definition.append(anagramList.get(c) + "\n");
//            definition.setText(anagramList.toString());
            int position = currentID + 1;
            status.setText("Showing " + position + "/" + wordlist.length + " words from " + LexData.getLexName());

            answerCount.setText("0/" + anagramList.size());

            selector.setSelection(currentID);

            Cursor cursor;
            switch (searchType) {
                case "Anagrams":
                    cursor = databaseAccess.getCursor_anagrams(wordlist[currentID].toString(), "", "", 0,0,false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;

//                case "Recall" :
//                text never changes
                case "Hook Words":
                case "Hooks":
                    cursor = databaseAccess.getCursor_hookwords(wordlist[currentID].toString(), "", "", "",false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
                    break;
                case "Blank Anagrams":
                case "BlankAnagrams":
                    cursor = databaseAccess.getCursor_blankAnagrams(wordlist[currentID].toString() + "?", "", "", 0,0,false);
                    anagramList = databaseAccess.wordsFromCursor(cursor);
//                    anagramList = databaseAccess.justblankAnagrams(wordlist[currentID].toString());
                    break;
                default:
                    return;

            }

            if (anagramList.size() == 0) {
                shorttoastMsg("There are no answers for '" + wordlist[currentID].toString() + "'; skipping");
                skipWord();
                return;
            }



            matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                    "Anagrams", "ProbFactor", "OPlayFactor", "Score"});


            while (cursor.moveToNext())
                matrixCursor.addRow(databaseAccess.get_CursorRow(cursor));



            if (LexData.getShowHooks()) {
                cursorAdapter = new ReviewListAdapter(getBaseContext(), R.layout.mclistitemunscored, cursor, from, to, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            }
            else {
                cursorAdapter = new ReviewListAdapter(getBaseContext(), R.layout.sclistitem, cursor, plainfrom, plainto, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
            }



            ListView lv = findViewById(R.id.lv);
            lv.setAdapter(cursorAdapter);

            answerCount.setText(anagramList.size() + " " + searchType);


/*
                if  (anagrams.moveToFirst()) {
                    do {
                        String firstName = c.getString(c.getColumnIndex("FirstName"));
                        int age = c.getInt(c.getColumnIndex("Age"));
                        results.add("Name: " + firstName + ",Age: " + age);
                    }while (anagrams.moveToNext());
                }


            for(int c = 0; c < anagrams.getCount(); c++)

                string[] row = { textBox1.Text, textBox2.Text, textBox3.Text };
            var listViewItem = new ListViewItem(row);
            listView1.Items.Add(listViewItem);


                lv.(anagrams.get(c) + "\n");




 */



            databaseAccess.close();
            adapter.notifyDataSetChanged();






        }

    };
    public class ReviewListAdapter extends SimpleCursorAdapter {
        private Context mContext;
        private int id;
        private List<String> items;

        public ReviewListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
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
            // if lower set to green
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
                                red.append("<font color='#00ff00'>" + (char) (letter - 32) + "</font>");
                                break;
                            case "Light Theme":
                            default:
                                red.append("<font color='#22aa22'>" + (char) (letter - 32) + "</font>");
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
                            green.append("<font color='#22aa22'>" + wordCol + "</font>");
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


    private void skipWord() {

        if (currentID < wordlist.length - 1) {
            currentID = currentID + 1;
            word.setText(wordlist[currentID]);
        }
        else {
            if (LexData.slideLoop)
                currentID = 0; /// if rotating
            longtoastMsg("End of List");
        }
    }
}
