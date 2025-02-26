package com.tylerhosting.hoot.hoot;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.Objects;

import static com.tylerhosting.hoot.hoot.CardUtils.programData;

public class LexData {
//    public static int CURRENT_VERSION = 11;
    // CSW22 is version 11

    public static int notification_id = 1;
    public static int getNotification_id() {
        return notification_id++;
    }
    // Global Length filter




//    private static boolean showFilter = true;
//    public static void setShowFilter(boolean filterSetting) { showFilter = filterSetting; }
//    public static boolean getShowFilter() { return showFilter; }

    private static int tileColor = 0xffffffff; // 1 = default
    public static void setTileColor(int colorInt) {
        tileColor = colorInt;
    }
    public static int getTileColor(Context context) {
        SharedPreferences shared;
        shared = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String themeName = shared.getString("theme", "Light Theme");
        if (themeName.equals("Light Theme") && tileColor == 0xffffffff)
            tileColor = 0xff000000;
        if (themeName.equals("Dark Theme") && tileColor == 0xff000000)
            tileColor = 0xffffffff;

        return tileColor;
    }

    public static int CURRENT_VERSION = 18; // Change here and NEW_DB_VERSION in SplashActivity

    private static boolean showCondensed = true;
    public static void setShowCondensed(boolean condensed) { showCondensed = condensed; }
    public static boolean getShowCondensed() { return showCondensed; }


    private static boolean showHooks = true;
    public static void setShowHooks(boolean hookSetting) { showHooks = hookSetting; }
    public static boolean getShowHooks() { return showHooks; }

    private static boolean showQuizHooks = true;
    public static void setShowQuizHooks(boolean quizHookSetting) { showQuizHooks = quizHookSetting; }
    public static boolean getShowQuixHooks() { return showQuizHooks; }

    private static boolean showStats = true;
    public static void setShowStats(boolean statSetting) { showStats = statSetting; }
    public static boolean getShowStats() { return showStats; }

    private  static boolean colorBlanks = true;
    public static void setColorBlanks(boolean colorBlanksColor) { colorBlanks = colorBlanksColor; }
    public static boolean getColorBlanks() { return colorBlanks; }

    private  static boolean fastScroll = true;
    public static void setFastScroll(boolean gofast) { fastScroll = gofast; }
    public static boolean getFastScroll() { return fastScroll; }

    private static boolean customkeyboard = true;
    public static void setCustomkeyboard(boolean ckey) { customkeyboard = ckey; }
    public static boolean getCustomkeyboard() { return customkeyboard;}

    private static String altOption = "Original";
    public static void setAltOption(String altSetting) { altOption = altSetting; }
    public static String getAltOption() { return altOption; }

    private  static String tapOption = "Definition";
    public static void setTapOption(String tapSetting) { tapOption = tapSetting; }
    public static String getTapOption() { return  tapOption; }


    private static int maxLength = 15;
    public static void setMaxLength(int maxLength) {
        LexData.maxLength = maxLength;
    }
    public static int getMaxLength() {
        return maxLength;
    }

    private static int listLimit = 5000;
    public static void setMaxList(int listLimit) {
        LexData.listLimit = listLimit;
    }
    public static int getMaxList() {
        return listLimit;
    }



    public static boolean AutoAdvance = false;
    public static void setAutoAdvance(boolean autoadvance) { AutoAdvance = autoadvance; }
    public static boolean getAutoAdvance() { return AutoAdvance; }

    public static boolean slideLoop = true;
    public static void setSlideLoop(boolean loop) { slideLoop = loop; }
    public static boolean getSlideLoop() { return slideLoop; }


    private static String databasePath;
    public static String getDatabasePath() { return databasePath; }
    public static void setDatabasePath(Context context, String databasePath) {
        LexData.databasePath = databasePath;
        if (Objects.equals(databasePath, "") || databasePath == null)
            LexData.databasePath = context.getApplicationContext().getApplicationInfo().dataDir + "/databases";
    }
    // work around to get getFilesDir for app without context
    public static String internalFilesDir;


