package com.example.sound_proof_android;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Record {

    Context context;
    WavRecorder wavRecorder;
    TimeZone deviceTimeZone;
    AudioRecord recorder;
    MediaPlayer player;
    long recordStopTime;

    public Record(Context context) {
        this.context = context;
        recorder = null;
        player = null;
        deviceTimeZone = Calendar.getInstance().getTimeZone();
        wavRecorder = new WavRecorder(getSoundRecordingPath());
    }

    // Begins recording from the user's microphone and automatically stops recording after 3 seconds.
    // The start time of the recording is also captured as a UNIX timestamps in milliseconds and also in
    // the regular default format.
    // The recording is also stored in external storage as an .mp3 which we can then synchronize with the actual audio
    // and use to analyze.
    public void startRecording() {
        try {
            wavRecorder.startRecording();

            Toast.makeText(context, "Recording Has Started", Toast.LENGTH_LONG).show();

            // Waits exactly 3 seconds and then ends the audio recording
            Thread.sleep(3000);
            stopRecording();
        } catch (Exception e) {
            Log.e("ERROR", "startRecording() failed");
        }
    }

    // Used to stop the audio recording and also captures the end time's UNIX timestamp
    public void stopRecording() {
        wavRecorder.stopRecording();
        recorder = null;

        Toast.makeText(context, "Recording Has Stopped.", Toast.LENGTH_LONG).show();

        calculateRecordStopTime();
        playRecording();
    }

    // Plays back the recording for testing purposes
    public void playRecording() {
        try {
            player = new MediaPlayer();
            player.setDataSource(getSoundRecordingPath()+"/soundproof.wav");
            player.prepare();
            player.start();
            Toast.makeText(context, "Playback Audio... Duration:" + String.valueOf(player.getDuration()), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Playback has failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateRecordStopTime() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    try {
                        URL url = new URL("https://soundproof.azurewebsites.net/servertime");
                        long requestTime = System.currentTimeMillis();
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        String stopServerTimeStr = readStream(in);
                        long responseTime = System.currentTimeMillis();
                        long latency = (responseTime-requestTime)/2;
                        double stopServerTime = Double.parseDouble(stopServerTimeStr);
                        recordStopTime = latency + (long) stopServerTime;
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        Log.d("*** serverExceptionTag", "Server time exception: " + e);
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    public String getSoundRecordingPath(){
        ContextWrapper contextWrapper = new ContextWrapper(context.getApplicationContext());
        File audioDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        return audioDirectory.getPath();
    }

    public long getRecordStopTime() {
        return recordStopTime;
    }

}
