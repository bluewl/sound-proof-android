package com.example.sound_proof_android;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.sound_proof_android.ui.home.HomeFragment;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import 	java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private Record record;
    private TextView currentActionText;

    private static final int REQUEST_PERMISSION_CODE = 200; // Request code to record audio / access mic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        record = new Record(MainActivity.this);

        requestPermission();

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_connect)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        currentActionText = (TextView) findViewById(R.id.currentActionText);
    }

    // PERMISSION REQUEST
    private void requestPermission() {
        List<String> listPermissionsNeeded = new ArrayList<>();
        // To access camera
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        // To access microphone
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }
        // To access download folder
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        // To access download folder
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_PERMISSION_CODE);
        }
    }

    // HTTP REQUEST
    // Receives start recording signal from the server: This is when the phone should start recording
    public void receiveRecordStartSignal() {

        System.out.println("*** trying to receive record start signal ***");
        // GET REQUEST TO RECEIVE SIGNAL TO RECORD (make this a function later)
        RequestQueue queue = Volley.newRequestQueue(this);
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
                    if (response.equals("204")) {
                        Log.i("LOG_RESPONSE", response + ": no signal. Retrying.");

                        // current action message updated
                        currentActionText.setText("Waiting for start record signal...");
                        currentActionText.setTextColor(Color.YELLOW);

                        // Request again
                        receiveRecordStartSignal();

                    } else if (response.equals("200")) {

                        // current action message updated
                        currentActionText.setText("Recording...");
                        currentActionText.setTextColor(Color.MAGENTA);

                        Log.i("LOG_RESPONSE", response + ": signal received: Start recording.");

                        // start recording
                        record.startRecording();

                        // Request for browser recording
                        receiveBrowserAudio();
                    }
//                    Toast.makeText(MainActivity.this, "Response: " + response.toString(), Toast.LENGTH_LONG).show();
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

    // HTTP REQUEST
    // Downloads encrypted browser audio data
    public void receiveBrowserAudio() {

        // current action message updated
        currentActionText.setText("Comparing Sound...");
        currentActionText.setTextColor(Color.CYAN);

        // GET REQUEST TO RECEIVE SIGNAL TO RECORD (make this a function later)
        RequestQueue queue = Volley.newRequestQueue(this);
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
                            Log.i("LOG_RESPONSE", "Browser Audio Received");
                            try {

                                // Parse JSON file
                                long browserStopTime = response.getLong("time");
                                String key = response.getString("key");
                                String iv = response.getString("iv");
                                String b64audio = response.getString("b64audio");

                                // Decrypt and save Browser wav file
                                Cryptography crypt = new Cryptography(MainActivity.this);
                                String decryptedAESkey = crypt.rsaDecrypt(Base64.decode(key, Base64.DEFAULT));
                                crypt.saveWav(crypt.aesDecrypt(b64audio, decryptedAESkey, iv));

                                // Sound Process then send post request of the result
                                SoundProcess sp = new SoundProcess(MainActivity.this, record.getRecordStopTime(), browserStopTime);
                                postResultResponse(sp.startProcess());
                            } catch (JSONException | IOException e) {
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

    // HTTP REQUEST
    // Used to send the post result response back to the server
    // immediately after the sound processing is done
    public void postResultResponse(boolean loginStatus) {
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = "https://soundproof.azurewebsites.net/login/2faresponse";

        String resultMessage = "";
        if(loginStatus){
            // current action message updated
            currentActionText.setText("Login Successful");
            currentActionText.setTextColor(Color.GREEN);

            resultMessage = "true";
        } else {
            // current action message updated
            currentActionText.setText("Login Failed\n\n  Try again");
            currentActionText.setTextColor(Color.RED);

            resultMessage = "false";
        }

        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            PublicKey publicKey = keyStore.getCertificate("spKey").getPublicKey();
            JSONObject postData = new JSONObject();

            postData.put("valid", resultMessage);
            postData.put("key", "-----BEGIN PUBLIC KEY-----" + android.util.Base64.encodeToString(publicKey.getEncoded(), android.util.Base64.DEFAULT).replaceAll("\n", "") + "-----END PUBLIC KEY-----");

            String mRequestBody = postData.toString();

            String finalResultMessage = resultMessage;
            StringRequest stringRequest = new StringRequest (Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    // should return the string number "200" which means success
                    Log.i("LOG_RESPONSE", response);
                    if (response.equals("200")) {
                        receiveRecordStartSignal();
                    }
//                    Toast.makeText(MainActivity.this, "Response: " + response.toString(), Toast.LENGTH_LONG).show();
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
            requestQueue.add(stringRequest);}
        catch (KeyStoreException e) {
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