package com.micinjector.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PrefsHelper {
    
    private final SharedPreferences prefs;
    private final Context context;
    
    public PrefsHelper(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
    }
    
    public boolean isEnabled() {
        return prefs.getBoolean(ConfigManager.KEY_ENABLED, false);
    }
    
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(ConfigManager.KEY_ENABLED, enabled).apply();
        ConfigManager.getInstance().setEnabled(enabled);
    }
    
    public String getTargetPackages() {
        return prefs.getString(ConfigManager.KEY_TARGET_PACKAGES, "");
    }
    
    public void setTargetPackages(String packages) {
        prefs.edit().putString(ConfigManager.KEY_TARGET_PACKAGES, packages).apply();
        ConfigManager.getInstance().setTargetPackages(packages);
    }
    
    public int getAudioSource() {
        return prefs.getInt(ConfigManager.KEY_AUDIO_SOURCE, ConfigManager.SOURCE_FILE);
    }
    
    public void setAudioSource(int source) {
        prefs.edit().putInt(ConfigManager.KEY_AUDIO_SOURCE, source).apply();
        ConfigManager.getInstance().setAudioSource(source);
    }
    
    public String getAudioFilePath() {
        return prefs.getString(ConfigManager.KEY_AUDIO_FILE_PATH, "");
    }
    
    public void setAudioFilePath(String path) {
        prefs.edit().putString(ConfigManager.KEY_AUDIO_FILE_PATH, path).apply();
        ConfigManager.getInstance().setAudioFilePath(path);
    }
    
    public int getPlayMode() {
        return prefs.getInt(ConfigManager.KEY_PLAY_MODE, ConfigManager.MODE_PTT);
    }
    
    public void setPlayMode(int mode) {
        prefs.edit().putInt(ConfigManager.KEY_PLAY_MODE, mode).apply();
        ConfigManager.getInstance().setPlayMode(mode);
    }
    
    public float getVolume() {
        return prefs.getFloat(ConfigManager.KEY_VOLUME, 1.0f);
    }
    
    public void setVolume(float volume) {
        prefs.edit().putFloat(ConfigManager.KEY_VOLUME, volume).apply();
        ConfigManager.getInstance().setVolume(volume);
    }
    
    public boolean isLoopEnabled() {
        return prefs.getBoolean(ConfigManager.KEY_LOOP_ENABLED, false);
    }
    
    public void setLoopEnabled(boolean enabled) {
        prefs.edit().putBoolean(ConfigManager.KEY_LOOP_ENABLED, enabled).apply();
        ConfigManager.getInstance().setLoopEnabled(enabled);
    }
    
    public boolean isFloatingWindowEnabled() {
        return prefs.getBoolean(ConfigManager.KEY_FLOATING_WINDOW_ENABLED, false);
    }
    
    public void setFloatingWindowEnabled(boolean enabled) {
        prefs.edit().putBoolean(ConfigManager.KEY_FLOATING_WINDOW_ENABLED, enabled).apply();
        ConfigManager.getInstance().setFloatingWindowEnabled(enabled);
    }
    
    public boolean isSystemCaptureEnabled() {
        return prefs.getBoolean(ConfigManager.KEY_SYSTEM_CAPTURE_ENABLED, false);
    }
    
    public void setSystemCaptureEnabled(boolean enabled) {
        prefs.edit().putBoolean(ConfigManager.KEY_SYSTEM_CAPTURE_ENABLED, enabled).apply();
        ConfigManager.getInstance().setSystemCaptureEnabled(enabled);
    }
    
    public void loadConfigToManager() {
        ConfigManager config = ConfigManager.getInstance();
        config.setEnabled(isEnabled());
        config.setTargetPackages(getTargetPackages());
        config.setAudioSource(getAudioSource());
        config.setAudioFilePath(getAudioFilePath());
        config.setPlayMode(getPlayMode());
        config.setVolume(getVolume());
        config.setLoopEnabled(isLoopEnabled());
        config.setFloatingWindowEnabled(isFloatingWindowEnabled());
        config.setSystemCaptureEnabled(isSystemCaptureEnabled());
    }
    
    public void clearAll() {
        prefs.edit().clear().apply();
        ConfigManager.getInstance().resetToDefaults();
    }
}