    private static String database = Flavoring.getflavoring();

    public static String getDatabase() { return database; }
//    public static void setDatabase (Context context, String database) {
//        LexData.database = database;
//    }
    public static void setDatabase(String database) {
        LexData.database = database;
    }

    // Loads Lexicon Details into memory
    private static Structures.Lexicon lexicon;
    public static void setLexicon(Context context, String lexName) {
        DatabaseAccess databaseAccess;
        Structures.Lexicon found;
        databaseAccess = DatabaseAccess.getInstance(context , getDatabasePath(), LexData.getDatabase());

        found = databaseAccess.get_lexicon(lexName);
        lexicon = found;

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor prefs = shared.edit();
        prefs.putString("lexicon", lexName);
        prefs.apply();
        Log.e("DBLex", LexData.getDatabasePath() + " " +  LexData.getLexName());
//
//        Log.i("LexDataPrefs", shared.getString("lexicon", "x"));
    }
    public static void setDefaultLexicon(Context context) {
        DatabaseAccess databaseAccess;
        Structures.Lexicon found;
        databaseAccess = DatabaseAccess.getInstance(context , getDatabasePath(), LexData.getDatabase());

        found = databaseAccess.get_lexicon(databaseAccess.get_firstValidLexicon());
        lexicon = found;

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor prefs = shared.edit();
        prefs.putString("lexicon", LexData.getLexName());
        prefs.apply();
    }


    public static Structures.Lexicon getLexicon() { return lexicon; }
    public  static String getLexName() {
        if (lexicon.LexiconName != null)
            return lexicon.LexiconName;
        else return "";
    }
    public static String getLexLanguage() {
        if (lexicon.LexLanguage == null || lexicon.LexLanguage.isEmpty() )
            return "en";
        else
            return lexicon.LexLanguage;
    }


    // the database from the cardbox


//    private static String cardfilePath;
//    public static String getCardfilePath() { return cardfilePath; }
//    public static void setCardfilePath(Context context, String cardfilePath) {
//        LexData.cardfilePath = cardfilePath;
//    }

    private static String cardfile;
    public static String getCardfile() { return cardfile; }
    public static void setCardfile (Cardbox cb) {
        cardfile = programData(cb.program) + File.separator + cb.lexicon + File.separator + cb.boxtype + ".db";
    }

    public static void setCardfile(String cardfile) {
        LexData.cardfile = cardfile;
    }


    private static String username;
    public static String getUsername() { return username; }
    public static void setUsername(String user) { username = user; }

    private static String cardslocation;
    public static String getCardslocation() { return cardslocation; }
    public static void setCardslocation(String cards) { cardslocation = cards; }

    public static class Cardbox    {
        public String program;
        public String lexicon;
        public String boxtype;
        public String title;
        public Cardbox()
        {
            program = "";
            lexicon = "";
            boxtype = "";
            title = "";
        }
        public Cardbox(String prog, String lex, String cb, String tit)
        {
            program = prog;
            lexicon = lex;
            boxtype = cb;
            title = tit;
        }
        public Cardbox(String prog, String lex, String cb)
        {
            program = prog;
            lexicon = lex;
            boxtype = cb;
            title = "";
        }
    }
    public static class ZWord    {
        public String question;
//        public String getQuestion() { return question; }
//        public void setQuestion(String question) { this.question = question; }

        public int cardbox;
        public int getCardbox() { return cardbox; }
        public void setCardbox(int cardbox) { this.cardbox = cardbox; }


        public int correct;
        public int incorrect;
        public int streak;
        public int last_correct;
        public int difficulty;
        public int next_scheduled;
//        public int date_added;
    }
    public static class HList    {
        public int id;
        public String title;
        public String description;
        public int quiz_type;
        public int lastscore;
        public int mean;
        public int passing;
        public int attempts;
        public int last_attempt;
        public int difficulty;
        public int cardbox;
        public int next_scheduled;
        public int date_added;
    }
    public static class HListWord    {
        public int listWordID;
        public int listID; // listID
        public String getWord() {
            return question;
        }
        public void setWord(String word) {
            this.question = word;
        }

