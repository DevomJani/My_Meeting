package com.codecrush.mymeeting;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Collections;

public class DualCameraActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;
    private DualCameraRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dual_camera);

        // Initialize GLSurfaceView
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new DualCameraRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Initialize cameras
       // initializeCameras();
    }

/*    private void initializeCameras() {
        // Get camera IDs for front/back cameras
        String frontCameraId = getCameraId(CameraCharacteristics.LENS_FACING_FRONT);
        String backCameraId = getCameraId(CameraCharacteristics.LENS_FACING_BACK);

        // Open front camera
        openCamera(frontCameraId, renderer.getFrontTextureSurface());
        // Open back camera
        openCamera(backCameraId, renderer.getBackTextureSurface());
    }*/

    private void openCamera(String cameraId, SurfaceTexture surfaceTexture) {
        // Similar to your existing code, but use surfaceTexture
        // to connect camera output to OpenGL texture
    }
}