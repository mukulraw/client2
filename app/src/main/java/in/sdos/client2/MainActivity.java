package in.sdos.client2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.wowza.gocoder.sdk.api.WowzaGoCoder;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcast;
import com.wowza.gocoder.sdk.api.broadcast.WZBroadcastConfig;
import com.wowza.gocoder.sdk.api.configuration.WZMediaConfig;
import com.wowza.gocoder.sdk.api.devices.WZAudioDevice;
import com.wowza.gocoder.sdk.api.devices.WZCamera;
import com.wowza.gocoder.sdk.api.devices.WZCameraView;
import com.wowza.gocoder.sdk.api.errors.WZStreamingError;
import com.wowza.gocoder.sdk.api.logging.WZLog;
import com.wowza.gocoder.sdk.api.player.WZPlayerConfig;
import com.wowza.gocoder.sdk.api.player.WZPlayerView;
import com.wowza.gocoder.sdk.api.status.WZState;
import com.wowza.gocoder.sdk.api.status.WZStatus;

public class MainActivity extends GoCoderSDKActivityBase {

    final private static String TAG = MainActivity.class.getSimpleName();

    // Stream player view
    private WZPlayerView mStreamPlayerView = null;
    private WZPlayerConfig mStreamPlayerConfig = null;

    // UI controls
    private MultiStateButton    mBtnPlayStream   = null;
    //private MultiStateButton    mBtnSettings     = null;
    private MultiStateButton    mBtnMic          = null;
    //private MultiStateButton    mBtnScale        = null;
    //private SeekBar mSeekVolume      = null;
    private ProgressDialog mBufferingDialog = null;

    //private StatusView        mStatusView       = null;
    //private TextView mHelp             = null;
    private TimerView         mTimerView        = null;
    //private ImageButton mStreamMetadata   = null;


    public WowzaGoCoder goCoder;

    // The GoCoder SDK camera view
    public WZCameraView goCoderCameraView;

    MultiStateButton start;

    WZBroadcast goCoderBroadcaster;

    // The GoCoder SDK audio device
    private WZAudioDevice goCoderAudioDevice;

    // The broadcast configuration settings
    public WZBroadcastConfig goCoderBroadcastConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        goCoder = WowzaGoCoder.init(this, "GOSK-1245-0103-306E-FFC2-2171");


        mRequiredPermissions = new String[]{};

        mStreamPlayerView = (WZPlayerView) findViewById(R.id.vwStreamPlayer);

        mBtnPlayStream = (MultiStateButton) findViewById(R.id.ic_play_stream);
        //mBtnSettings = (MultiStateButton) findViewById(R.id.ic_settings);
        mBtnMic = (MultiStateButton) findViewById(R.id.ic_mic);
        //mBtnScale = (MultiStateButton) findViewById(R.id.ic_scale);

        mTimerView = (TimerView) findViewById(R.id.txtTimer2);
        //mStatusView = (StatusView) findViewById(R.id.statusView);
        //mStreamMetadata = (ImageButton) findViewById(R.id.imgBtnStreamInfo);
        //mHelp = (TextView) findViewById(R.id.streamPlayerHelp);

        //mSeekVolume = (SeekBar) findViewById(R.id.sb_volume);

        mTimerView.setVisibility(View.GONE);

