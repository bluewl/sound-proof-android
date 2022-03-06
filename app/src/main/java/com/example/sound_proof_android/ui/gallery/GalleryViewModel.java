package com.example.sound_proof_android.ui.gallery;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


import java.util.UUID;

public class GalleryViewModel extends ViewModel {

    private final MutableLiveData<String> mText;


    public GalleryViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Android Unique Device ID: \n" + GalleryFragment.uniqueID);
    }


    public LiveData<String> getText() {
        return mText;
    }
}