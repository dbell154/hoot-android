package com.tylerhosting.hoot.hoot;

/*
How to zip
String backupDBPath = Environment.getExternalStorageDirectory().getPath() + "/Mobikul-Pos-db-backup";
final File backupDBFolder = new File(backupDBPath);
        backupDBFolder.mkdirs();
        final File backupDB = new File(backupDBFolder, "/db_pos.db");
String[] s = new String[1];
s[0] = backupDB.getAbsolutePath();
zip(s, backupDBPath + "/pos_demo.zip");
 */

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static android.content.ContentValues.TAG;

/*
How to unzip
 File sd = Environment.getExternalStorageDirectory();
if (sd.canWrite()) {
   final File backupDBFolder = new File(sd.getPath());
   unzip(backupDBPath, backupDBFolder.getPath());
}
 */
public class ZipManager {
    private static int BUFFER_SIZE = 6 * 1024;

    public static void zip(String[] files, String zipFile) throws IOException {
        BufferedInputStream origin = null;
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
        try {
            byte data[] = new byte[BUFFER_SIZE];

            for (int i = 0; i < files.length; i++) {
                FileInputStream fi = new FileInputStream(files[i]);
                origin = new BufferedInputStream(fi, BUFFER_SIZE);
                try {
                    ZipEntry entry = new ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1));
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
                        out.write(data, 0, count);
                    }
                } finally {
                    origin.close();
                }
            }
        } finally {
            out.close();
        }
    }
    public static void unzip(String zipFile, String location) throws IOException {
        try {
            File f = new File(location);
            if (!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + File.separator + ze.getName();

                    if (ze.isDirectory()) {
                        File unzipFile = new File(path);
                        if (!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        FileOutputStream fout = new FileOutputStream(path, false);

                        try {
                            for (int c = zin.read(); c != -1; c = zin.read()) {
                                fout.write(c);
                            }
                            zin.closeEntry();
                        } finally {
                            fout.close();
                        }
                        /* if time should be restored as well
                        long time = ze.getTime();
                        if (time > 0)
                            unzipFile.setLastModified(time);
                        */
                    }
                }
            } finally {
                zin.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Unzip exception", e);
        }
    }}