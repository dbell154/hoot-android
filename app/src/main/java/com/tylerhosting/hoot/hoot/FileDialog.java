package com.tylerhosting.hoot.hoot;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

//https://stackoverflow.com/questions/3592717/choose-file-dialog
//public class OldFileDialog {
//    private static final String INTERNAL_DB = "Internal";
//    private static final String PARENT_DIR = "..";
//    private final String TAG = getClass().getName();
//    private String[] fileList;
//    private File currentPath;
//    public interface FileSelectedListener {
//        void fileSelected(File file);
//    }
//    public interface DirectorySelectedListener {
//        void directorySelected(File directory);
//    }
//    private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileDialog.FileSelectedListener>();
//    private ListenerList<DirectorySelectedListener> dirListenerList = new ListenerList<FileDialog.DirectorySelectedListener>();
//    private Activity activity;
//    private boolean selectDirectoryOption;
//    private String fileEndsWith;
//
//    public FileDialog(Activity activity, File initialPath, String fileEndsWith) {
//        this.activity = activity;
//        setFileEndsWith(fileEndsWith);
//        if (!initialPath.exists()) initialPath = Environment.getExternalStorageDirectory();
//        loadFileList(initialPath);
//    }
//
//    /**
//     * @return file dialog
//     */
//    public Dialog createFileDialog() {
//        Dialog dialog;
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.LightTheme);
//
//        builder.setTitle(currentPath.getPath());
//        if (selectDirectoryOption) {
//            builder.setPositiveButton("Select directory", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    Log.d(TAG, currentPath.getPath());
//                    fireDirectorySelectedEvent(currentPath);
//                }
//            });
//        }
//
//        builder.setItems(fileList, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//                String fileChosen = fileList[which];
//                File chosenFile = getChosenFile(fileChosen);
//                if (chosenFile.isDirectory()) {
//                    loadFileList(chosenFile);
//                    dialog.cancel();
//                    dialog.dismiss();
//                    showDialog();
//                } else fireFileSelectedEvent(chosenFile);
//            }
//        });
//
//        builder.setCancelable(false);
///*                builder.setPositiveButton(R.string.yes,new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog,int id) {
//                // if this button is clicked, close
//                // current activity
//                MainActivity.this.finish();
//            }
//        });*/
//        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog,int id) {
//                // if this button is clicked, just close
//                // the dialog box and do nothing
//                dialog.cancel();
//            }
//        });
//
//        dialog = builder.show();
//        return dialog;
//    }
//
//
//    public void addFileListener(FileSelectedListener listener) {
//        fileListenerList.add(listener);
//    }
//
//    public void removeFileListener(FileSelectedListener listener) {
//        fileListenerList.remove(listener);
//    }
//
//    public void setSelectDirectoryOption(boolean selectDirectoryOption) {
//        this.selectDirectoryOption = selectDirectoryOption;
//    }
//
//    public void addDirectoryListener(DirectorySelectedListener listener) {
//        dirListenerList.add(listener);
//    }
//
//    public void removeDirectoryListener(DirectorySelectedListener listener) {
//        dirListenerList.remove(listener);
//    }
//
//
//
//    /**
//     * Show file dialog
//     */
//    public void showDialog() {
//        createFileDialog().show();
//    }
//
//    private void fireFileSelectedEvent(final File file) {
//        fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
//            public void fireEvent(FileSelectedListener listener) {
//                listener.fileSelected(file);
//            }
//        });
//    }
//
//    private void fireDirectorySelectedEvent(final File directory) {
//        dirListenerList.fireEvent(new ListenerList.FireHandler<DirectorySelectedListener>() {
//            public void fireEvent(DirectorySelectedListener listener) {
//                listener.directorySelected(directory);
//            }
//        });
//    }
//
//    private void loadFileList(File path) {
//        this.currentPath = path;
//        List<String> r = new ArrayList<String>();
//        if (path.exists()) {
//            if (fileEndsWith.equals("db3"))
//                r.add(INTERNAL_DB);
//
//            if (path.getParentFile() != null) r.add(PARENT_DIR);
//            FilenameFilter filter = new FilenameFilter() {
//                public boolean accept(File dir, String filename) {
//                    File sel = new File(dir, filename);
//                    if (!sel.canRead()) return false;
//                    if (selectDirectoryOption) return sel.isDirectory();
//                    else {
//                        boolean endsWith = fileEndsWith != null ? filename.toLowerCase().endsWith(fileEndsWith) : true;
//                        return endsWith || sel.isDirectory();
//                    }
//                }
//
//                /*
//                public boolean accept(File dir, String filename) {
//                    File sel = new File(dir, filename);
//                    return filename.contains(fileEndsWith) || sel.isDirectory();
//                }
//                */
//
//            };
//            String[]
//                    fileList1 = path.list(filter);
//            if (!(fileList1 == null))
//                for (String file : fileList1) {
//                    r.add(file);
//                }
//        }
//        fileList = (String[]) r.toArray(new String[]{});
//    }
//
//    private File getChosenFile(String fileChosen) {
//        if (fileChosen.equals(INTERNAL_DB)) {
//            Flavoring.addflavoring(); // sets database
//            return new File(LexData.internalFilesDir  + File.separator + LexData.getDatabase());
//        }
//        else if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
//        else return new File(currentPath, fileChosen);
//    }
//
//    private void setFileEndsWith(String fileEndsWith) {
//        this.fileEndsWith = fileEndsWith != null ? fileEndsWith.toLowerCase() : fileEndsWith;
//    }
//}

