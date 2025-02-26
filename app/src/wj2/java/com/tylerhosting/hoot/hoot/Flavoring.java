package com.tylerhosting.hoot.hoot;

import android.content.Context;

public class Flavoring {
    public void Flavoring(){}
    private static String flavor = "aHoot.db3";
    public static void addflavoring(Context context){
        LexData.setDatabase(context, flavor);
    }
    public static void addflavoring(){
        LexData.setDatabase(flavor);
    }
    public static String getflavoring() {
        return flavor;
    }

}