        public String question;           //"ListWord VARCHAR(22) PRIMARY KEY, " +
        public int correct;
        public int incorrect;
        public int streak;
        public int last_correct;
//        public int lastscore;
//        public int mean;
//        public int attempts;
//        public int last_attempt;
        public int difficulty;
        public int cardbox;
        public int next_scheduled;
        public HListWord()
        {
            listWordID = 0;
            listID = 0;
            question = "";

            correct = 0;
            incorrect = 0;
            streak = 0;
            last_correct = 0;

            difficulty = 0;
            cardbox = 0;
            next_scheduled = 0;
        }
        public HListWord(HListWord orig)
        {
            listWordID = orig.listWordID;
            listID = orig.listID;
            question = orig.question;
            correct = orig.correct;
            incorrect = orig.incorrect;
            streak = orig.streak;
            last_correct = orig.last_correct;
            difficulty = orig.difficulty;
            cardbox = orig.cardbox;
            next_scheduled = orig.next_scheduled;
        }
    }

    public static class WordList
    {
        public int ListID;
        public String ListName;
        public String ListAuthor;
        public String ListCredits;
        public String ListDescription;
        public String ListSource;
        public String ListLink;
        public String ListAuthorEmail;
        public int CategoryID;

        public WordList()
        {

        }
        //SELECT ListID, ListName, ListAuthor, ListCredits, ListDescription, ListSource, ListLink, ListAuthorEmail, CategoryID
        public WordList(String name, String author, String credits, String description, String source, String link, String email, int category)
        {
            ListName = name;
            ListAuthor = author;
            ListCredits = credits;
            ListDescription = description;
            ListSource = source;
            ListLink = link;
            ListAuthorEmail = email;
            CategoryID = category;
        }
//        public WordList getWordList(ListID) {
//            if (ListID = WordList.ListID)
//            return WordList;
//        }

    }

public static class GlobalWord {
        public int WordID;
        public String Word;

        public GlobalWord()
        {

        }

        public GlobalWord(int wordid, String word)
        {
            WordID = wordid;
            Word = word;
        }
}









