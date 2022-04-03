package com.example.sound_proof_android.ui.process;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.sound_proof_android.OneThirdOctaveBands;
import com.example.sound_proof_android.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import java.nio.charset.StandardCharsets;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class ProcessFragment extends Fragment {

    private Button processButton;
    int type [] = {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1};
    int numberOfBytes[] = {4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 2, 4, 4};
    int chunkSize, subChunk1Size, sampleRate, byteRate, subChunk2Size=1, bytePerSample;
    short audioFomart, numChannels, blockAlign, bitsPerSample=8;
    String chunkID, format, subChunk1ID, subChunk2ID;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_process, container, false);

        processButton = v.findViewById(R.id.processButton);

        processButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    // testing AES decryption
                    b64Wav(aesDecrypt());

                    // IMPORTANT: Below code is original code to get wav file for mobile audio
                    // ContextWrapper contextWrapper = new ContextWrapper(getActivity().getApplicationContext());
                    // File audioDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                    // String audioFileDirectory = audioDirectory + "/soundproof.wav";
                    // File file = new File(audioFileDirectory);
                    // InputStream fileInputstream = new FileInputStream(file);
                    //

                    // TEST: reading two wav file from res/raw (local) folder to compare
                    // note: sptest1 has around 80ms of lag from mobile audio to browser audio
                    double[] mobileAudioData = readWav(getResources().openRawResource(R.raw.sptest1_mobile_0ms));
                    double[] browserAudioData = readWav(getResources().openRawResource(R.raw.sptest1_browser_plus80ms));
                    int lag = 80;

                    OneThirdOctaveBands third = new OneThirdOctaveBands(sampleRate, OneThirdOctaveBands.FREQUENCY_BANDS.REDUCED, getActivity());
                    // filtered mobile audio signals
                    // should be [24][signal data length]
                    double[][] filteredSignal = third.thirdOctaveFiltering(mobileAudioData);
                    double[][] filteredSignalDup = third.thirdOctaveFiltering(browserAudioData);

                    // normalize function test
                    for (int i = 0; i < 24; i++) {
                        // should print 1 or -1 but not 0
                        System.out.println("normalization mapping of freq range " + i + " is " + normalize(filteredSignal[i], filteredSignalDup[i], lag));
                    }
                    // similarity score test
                    double simScore = similarityScore(filteredSignal, filteredSignalDup, lag, 24);
                    System.out.println("Similary Score is " + simScore);
                    // *** test1 end

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return v;
    }

    public ByteBuffer ByteArrayToNumber(byte bytes[], int numOfBytes, int type){
        ByteBuffer buffer = ByteBuffer.allocate(numOfBytes);
        if (type == 0){
            buffer.order(BIG_ENDIAN); // Check the illustration. If it says little endian, use LITTLE_ENDIAN
        }
        else{
            buffer.order(LITTLE_ENDIAN);
        }
        buffer.put(bytes);
        buffer.rewind();
        return buffer;
    }

    public float convertToFloat(byte[] array, int type) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        if (type == 1){
            buffer.order(LITTLE_ENDIAN);
        }
        return (float) buffer.getShort();
    }

    public double[] readWav(InputStream wavFile) throws IOException {

        InputStream fileInputstream = wavFile;
        ByteBuffer byteBuffer;
        for(int i=0; i<numberOfBytes.length; i++){
            byte byteArray[] = new byte[numberOfBytes[i]];
            int r = fileInputstream.read(byteArray, 0, numberOfBytes[i]);
            byteBuffer = ByteArrayToNumber(byteArray, numberOfBytes[i], type[i]);
            if (i == 0) {chunkID =  new String(byteArray); System.out.println("chunkID " + chunkID);}
            if (i == 1) {chunkSize = byteBuffer.getInt(); System.out.println("chunkSize " + chunkSize);}
            if (i == 2) {format =  new String(byteArray); System.out.println("format " + format);}
            if (i == 3) {subChunk1ID = new String(byteArray);System.out.println("subChunk1ID " + subChunk1ID);}
            if (i == 4) {subChunk1Size = byteBuffer.getInt(); System.out.println("subChunk1Size " + subChunk1Size);}
            if (i == 5) {audioFomart = byteBuffer.getShort(); System.out.println("audioFomart " + audioFomart);}
            if (i == 6) {numChannels = byteBuffer.getShort();System.out.println("numChannels " + numChannels);}
            if (i == 7) {sampleRate = byteBuffer.getInt();System.out.println("sampleRate " + sampleRate);}
            if (i == 8) {byteRate = byteBuffer.getInt();System.out.println("byteRate " + byteRate);}
            if (i == 9) {blockAlign = byteBuffer.getShort();System.out.println("blockAlign " + blockAlign);}
            if (i == 10) {bitsPerSample = byteBuffer.getShort();System.out.println("bitsPerSample " + bitsPerSample);}
            if (i == 11) {
                subChunk2ID = new String(byteArray) ;
                if(subChunk2ID.compareTo("data") == 0) {
                    continue;
                }
                else if( subChunk2ID.compareTo("LIST") == 0) {
                    byte byteArray2[] = new byte[4];
                    r = fileInputstream.read(byteArray2, 0, 4);
                    byteBuffer = ByteArrayToNumber(byteArray2, 4, 1);
                    int temp = byteBuffer.getInt();
                    //redundant data reading
                    byte byteArray3[] = new byte[temp];
                    r = fileInputstream.read(byteArray3, 0, temp);
                    r = fileInputstream.read(byteArray2, 0, 4);
                    subChunk2ID = new String(byteArray2) ;
                }
            }
            if (i == 12) {subChunk2Size = byteBuffer.getInt();System.out.println("subChunk2Size " + subChunk2Size);}
        }
        bytePerSample = bitsPerSample/8;
        float value;
        ArrayList<Float> dataVector = new ArrayList<>();
        while (true){
            byte byteArray[] = new byte[bytePerSample];
            int v = fileInputstream.read(byteArray, 0, bytePerSample);
            value = convertToFloat(byteArray,1);
            dataVector.add(value);
            if (v == -1) break;
        }
        double[] data = new double[dataVector.size()];
        for(int i=0;i<dataVector.size();i++){
            data[i] = dataVector.get(i);
        }
        System.out.println("******** signal length ********");
        System.out.println(data.length);
        return data;
    }

    // Used to calculate the cross correlation between two signals
    public double crossCorrelation(double[] x, double[] y, int l, int n){
        double sumCorrelation = 0; // used to keep track of the sum amount for the correlation
        // loop through n bands (24) for each signal
        for (int i = 0; i < n; i++) {
            if(i < 0 || i > n-1){ // where y(i) = 0 if i < 0 or i > n − 1.
                y[i] = 0;
            }
            sumCorrelation += x[i] * y[i - l];
        }
        return sumCorrelation;
    }

    // Used to calculate and return lag time in ms
    public int lagCalc(long browserNtpStartTime, long mobileNtpStartTime){
        return (int)(mobileNtpStartTime - browserNtpStartTime);
    }

    // Used to normalize the cross correlation of two signals
    // returning 1 indicates the two signals have the same shape,
    // returning -1 indicates the two signals have the same shape but opposite signs,
    // returning 0 indicates the two signals are uncorrelated
    public int normalize(double[] x, double[] y, int l){
        return (int)(crossCorrelation(x, y, l, x.length) / Math.sqrt((crossCorrelation(x, x, 0, x.length) * crossCorrelation(y, y, 0, y.length))) );
    }

    // Used to compute the similarity score by determining the average
    // from the max cross-correlation across signals x[i] and y[i].
    public double similarityScore(double[][] x, double[][] y, int l, int n){
        double runningSum = 0;
        for (int i = 0; i < n; i++) {
            double temp = normalize(x[i], y[i], l);
            System.out.println(" ** normalization for array " + i + " is " + temp);
            runningSum += temp;
        }
        return (runningSum / n);
    }

    // Decrypting audio file with AES encryption
    public byte[] aesDecrypt(){
        try
        {
            String data = "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";
            String key = "12345678123456781234567812345678";
            String iv = "1234567812345678";

            Decoder decoder = Base64.getDecoder();
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
    private static void b64Wav(byte[] audioB64){
        byte[] decoded = Base64.getDecoder().decode(audioB64);

        try
        {
            System.out.println("***** TEST: DECRYPTING AES");
            File audiofile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"decrypedaudio.wav");
            FileOutputStream os = new FileOutputStream(audiofile);
            os.write(decoded);
            os.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}