package com.tylerhosting.hoot.hoot;


import static android.view.View.GONE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;

public class cLookupActivity
extends AppCompatActivity {

//        private Handler mWaitHandler = new Handler();

        SharedPreferences shared;
        SharedPreferences.Editor prefs;

        DatabaseAccess databaseAccess;
        Button search;
        EditText etEntry;
        TextView status, results;
        Keyboard basicKeyboard; // basic
        KeyboardView mKeyboardView;
CharSequence text;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.activity_lookup);
            text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            Utils.setStartTheme(this);

            shared = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs = shared.edit();

            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
            initializeDatabase();

            String lexicon = shared.getString("lexicon", "");
            setLexicon(lexicon);

            populateResources();

            createNotificationChannel();

            executeSearch();
        }

        private void executeSearch() {
String word = text.toString().toUpperCase();
//            String word = "HYZER".toUpperCase();
//            processString(word);
            status.setText(R.string.searching);

            //String word = etEntry.getText().toString().toUpperCase();
            if (word.isEmpty())
                return;
            results.setMovementMethod(new ScrollingMovementMethod());
            results.setText(word + " ");
            Toast.makeText(this, "Searching. . .", Toast.LENGTH_SHORT).show();

            databaseAccess.open();

//            if (!databaseAccess.wordJudge(word)) {
//                String notice = word + " isn't a valid Scrabble word in " + LexData.getLexName() + "\r\n";
//                results.setText(notice);
//            }

            String def = databaseAccess.getDefinition(word);
            results.append(def);

            String lex = databaseAccess.getValidLexicons(word);
            results.append(lex);

            String notlex = databaseAccess.getInValidLexicons(word);
            results.append(notlex);

            //       etEntry.setText("");
            databaseAccess.close();
            //       hideCustomKeyboard();
        }




        private void initializeDatabase() {
            LexData.internalFilesDir = this.getFilesDir().getAbsolutePath().replace("files", "databases");

            // need to save database and path, and reset when starting
            String fullpath = shared.getString("database", "");
            if (fullpath.equals("")) {
                fullpath = "Internal";
                prefs.putString("database", "Internal");
                prefs.apply();
            }

            // set database lexdata to default if internal
            if (fullpath.equals("Internal") || !fullpath.contains(File.separator))  {
                Flavoring.addflavoring(getApplicationContext()); // sets database
                LexData.setDatabasePath(getApplicationContext(), "");
            }
            // set database lexdata to saved path
            else {
                LexData.setDatabase(getApplicationContext(), fullpath.substring(fullpath.lastIndexOf(File.separator)));
                LexData.setDatabasePath(getApplicationContext(), fullpath.substring(0, fullpath.lastIndexOf(File.separator)));
            }

            databaseAccess = DatabaseAccess.getInstance(getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
        }



        public void setLexicon(String lexname) {
            Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexname);
            LexData.setLexicon(getApplicationContext(), lexicon.LexiconName);

            // Don't attempt to extract in Splash
            if (!databaseAccess.checkLexicon(lexicon)) {
                Toast.makeText(getApplicationContext(), "Failed to load lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
                databaseAccess.defaultDBLexicon(getApplicationContext());
                Utils.setDatabasePreference(this);
                Toast.makeText(getApplicationContext(), "Resetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(this, "Using Lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
            Toast.makeText(this, LexData.getLexicon().LexiconNotice, Toast.LENGTH_LONG).show();
            Utils.setLexiconPreference(this);
        }

        public void createNotificationChannel() {
            final String CHANNEL_ID = "Hoot";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "Hoot";
                String description = "Hoot Notifications";
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }

        private void populateResources() {

//            search = (Button) findViewById(R.id.btnLookup);
//            search.setOnClickListener(doSearch);
//
//            results = findViewById(R.id.txtResults);
//            status = findViewById(R.id.lblStatus);
//            etEntry = findViewById(R.id.etWord);
//            etEntry.addTextChangedListener(entryWatcher);
//            etEntry.setOnEditorActionListener(entryAction);
//            etEntry.setFilters(new InputFilter[]{new InputFilter.AllCaps()});
//
//            TextView lexTitle = findViewById(R.id.lexName);
//            lexTitle.setText(LexData.getLexName());
//            TextView note = findViewById(R.id.lexNotice);
//            note.setText(LexData.getLexicon().LexiconNotice);


            search = (Button) findViewById(R.id.btnLookup);
            search.setVisibility(GONE);
            etEntry = findViewById(R.id.etWord);
            etEntry.setVisibility(GONE);

            results = findViewById(R.id.txtResults);
            status = findViewById(R.id.lblStatus);

            TextView lexTitle = findViewById(R.id.lexName);
            lexTitle.setVisibility(GONE);

            lexTitle.setText(LexData.getLexName());
            TextView note = findViewById(R.id.lexNotice);
            note.setVisibility(GONE);

            note.setText(LexData.getLexicon().LexiconNotice);
        }

}
