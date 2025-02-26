package com.tylerhosting.hoot.hoot;

public class Structures {
    public static class TileSet {
        /// <summary> SetNumber </summary>
        public int SetNumber;
        /// <summary> TileSet Title </summary>
        public String SetName;
        /// <summary> Char Distribution </summary>
        public int[] chardist;
        /// <summary> Char Values </summary>
        public int[] charvalue;
        /// <summary> Tile count </summary>
        public int tilecount;

        /// <summary> Constructor </summary>
        public TileSet()
        {
            SetNumber = 0;
            SetName = "";
            chardist = new int[27];
            charvalue = new int[27];
            tilecount = 0;
        }
        /// <summary> Constructor </summary>
        public TileSet(int ID, String name, int[] dist, int[] values, int total)
        {
            SetNumber = ID;
            SetName = name;
            chardist = dist;
            charvalue = values;
            tilecount = total;
        }
    }
    public static class TileFrequency implements Comparable<TileFrequency> {
        public int letter;
        public int freq;
        public TileFrequency()
        {
            letter = 0;
            freq = 0;
        }
        public TileFrequency(int ltr, int frq)
        {
            letter = ltr;
            freq = frq;
        }
        public int compareTo(TileFrequency other) {
            if(this.getFreq() < other.getFreq()) // using < for descreasing
                return 1;
            else if (this.getFreq() == other.getFreq())
                return 0 ;
            return -1 ;
        }

        public int getFreq() {
            return this.freq ;
        }
    }
    public static class Lexicon {
        /// <summary> LexiconID </summary>
        public int LexiconID;
        /// <summary> Lexicon Name </summary>
        public String LexiconName;
        /// <summary> Lexicon Source </summary>
        public String LexiconSource;
        /// <summary> LexiconStuff </summary>
        public String LexiconStuff;
        /// <summary> LexiconNotice </summary>
        public String LexiconNotice;
        public String LexLanguage;
        /// <summary> Constructor </summary>
        public Lexicon()
        {
            LexiconID = 0;
            LexiconName = "";
            LexiconSource = "";
            LexiconStuff = "";
            LexiconNotice = "";
            LexLanguage = "en";
        }
        public Lexicon(int id, String name, String source, String stuff, String notice, String language)
        {
            LexiconID = id;
            LexiconName = name;
            LexiconSource = source;
            LexiconStuff = stuff;
            LexiconNotice = notice;
            LexLanguage = language;
        }
        public Lexicon(String name, String source, String stuff, String notice, String language)
        {
            LexiconID = 0;
            LexiconName = name;
            LexiconSource = source;
            LexiconStuff = stuff;
            LexiconNotice = notice;
            LexLanguage = language;
        }

    }
    public static class IndexedWord {
        public int WordID;
        public String Word;
        public IndexedWord() {
            WordID = 0;
            Word = "";
        }
        public IndexedWord(int id, String word) {
            WordID = id;
            Word = word;
        }
        public int Index(String word) {
            return WordID;
        }

    }
}