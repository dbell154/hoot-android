package com.tylerhosting.hoot.hoot;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.tylerhosting.hoot.hoot.CardUtils.dtDate;
import static com.tylerhosting.hoot.hoot.CardUtils.lastbox;
import static com.tylerhosting.hoot.hoot.CardUtils.nextDate;
import static com.tylerhosting.hoot.hoot.CardUtils.today;
import static com.tylerhosting.hoot.hoot.CardUtils.unixDate;
import static com.tylerhosting.hoot.hoot.Utils.sortString;

public class CardDatabase extends SQLiteOpenHelper {

    private static final String WordTable =
            "CREATE TABLE  IF NOT EXISTS questions (" +
                    "question       VARCHAR (16) UNIQUE ON CONFLICT IGNORE," +
                    "    correct        INTEGER      DEFAULT (0)," +
                    "    incorrect      INTEGER      DEFAULT (0)," +
                    "    streak         INTEGER      DEFAULT (0)," +
                    "    last_correct   INTEGER," +
                    "    difficulty     INTEGER      DEFAULT (0)," +
                    "    cardbox        INTEGER      DEFAULT (0)," +
                    "    next_scheduled INTEGER)";



    private static final String ListTable = "CREATE TABLE IF NOT EXISTS tblList ( " +
            "ListCardID          INTEGER       PRIMARY KEY," +
            "ListCardTitle       VARCHAR (80)  UNIQUE NOT NULL," +
            "ListCardDescription VARCHAR (256)," +
            "quiz_type           INTEGER," +
            "lastscore           INTEGER," +
            "mean                INTEGER," +
            "passing             INTEGER," +
            "attempts            INTEGER," +
            "last_attempt        INTEGER," +
            "difficulty          INTEGER," +
            "cardbox             INTEGER," +
            "next_scheduled      INTEGER," +
            "date_added          INTEGER" +
            ")";

    private static final String TableIndex = "CREATE UNIQUE INDEX IF NOT EXISTS question_index ON questions (question);";

    private static final String Metadata = "CREATE TABLE IF NOT EXISTS android_metadata ( " +
            "locale TEXT" +
            ")";

    private static final String MetadataInsert = "INSERT or IGNORE into android_metadata (locale) Values ('en_US')";


    private static final String ListWordsTable = "CREATE TABLE IF NOT EXISTS tblListWords( " +
            "ListWordID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "ListCardID INTEGER, " +

            " question       VARCHAR (16)," +
            " correct        INTEGER      DEFAULT (0)," +
            " incorrect      INTEGER      DEFAULT (0)," +
            " streak         INTEGER      DEFAULT (0)," +
            " last_correct   INTEGER," +

//            "ListWord VARCHAR(22), " +
//            "lastscore INTEGER, " +
//            "mean INTEGER, " +
//            "attempts INTEGER, " +
//            "last_attempt INTEGER, " +

            " difficulty INTEGER, " +
            " cardbox INTEGER, " +
            " next_scheduled INTEGER, " +

            "CONSTRAINT CardWord UNIQUE(ListCardID, question) ON CONFLICT IGNORE, " +
            "FOREIGN KEY(ListCardID) REFERENCES tblList(ListCardID) " +
            ");";

//    private static final String ListWordsIndex = "CREATE UNIQUE INDEX listquestion_index ON tblListWords (question, ListCardID);";


