package com.example.sound_proof_android;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

import android.content.Context;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class SoundProcess {

    private Context context;

    int type [] = {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 1};
    int numberOfBytes[] = {4, 4, 4, 4, 4, 2, 2, 4, 4, 2, 2, 4, 4};
    double simThreshold = 0.13;
    AudioSignal mobileAudioObj;
    AudioSignal browserAudioObj;
    int lag;

    // order = 0: mobile on first parameter & browser on second parameter
    // order = 1: browser on first parameter & mobile on second parameter
    int order;

    public SoundProcess(Context context, long mobileStopTime, long browserStopTime) throws IOException {
        this.context = context;

        lag = (int) Math.abs((int) Math.abs(mobileStopTime-browserStopTime) - 3000);

        // TEST
        System.out.println("mobile: " + mobileStopTime);
        System.out.println("browser: " + browserStopTime);
        System.out.println("lag: " + lag);
        //

        Record record = new Record(context);
        String fileName = record.getSoundRecordingPath();
        InputStream mobileAudioWav = new FileInputStream(fileName+"/soundproof.wav");
        InputStream browserAudioWav = new FileInputStream(fileName+"/browseraudio.wav");
        mobileAudioObj = readWav(mobileAudioWav);
        browserAudioObj = readWav(browserAudioWav);
        if (mobileStopTime > browserStopTime) {
            order = 0;
        } else {
            order = 1;
        }
        mobileAudioWav.close();
        browserAudioWav.close();
    }

    public boolean startProcess() {
        // get both audio signals
        double[] mobileAudioData = mobileAudioObj.getAudioData();
        double[] browserAudioData = browserAudioObj.getAudioData();

        // filter them which both ends with [24][# of signal]
        OneThirdOctaveBands third = new OneThirdOctaveBands(mobileAudioObj.getSampleRate(), OneThirdOctaveBands.FREQUENCY_BANDS.REDUCED, context);
        double[][] filteredMobileSignal = third.thirdOctaveFiltering(mobileAudioData);
        double[][] filteredBrowserSignal = third.thirdOctaveFiltering(browserAudioData);

        double simScore = -1;
        if (order == 0) {
            simScore = similarityScore(filteredMobileSignal, filteredBrowserSignal, lag);
        } else if (order == 1) {
            simScore = similarityScore(filteredBrowserSignal, filteredMobileSignal, lag);
        }
        System.out.println("Similary Score is " + simScore);

        if(simScore > simThreshold){
            Toast.makeText(context, "Lag: " + lag + "\nSimilarity Score: " + simScore, Toast.LENGTH_LONG).show();
            System.out.println("Login Accepted - Similarity score passed.");
            return true;
        } else {
            Toast.makeText(context, "Lag: " + lag + "\nSimilarity Score: " + simScore, Toast.LENGTH_LONG).show();
            System.out.println("Login Rejected - Similarity score failed.");
            return false;
        }
    }

    // Used to compute the similarity score by determining the average
    // from the max cross-correlation across signals x[i] and y[i].
    public double similarityScore(double[][] x, double[][] y, int l){
        if(l > 700) return 0.0; // reject the audio if there is a lag greater than 300ms

        System.out.println("************ ENTERING SIMILARITY SCORE TESTING");
        System.out.println(x.length);
        System.out.println("************ ENTERING SIMILARITY SCORE TESTING");
        double runningSum = 0;
        for (int i = 0; i < x.length; i++) {
            double maxCrossCorr = maxCrossCorrelation(x[i], y[i], l);
            System.out.println(" ** Max Cross-Correlation for array " + (i + 1) + " is " + maxCrossCorr);
            runningSum += maxCrossCorr;
        }
        return (runningSum / x.length);
    }

    public float maxCrossCorrelation(double[] x, double[] y, int l) {
        float max = normalize(x,y,0);
        int init = l - 200;
        if (init < 0) { init = 0; }
        for (int i = init; i < l+200; i++) {
            max = max(normalize(x,y,i), max);
        }
        return max;
    }

    // Used to normalize the cross correlation of two signals
    // returning 1 indicates the two signals have the same shape,
    // returning -1 indicates the two signals have the same shape but opposite signs,
    // returning 0 indicates the two signals are uncorrelated
    public float normalize(double[] x, double[] y, int l){
        return (float)(crossCorrelation(x, y, l) / Math.sqrt((crossCorrelation(x, x, 0) * crossCorrelation(y, y, 0))) );
    }

    // lag = 80ms - 100m
    // Used to calculate the cross correlation between two signals
    public double crossCorrelation(double[] x, double[] y, int l){
        double sumCorrelation = 0; // used to keep track of the sum amount for the correlation
        int smallerLength = min(x.length, y.length);
        for (int i = l*48; i < smallerLength; i++) {
            sumCorrelation += x[i] * y[i - l*48];
        }
        return sumCorrelation;
    }

    // Used to calculate and return lag time in ms
    public int lagCalc(long firstStartTime, long secondStartTime){
        return (int)(firstStartTime - secondStartTime);
    }

    public AudioSignal readWav(InputStream wavFile) throws IOException {

        AudioSignal audioSignal = new AudioSignal();

        InputStream fileInputstream = wavFile;
        ByteBuffer byteBuffer;
        for(int i=0; i<numberOfBytes.length; i++){
            byte byteArray[] = new byte[numberOfBytes[i]];
            int r = fileInputstream.read(byteArray, 0, numberOfBytes[i]);
            byteBuffer = ByteArrayToNumber(byteArray, numberOfBytes[i], type[i]);
            if (i == 0) {audioSignal.setChunkID(new String(byteArray));System.out.println("chunkID " + audioSignal.getChunkID()); }
            if (i == 1) {audioSignal.setChunkSize(byteBuffer.getInt()); System.out.println("chunkSize " + audioSignal.getChunkSize());}
            if (i == 2) {audioSignal.setFormat(new String(byteArray)); System.out.println("format " + audioSignal.getFormat());}
            if (i == 3) {audioSignal.setSubChunk1ID(new String(byteArray)); System.out.println("subChunk1ID " + audioSignal.getSubChunk1ID());}
            if (i == 4) {audioSignal.setSubChunk1Size(byteBuffer.getInt()); System.out.println("subChunk1Size " + audioSignal.subChunk1Size);}
            if (i == 5) {audioSignal.setAudioFomart(byteBuffer.getShort()); System.out.println("audioFomart " + audioSignal.getAudioFomart());}
            if (i == 6) {audioSignal.setNumChannels(byteBuffer.getShort()); System.out.println("numChannels " + audioSignal.numChannels);}
            if (i == 7) {audioSignal.setSampleRate(byteBuffer.getInt()); System.out.println("sampleRate " + audioSignal.getSampleRate());}
            if (i == 8) {audioSignal.setByteRate(byteBuffer.getInt()); System.out.println("byteRate " + audioSignal.byteRate);}
            if (i == 9) {audioSignal.setBlockAlign(byteBuffer.getShort()); System.out.println("blockAlign " + audioSignal.getBlockAlign());}
            if (i == 10) {audioSignal.setBitsPerSample(byteBuffer.getShort()); System.out.println("bitsPerSample " + audioSignal.getBitsPerSample());}
            if (i == 11) {
                audioSignal.setSubChunk2ID(new String(byteArray));
                if(audioSignal.getSubChunk2ID().compareTo("data") == 0) {
                    continue;
                }
                else if(audioSignal.getSubChunk2ID().compareTo("LIST") == 0) {
                    byte byteArray2[] = new byte[4];
                    r = fileInputstream.read(byteArray2, 0, 4);
                    byteBuffer = ByteArrayToNumber(byteArray2, 4, 1);
                    int temp = byteBuffer.getInt();
                    //redundant data reading
                    byte byteArray3[] = new byte[temp];
                    r = fileInputstream.read(byteArray3, 0, temp);
                    r = fileInputstream.read(byteArray2, 0, 4);
                    audioSignal.setSubChunk2ID(new String(byteArray2));
                }
            }
            if (i == 12) {
                audioSignal.setSubChunk2Size(byteBuffer.getInt());
            }
        }
        audioSignal.setBytePerSample(audioSignal.getBitsPerSample()/8);
        float value;
        ArrayList<Float> dataVector = new ArrayList<>();
        while (true){
            byte byteArray[] = new byte[audioSignal.getBytePerSample()];
            int v = fileInputstream.read(byteArray, 0, audioSignal.getBytePerSample());
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
        System.out.println("*******************************");
        audioSignal.setAudioData(data);
        return audioSignal;
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
}
