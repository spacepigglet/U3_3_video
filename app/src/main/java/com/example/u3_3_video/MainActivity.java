package com.example.u3_3_video;

import static androidx.camera.view.CameraController.VIDEO_CAPTURE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.OutputResults;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.video.impl.VideoCaptureConfig;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.video.AudioConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.u3_3_video.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
     ActivityMainBinding binding ;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private VideoCapture videoCapture;
    private final ActivityResultLauncher<String> activityResultLauncher= null;

    private File videoFile;
    private Recorder recorder;
    private static final String KEY = "fileUri" ;
    LifecycleCameraController cameraController;
    private boolean isRecording = false;
    private Recording recording;
    private String fileName;
    private String filePath;
    private Uri uri;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        Log.d("Current State: ", "MainActivity: onCreate()");

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        startCamera();

        binding.toggleVideoButton.setOnClickListener(onClick -> toggleRecording());
        //binding.toggleRecordingButton.setOnClickListener(onClick -> toggleRecording())

        binding.playVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isRecording){
                    Toast.makeText(MainActivity.this, "Stop recording to be able to play", Toast.LENGTH_SHORT).show();
                } else {//if (uri != null){
                    //start second activity
                    if(intent == null) {
                        intent = new Intent(MainActivity.this, PlaybackActivity.class);
                    }//  pass data to the second activity using extras
                    if(uri != null){
                        intent.putExtra("fileUri", uri.toString());
                    } else {
                        intent.putExtra("fileUri", "");
                    }
                    // Start the second activity
                    startActivity(intent);
                } /*else {
                    Toast.makeText(MainActivity.this, "No recent recording to play", Toast.LENGTH_SHORT).show();
                }*/
            }
        });

    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        PreviewView cameraView = binding.cameraPreview;
        cameraController = new LifecycleCameraController(getBaseContext());
        cameraController.bindToLifecycle(this);
        cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        cameraController.setEnabledUseCases(VIDEO_CAPTURE);
        cameraView.setController(cameraController);
    }

    private void toggleRecording() {
        if (isRecording) {
            binding.toggleVideoButton.setText("Take video");
            stopRecording();

        } else {
            binding.toggleVideoButton.setText("Stop recording");
            startRecording();

        }

        isRecording = !isRecording;
    }
    private void createFileName(){

        fileName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
    }

    @SuppressLint("MissingPermission")
    private void startRecording() {
        //videoFile = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "recorded_video.mp4");
        //binding.cameraPreview.setVisibility(View.VISIBLE);
        //binding.exoPlayerView.setVisibility(View.GONE);
        //isRecording = true;
        createFileName();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");


        filePath = MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/Movies/CameraX-Video/" + fileName +".mp4";
        Log.d("filePath", "manually built path: " + filePath);
        //findFilePath();
        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        AudioConfig audioConfig = AudioConfig.create(true);
        recording = cameraController.startRecording(options, audioConfig, ContextCompat.getMainExecutor(this), videoRecordEvent -> { //new Consumer<VideoRecordEvent> @Override public void accept(VideoRecordEvent event)
            String msg ="";

            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                // Handle the start of a new active recording
                msg = "Recording started";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            } else if (videoRecordEvent instanceof VideoRecordEvent.Pause) {
                msg = "Recording paused";
                // Handle the case where the active recording is paused
            } else if (videoRecordEvent instanceof VideoRecordEvent.Resume) {
                // Handles the case where the active recording is resumed
                msg = "Recording resumed";
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;
                // Handles a finalize event for the active recording, checking Finalize.getError()
                msg = "Recording stopped";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                //recording.stop();
                recording.close();
                uri = finalizeEvent.getOutputResults().getOutputUri();
                Log.d("filePath", "Recording stopped filePath: " + uri);
                        //The output Uri can be obtained via OutputResults.getOutputUri() from VideoRecordEvent.Finalize.getOutputResults().
                //cameraController.unbind();
                int error = finalizeEvent.getError();
                if (error != VideoRecordEvent.Finalize.ERROR_NONE) {
                    msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    //...
                }
            }
            //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

            // All events, including VideoRecordEvent.Status, contain RecordingStats.
            // This can be used to update the UI or track the recording duration.
            RecordingStats recordingStats = videoRecordEvent.getRecordingStats();
            //...
        });

        /*recorder = new Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        PendingRecording pendingRecording = recorder.prepareRecording(getBaseContext(), options);
        pendingRecording.withAudioEnabled();
        recording = pendingRecording.start(ContextCompat.getMainExecutor(this), videoRecordEvent -> { //new Consumer<VideoRecordEvent> @Override public void accept(VideoRecordEvent event)
            String msg ="";

           (...)
        });*/

    }

    private void stopRecording() {
        recording.stop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (uri != null) {
            outState.putString(KEY, uri.toString());
        }//outState = mState;
        Log.d("Current State", "MainActivity: onSaveInstanceState");

    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Current State: ", "MainActivity : onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Current State: ", "MainActivity : onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("Current State:", "MainActivity onRestart()");
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Current State", "MainActivity: onRestoreInstanceState");
        if (savedInstanceState != null) {
            uri = Uri.parse(savedInstanceState.getString(KEY));
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Current State", "MainActivity: onDestroy()");
    }




/**
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot()); //R.layout.activity_main

        exoPlayer = new ExoPlayer.Builder(getApplicationContext()).build();
        binding.exoPlayerView.setPlayer(exoPlayer);
        binding.toggleButton.setOnClickListener(onClick -> videoButtonBehaviour());
    }

    private void setupCamera(){
        Recorder recorder = new Recorder.Builder().build();
        VideoCapture<>
        PreviewView previewView = viewBinding.previewView;
        LifecycleCameraController cameraController = new LifecycleCameraController(baseContext);
        cameraController.bindToLifecycle(this);
        cameraController.setCameraSelector(CameraSelector.DEFAULT_BACK_CAMERA);
        previewView.setController(cameraController);
    }

    private void videoButtonBehaviour() {


    }
    **/
}