package org.ed.track.callsession;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

//import org.ed.track.BuildConfig;
import org.ed.track.R;
import org.ed.track.databinding.ActivityCallBinding;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CallActivity extends AppCompatActivity {

    private ActivityCallBinding binding;

    private String myAppId = "cbb2ffa6bfb7439ba732bda7a8f6d626";
    private RtcEngine mRtcEngine;

    private String channelName;

    int uid = 0; // or unique user ID (integer)

    String callToken;

    private static final int PERMISSION_REQ_ID = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        channelName = getIntent().getStringExtra("channel_name");
        Log.e("channelName", "onCreate: " + channelName);
        if (channelName == null) {
            showToast("Channel name not provided");
            finish();
            return;
        }

        findOrSaveToken();

        binding.leaveSession.setOnClickListener(v -> {
            cleanupAgoraEngine();
            finish();
        });


    }

    public interface OnTokenReceived {
        void onTokenReceived(String token);
    }

    private void fetchAgoraToken(String channelName, int uid, OnTokenReceived callback) {
        OkHttpClient client = new OkHttpClient();

        String url = "http://192.168.1.212:8000/get-token"; // for emulator accessing localhost
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject json = new JSONObject();
        try {
            json.put("channelName", channelName);
            json.put("uid", uid);
            json.put("role", "publisher");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.e("AgoraToken", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("AgoraToken", "Unexpected code " + response);
                    return;
                }

                String resBody = response.body().string();
                try {
                    JSONObject resJson = new JSONObject(resBody);
                    String token = resJson.getString("token");


                    // Run on UI thread to update UI or join channel
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onTokenReceived(token);
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void findOrSaveToken() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("calls").document(channelName).get().addOnSuccessListener(doc -> {

            Log.e("doc.exists()", "findOrSaveToken: " + doc.exists());
            if (doc.exists()) {
                String getToken = doc.getString("token");
                callToken = doc.getString("token");
                if (checkPermissions()) {
                    startVideoCalling(channelName, getToken, uid); // initializes and sets up video
                } else {
                    requestPermissions(); // handle permission flow separately
                }
            } else {
                fetchAgoraToken(channelName, 0, token -> {

                    callToken = token;
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", token);
                    data.put("channelName", channelName);
                    data.put("createdAt", FieldValue.serverTimestamp());
                    db.collection("calls").document(channelName).set(data);
                    Log.e("doc.exists()", "findOrSaveToken: " + channelName + "toen " + token);

                    if (checkPermissions()) {
                        startVideoCalling(channelName, token, uid); // initializes and sets up video
                    } else {
                        requestPermissions(); // handle permission flow separately
                    }
                });
            }
        });
    }


    /**
     * Initialize the engine
     * For real-time communication, initialize an RtcEngine instance and
     * set up event handlers to manage user interactions within the channel.
     * Use RtcEngineConfig to specify the application context, App ID, and custom event handler,
     * then call RtcEngine.create(config) to initialize the engine, enabling further channel operations.
     **/
    private void initializeAgoraVideoSDK() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = myAppId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing RTC engine: " + e.getMessage());
        }
    }

    /**
     * Join a channel
     * To join a channel, call joinChannel with the following parameters:
     * <p>
     * Channel name: The name of the channel to join. Clients that pass the same channel name join the same channel. If a channel with the specified name does not exist, it is created when the first user joins.
     * <p>
     * Authentication token: A dynamic key that authenticates a user when the client joins a channel. In a production environment, you obtain a token from a token server in your security infrastructure. For the purpose of this guide Generate a temporary token.
     * <p>
     * User ID: A 32-bit signed integer that identifies a user in the channel. You can specify a unique user ID for each user yourself. If you set the user ID to 0 when joining a channel, the SDK generates a random number for the user ID and returns the value in the onJoinChannelSuccess callback.
     * <p>
     * Channel media options: Configure ChannelMediaOptions to define publishing and subscription settings, optimize performance for your specific use-case, and set optional parameters.
     **/
    private void joinChannel(String channelName, String token, int uid) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        Log.e("channelName", "joinChannel: " + channelName + "token " + token);
        mRtcEngine.joinChannel(token, channelName, uid, options);
    }

    /**
     * Subscribe to Video SDK events
     * The Video SDK provides an interface for subscribing to channel events.
     * To use it, create an instance of IRtcEngineEventHandler and
     * implement the event methods you want to handle.
     **/

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        // Triggered when the local user successfully joins the specified channel.
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            showToast("Joined channel " + channel);
        }

        // Triggered when a remote user/host joins the channel.
        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(() -> {
                // Initialize and display remote video view for the new user.
                setupRemoteVideo(uid);
                showToast("User joined: " + uid);
            });
        }

        // Triggered when a remote user/host leaves the channel.
        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            runOnUiThread(() -> {
                cleanupAgoraEngine();
                finish();
                showToast("Session Call ended by: " + uid);
            });
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            showToast("Error: " + err);
            Log.e("onError", "onError: " + err);
        }
    };

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    //Enable the video module
    private void enableVideo() {
        mRtcEngine.enableVideo();
        mRtcEngine.startPreview();
    }

    /**
     * Display the local video
     * Call setupLocalVideo to initialize the local view and set the local video display properties
     **/
    private void setupLocalVideo() {
        FrameLayout container = findViewById(R.id.local_video_view_container);
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0));
    }

    /**
     * Display remote video
     * When a remote user joins the channel, call setupRemoteVideo and pass in the remote user's uid,
     * obtained from the onUserJoined callback, to display the remote video.
     **/

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
    }


    /**
     * Use the following code to handle runtime permissions in your Android app.
     * The logic ensures that the necessary permissions are granted before starting Video Calling.
     **/
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID);
    }

    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            return new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            };
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID && checkPermissions()) {
            startVideoCalling(channelName, callToken, uid);
        }
    }

    private void startVideoCalling(String channelName, String token, int uid) {
        initializeAgoraVideoSDK();
        enableVideo();
        setupLocalVideo();
        joinChannel(channelName, token, uid); // join after setup

    }

    private void cleanupAgoraEngine() {
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            mRtcEngine = null;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupAgoraEngine();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cleanupAgoraEngine();
    }
}