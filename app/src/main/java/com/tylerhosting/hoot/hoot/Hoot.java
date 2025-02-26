package com.tylerhosting.hoot.hoot;

import android.app.Application;
import android.content.Context;

public class Hoot extends Application {

    protected static Context context;

    public void onCreate() {
        super.onCreate();
        Hoot.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return Hoot.context;
    }
}