    private static int tiles = 0;
    public static void setTileset(int tileSet) {
        tiles = tileSet;
        Tiles.SetTiles(tileSet);
    }
    public static String valueSet(int value)
    {
        StringBuilder valueString  = new StringBuilder("[");
        for (int n = 0; n < 27; n++)
        {
            if (Tiles.tiles.charvalue[n] == value)
            {
                char ascii = (char)(n + 'A');
                valueString.append(ascii);
            }
        }
        if (valueString.toString().equals("["))
            valueString = new StringBuilder("@");
        else
            valueString.append("]");
        return valueString.toString();
    }
    public static String valueSet(int from, int to)
    {
        StringBuilder valueString  = new StringBuilder("[");
        for (int n = 0; n < 27; n++)
        {
            if (Tiles.tiles.charvalue[n] >= from && Tiles.tiles.charvalue[n] <= to)
            {
                char ascii = (char)(n + 'A');
                valueString.append(ascii);
            }
        }
        if (valueString.toString().equals("["))
            valueString = new StringBuilder("@");
        else
            valueString.append("]");
        return valueString.toString();
    }
    public static String valueLetters(int from, int to)
    {
        StringBuilder valueString  = new StringBuilder();
        for (int n = 0; n < 27; n++)
        {
            if (Tiles.tiles.charvalue[n] >= from && Tiles.tiles.charvalue[n] <= to)
            {
                char ascii = (char)(n + 'A');
                valueString.append(ascii);
            }
        }
        if (valueString.toString().isEmpty())
            valueString = new StringBuilder("@");
        return valueString.toString();
    }
//    public static int[] tileFreq(){
//        Structures.TileFrequency tilefreq[];
//        tilefreq = new Structures.TileFrequency[26];
//
//        for (int i = 0; i < 26; i++)
//        {
//            tilefreq[i] = new Structures.TileFrequency(Tiles.tiles.chardist[i], i);
//        }
//        Arrays.sort(tilefreq);
//
//        int freqIndex[] = new int[26];
//        for (int i = 0; i < 26; i++)
//            freqIndex[i] = tilefreq[i].letter;
//        return freqIndex;
//    }
    public static class Tiles {
        /// <summary> Globally available tile set </summary>
        public static Structures.TileSet tiles;
        public static String[] setName = {
                "English Scrabble",
                "English Friends",
                "French Scrabble",
                "Super Scrabble",
                "WordSmith",
                "French Friends",
                "Italian Friends",
                "Scarabeo (Italian)",
                "Italian Scrabble"
        };
        /// <summary> Set tileset based on number </summary>
        /// <param name="set">set number</param>
        public static void SetTiles(int set)
        {

            // CAUTION: INDEXING DIFFERS FROM PC VERSION
            switch (set)
            {
                case 1:
                    int[] set2char =  { 9, 2, 2, 5, 13, 2, 3, 4, 8, 1, 1, 4, 2, 5, 8, 2, 1, 6, 5, 7, 4, 2, 2, 1, 2, 1,  2 };
                    int[] set2value = { 1, 4, 4, 2, 1, 4, 3, 3, 1, 10, 5, 2, 4, 2, 1, 4, 10, 1, 1, 1, 2, 5, 4, 8, 3, 10,  0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set2char,set2value, 104);
                    break;
                case 2:
                    int[] set3char =  { 9, 2, 2, 3, 15, 2, 2, 2, 8, 1, 1, 5, 3, 6, 6, 2, 1, 6, 6, 6, 6, 2, 1,  1,  1,  1,  2 };
                    int[] set3value = { 1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 10, 1, 2, 1, 1, 3, 8, 1, 1, 1, 1, 4, 10, 10, 10, 10,  0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set3char, set3value, 102);
                    break;
                case 3:
                    int[] set4char = { 16, 4, 6, 8, 24, 4, 5, 5, 13, 2, 2, 7, 6, 13, 15, 4, 2, 13, 10, 15, 7, 3, 4, 2, 4, 2,  4 };
                    int[] set4value = { 1, 3, 3, 2, 1,  4, 2, 4, 1,  8, 5, 1, 3, 1,  1,  3, 10, 1, 1,  1,  1, 4, 4, 8, 4, 10,  0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set4char, set4value, 165);
                    // using 165 for tilecount instead of 200 because formula cannot calculate 200 Combination
                    break;



                case 4:
                    int[] set5char = { 10, 1, 1, 5, 13, 1, 2, 4, 8, 1, 1, 5, 3,  5, 7, 2, 1,  7, 5, 7, 4, 1, 3, 1, 3, 1,  2 };
                    int[] set5value = { 1, 4, 4, 2, 1,  5, 3, 3, 1, 7, 6, 2, 2,  2, 1, 4, 10, 1, 1, 1, 1, 5, 4, 7, 3, 10, 0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set5char, set5value, 102);
                    break;
                // todo other tile sets

                case 5: // FRENCH FRIENDS
                    int[] set6char = { 12, 2, 2, 3, 13, 2, 2, 2, 7, 1,  1,  4, 2, 6, 7, 2, 1,  6, 6, 6, 4, 1, 1,  1,  1,  1,   3 };
                    int[] set6value = { 1, 5, 4, 3, 1,  5, 5, 5, 1, 10, 10, 2, 4, 1, 1, 4, 10, 1, 1, 1, 2, 8, 10, 10, 10, 8,   0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set6char, set6value, 99);
                    break;
                case 6: // ITALIAN FRIENDS
                    int[] set7char = { 12, 3, 6, 3, 10, 3, 2, 2, 10, 0, 0, 5, 5, 4, 12, 3, 1,  6, 6, 6, 4, 3, 0, 0, 0, 1,   2 };
                    int[] set7value = { 1, 6, 2, 5,  1, 5, 8, 8,  1, 0, 0, 3, 3, 3, 1,  5, 10, 2, 2, 2, 3, 5, 0, 0, 0, 8,   0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set7char, set7value, 109);
                    break;
                case 7: // scarabeo
                    int[] set8char = { 12, 4, 7, 4, 12, 4, 4, 2, 12, 0, 0, 6, 6, 6, 12, 4,  2, 7, 7, 7, 4, 4, 0, 0, 0, 2,   2 };
                    int[] set8value = { 1, 4, 1, 4,  1, 4, 4, 8,  1, 0, 0, 2, 2, 2,  1, 3, 10, 1, 1, 1, 4, 4, 0, 0, 0, 8,   0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set8char, set8value, 130);
                    break;


                // NEED TO ADD TO TABLE IN MASTER DATABASE
                case 8: // italian (ADD TO TABLE)
                    int[] set9char = { 14, 3, 6, 3, 11, 3, 2, 2, 12, 0, 0, 5, 5, 5, 15, 3, 1,  6, 6, 6, 5, 3, 0, 0, 0, 2,  2 };
                    int[] set9value = { 1, 5, 2, 5, 1,  5, 8, 8,  1, 0, 0, 3, 3, 3, 1,  5, 10, 2, 2, 2, 3, 5, 0, 0, 0, 8,  0 };
                    tiles = new Structures.TileSet(set, setName[set - 1], set9char, set9value, 120);
                    break;
                case 0:
                default:
                    int[] set1char =  { 9, 2, 2, 4, 12, 2, 3, 2, 9, 1, 1, 4, 2, 6, 8, 2, 1, 6, 4, 6, 4, 2, 2, 1, 2, 1,  2 };
                    int[] set1value = { 1, 3, 3, 2, 1, 4, 2, 4, 1, 8, 5, 1, 3, 1, 1, 3, 10, 1, 1, 1, 1, 4, 4, 8, 4, 10,  0 };
                    tiles = new Structures.TileSet(1, setName[0], set1char, set1value, 100);
                    break;
                // see http://tiger2002crossword.blogspot.com/p/wordsmith.html
            }
        }
    }
    public static String getTilesetName() {
        return tileset[tiles];
//        return Tiles.setName[tiles + 1];
    }
    public static String[] tileset = {
// todo this is a duplicate of the above; consolidate
            "English Scrabble",
            "English Friends",
            "French Scrabble",
            "Super Scrabble",
            "WordSmith",
            "French Friends",
            "Italian Friends",
            "Scarabeo (Italian)",
            "Italian Scrabble"
    };