    public CardDatabase(@Nullable Context context, @Nullable String name,
                        @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, null, version);
    }


    // Method is called during creation of the database (open
    @Override
    public void onCreate(SQLiteDatabase database) {
//        File full = new File(database.getPath());
//        Log.e("mkdirs", "full= " + full.getAbsolutePath());
//        File directory = new File(full.getParent());
//        Log.e("mkdirs", "directory = " + directory.getAbsolutePath() );
//        String x = directory.toString();
//        Log.e("mkdirs", x);
//
//        boolean success = directory.mkdirs();
//        if (!success) {
//            Log.e("mkdirs", "failed to mkdirs");
////            return;
//        }


//        if (!directory.mkdirs())
//            return;

        if (getDatabaseName(). endsWith("Lists.db")) {
            database.execSQL(ListTable);
            database.execSQL(ListWordsTable);
//            database.execSQL(ListWordsIndex);
        }
        else {
            // Different CREATE commands for different types;
            database.execSQL(WordTable);
            database.execSQL(TableIndex);
            database.execSQL(Metadata);
            database.execSQL(MetadataInsert);
        }
        return;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static boolean addCardFilter(SQLiteDatabase database, Cursor filterCursor, boolean alphagram) {

    String[] words;
    String[] list;
    int counter = 0;
    int column;

    // CREATE LIST FROM CURSOR
    words = new String[filterCursor.getCount()];
//        column = filterCursor.getColumnIndex("question");
//        if (column == -1)
    column = filterCursor.getColumnIndex("Word");


// NEED TO HANDLE ERROR IF NO ENTRIES
    if (filterCursor.getCount() == 0)
        return false;

    filterCursor.moveToFirst();
    words[counter] = filterCursor.getString(column).toUpperCase();
    while (filterCursor.moveToNext()) {
        counter++;
        words[counter] = filterCursor.getString(column).toUpperCase();
    }

    // this is for anagrams ONLY
    if (alphagram) {
        list = getAlphagrams(words);
    }
    else {
        list = words;
    }

    // ADD TO cardFilter TABLE
    database.execSQL("DROP TABLE IF EXISTS `cardFilter`");

    String sql = "CREATE TABLE `cardFilter` ( " +
            "WordID INTEGER PRIMARY KEY," +
            "card VARCHAR(22) );";
    database.execSQL(sql);

    database.beginTransaction();
    for (int k = 0; k < list.length; k++) {
        if (list[k].length() > 15)
            continue;
        database.execSQL("INSERT OR IGNORE INTO cardFilter (card) " +
                "VALUES ('" + list[k] + "');");
    }

    // If successful commit those changes
    database.setTransactionSuccessful();
    database.endTransaction();
    return true;
}
    public static String[] getAlphagrams(String[] words) {

        List<String> alphawords = new ArrayList<>();
        for (int id = 0; id < words.length; id++) {
            String sorted  = sortString(words[id]);
            alphawords.add(sorted);
        }
        Set<String> alphagrams = new LinkedHashSet<>(alphawords);

        /*        Set<String> alphagrams = new LinkedHashSet<>();
        for (int id = 0; id < words.length; id++) {
            String sorted  = sortString(words[id]);
            alphagrams.add(sorted);
        }
*/
        String[] alpha = alphagrams.toArray(new String[0]);
//        Arrays.sort(alpha);

//        Arrays.sort(alpha);
        return alpha;

    }
    //public void addAnagrams
    public static int addCards(SQLiteDatabase database, String[] list) {
        String sql;

        int boxNumber = 0;
        long next = nextDate(boxNumber);
        Cursor wordcount;
        int beginCount = 0; int endCount = 0;

        sql = "select correct from questions";
        wordcount = database.rawQuery(sql, null);
        beginCount = wordcount.getCount();
        wordcount.close();

        database.beginTransaction();
//        ContentValues cv = new ContentValues(); //Declare once
        for (int k = 0; k < list.length; k++) {
//            database.execSQL("INSERT OR IGNORE INTO " +
//                    "questions (question) VALUES ('" + list[k] + "')");

            if (list[k].length() > 15)
                continue;
//            Log.e("add", list[k]);

//            String alphagram = Utils.sortString(list[k]);

            database.execSQL("INSERT OR IGNORE INTO questions (question, " +
                            "correct, incorrect, streak, last_correct, difficulty," +
                            "cardbox, next_scheduled) " +
//                            "VALUES ('" + alphagram + "', " +
                            "VALUES ('" + list[k] + "', " +
                            "0, 0, 0, 0, 0," +
                            boxNumber + ", " + next + ");"
            );

        }
        // If successful commit those changes
        database.setTransactionSuccessful();
        database.endTransaction();
//        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));

        sql = "select correct from questions";
        wordcount = database.rawQuery(sql, null);
        endCount = wordcount.getCount();
        wordcount.close();

        int cardsAdded = endCount - beginCount; // returns 0
        return cardsAdded;
    }
    public static int addWords(SQLiteDatabase database, String[] list) {
        String sql;
        int boxNumber = 0;
        long next = nextDate(boxNumber);
        Cursor wordcount;
        int beginCount = 0; int endCount = 0;

        //        int counter = 0;

        sql = "select correct from questions";
        wordcount = database.rawQuery(sql, null);
        beginCount = wordcount.getCount();
        wordcount.close();

        database.beginTransaction();
//        ContentValues cv = new ContentValues(); //Declare once
        for (int k = 0; k < list.length; k++) {

            if (list[k].length() > 15)
                continue;

            database.execSQL("INSERT OR IGNORE INTO questions (question, " +
                    "correct, incorrect, streak, last_correct, difficulty," +
                    "cardbox, next_scheduled) " +
                    "VALUES ('" + list[k] + "', " +
                    "0, 0, 0, 0, 0," +
                    boxNumber + ", " + next + ");"
            );

//            Log.e("Word", list[k]);
        }
        // If successful commit those changes
        database.setTransactionSuccessful();
        database.endTransaction();

        sql = "select correct from questions";
        wordcount = database.rawQuery(sql, null);
        endCount = wordcount.getCount();
        wordcount.close();

        int cardsAdded = endCount - beginCount; // returns 0
        return cardsAdded;
    }
    public static LexData.ZWord getCard(SQLiteDatabase database, String word) {
        LexData.ZWord wordInfo = new LexData.ZWord();

        String sql = "SELECT * from questions WHERE question = '" + word + "';";

        Cursor cursor = database.rawQuery(sql, null);
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }

        cursor.moveToFirst();
//        while (cursor.moveToNext())
//        {
        wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
        wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
        wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
        wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
        wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
        wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
        wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
        wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
//        }
//        if (cursor.getCount() == 0) {
//            cursor.close();
//            return null;
//        }
//        else
        cursor.close();

        return wordInfo;
    }
    public static LexData.ZWord oldgetCard(SQLiteDatabase database, String word) {
        LexData.ZWord wordInfo = new LexData.ZWord();

        String sql = "SELECT * from questions WHERE question = '" + word + "';";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
        }
        if (cursor.getCount() == 0) {
            cursor.close();
            return null;
        }
        else
            cursor.close();
        return wordInfo;
    }
    public Cursor getScheduledCards(SQLiteDatabase database, String order, String count, String filter) {

        String orderby;
        switch (order) {
            case "box":
                orderby = " cardbox, next_scheduled ";
                break;
            case "boxrandom":
                orderby = " cardbox, next_scheduled/36000, RANDOM() ";
                break;
            case "schrandom":
                orderby = " next_scheduled/36000, cardbox, RANDOM() ";
                break;
            case "schedule":
            default:
                orderby = " next_scheduled, cardbox ";
                break;
        }
        String wordcount = "";
        if (filter == null)
            filter = "";

        if (!(count == null)) {
            Integer countValue = Integer.parseInt(count);
            if (countValue > 0)
                wordcount = " LIMIT " + countValue.toString();
        }

        Cursor cursor = database.rawQuery("SELECT question \n" +
                "FROM questions " +
                filter +
                " ORDER BY " + orderby +
                wordcount, null);

        return cursor;
    }
    public static LexData.HListWord getListWord(SQLiteDatabase database, int list, String word) {
        LexData.HListWord wordInfo = new LexData.HListWord();

        String sql = "SELECT * from tblListWords WHERE question = '" + word + "' AND ListCardID = " + list + ";";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            wordInfo.question = word;
//            wordInfo.question = myReader["question"].ToString();

//            wordInfo.lastscore = cursor.getInt(cursor.getColumnIndex("lastscore"));
//            wordInfo.mean = cursor.getInt(cursor.getColumnIndex("mean"));
//            wordInfo.attempts = cursor.getInt(cursor.getColumnIndex("attempts"));
//            wordInfo.last_attempt = cursor.getInt(cursor.getColumnIndex("last_attempt"));

            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));

            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            wordInfo.listID = list;
        }
        cursor.close();
        return wordInfo;
    }
    public Cursor getScheduledListCards(SQLiteDatabase database, String listName, String order, String count) {
        String sql =       "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listName + "';";
        Cursor precursor= database.rawQuery(sql, null);
        precursor.moveToFirst();
        String listID = precursor.getString(precursor.getColumnIndex("ListCardID"));


        String orderby;
        switch (order) {
            case "box":
                orderby = " cardbox, next_scheduled, ListWordID ";
                break;
            case "boxrandom":
                orderby = " cardbox, next_scheduled/36000, RANDOM() ";
                break;
            case "schrandom":
                orderby = " next_scheduled/36000, cardbox, RANDOM() "; //
                break;
            case "schedule":
            default:
                orderby = " next_scheduled, cardbox, ListWordID ";
                break;
        }
        String wordcount = "";

        if (!(count == null)) {
            Integer countValue = Integer.parseInt(count);
            if (countValue > 0)
                wordcount = " LIMIT " + countValue.toString();
        }


        Cursor cursor = database.rawQuery("SELECT question \n" +
                "FROM tblListWords WHERE ListCardID = " + listID + " ORDER BY " +
                orderby +
                wordcount, null);
        return cursor;
    }
    public static LexData.ZWord moveCard(SQLiteDatabase database, String word, int direction) {
        int uday = unixDate(today());

//        DateTime thePresent = DateTime.Now;
//        Int32 today = unixDate(thePresent);

        LexData.ZWord selection = getCard(database, word);
        String sql;
        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
        if (direction == (int) CardUtils.boxmove.forward.ordinal())
        {
            selection.cardbox += 1;
            selection.next_scheduled = nextDate((int)selection.cardbox);
            selection.last_correct = uday;
            sql = "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                    "correct = " + ++selection.correct + ", " +
                    "streak = " + ++selection.streak + ", " +
                    "last_correct = " + selection.last_correct + ", " +
                    "next_scheduled = " + selection.next_scheduled + " " +
                    "WHERE question = '" + word + "';";
        }
        else
        {
            if (direction == (int) CardUtils.boxmove.back.ordinal())
                selection.cardbox = lastbox((int)selection.cardbox);
            else // boxmove.zero
                selection.cardbox = 0;
            selection.next_scheduled = nextDate((int)selection.cardbox);
            sql = "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                    "incorrect = " + ++selection.incorrect + ", " +
                    "streak = " + 0 + ", " +
                    "next_scheduled = " + selection.next_scheduled + " " +
                    "WHERE question = '" + word + "';";
        }

        database.execSQL(sql);
        return selection;
    }
    // only need to move one at a time
    public static LexData.HListWord moveListWord(SQLiteDatabase database, int listID, String word, int direction)    {
        String sql;
        // mean is set by calling method
        // this sets box, schedule and saves
        LexData.HListWord listword = getListWord(database, listID, word);
        String specs = listID + " " + word;
        //       Log.e("CDSpecs", specs );
        int uday = unixDate(today());

        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak

        if (direction == (int) CardUtils.boxmove.forward.ordinal())
        {
            listword.cardbox += 1;
            listword.incorrect++;
            listword.last_correct = uday;
        }
        else
        {
            if (direction == (int) CardUtils.boxmove.back.ordinal())
                listword.cardbox = lastbox((int)listword.cardbox);
            else // boxmove.zero
                listword.cardbox = 0;
            listword.correct++;
        }
        listword.next_scheduled = nextDate((int)listword.cardbox);
        sql =
                "UPDATE tblListWords SET cardbox = " + listword.cardbox + ", " +

//                        "mean = " + listword.mean + ", " +
//                        "attempts = " + ++listword.attempts + ", " +
//                        "last_attempt = " + uday + ", " +
//                        "lastscore = " + listword.lastscore + ", " +
                        "correct = " + ++listword.correct + ", " +
                        "streak = " + ++listword.streak + ", " +
                        "last_correct = " + listword.last_correct + ", " +


                        "next_scheduled = " + listword.next_scheduled + " " +
                        "WHERE question = '" + listword.question + "' " +
                        "AND ListCardID = " + listword.listID + ";";

//        UPDATE tblListWords SET cardbox = 0, mean = 0, attempts = 1, last_attempt = 1597381274, lastscore = 0, next_scheduled = 1597467674 WHERE ListWord = '' AND ListCardID = 0;

        //       Log.e("sql", sql);
        database.execSQL(sql);
        return listword;
    }


    // Hoot Lists
    public static int addList(SQLiteDatabase database, LexData.HList hlist, List<String> wordList) {
//        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
//        if (!File.Exists(dbSource(cb)))
//            CardUtils.createDatabase(cb);
        String sql;
        int added = unixDate(today());

        sql = "INSERT or IGNORE INTO tblList (ListCardTitle, ListCardDescription, " +
                        "quiz_type, " +
                        "lastscore, mean, passing, attempts, last_attempt, difficulty, " +
                        "cardbox, next_scheduled, " +
                        "date_added) " +
                        "VALUES ('" + hlist.title + "', '" + hlist.description + "', " +
                        hlist.quiz_type + ", " +
                        "0,0," + hlist.passing +
                        ",0,0,0," +
                        hlist.cardbox + ", " + nextDate((int)hlist.cardbox) + ", " + added + ");";            // add grading, box, etc

        database.execSQL(sql);

        int counter = 0;
        if (hlist.quiz_type == Utils.getSearchType("Anagrams"))
            counter = CardDatabase.addListCards(database, wordList, hlist.title);
        else
            counter = CardDatabase.addListWords(database, wordList, hlist.title);
        return counter;
    }
    public static int addList(SQLiteDatabase database, List<String> wordList, String listTitle, String quiztype, int passing)    {
        int subsearch = Utils.getSearchType(quiztype);
        // force all new lists to box 0
        int boxNumber = 0;

        String sql;
        int added = unixDate(today());

              sql =  "INSERT INTO tblList (ListCardTitle, ListCardDescription, " +
                        "quiz_type, " +
                        "lastscore, mean, passing, attempts, last_attempt, difficulty, " +
                        "cardbox, next_scheduled, " +
                        "date_added) " +
                        "VALUES ('" + listTitle + "', 'Description', " + subsearch + "," +
                        "0,0," + passing + ",0,0,0," +
                        boxNumber + ", " + nextDate((int)boxNumber) + ", " + added + ");";            // add grading, box, etc

        database.execSQL(sql);

        int counter = 0;
        if (quiztype == "Anagrams")
            counter = CardDatabase.addListCards(database, wordList, listTitle);
        else
            counter = CardDatabase.addListWords(database, wordList, listTitle);
        return counter;
    }
    public static boolean saveList(SQLiteDatabase database, LexData.HList hlist)    {
        String sql;
        sql =
                "UPDATE tblList " +
                        "SET ListCardTitle = '" + hlist.title + "', " +
                        "ListCardDescription = '" + hlist.description + "', " +
                        "quiz_type = " + hlist.quiz_type + ", " +
                        "lastscore = " + hlist.lastscore + ", " +
                        "mean = " + hlist.mean + ", " +
                        "passing = " + hlist.passing + ", " +
                        "attempts = " + hlist.attempts + ", " +
                        "last_attempt = " + hlist.last_attempt + ", " +
                        "difficulty = " + hlist.difficulty + ", " +
                        "cardbox = " + hlist.cardbox + ", " +
                        "next_scheduled = " + hlist.next_scheduled + ", " +
                        "date_added = " + hlist.date_added + " " +
                        "WHERE ListCardID = " + hlist.id + ";";

        // need to check for duplicate (catch)
        database.execSQL(sql);
        // get list id
        return true;
    }
    public static int addListWords(SQLiteDatabase database, List<String> wordList, String listTitle)    {
        String sql;
        Cursor wordcount;
        int beginCount = 0; int endCount = 0;

        sql =       "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
        Cursor cursor= database.rawQuery(sql, null);
        cursor.moveToFirst();
        int listID = cursor.getInt(cursor.getColumnIndex("ListCardID"));

        sql = "select correct from tblListWords";
        wordcount = database.rawQuery(sql, null);
        beginCount = wordcount.getCount();
        wordcount.close();

        database.beginTransaction();
        int counter = 0;
        for (int i = 0; i < wordList.size(); i++) {
            if (wordList.get(i).length() > 15)
                continue;
            sql =
                    "INSERT OR IGNORE INTO tblListWords (ListCardID, question, " +
                            "correct, incorrect, streak, last_correct, difficulty, cardbox, next_scheduled) " +
                            "VALUES ( " + listID + ", '" + wordList.get(i) + "', " +
                            "0,0,0,0,0,0," +
                            nextDate(0) + "); ";
            database.execSQL(sql);
            counter++;
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        sql = "select correct from tblListWords";
        wordcount = database.rawQuery(sql, null);
        endCount = wordcount.getCount();
        wordcount.close();

        int cardsAdded = endCount - beginCount; // returns 0
        return cardsAdded;

//        return counter;
    }
    // adds alphagrams
    public static int addListCards(SQLiteDatabase database, List<String> wordList, String listTitle)     {
        String sql;
        Cursor wordcount;
//        int counter = 0;
        int beginCount = 0; int endCount = 0;
        sql = "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
        Cursor cursor = database.rawQuery(sql, null);

        // gets listID
//        Log.e("cur", String.valueOf(cursor.getCount()));
        cursor.moveToFirst();
        int listID = cursor.getInt(cursor.getColumnIndex("ListCardID"));
        cursor.close();

        sql = "select correct from tblListWords";
        wordcount = database.rawQuery(sql, null);
        beginCount = wordcount.getCount();
        wordcount.close();


// todo  fix other adds to show card count like this

        // inserts cards
        database.beginTransaction();
        for (int i = 0; i < wordList.size(); i++) {
            if (wordList.get(i).length() > 15)
                continue;
 //           Log.e("CDaddListCards", wordList.get(i));

            // THIS IS THE DIFFERENCE BETWEEN LISTWORDS
            String alphagram = sortString(wordList.get(i));

            sql =
                    "INSERT OR IGNORE INTO tblListWords (ListCardID, question, " +
                            "correct, incorrect, streak, last_correct, difficulty, cardbox, next_scheduled) " +
                            "VALUES ( " + listID + ", '" + alphagram + "', " +
                            "0,0,0,0,0,0," +
                            nextDate(0) + "); ";
            database.execSQL(sql);
//            counter++;

        }
        database.setTransactionSuccessful();
        database.endTransaction();

// todo   like this

        sql = "select correct from tblListWords";
        wordcount = database.rawQuery(sql, null);
        endCount = wordcount.getCount();
        wordcount.close();

        int cardsAdded = endCount - beginCount; // returns 0
        return cardsAdded;
    }
    public static LexData.HList getList(SQLiteDatabase database, String listTitle)     {
        LexData.HList hlist = new LexData.HList();
        String sql =
            "SELECT * FROM tblList WHERE ListCardTitle = '" + listTitle + "'";

        Cursor cursor = database.rawQuery(sql, null);
        cursor.moveToFirst();

            hlist.title = cursor.getString(cursor.getColumnIndex( "ListCardTitle"));
            hlist.description = cursor.getString(cursor.getColumnIndex( "ListCardDescription"));
            hlist.quiz_type = cursor.getInt(cursor.getColumnIndex("quiz_type"));
            hlist.id = cursor.getInt(cursor.getColumnIndex("ListCardID"));
            hlist.lastscore = cursor.getInt(cursor.getColumnIndex("lastscore"));
            hlist.mean = cursor.getInt(cursor.getColumnIndex("mean"));
            hlist.passing = cursor.getInt(cursor.getColumnIndex("passing"));
            hlist.attempts = cursor.getInt(cursor.getColumnIndex("attempts"));
            hlist.last_attempt = cursor.getInt(cursor.getColumnIndex("last_attempt"));
            hlist.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            hlist.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            hlist.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            hlist.date_added = cursor.getInt(cursor.getColumnIndex("date_added"));

        return hlist;
    }
    public static List<LexData.HListWord> getListWords(SQLiteDatabase database, String title)    {

        List<LexData.HListWord> cards = null;

        String sql =
                "SELECT ListWord, tblListWords.ListWordID, tblListWords.ListCardID, tblListWords.correct, tblListWords.incorrect, tblListWords.streak, " +
                        "tblListWords.last_correct, tblListWords.difficulty, tblListWords.cardbox, tblListWords.next_scheduled " +
                        "FROM tblListWords, tblList WHERE (tblListWords.ListCardID = tblList.ListCardID) " +
                        "AND (tblList.ListCardTitle = '" + title + "') " +
                        "ORDER BY tblListWords.ListWordID;";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.HListWord card = new LexData.HListWord();
            card.listWordID = cursor.getInt(cursor.getColumnIndex("ListWordID"));
            card.listID = cursor.getInt(cursor.getColumnIndex("ListCardID"));
            card.question = cursor.getString(cursor.getColumnIndex("question"));
            card.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            card.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            card.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            card.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            card.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            card.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            card.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            cards.add(card);
        }
        cursor.close();
        return cards;
    }


    // TEST Methods
    public Cursor theseWords(SQLiteDatabase database, String[] selections) {
        String list = "'" + selections[0] + "'";
        for (int j = 1; j < selections.length; j++)
            list = list + ", '" + selections[j] + "'";

        Cursor cursor = database.rawQuery("SELECT question \n" +
                "FROM questions WHERE question in ( " + list + ") ORDER BY cardbox", null);
        return cursor;
    }
    public static int getMinLastAttempt(SQLiteDatabase database, int list) {
        String cmd = "SELECT Min(last_attempt) from tblListWords WHERE ListCardID = '" + list + "' ";
        Cursor cursor= database.rawQuery(cmd, null);
        int min = cursor.getInt(1);
        return min;
    }
    public static void startListQuery(SQLiteDatabase database, int list) {
        long uday = unixDate(today());
        String cmd = "UPDATE tblList SET last_attempt = " + uday + " WHERE ListCardID = " + list;
    }
    public static void addWordFilter(SQLiteDatabase database, Cursor filterCursor) {

        String[] words;
        int counter = 0;
        int column;

        // CREATE LIST FROM CURSOR
        words = new String[filterCursor.getCount()];
        column = filterCursor.getColumnIndex("Word");

        filterCursor.moveToFirst();
        words[counter] = filterCursor.getString(column).toUpperCase();
        while (filterCursor.moveToNext()) {
            counter++;
            words[counter] = filterCursor.getString(column).toUpperCase();
        }

        // ADD TO cardFilter TABLE
        database.execSQL("DROP TABLE IF EXISTS `cardFilter`");

        String sql = "CREATE TABLE `cardFilter` ( " +
                "WordID INTEGER PRIMARY KEY," +
                "card VARCHAR(22) );";
        database.execSQL(sql);

        database.beginTransaction();
        for (int k = 0; k < words.length; k++) {
            if (words[k].length() > 15)
                continue;
            database.execSQL("INSERT OR IGNORE INTO cardFilter (card) " +
                    "VALUES ('" + words[k] + "');");
        }

        // If successful commit those changes
        database.setTransactionSuccessful();
        database.endTransaction();
        return;
    }
    // gets standard cardbox
    public static CardDatabase getCardDB(Context context) {
        CardDatabase cardDatabase = new CardDatabase(context, LexData.getCardfile(), null, 1);
        return cardDatabase;
    }
    public static CardDatabase getListDB(Context context) {
        CardDatabase cardDatabase;
        cardDatabase = new CardDatabase(context, LexData.getCardfile(), null, 1);
        return cardDatabase;
    }


    // UNUSED METHODS
    // card quiz uses ZWord instead of string
    public static List<LexData.ZWord> getCards(SQLiteDatabase database) { // alphagrams

        List<LexData.ZWord> listWords = null;

        String sql = "SELECT * from questions;";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.ZWord wordInfo = new LexData.ZWord();

            wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            listWords.add(wordInfo);
        }
        cursor.close();
        return listWords;
    }
    public static List<LexData.ZWord> cursorToList(Cursor cursor) {
        List<LexData.ZWord> listWords = null;
        while (cursor.moveToNext())
        {
            LexData.ZWord wordInfo = new LexData.ZWord();

            wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            listWords.add(wordInfo);
        }
        cursor.close();
        return listWords;
    }
    public static List<LexData.ZWord> getCardBox(SQLiteDatabase database, int boxnumber) {
        List<LexData.ZWord> listWords = null;

        String sql = "SELECT * from questions WHERE cardbox = " + boxnumber + ";";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.ZWord wordInfo = new LexData.ZWord();

            wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            listWords.add(wordInfo);
        }
        cursor.close();
        return listWords;
    }
    public static List<LexData.ZWord> getCardsDue(SQLiteDatabase database, Date due) {
        List<LexData.ZWord> listWords = null;

        String sql = "SELECT question, cardbox, next_scheduled from questions WHERE next_scheduled <= " + unixDate(due) + " " +
                " ORDER BY next_scheduled;";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.ZWord wordInfo = new LexData.ZWord();

            wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            listWords.add(wordInfo);
        }
        cursor.close();
        return listWords;
    }
    public static List<LexData.ZWord> getCardSchedule(SQLiteDatabase database) {
        List<LexData.ZWord> listWords = null;

        String sql = "SELECT question, cardbox, next_scheduled from questions ORDER BY next_scheduled;";
        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.ZWord wordInfo = new LexData.ZWord();

            wordInfo.question = cursor.getString(cursor.getColumnIndex("question"));
//            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            wordInfo.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            wordInfo.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            wordInfo.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            wordInfo.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            wordInfo.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            wordInfo.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            listWords.add(wordInfo);
        }
        cursor.close();
        return listWords;
    }

    public static boolean moveCards(SQLiteDatabase database, List<String> words, int direction) {
        int uday = unixDate(today());

        String sql = null;
        database.beginTransaction();
        for (int i = 0; i < words.size(); i++)
        {
            LexData.ZWord selection = getCard(database, words.get(i));
            // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
            if (direction == (int) CardUtils.boxmove.forward.ordinal())
            {
                selection.cardbox += 1;
                selection.next_scheduled = nextDate((int)selection.cardbox);
                selection.last_correct = uday;
                sql =
                        "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                                "correct = " + ++selection.correct + ", " +
                                "streak = " + ++selection.streak + ", " +
                                "last_correct = " + selection.last_correct + ", " +
                                "next_scheduled = " + selection.next_scheduled + " " +
                                "WHERE question = '" + selection.question + "';";
            }
            else
            {
                if (direction == (int) CardUtils.boxmove.back.ordinal())
                    selection.cardbox = lastbox((int)selection.cardbox);
                else // boxmove.zero
                    selection.cardbox = 0;
                selection.next_scheduled = nextDate((int)selection.cardbox);
                sql =
                        "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                                "incorrect = " + ++selection.incorrect + ", " +
                                "streak = " + 0 + ", " +
                                "next_scheduled = " + selection.next_scheduled + " " +
                                "WHERE question = '" + selection.question + "';"; // word
            }

            database.execSQL(sql);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return true;
    }
    public static boolean moveCardsToBox(SQLiteDatabase database, List<String> words, int box)    {
        int uday = unixDate(today());

        String sql = null;
        database.beginTransaction();
        for (int i = 0; i < words.size(); i++)
        {
            LexData.ZWord selection = getCard(database, words.get(i));
            selection.cardbox = box;
            selection.next_scheduled = nextDate((int)selection.cardbox);
            sql =
                    "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                            "next_scheduled = " + selection.next_scheduled + " " +
                            "WHERE question = '" + selection.question + "';";
            database.execSQL(sql);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return true;
    }
    public static LexData.ZWord rescheduleCard(SQLiteDatabase database, String word, Date newday) {

        int uday = unixDate(newday);

        LexData.ZWord selection = getCard(database, word);
        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
        selection.next_scheduled = CardUtils.unixDate(newday);
        String sql =
                "UPDATE questions SET next_scheduled = " + selection.next_scheduled + " " +
                        "WHERE question = '" + word + "';";

        database.execSQL(sql);
        return selection;
    }
    public static boolean rescheduleCards(SQLiteDatabase database, List<String> words, Date newday) {
        int uday = unixDate(newday);

        String sql = null;
        database.beginTransaction();
        for (int i = 0; i < words.size(); i++) {
            LexData.ZWord selection = getCard(database, words.get(i));
            // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
            selection.next_scheduled = CardUtils.unixDate(newday);
            sql = "UPDATE questions SET next_scheduled = " + selection.next_scheduled + " " +
                    "WHERE question = '" + selection.question + "';";
            database.execSQL(sql);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return true;
    }
    public static boolean dropCards(SQLiteDatabase database, List<String> cards)    {

        String sql;
        database.beginTransaction();
        for (int i = 0; i < cards.size(); i++)
        {
            sql = "DELETE FROM questions WHERE question = '" + cards.get(i) + "'";
            database.execSQL(sql);
        }
        database.setTransactionSuccessful();
        database.endTransaction();
        return true;
    }
    public static List<String> getListTitles(SQLiteDatabase database)     {
        List<String> titles = null;
        String sql =
                "SELECT ListCardTitle from tblList";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            titles.add(title);
        }
        cursor.close();
        return titles;
    }
    public static List<LexData.HList> getLists(SQLiteDatabase database)     {
        List<String> listTitles = null;
        String sql =
                "SELECT * from tblList";

        Cursor cursor = database.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndex("ListCardTitle"));
            listTitles.add(title);
        }
        cursor.close();



        List<LexData.HList> hlists = null;

        for (int i = 0; i < listTitles.size(); i++)
        {
            LexData.HList hlist = getList(database, listTitles.get(i));
            hlists.add(hlist);
        }
        return hlists;

    }
    public static List<LexData.HListWord> getListBox(SQLiteDatabase database, int boxnumber, String title)    {
        List<LexData.HListWord> lstReader = null;

        String sql =
                "SELECT ListWord, tblListWords.ListWordID, tblListWords.ListCardID, " +
                        "tblListWords.lastscore, tblListWords.mean, tblListWords.attempts, " +
                        "tblListWords.last_attempt, tblListWords.difficulty, tblListWords.cardbox, tblListWords.next_scheduled " +
                        "FROM tblListWords, tblList WHERE (tblListWords.ListCardID = tblList.ListCardID) " +
                        "AND (tblListWords.cardbox = " + boxnumber + ") " +
                        "AND (tblList.ListCardTitle = '" + title + "') " +
                        "ORDER BY tblListWords.ListWordID;";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.HListWord card = new LexData.HListWord();
            card.listWordID = cursor.getInt(cursor.getColumnIndex("ListWordID"));
            card.listID = cursor.getInt(cursor.getColumnIndex("ListCardID"));
            card.question = cursor.getString(cursor.getColumnIndex("question"));
            card.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            card.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            card.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            card.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            card.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            card.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            card.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            lstReader.add(card);
        }
        cursor.close();
        return lstReader;
    }
    public static List<LexData.HListWord> getListWordsDue(SQLiteDatabase database, String title, Date due)    {
        int uday = unixDate(due);

        List<LexData.HListWord> words = null;

        String sql =
                "SELECT ListWord, tblListWords.ListWordID, tblListWords.ListCardID, tblListWords.correct, tblListWords.incorrect, tblListWords.streak, " +
                        "tblListWords.last_correct, tblListWords.difficulty, tblListWords.cardbox, tblListWords.next_scheduled " +
                        "FROM tblListWords, tblList WHERE (tblListWords.ListCardID = tblList.ListCardID) " +
                        "AND tblList.ListCardTitle = '" + title + "' " +
                        "AND tblListWords.next_scheduled <= " + unixDate(due) + " " +
                        "ORDER BY tblListWords.next_scheduled;";

        Cursor cursor = database.rawQuery(sql, null);

        while (cursor.moveToNext())
        {
            LexData.HListWord card = new LexData.HListWord();
            card.listWordID = cursor.getInt(cursor.getColumnIndex("ListWordID"));
            card.listID = cursor.getInt(cursor.getColumnIndex("ListCardID"));
            card.question = cursor.getString(cursor.getColumnIndex("question"));
            card.correct = cursor.getInt(cursor.getColumnIndex("correct"));
            card.incorrect = cursor.getInt(cursor.getColumnIndex("incorrect"));
            card.streak = cursor.getInt(cursor.getColumnIndex("streak"));
            card.last_correct = cursor.getInt(cursor.getColumnIndex("last_correct"));
            card.difficulty = cursor.getInt(cursor.getColumnIndex("difficulty"));
            card.cardbox = cursor.getInt(cursor.getColumnIndex("cardbox"));
            card.next_scheduled = cursor.getInt(cursor.getColumnIndex("next_scheduled"));
            words.add(card);
        }
        cursor.close();
        return words;
    }
    //        public static HListWord getListWord(Cardbox cb, String title, String )
    public static boolean moveList(SQLiteDatabase database, String title, int direction)    {
        // attempts, lastscore, mean, last_attempt set by gradeList
        LexData.HList hlist = getList(database, title);

        String sql;

        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
        if (direction == (int) CardUtils.boxmove.forward.ordinal())
        {
            hlist.cardbox += 1;
            hlist.next_scheduled = nextDate((int)hlist.cardbox);
            sql =
                    "UPDATE tblList SET cardbox = " + hlist.cardbox + ", " +
                            "next_scheduled = " + hlist.next_scheduled + " " +
                            "WHERE ListCardTitle = '" + title + "';";
        }
        else
        {
            if (direction == (int) CardUtils.boxmove.back.ordinal())
                hlist.cardbox = lastbox((int)hlist.cardbox);
            else // boxmove.zero
                hlist.cardbox = 0;
            hlist.next_scheduled = nextDate((int)hlist.cardbox);
            sql =
                    "UPDATE tblList SET cardbox = " + hlist.cardbox + ", " +
                            "next_scheduled = " + hlist.next_scheduled + " " +
                            "WHERE ListCardTitle = '" + title + "';";
        }
        database.execSQL(sql);

        Date due = dtDate(hlist.next_scheduled);
//            DateTime due = new DateTime(1970, 1, 1, 0, 0, 0, 0, System.DateTimeKind.Utc);
//            due = due.AddSeconds(hlist.next_scheduled).ToLocalTime();

        long ms = due.getTime() - today().getTime();
        long days = ms / (1000*60*60*24);
//        MessageBox.Show(String.Format("Moved {0} to box {1}: Due: {2} ({3} days)",
//                title, hlist.cardbox, due.ToString("d"), days));
        // get boxtype from db
        return true;
    }
    public static boolean rescheduleList(SQLiteDatabase database, String title, Date newday)    {
        int uday = unixDate(today());
        String sql;

        // attempts, lastscore, mean, last_attempt set by gradeList

        LexData.HList hlist = getList(database, title);

        hlist.next_scheduled = CardUtils.unixDate(newday);

        sql =
                "UPDATE tblList SET next_scheduled = " + hlist.next_scheduled + " " +
                        "WHERE ListCardTitle = '" + title + "';";
        database.execSQL(sql);

//        MessageBox.Show(String.Format("Rescheduled {0} for {1})",
//                title, dtDate(hlist.next_scheduled)));
        return true;
    }
    public static boolean dropList(SQLiteDatabase database, String listTitle)    {
        String sql;
        database.beginTransaction();

        sql =
                "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";

        Cursor cursor= database.rawQuery(sql, null);
        int id = cursor.getInt(cursor.getColumnIndex("ListCardID"));

        // for listid, delete words
        sql =
                "DELETE FROM tblListWords WHERE ListCardID = " + id;
        database.execSQL(sql);

        // delete list
        sql =
                "DELETE FROM tblList WHERE ListCardID = " + id;
        database.execSQL(sql);

        database.setTransactionSuccessful();
        database.endTransaction();
        return true;
    }
    public static boolean dropListCards(SQLiteDatabase database, String listTitle, List<String> cards)    {
        String sql;
        Cursor cursor;

        database.beginTransaction();

        for (int i = 0; i < cards.size(); i++) {
            sql =  "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
            cursor = database.rawQuery(sql, null);
            int id = cursor.getInt(cursor.getColumnIndex("ListCardID"));

            sql =  "DELETE FROM tblListWords WHERE ListWord = '" + cards.get(i) + "'" +
                            " AND ListCardID = " + id + ";";
            database.execSQL(sql);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

//            MessageBox.Show(String.Format("Deleted {0} from list", card));
        return true;
    }
    public static LexData.HList scoreList(SQLiteDatabase database, String listTitle)    {

        String sql;

            LexData.HList current = getList(database, listTitle);
            float cardbox = 0;

            int uday = unixDate(today());
            sql = "SELECT AVG(lastscore) FROM tblListWords WHERE ListCardID = " + current.id;

            Cursor cursor= database.rawQuery(sql, null);
            int mean = cursor.getInt(0);

            // sets cardbox as a rank between passing score and 100
            cardbox = (mean - current.passing) / ((100 - current.passing) / 10);

            // alternately, move to next box if passing score
            // if (mean >= current.passing) cardbox++;

            int next_scheduled = nextDate((int)cardbox);

            sql =
                    "UPDATE tblList SET mean = " + mean + ", " +
                            "lastscore = " + current.mean + ", " +
                            "cardbox = " + cardbox + ", "+
                            "attempts = " + (current.attempts + 1) + ", " +
                            "last_attempt = " + uday + ", " +
                            "next_scheduled = " + next_scheduled + " " +
                            "WHERE ListCardID = " + current.id;
            database.execSQL(sql);

            LexData.HList updated = getList(database, listTitle);
            return updated;
    }

    /*
    public static List<HookSet> getHookSets(List<String>cards)
    {
        Assembly myAssembly = typeof(Utilities).Assembly;
        ResourceManager rm = new ResourceManager("Hoot.Resources.Strings", myAssembly);

        List<HookSet> myList = new List<HookSet>();
        foreach (String line in cards)
        {
            String adder;
            adder = line.Trim(); // leading spaces okay in file
            if (adder.Length == 0)
                continue;
            adder = hStringUtils.RemoveDiacritics(adder);
            if (!Char.IsLetter(adder[0]))
                continue;

            adder = adder.ToUpper(); // already trimmed

            // split words and add to list
            if (adder.Length < Flags.lenLimit + 1)
            {
                adder = adder.ToUpper(); // already trimmed
                HookSet lineWord = new HookSet(adder);
                // NEED TO FIND ALL WORDS
                lineWord = Hoot.HookSet.findWord(adder, LexWords.hootData);
                if (!(lineWord == null))
                    myList.Add(lineWord);
            }
        }
        return (myList);
    }
    */

/*     O R I G I N A L  CODE
    public static int getMinLastAttempt(LexData.Cardbox cb, int list)
    {
        String db = CardUtils.dbSource(cb);


        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        sqlcmd.CommandText =
                "SELECT Min(last_attempt) from tblListWords WHERE ListCardID = '" + list + "' ";
        int min = Convert.ToInt32(sqlcmd.ExecuteScalar());
        connection.Close();
        return min;
    }
    public static void startListQuery(LexData.Cardbox cb, int list)
    {
        String db = CardUtils.dbSource(cb);
        DateTime thePresent = DateTime.Now;
        Int32 today = CardUtils.unixDate(thePresent);

        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;
        sqlcmd.CommandText =
                "UPDATE tblList SET last_attempt = " + today + " WHERE ListCardID = " + list;
        sqlcmd.ExecuteNonQuery();
        connection.Close();
    }
    public static int addCards(Cardbox cb, decimal boxNumber, List<String> wordList, int days) // adds alphagrams
    {
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        Int32 next = nextDate((int)boxNumber);

        int counter = 0;
        foreach (String addition in wordList)
        try
        {
            String alphagram = hStringUtils.SortString(addition);
            // need to add all fields because ZYZZYVA does not have defaults set in database

            sqlcmd.CommandText =
                    "INSERT INTO questions (question, " +
                            "correct, incorrect, streak, last_correct, difficulty," +
                            "cardbox, next_scheduled) " +
                            "VALUES ('" + alphagram + "', " +
                            "0, 1, 0, 0, 0," +
                            boxNumber + ", " + next + ");";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (SQLiteException ex)
        {
            if (!(ex.ErrorCode == 19)) // simply skip duplicates
                MessageBox.Show(ex.ToString());
        }

        transact.Commit();
        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    public static int addCards(Cardbox cb, decimal boxNumber, List<HookSet> wordList, int days) // adds alphagrams
    {
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        Int32 next = nextDate((int)boxNumber);

        int counter = 0;
        foreach (HookSet addition in wordList)
        try
        {
            String alphagram = hStringUtils.SortString(addition.Word);
            // need to add all fields because ZYZZYVA does not have defaults set in database

            sqlcmd.CommandText =
                    "INSERT INTO questions (question, " +
                            "correct, incorrect, streak, last_correct, difficulty," +
                            "cardbox, next_scheduled) " +
                            "VALUES ('" + alphagram + "', " +
                            "0, 1, 0, 0, 0," +
                            boxNumber + ", " + next + ");";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (SQLiteException ex)
        {
            if (ex.ErrorCode != 19)
                MessageBox.Show(ex.ToString());
        }

        transact.Commit();
        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    public static int addWords(Cardbox cb, decimal boxNumber, List<String> wordList, int days)
    {
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        // adds words instead of alphagrams - Hooks
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        Int32 next = nextDate((int)boxNumber);

        int counter = 0;
        foreach (String addition in wordList)
        try
        {
            // need to add all fields because ZYZZYVA does not have defaults set in database

            sqlcmd.CommandText =
                    "INSERT INTO questions (question, " +
                            "correct, incorrect, streak, last_correct, difficulty," +
                            "cardbox, next_scheduled) " +
                            "VALUES ('" + addition + "', " +
                            "0, 1, 0, 0, 0," +
                            boxNumber + ", " + next + ");";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (SQLiteException ex)
        {
            if (!(ex.ErrorCode == 19)) // simply skip duplicates
                MessageBox.Show(ex.ToString());
        }

        transact.Commit();
        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    public static int addWords(Cardbox cb, decimal boxNumber, List<HookSet> wordList, int days)
    {
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        // adds words instead of alphagrams - Hooks
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        Int32 next = nextDate((int)boxNumber);

        int counter = 0;
        foreach (HookSet addition in wordList)
        try
        {
            // need to add all fields because ZYZZYVA does not have defaults set in database
            sqlcmd.CommandText =
                    "INSERT INTO questions (question, " +
                            "correct, incorrect, streak, last_correct, difficulty," +
                            "cardbox, next_scheduled) " +
                            "VALUES ('" + addition.Word + "', " +
                            "0, 1, 0, 0, 0," +
                            boxNumber + ", " + next + ");";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (SQLiteException ex)
        {
            if (!(ex.ErrorCode == 19)) // simply skip duplicates
                MessageBox.Show(ex.ToString());
        }

        transact.Commit();
        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    public static ZWord getCard(Cardbox cb, String word)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        ZWord wordInfo = new ZWord();
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql = "SELECT * from questions WHERE question = '" + word + "';";
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();
        if (myReader.Read())
        {
            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = Convert.ToDecimal(myReader["correct"]);
            wordInfo.incorrect = Convert.ToDecimal(myReader["incorrect"]);
            wordInfo.streak = Convert.ToDecimal(myReader["streak"]);
            wordInfo.last_correct = Convert.ToInt32(myReader["last_correct"]);
            wordInfo.difficulty = Convert.ToDecimal(myReader["difficulty"]);
            wordInfo.cardbox = Convert.ToDecimal(myReader["cardbox"]);
            wordInfo.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
        }
        myReader.Close();
        return wordInfo;
    }
    public static List<ZWord> getCards(Cardbox cb) // alphagrams
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<ZWord> listWords = new List<ZWord>();

        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql = "SELECT * from questions;";
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
        {
            ZWord wordInfo = new ZWord();
            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = Convert.ToDecimal(myReader["correct"]);
            wordInfo.incorrect = Convert.ToDecimal(myReader["incorrect"]);
            wordInfo.streak = Convert.ToDecimal(myReader["streak"]);
            wordInfo.last_correct = Convert.ToInt32(myReader["last_correct"]);
            wordInfo.difficulty = Convert.ToDecimal(myReader["difficulty"]);
            wordInfo.cardbox = Convert.ToDecimal(myReader["cardbox"]);
            wordInfo.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            listWords.Add(wordInfo);
        }
        myReader.Close();
        return listWords;
    }
    public static List<ZWord> getCardBox(Cardbox cb, decimal boxnumber) // alphagrams
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<ZWord> listWords = new List<ZWord>();

        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql = "SELECT * from questions WHERE cardbox = " + boxnumber.ToString() + ";";
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
        {
            ZWord wordInfo = new ZWord();
            wordInfo.question = myReader["question"].ToString();
            wordInfo.correct = Convert.ToDecimal(myReader["correct"]);
            wordInfo.incorrect = Convert.ToDecimal(myReader["incorrect"]);
            wordInfo.streak = Convert.ToDecimal(myReader["streak"]);
            wordInfo.last_correct = Convert.ToInt32(myReader["last_correct"]);
            wordInfo.difficulty = Convert.ToDecimal(myReader["difficulty"]);
            wordInfo.cardbox = Convert.ToDecimal(myReader["cardbox"]);
            wordInfo.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            listWords.Add(wordInfo);
        }
        myReader.Close();
        return listWords;
    }
    public static List<ZWord> getCardsDue(Cardbox cb, DateTime due)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<ZWord> cards = new List<ZWord>();
        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql = "SELECT question, cardbox, next_scheduled from questions WHERE next_scheduled <= " + unixDate(due) + " " +
                " ORDER BY next_scheduled;";
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();

        while (myReader.Read())
        {
            ZWord card = new ZWord();
            card.question = myReader["question"].ToString();
            card.cardbox = Convert.ToDecimal(myReader["cardbox"]);
            card.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            cards.Add(card);
        }
        myReader.Close();
        return cards;
    }
    public static List<ZWord> getCardSchedule(Cardbox cb)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<ZWord> cards = new List<ZWord>();
        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql = "SELECT question, cardbox, next_scheduled from questions ORDER BY next_scheduled;";
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();

        while (myReader.Read())
        {
            ZWord card = new ZWord();
            card.question = myReader["question"].ToString();
            card.cardbox = Convert.ToInt32(myReader["cardbox"]);
            card.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            cards.Add(card);
        }
        myReader.Close();
        return cards;
    }

    public static ZWord moveCard(Cardbox cb, String word, int direction)
    {
        // sets correct to today if moving forward
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);

        ZWord selection = getCard(cb, word);
        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
        if (direction == (int) CardUtils.boxmove.forward)
        {
            selection.cardbox += 1;
            selection.next_scheduled = nextDate((int)selection.cardbox);
            selection.last_correct = today;
            sqlcmd.CommandText =
                    "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                            "correct = " + ++selection.correct + ", " +
                            "streak = " + ++selection.streak + ", " +
                            "last_correct = " + selection.last_correct + ", " +
                            "next_scheduled = " + selection.next_scheduled + " " +
                            "WHERE question = '" + word + "';";
        }
        else
        {
            if (direction == (int) CardUtils.boxmove.back)
                selection.cardbox = lastbox((int)selection.cardbox);
            else // boxmove.zero
                selection.cardbox = 0;
            selection.next_scheduled = nextDate((int)selection.cardbox);
            sqlcmd.CommandText =
                    "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                            "incorrect = " + ++selection.incorrect + ", " +
                            "streak = " + 0 + ", " +
                            "next_scheduled = " + selection.next_scheduled + " " +
                            "WHERE question = '" + word + "';";
        }
        try
        {
            sqlcmd.ExecuteNonQuery();
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
            return null;
        }

        connection.Close();
        return selection;
    }
    public static bool moveCards(Cardbox cb, List<String> words, int direction)
    {
        // sets correct to today if moving forward
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        foreach (String word in words)
        {
            ZWord selection = getCard(cb, word);
            // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
            if (direction == (int) CardUtils.boxmove.forward)
            {
                selection.cardbox += 1;
                selection.next_scheduled = nextDate((int)selection.cardbox);
                selection.last_correct = today;
                sqlcmd.CommandText =
                        "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                                "correct = " + ++selection.correct + ", " +
                                "streak = " + ++selection.streak + ", " +
                                "last_correct = " + selection.last_correct + ", " +
                                "next_scheduled = " + selection.next_scheduled + " " +
                                "WHERE question = '" + word + "';";
            }
            else
            {
                if (direction == (int) CardUtils.boxmove.back)
                    selection.cardbox = lastbox((int)selection.cardbox);
                else // boxmove.zero
                    selection.cardbox = 0;
                selection.next_scheduled = nextDate((int)selection.cardbox);
                sqlcmd.CommandText =
                        "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                                "incorrect = " + ++selection.incorrect + ", " +
                                "streak = " + 0 + ", " +
                                "next_scheduled = " + selection.next_scheduled + " " +
                                "WHERE question = '" + word + "';";
            }
            try
            {
                sqlcmd.ExecuteNonQuery();
            }
            catch (System.Exception ex)
            {
                MessageBox.Show(ex.ToString());
                return false;
            }

        }
        transact.Commit();
        connection.Close();

        return true;
    }

    public static bool moveCards(Cardbox cb, List<String> words, decimal box)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        foreach (String word in words)
        {
            ZWord selection = getCard(cb, word);
            selection.cardbox = box;
            selection.next_scheduled = nextDate((int)selection.cardbox);
            sqlcmd.CommandText =
                    "UPDATE questions SET cardbox = " + selection.cardbox + ", " +
                            "next_scheduled = " + selection.next_scheduled + " " +
                            "WHERE question = '" + word + "';";
            try
            {
                sqlcmd.ExecuteNonQuery();
            }
            catch (System.Exception ex)
            {
                MessageBox.Show(ex.ToString());
                return false;
            }
        }
        transact.Commit();
        connection.Close();
        return true;
    }

    public static ZWord rescheduleCard(Cardbox cb, String word, DateTime newday)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);

        ZWord selection = getCard(cb, word);
        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
        selection.next_scheduled = CardUtils.unixDate(newday);
        sqlcmd.CommandText =
                "UPDATE questions SET next_scheduled = " + selection.next_scheduled + " " +
                        "WHERE question = '" + word + "';";
        try
        {
            sqlcmd.ExecuteNonQuery();
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
            return null;
        }
        connection.Close();

        //DateTime due = dtDate(selection.next_scheduled);
        //int days = due.Subtract(DateTime.Now).Days;
        return selection;
    }
    public static bool rescheduleCards(Cardbox cb, List<String> words, DateTime newday)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        foreach (String word in words)
        {
            ZWord selection = getCard(cb, word);
            // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
            selection.next_scheduled = CardUtils.unixDate(newday);
            sqlcmd.CommandText =
                    "UPDATE questions SET next_scheduled = " + selection.next_scheduled + " " +
                            "WHERE question = '" + word + "';";
            try
            {
                sqlcmd.ExecuteNonQuery();
            }
            catch (System.Exception ex)
            {
                MessageBox.Show(ex.ToString());
                return false;
            }
        }
        transact.Commit();
        connection.Close();
        return true;
    }

    public static bool dropCards(Cardbox cb, List<String> cards)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;
        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        foreach (String card in cards)
        {
            sqlcmd.CommandText =
                    "DELETE FROM questions WHERE question = '" + card + "'";
            try
            {
                sqlcmd.ExecuteNonQuery();
            }
            catch (System.Exception ex)
            {
                connection.Close();
                MessageBox.Show(ex.ToString());
                return false;
            }
        }
        transact.Commit();
        connection.Close();
//            MessageBox.Show(String.Format("Deleted {0} from list", card));
        return true;
    }

    // Hoot Lists
    public static int OrigaddList(Cardbox cb, HList hlist, List<String> wordList)
    {
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        DateTime thePresent = DateTime.Now;
        Int32 added = (Int32)(thePresent.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

        sqlcmd.CommandText =
                "INSERT INTO tblList (ListCardTitle, ListCardDescription, " +
                        "quiz_type, " +
                        "lastscore, mean, passing, attempts, last_attempt, difficulty, " +
                        "cardbox, next_scheduled, " +
                        "date_added) " +
                        "VALUES ('" + hlist.title + "', '" + hlist.description + "', " +
                        "1," +
                        "0,0," + hlist.passing +
                        ",0,0,0," +
                        hlist.cardbox + ", " + nextDate((int)hlist.cardbox) + ", " + added + ");";            // add grading, box, etc

        // need to check for duplicate (catch)
        sqlcmd.ExecuteNonQuery();
        // get list id

        sqlcmd.CommandText = "SELECT last_insert_rowid();";
        var listID = sqlcmd.ExecuteScalar();
        transact.Commit();
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        // should call separate method to add words
        int counter = 0;
        foreach (String addition in wordList)
        try
        {
            sqlcmd.CommandText =
                    "INSERT INTO tblListWords (ListCardID, ListWord, " +
                            "lastscore, mean, attempts, last_attempt, difficulty, cardbox, next_scheduled) " +
                            "VALUES ( " + listID + ", '" + addition + "', " +
                            "0,0,0,0,0,0," +
                            nextDate((int)hlist.cardbox) + "); ";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
        }

        transact.Commit();

        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    public static int addList(Cardbox cb, HList hlist, List<String> wordList)
    {
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 added = (Int32)(thePresent.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

        sqlcmd.CommandText =
                "INSERT INTO tblList (ListCardTitle, ListCardDescription, " +
                        "quiz_type, " +
                        "lastscore, mean, passing, attempts, last_attempt, difficulty, " +
                        "cardbox, next_scheduled, " +
                        "date_added) " +
                        "VALUES ('" + hlist.title + "', '" + hlist.description + "', " +
                        hlist.quiz_type + ", " +
                        "0,0," + hlist.passing +
                        ",0,0,0," +
                        hlist.cardbox + ", " + nextDate((int)hlist.cardbox) + ", " + added + ");";            // add grading, box, etc

        sqlcmd.ExecuteNonQuery();
        connection.Close();

        int counter = 0;
        if (hlist.quiz_type == Searches.getSearchType("Anagrams"))
            counter = CardUtils.addListCards(cb, wordList, hlist.title);
        else
            counter = CardUtils.addListWords(cb, wordList, hlist.title);
        return counter;
    }
    public static int addList(Cardbox cb, List<String> wordList, String listTitle, String quiztype, int passing)
    {
        int subsearch = Searches.getSearchType(quiztype);
        // force all new lists to box 0
        decimal boxNumber = 0;
        Directory.CreateDirectory(programData(cb.program) + cb.lexicon);
        if (!File.Exists(CardUtils.dbSource(cb)))
            CardUtils.createDatabase(cb);

        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 added = (Int32)(thePresent.Subtract(new DateTime(1970, 1, 1))).TotalSeconds;

        sqlcmd.CommandText =
                "INSERT INTO tblList (ListCardTitle, ListCardDescription, " +
                        "quiz_type, " +
                        "lastscore, mean, passing, attempts, last_attempt, difficulty, " +
                        "cardbox, next_scheduled, " +
                        "date_added) " +
                        "VALUES ('" + listTitle + "', 'Description', " + subsearch + "," +
                        "0,0," + passing + ",0,0,0," +
                        boxNumber + ", " + nextDate((int)boxNumber) + ", " + added + ");";            // add grading, box, etc

        sqlcmd.ExecuteNonQuery();
        connection.Close();

        int counter = 0;
        if (quiztype == "Anagrams")
            counter = CardUtils.addListCards(cb, wordList, listTitle);
        else
            counter = CardUtils.addListWords(cb, wordList, listTitle);

        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    public static bool saveList(Cardbox cb, HList hlist)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        sqlcmd.CommandText =
                "UPDATE tblList " +
                        "SET ListCardTitle = '" + hlist.title + "', " +
                        "ListCardDescription = '" + hlist.description + "', " +
                        "quiz_type = " + hlist.quiz_type + ", " +
                        "lastscore = " + hlist.lastscore + ", " +
                        "mean = " + hlist.mean + ", " +
                        "passing = " + hlist.passing + ", " +
                        "attempts = " + hlist.attempts + ", " +
                        "last_attempt = " + hlist.last_attempt + ", " +
                        "difficulty = " + hlist.difficulty + ", " +
                        "cardbox = " + hlist.cardbox + ", " +
                        "next_scheduled = " + hlist.next_scheduled + ", " +
                        "date_added = " + hlist.date_added + " " +
                        "WHERE ListCardID = " + hlist.id + ";";

        // need to check for duplicate (catch)
        sqlcmd.ExecuteNonQuery();
        // get list id

        connection.Close();
        return true;
    }

    // need to change to HListWord??
    public static int addListWords(Cardbox cb, List<String> wordList, String listTitle)
    {
        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        sqlcmd.CommandText =
                "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
        int listID = Convert.ToInt32(sqlcmd.ExecuteScalar());

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        int counter = 0;
        foreach (String addition in wordList)
        try
        {
            //"INSERT INTO tblListWords (ListCardID, ListWord) " +
            //"VALUES ( " + listID + ", '" + addition + "');";

            sqlcmd.CommandText =
                    "INSERT INTO tblListWords (ListCardID, ListWord, " +
                            "lastscore, mean, attempts, last_attempt, difficulty, cardbox, next_scheduled) " +
                            "VALUES ( " + listID + ", '" + addition + "', " +
                            "0,0,0,0,0,0," +
                            nextDate(0) + "); ";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (SQLiteException ex)
        {
            if (!(ex.ErrorCode == 19)) // simply skip duplicates
                MessageBox.Show(ex.ToString());
        }

        transact.Commit();

        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }
    // need to change to HListWord??
    public static int addListCards(Cardbox cb, List<String> wordList, String listTitle) // adds alphagrams
    {
        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        sqlcmd.CommandText =
                "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
        int listID = Convert.ToInt32(sqlcmd.ExecuteScalar());

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        int counter = 0;
        foreach (String addition in wordList)
        try
        {
            String alphagram = hStringUtils.SortString(addition);

            sqlcmd.CommandText =
                    "INSERT INTO tblListWords (ListCardID, ListWord, " +
                            "lastscore, mean, attempts, last_attempt, difficulty, cardbox, next_scheduled) " +
                            "VALUES ( " + listID + ", '" + alphagram + "', " +
                            "0,0,0,0,0,0," +
                            nextDate(0) + "); ";
            sqlcmd.ExecuteNonQuery();
            counter++;
        }
        catch (SQLiteException ex)
        {
            if (!(ex.ErrorCode == 19)) // simply skip duplicates
                MessageBox.Show(ex.ToString());
//                    counter--;
// COUNTER IS WRONG

        }

        transact.Commit();

        connection.Close();
        MessageBox.Show(String.Format("Added {0} words to list", counter.ToString()));
        return counter;
    }

    public static List<String> getListTitles(Cardbox cb)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<String> lstReader = new List<String>();
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql =
                "SELECT ListCardTitle from tblList";
        // need to specify list name
        // need list name drop down
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
        {
            String title = myReader.GetString(0);
            lstReader.Add(title);
        }
        myReader.Close();
        return lstReader;
    }
    public static List<HList> getLists(Cardbox cb)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }
        // just getting names first
        List<String> listTitles = new List<String>();
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql =
                "SELECT * from tblList";
        // need to specify list name
        // need list name drop down
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);
        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
            listTitles.Add(myReader["ListCardTitle"].ToString());
        myReader.Close();

        List<HList> hlists = new List<HList>();
        foreach (String list in listTitles)
        {
            HList hlist = getList(cb, list);
            hlists.Add(hlist);
        }
        return hlists;

    }
    public static HList getList(Cardbox cb, String listTitle)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        HList hlist = new HList();
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;
        sqlcmd.CommandText =
                "SELECT * FROM tblList WHERE ListCardTitle = '" + listTitle + "'";
        SQLiteDataReader myReader = sqlcmd.ExecuteReader();
        while (myReader.Read())
        {
            hlist.title = myReader["ListCardTitle"].ToString();
            hlist.description = myReader["ListCardDescription"].ToString();
            hlist.quiz_type = Convert.ToInt32(myReader["quiz_type"]);
            hlist.id = Convert.ToInt32(myReader["ListCardID"]);
            hlist.lastscore = Convert.ToInt32(myReader["lastscore"]);
            hlist.mean = Convert.ToInt32(myReader["mean"]);
            hlist.passing = Convert.ToInt32(myReader["passing"]);
            hlist.attempts = Convert.ToInt32(myReader["attempts"]);
            hlist.last_attempt = Convert.ToInt32(myReader["last_attempt"]);
            hlist.difficulty = Convert.ToInt32(myReader["difficulty"]);
            hlist.cardbox = Convert.ToInt32(myReader["cardbox"]);
            hlist.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            hlist.date_added = Convert.ToInt32(myReader["date_added"]);
        }
        myReader.Close();
        return hlist;
    }
    public static List<HListWord> getListBox(Cardbox cb, decimal boxnumber, String title)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<HListWord> lstReader = new List<HListWord>();
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql =
                "SELECT ListWord, tblListWords.ListWordID, tblListWords.ListCardID, tblListWords.lastscore, tblListWords.mean, tblListWords.attempts, " +
                        "tblListWords.last_attempt, tblListWords.difficulty, tblListWords.cardbox, tblListWords.next_scheduled " +
                        "FROM tblListWords, tblList WHERE (tblListWords.ListCardID = tblList.ListCardID) " +
                        "AND (tblListWords.cardbox = " + boxnumber.ToString() + ") " +
                        "AND (tblList.ListCardTitle = '" + title + "') " +
                        "ORDER BY tblListWords.ListWordID;";
        // need to specify list name
        // need list name drop down
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
        {
            HListWord card = new HListWord();
            card.listWordID = Convert.ToInt32(myReader["ListWordID"]);
            card.listID = Convert.ToInt32(myReader["ListCardID"]);
            card.word = myReader["ListWord"].ToString();
            card.lastscore = Convert.ToInt32(myReader["lastscore"]);
            card.mean = Convert.ToInt32(myReader["mean"]);
            card.attempts = Convert.ToInt32(myReader["attempts"]);
            card.last_attempt = Convert.ToInt32(myReader["last_attempt"]);
            card.difficulty = Convert.ToInt32(myReader["difficulty"]);
            card.cardbox = Convert.ToInt32(myReader["cardbox"]);
            card.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            lstReader.Add(card);
        }
        myReader.Close();
        return lstReader;
    }
    public static List<HListWord> getListWords(Cardbox cb, String title)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }

        List<HListWord> cards = new List<HListWord>();
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        String sql =
                "SELECT ListWord, tblListWords.ListWordID, tblListWords.ListCardID, tblListWords.lastscore, tblListWords.mean, tblListWords.attempts, " +
                        "tblListWords.last_attempt, tblListWords.difficulty, tblListWords.cardbox, tblListWords.next_scheduled " +
                        "FROM tblListWords, tblList WHERE (tblListWords.ListCardID = tblList.ListCardID) " +
                        "AND (tblList.ListCardTitle = '" + title + "') " +
                        "ORDER BY tblListWords.ListWordID;";

        // need to specify list name
        // need list name drop down
        SQLiteCommand cmd = new SQLiteCommand(sql, connection);

        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
        {
            HListWord card = new HListWord();
            card.listWordID = Convert.ToInt32(myReader["ListWordID"]);
            card.listID = Convert.ToInt32(myReader["ListCardID"]);
            card.word = myReader["ListWord"].ToString();
            card.lastscore = Convert.ToInt32(myReader["lastscore"]);
            card.mean = Convert.ToInt32(myReader["mean"]);
            card.attempts = Convert.ToInt32(myReader["attempts"]);
            card.last_attempt = Convert.ToInt32(myReader["last_attempt"]);
            card.difficulty = Convert.ToInt32(myReader["difficulty"]);
            card.cardbox = Convert.ToInt32(myReader["cardbox"]);
            card.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            cards.Add(card);
        }
        myReader.Close();
        return cards;
    }
    public static List<HListWord> getListWordsDue(Cardbox cb, String title, DateTime due)
    {
        if (!File.Exists(CardUtils.dbSource(cb)))
        {
            MessageBox.Show("Database does not exist");
            return null;
        }
        List<HListWord> words = new List<HListWord>();
        String db = CardUtils.dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();
        String sql =
                "SELECT ListWord, tblListWords.ListWordID, tblListWords.ListCardID, tblListWords.lastscore, tblListWords.mean, tblListWords.attempts, " +
                        "tblListWords.last_attempt, tblListWords.difficulty, tblListWords.cardbox, tblListWords.next_scheduled " +
                        "FROM tblListWords, tblList WHERE (tblListWords.ListCardID = tblList.ListCardID) " +
                        "AND tblList.ListCardTitle = '" + title + "' " +
                        "AND tblListWords.next_scheduled <= " + unixDate(due) + " " +
                        "ORDER BY tblListWords.next_scheduled;";

        SQLiteCommand cmd = new SQLiteCommand(sql, connection);
        SQLiteDataReader myReader = cmd.ExecuteReader();
        while (myReader.Read())
        {
            HListWord word = new HListWord();
            word.listWordID = Convert.ToInt32(myReader["ListWordID"]);
            word.listID = Convert.ToInt32(myReader["ListCardID"]);
            word.word = myReader["ListWord"].ToString();
            word.lastscore = Convert.ToInt32(myReader["lastscore"]);
            word.mean = Convert.ToInt32(myReader["mean"]);
            word.attempts = Convert.ToInt32(myReader["attempts"]);
            word.last_attempt = Convert.ToInt32(myReader["last_attempt"]);
            word.difficulty = Convert.ToInt32(myReader["difficulty"]);
            word.cardbox = Convert.ToInt32(myReader["cardbox"]);
            word.next_scheduled = Convert.ToInt32(myReader["next_scheduled"]);
            words.Add(word);
        }
        myReader.Close();
        return words;
    }
    //        public static HListWord getListWord(Cardbox cb, String title, String )
    public static bool moveList(Cardbox cb, String title, int direction)
    {
        // attempts, lastscore, mean, last_attempt set by gradeList
        String db = dbSource(cb);
        HList hlist = getList(cb, title);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);

        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak
        if (direction == (int) CardUtils.boxmove.forward)
        {
            hlist.cardbox += 1;
            hlist.next_scheduled = nextDate((int)hlist.cardbox);
            sqlcmd.CommandText =
                    "UPDATE tblList SET cardbox = " + hlist.cardbox + ", " +
                            "next_scheduled = " + hlist.next_scheduled + " " +
                            "WHERE ListCardTitle = '" + title + "';";
        }
        else
        {
            if (direction == (int) CardUtils.boxmove.back)
                hlist.cardbox = lastbox((int)hlist.cardbox);
            else // boxmove.zero
                hlist.cardbox = 0;
            hlist.next_scheduled = nextDate((int)hlist.cardbox);
            sqlcmd.CommandText =
                    "UPDATE tblList SET cardbox = " + hlist.cardbox + ", " +
                            "next_scheduled = " + hlist.next_scheduled + " " +
                            "WHERE ListCardTitle = '" + title + "';";
        }
        try
        {
            sqlcmd.ExecuteNonQuery();
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
            return false;
        }

        connection.Close();

        DateTime due = dtDate(hlist.next_scheduled);
//            DateTime due = new DateTime(1970, 1, 1, 0, 0, 0, 0, System.DateTimeKind.Utc);
//            due = due.AddSeconds(hlist.next_scheduled).ToLocalTime();
        int days = due.Subtract(DateTime.Now).Days;

        MessageBox.Show(String.Format("Moved {0} to box {1}: Due: {2} ({3} days)",
                title, hlist.cardbox, due.ToString("d"), days));
        // get boxtype from db
        return true;
    }
    public static bool rescheduleList(Cardbox cb, String title, DateTime newday)
    {
        // attempts, lastscore, mean, last_attempt set by gradeList
        String db = dbSource(cb);
        HList hlist = getList(cb, title);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        hlist.next_scheduled = CardUtils.unixDate(newday);

        sqlcmd.CommandText =
                "UPDATE tblList SET next_scheduled = " + hlist.next_scheduled + " " +
                        "WHERE ListCardTitle = '" + title + "';";
        try
        {
            sqlcmd.ExecuteNonQuery();
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
            return false;
        }

        connection.Close();

        MessageBox.Show(String.Format("Rescheduled {0} for {1})",
                title, dtDate(hlist.next_scheduled)));
        return true;
    }
    // only need to move one at a time
    public static HListWord moveListWord(Cardbox cb, String title, HListWord gradeword, int direction)
    {
        // mean is set by calling method
        // this sets box, schedule and saves
        String db = dbSource(cb);
        HListWord listword = new HListWord(gradeword);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        DateTime thePresent = DateTime.Now;
        Int32 today = unixDate(thePresent);
        // ?? streak in Zyzzyva goes from 0 to -1; ?? highest or current streak

        if (direction == (int) CardUtils.boxmove.forward)
        {
            listword.cardbox += 1;
        }
        else
        {
            if (direction == (int) CardUtils.boxmove.back)
                listword.cardbox = lastbox((int)listword.cardbox);
            else // boxmove.zero
                listword.cardbox = 0;
        }
        listword.next_scheduled = nextDate((int)listword.cardbox);
        sqlcmd.CommandText =
                "UPDATE tblListWords SET cardbox = " + listword.cardbox + ", " +
                        "mean = " + listword.mean + ", " +
                        "attempts = " + ++listword.attempts + ", " +
                        "last_attempt = " + today + ", " +
                        "lastscore = " + listword.lastscore + ", " +
                        "next_scheduled = " + listword.next_scheduled + " " +
                        "WHERE ListWord = '" + listword.word + "' " +
                        "AND ListCardID = " + listword.listID + ";";
        try
        {
            sqlcmd.ExecuteNonQuery();
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
            return null;
        }
        connection.Close();
        return listword;
    }
    public static bool dropList(Cardbox cb, String listTitle)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        try
        {
            // select listid
            sqlcmd.CommandText =
                    "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
            int id = Convert.ToInt32(sqlcmd.ExecuteScalar());

            // for listid, delete words
            sqlcmd.CommandText =
                    "DELETE FROM tblListWords WHERE ListCardID = " + id;
            sqlcmd.ExecuteNonQuery();

            // delete list
            sqlcmd.CommandText =
                    "DELETE FROM tblList WHERE ListCardID = " + id;
            sqlcmd.ExecuteNonQuery();

            transact.Commit();

            connection.Close();
            return true;
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
        }
        return false;
    }

    public static bool dropListCards(Cardbox cb, String listTitle, List<String> cards)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        SQLiteTransaction transact;
        transact = connection.BeginTransaction();
        sqlcmd.Transaction = transact;

        foreach (String card in cards)
        {
            sqlcmd.CommandText =
                    "SELECT ListCardID from tblList WHERE ListCardTitle = '" + listTitle + "' ";
            int id = Convert.ToInt32(sqlcmd.ExecuteScalar());

            sqlcmd.CommandText =
                    "DELETE FROM tblListWords WHERE ListWord = '" + card + "'" +
                            " AND ListCardID = " + id + ";";
            try
            {
                sqlcmd.ExecuteNonQuery();
            }
            catch (System.Exception ex)
            {
                connection.Close();
                MessageBox.Show(ex.ToString());
                return false;
            }
        }
        transact.Commit();
        connection.Close();
//            MessageBox.Show(String.Format("Deleted {0} from list", card));
        return true;
    }
    public static HList scoreList(Cardbox cb, String listTitle)
    {
        String db = dbSource(cb);
        SQLiteConnection connection = new SQLiteConnection("Data Source=" + db + ";Version=3;");
        connection.Open();

        SQLiteCommand sqlcmd = new SQLiteCommand();
        sqlcmd.Connection = connection;

        try
        {
            HList current = getList(cb, listTitle);
            decimal cardbox = 0;

            DateTime thePresent = DateTime.Now;
            Int32 today = unixDate(thePresent);
            sqlcmd.CommandText =
                    "SELECT AVG(lastscore) FROM tblListWords WHERE ListCardID = " + current.id;
            int mean = Convert.ToInt32(sqlcmd.ExecuteScalar());

            // sets cardbox as a rank between passing score and 100
            cardbox = (mean - current.passing) / ((100 - current.passing) / 10);

            // alternately, move to next box if passing score
            // if (mean >= current.passing) cardbox++;

            int next_scheduled = nextDate((int)cardbox);

            sqlcmd.CommandText =
                    "UPDATE tblList SET mean = " + mean + ", " +
                            "lastscore = " + current.mean + ", " +
                            "cardbox = " + cardbox + ", "+
                            "attempts = " + (current.attempts + 1) + ", " +
                            "last_attempt = " + today + ", " +
                            "next_scheduled = " + next_scheduled + " " +
                            "WHERE ListCardID = " + current.id;
            sqlcmd.ExecuteNonQuery();

            HList updated = getList(cb, listTitle);
            connection.Close();
            return updated;
        }
        catch (System.Exception ex)
        {
            MessageBox.Show(ex.ToString());
        }
        return null;
    }

    public static List<HookSet> getHookSets(List<String>cards)
    {
        Assembly myAssembly = typeof(Utilities).Assembly;
        ResourceManager rm = new ResourceManager("Hoot.Resources.Strings", myAssembly);

        List<HookSet> myList = new List<HookSet>();
        foreach (String line in cards)
        {
            String adder;
            adder = line.Trim(); // leading spaces okay in file
            if (adder.Length == 0)
                continue;
            adder = hStringUtils.RemoveDiacritics(adder);
            if (!Char.IsLetter(adder[0]))
                continue;

            adder = adder.ToUpper(); // already trimmed

            // split words and add to list
            if (adder.Length < Flags.lenLimit + 1)
            {
                adder = adder.ToUpper(); // already trimmed
                HookSet lineWord = new HookSet(adder);
                // NEED TO FIND ALL WORDS
                lineWord = Hoot.HookSet.findWord(adder, LexWords.hootData);
                if (!(lineWord == null))
                    myList.Add(lineWord);
            }
        }
        return (myList);
    }
*/
}
