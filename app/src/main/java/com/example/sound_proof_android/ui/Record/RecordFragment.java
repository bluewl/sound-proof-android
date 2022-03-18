package com.example.sound_proof_android.ui.Record;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.sound_proof_android.QRCodeActivity;
import com.example.sound_proof_android.R;
import com.example.sound_proof_android.SNTPClient;

import android.content.SharedPreferences;
import android.content.SharedPreferences.*;
import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;


public class RecordFragment extends Fragment {

    private Button startRec;
    private Button playRec;
    private TextView offset;
    private TextView localStartTime;
    private TextView ntpStartTime;
    private TextView localStopTime;
    private TextView ntpStopTime;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200; // Request code to record audio / access mic
    private static final String LOG_TAG = "AudioRecord"; // Used to log exceptions
    private MediaRecorder recorder = null; // Used to record the sound audio
    private MediaPlayer player = null; // Used to playback the recorded audio for testing purposes

    private TimeZone deviceTimeZone;
    public static String ntpDate = "nothing";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RecordViewModel recordViewModel =
                new ViewModelProvider(this).get(RecordViewModel.class);

        View v = inflater.inflate(R.layout.fragment_record, container, false);

        startRec = v.findViewById(R.id.record_button);
        playRec = v.findViewById(R.id.play_recording_button);
        offset = v.findViewById(R.id.offsetText);
        localStartTime = v.findViewById(R.id.localStartTimeText);
        ntpStartTime = v.findViewById(R.id.ntpStartTimeText);
        localStopTime = v.findViewById(R.id.localStopTimeText);
        ntpStopTime = v.findViewById(R.id.ntpStopTimeText);
        deviceTimeZone = Calendar.getInstance().getTimeZone();

        startRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        playRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playRecording();
            }
        });

        return v;
    }

    // Begins recording from the user's microphone and automatically stops recording after 3 seconds.
    // The start time of the recording is also captured as a UNIX timestamps in milliseconds and also in
    // the regular default format.
    // The recording is also stored in external storage as an .mp3 which we can then synchronize with the actual audio
    // and use to analyze.
    public void startRecording() {
        try{
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(getSoundRecordingPath()); // getAudioFilePath() // "/Users/eric/Desktop/Audio Sources test" // local path for storing recording
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.prepare();
            recorder.start();

            // Used to get NTP time and compare to local time to find offset
            // Displays offset, local device's time and the NTP's time when the recording started
            SNTPClient.getDate(deviceTimeZone, new SNTPClient.Listener() {
                @Override
                public void onTimeResponse(String rawDate, Date date, Exception ex) {
                    //MainActivity.ntpDate = "NTP date: " + rawDate;
                    offset.setText("Offset: " + SNTPClient.localTimeNtpTimeOffset + "ms");

                    localStartTime.setText("Local Start Time: " + SNTPClient.localStartTime);

                    ntpStartTime.setText("NTP Start Time: " + SNTPClient.NtpStartTime);
                }
            });

            Toast.makeText(getActivity(), "Recording Has Started", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "startRecording() failed");
        }

        // Waits exactly 3 seconds and then ends the audio recording
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopRecording();
            }
        }, 3000);
    }

    // Used to stop the audio recording and also captures the end time's UNIX timestamp
    public void stopRecording() {
        recorder.stop();
        recorder.release();
        // Used to store the end time of the recording as a UNIX timestamp in milliseconds (local device time)
        Long localStopTimestamp = System.currentTimeMillis();
        recorder = null;

        // Displays the local device's time and NTP server's time once the recording ends.
        localStopTime.setText("Local Stop Time: " + localStopTimestamp);

        // given the audio recording is 3000ms in duration, the NTP stop time will be 3000ms after the NTP start time
        Long ntpStopTimestamp = SNTPClient.NtpStartTime + 3000;
        ntpStopTime.setText("NTP Stop Time: " + ntpStopTimestamp);

        Toast.makeText(getActivity(), "Recording Has Stopped.", Toast.LENGTH_LONG).show();
    }

    // Plays back the recording for testing purposes
    public void playRecording() {
        try {
            player = new MediaPlayer();
            player.setDataSource(getSoundRecordingPath());
            player.prepare();
            player.start();
            Toast.makeText(getActivity(), "Playback Audio... Duration:" + String.valueOf(player.getDuration()), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "playRecording() failed");
            Toast.makeText(getActivity(), "Playback has failed", Toast.LENGTH_LONG).show();
        }
    }

    // Used to get the file path where the sound recording will be stored in the device's external storage
    // and returns it as a string for the media recorder and media player to read.
    private String getSoundRecordingPath(){
        ContextWrapper contextWrapper = new ContextWrapper(getActivity().getApplicationContext());
        File audioDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(audioDirectory, "soundproof.mp3");
        return file.getPath();
    }
}