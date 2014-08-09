package com.example.project_k.app;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.project_k.app.util.KaraokeUtil;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import java.io.IOException;

/**
 * Created by sujeetb on 8/7/14.
 */
public class KaraokeActivity extends YouTubeFailureRecoveryActivity implements
        View.OnClickListener,
        TextView.OnEditorActionListener,
        CompoundButton.OnCheckedChangeListener,
        AdapterView.OnItemSelectedListener {

    private static final String[] arr = new String[] {"shakira.3gp", "let_it_go.3gp", "gangnam_style.3gp", "snowman.3gp"};

    private static final ListEntry[] ENTRIES = {
            new ListEntry("Shakira", "7-7knsP2n5w", false),
            new ListEntry("Let It Go", "0HtACLaRDk0", false),
            new ListEntry("Gangnam Style", "9bZkp7q19f0", false),
            new ListEntry("Build Snowman", "6ZOI1yUGPBQ", false)};

    private static final String KEY_CURRENTLY_SELECTED_ID = "currentlySelectedId";

    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer player;
    private TextView stateText;
    private ArrayAdapter<ListEntry> videoAdapter;
    private Spinner videoChooser;
    private Button playButton;
    private Button pauseButton;
    private EditText skipTo;
    private TextView eventLog;
    private StringBuilder logString;
    private RadioGroup styleRadioGroup;

    private MyPlaylistEventListener playlistEventListener;
    private MyPlayerStateChangeListener playerStateChangeListener;
    private MyPlaybackEventListener playbackEventListener;

    private int currentlySelectedPosition;
    private String currentlySelectedId;

    // Android audio capture and player variables.
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private static String mFileName = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("BLAH", getBaseContext().getFilesDir().getAbsolutePath());
        KaraokeUtil.copyFile("bin/ffmpeg", "/data/data/com.example.project_k.app/files/ffmpeg", getBaseContext());

        setContentView(R.layout.karaoke_activity);

        youTubePlayerView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        stateText = (TextView) findViewById(R.id.state_text);
        videoChooser = (Spinner) findViewById(R.id.video_chooser);
        playButton = (Button) findViewById(R.id.play_button);
        pauseButton = (Button) findViewById(R.id.pause_button);
        skipTo = (EditText) findViewById(R.id.skip_to_text);
        eventLog = (TextView) findViewById(R.id.event_log);

        styleRadioGroup = (RadioGroup) findViewById(R.id.style_radio_group);
        ((RadioButton) findViewById(R.id.style_default)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.style_minimal)).setOnCheckedChangeListener(this);
        ((RadioButton) findViewById(R.id.style_chromeless)).setOnCheckedChangeListener(this);
        logString = new StringBuilder();

        videoAdapter = new ArrayAdapter<ListEntry>(this, android.R.layout.simple_spinner_item, ENTRIES);
        videoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        videoChooser.setOnItemSelectedListener(this);
        videoChooser.setAdapter(videoAdapter);

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        skipTo.setOnEditorActionListener(this);

        youTubePlayerView.initialize(DeveloperKey.DEVELOPER_KEY, this);

        playlistEventListener = new MyPlaylistEventListener();
        playerStateChangeListener = new MyPlayerStateChangeListener();
        playbackEventListener = new MyPlaybackEventListener();


        // Android audio capture and player variables.
        //mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName = "/sdcard/Download/singing_" + arr[currentlySelectedPosition] + ".3gp";
        //mFileName += "/audiorecordtest.3gp";
        Log.e("Filename", mFileName);


        setControlsEnabled(false);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        this.player = player;
        player.setPlaylistEventListener(playlistEventListener);
        player.setPlayerStateChangeListener(playerStateChangeListener);
        player.setPlaybackEventListener(playbackEventListener);

        if (!wasRestored) {
            playVideoAtSelection();
        }
        setControlsEnabled(true);
    }

    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return youTubePlayerView;
    }

    private void playVideoAtSelection() {
        ListEntry selectedEntry = videoAdapter.getItem(currentlySelectedPosition);
        if (selectedEntry.id != currentlySelectedId && player != null) {
            currentlySelectedId = selectedEntry.id;
            if (selectedEntry.isPlaylist) {
                player.cuePlaylist(selectedEntry.id);
            } else {
                player.cueVideo(selectedEntry.id);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        currentlySelectedPosition = pos;
        playVideoAtSelection();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    @Override
    public void onClick(View v) {
        if (v == playButton) {
            player.play();
        } else if (v == pauseButton) {
            player.pause();
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == skipTo) {
            int skipToSecs = parseInt(skipTo.getText().toString(), 0);
            player.seekToMillis(skipToSecs * 1000);
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(skipTo.getWindowToken(), 0);
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked && player != null) {
            switch (buttonView.getId()) {
                case R.id.style_default:
                    player.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
                    break;
                case R.id.style_minimal:
                    player.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
                    break;
                case R.id.style_chromeless:
                    player.setPlayerStyle(YouTubePlayer.PlayerStyle.CHROMELESS);
                    break;
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(KEY_CURRENTLY_SELECTED_ID, currentlySelectedId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        currentlySelectedId = state.getString(KEY_CURRENTLY_SELECTED_ID);
    }

    private void updateText() {
        stateText.setText(String.format("Current state: %s %s %s",
                playerStateChangeListener.playerState, playbackEventListener.playbackState,
                playbackEventListener.bufferingState));
    }

    private void log(String message) {
        logString.append(message + "\n");
        eventLog.setText(logString);
    }

    private void setControlsEnabled(boolean enabled) {
        playButton.setEnabled(enabled);
        pauseButton.setEnabled(enabled);
        skipTo.setEnabled(enabled);
        videoChooser.setEnabled(enabled);
        for (int i = 0; i < styleRadioGroup.getChildCount(); i++) {
            styleRadioGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    private static final int parseInt(String intString, int defaultValue) {
        try {
            return intString != null ? Integer.valueOf(intString) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;

        return (hours == 0 ? "" : hours + ":")
                + String.format("%02d:%02d", minutes % 60, seconds % 60);
    }

    private String getTimesText() {
        int currentTimeMillis = player.getCurrentTimeMillis();
        int durationMillis = player.getDurationMillis();
        return String.format("(%s/%s)", formatTime(currentTimeMillis), formatTime(durationMillis));
    }

    private final class MyPlaylistEventListener implements YouTubePlayer.PlaylistEventListener {
        @Override
        public void onNext() {
            log("NEXT VIDEO");
        }

        @Override
        public void onPrevious() {
            log("PREVIOUS VIDEO");
        }

        @Override
        public void onPlaylistEnded() {
            log("PLAYLIST ENDED");
        }
    }

    private final class MyPlaybackEventListener implements YouTubePlayer.PlaybackEventListener {
        String playbackState = "NOT_PLAYING";
        String bufferingState = "";
        @Override
        public void onPlaying() {
            playbackState = "PLAYING";
            updateText();
            log("\tPLAYING " + getTimesText());

            stopPlaying();
            startRecording();
        }

        @Override
        public void onBuffering(boolean isBuffering) {
            bufferingState = isBuffering ? "(BUFFERING)" : "";
            updateText();
            log("\t\t" + (isBuffering ? "BUFFERING " : "NOT BUFFERING ") + getTimesText());

            stopPlaying();
        }

        @Override
        public void onStopped() {
            playbackState = "STOPPED";
            updateText();
            log("\tSTOPPED");

            stopRecording();
        }

        @Override
        public void onPaused() {
            playbackState = "PAUSED";
            updateText();
            log("\tPAUSED " + getTimesText());

            stopRecording();
            KaraokeUtil.combine(
                    "/sdcard/Download/" + arr[currentlySelectedPosition],
                    mFileName);
            startPlaying();
        }

        @Override
        public void onSeekTo(int endPositionMillis) {
            log(String.format("\tSEEKTO: (%s/%s)",
                    formatTime(endPositionMillis),
                    formatTime(player.getDurationMillis())));
        }
    }

    private final class MyPlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {
        String playerState = "UNINITIALIZED";

        @Override
        public void onLoading() {
            playerState = "LOADING";
            updateText();
            log(playerState);
        }

        @Override
        public void onLoaded(String videoId) {
            playerState = String.format("LOADED %s", videoId);
            updateText();
            log(playerState);
        }

        @Override
        public void onAdStarted() {
            playerState = "AD_STARTED";
            updateText();
            log(playerState);
        }

        @Override
        public void onVideoStarted() {
            playerState = "VIDEO_STARTED";
            updateText();
            log(playerState);
        }

        @Override
        public void onVideoEnded() {
            playerState = "VIDEO_ENDED";
            updateText();
            log(playerState);
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason reason) {
            playerState = "ERROR (" + reason + ")";
            if (reason == YouTubePlayer.ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION) {
                // When this error occurs the player is released and can no longer be used.
                player = null;
                setControlsEnabled(false);
            }
            updateText();
            log(playerState);
        }

    }

    private static final class ListEntry {

        public final String title;
        public final String id;
        public final boolean isPlaylist;

        public ListEntry(String title, String videoId, boolean isPlaylist) {
            this.title = title;
            this.id = videoId;
            this.isPlaylist = isPlaylist;
        }

        @Override
        public String toString() {
            return title;
        }

    }





    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            log("startRecording: prepare() failed");
        }

        mRecorder.start();
    }

    private void stopRecording() {

        if (mRecorder != null) {
          mRecorder.stop();
          mRecorder.release();
          mRecorder = null;
        }
    }


    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource("/sdcard/Download/output.3gp");
            //mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            log("startPlaying: prepare() failed");
        }
    }

    private void stopPlaying() {
        if (mPlayer != null) {
          mPlayer.release();
          mPlayer = null;
        }
    }
}