        if (sGoCoderSDK != null) {
            mTimerView.setTimerProvider(new TimerView.TimerProvider() {
                @Override
                public long getTimecode() {
                    return mStreamPlayerView.getCurrentTime();
                }

                @Override
                public long getDuration() {
                    return mStreamPlayerView.getDuration();
                }
            });

            /*mSeekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (mStreamPlayerView != null && mStreamPlayerView.isPlaying()) {
                        mStreamPlayerView.setVolume(progress);
                    }
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });*/

            // listen for volume changes from device buttons, etc.
            //mVolumeSettingChangeObserver = new VolumeChangeObserver(this, new Handler());
            //getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mVolumeSettingChangeObserver);
            /*mVolumeSettingChangeObserver.setVolumeChangeListener(new VolumeChangeObserver.VolumeChangeListener() {
                @Override
                public void onVolumeChanged(int previousLevel, int currentLevel) {
                    if (mSeekVolume != null)
                        mSeekVolume.setProgress(currentLevel);

                    if (mStreamPlayerView != null && mStreamPlayerView.isPlaying()) {
                        mStreamPlayerView.setVolume(currentLevel);
                    }
                }
            });*/

            //mBtnScale.setState(mStreamPlayerView.getScaleMode() == WZMediaConfig.FILL_VIEW);

            // The streaming player configuration properties
            mStreamPlayerConfig = new WZPlayerConfig();

            mBufferingDialog = new ProgressDialog(this);
            mBufferingDialog.setTitle(R.string.status_buffering);
            mBufferingDialog.setMessage(getResources().getString(R.string.msg_please_wait));
            mBufferingDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    cancelBuffering(dialogInterface);
                }
            });

        } else {
            //mHelp.setVisibility(View.GONE);
            //mStatusView.setErrorMessage(WowzaGoCoder.getLastError().getErrorDescription());
        }



        goCoderCameraView = (WZCameraView) findViewById(R.id.cameraPreview);

        goCoderAudioDevice = new WZAudioDevice();


        goCoderBroadcaster = new WZBroadcast();

// Create a configuration instance for the broadcaster
        goCoderBroadcastConfig = new WZBroadcastConfig(WZMediaConfig.FRAME_SIZE_320x240);

        goCoderCameraView.setScaleMode(WZMediaConfig.RESIZE_TO_ASPECT);

// Set the connection properties for the target Wowza Streaming Engine server or Wowza Cloud account

        //goCoderBroadcastConfig.setConnectionParameters(new WZDataMap());

// Designate the camera preview as the video source
        goCoderBroadcastConfig.setVideoBroadcaster(goCoderCameraView);



        //if (mPermissionsGranted && goCoderCameraView != null) {
        //if (goCoderCameraView.isPreviewPaused())
        //    goCoderCameraView.onResume();
        //  else
        goCoderCameraView.startPreview();
        //}

