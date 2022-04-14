package com.example.sound_proof_android;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

    private Context context;

    public Cryptography(Context context) {
        this.context = context;
    }

    // Decrypting audio file with AES encryption
    public byte[] aesDecrypt(String data, String key, String iv){
        try
        {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] encrypted1 = decoder.decode(data);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());

            cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);

            byte[] decrypted = cipher.doFinal(encrypted1);

            //just for testing, output should be the same as the data string
            String originalString = new String(decrypted);
            System.out.println(originalString.trim());
            //

            return decrypted;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    // example usage: b64Wav(decrypt());
    public void saveWav(byte[] audioB64){

        Record record = new Record(context);
        String fileName = record.getSoundRecordingPath();

        byte[] decoded = Base64.getDecoder().decode(audioB64);

        try
        {
            FileOutputStream os = new FileOutputStream(fileName+"/browseraudio.wav");
            os.write(decoded);
            os.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
