package com.tylerhosting.hoot.hoot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.support.v4.app.ActivityCompat.startActivityForResult;

import static com.tylerhosting.hoot.hoot.Hoot.context;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import android.support.v4.app.FragmentActivity;

//import com.sun.mail.smtp.SMTPAddressFailedException;

class Utils {
    private static final String TAG = SQLiteAssetHelper.class.getSimpleName();

    static SharedPreferences shared;
    static SharedPreferences.Editor prefs;
    private static SQLiteDatabase cards;

    public static boolean usingLegacy() {
        // Environment.isExternalStorageLegacy() is only available after API 29
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Environment.isExternalStorageLegacy())
                return true;
            else return false;
        }
        return true; // build is less than 29
    }

    public static boolean usingSAF() {
        boolean saf;
        saf = shared.getBoolean("saf", false);

        if (!usingLegacy() || saf)
            return true;

        return false;
    }

    public static boolean isFirstInstall(Context context) {
        try {
            long firstInstallTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).firstInstallTime;
            long lastUpdateTime = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime;
//            String ver = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).
            return firstInstallTime == lastUpdateTime;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }

    }

    public static String limitStringer(int limit, int offset) {
        StringBuilder builder = new StringBuilder();

        // assumes values are assigned
        if (limit == 0)
            limit = LexData.getMaxList();

        if (limit > 0 || offset > 0) {
            // must have a limit if there is an offset
            if (limit == 0)
                builder.append(" LIMIT -1 ");
            else
                builder.append(" LIMIT " + limit);

            if (offset > 0) {
                // don't reduce it; already done
                builder.append(" OFFSET " + offset + " ");
            }
            Log.d("limitStringer", builder.toString());

            return builder.toString();
        }
        else
            return "";
    }



    public static boolean themeChanged(String themeName, Context context) {
//        SharedPreferences shared;
        shared = PreferenceManager.getDefaultSharedPreferences(context);
        String themeAfter = shared.getString("theme", null);
        if (!(themeName.equals(themeAfter))) {
            return true;
//            themeName = themeAfter;
//            Toast.makeText(this, "set theme", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
    public static String setStartTheme(Activity activity) {
//        SharedPreferences shared;
        shared = PreferenceManager.getDefaultSharedPreferences(activity);
        // sets theme for first use
        String themeName = shared.getString("theme", "Light Theme");

        // save theme to preferences
        SharedPreferences.Editor prefs = shared.edit();
        prefs.putString("theme", themeName);
        prefs.apply();

        setNewTheme(themeName, activity);
        return themeName;
    }
    public static String getTheme(Activity activity) {
//        SharedPreferences shared;
        shared = PreferenceManager.getDefaultSharedPreferences(activity);
        // sets theme for first use
        String themeName = shared.getString("theme", "Light Theme");
        return themeName;
    }
    public static void setNewTheme(String themeName, Activity activity) {
        switch (themeName) {
            case "Dark Theme":
                activity.setTheme(R.style.DarkTheme);
                break;
            case "Red Theme":
                activity.setTheme(R.style.RedTheme);
                break;
            case "Pink Theme":
                activity.setTheme(R.style.PinkTheme);
                break;
            case "Light Theme":
            default:
                activity.setTheme(R.style.LightTheme);
                break;
        }

    }
    public static void setNewTheme(Activity activity) {
//        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(activity);
        String themeName = shared.getString("theme", "Light Theme");
        switch (themeName) {
            case "Dark Theme":
                activity.setTheme(R.style.DarkTheme);
                break;
            case "Red Theme":
                activity.setTheme(R.style.RedTheme);
                break;
            case "Pink Theme":
                activity.setTheme(R.style.PinkTheme);
                break;
            case "Light Theme":
            default:
                activity.setTheme(R.style.LightTheme);
                break;
        }
        // will i ever need to save preference here
    }
    public static ProgressDialog themeDialog(Context context) {
        ProgressDialog pd;
//        SharedPreferences
                shared = PreferenceManager.getDefaultSharedPreferences(context);
        String themeName = shared.getString("theme", "Light Theme");
        switch (themeName) {
            case "Dark Theme":
                pd = new ProgressDialog(context,R.style.darkAlertDialog);
                break;
            case "Light Theme":
            default:
                pd = new ProgressDialog(context);
                break;
        }
        return pd;
    }


    // String Utils
    public static boolean isParsable(String input){
        try{
            Integer.parseInt(input);
            return true;
        }catch(Exception e){
            return false;
        }
    }
    public static int analyzeMin(String pattern) {

        if(pattern.contains("<"))

        {
            Pattern p = Pattern.compile("\\<(.*?)\\>");
            Matcher m = p.matcher(pattern);
            do {
                while (m.find()) {
                    //System.out.println(m.group(1));
                    String answer = m.group(1);
                    //String lettersmatching = get_ReplacementLetters(answer);
                    String original = "<" + answer + ">";
                    pattern = pattern.replace(original, "?");
                }

            } while (pattern.contains("<") & pattern.contains(">"));
        }


        // MODIFY PATTERN (LENGTH) BY REPLACING <..> WITH ?

        // first check if no minumum and return 2
//        String testMin = pattern.replaceAll("~", ""); // does not contain ~
//        if (testMin.length() != pattern.length())
//            return 2;

        // ~ makes possible length shorter
        // ignore for minimum = String globals = "*+\\";
        pattern = pattern.replaceAll("[*0@]", ""); // * only lengthens the result, ignore 0
        // calculate minimum
        String unused = pattern.replaceAll("[cvABCDEFGHIJKLMNOPQRSTUVWXYZ123456789?.]", "");



        if (unused.length() == 0)
            return pattern.length();
        else
            return 2;
    }
    public static int analyzeMax(String pattern) {

        if(pattern.contains("<"))

        {
            Pattern p = Pattern.compile("\\<(.*?)\\>");
            Matcher m = p.matcher(pattern);
            do {
                while (m.find()) {
                    //System.out.println(m.group(1));
                    String answer = m.group(1);
                    //String lettersmatching = get_ReplacementLetters(answer);
                    String original = "<" + answer + ">";
                    pattern = pattern.replace(original, "?");
                }

            } while (pattern.contains("<") & pattern.contains(">"));
        }


        String testMin = pattern.replaceAll("[*@]", ""); // does not contain *
        if (testMin.length() != pattern.length())
            return LexData.getMaxLength();

        // else calculate maximum
        pattern = pattern.replace("0", "");
        String unused = pattern.replaceAll("[cvABCDEFGHIJKLMNOPQRSTUVWXYZ123456789?.]", "");
        if (unused.length() == 0)
            return pattern.length();
        else
            return LexData.getMaxLength();
    }
    public static String validateString(String text) {
    String invalid = "1234567890*?,+[]<>(|){}\\~-";
    StringBuilder stringBuilder = new StringBuilder();
    text = text.toUpperCase();
    // remove nested <>

/*    if (text.contains("<"))
    {
        do
        {
            String answer = text.Split('<', '>')[1];
            // String answer = exp.Substring(exp.IndexOf("<") + 1, exp.Length - exp.IndexOf(">"));
            String original = "<" + answer + ">";
            text = text.Replace(original, "");
        } while (text.Contains("<"));
    }

 */

    for (int i = 0; i < text.length(); i++)
//    foreach (var c in text)
    {
        String addition = Character.toString(text.charAt(i));
        if (invalid.contains(addition))
            continue;
        stringBuilder.append(text.charAt(i));
    }

    return stringBuilder.toString();
}
    public static String sortString(String word) {
        char[] charArray = word.toCharArray();
        Arrays.sort(charArray);
        return String.valueOf(charArray);
    }
    public static String deSpaceString (String word) {
        return word.replaceAll("\\s","");
    }
    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }
    public static String buildPattern(String pattern) {
        if (pattern.trim() == "")
            return null;
        // parent allows updating progress bar on form
        // rebuild string for regexp
        // trial
        //                    int min = analyzeMin(pattern);
        //                    int max = analyzeMax(pattern);

        String tenString = LexData.valueSet(10);
        ; // "[QZ]"
        String nineString = LexData.valueSet(9);
        String eightString = LexData.valueSet(8);
        String sevenString = LexData.valueSet(7);
        String sixString = LexData.valueSet(6);
        String fiveString = LexData.valueSet(5);
        String fourString = LexData.valueSet(4);
        String threeString = LexData.valueSet(3);
        String twoString = LexData.valueSet(2);
        String oneString = LexData.valueSet(1); // "[AEIOULNRST]";

        String carrot = "^";
        String exp = carrot + pattern.replace('?', '.') + "$";

        //        exp = exp.replace("@", "[AEIOU]");
        //        exp = exp.replace("#", "[^AEIOU]");
        exp = exp.replace("v", "[AEIOU]");
        exp = exp.replace("c", "[^AEIOU]");
        exp = exp.replace("~", "?");
        exp = exp.replace("10", tenString);
        exp = exp.replace("1", oneString);
        exp = exp.replace("{" + oneString, "{1"); // change back if quantifier
        exp = exp.replace("\\" + oneString, "\\1"); // change back if capture

        exp = exp.replace("2", twoString);
        exp = exp.replace("{" + twoString, "{2"); // change back if quantifier
        exp = exp.replace("\\" + twoString, "\\2"); // change back if quantifier

        exp = exp.replace("3", threeString);
        exp = exp.replace("{" + threeString, "{3"); // change back if quantifier
        exp = exp.replace("\\" + threeString, "\\3"); // change back if quantifier

        exp = exp.replace("4", fourString);
        exp = exp.replace("{" + fourString, "{4"); // change back if quantifier
        exp = exp.replace("\\" + fourString, "\\4"); // change back if quantifier

        exp = exp.replace("5", fiveString);
        exp = exp.replace("{" + fiveString, "{5"); // change back if quantifier
        exp = exp.replace("\\" + fiveString, "\\5"); // change back if quantifier

        exp = exp.replace("6", sixString);
        exp = exp.replace("{" + sixString, "{6"); // change back if quantifier
        exp = exp.replace("\\" + sixString, "\\6"); // change back if quantifier

        exp = exp.replace("7", sevenString);
        exp = exp.replace("{" + sevenString, "{7"); // change back if quantifier
        exp = exp.replace("\\" + sevenString, "\\7"); // change back if quantifier

        exp = exp.replace("8", eightString);
        exp = exp.replace("{" + eightString, "{8"); // change back if quantifier
        exp = exp.replace("\\" + eightString, "\\8"); // change back if quantifier

        exp = exp.replace("9", nineString);
        exp = exp.replace("{" + nineString, "{9"); // change back if quantifier
        exp = exp.replace("\\" + nineString, "\\9"); // change back if quantifier

        exp = exp.replace("0", "");

        exp = exp.replace("*", ".*");
        exp = exp.replace("@", ".*");
        exp = exp.toUpperCase();
        return exp;
    }



    public static int getValue(String word) {
        int value = 0;
        char[] b = word.toCharArray();

        for (int x = 0; x < b.length; x++)
        {
            // bc - ghj - lmn - p - uvy are different

            // b[x] is the character
            value += LexData.Tiles.tiles.charvalue[b[x] - 'A'];
        }
        return value;
    }
    public static void wordDefinition(Context context, String word, String details) {
//        SharedPreferences shared;
        shared = PreferenceManager.getDefaultSharedPreferences(context);
        int listfontsize = Integer.parseInt(shared.getString("listfont","24"));
        AlertDialog alertDialog;

        int matchColor;
        String themeName = shared.getString("theme", "Light Theme");
        switch (themeName) {
            case "Dark Theme":
                matchColor = Color.YELLOW;
                alertDialog = new AlertDialog.Builder(context, R.style.darkAlertDialog).create();
                break;
            default:
            case "Light Theme":
                matchColor = Color.BLUE;
                alertDialog = new AlertDialog.Builder(context).create();
                break;
        }

        // Set Custom Title
        TextView title = new TextView(context);
        // Title Properties

        title.setText(word);
        title.setPadding(10, 90, 10, 10);   // Set Position
        title.setGravity(Gravity.CENTER);

        title.setTextColor(matchColor);

        title.setTextSize ((int)(listfontsize * 1.1)); //25

        AssetManager assetManager = context.getAssets();
        Typeface tile = Typeface.createFromAsset(assetManager, "fonts/nutiles.ttf");
        title.setTypeface(tile);
        alertDialog.setCustomTitle(title);

        // Set Message
        TextView msg = new TextView(context);

        // add to details if desired
        // Integer.toString(Utils.getValue(selectedWord)) + " points\n" +
        msg.setText(details);
        msg.setTextSize((int)(listfontsize * .8));
        msg.setPadding(10,4,10,4);

        msg.setGravity(Gravity.CENTER_HORIZONTAL);
        alertDialog.setView(msg);

        // Set Button
        // you can more buttons
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE,"OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // Perform Action on Button
            }
        });

        new Dialog(context.getApplicationContext());
        alertDialog.show();

        // Set Properties for OK Button
        final Button okBT = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        LinearLayout.LayoutParams neutralBtnLP = (LinearLayout.LayoutParams) okBT.getLayoutParams();
        neutralBtnLP.gravity = Gravity.FILL_HORIZONTAL;
        okBT.setPadding(50, 10, 10, 10);   // Set Position
        okBT.setTextColor(matchColor);
        okBT.setLayoutParams(neutralBtnLP);
        okBT.setGravity(Gravity.CENTER);

        final Button cancelBT = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams negBtnLP = (LinearLayout.LayoutParams) okBT.getLayoutParams();
        negBtnLP.gravity = Gravity.FILL_HORIZONTAL;
        cancelBT.setTextColor(matchColor);
        cancelBT.setLayoutParams(negBtnLP);
    }
    public static boolean permission(Context context) {
        if (ContextCompat.checkSelfPermission(context,
                WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            int okay = 1;
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{WRITE_EXTERNAL_STORAGE},
                    okay);

            return ContextCompat.checkSelfPermission(context,
                    WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;

        } else {
            return true;
            // Permission has already been granted
        }
    }
    public  boolean isWriteStoragePermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted2");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }
    public static boolean fileExist(String fname){
        File file = new File(fname);
        return file.exists();
    }


    public static void setDatabasePreference(Context context) {
//        SharedPreferences
                shared = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor prefs = shared.edit();
        if (LexData.getDatabasePath().equals(LexData.internalFilesDir) && LexData.getDatabase().equals(Flavoring.getflavoring()))
//            if (LexData.getDatabasePath().equals(LexData.internalFilesDir) || !usingLegacy())
            prefs.putString("database", "Internal");
        else
            prefs.putString("database", LexData.getDatabasePath() + "/" + LexData.getDatabase());
        prefs.apply();
    }
    public static void setLexiconPreference(Context context) {
//        SharedPreferences
                shared = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor prefs = shared.edit();
        prefs.putString("lexicon", LexData.getLexName());
        prefs.apply();
    }


    // Database copying
    // copies from internal, not assets
    public static void copyInternalDatabase(Context context, String path, File destination) {
        InputStream inputStream = null;
        String fname = Flavoring.getflavoring(); // sets database
        File source = new File(LexData.internalFilesDir  + File.separator + fname);


        try {
            inputStream = new FileInputStream(source);  // 2nd line
            copyStreamTo(inputStream, destination);
        } catch (IOException e) {
            //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyDB", "Failed to close the destination");
            }
        }
    }
    // copies from assets, not internal
    public static void copyDatabaseFromAssets(Context context, String path, File destination) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(path);
