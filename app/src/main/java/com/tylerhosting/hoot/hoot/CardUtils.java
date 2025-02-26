package com.tylerhosting.hoot.hoot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static com.tylerhosting.hoot.hoot.Hoot.context;
import static com.tylerhosting.hoot.hoot.Hoot.getAppContext;
import static com.tylerhosting.hoot.hoot.Utils.setNewTheme;
import static com.tylerhosting.hoot.hoot.Utils.usingLegacy;

public class CardUtils
    {


        // Cardbox Utilities methods

        public static boolean createCardFolder() {
            // create folders
            File full = new File(LexData.getCardfile()); // includes database name
            File directory = new File(full.getParent()); // path only

            if (directory.exists())
                return true;

            boolean success = directory.mkdirs();
            if (!success) {
                Log.e("mkdirs", "failed to mkdirs");
                return false;
            }
            return true;
        }


        public enum boxmove {
            back,
            forward,
            zero
        };

        // gets just the path to the program
        public static String programData(String program) {

            if (!usingLegacy()) {
                LexData.setCardfile("Internal");
                SharedPreferences shared;
                SharedPreferences.Editor prefs;

                shared = PreferenceManager.getDefaultSharedPreferences(context);

                prefs = shared.edit();
                prefs.putString("cardlocaiton", "Internal");
                prefs.apply();
                LexData.setCardfile("Internal");
                Toast.makeText(getAppContext(), "Cards set to Internal for Storage Access Framework", Toast.LENGTH_LONG);
            }


            String folder = "";
            String BasePath;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
                BasePath = Environment.getExternalStoragePublicDirectory("Documents").toString();
            else
                BasePath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).toString();
//            String CardPath


            folder = BasePath + "/Cards";


            if (LexData.getCardslocation().equals("Internal")) {
                folder = LexData.internalFilesDir + "/Cards";
                String folder2 = Environment.getDataDirectory().toString();
                Log.d("SAF", folder);
                Log.d("SAF", folder2);
            }
/*            switch (program)
folder = Environment.
            {
                case "Zyzzyva":
                    folder =
                            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile) + "\\Zyzzyva\\quiz\\data\\";
                    break;
                case "Collins Zyzzyva":
                    folder =
                            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile) + "\\.collinszyzzyva\\quiz\\data\\";
                    break;
                case "Hoot":
                    folder = Properties.Settings.Default.usrFolder + "Cards\\";
                    break;
            }
            */
            return folder;
        }

        // gets the path to the designated lexicon
        public static String lexiconData(String program, String lexicon) {
            return (programData(program) + lexicon + "\\");
        }

        // gets the path to the database to open
        public static String dbSource(LexData.Cardbox cb) {
            return programData(cb.program) + cb.lexicon + "\\" + cb.boxtype + ".db";
        }

        // temporary
        public static int lastbox(int current) {
            // routine to determine where to move missed questions
            return current / 2;
        }

        public static int boxDays(int card) {
            switch (card)
            {
                case 0: return(1);
                case 1: return(4);
                case 2: return(7);
                case 3: return(12);
                case 4: return(20);
                case 5: return(30);
                case 6: return(60);
                case 7: return(90);
                case 8: return(150);
                case 9: return(270);
                default: return(480);
            }
        }

        public static Date today() {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); // same for minutes and seconds
            cal.add(Calendar.DATE, 1);

            return cal.getTime();
        }

        public static int nextDate(int card) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); // same for minutes and seconds
            cal.add(Calendar.DATE, 1);

            Date today = cal.getTime();
            Date nextday;

            //DateTime today = DateTime.Now;
            //DateTime nextday = new DateTime();
            switch (card)
            {
                case 0: nextday = Utils.DateUtils.addDays(today,1); break;
                case 1: nextday = Utils.DateUtils.addDays(today,4); break;
                case 2: nextday = Utils.DateUtils.addDays(today,7); break;
                case 3: nextday = Utils.DateUtils.addDays(today,12); break;
                case 4: nextday = Utils.DateUtils.addDays(today,20); break;
                case 5: nextday = Utils.DateUtils.addDays(today,30); break;
                case 6: nextday = Utils.DateUtils.addDays(today,60); break;
                case 7: nextday = Utils.DateUtils.addDays(today,90); break;
                case 8: nextday = Utils.DateUtils.addDays(today,150); break;
                case 9: nextday = Utils.DateUtils.addDays(today,270); break;
                default: nextday = Utils.DateUtils.addDays(today,480); break;

/*                case 0: nextday = today.AddDays(1); break;
                case 1: nextday = today.AddDays(4); break;
                case 2: nextday = today.AddDays(7); break;
                case 3: nextday = today.AddDays(12); break;
                case 4: nextday = today.AddDays(20); break;
                case 5: nextday = today.AddDays(30); break;
                case 6: nextday = today.AddDays(60); break;
                case 7: nextday = today.AddDays(90); break;
                case 8: nextday = today.AddDays(150); break;
                case 9: nextday = today.AddDays(270); break;
                default: nextday = today.AddDays(480); break;*/
            }
            return (int)unixDate(nextday);
        }

        // Date to unix
        public static int unixDate(Date dt)
        {
            return (int)(dt.getTime() / 1000L);
        }

        // unix to Date
        public static Date dtDate(int unix) {
            unix = (unix / 86400) * 86400;
            Date conversion =new Date((long)unix*1000);
            return conversion;
        }

        public static String dtDateStr(int unix) {

            return "";
// see CardQuizActivity dateformat
        }


        public static String dueDays(Date dt) {
            long span = dt.getTime() - CardUtils.today().getTime();
            long days = TimeUnit.DAYS.convert(span, TimeUnit.MILLISECONDS);
            int dayspan = (int) days;

            if (dayspan == 0)
                return "Today";
            if (dayspan == 1)
                return "Tomorrow";
            if (dayspan == -1)
                return "Yesterday";
            String dayString = "";
            if (dayspan > 1)
            {
                if (span < 280)
                    dayString = String.format(Locale.US, "in %d days (%tD)", dayspan, dt);
                else
                    dayString = String.format(Locale.US, "in %d days (%tD)", dayspan, dt);
                //dayString = String.Format("in {0} days ({1})", dayspan.ToString(), dt.ToString("MM.dd.yyyy", new CultureInfo("en-us")));
            }
            if (dayspan < -1)
            {
                if (span > -280)
                    dayString = String.format("%d days ago (%tD)", (-dayspan), dt);
                else
                    dayString = String.format("%d days ago (%tD)", (-dayspan), dt);
                //dayString = String.Format("{0} days ago({ 1})", (-dayspan).ToString(), dt.ToString("MM.dd.yyyy", new CultureInfo("en - us")));
            }
            return dayString;
        }

        // Zyzzyva databases

}