public class FileDialog {
    //private static final String INTERNAL_DB = "Internal";
private static String storage = "Internal";
    /// todo make this a variable Internal/External

    private static final String PARENT_DIR = "..";
    private final String TAG = getClass().getName();
    private String[] fileList;
    private File currentPath;
    public interface FileSelectedListener {
        void fileSelected(File file);
    }
    public interface DirectorySelectedListener {
        void directorySelected(File directory);
    }
    private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileSelectedListener>();
    private ListenerList<DirectorySelectedListener> dirListenerList = new ListenerList<DirectorySelectedListener>();
    private Activity activity;
    private boolean selectDirectoryOption;
    private String fileEndsWith;

    public FileDialog(Activity activity, File initialPath, String fileEndsWith) {
        this.activity = activity;
        setFileEndsWith(fileEndsWith);

        String internalDBPath = "";
        String internalFilesPath = "";
        // overrides initialPath
//        if (Utils.usingSAF()) {

//            if (fileEndsWith.equals("db3")) {
                // for databases
                internalDBPath = activity.getFilesDir().getAbsolutePath().replace("files", "databases");
                File internal = new File(internalDBPath);
                initialPath = internal;
                storage = "External";
//            } else {
//                /// for other files
//                internalFilesPath = activity.getFilesDir().getAbsolutePath().replace("files","") ;
//                File internal = new File(internalFilesPath);
//                initialPath = internal;
//                storage = "External";
//            }

//        } else {
//            if (!initialPath.exists())
//                initialPath = Environment.getExternalStorageDirectory();
//            storage = "Internal";
//        }

        Log.d("internalDBPath",internalDBPath );
        Log.d("internalFilesPath", internalFilesPath);

        loadFileList(initialPath, fileEndsWith);
    }

    /**
     * @return file dialog
     */
    public Dialog createFileDialog() {
        Dialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.LightTheme);
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.DarkTheme);
/// todo use DarkTheme
Log.d("import", "createFileDialog");

        builder.setTitle(currentPath.getPath());
        if (selectDirectoryOption) {
            builder.setPositiveButton("Select directory", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
Log.d(TAG, currentPath.getPath());
                    fireDirectorySelectedEvent(currentPath);
                }
            });
        }
Log.d("import", "setTitle");

        builder.setItems(fileList, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String fileChosen = fileList[which];
                File chosenFile = getChosenFile(fileChosen);
                if (chosenFile.isDirectory()) {
                    loadFileList(chosenFile, fileEndsWith);
                    dialog.cancel();
                    dialog.dismiss();
                    showDialog();
                } else fireFileSelectedEvent(chosenFile);
            }
        });
Log.d("import", "setItems");

        builder.setCancelable(false);
/*                builder.setPositiveButton(R.string.yes,new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                // if this button is clicked, close
                // current activity
                MainActivity.this.finish();
            }
        });*/
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                // if this button is clicked, just close
                // the dialog box and do nothing
                dialog.cancel();
            }
        });

