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

import java.util.Calendar;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_record, container, false);

        // Look for start recording signal (HTTP POST LONG POLLING)
        ((MainActivity)getActivity()).receiveRecordStartSignal();

        return v;
    }

}