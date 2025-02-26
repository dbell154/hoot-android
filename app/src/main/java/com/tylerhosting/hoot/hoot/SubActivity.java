package com.tylerhosting.hoot.hoot;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SubActivity extends SearchActivity {

    String filters = "", ordering = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = "SubActivity";

        loadBundle();
        hideform();
        term.setVisibility(VISIBLE);
        term.setText(message);
    }
    private void loadBundle() {
        Intent intentExtras = getIntent();
        Bundle extrasBundle = ((Intent) intentExtras).getExtras();
        if(extrasBundle.isEmpty())
            return;
        String word = extrasBundle.getString("term");
        if (word.trim().length() == 0)
            return;
        String desc = extrasBundle.getString("desc");



//        filters = extrasBundle.getString("filters");
        ordering = extrasBundle.getString("ordering");

        // ?? move to populate
        if (extrasBundle.containsKey("search")) {
            int search = extrasBundle.getInt("search");
            doSubsearch(search, word);
        }
        else
            doSubsearch(9, word);
//        hidekeyboard();
    }

    @SuppressLint("NonConstantResourceId")
    private void doSubsearch(int search, String word) {


//        Cursor cursor;
//        SimpleCursorAdapter cursorAdapter;
        lStartTime = System.nanoTime();

        databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        databaseAccess.open();
        switch (search) {
            case R.id.anagrams:
                cursor = databaseAccess.getCursor_anagrams(word, filters, ordering, 0,0,false); // never has blanks
                message = word + " Anagrams";
                break;
            case R.id.misspells:
                cursor = databaseAccess.getCursor_misspells(word, ordering, false);
                message = word + " Misspells";
                break;
            case R.id.hookwords:
                cursor = databaseAccess.getCursor_hookwords(word, filters, ordering, "",false);
                message = word + " Hook Words";
                break;
            case R.id.wordbuilder:
                // thread
                // cursor = databaseAccess.getCursor_subanagrams(word, filters, ordering);
                subSearchThread(this, R.id.wordbuilder, word, filters);
                message = word + " Word Builder";
                return;
//                break;
            case R.id.containsall:
                // thread
                //cursor = databaseAccess.getCursor_superanagrams(word, filters, ordering);
                subSearchThread(this,R.id.containsall, word, filters);
                message = word + " Contains all";
                return;
//                break;
            case R.id.contains:
                cursor = databaseAccess.getCursor_contains(word, filters, ordering, "",false);
                message = word + " Extensions";
                break;
            case R.id.begins:
                cursor = databaseAccess.getCursor_begins(word, filters, ordering, limits, false);
                message = word + " Begins With";
                break;
            case R.id.ends:
                cursor = databaseAccess.getCursor_ends(word, filters, ordering, limits,false);
                message = word + " Ends With";
                break;
            case R.id.subwords:
                cursor = databaseAccess.getCursor_subwords(2, LexData.getMaxLength(), word);
                message = word + " Subwords";
                break;
            case R.id.stretches:
                cursor = databaseAccess.getCursor_stretches(word, false);
                message = word + " Stretches";
                break;
            case R.id.Joins:
                cursor = databaseAccess.getCursor_splitters(word.length() + 5, word.length() + 5, word, filters,ordering,false);
                message = word + " Joins";
                break;

            case R.id.blankanagrams:
                cursor = databaseAccess.getCursor_blankAnagrams(word + "?", filters, ordering, 0,0,false);
                message = word + " Blank Anagrams";
                break;
            case R.id.transpositions:
                cursor = databaseAccess.getCursor_transpositions(word);
                message = word + " Transpositions";
                break;
            case R.id.categories:
                int listID = databaseAccess.getListID(word);
                cursor = databaseAccess.getCursor_listwords(listID, filters, ordering);
                message = "Category: " + word ;
                break;

            default:
                return;
        }

//        ListView lv = findViewById(R.id.subresults);
//        lv.setAdapter(cursorAdapter);

        displayResults();
        hidekeyboard();

        databaseAccess.close();
    }

    protected void hideform() {

        stype.setVisibility(GONE);
        stems.setVisibility(GONE);
        predef.setVisibility(GONE);
        categories.setVisibility(GONE);
        etTerm.setVisibility(GONE);

        guide1.setVisibility(GONE);
        guide3.setVisibility(GONE);
        Underlay.setVisibility(GONE);
        minimum.setVisibility(GONE);
        maximum.setVisibility(GONE);
        begins.setVisibility(GONE);
        clearBegins.setVisibility(GONE);
        ends.setVisibility(GONE);
        clearEnds.setVisibility(GONE);
        //etTerm.setVisibility(GONE);
        blank.setVisibility(GONE);
        clearEntry.setVisibility(GONE);
        altSearch.setVisibility(GONE);
        sortby.setVisibility(GONE);
        thenby.setVisibility(GONE);
        search.setVisibility(GONE);

        etFilter.setVisibility(GONE);
        emptyRack.setVisibility(GONE);
        etLimit.setVisibility(GONE);
        etOffset.setVisibility(GONE);

        more.setVisibility(GONE);
        altSearch.setVisibility(GONE);
        specStatus.setVisibility(GONE);


        collapse.setChecked(true);
        collapse.setVisibility(View.INVISIBLE);
        clear.setVisibility(GONE);

        ConstraintSet hiding = new ConstraintSet();
        ConstraintLayout layout = findViewById(R.id.search_layout);
        hiding.clone(layout);
        hiding.connect(R.id.imcheader,ConstraintSet.TOP, R.id.collapse, ConstraintSet.BOTTOM);
        hiding.applyTo(layout);

    }

    public Cursor subSearchThread(final Context context, final int searchType, final String term, final String filters){
        @SuppressLint("StaticFieldLeak")
        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            private ProgressDialog dialog;
            //            private Cursor tcursor;
            private MatrixCursor matrixCursor;

            @Override
            protected void onPreExecute()
            {
                databaseAccess.open();
//                Log.i("Search Type", Integer.toString(searchType));
                this.dialog = Utils.themeDialog(context);
//                this.dialog = new ProgressDialog(context);
                this.dialog.setMessage("Please Wait!\r\nThis search may take a minute or two...");
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        //dialog.dismiss();
                    }
                });
                this.dialog.setCancelable(true);
                this.dialog.setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                    @Override
                    public void onCancel(DialogInterface dialog)
                    {
                        // cancel AsyncTask
                        cancel(false);
                    }
                });
                dialog.setCanceledOnTouchOutside(false);
                this.dialog.show();
            }

            @Override
            protected void onCancelled()//called on ui thread
            {
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                dialog.cancel();
//                cursor = getThreadCursor();
//                databaseAccess.close();
                displayPartialResults();
            }
            @SuppressLint("NonConstantResourceId")
            @Override
            protected Void doInBackground(Void... params)
            {
                Cursor precursor;
                matrixCursor = new MatrixCursor(new String[]{"_id", "Word", "WordID", "FrontHooks", "BackHooks", "InnerFront", "InnerBack",
                        "Anagrams", "ProbFactor", "OPlayFactor", "Score"});
                String lenFilter = String.format("Length(Word) <= %1$s", LexData.getMaxLength());
                // String lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) <= %2$s", LexData.getMaxLength(), term.length());
                char[] a = term.toCharArray(); // anagram
                int[] first = new int[26]; // letter count of anagram
                int c; // array position
                int blankcount = 0;
//                databaseAccess.open();

                // threads from searches
                switch (searchType) {
                    case R.id.wordbuilder:
                        // only difference between this and anagram is changing the length filter
                        //tcursor = databaseAccess.getCursor_subanagrams(term, filters, ordering);
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

                        databaseAccess.open();
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                "WHERE (" + lenFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
//                            String word = precursor.getString(1);
                            char[] b = word.toCharArray();
                            if (databaseAccess.isAnagram(first, b, blankcount)) {
                                matrixCursor.addRow(get_CursorRow(precursor));
                            }
                            if (LexData.getMaxList() > 0)
                                if (matrixCursor.getCount() == LexData.getMaxList())
                                    cancel(false);

                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        databaseAccess.close();
                        cursor = matrixCursor;
                        break;
                    case R.id.containsall:
                        // superanagrams
                        if (term.trim().length() < 2)
                            return null;
                        lenFilter = String.format("Length(Word) <= %1$s AND Length(Word) >= %2$s", LexData.getMaxLength(), term.length());

                        // initialize word to anagram
                        for (c = 0; c < a.length; c++) {
                            if (a[c] == '?') {
                                blankcount++;
                                continue;
                            }
                            first[a[c] - 'A']++;
                        }

                        String alpha = term.replaceAll("[^A-Za-z]+", "");
                        StringBuilder speedFilter = new StringBuilder();
                        for (int letter = 0; letter < alpha.length() && letter < 3; letter++)
                            speedFilter.append(" AND Word LIKE '%" + alpha.substring(letter,letter+1) + "%' ");

                        // LIKE filters the initial search by the first letter
                        databaseAccess.open();
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                " WHERE (" + lenFilter +
                                speedFilter +
                                filters +
                                " ) " + ordering);

                        while (precursor.moveToNext()) {
                            String word = precursor.getString(cache.getColumnIndex(precursor,"Word"));
//                            String word = precursor.getString(precursor.getColumnIndex("Word"));
                            char[] b = word.toCharArray();
                            if (databaseAccess.containsall(first, b, blankcount)) {
                                matrixCursor.addRow(get_CursorRow(precursor));
                            }
                            if (LexData.getMaxList() > 0)
                                if (matrixCursor.getCount() == LexData.getMaxList())
                                    cancel(false);
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }
                        }
                        precursor.close();
                        databaseAccess.close();
                        cursor = matrixCursor;
                        break;

                    case R.id.Joins:
                        databaseAccess.open();
                        precursor = databaseAccess.rawQuery("SELECT WordID as _id, Word, WordID, FrontHooks, BackHooks, " +
                                "InnerFront, InnerBack, Anagrams, ProbFactor, OPlayFactor, Score \n" +
                                "FROM     `" + LexData.getLexName() + "` \n" +
                                " WHERE (" + lenFilter +
                                filters +
                                " AND Word LIKE '__%" + term + "%__'" +
                                " ) " + ordering);

                        String front, back;
                        while (precursor.moveToNext()) {
                            String text = precursor.getString(precursor.getColumnIndex("Word"));
                            // LOOK FOR 0 BASED INDEX OF INSERTED LETTER
                            for (int i = 2; i < (text.length() - term.length()) ; i++)
                            {
                                if (text.substring(i, i + term.length()).equals( term))
                                {
                                    front = text.substring(0, i );
                                    back = text.substring(i + term.length());

                                    if (databaseAccess.wordJudge(front)) {
                                        if (databaseAccess.wordJudge(back)) {
                                            // replace letter with lower case
                                            String finding = front + term.toLowerCase() + back;
                                            String [] columnValues = new String[11];
                                            for (int j = 0; j < 11; j++)
                                                columnValues[j] = precursor.getString(j);
                                            columnValues[precursor.getColumnIndex("Word")] = finding;
                                            matrixCursor.addRow(columnValues);
                                            if (unfiltered)
                                                if (LexData.getMaxList() > 0)
                                                    if (matrixCursor.getCount() == LexData.getMaxList())
                                                        break;
                                        }
                                    }
                                }
                            }
                            if (isCancelled()) {
                                precursor.close();
                                //Log.i("Count: ", Integer.toString(matrixCursor.getCount()));
                                cursor = matrixCursor;
                                break;
                            }

                        }
                        precursor.close();
                        databaseAccess.close();
                        cursor = matrixCursor;
                        break;

                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                //called on ui thread
                if (this.dialog != null) {
                    this.dialog.dismiss();
                }
                cursor = matrixCursor;
//                databaseAccess.close();
                displayResults();
                return;
            }

        };
        task.execute();
        return cursor;
    }

}