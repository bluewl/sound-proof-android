package com.example.sound_proof_android;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sound_proof_android.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;

import java.util.Calendar;
import java.util.Date;
import 	java.util.GregorianCalendar;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200; // Request code to record audio / access mic
    private static final String LOG_TAG = "AudioRecord"; // Used to log exceptions
    private MediaRecorder recorder = null; // Used to record the sound audio
    private MediaPlayer player = null; // Used to playback the recorded audio for testing purposes

    public static String ntpDate = "nothing";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getMicrophoneAccess(); // requests microphone access from the user

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    // Begins recording from the user's microphone and automatically stops recording after 3 seconds.
    // The start time of the recording is also captured as a UNIX timestamps in milliseconds and also in
    // the regular default format.
    // The recording is also stored in external storage as an .mp3 which we can then synchronize with the actual audio
    // and use to analyze.
    public void startRecording(View v) {
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
            TimeZone deviceTimeZone = Calendar.getInstance().getTimeZone();
            SNTPClient.getDate(deviceTimeZone, new SNTPClient.Listener() {
                @Override
                public void onTimeResponse(String rawDate, Date date, Exception ex) {
                    //MainActivity.ntpDate = "NTP date: " + rawDate;
                    TextView offset = findViewById(R.id.offsetText);
                    offset.setText("Offset: " + SNTPClient.localTimeNtpTimeOffset);

                    TextView localStartTime = findViewById(R.id.localStartTimeText);
                    localStartTime.setText("Local Start Time: " + SNTPClient.localStartTime);

                    TextView ntpStartTime = findViewById(R.id.ntpStartTimeText);
                    ntpStartTime.setText("NTP Start Time: " + SNTPClient.NtpStartTime);
                }
            });

            Toast.makeText(this, "Recording Has Started", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "startRecording() failed");
        }

        // Waits exactly 3 seconds and then ends the audio recording
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               stopRecording(v);
            }
        }, 3000);
    }

    // Used to stop the audio recording and also captures the end time's UNIX timestamp
    public void stopRecording(View v) {
        recorder.stop();
        recorder.release();
        // Used to store the end time of the recording as a UNIX timestamp in milliseconds (local device time)
        Long localStopTimestamp = System.currentTimeMillis();
        recorder = null;

        // Displays the local device's time and NTP server's time once the recording ends.
        TextView localStopTime = findViewById(R.id.localStopTimeText);
        localStopTime.setText("Local Stop Time: " + localStopTimestamp);

        Long ntpStopTimestamp = SNTPClient.NtpStartTime + SNTPClient.localTimeNtpTimeOffset;
        TextView ntpStopTime = findViewById(R.id.ntpStopTimeText);
        ntpStopTime.setText("NTP Stop Time: " + ntpStopTimestamp);

        Toast.makeText(this, "Recording Has Stopped.", Toast.LENGTH_LONG).show();
    }


    // Plays back the recording for testing purposes
    public void playRecording(View v) {
        try {
            player = new MediaPlayer();
            player.setDataSource(getSoundRecordingPath());
            player.prepare();
            player.start();
            Toast.makeText(this, "Playback Audio... Duration:" + String.valueOf(player.getDuration()), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(LOG_TAG, "playRecording() failed");
            Toast.makeText(this, "Playback has failed", Toast.LENGTH_LONG).show();
        }
    }

    // Requests permission to access the device's microphone
    // if the user has not already granted access.
    private void getMicrophoneAccess(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    // Used to get the file path where the sound recording will be stored in the device's external storage
    // and returns it as a string for the media recorder and media player to read.
    private String getSoundRecordingPath(){
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File audioDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(audioDirectory, "soundproof.mp3");
        return file.getPath();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}