//            Log.e("CopyDB", inputStream.toString());
            copyStreamTo(inputStream, destination);
        } catch (IOException e) {
            //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyDB", "Failed to close the destination");
            }
        }
    }
    public static void copyFilesFromAssets(Context context, String path, File destination) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(path);
//            Log.e("CopyDB", inputStream.toString());
            copyStreamTo(inputStream, destination);
        } catch (IOException e) {
            //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyFiles", "Failed to close the destination");
            }
        }
    }
    // copied from ToolsActivity where used to copy database to Documents
    public static boolean BackupDatabase(Context context, String internalDatabase) {
        String DB_NAME = Flavoring.getflavoring();
//        String DB_NAME = LexData.getDatabase();
        String DEST_PATH = LexData.internalFilesDir  + File.separator + DB_NAME;;


//        if (!permission(context))
//            return false;
//
        // setup output handle
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
//            DEST_PATH = Environment.getExternalStoragePublicDirectory("Documents").toString();
//        else
//            DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        // need to change aHoot to aHootVER, etc
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context, LexData.getDatabasePath(), LexData.getDatabase());
        String version = String.valueOf(databaseAccess.getVersion());
        String[] nameParts = DB_NAME.split("\\.(?=[^\\.]+$)");
        String backupName = nameParts[0] + version + "." + nameParts[1];