    public static String[] languages = {
            "en",
            "fr",
            "es",
            "it"
    };

    public static String[] searchText = {
            "Anagrams", // anagrams,
            "Length", // "Letter Count", //length,
            "Hook Words", //hooks
            "Pattern ⌛",
            "Contains", // extensions, // contains

            "Word Builder ⌛", // subanagrams, // word builder (contains only)
            "Contains All", // superanagrams, // contains all letters
            "Contains Any",
            "Begins With", //begins,
            "Ends With", // ends,

            "Subwords", // subwords,
            "Parallel ⌛",
            "Joins ⌛",
            // add searches here, adjust search type case numbers in Search Activity
            // AND adjust onItemSelectedListener
            // AND adjust collapse
            //Misspells, Transpositions, Stretches, Splitters, Blank Anagrams


            "Stems", // blankanagrams, // stems
            "Predefined",

            "Subject Lists",
            "Takes Prefix",
            "Takes Suffix",
            "Alt Ending ⌛",
            "Replace ⌛",
            "With Definition",
            "From File"
    };

    public static String[] multiPrimarySearchText = {
            "Anagrams", // anagrams,
            "Length", // "Letter Count", //length,
//            "Hook Words", //hooks
            "Pattern ⌛",
            "Contains", // extensions, // contains

            "Word Builder ⌛", // subanagrams, // word builder (contains only)
            "Contains All", // superanagrams, // contains all letters
            "Contains Any",
            "Begins With", //begins,
            "Ends With", // ends,
//
//            "Subwords", // subwords,
//            "Parallel ⌛",
//            "Joins ⌛",
//            // add searches here, adjust search type case numbers in Search Activity
//            // AND adjust onItemSelectedListener
//            // AND adjust collapse
//            //Misspells, Transpositions, Stretches, Splitters, Blank Anagrams
//
//
////            "Stems", // blankanagrams, // stems
            "Predefined",
            "Subject Lists",

//            "Takes Prefix",
//            "Takes Suffix",
//            "Alt Ending ⌛",
//            "Replace ⌛",
            "With Definition",
            "From File"
    };

