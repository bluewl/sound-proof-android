package com.example.sound_proof_android.ui.connect;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.sound_proof_android.QRCodeActivity;
import com.example.sound_proof_android.R;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ConnectFragment extends Fragment {

    private ConnectViewModel mViewModel;
    private EditText browserText;
    private Button submitButton;
    private Button qrButton;
    private TextView pubKeyText;
    // RSA variables
    private static final String TAG = "RSACryptor";
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private String test = "abcdefghijkhmlopqrstuvwxyz";
    byte[] encrypted;
    String decrypted;
    KeyPairGenerator keyPairGenerator;
    KeyPair kp;

    public static ConnectFragment newInstance() {
        return new ConnectFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_connect, container, false);

        browserText = v.findViewById(R.id.browserText);
        submitButton = v.findViewById(R.id.submitButton);
        qrButton = v.findViewById(R.id.qrButton);
        pubKeyText = v.findViewById(R.id.pubKeyText);

        try {
            displayKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Implement POST REQUEST here

            }
        });

        qrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), QRCodeActivity.class);
                startActivity(intent);
            }
        });
        return v;
    }

    // server should make sure to use PKCS1 padding
    public void createKey(){
        try {
            keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(
                            "spKey",
                            KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                            .setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
                            .setDigests(KeyProperties.DIGEST_SHA256 , KeyProperties.DIGEST_SHA512)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                            .build());
            kp = keyPairGenerator.generateKeyPair();
        }
        catch (Exception e){
            Log.e(TAG, ""+e);
        }
    }

    // displays the public key of the phone
    // if the keystore is empty, both public and private key is created
    public void displayKey() throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias("spKey")) {
            createKey();
        }
        PublicKey publicKey = keyStore.getCertificate("spKey").getPublicKey();

        Log.d(TAG,"publicKey1"+publicKey);
        String pubkey3 = publicKey.getEncoded().toString();
        Log.d(TAG,"publicKey3"+ pubkey3);
        Log.d(TAG,"publicKey2"+Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));
        pubKeyText.setText("Public Key: \n" + Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));
    }

    public String decrypt(final byte[] encryptedText){
        try{
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey("spKey", null);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,privateKey);
            byte[] decryptedText = cipher.doFinal(encryptedText);
            Log.d(TAG, "end decrypt" + decryptedText.toString());
            return new String(decryptedText);
        }catch (UnrecoverableKeyException | IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e){
            Log.e("decrypt catch", e.getMessage()+"");
            String text = new String(encryptedText);
            return text;
        }

    }

    // temp block of code: app does not need to encrypt
    public byte[] encrypt(final String text) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, KeyStoreException, CertificateException, IOException {
        Log.d(TAG, "encrypt test" + text+"");
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        PublicKey publicKey = keyStore.getCertificate("spKey").getPublicKey();
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

        Log.d(TAG,"publicKey1"+publicKey);
        String pubkey3 = publicKey.getEncoded().toString();
        Log.d(TAG,"publicKey3"+ pubkey3);
        Log.d(TAG,"publicKey2"+Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));
        cipher.init(Cipher.ENCRYPT_MODE,publicKey);

        byte[] publicKeyBytes = Base64.encode(publicKey.getEncoded(),0);
        String pubKey = new String(publicKeyBytes);
        Log.d(TAG,"publicKey4444"+ pubkey3);

        byte[] encryptedBytes = cipher.doFinal(text.getBytes("utf-8"));
        Log.d(TAG, "encry"+encryptedBytes.toString());
        return encryptedBytes;
    }

}