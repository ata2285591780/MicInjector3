package com.micinjector.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    
    private static final String AUDIO_FOLDER = "MicInjector";
    private static final String[] AUDIO_EXTENSIONS = {".wav", ".mp3", ".ogg", ".m4a", ".3gp"};
    
    public static File getAudioDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), AUDIO_FOLDER);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }
    
    public static File getDefaultAudioFile(Context context) {
        File audioDir = getAudioDirectory(context);
        File[] files = audioDir.listFiles();
        
        if (files != null && files.length > 0) {
            for (File file : files) {
                if (isAudioFile(file)) {
                    return file;
                }
            }
        }
        
        return null;
    }
    
    public static List<File> getAudioFiles(Context context) {
        List<File> audioFiles = new ArrayList<>();
        File audioDir = getAudioDirectory(context);
        
        if (audioDir.exists() && audioDir.isDirectory()) {
            File[] files = audioDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (isAudioFile(file)) {
                        audioFiles.add(file);
                    }
                }
            }
        }
        
        return audioFiles;
    }
    
    public static boolean isAudioFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        
        String name = file.getName().toLowerCase();
        for (String ext : AUDIO_EXTENSIONS) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static String getFileName(File file) {
        if (file == null) {
            return "";
        }
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            return name.substring(0, dotIndex);
        }
        return name;
    }
    
    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    public static Uri getUriForFile(File file) {
        if (file == null) {
            return null;
        }
        return Uri.fromFile(file);
    }
    
    public static boolean deleteFile(File file) {
        if (file != null && file.exists()) {
            return file.delete();
        }
        return false;
    }
    
    public static File createAudioFile(Context context, String fileName) {
        File audioDir = getAudioDirectory(context);
        String fullName = fileName;
        
        if (!fullName.toLowerCase().endsWith(".wav")) {
            fullName += ".wav";
        }
        
        return new File(audioDir, fullName);
    }
}