//        File destination = new File(DEST_PATH  + File.separator + backupName);
        String dest = LexData.internalFilesDir  + File.separator + backupName;
        Log.d("Utils BackupDatabase", internalDatabase);
        Log.d("Utils BackupDatabase", dest);
        File destination = new File(LexData.internalFilesDir  + File.separator + backupName);

        // Don't make multiple backups
//        int num = 0;
//        while (destination.exists()) {
//            num++;
//            String[] alt = backupName.split("\\.(?=[^\\.]+$)");
//            String dest = DEST_PATH  + File.separator + alt[0] + "(" + num + ")" + "." + alt[1];
//            destination  = new File(DEST_PATH  + File.separator + alt[0] + "(" + num + ")" + "." + alt[1]);
//            Log.d("utils", dest);
//        }

//        Toast.makeText(context, "Copying database to " + destination.getAbsolutePath(), Toast.LENGTH_LONG).show();

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(internalDatabase));  // 2nd line
//            inputStream =  context.openFileInput(internalDatabase);

            copyStreamTo(inputStream, destination);
        } catch (IOException e) {
            //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyDB", "Failed to close the destination");
            }
        }

//        Toast.makeText(context, "Copied database to " + destination.getAbsolutePath(), Toast.LENGTH_LONG).show();

        return true;
    }
    // used by copyAssetsDataBase
    public static void copyStreamTo(InputStream inputStream, File destination) {
        String dest=destination.getAbsolutePath();
        Log.d("destination", dest);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(destination);
Log.d("copyStream input", inputStream.toString());
Log.d("copyStream output", outputStream.toString());
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        } catch (FileNotFoundException e) {
            Log.e("CopyDB", "File Not Found");
            //throw new ExternalSQLiteOpenHelperException("Failed to open database destination");
        } catch (IOException e) {
            Log.e("CopyDB", "IO Error");
            //throw new ExternalSQLiteOpenHelperException("Failed to copy external database to the destination");
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyDB", "Failed to close the external database");
            }
        }
    }

