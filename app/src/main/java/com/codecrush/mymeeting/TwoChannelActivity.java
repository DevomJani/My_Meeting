package com.codecrush.mymeeting;

import android.Manifest;
import android.os.Bundle;
import android.Manifest;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;

public class TwoChannelActivity extends AppCompatActivity {
    private RtcEngine rtcEngine;
    private SurfaceView primaryCameraView;
    private SurfaceView secondaryCameraView;
    private boolean isSecondCameraActive = false;

    private EditText uidInput;
    private int primaryUid = 0; // Example UID for primary camera
    private int secondaryUid = 1; // Example UID for secondary camera

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_channel);

        // Request necessary permissions
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        }, 1000);

        // Initialize UI components
        primaryCameraView = new SurfaceView(getApplicationContext());
        secondaryCameraView = new SurfaceView(getApplicationContext());
        LinearLayout videoContainer = findViewById(R.id.videoContainer);
        videoContainer.addView(primaryCameraView);

        uidInput = findViewById(R.id.uidInput);
        Button startSecondCameraButton = findViewById(R.id.startSecondCameraButton);
        Button publishSecondCameraButton = findViewById(R.id.publishSecondCameraButton);

        // Initialize Agora
        try {
            rtcEngine = RtcEngine.create(getApplicationContext(), "8f0a7c3a1dc94719848272d461c4d78d", rtcEventHandler);
            rtcEngine.enableVideo();
            rtcEngine.setupLocalVideo(new VideoCanvas(primaryCameraView, VideoCanvas.RENDER_MODE_FIT, primaryUid));
            rtcEngine.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start/Stop secondary camera
        startSecondCameraButton.setOnClickListener(v -> {
            if (isSecondCameraActive) {
                stopSecondaryCamera();
            } else {
                startSecondaryCamera();
            }
        });

        // Publish/Unpublish secondary camera
        publishSecondCameraButton.setOnClickListener(v -> {
            String uidText = uidInput.getText().toString();
            if (!uidText.isEmpty()) {
                secondaryUid = Integer.parseInt(uidText);
                if (isSecondCameraActive) {
                    publishSecondaryCamera();
                } else {
                    rtcEngine.leaveChannel();
                }
            }
        });
    }

    private void startSecondaryCamera() {
        rtcEngine.startCameraCapture(Constants.VideoSourceType.VIDEO_SOURCE_CAMERA_SECONDARY, null);
        rtcEngine.setupLocalVideo(new VideoCanvas(secondaryCameraView, VideoCanvas.RENDER_MODE_FIT, secondaryUid));
        rtcEngine.startPreview(Constants.VideoSourceType.VIDEO_SOURCE_CAMERA_SECONDARY);
        isSecondCameraActive = true;
        LinearLayout videoContainer = findViewById(R.id.videoContainer);
        videoContainer.addView(secondaryCameraView);
    }

    private void stopSecondaryCamera() {
        rtcEngine.stopCameraCapture(Constants.VideoSourceType.VIDEO_SOURCE_CAMERA_SECONDARY);
        isSecondCameraActive = false;
        LinearLayout videoContainer = findViewById(R.id.videoContainer);
        videoContainer.removeView(secondaryCameraView);
    }

    private void publishSecondaryCamera() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCameraTrack = false;
        options.publishSecondaryCameraTrack = true;
        options.autoSubscribeAudio = false;
        options.autoSubscribeVideo = false;

        rtcEngine.joinChannel("007eJxTYPD9f3/BpOP12xz8NHy/Rf5penvk05bTh5/IWqw7EzpHXzdKgcEizSDRPNk40TAl2dLE3NDSwsTCyNwoxcTMMNkkxdwi5V/69PSGQEYGhX1cTIwMEAjiszCUpBaXMDAAADcPIdE=", "test", secondaryUid, options);
    }

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            // Handle remote user joining
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            // Handle remote user leaving
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            rtcEngine.destroy();
        }
    }
}