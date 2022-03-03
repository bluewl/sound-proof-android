package com.example.sound_proof_android.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sound_proof_android.databinding.FragmentGalleryBinding;

import android.content.SharedPreferences;
import android.content.SharedPreferences.*;
import android.content.Context;

import java.util.UUID;
import android.provider.Settings;


public class GalleryFragment extends Fragment {

    // Android UID doesn't change on uninstall or reinstall, will change on a factory reset. Can be changed on a rooted phone

    private FragmentGalleryBinding binding;
    public static String uniqueID = null; // NOTE: the unique ID will reset if the user uninstalls the application.


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        uniqueID = getAndroidUniqueDeviceID();


        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }


    // Uses the Android Unique Device ID
    public String getAndroidUniqueDeviceID(){
        return Settings.Secure.getString(getActivity().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    // An alternative to using the Android Unique Device ID
    // Creates a unique ID and stores it in the device's internal storage
    public void uniqueIDGenerator(){
        // retrieve the stored uniqueID
        SharedPreferences sharedPref = getActivity().getSharedPreferences("uniqueID", Context.MODE_PRIVATE);
        uniqueID = sharedPref.getString("uniqueID", null);

        if(uniqueID == null) { // if the uniqueID has not already been generated
            sharedPref = getActivity().getSharedPreferences("uniqueID", Context.MODE_PRIVATE);
            String temp = UUID.randomUUID().toString(); // generates the uniqueID
            Editor editor = sharedPref.edit();
            editor.putString("uniqueID", temp); // stores the uniqueID in the device's internal storage
            editor.commit();
            uniqueID = temp;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}