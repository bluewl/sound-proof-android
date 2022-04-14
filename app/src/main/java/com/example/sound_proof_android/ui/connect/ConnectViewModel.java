package com.example.sound_proof_android.ui.connect;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConnectViewModel extends ViewModel {
    private MutableLiveData<String> selectedCode = new MutableLiveData<>();

    public void selectCode(String code) {
        selectedCode.setValue(code);
    }

    public LiveData<String> getSelectedCode() {
        return selectedCode;
    }

}