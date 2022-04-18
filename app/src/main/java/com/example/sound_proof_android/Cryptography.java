package com.example.sound_proof_android;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

    private Context context;
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

    public Cryptography(Context context) {
        this.context = context;
    }

    public String rsaDecrypt(final byte[] encryptedText){
        try{
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey("spKey", null);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,privateKey);
            byte[] decryptedText = cipher.doFinal(encryptedText);
            return new String(decryptedText);
        }catch (UnrecoverableKeyException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e){
            Log.e("decrypt catch", e.getMessage()+"");
            e.printStackTrace();
            return null;
        }

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

            saveWav(decrypted);

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
