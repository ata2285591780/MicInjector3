package com.micinjector.config;

public class ConfigManager {
    
    public static final String PREF_NAME = "mic_injector_prefs";
    
    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_TARGET_PACKAGES = "target_packages";
    public static final String KEY_AUDIO_SOURCE = "audio_source";
    public static final String KEY_AUDIO_FILE_PATH = "audio_file_path";
    public static final String KEY_PLAY_MODE = "play_mode";
    public static final String KEY_VOLUME = "volume";
    public static final String KEY_LOOP_ENABLED = "loop_enabled";
    public static final String KEY_FLOATING_WINDOW_ENABLED = "floating_window_enabled";
    public static final String KEY_SYSTEM_CAPTURE_ENABLED = "system_capture_enabled";
    
    public static final int SOURCE_FILE = 0;
    public static final int SOURCE_SYSTEM_CAPTURE = 1;
    
    public static final int MODE_ONCE = 0;
    public static final int MODE_LOOP = 1;
    public static final int MODE_PTT = 2;
    
    public static final int DEFAULT_SAMPLE_RATE = 44100;
    public static final int DEFAULT_CHANNEL_CONFIG = 1;
    public static final int DEFAULT_AUDIO_FORMAT = 2;
    
    private boolean enabled;
    private String targetPackages;
    private int audioSource;
    private String audioFilePath;
    private int playMode;
    private float volume;
    private boolean loopEnabled;
    private boolean floatingWindowEnabled;
    private boolean systemCaptureEnabled;
    
    private static ConfigManager instance;
    
    private ConfigManager() {
        resetToDefaults();
    }
    
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }
    
    public void resetToDefaults() {
        enabled = false;
        targetPackages = "";
        audioSource = SOURCE_FILE;
        audioFilePath = "";
        playMode = MODE_PTT;
        volume = 1.0f;
        loopEnabled = false;
        floatingWindowEnabled = false;
        systemCaptureEnabled = false;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getTargetPackages() {
        return targetPackages;
    }
    
    public void setTargetPackages(String targetPackages) {
        this.targetPackages = targetPackages;
    }
    
    public int getAudioSource() {
        return audioSource;
    }
    
    public void setAudioSource(int audioSource) {
        this.audioSource = audioSource;
    }
    
    public String getAudioFilePath() {
        return audioFilePath;
    }
    
    public void setAudioFilePath(String audioFilePath) {
        this.audioFilePath = audioFilePath;
    }
    
    public int getPlayMode() {
        return playMode;
    }
    
    public void setPlayMode(int playMode) {
        this.playMode = playMode;
    }
    
    public float getVolume() {
        return volume;
    }
    
    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
    }
    
    public boolean isLoopEnabled() {
        return loopEnabled;
    }
    
    public void setLoopEnabled(boolean loopEnabled) {
        this.loopEnabled = loopEnabled;
    }
    
    public boolean isFloatingWindowEnabled() {
        return floatingWindowEnabled;
    }
    
    public void setFloatingWindowEnabled(boolean floatingWindowEnabled) {
        this.floatingWindowEnabled = floatingWindowEnabled;
    }
    
    public boolean isSystemCaptureEnabled() {
        return systemCaptureEnabled;
    }
    
    public void setSystemCaptureEnabled(boolean systemCaptureEnabled) {
        this.systemCaptureEnabled = systemCaptureEnabled;
    }
    
    public String[] getTargetPackageArray() {
        if (targetPackages == null || targetPackages.trim().isEmpty()) {
            return new String[0];
        }
        return targetPackages.split(",");
    }
    
    public boolean isPackageTargeted(String packageName) {
        if (targetPackages == null || targetPackages.trim().isEmpty()) {
            return false;
        }
        String[] packages = getTargetPackageArray();
        for (String pkg : packages) {
            if (pkg.trim().equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
