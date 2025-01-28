package com.codecrush.mymeeting;

import static android.opengl.GLES10.glGenTextures;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {

    private GLSurfaceView glSurfaceView;
    private SurfaceTexture backCameraSurfaceTexture, frontCameraSurfaceTexture;
    private int backTextureId, frontTextureId;
    private FloatBuffer vertexBuffer;
    private int program;

    // Camera variables
    private CameraManager cameraManager;
    private CameraDevice backCamera, frontCamera;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private HandlerThread cameraHandlerThread;
    private Handler cameraHandler;


    // Shader code
    private final String vertexShaderCode =
            "attribute vec4 vPosition;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "   gl_Position = vPosition;\n" +
                    "   vTexCoord = (vPosition.xy + 1.0) * 0.5;\n" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "uniform sampler2D backTexture;\n" +
                    "uniform sampler2D frontTexture;\n" +
                    "varying vec2 vTexCoord;\n" +
                    "void main() {\n" +
                    "   vec4 backColor = texture2D(backTexture, vTexCoord);\n" +
                    "   vec2 frontUV = vec2(1.0 - vTexCoord.x * 0.3, vTexCoord.y * 0.3);\n" + // PIP scaling + mirror
                    "   vec4 frontColor = texture2D(frontTexture, frontUV);\n" +
                    "   if(vTexCoord.x > 0.7 && vTexCoord.y < 0.3) {\n" +
                    "       gl_FragColor = frontColor;\n" +
                    "   } else {\n" +
                    "       gl_FragColor = backColor;\n" +
                    "   }\n" +
                    "}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraHandlerThread = new HandlerThread("CameraBackground");
        cameraHandlerThread.start();
        cameraHandler = new Handler(cameraHandlerThread.getLooper());

        // Check camera permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            initGL();
        }
    }

    private void initGL() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(this);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initGL();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // Generate textures
        int[] textures = new int[2];
        GLES20.glGenTextures(2, textures, 0);
        backTextureId = textures[0];
        frontTextureId = textures[1];

        // Configure textures
        setupTexture(backTextureId);
        setupTexture(frontTextureId);

        // Create SurfaceTextures
        backCameraSurfaceTexture = new SurfaceTexture(backTextureId);
        frontCameraSurfaceTexture = new SurfaceTexture(frontTextureId);

        // Setup cameras
        setupCameras();

        // Initialize shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        // Check for linking errors
        IntBuffer linkStatus = IntBuffer.allocate(1);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus);
        if (linkStatus.get(0) != GLES20.GL_TRUE) {
            Log.e("GL", "Program linking failed: " + GLES20.glGetProgramInfoLog(program));
        }

        // Initialize vertex data
        float[] vertices = {
                -1.0f, -1.0f,  // Bottom-left
                1.0f, -1.0f,   // Bottom-right
                -1.0f, 1.0f,   // Top-left
                1.0f, 1.0f     // Top-right
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
    }

    private void setupTexture(int textureId) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    private void setupCameras() {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            String backCameraId = cameraIds[0];
            String frontCameraId = cameraIds[1];

            // Open back camera
            openCamera(backCameraId, true);
            // Open front camera
            openCamera(frontCameraId, false);

        } catch (CameraAccessException e) {
            Log.e("CAMERA", "Camera access error", e);
        }
    }

    private void openCamera(String cameraId, boolean isBackCamera) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    if (isBackCamera) {
                        backCamera = camera;
                    } else {
                        frontCamera = camera;
                    }
                    startPreview(camera, isBackCamera);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e("CAMERA", "Camera error: " + error);
                }
            },  new Handler(Looper.getMainLooper()));
        } catch (CameraAccessException e) {
            Log.e("CAMERA", "Cannot open camera", e);
        }
    }

    private void startPreview(CameraDevice camera, boolean isBackCamera) {
        SurfaceTexture surfaceTexture = isBackCamera ? backCameraSurfaceTexture : frontCameraSurfaceTexture;
        surfaceTexture.setDefaultBufferSize(1280, 720); // Adjust based on camera resolution

        Surface surface = new Surface(surfaceTexture);

        if (surfaceTexture == null) {
            Log.e("CAMERA", "SurfaceTexture is null");
            return;
        }

        try {
            camera.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                CaptureRequest.Builder requestBuilder =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                requestBuilder.addTarget(surface);
                                session.setRepeatingRequest(requestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                Log.e("CAMERA", "Capture session failed", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e("CAMERA", "Configuration failed");
                        }
                    },new Handler(Looper.getMainLooper()));
        } catch (CameraAccessException e) {
            Log.e("CAMERA", "Cannot create capture session", e);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        try {
            if (backCameraSurfaceTexture != null) {
                backCameraSurfaceTexture.updateTexImage();
            }
            if (frontCameraSurfaceTexture != null) {
                frontCameraSurfaceTexture.updateTexImage();
            }
        } catch (RuntimeException e) {
            Log.e("GL", "Texture update failed: " + e.getMessage());
        }

        /*// Update camera frames
        backCameraSurfaceTexture.updateTexImage();
        frontCameraSurfaceTexture.updateTexImage();*/

        // Clear screen
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Use shader program
        GLES20.glUseProgram(program);

        // Bind textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backTextureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "backTexture"), 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frontTextureId);
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "frontTexture"), 1);

        // Set vertex attributes
        int positionHandle = GLES20.glGetAttribLocation(program, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // Cleanup
        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Check for compilation errors
        IntBuffer compiled = IntBuffer.allocate(1);
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled);
        if (compiled.get(0) == 0) {
            Log.e("GL", "Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (backCamera != null) {
            backCamera.close();
            backCamera = null;
        }
        if (frontCamera != null) {
            frontCamera.close();
            frontCamera = null;
        }
        cameraHandlerThread.quitSafely();
    }
}