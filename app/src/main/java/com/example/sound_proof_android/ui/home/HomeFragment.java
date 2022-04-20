package com.example.sound_proof_android.ui.home;

import android.graphics.Color;
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

    public TextView currentActionText;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        currentActionText = v.findViewById(R.id.currentActionText);
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias("spKey")) {
                currentActionText.setText("Please connect with an account");
                currentActionText.setTextColor(Color.RED);
            } else {
                ((MainActivity)getActivity()).setCurrentActionText(currentActionText);
                // Look for start recording signal (HTTP POST LONG POLLING)
                currentActionText.setText("Waiting for start record signal...");
                currentActionText.setTextColor(Color.YELLOW);
                if (!((MainActivity) getActivity()).getInProcessStatus()) {
                    ((MainActivity)getActivity()).receiveRecordStartSignal();
                }
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