// Designate the audio device as the audio broadcaster
        goCoderBroadcastConfig.setAudioBroadcaster(goCoderAudioDevice);

        start = findViewById(R.id.ic_broadcast);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                goCoderBroadcastConfig.setHostAddress("ec2-18-219-154-44.us-east-2.compute.amazonaws.com");
                goCoderBroadcastConfig.setPortNumber(1935);
                goCoderBroadcastConfig.setApplicationName("live");
                goCoderBroadcastConfig.setStreamName("test2");



                Log.d("keyFrame", String.valueOf(goCoderBroadcastConfig.getVideoKeyFrameInterval()));

                WZStreamingError configValidationError = goCoderBroadcastConfig.validateForBroadcast();

                if (configValidationError != null) {
                    //Toast.makeText(LiveScreen.this, configValidationError.getErrorDescription(), Toast.LENGTH_LONG).show();
                } else if (goCoderBroadcaster.getStatus().isRunning()) {
                    // Stop the broadcast that is currently running
                    goCoderBroadcaster.endBroadcast(MainActivity.this);
                } else {
                    // Start streaming
                    goCoderBroadcaster.startBroadcast(goCoderBroadcastConfig, MainActivity.this);
                }


            }
        });


    }

    @Override
    protected void onDestroy() {
        /*if (mVolumeSettingChangeObserver != null)
            getApplicationContext().getContentResolver().unregisterContentObserver(mVolumeSettingChangeObserver);
*/

        WZStreamingError configValidationError = goCoderBroadcastConfig.validateForBroadcast();


        if (configValidationError != null) {
            //Toast.makeText(LiveScreen.this, configValidationError.getErrorDescription(), Toast.LENGTH_LONG).show();
        } else if (goCoderBroadcaster.getStatus().isRunning()) {
            // Stop the broadcast that is currently running
            goCoderBroadcaster.endBroadcast(MainActivity.this);
        } else {
            // Start streaming
            goCoderBroadcaster.startBroadcast(goCoderBroadcastConfig, MainActivity.this);
        }

        super.onDestroy();
    }

    /**
     * Android Activity class methods
     */

    @Override
    protected void onResume() {
        super.onResume();



        //goCoderCameraView.onResume();


        if (goCoder != null && goCoderCameraView != null) {

            WZCamera activeCamera = goCoderCameraView.getCamera();
            if (activeCamera != null && activeCamera.hasCapability(WZCamera.FOCUS_MODE_CONTINUOUS))
                activeCamera.setFocusMode(WZCamera.FOCUS_MODE_CONTINUOUS);
        }

        syncUIControlState();
    }

    @Override
    protected void onPause() {
        if (mStreamPlayerView != null && mStreamPlayerView.isPlaying()) {
            mStreamPlayerView.stop();

            // Wait for the streaming player to disconnect and shutdown...
            mStreamPlayerView.getCurrentStatus().waitForState(WZState.IDLE);
        }

        super.onPause();
    }

    /**
     * Click handler for the playback button
     */
    public void onTogglePlayStream(View v) {
        if (mStreamPlayerView.isPlaying()) {
            mStreamPlayerView.stop();
        } else if (mStreamPlayerView.isReadyToPlay()) {
            //mHelp.setVisibility(View.GONE);

            WZStreamingError configValidationError = mStreamPlayerConfig.validateForPlayback();
            if (configValidationError != null) {
                //mStatusView.setErrorMessage(configValidationError.getErrorDescription());
            } else {
                // Set the detail level for network logging output
                mStreamPlayerView.setLogLevel(mWZNetworkLogLevel);

                // Set the player's pre-buffer duration as stored in the app prefs
                float preBufferDuration = GoCoderSDKPrefs.getPreBufferDuration(PreferenceManager.getDefaultSharedPreferences(this));
                mStreamPlayerConfig.setPreRollBufferDuration(preBufferDuration);

                // Start playback of the live stream

                Log.d("config" , mStreamPlayerConfig.toString());

                mStreamPlayerView.play(mStreamPlayerConfig, this);
            }

        }
    }

    /**
     * WZStatusCallback interface methods
     */
    @Override
    public synchronized void onWZStatus(WZStatus status) {
        final WZStatus playerStatus = new WZStatus(status);

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                switch(playerStatus.getState()) {
                    case WZPlayerView.STATE_PLAYING:
                        // Keep the screen on while we are playing back the stream
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        if (mStreamPlayerConfig.getPreRollBufferDuration() == 0f)
                            mTimerView.startTimer();

                        // Since we have successfully opened up the server connection, store the connection info for auto complete
                        GoCoderSDKPrefs.storeHostConfig(PreferenceManager.getDefaultSharedPreferences(MainActivity.this), mStreamPlayerConfig);

                        // Log the stream metadata
                        WZLog.debug(TAG, "Stream metadata:\n" + mStreamPlayerView.getMetadata());
                        break;

                    case WZPlayerView.STATE_READY_TO_PLAY:
                        // Clear the "keep screen on" flag
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                        mTimerView.stopTimer();
                        break;

                    case WZPlayerView.STATE_PREBUFFERING_STARTED:
                        showBuffering();
                        break;

                    case WZPlayerView.STATE_PREBUFFERING_ENDED:
                        hideBuffering();
                        // Make sure player wasn't signaled to shutdown
                        if (mStreamPlayerView.isPlaying())
                            mTimerView.startTimer();
                        break;

                    default:
                        break;
                }
                syncUIControlState();
            }
        });
    }

    @Override
    public synchronized void onWZError(final WZStatus playerStatus) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                displayErrorDialog(playerStatus.getLastError());
                syncUIControlState();
            }
        });
    }

    /**
     * Click handler for the mic/mute button
     */
    public void onToggleMute(View v) {
        mBtnMic.toggleState();

        if (mStreamPlayerView != null)
            mStreamPlayerView.mute(!mBtnMic.isOn());

        //mSeekVolume.setEnabled(mBtnMic.isOn());
    }

    public void onToggleScaleMode(View v) {
        int newScaleMode = mStreamPlayerView.getScaleMode() == WZMediaConfig.RESIZE_TO_ASPECT ? WZMediaConfig.FILL_VIEW : WZMediaConfig.RESIZE_TO_ASPECT;
        //mBtnScale.setState(newScaleMode == WZMediaConfig.FILL_VIEW);
        mStreamPlayerView.setScaleMode(newScaleMode);
    }

    /**
     * Click handler for the metadata button
     */
    /*public void onStreamMetadata(View v) {
        WZDataMap streamMetadata = mStreamPlayerView.getMetadata();
        WZDataMap streamStats = mStreamPlayerView.getStreamStats();
        WZDataMap streamConfig = mStreamPlayerView.getStreamConfig().toDataMap();

        WZDataMap streamInfo = new WZDataMap();

        streamInfo.put("- Stream Statistics -", streamStats);
        streamInfo.put("- Stream Metadata -", streamMetadata);
        streamInfo.put("- Stream Configuration -", streamConfig);

        DataTableFragment dataTableFragment = DataTableFragment.newInstance("Stream Information", streamInfo, false, false);

        // Display/hide the data table fragment
        getFragmentManager().beginTransaction()
                .add(android.R.id.content, dataTableFragment)
                .addToBackStack("metadata_fragment")
                .commit();
    }*/

    /**
     * Click handler for the settings button
     */
    public void onSettings(View v) {
        // Display the prefs fragment
        GoCoderSDKPrefs.PrefsFragment prefsFragment = new GoCoderSDKPrefs.PrefsFragment();
        prefsFragment.setFixedSource(true);
        prefsFragment.setForPlayback(true);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, prefsFragment)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Update the state of the UI controls
     */
    private void syncUIControlState() {
        boolean disableControls = (!(mStreamPlayerView.isReadyToPlay() || mStreamPlayerView.isPlaying()) || sGoCoderSDK == null);

        if (disableControls) {
            mBtnPlayStream.setEnabled(false);
            //mBtnSettings.setEnabled(false);
            //mSeekVolume.setEnabled(false);
            //mBtnScale.setEnabled(false);
            mBtnMic.setEnabled(false);
            //mStreamMetadata.setEnabled(false);
        } else {
            mBtnPlayStream.setState(mStreamPlayerView.isPlaying());
            mBtnPlayStream.setEnabled(true);

            if (mStreamPlayerConfig.isAudioEnabled()) {
                mBtnMic.setVisibility(View.VISIBLE);
                mBtnMic.setEnabled(true);

                //mSeekVolume.setVisibility(View.VISIBLE);
                //mSeekVolume.setEnabled(mBtnMic.isOn());
                //mSeekVolume.setProgress(mStreamPlayerView.getVolume());
            } else {
                //mSeekVolume.setVisibility(View.GONE);
                mBtnMic.setVisibility(View.GONE);
            }

            //mBtnScale.setVisibility(View.VISIBLE);
            //mBtnScale.setVisibility(mStreamPlayerView.isPlaying() && mStreamPlayerConfig.isVideoEnabled() ? View.VISIBLE : View.GONE);
            //mBtnScale.setEnabled(mStreamPlayerView.isPlaying() && mStreamPlayerConfig.isVideoEnabled());

            //mBtnSettings.setEnabled(!mStreamPlayerView.isPlaying());
            //mBtnSettings.setVisibility(mStreamPlayerView.isPlaying() ? View.GONE : View.VISIBLE);

            //mStreamMetadata.setEnabled(mStreamPlayerView.isPlaying());
            //mStreamMetadata.setVisibility(mStreamPlayerView.isPlaying() ? View.VISIBLE : View.GONE);
        }
    }

    private void showBuffering() {
        if (mBufferingDialog == null) return;
        mBufferingDialog.show();
    }

    private void cancelBuffering(DialogInterface dialogInterface) {
        if (mStreamPlayerView != null && mStreamPlayerView.isPlaying())
            mStreamPlayerView.stop();
    }

    private void hideBuffering() {
        if (mBufferingDialog.isShowing())
            mBufferingDialog.dismiss();
    }

    @Override
    public void syncPreferences() {


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        GoCoderSDKPrefs.saveConfigDetails(prefs , "test1");


        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mWZNetworkLogLevel = Integer.valueOf(prefs.getString("wz_debug_net_log_level", String.valueOf(WZLog.LOG_LEVEL_DEBUG)));



        if (mStreamPlayerConfig != null)
            GoCoderSDKPrefs.updateConfigFromPrefs(prefs, mStreamPlayerConfig);
    }

}