/// todo         process back button here?
Log.d("import", "before show");

        dialog = builder.show();
        return dialog;
    }


    public void addFileListener(FileSelectedListener listener) {
        fileListenerList.add(listener);
    }

    public void removeFileListener(FileSelectedListener listener) {
        fileListenerList.remove(listener);
    }

    public void setSelectDirectoryOption(boolean selectDirectoryOption) {
        this.selectDirectoryOption = selectDirectoryOption;
    }

    public void addDirectoryListener(DirectorySelectedListener listener) {
        dirListenerList.add(listener);
    }

    public void removeDirectoryListener(DirectorySelectedListener listener) {
        dirListenerList.remove(listener);
    }



    /**
     * Show file dialog
     */
    public void showDialog() {
        createFileDialog().show();
    }

    private void fireFileSelectedEvent(final File file) {
        fileListenerList.fireEvent(new ListenerList.FireHandler<FileSelectedListener>() {
            public void fireEvent(FileSelectedListener listener) {
                listener.fileSelected(file);
            }
        });
    }

    private void fireDirectorySelectedEvent(final File directory) {
        dirListenerList.fireEvent(new ListenerList.FireHandler<DirectorySelectedListener>() {
            public void fireEvent(DirectorySelectedListener listener) {
                listener.directorySelected(directory);
            }
        });
    }

    private void loadFileList(File path, String ext) {
        this.currentPath = path;

        ///  todo
//        ?? change currentPath
Log.d("import", "loadFileList");

        List<String> r = new ArrayList<String>();
        if (path.exists()) {
            if (fileEndsWith.equals(ext))
//                if (fileEndsWith.equals("db3"))
                r.add(storage);

            if (path.getParentFile() != null) r.add(PARENT_DIR);
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    if (!sel.canRead()) return false;

                    if (selectDirectoryOption) {
                        //(if in restricted folders, return false.)

                        return sel.isDirectory();
                    }
                    else {
                        //boolean endsWith = fileEndsWith == null || filename.toLowerCase().endsWith(fileEndsWith);

// Before
//                        boolean endsWith = fileEndsWith != null ? filename.toLowerCase().endsWith(fileEndsWith) : true;
                        boolean endsWith = fileEndsWith != null ?
                                filename.toLowerCase().endsWith(fileEndsWith) || fileEndsWith.equals("*") : true;
                        return endsWith || sel.isDirectory();
                    }
                }

                /*
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return filename.contains(fileEndsWith) || sel.isDirectory();
                }
                */

            };
            String[]
                    fileList1 = path.list(filter);
            if (!(fileList1 == null))
                for (String file : fileList1) {
                    r.add(file);
                }
        }
        fileList = (String[]) r.toArray(new String[]{});
    }

    private File getChosenFile(String fileChosen) {
        if (fileChosen.equals(storage)) {
Log.d("import", "getChosenFile");
Log.d("import", fileChosen);

/// todo
//            if (!Utils.usingLegacy()) {


                    if (storage.equals("Internal")) {
                        storage = "External";
                        if (fileEndsWith.equals("db3"))
                            fileChosen = activity.getFilesDir().getAbsolutePath().replace("files", "databases");
                        else
                            fileChosen = activity.getFilesDir().getAbsolutePath().replace("files", "");
                    }
                    else {
                        storage = "Internal";
                        fileChosen = Environment.getExternalStorageDirectory() + "//Documents//";
                     }

Log.d("import", fileChosen);


// Before
//            if (fileEndsWith.equals("db3")) {
//                if (storage.equals("Internal")) {
//                    storage = "External";
//                    fileChosen = activity.getFilesDir().getAbsolutePath().replace("files", "databases");
//                }
//                else {
//                    storage = "Internal";
//                    fileChosen = Environment.getExternalStorageDirectory() + "//Documents//";
//                }
//                Log.d("import", fileChosen);
//
//            }
//            else {
//                if (storage.equals("Internal")) {
//                    storage = "External";
//                    fileChosen = activity.getFilesDir().getAbsolutePath();
//                }
//                else {
//                    storage = "Internal";
//                    fileChosen = Environment.getExternalStorageDirectory() + "//Documents//";
//                }
//
//            }

                return new File(fileChosen);

//            } else {
//                Flavoring.addflavoring(); // sets database
//                return new File(LexData.internalFilesDir  + File.separator + LexData.getDatabase());
//            }

        }
        else if (fileChosen.equals(PARENT_DIR)) return currentPath.getParentFile();
        else return new File(currentPath, fileChosen);
    }

    private void setFileEndsWith(String fileEndsWith) {
        this.fileEndsWith = fileEndsWith != null ? fileEndsWith.toLowerCase() : fileEndsWith;
    }
}


class ListenerList<L> {
    private List<L> listenerList = new ArrayList<L>();

    public interface FireHandler<L> {
        void fireEvent(L listener);
    }

    public void add(L listener) {
        listenerList.add(listener);
    }

    public void fireEvent(FireHandler<L> fireHandler) {
        List<L> copy = new ArrayList<L>(listenerList);
        for (L l : copy) {
            fireHandler.fireEvent(l);
        }
    }

    public void remove(L listener) {
        listenerList.remove(listener);
    }

    public List<L> getListenerList() {
        return listenerList;
    }
}