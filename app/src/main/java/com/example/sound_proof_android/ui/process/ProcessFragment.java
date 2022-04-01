package com.example.sound_proof_android.ui.process;

import android.content.ContextWrapper;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
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
                    double[] data = readWav();
                    OneThirdOctaveBands third = new OneThirdOctaveBands(sampleRate, OneThirdOctaveBands.FREQUENCY_BANDS.REDUCED, getActivity());
                    // filtered mobile audio signals
                    // should be [24][signal data length]
                    double[][] filteredSignal = third.thirdOctaveFiltering(data);

                    // *** test1: duplicating mobile audio to test cross-correlation
                    double[][] filteredSignalDup = filteredSignal.clone();

                    // assuming lag is 0ms
                    // normalize function test
                    for (int i = 0; i < 24; i++) {
                        // should print 1 or -1 but not 0
                        System.out.println("normalization mapping of freq range " + i + " is " + normalize(filteredSignal[i], filteredSignalDup[i], 0));
                    }
                    // similarity score test
                    double simScore = similarityScore(filteredSignal, filteredSignalDup, 0, 24);
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

    public double[] readWav() throws IOException {
        ContextWrapper contextWrapper = new ContextWrapper(getActivity().getApplicationContext());
        File audioDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        String audioFileDirectory = audioDirectory + "/soundproof.wav";
        System.out.println("directory " + audioFileDirectory);
        File file = new File(audioFileDirectory);
        InputStream fileInputstream = new FileInputStream(file);
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

    public float crossCorrelation(double[] x, double[] y, int l, int n){
        float sumCorrelation = 0; // used to keep track of the sum amount for the correlation
        // loop through n bands for each signal
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
            runningSum += normalize(x[i], y[i], l);
        }
        return (1.0f / n) * runningSum;
    }

}