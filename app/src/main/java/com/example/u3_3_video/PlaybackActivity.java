package com.example.u3_3_video;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.u3_3_video.databinding.ActivityPlaybackBinding;

public class PlaybackActivity extends AppCompatActivity {
    ActivityPlaybackBinding binding;
    private ExoPlayer exoPlayer;
    private String filePath;
    private static final String KEY = "restored_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlaybackBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        Log.d("Current State", "PlaybackActivity: OnCreate()");

        binding.newVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlaybackActivity.this.finish();
                //Intent intent = new Intent(PlaybackActivity.this, MainActivity.class);
                //startActivity(intent);
            }
        });

        filePath = getIntent().getStringExtra("fileUri");
        Log.d("Current State", "filePath after getIntent().getStringExtra(\"fileUri\"):" + filePath);

        /*if (filePath.isEmpty()) {
            Log.d("Current State", "filePath.isEmpty, attempting to restore from savedInstanceState");
            if(savedInstanceState != null) {
                Log.d("Current State", "savedInstanceState != null");
                filePath = savedInstanceState.getString(KEY);
                Log.d("Current State", "filePath after savedInstanceState.getString(KEY):" + filePath);
            }
        }*/
        if (filePath != null && !filePath.isEmpty()) {
            Log.d("Current State", "player to be initialized with filepath: " + filePath);
            initializePlayer();
        } else {
            Toast.makeText(this, "No recent video to play", Toast.LENGTH_SHORT).show();
            Log.d("Current State", "Filepath is null or empty");
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY, filePath);
        //outState = mState;
        Log.d("Current State", "PlackbackActivity: onSaveInstanceState");

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("Current State", "onRestoreInstanceState");
        if (savedInstanceState != null) {
            filePath = savedInstanceState.getString(KEY);
            Log.d("Current State", "onRestoreInstanceState - filePath after savedInstanceState.getString(KEY):" + filePath);

            /*// Check if filePath is non-empty, then initialize the player
            if (!TextUtils.isEmpty(filePath)) {
                Log.d("Current State", "onRestoreInstanceState - player to be initialized with filepath: " + filePath);
                initializePlayer();
            } else {
                Toast.makeText(this, "No recent video to play", Toast.LENGTH_SHORT).show();
            }*/
        }
    }

    private void initializePlayer() {
        Log.d("Current State", "initializePlayer()");
        exoPlayer = new ExoPlayer.Builder(this).build();
        binding.exoPlayerView.setPlayer(exoPlayer);

        MediaItem mediaItem = MediaItem.fromUri(filePath);

        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        //exoPlayer.setPlayWhenReady(true); // Auto-play
        exoPlayer.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Current State: ", "PlaybackActivity : onPause()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Current State: ", "PlaybackActivity : onStop()");
    }

    @Override
    protected void onDestroy() {
        Log.d("Current State", "PlaybackActivity: onDestroy()");
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false); // Pause the player when the activity is not visible
            exoPlayer.release();
        }
    }
}