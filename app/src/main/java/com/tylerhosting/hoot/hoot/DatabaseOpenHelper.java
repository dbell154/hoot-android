package com.tylerhosting.hoot.hoot;

import android.content.Context;

//import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class DatabaseOpenHelper extends SQLiteAssetHelper {
    private static final int DATABASE_VERSION = 1;

/*    public DatabaseOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }*/

    public DatabaseOpenHelper(Context context, String databasePath, String HOOT_DB) {
        super(context, databasePath, HOOT_DB, null, DATABASE_VERSION);
    }
}
