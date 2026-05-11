package com.micinjector.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.micinjector.MainActivity;
import com.micinjector.R;
import com.micinjector.audio.AudioFileEngine;
import com.micinjector.audio.AudioSystemCaptureEngine;
import com.micinjector.config.ConfigManager;
import com.micinjector.config.PrefsHelper;
import com.micinjector.hook.MainHook;

public class FloatingWindowService extends Service {

    private static final String CHANNEL_ID = "MicInjectorChannel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View floatingView;
    private LinearLayout layoutControls;
    private LinearLayout layoutPtt;

    private TextView textStatus;
    private ImageButton btnPlayPause;
    private ImageButton btnStop;
    private Button btnPtt;
    private TextView textCurrentFile;

    private AudioFileEngine audioFileEngine;
    private AudioSystemCaptureEngine systemCaptureEngine;

    private PrefsHelper prefsHelper;
    private boolean isServiceRunning = false;
    private boolean isPlaying = false;
    private boolean isPttMode = false;

    private Handler mainHandler;
    private MediaProjection mediaProjection;

    public class LocalBinder extends android.os.Binder {
        public FloatingWindowService getService() {
            return FloatingWindowService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mainHandler = new Handler(Looper.getMainLooper());
        prefsHelper = new PrefsHelper(this);
        prefsHelper.loadConfigToManager();

        audioFileEngine = new AudioFileEngine();
        audioFileEngine.setPlaybackListener(new AudioFileEngine.PlaybackListener() {
            @Override
            public void onPlaybackStarted() {
                mainHandler.post(() -> updatePlayButtonState(true));
            }

            @Override
            public void onPlaybackStopped() {
                mainHandler.post(() -> updatePlayButtonState(false));
            }

            @Override
            public void onPlaybackError(String error) {
                mainHandler.post(() -> {
                    Toast.makeText(FloatingWindowService.this, 
                        "Playback error: " + error, Toast.LENGTH_SHORT).show();
                    updatePlayButtonState(false);
                });
            }
        });

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        createFloatingWindow();
        isServiceRunning = true;
    }

    private void createFloatingWindow() {
        if (floatingView != null) {
            return;
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_window, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        windowManager.addView(floatingView, params);

        initFloatingViewControls();
        setupTouchListener(params);
        updateFloatingUI();
    }

    private void initFloatingViewControls() {
        layoutControls = floatingView.findViewById(R.id.layout_controls);
        layoutPtt = floatingView.findViewById(R.id.layout_ptt);
        textStatus = floatingView.findViewById(R.id.text_status);
        btnPlayPause = floatingView.findViewById(R.id.btn_play_pause);
        btnStop = floatingView.findViewById(R.id.btn_stop);
        btnPtt = floatingView.findViewById(R.id.btn_ptt);
        textCurrentFile = floatingView.findViewById(R.id.text_current_file);

        btnPlayPause.setOnClickListener(v -> togglePlayback());
        btnStop.setOnClickListener(v -> stopPlayback());
        btnPtt.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startPtt();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopPtt();
                    return true;
            }
            return false;
        });

        ImageButton btnClose = floatingView.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(v -> stopSelf());

        ImageButton btnMinimize = floatingView.findViewById(R.id.btn_minimize);
        btnMinimize.setOnClickListener(v -> minimizeWindow());

        int playMode = ConfigManager.getInstance().getPlayMode();
        isPttMode = (playMode == ConfigManager.MODE_PTT);

        layoutControls.setVisibility(isPttMode ? View.GONE : View.VISIBLE);
        layoutPtt.setVisibility(isPttMode ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener(WindowManager.LayoutParams params) {
        View dragHandle = floatingView.findViewById(R.id.layout_drag_handle);

        dragHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_MOVE:
                    params.x = (int) event.getRawX();
                    params.y = (int) event.getRawY() - 100;
                    windowManager.updateViewLayout(floatingView, params);
                    return true;
                case MotionEvent.ACTION_UP:
                    return true;
            }
            return false;
        });
    }

    private void togglePlayback() {
        if (isPlaying) {
            pausePlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        String filePath = ConfigManager.getInstance().getAudioFilePath();
        if (filePath == null || filePath.isEmpty()) {
            Toast.makeText(this, "Please select an audio file first", Toast.LENGTH_SHORT).show();
            return;
        }

        audioFileEngine.startPlaybackFile(filePath);
        isPlaying = true;
        updatePlayButtonState(true);
        textStatus.setText("Playing");
    }

    private void pausePlayback() {
        audioFileEngine.stopPlayback();
        isPlaying = false;
        updatePlayButtonState(false);
        textStatus.setText("Paused");
    }

    private void stopPlayback() {
        audioFileEngine.stopPlayback();
        isPlaying = false;
        updatePlayButtonState(false);
        textStatus.setText("Stopped");
    }

    private void startPtt() {
        isPttMode = true;
        audioFileEngine.startRecording();
        btnPtt.setText("Speaking...");
        textStatus.setText("Recording");
    }

    private void stopPtt() {
        audioFileEngine.stopRecording();
        btnPtt.setText("Hold to Talk");
        textStatus.setText("Ready");
    }

    private void updatePlayButtonState(boolean playing) {
        isPlaying = playing;
        btnPlayPause.setImageResource(playing ? 
            android.R.drawable.ic_media_pause : 
            android.R.drawable.ic_media_play);
    }

    private void updateFloatingUI() {
        String filePath = ConfigManager.getInstance().getAudioFilePath();
        if (filePath != null && !filePath.isEmpty()) {
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            textCurrentFile.setText(fileName);
        } else {
            textCurrentFile.setText("No file selected");
        }

        textStatus.setText("Ready");
    }

    private void minimizeWindow() {
        if (floatingView != null) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(floatingView, "alpha", 1f, 0f);
            animator.setDuration(300);
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    if (floatingView != null) {
                        windowManager.removeView(floatingView);
                        floatingView = null;
                    }
                }
            });
            animator.start();
        }
    }

    public void setMediaProjection(MediaProjection projection) {
        this.mediaProjection = projection;
        if (systemCaptureEngine != null) {
            systemCaptureEngine.setMediaProjection(projection);
        }
    }

    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "MicInjector Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("MicInjector floating window service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MicInjector")
            .setContentText("Service running")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isServiceRunning = false;

        if (audioFileEngine != null) {
            audioFileEngine.release();
            audioFileEngine = null;
        }

        if (systemCaptureEngine != null) {
            systemCaptureEngine.release();
            systemCaptureEngine = null;
        }

        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
}