    public static String[] multiCriteriaSearchText = {
            "Anagrams", // anagrams,

            "Length", // "Letter Count", //length,
            "Hook Words", //hooks
            "Pattern ⌛",
            "Contains", // extensions, // contains
            "Word Builder ⌛", // subanagrams, // word builder (contains only)

            "Contains All", // superanagrams, // contains all letters
            "Contains Any",
            "Begins With", //begins,
            "Ends With", // ends,
//
//            "Subwords", // subwords,
//            "Parallel ⌛",
//            "Joins ⌛",
//            // add searches here, adjust search type case numbers in Search Activity
//            // AND adjust onItemSelectedListener
//            // AND adjust collapse
//            //Misspells, Transpositions, Stretches, Splitters, Blank Anagrams
//
//
////            "Stems", // blankanagrams, // stems
////            "Predefined",
            "Subject Lists",
            "In Lexicon",
            "Predefined",

            "Takes Prefix",
            "Takes Suffix",
            "Alt Ending ⌛",
            "Replace ⌛",
            "With Definition",
            "From File"
    };

    public static String[] cardFilterText = {
            "Anagrams", // anagrams,
            "Length", // "Letter Count", //length,
            "Pattern",
            "Contains", // extensions, // contains

            "Word Build", // superanagrams, // contains all letters
            "Contains Any",
            "Begins With", //begins,
            "Ends With", // ends,
            "From File"
    };


    public static String[] predefText = {
            "2 Letter",
            "Top 3 Letter",
            "3 Letter",
            "Top 4 Letter",

            "Top Fours", //(BE JQXZ)
            "High Fours", //(BE FHKVWY)
            "Top Fives",
            "High Fives",

            "Vowel Heavy", // vowels,
            "Consonant Dump", // novowels,
            "Q not U",
            "Palindromes",
//            "Semordnilaps",
            "Hookless Words",
            "Unique Hooks",
            "Hooked Words"
            ,
//          "Unique Hook Words",
//            "By Probability"
    };
    public static String[] stemsText = {
            "Top 6 Letter",
            "Other 6 letter",
            "Top 7 letter",
            "Other 7 letter"
    };

//            "Semordnilaps",
    public static String[] sortby = {
            "Sort By:",
            "↑Length",
            "↓Length",
            "↑Word",
            "↓Word",
            "↑Score",
            "↓Score",
            "↑Probability",
            "↓Probability",
            "↑Playability",
            "↓Playability",
            "↑Anagrams",
            "↓Anagrams",
        "↑Alphagram",
        "↓Alphagram",
        "⇅Random" //,
//        "↑Unsorted"
    };
    public static String[] thenby = {
            "Then By:",
            "↑Word",
            "↓Word",
            "↑Score",
            "↓Score",
            "↑Probability",
            "↓Probability",
            "↑Playability",
            "↓Playability",
            "↑Anagrams",
            "↓Anagrams",
            "↑Length",
            "↓Length",
            "↑Alphagram",
            "↓Alphagram",
            "⇅Random"
    };

    public static String[] cardtypes = {
            "Anagrams",
            "Hooks",
            "Blank Anagrams",
            "Lists"
    };

    // for lists
    public static String[] quiztypes = {
            "Anagrams",
            "Hooks",
            "Blank Anagrams"
    };

//    public static String[] tilecolors = {
//            "red", "blue", "green", "cyan", "magenta",
//            "yellow", "aqua", "fuchsia", "lime", "maroon",
//            "navy", "olive", "purple", "teal"
//    };
    
}
