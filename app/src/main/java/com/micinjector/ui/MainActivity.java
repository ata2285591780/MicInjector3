package com.micinjector.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.micinjector.R;
import com.micinjector.config.ConfigManager;
import com.micinjector.config.PrefsHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private static final int MEDIA_PROJECTION_REQUEST_CODE = 1002;

    private Switch switchEnabled;
    private TextInputEditText editTargetPackages;
    private RadioGroup radioGroupAudioSource;
    private RadioButton radioFileSource;
    private RadioButton radioSystemCapture;
    private Button btnSelectAudioFile;
    private TextView textSelectedFile;
    private RadioGroup radioGroupPlayMode;
    private RadioButton radioModeOnce;
    private RadioButton radioModeLoop;
    private RadioButton radioModePtt;
    private CheckBox checkLoopEnabled;
    private SeekBar seekbarVolume;
    private TextView textVolumeValue;
    private Switch switchFloatingWindow;
    private Button btnStartService;
    private Button btnStopService;

    private PrefsHelper prefsHelper;
    private String selectedAudioPath = "";
    private FloatingWindowService floatingService;
    private boolean isServiceBound = false;

    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    selectedAudioPath = getPathFromUri(uri);
                    textSelectedFile.setText(selectedAudioPath);
                    prefsHelper.setAudioFilePath(selectedAudioPath);
                }
            }
        }
    );

    private final ActivityResultLauncher<Intent> mediaProjectionLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                startFloatingService();
            }
        }
    );

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingWindowService.LocalBinder binder = (FloatingWindowService.LocalBinder) service;
            floatingService = binder.getService();
            isServiceBound = true;
            updateServiceButtonStates();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatingService = null;
            isServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefsHelper = new PrefsHelper(this);

        initViews();
        setupListeners();
        loadSavedConfig();
        checkPermissions();
    }

    private void initViews() {
        switchEnabled = findViewById(R.id.switch_enabled);
        editTargetPackages = findViewById(R.id.edit_target_packages);
        radioGroupAudioSource = findViewById(R.id.radio_group_audio_source);
        radioFileSource = findViewById(R.id.radio_file_source);
        radioSystemCapture = findViewById(R.id.radio_system_capture);
        btnSelectAudioFile = findViewById(R.id.btn_select_audio_file);
        textSelectedFile = findViewById(R.id.text_selected_file);
        radioGroupPlayMode = findViewById(R.id.radio_group_play_mode);
        radioModeOnce = findViewById(R.id.radio_mode_once);
        radioModeLoop = findViewById(R.id.radio_mode_loop);
        radioModePtt = findViewById(R.id.radio_mode_ptt);
        checkLoopEnabled = findViewById(R.id.check_loop_enabled);
        seekbarVolume = findViewById(R.id.seekbar_volume);
        textVolumeValue = findViewById(R.id.text_volume_value);
        switchFloatingWindow = findViewById(R.id.switch_floating_window);
        btnStartService = findViewById(R.id.btn_start_service);
        btnStopService = findViewById(R.id.btn_stop_service);
    }

    private void setupListeners() {
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsHelper.setEnabled(isChecked);
            updateUIState();
        });

        editTargetPackages.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String packages = editTargetPackages.getText() != null ? 
                    editTargetPackages.getText().toString() : "";
                prefsHelper.setTargetPackages(packages);
            }
        });

        radioGroupAudioSource.setOnCheckedChangeListener((group, checkedId) -> {
            int source = (checkedId == R.id.radio_file_source) ? 
                ConfigManager.SOURCE_FILE : ConfigManager.SOURCE_SYSTEM_CAPTURE;
            prefsHelper.setAudioSource(source);
            updateAudioSourceUI(source);
        });

        btnSelectAudioFile.setOnClickListener(v -> openFilePicker());

        radioGroupPlayMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radio_mode_once) {
                mode = ConfigManager.MODE_ONCE;
            } else if (checkedId == R.id.radio_mode_loop) {
                mode = ConfigManager.MODE_LOOP;
            } else {
                mode = ConfigManager.MODE_PTT;
            }
            prefsHelper.setPlayMode(mode);
        });

        checkLoopEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsHelper.setLoopEnabled(isChecked);
        });

        seekbarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f;
                textVolumeValue.setText(progress + "%");
                if (fromUser) {
                    prefsHelper.setVolume(volume);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchFloatingWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefsHelper.setFloatingWindowEnabled(isChecked);
            if (isChecked) {
                requestMediaProjection();
            } else {
                stopFloatingService();
            }
        });

        btnStartService.setOnClickListener(v -> {
            if (switchFloatingWindow.isChecked()) {
                requestMediaProjection();
            } else {
                startFloatingService();
            }
        });

        btnStopService.setOnClickListener(v -> stopFloatingService());
    }

    private void loadSavedConfig() {
        prefsHelper.loadConfigToManager();

        switchEnabled.setChecked(prefsHelper.isEnabled());
        editTargetPackages.setText(prefsHelper.getTargetPackages());

        if (prefsHelper.getAudioSource() == ConfigManager.SOURCE_FILE) {
            radioFileSource.setChecked(true);
        } else {
            radioSystemCapture.setChecked(true);
        }

        selectedAudioPath = prefsHelper.getAudioFilePath();
        textSelectedFile.setText(selectedAudioPath);

        int mode = prefsHelper.getPlayMode();
        if (mode == ConfigManager.MODE_ONCE) {
            radioModeOnce.setChecked(true);
        } else if (mode == ConfigManager.MODE_LOOP) {
            radioModeLoop.setChecked(true);
        } else {
            radioModePtt.setChecked(true);
        }

        checkLoopEnabled.setChecked(prefsHelper.isLoopEnabled());

        int volumeProgress = (int) (prefsHelper.getVolume() * 100);
        seekbarVolume.setProgress(volumeProgress);
        textVolumeValue.setText(volumeProgress + "%");

        switchFloatingWindow.setChecked(prefsHelper.isFloatingWindowEnabled());

        updateUIState();
        updateServiceButtonStates();
    }

    private void updateUIState() {
        boolean isEnabled = switchEnabled.isChecked();
        editTargetPackages.setEnabled(isEnabled);
        radioGroupAudioSource.setEnabled(isEnabled);
        btnSelectAudioFile.setEnabled(isEnabled && radioFileSource.isChecked());
        radioGroupPlayMode.setEnabled(isEnabled);
        checkLoopEnabled.setEnabled(isEnabled);
        seekbarVolume.setEnabled(isEnabled);
    }

    private void updateAudioSourceUI(int source) {
        btnSelectAudioFile.setEnabled(source == ConfigManager.SOURCE_FILE);
    }

    private void updateServiceButtonStates() {
        if (isServiceBound && floatingService != null) {
            boolean isRunning = floatingService.isServiceRunning();
            btnStartService.setEnabled(!isRunning);
            btnStopService.setEnabled(isRunning);
        } else {
            btnStartService.setEnabled(true);
            btnStopService.setEnabled(false);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        filePickerLauncher.launch(intent);
    }

    private String getPathFromUri(Uri uri) {
        if (uri == null) return "";

        try {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] projection = {android.provider.OpenableColumns.DISPLAY_NAME};
                android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor != null) {
                    try {
                        if (cursor.moveToFirst()) {
                            return cursor.getString(0);
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            return uri.getLastPathSegment();
        } catch (Exception e) {
            return uri.toString();
        }
    }

    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please enable overlay permission", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted: " + permissions[i], Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission denied: " + permissions[i], Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void requestMediaProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager mpm = (MediaProjectionManager) 
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent intent = mpm.createScreenCaptureIntent();
            mediaProjectionLauncher.launch(intent);
        }
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingWindowService.class);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopFloatingService() {
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        
        Intent intent = new Intent(this, FloatingWindowService.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
}