//    public static String GetTextFile(String filespec) {
//
//        List<String> words;
////        words = Utils.getWordsFromFile(importfile.getText().toString());
//        words = Utils.getWordsFromFile(filespec);
//        Log.d("words in file", "Total " + words.size());
//
//        StringBuilder textlist = new StringBuilder();
//
//        textlist.append("'" + words.get(0)+ "'");
//        for(int c = 1; c < words.size(); c++) {
//            textlist.append(", '" + words.get(c) + "'");
//        }
//
//        return textlist.toString();
////        cursor = databaseAccess.getCursor_getWords(textlist.toString(),"", "");
////        Log.d("words in file", "cursor " + cursor.getCount());
//
////        Create comma separated list as in AnagramQuizActivity and call getCursor_getWords(String list)
//
//    }

    public static String GetTextFile(Context context, String  filespec)  {

        List<String> words;

//        SharedPreferences shared;
        shared = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String themeName = shared.getString("theme", "Light Theme");


//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || LexData.getUsername().equals("SAF")) {


//        Boolean saf = shared.getBoolean("saf",false);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || saf) {
        if (Utils.usingSAF()) {
            try {
                words = Utils.getWordsFromURI(context, filespec);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else
            words = Utils.getWordsFromFile(filespec);

        Log.d("words in file", "Total " + words.size());

        StringBuilder textlist = new StringBuilder();

        textlist.append("'" + words.get(0)+ "'");
        for(int c = 1; c < words.size(); c++) {
            textlist.append(", '" + words.get(c) + "'");
        }

        return textlist.toString();
//        cursor = databaseAccess.getCursor_getWords(textlist.toString(), ordering, limits, filters);
//        Log.d("words in file", "cursor " + cursor.getCount());

//        Create comma separated list as in AnagramQuizActivity and call getCursor_getWords(String list)

    }
    public static List<String> getWordsFromFile(String file) {
        BufferedReader sr = null;
        List<String> words = new ArrayList<>();
        String word;

        Log.e("File", file);
        String line = "";
        try {
            sr = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("Error",  "catch bufferedreader");
        }

        // get the words first
        do {
            try {
                line = sr.readLine();
            } catch (IOException oops) {
                Log.e("Error", "readline");
                break;
            }
            if (line == null)
                break;

            line = line.trim().toUpperCase(); // leading spaces okay in file
            if (line.isEmpty()) {
                continue;
            }
            if ((line.startsWith("//"))) {
                continue;
            }
            // add line to word list
            line = line.replaceAll("\t", " ");
            int sep = line.indexOf(' ');
            if (sep != -1) {
                word = line.substring(0, sep); // keyword
            }
            else
                word = line;
            Log.d("Word", word);

            words.add(word);

        } while (true);
        return words;
    }


    // URI utilities
    public static List<String> getWordsFromURI(Context context, String file) throws IOException {
//        private String readTextFromUri(Uri uri) throws IOException
        Uri uri = Uri.parse(file);

        StringBuilder stringBuilder = new StringBuilder();


//                String line;
//                while ((line = reader.readLine()) != null) {
//                    stringBuilder.append(line);
//                }
//            }
//            return stringBuilder.toString();


//        BufferedReader sr = null;
        List<String> words = new ArrayList<>();
        String word;

        Log.e("File", file);
        String line;

        try (InputStream inputStream =
                     context.getContentResolver().openInputStream(uri);
             BufferedReader sr = new BufferedReader(
//                         BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {

//        try {
//            sr = new BufferedReader(new FileReader(file));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.e("Error",  "catch bufferedreader");
//        }

            // get the words first
            do {
                try {
                    line = sr.readLine();
                } catch (IOException oops) {
                    Log.e("Error", "readline");
                    break;
                }
                if (line == null)
                    break;

                line = line.trim().toUpperCase(); // leading spaces okay in file
                if (line.isEmpty()) {
                    continue;
                }
                if ((line.startsWith("//"))) {
                    continue;
                }
                // add line to word list
                line = line.replaceAll("\t", " ");
                int sep = line.indexOf(' ');
                if (sep != -1) {
                    word = line.substring(0, sep); // keyword
                } else
                    word = line;
                Log.d("Word", word);

                words.add(word);

            } while (true);
            return words;
        }
    }
    public static boolean writeList(Context context, Cursor cursor, Uri uri) {
        int counter = 0;
        int column = cursor.getColumnIndex("Word");

        String sep = System.lineSeparator();
        counter++;
        cursor.moveToFirst();

        try {

            ParcelFileDescriptor pfd = context.getContentResolver().
                    openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());


            fileOutputStream.write((cursor.getString(column).toUpperCase() + sep).getBytes());
            while (cursor.moveToNext()) {
                counter++;
                fileOutputStream.write((cursor.getString(column).toUpperCase() + sep).getBytes());
            }
            fileOutputStream.close();
            Toast.makeText(context, "Saving " + counter + " words to " + uri.getLastPathSegment(), Toast.LENGTH_LONG).show();



            fileOutputStream.write(("Overwritten at " + System.currentTimeMillis() +
                    "\n").getBytes());
            // Let the document provider know you're done by closing the stream.
            fileOutputStream.close();
            pfd.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public static boolean exportList(Context context, String listName, Uri uri) {
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context, LexData.getDatabasePath(), LexData.getDatabase());

        databaseAccess.open();

        String sql = "Select ListID from WordLists where Upper(ListName) = '" + listName.toUpperCase() + "'";
        Log.d("sql", sql);
        Cursor cursor = databaseAccess.rawQuery(sql);
        cursor.moveToFirst();
//        Cursor cursor = databaseAccess.rawQuery("Select ListID from WordLists where Upper(ListName) = '" + listName.toUpperCase() + "'");
        int listID = cursor.getInt(cursor.getColumnIndex("ListID"));
        cursor.close();

        cursor = databaseAccess.getCursor_listwords(listID, "", "");

        int counter = 0;
        int column = cursor.getColumnIndex("Word");



        Cursor detailCursor = databaseAccess.getCursor_subjectList(listName);
        Log.d("listname", listName);

        detailCursor.moveToFirst();
        String lN = detailCursor.getString(detailCursor.getColumnIndex("ListName"));
        String lA = detailCursor.getString(detailCursor.getColumnIndex("ListAuthor"));
        String lC = detailCursor.getString(detailCursor.getColumnIndex("ListCredits"));
        String lD = detailCursor.getString(detailCursor.getColumnIndex("ListDescription"));
        String lS = detailCursor.getString(detailCursor.getColumnIndex("ListSource"));
        String lL = detailCursor.getString(detailCursor.getColumnIndex("ListLink"));
        String lAE = detailCursor.getString(detailCursor.getColumnIndex("ListAuthorEmail"));
        String catID = detailCursor.getString(detailCursor.getColumnIndex("CategoryID"));
        detailCursor.close();

        sql = "SELECT Category from tblListCategories WHERE CategoryID = '" + catID + "'";
        Cursor catCursor = databaseAccess.rawQuery(sql);
        catCursor.moveToFirst();
        String cat = catCursor.getString(catCursor.getColumnIndex("Category"));
        catCursor.close();


        try {

            ParcelFileDescriptor pfd = context.getContentResolver().
                    openFileDescriptor(uri, "w");
//            FileOutputStream fileOutputStream =
//                    new FileOutputStream(pfd.getFileDescriptor());

//        FileOutputStream writer =
//                new FileOutputStream(pfd.getFileDescriptor());

            FileWriter writer = new FileWriter(pfd.getFileDescriptor());

            if (lN != null && !lN.isEmpty())
                writer.append("//List Name: " + lN + "\r\n");
            if (lA != null && !lA.isEmpty())
                writer.append("//List Author: " + lA + "\r\n");
            if (lC != null && !lC.isEmpty())
                writer.append("//List Credits: " + lC + "\r\n");
            if (lD != null && !lD.isEmpty())
                writer.append("//List Description: " + lD + "\r\n");
            if (lS != null && !lS.isEmpty())
                writer.append("//List Source: " + lS + "\r\n");
            if (lL != null && !lL.isEmpty())
                writer.append("//List Link: " + lL + "\r\n");
            if (lAE != null && !lAE.isEmpty())
                writer.append("//List Email: " + lAE + "\r\n");
            if (cat != null && !cat.isEmpty())
                writer.append("//List Category: " + cat + "\r\n");




            String sep = System.lineSeparator();
            counter++;
            cursor.moveToFirst();

            writer.append(cursor.getString(column).toUpperCase() + sep);
            while (cursor.moveToNext()) {
                counter++;
                writer.append(cursor.getString(column).toUpperCase() + sep);
            }
            writer.close();
//            Toast.makeText(context(), "Saving " + counter + " words to " + filename, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    public static class DateUtils {
        public static Date addDays(Date date, int days) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, days); //minus number would decrement the days
            return cal.getTime();
        }
    }


    // used in creating card boxes
    public static String[] searchtypes = {"None", "Anagrams", "SubAnagrams", "SuperAnagrams", "Hooks",
            "FrontHooks", "BackHooks", "SurpriseHooks", "AnagramHooks", "Extensions",
            "FrontExtensions", "BackExtensions", "DoubleExtensions", "ShortExtensions", "Subwords",
            "BlankAnagrams", "Transpositions", "Misspells", "Joins", "Stretches",
            "Conjugate", "Pattern", "Parallel"
    };
    public static int getSearchType(String value) {
        if (Arrays.asList(searchtypes).contains(value))
            return java.util.Arrays.asList(searchtypes).indexOf(value);
        else
            return 0;
    }

    public static boolean lexAlert(final Context context, final String lexname) {
        // calls configureLexicon if selected

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context, LexData.getDatabasePath(), LexData.getDatabase());
        final boolean answer[] = new boolean[1];
        builder.setTitle("Lexicon not configured");
        builder.setMessage("The selected lexicon has not been configured for app use. " +
                "Continue to attempt extraction through the app, or used the PC version of Hoot to configure it.");

        builder.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                dialogInterface.cancel();
                dialogInterface.dismiss();
                answer[0] = true;
/*                Toast.makeText(context, "Extracting Lexicon " + lexname, Toast.LENGTH_LONG).show();
                Structures.Lexicon lexicon = databaseAccess.get_lexicon(lexname);
                LexData.setLexicon(context, lexicon.LexiconName);
                databaseAccess.MakeLexiconThread(context, lexicon, "");
*/
                databaseAccess.configureLexicon(context, lexname);

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                answer[0] = false;
//                dialogInterface.cancel();
            }
        });

        builder.show();
