package com.tylerhosting.hoot.hoot;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * A {@link PreferenceActivity} that presents a set of application menu. On
 * handset devices, menu are presented as a single list. On tablets,
 * menu are split by category, with category headers shown to the left of
 * the list of menu.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */

public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    String themeName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // themeName = Utils.setStartTheme(this);
        Log.wtf("Tablet", "SetAct");

        SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(this);

        pref.registerOnSharedPreferenceChangeListener(this);
        setupActionBar();
    }

    protected void onResume() {
        super.onResume();
    /* if (Utils.themeChanged(themeName, this)) {
            Utils.setNewTheme(this);
            recreate();
        }
    */

    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        //loadHeadersFromResource(R.xml.menu, target);
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("lexicon".equals(key))
            recreate();
        if ("tileset".equals(key))
            recreate();
        if ("database".equals(key))
            recreate();
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane menu UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        DatabaseAccess databaseAccess;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            Preference btnStorage = findPreference("storage");
            btnStorage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{WRITE_EXTERNAL_STORAGE},
                            1);
                    return true;
                }
            });

            Preference btnDatabase = findPreference("database");
            btnDatabase.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    selectDatabase();

                    return true;
                }
            });

            Preference btnLexicon = findPreference("lexicon");
            btnLexicon.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    selectLexicon();
                    return true;
                }
            });

            Log.i("SettingsOpenPrefs", findPreference("lexicon").toString());


            Preference btnTileSet = findPreference("tileset");
            btnTileSet.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    selectTiles();
                    return true;
                }
            });

            Preference btnTileColor = findPreference("tilecolor");
            btnTileColor.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    selectTileColor();
                    return true;
                }
            });





            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("user"));
            bindPreferenceSummaryToValue(findPreference("database"));
            bindPreferenceSummaryToValue(findPreference("lexicon"));
            bindPreferenceSummaryToValue(findPreference("tileset"));
            bindPreferenceSummaryToValue(findPreference("wordlength"));
            bindPreferenceSummaryToValue(findPreference("listlimit"));

            bindPreferenceSummaryToValue(findPreference("listfont"));
            bindPreferenceSummaryToValue(findPreference("tapping"));
            bindPreferenceSummaryToValue(findPreference("altending"));
            bindPreferenceSummaryToValue(findPreference("theme"));
            bindPreferenceSummaryToValue(findPreference("cardlocation"));
            bindPreferenceSummaryToValue(findPreference("cardcount"));

//            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
//            String tc = Integer.toString(settings.getInt("tilecolor", 0xffffffff));
//
//            bindPreferenceSummaryToValue(findPreference("tilecolor"));

