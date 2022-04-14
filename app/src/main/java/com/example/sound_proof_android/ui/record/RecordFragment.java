package com.example.sound_proof_android.ui.record;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.sound_proof_android.R;
import com.example.sound_proof_android.SNTPClient;
import com.example.sound_proof_android.WavRecorder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;


public class RecordFragment extends Fragment {

    private Button startRec;
    private Button playRec;
    private Button syncTime;
    private Button recordSignalButton;
    private TextView offset;
    private TextView localStartTime;
    private TextView ntpStartTime;
    private TextView localStopTime;
    private TextView ntpStopTime;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200; // Request code to record audio / access mic
    private static final String LOG_TAG = "AudioRecord"; // Used to log exceptions
    private AudioRecord recorder = null; // Used to record the sound audio
    private MediaPlayer player = null; // Used to playback the recorded audio for testing purposes

    private TimeZone deviceTimeZone;
    public static String ntpDate = "nothing";
    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    WavRecorder wavRecorder;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        RecordViewModel recordViewModel =
                new ViewModelProvider(this).get(RecordViewModel.class);

        View v = inflater.inflate(R.layout.fragment_record, container, false);

        startRec = v.findViewById(R.id.record_button);
        playRec = v.findViewById(R.id.play_recording_button);
        syncTime = v.findViewById(R.id.sync_time_button);
        offset = v.findViewById(R.id.offsetText);
        localStartTime = v.findViewById(R.id.localStartTimeText);
        ntpStartTime = v.findViewById(R.id.ntpStartTimeText);
        localStopTime = v.findViewById(R.id.localStopTimeText);
        ntpStopTime = v.findViewById(R.id.ntpStopTimeText);
        deviceTimeZone = Calendar.getInstance().getTimeZone();
        recordSignalButton = v.findViewById(R.id.recordSignalButton);
        startRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });

        recordSignalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recieveRecordStart();
            }
        });

        playRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playRecording();
            }
        });

        syncTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                syncWithServerTime();
            }
        });

        return v;
    }

    public void recieveRecordStart() {
        System.out.println("*** USER LOGGED IN ***");
        // GET REQUEST TO RECEIVE SIGNAL TO RECORD (make this a function later)
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = "https://soundproof.azurewebsites.net/login/2farecordpolling";

        // get public key
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate("spKey").getPublicKey();
            JSONObject postData = new JSONObject();
            postData.put("key", "-----BEGIN PUBLIC KEY-----" + Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT).replaceAll("\n", "") + "-----END PUBLIC KEY-----");
            String mRequestBody = postData.toString();

            StringRequest stringRequest = new StringRequest (Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // returns string "200" which means success
                    // the phone should start recording if (response.equals("200")) or something like that
                    Log.i("LOG_RESPONSE", response);
                    System.out.println("hi you are in response");
                    if (response.equals("204")) {
                        recieveRecordStart();
                    } else if (response.equals("200")) {
                        receiveBrowserAudio();
                    }
                    Toast.makeText(getActivity(), "Response: " + response.toString(), Toast.LENGTH_LONG).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("LOG_RESPONSE", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return mRequestBody == null ? null : mRequestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };
            // setting timeout
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Access the RequestQueue through your singleton class.
            queue.add(stringRequest);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // GET REQUEST DONE
    }

    // temporary (already exist in process fragment but it will all be combined)
    public void receiveBrowserAudio() {
        // GET REQUEST TO RECEIVE SIGNAL TO RECORD (make this a function later)
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = "https://soundproof.azurewebsites.net/login/2farecordingdata";

        // get public key
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate("spKey").getPublicKey();
            JSONObject postData = new JSONObject();
            postData.put("key", "-----BEGIN PUBLIC KEY-----" + android.util.Base64.encodeToString(publicKey.getEncoded(), android.util.Base64.DEFAULT).replaceAll("\n", "") + "-----END PUBLIC KEY-----");

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.POST, url, postData, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response returns a JSON file that includes time, key, iv, b64audio
                            System.out.println("test1");
                            Log.i("LOG_RESPONSE", "Response: " + response);
                            System.out.println("test2");
                            // temp: testing to see if the json file is correct
                            File json = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"test.json");
                            try {
                                FileOutputStream os = new FileOutputStream(json);
                                os.write(response.toString().getBytes(Charset.forName("UTF-8")));
                                os.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("LOG_RESPONSE", error.toString());
                        }
                    }) {
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    if (response != null) {
                        System.out.println(response.statusCode);
                    }
                    return super.parseNetworkResponse(response);
                }
            };

            // setting timeout
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(25000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Access the RequestQueue through your singleton class.
            queue.add(jsonObjectRequest);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        // GET REQUEST DONE
    }

    // Begins recording from the user's microphone and automatically stops recording after 3 seconds.
    // The start time of the recording is also captured as a UNIX timestamps in milliseconds and also in
    // the regular default format.
    // The recording is also stored in external storage as an .mp3 which we can then synchronize with the actual audio
    // and use to analyze.
    public void startRecording() {
        try {
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            }
            wavRecorder = new WavRecorder(getSoundRecordingPath());
            wavRecorder.startRecording();

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

            // Waits exactly 3 seconds and then ends the audio recording
            Thread.sleep(3000);
            stopRecording();
        } catch (Exception e) {
            Log.e(LOG_TAG, "startRecording() failed");
        }
    }

    // Used to stop the audio recording and also captures the end time's UNIX timestamp
    public void stopRecording() {
        wavRecorder.stopRecording();
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
            player.setDataSource(getSoundRecordingPath()+"/soundproof.wav");
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
        return audioDirectory.getPath();
    }

    private void syncWithServerTime(){
        Toast.makeText(getActivity(), "Inside sync function", Toast.LENGTH_LONG).show();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    try {
                        URL url = new URL("https://soundproof.azurewebsites.net/servertime");
                        long requestTime = System.currentTimeMillis();
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        String temp = readStream(in);
                        long responseTime = System.currentTimeMillis();
                        Log.d("localServerTag", "Request time: " + requestTime);
                        Log.d("stopServerSuccessTag", "Stop server time: " + temp);
                        long latency = (responseTime-requestTime)/2;
                        Log.d("latencySuccessTag", "Latency: " + latency);
                        //long stopServerTime = new Long(temp);
                        //long stopServerTimeLatency = stopServerTime + latency;
                        Log.d("localServerTag", "Response time: " + responseTime);
                        Log.d("stopServerTimeSuccessTag", "Stop server time with latency: " + temp + " + " + latency);
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        Log.d("serverExceptionTag", "Server time exception: " + e);
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


        /*timeRequest.open('GET', "{{ url_for('spAPI.servertime') }}");
        timeRequest.onreadystatechange = function () {
            if (timeRequest.readyState != 4) {
                return;
            }
            var responseTime = (new Date).getTime();
            var rtdLatency = (responseTime - requestTime)/2;
            var serverTimeAtRequest = parseFloat(timeRequest.response);

            serverTime = serverTimeAtRequest+rtdLatency;

            console.log("client time request made", requestTime.valueOf());
            console.log("client time recieve response", responseTime.valueOf());
            console.log("latency", rtdLatency.valueOf());
            console.log("server's time when request recieved", serverTimeAtRequest);
            console.log("server time + latency", serverTime);

            recordingTime = serverTime.valueOf();
        };
        timeRequest.send(null);*/
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
}