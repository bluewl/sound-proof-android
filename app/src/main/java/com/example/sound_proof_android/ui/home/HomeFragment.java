package com.example.sound_proof_android.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sound_proof_android.MainActivity;
import com.example.sound_proof_android.R;
import com.example.sound_proof_android.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.SQLOutput;
import java.util.Calendar;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias("spKey")) {
                // display "pls connect with an account" or something like that
                System.out.println("*** pls");
                System.out.println("*** connect");
                System.out.println("*** with");
                System.out.println("*** an");
                System.out.println("*** account");
            } else {
                // Look for start recording signal (HTTP POST LONG POLLING)
                ((MainActivity)getActivity()).receiveRecordStartSignal();
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return v;
    }

}