//        AlertDialog dialog = builder.create();
//        dialog.show();

        return answer[0];
    }
    public static boolean exitAlert(final Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.darkAlertDialog);
        //final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.alertDialogStyle);
        final DatabaseAccess databaseAccess = DatabaseAccess.getInstance(context, LexData.getDatabasePath(), LexData.getDatabase());

//        builder.setTitle("Exit");
//        builder.setMessage("Do you want to exit ??");
//        SharedPreferences
        shared = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("Exit: ", shared.getString("database", "") );
        Log.d("Exit: ", shared.getString("lexicon", ""));
        Log.d("Exit: ", LexData.getDatabasePath() + " - " + LexData.getDatabase());
        Log.d("Exit: ", LexData.getLexName());
        builder.setTitle("Do you want to exit ??");
        builder.setPositiveButton("Yes. Exit now!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                databaseAccess.close();
                ActivityCompat.finishAffinity((Activity)context);
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

    // https://stackoverflow.com/questions/27962116/simplest-way-to-encrypt-a-text-file-in-java
    public static List<String> splitSqlScript(String script, char delim) {
        List<String> statements = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();
        boolean inLiteral = false;
        char[] content = script.toCharArray();
        for (int i = 0; i < script.length(); i++) {
            if (content[i] == '"') {
                inLiteral = !inLiteral;
            }
            if (content[i] == delim && !inLiteral) {
                if (sb.length() > 0) {
                    statements.add(sb.toString().trim());
                    sb = new StringBuilder();
                }
            } else {
                sb.append(content[i]);
            }
        }
        if (sb.length() > 0) {
            statements.add(sb.toString().trim());
        }
        return statements;
    }
    public static void writeExtractedFileToDisk(InputStream in, OutputStream outs) throws IOException {
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer))>0){
            outs.write(buffer, 0, length);
        }
        outs.flush();
        outs.close();
        in.close();
    }
    public static ZipInputStream getFileFromZip(InputStream zipFileStream) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipFileStream);
        ZipEntry ze;
        while ((ze = zis.getNextEntry()) != null) {
            Log.w(TAG, "extracting file: '" + ze.getName() + "'...");
            return zis;
        }
        return null;
    }
    public static String convertStreamToString(InputStream is) {
        return new Scanner(is).useDelimiter("\\A").next();
    }

    public static class Encrypting {
        SecretKey secretKey;
        public byte[] enc(byte[] txt, SecretKey secretKey) {
            byte[] textEncrypted = new byte[0];
            try {
                KeyGenerator keygenerator = KeyGenerator.getInstance("DES");
                SecretKey myDesKey = keygenerator.generateKey();

                Cipher desCipher;
                desCipher = Cipher.getInstance("DES");


                byte[] text = "No body can see me.".getBytes("UTF8");


                desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);
                textEncrypted = desCipher.doFinal(text);
            } catch (Exception e) {
                Log.e("Exception", e.toString());
            }

            return textEncrypted;
        }

        public byte[] dec(byte[] textEncrypted) {
            byte[] textDecrypted = new byte[0];
            try {
                KeyGenerator keygenerator = KeyGenerator.getInstance("DES");
                SecretKey myDesKey = keygenerator.generateKey();

                Cipher desCipher;
                desCipher = Cipher.getInstance("DES");


                String s = new String(textEncrypted);
                System.out.println(s);

                desCipher.init(Cipher.DECRYPT_MODE, myDesKey);
                textDecrypted = desCipher.doFinal(textEncrypted);

                s = new String(textDecrypted);
            } catch (Exception e) {
                Log.e("Exception", e.toString());
            }

            return textDecrypted;
        }

        public  void main(String[] args) {

            try{
                KeyGenerator keygenerator = KeyGenerator.getInstance("DES");
                SecretKey myDesKey = keygenerator.generateKey();


                Cipher desCipher;
                desCipher = Cipher.getInstance("DES");


                byte[] text = "No body can see me.".getBytes("UTF8");


                desCipher.init(Cipher.ENCRYPT_MODE, myDesKey);
                byte[] textEncrypted = desCipher.doFinal(text);

                String s = new String(textEncrypted);
                System.out.println(s);

                desCipher.init(Cipher.DECRYPT_MODE, myDesKey);
                byte[] textDecrypted = desCipher.doFinal(textEncrypted);

                s = new String(textDecrypted);
                System.out.println(s);
            }catch(Exception e)
            {
                System.out.println("Exception");
            }
        }



        // https://stackoverflow.com/questions/4275311/how-to-encrypt-and-decrypt-file-in-android
        public static byte[] generateKey(String password) throws Exception
        {
            byte[] keyStart = password.getBytes("UTF-8");

            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(keyStart);
            kgen.init(128, sr);
            SecretKey skey = kgen.generateKey();
            return skey.getEncoded();
        }

        public static byte[] encodeFile(byte[] key, byte[] fileData) throws Exception
        {

            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(fileData);

            return encrypted;
        }

        public static byte[] decodeFile(byte[] key, byte[] fileData) throws Exception
        {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);

            byte[] decrypted = cipher.doFinal(fileData);

            return decrypted;
        }


    }

    public static void oBackupDatabase(Context context, String internalDatabase) {

//        if (true)
        //          return;

        // todo fix later
        String DB_NAME = "aHootBackup.db3";
        String DEST_PATH;

/*        if (!isExternalStorageWritable()) {
            Toast.makeText(getApplicationContext(),"Can't write to external storage", Toast.LENGTH_SHORT ).show();
            return;
        }
*/

        // setup output handle
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            DEST_PATH = Environment.getExternalStoragePublicDirectory("Documents").toString();
        else
            DEST_PATH = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();

        File directory = new File(DEST_PATH);
        if (!directory.mkdirs())
            Log.e("BackupDB", "Can't mkdirs()" + directory.getAbsolutePath());
        File destination = new File(DEST_PATH  + File.separator + DB_NAME);


        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(internalDatabase));  // 2nd line
