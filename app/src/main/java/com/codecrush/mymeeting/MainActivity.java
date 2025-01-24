package com.codecrush.mymeeting;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Random;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcConnection;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.RtcEngineEx;
import io.agora.rtc2.video.CameraCapturerConfiguration;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class MainActivity extends AppCompatActivity {
    private static final String APP_ID = "8f0a7c3a1dc94719848272d461c4d78d";
    private static final String CHANNEL_1 = "channel-1";
    private static final String CHANNEL_2 = "channel-2";
    private static final String TOKEN_1 = "007eJxTYLj645h1UH5YSvzH9QkXLhxMtTnYyfiwab2HmHlFFbvK6zAFBos0g0TzZONEw5RkSxNzQ0sLEwsjc6MUEzPDZJMUc4uUi7cnpjcEMjKEpmmzMDJAIIjPyZCckZiXl5qja8jAAAAlJyEg";
    private static final String TOKEN_2 = "007eJxTYPgtP6n1qlfQZLcLrcdnCh6eqiTe3eP7rDzL9a6u8JTg+ssKDBZpBonmycaJhinJlibmhpYWJhZG5kYpJmaGySYp5hYpj29PTG8IZGTgWzGLhZEBAkF8TobkjMS8vNQcXSMGBgD/wSE7";

    private RtcEngineEx engine;
    private RtcEngineEx engine2;
    private RtcConnection rtcConnection1 = new RtcConnection();
    private RtcConnection rtcConnection2 = new RtcConnection();

    private FrameLayout localContainer;
    private FrameLayout remoteContainer1;
    private FrameLayout remoteContainer2;

    private static final int PERMISSION_REQ_ID = 22;

    private String[] getRequiredPermissions() {
        return new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
        };
    }

    private boolean checkPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private final IRtcEngineEventHandler eventHandler1 = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Joined Channel 1: " + channel, Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            //runOnUiThread(() -> setupRemoteVideo(remoteContainer1, uid, rtcConnection1));
        }
    };

    private final IRtcEngineEventHandler eventHandler2 = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Joined Channel 2: " + channel, Toast.LENGTH_SHORT).show());
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            //runOnUiThread(() -> setupRemoteVideo(remoteContainer2, uid, rtcConnection2));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        localContainer = findViewById(R.id.local_video_view_container);
        remoteContainer1 = findViewById(R.id.remote_video_view_container_1);
        remoteContainer2 = findViewById(R.id.remote_video_view_container_2);

        if (checkPermissions()) {
            initializeAndJoinChannels();
        } else {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), PERMISSION_REQ_ID);
        }
    }

    private void initializeAndJoinChannels() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getApplicationContext();
            config.mAppId = APP_ID;
            engine = (RtcEngineEx) RtcEngine.create(config);
            engine2 = (RtcEngineEx) RtcEngine.create(config);

            // Configure the first channel with the front camera
            rtcConnection1.channelId = CHANNEL_1;
            rtcConnection1.localUid = new Random().nextInt(512) + 1;
            ChannelMediaOptions options1 = new ChannelMediaOptions();
            options1.autoSubscribeAudio = true;
            options1.autoSubscribeVideo = true;
            options1.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;

            engine.setCameraCapturerConfiguration(new CameraCapturerConfiguration(
                    CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_FRONT
            ));
            engine.joinChannelEx(TOKEN_1, rtcConnection1, options1, eventHandler1);

            // Configure the second channel with the back camera
            rtcConnection2.channelId = CHANNEL_2;
            rtcConnection2.localUid = new Random().nextInt(512) + 512;
            ChannelMediaOptions options2 = new ChannelMediaOptions();
            options2.autoSubscribeAudio = true;
            options2.autoSubscribeVideo = true;
            options2.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;

            engine2.setCameraCapturerConfiguration(new CameraCapturerConfiguration(
                    CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR
            ));
            engine2.joinChannelEx(TOKEN_2, rtcConnection2, options2, eventHandler2);

            setupLocalVideo();
        } catch (Exception e) {
            throw new RuntimeException("RTC SDK Initialization Error: " + e.getMessage());
        }
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = new SurfaceView(this);
        remoteContainer1.addView(surfaceView);
        remoteContainer1.removeAllViews();
        engine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, rtcConnection1.localUid));

        remoteContainer2.addView(surfaceView);
        remoteContainer2.removeAllViews();
        engine2.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, rtcConnection2.localUid));

        engine.startPreview();
        engine2.startPreview();
        engine.enableVideo();
        engine2.enableVideo();


    }

    private void setupRemoteVideo(FrameLayout container, int uid, RtcConnection connection) {
        SurfaceView surfaceView = new SurfaceView(this);
        surfaceView.setZOrderMediaOverlay(true);
        container.removeAllViews();
        container.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        engine.setupRemoteVideoEx(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid), connection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (checkPermissions()) {
                initializeAndJoinChannels();
            } else {
                Toast.makeText(this, "Permissions denied!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engine.leaveChannelEx(rtcConnection1);
        engine.leaveChannelEx(rtcConnection2);
        RtcEngine.destroy();
    }
}
