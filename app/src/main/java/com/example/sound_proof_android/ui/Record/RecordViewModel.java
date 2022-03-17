package com.example.sound_proof_android.ui.Record;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RecordViewModel extends ViewModel {

    private final MutableLiveData<String> mText;


    public RecordViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Android Unique Device ID: \n" + RecordFragment.uniqueID);
    }


    public LiveData<String> getText() {
        return mText;
    }
}