//            inputStream =  context.openFileInput(internalDatabase);

            copyStreamTo(inputStream, destination);
        } catch (IOException e) {
            //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyDB", "Failed to close the destination");
            }
        }
    }
    public static void copyUriDatabaseToInternal(Context context, Uri input, String filename) {

        InputStream inputStream = null;
        File internalDatabase;
        String internalDBPath = LexData.internalFilesDir+ File.separator + filename;
        ; // + File.separator + Flavoring.getflavoring();
        Log.d("import", "CogyUriDatabaseToInternal" + internalDBPath);

        internalDatabase = new File(internalDBPath);

        try {
            inputStream = context.getContentResolver().openInputStream(input);
//            Log.e("CopyDB", inputStream.toString());
            copyStreamTo(inputStream, internalDatabase);
        } catch (IOException e) {
            //throw new ExternalSQLiteOpenHelperException("Failed to open database from assets/" + path);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e("CopyDB", "Failed to close the destination");
            }
        }
    }
    public static void makeListCards(Context context, String database) {
        cards = context.openOrCreateDatabase(database, Context.MODE_PRIVATE, null);
        String path = context.getFilesDir().getAbsolutePath() + database; // path to the root of internal memory.
        File f = new File(path);
        f.setWritable(true, false);



//        make private and set permission (see Google)
    }
    public static void createListDB(String database) {
        cards = SQLiteDatabase.openDatabase(database, null, 0);
        cards.beginTransaction();
        Log.e("Lists", "before tblList");

        String sql = "CREATE TABLE IF NOT EXISTS tblList( " +
                "ListCardID INTEGER PRIMARY KEY, " +
                "ListCardTitle VARCHAR(80) UNIQUE NOT NULL, " +
                "ListCardDescription VARCHAR(256), " +

                "quiz_type INTEGER, " +

                // ONLY SCORE IF RECALL TYPE
                "lastscore INTEGER, " +
                "mean INTEGER, " +
                "passing INTEGER, " +
                "attempts INTEGER, " +
                "last_attempt INTEGER, " +
                "difficulty INTEGER, " +
                "cardbox INTEGER, " +
                "next_scheduled INTEGER, " +

                "date_added INTEGER " +
                ");";

        cards.execSQL(sql);
        Log.e("Lists", "after tblList");


        sql = "CREATE TABLE IF NOT EXISTS tblListWords( " +
                "ListWordID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ListCardID INTEGER, " +
                "ListWord VARCHAR(22), " +

                "lastscore INTEGER, " +
                "mean INTEGER, " +
                "attempts INTEGER, " +
                "last_attempt INTEGER, " +
                "difficulty INTEGER, " +
                "cardbox INTEGER, " +
                "next_scheduled INTEGER, " +

                "CONSTRAINT CardWord UNIQUE(ListCardID, ListWord) ON CONFLICT IGNORE, " +
                "FOREIGN KEY(ListCardID) REFERENCES tblList(ListCardID) " +
                ");";
        cards.execSQL(sql);
        cards.endTransaction();
        cards.close();
        Log.e("Lists", "after tblListWords");
    }
}
