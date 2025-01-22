package com.codecrush.mymeeting;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class AudienceActivity extends AppCompatActivity {
    private static final String APP_ID = "8f0a7c3a1dc94719848272d461c4d78d"; // Replace with your App ID
    private static final String TEMP_TOKEN = ""; // Replace with your Token
    private static final String CHANNEL_NAME = "test"; // Replace with your Channel Name

    private RtcEngine mRtcEngine;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            runOnUiThread(() -> {
                Toast.makeText(AudienceActivity.this, "Join channel success", Toast.LENGTH_SHORT).show();
                Log.d("AudienceActivity", "Join channel success");
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> {
                setupRemoteVideo(uid);
                Log.d("AudienceActivity", "User joined: " + uid);
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            runOnUiThread(() -> {
                Toast.makeText(AudienceActivity.this, "User offline: " + uid, Toast.LENGTH_SHORT).show();
                Log.d("AudienceActivity", "User offline: " + uid);
            });
        }

        @Override
        public void onError(int err) {
            super.onError(err);
            runOnUiThread(() -> {
                Toast.makeText(AudienceActivity.this, "Error: " + err, Toast.LENGTH_SHORT).show();
                Log.e("AudienceActivity", "Error: " + err);
            });
        }
    };

    private void initializeAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = APP_ID;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
            Log.d("AudienceActivity", "RtcEngine created");
        } catch (Exception e) {
            throw new RuntimeException("Check the error.");
        }

        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE;
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        options.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY;

        mRtcEngine.joinChannel(TEMP_TOKEN, CHANNEL_NAME, 0, options);
        Log.d("AudienceActivity", "Joining channel");
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = findViewById(R.id.remote_video_view_container);
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        // Pass the SurfaceView object to the SDK and set the remote view
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid));
        Log.d("AudienceActivity", "Remote video setup for uid: " + uid);
    }

    private static final int PERMISSION_REQ_ID = 22;

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

    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (checkPermissions()) {
            initializeAndJoinChannel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audience);
        if (checkPermissions()) {
            initializeAndJoinChannel();
        } else {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRtcEngine.leaveChannel();
        RtcEngine.destroy();
        Log.d("AudienceActivity", "RtcEngine destroyed");
    }
}
