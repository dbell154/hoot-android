package com.tylerhosting.hoot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.tylerhosting.hoot.hoot.R;

public class EncryptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt);



    }

    // https://stackoverflow.com/questions/4275311/how-to-encrypt-and-decrypt-file-in-android

//    private void encryptAndSave() {
//        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "your_folder_on_sd", "file_name");
//        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
//        byte[] yourKey = generateKey("password");
//        byte[] filesBytes = encodeFile(yourKey, yourByteArrayContainigDataToEncrypt);
//        bos.write(fileBytes);
//        bos.flush();
//        bos.close();
//
//    }
//
//    private void decryptAndShow() {
//
//        KeyGenerator kgen = KeyGenerator.getInstance("AES");
//
//        SecretKey yourKey = kgen.generateKey("password");
//        byte[] decodedData = decodeFile(yourKey, bytesOfYourFile);
//    }



}