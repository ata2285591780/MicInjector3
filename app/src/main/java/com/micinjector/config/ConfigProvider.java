package com.micinjector.config;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class ConfigProvider extends ContentProvider {
    
    public static final String AUTHORITY = "com.micinjector.config";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    private static final int CONFIG = 1;
    private static final int CONFIG_KEY = 2;
    
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";
    
    static {
        uriMatcher.addURI(AUTHORITY, "config", CONFIG);
        uriMatcher.addURI(AUTHORITY, "config/*", CONFIG_KEY);
    }
    
    @Override
    public boolean onCreate() {
        return true;
    }
    
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, 
                       String[] selectionArgs, String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{COLUMN_KEY, COLUMN_VALUE});
        
        ConfigManager config = ConfigManager.getInstance();
        
        switch (uriMatcher.match(uri)) {
            case CONFIG:
                addConfigRows(cursor, config);
                break;
            case CONFIG_KEY:
                String key = uri.getLastPathSegment();
                addConfigRow(cursor, key, config);
                break;
            default:
                return null;
        }
        
        return cursor;
    }
    
    private void addConfigRows(MatrixCursor cursor, ConfigManager config) {
        cursor.addRow(new Object[]{"enabled", config.isEnabled()});
        cursor.addRow(new Object[]{"target_packages", config.getTargetPackages()});
        cursor.addRow(new Object[]{"audio_source", config.getAudioSource()});
        cursor.addRow(new Object[]{"audio_file_path", config.getAudioFilePath()});
        cursor.addRow(new Object[]{"play_mode", config.getPlayMode()});
        cursor.addRow(new Object[]{"volume", config.getVolume()});
        cursor.addRow(new Object[]{"loop_enabled", config.isLoopEnabled()});
        cursor.addRow(new Object[]{"floating_window_enabled", config.isFloatingWindowEnabled()});
        cursor.addRow(new Object[]{"system_capture_enabled", config.isSystemCaptureEnabled()});
    }
    
    private void addConfigRow(MatrixCursor cursor, String key, ConfigManager config) {
        switch (key) {
            case "enabled":
                cursor.addRow(new Object[]{key, config.isEnabled()});
                break;
            case "target_packages":
                cursor.addRow(new Object[]{key, config.getTargetPackages()});
                break;
            case "audio_source":
                cursor.addRow(new Object[]{key, config.getAudioSource()});
                break;
            case "audio_file_path":
                cursor.addRow(new Object[]{key, config.getAudioFilePath()});
                break;
            case "play_mode":
                cursor.addRow(new Object[]{key, config.getPlayMode()});
                break;
            case "volume":
                cursor.addRow(new Object[]{key, config.getVolume()});
                break;
            case "loop_enabled":
                cursor.addRow(new Object[]{key, config.isLoopEnabled()});
                break;
            case "floating_window_enabled":
                cursor.addRow(new Object[]{key, config.isFloatingWindowEnabled()});
                break;
            case "system_capture_enabled":
                cursor.addRow(new Object[]{key, config.isSystemCaptureEnabled()});
                break;
        }
    }
    
    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd." + AUTHORITY;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
