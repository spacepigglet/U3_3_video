package com.example.u3_3_video;

import static androidx.camera.view.CameraController.VIDEO_CAPTURE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Recording;
import androidx.camera.video.RecordingStats;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.camera.view.video.AudioConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.u3_3_video.databinding.ActivityMainBinding;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
     ActivityMainBinding binding ;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final String KEY = "fileUri" ;
    LifecycleCameraController cameraController;
    private boolean isRecording = false;
    private Recording recording;
    private String fileName;
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
                }
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
        createFileName();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

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

                int error = finalizeEvent.getError();
                if (error != VideoRecordEvent.Finalize.ERROR_NONE) {
                    msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
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
        cameraController.unbind();
        Log.d("Current State", "MainActivity: onDestroy()");
    }

}