//            bindPreferenceSummaryToValue(findPreference("example_list"));
        }


        @Override
        public void onResume() {
            super.onResume();
            Log.i("SettingsResumePrefs", findPreference("lexicon").toString());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity().getApplicationContext(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        public void selectDatabase() {
            if (!Utils.permission(getActivity())) // requests permission
                if (ContextCompat.checkSelfPermission(getActivity(),
                        WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) // tests permission
                    return;



// if saf, use internal, else select from documents
            File mPath;

///////////// ENABLE THIS WHEN ABLE TO IMPORT DATABASES


//            if(Utils.usingSAF()) {
////                String internalDBPath = getFilesDir().getAbsolutePath().replace("files", "databases") + File.separator + Flavoring.getflavoring();
//                mPath = new File(LexData.internalFilesDir);
//            }
//            else {
//                mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
//            }

            mPath = new File(Environment.getExternalStorageDirectory() + "//Documents//");
            Log.d("mPath", mPath.getPath());

//            File mPath = new File(Environment.getExternalStorageDirectory() +"//Documents//" );
            FileDialog fileDialog = new FileDialog(getActivity(), mPath, "db3");
            fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
                public void fileSelected(File file) {
                    String full = file.getAbsolutePath();
                    LexData.setDatabase(getActivity().getApplicationContext(),file.getName());
                    LexData.setDatabasePath(getActivity().getApplicationContext(),full.substring(0,full.lastIndexOf(File.separator)));
                    Utils.setDatabasePreference(getActivity().getApplicationContext());
                    databaseAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());

                    SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                    String lexicon = shared.getString("lexicon","");
                    SharedPreferences.Editor prefs = shared.edit();

                    if (!databaseAccess.isValidDatabase()) {
                        databaseAccess.defaultDBLexicon(getActivity().getApplicationContext());
                        databaseAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext(), LexData.getDatabasePath(), LexData.getDatabase());
                        prefs.putString("database", "Internal");
                        prefs.apply();
                        Toast.makeText(getActivity(), file.getName() + " is not a valid Hoot database. \r\n Resetting to default database" , Toast.LENGTH_LONG).show();
                    }

                    // CHECK BLANK LEXICON
                    if (lexicon == "") {
                        lexicon = databaseAccess.get_firstValidLexicon();
                        if (lexicon == "") {
                            // if no valid lexicons, make dummy
                            databaseAccess.MakeDummyLexicon();
                            lexicon = databaseAccess.get_firstValidLexicon();

                            Toast.makeText(getActivity(), "No lexicons are configured for app use. setting to dummy lexicon. \r\n Select a lexicon to configure ", Toast.LENGTH_LONG).show();
                        }
                        setLexicon(lexicon);
                    }

                    else {
                        // IF LEXICON IS NOT BLANK, AND SAME LEXICON EXISTS
                        if (databaseAccess.lexiconExists(lexicon)) { // if same lexicon exists
                            if (databaseAccess.checkLexicon(lexicon)) // if lexicon is valid
                                setLexicon(lexicon);
                            else {
                                Toast.makeText(getActivity(), "Lexicon " + lexicon + " is not configured for app use. Getting first valid lexicon ", Toast.LENGTH_LONG).show();
                                lexicon = databaseAccess.get_firstValidLexicon();
                                if (lexicon == "") {
                                    // if no valid lexicons, make dummy
                                    databaseAccess.MakeDummyLexicon();
                                    lexicon = databaseAccess.get_firstValidLexicon();

                                    Toast.makeText(getActivity(), "No lexicons are configured for app use. setting to dummy lexicon. \r\n Select a lexicon to configure " , Toast.LENGTH_LONG).show();
                                }
                                setLexicon(lexicon);
                            }
                        }

                        // SAME LEXICON DOESN'T EXIST, GET FIRST
                        else { // same lexicon doesn't exist
                            Toast.makeText(getActivity(), "Lexicon " + lexicon + " doesn't exist in current database. Getting first valid lexicon", Toast.LENGTH_LONG).show();
                            lexicon = databaseAccess.get_firstValidLexicon();
                            if (lexicon == "") {
                                // if no valid lexicons, make dummy
                                databaseAccess.MakeDummyLexicon();
                                lexicon = databaseAccess.get_firstValidLexicon();

                                Toast.makeText(getActivity(), "No lexicons are configured for app use. setting to dummy lexicon. \r\n Select a lexicon to configure ", Toast.LENGTH_LONG).show();
                            }
                            setLexicon(lexicon);
                        }
                    }
                }
            });
            fileDialog.showDialog();
        }

        public void selectLexicon(){
            // only called from Select options, not when loading
            databaseAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());
            ArrayList<Structures.Lexicon> lexiconList = databaseAccess.get_lexicons();

            if (!lexiconList.isEmpty()) {
                ArrayList<String> options= new ArrayList<>();
                for (int c = 0; c < lexiconList.size(); c++) {
                    options.add(lexiconList.get(c).LexiconName);
                }
                final String[] opts;
                opts = options.toArray(new String[]{});
                Log.d("opts", String.valueOf(opts.length));


                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Select");
                builder.setItems(opts, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        // opts[item] is the name of the lexicon

                        dialog.dismiss();
                        setLexicon(opts[item]);

                    }
                });
                builder.show();
                SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                Log.i("SetSelectLexiconPrefs", shared.getString("lexicon", "x"));

            }
            else {
                Toast.makeText(getActivity(), "Empty lexicon. \r\nIgnoring " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
                if (true) return;

//5                Toast.makeText(getActivity(), "Empty lexicon. \r\nResetting to default database/lexicon " + LexData.getLexName(), Toast.LENGTH_SHORT).show();
                databaseAccess.defaultDBLexicon(getActivity().getApplicationContext());
                Utils.setDatabasePreference(getActivity().getApplicationContext());
                setLexicon(LexData.getLexName());

            }
        }
        public void setLexicon(String lexname) { // sets LexData and prefs; this handles whether or not valid; don't set before calling
            SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
//            SharedPreferences.Editor prefs = shared.edit();

            databaseAccess = DatabaseAccess.getInstance(getActivity().getApplicationContext() , LexData.getDatabasePath(), LexData.getDatabase());
            Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexname);
            // need to check lexicon before changing
            if (!databaseAccess.checkLexicon(lexicon))
                Utils.lexAlert(getActivity(),lexname); // not getApplicationContext
            else {
                LexData.setLexicon(getActivity().getApplicationContext(), lexicon.LexiconName);
                Toast.makeText(getActivity(), "Using Lexicon " + lexicon.LexiconName, Toast.LENGTH_SHORT).show();
                Toast.makeText(getActivity(), lexicon.LexiconNotice, Toast.LENGTH_LONG).show();
                Utils.setLexiconPreference(getActivity().getApplicationContext());
                Log.i("SettingsSetLexiconPrefs", shared.getString("lexicon", "x"));

            }
        }

        public void selectTiles() {
            AlertDialog dialog;
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            ArrayList<String> options=new ArrayList<String>();
            for (int c = 0; c < LexData.tileset.length; c++) {
                options.add(LexData.tileset[c]);
            }
            final String[] opts;
            opts = options.toArray(new String[]{});

            builder.setTitle("Please Select a Tile Set...");
            builder.setItems(opts, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    LexData.setTileset(which);
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor prefs = settings.edit();
                    prefs.putString("tileset", LexData.tileset[which]);
                    prefs.apply();
                }
            });
            dialog = builder.create();
            dialog.show();
        }


        public void selectTileColor() {
            startActivity(new Intent(getActivity(), TileColorActivity.class));


//            AlertDialog dialog;
//            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
//
//
//
//            ArrayList<Color> options= new ArrayList<>();
//            for (int c = 0; c < LexData.tilecolors.length; c++) {
//                options.add(Color.valueOf(Color.parseColor(LexData.tilecolors[c]));
//            }
//            final String[] opts;
//            opts = options.toArray(new String[]{});
//
//
//
//            builder.setTitle("Please Select a Tile Color...");
//
//            builder.setItems(opts, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                    LexData.setTileColor(which);
//                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
//                    SharedPreferences.Editor prefs = settings.edit();
//                    prefs.putString("tilecolor", LexData.getTileColor());
//                    prefs.apply();
//                }
//            });
//            dialog = builder.create();
//            dialog.show();
        }

    }

    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane menu UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane menu UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
