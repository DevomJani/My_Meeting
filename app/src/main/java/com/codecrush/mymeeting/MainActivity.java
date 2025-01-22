package com.codecrush.mymeeting;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.codecrush.mymeeting.R;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {

    private RtcEngine frontEngine;
    private RtcEngine backEngine;
    private static final String APP_ID = "8f0a7c3a1dc94719848272d461c4d78d"; // Replace with your App ID
    private static final String TEMP_TOKEN = ""; // Replace with your Token
    private static final String CHANNEL_NAME = "test"; // Replace with your Channel Name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startDualCameraStreaming();
    }

    private void startDualCameraStreaming() {
        // Initialize the front camera engine
        try {
            frontEngine = RtcEngine.create(getApplicationContext(), APP_ID, new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Front Camera joined channel: " + channel, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "User joined: " + uid, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "User offline: " + uid, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize the back camera engine
        try {
            backEngine = RtcEngine.create(getApplicationContext(), APP_ID, new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Back Camera joined channel: " + channel, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "User joined: " + uid, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "User offline: " + uid, Toast.LENGTH_SHORT).show());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set video configuration for both engines
        setupVideoConfig(frontEngine);
        setupVideoConfig(backEngine);

        // Enable video and start preview for both cameras
        frontEngine.enableVideo();
        backEngine.enableVideo();

        // Front camera view
        FrameLayout frontContainer = findViewById(R.id.frontCameraContainer);
        SurfaceView frontSurfaceView = new SurfaceView(getBaseContext());
        frontContainer.addView(frontSurfaceView);
        VideoCanvas frontCanvas = new VideoCanvas(frontSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
        frontEngine.setupLocalVideo(frontCanvas);

        // Back camera view
        FrameLayout backContainer = findViewById(R.id.backCameraContainer);
        SurfaceView backSurfaceView = new SurfaceView(getBaseContext());
        backContainer.addView(backSurfaceView);
        VideoCanvas backCanvas = new VideoCanvas(backSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 1);
        backEngine.setupLocalVideo(backCanvas);

        // Join the channel with both engines
        frontEngine.joinChannel(TEMP_TOKEN, CHANNEL_NAME, null, 0); // Front camera with UID 0
        backEngine.joinChannel(TEMP_TOKEN, CHANNEL_NAME, null, 1); // Back camera with UID 1
    }

    private void setupVideoConfig(RtcEngine engine) {
        engine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
        );
        engine.setVideoEncoderConfiguration(configuration);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (frontEngine != null) {
            frontEngine.leaveChannel();
            frontEngine.stopPreview();
            RtcEngine.destroy();
            frontEngine = null;
        }
        if (backEngine != null) {
            backEngine.leaveChannel();
            backEngine.stopPreview();
            RtcEngine.destroy();
            backEngine = null;
        }
    }
}
