package com.micinjector.hook;

import android.app.ActivityManager;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.micinjector.audio.AudioFileEngine;
import com.micinjector.config.ConfigManager;
import com.micinjector.config.PrefsHelper;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook {
    
    private static final String TAG = "MicInjector";
    private static MainHook instance;
    
    private XC_LoadPackage.LoadPackageParam loadPackageParam;
    private AudioFileEngine audioFileEngine;
    private volatile boolean isHookActive = false;
    private volatile boolean isInjecting = false;
    
    private byte[] injectedAudioData;
    private int injectedDataOffset = 0;
    private String currentTargetPackage = null;
    
    private Handler mainHandler;
    
    private MainHook() {
        audioFileEngine = new AudioFileEngine();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized MainHook getInstance() {
        if (instance == null) {
            instance = new MainHook();
        }
        return instance;
    }
    
    public void init(XC_LoadPackage.LoadPackageParam param) {
        this.loadPackageParam = param;
        
        try {
            PrefsHelper prefsHelper = new PrefsHelper(getModuleContext());
            prefsHelper.loadConfigToManager();
        } catch (Exception e) {
            Log.e(TAG, "Error loading config: " + e.getMessage());
        }
        
        hookAudioRecord();
        
        Log.i(TAG, "MicInjector hook initialized for package: " + param.packageName);
    }
    
    private Context getModuleContext() {
        try {
            return loadPackageParam.packageInfo.applicationInfo.context;
        } catch (Exception e) {
            try {
                return XposedHelpers.callMethod(
                    XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.app.ActivityThread", null),
                        "currentActivityThread"
                    ),
                    "getSystemContext"
                );
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    private void hookAudioRecord() {
        try {
            Class<?> audioRecordClass = XposedHelpers.findClass("android.media.AudioRecord", null);
            
            XposedBridge.hookAllConstructors(audioRecordClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    handleAudioRecordCreation(param);
                }
            });
            
            Method[] methods = audioRecordClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals("read") && 
                    (method.getParameterTypes().length >= 3 && 
                     method.getParameterTypes()[0] == byte[].class)) {
                    
                    XposedBridge.hookMethod(method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            handleAudioRead(param);
                        }
                    });
                }
            }
            
            Log.i(TAG, "AudioRecord hooks installed");
            
        } catch (Exception e) {
            Log.e(TAG, "Error hooking AudioRecord: " + e.getMessage());
        }
    }
    
    private void handleAudioRecordCreation(XC_MethodHook.MethodHookParam param) {
        try {
            int audioSource = -1;
            
            if (param.args.length > 0 && param.args[0] instanceof Integer) {
                audioSource = (Integer) param.args[0];
            }
            
            if (audioSource == MediaRecorder.AudioSource.MIC || 
                audioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION ||
                audioSource == MediaRecorder.AudioSource.VOICE_RECOGNITION) {
                
                String currentProcess = getCurrentProcessName();
                
                if (currentProcess != null && 
                    ConfigManager.getInstance().isPackageTargeted(currentProcess)) {
                    
                    currentTargetPackage = currentProcess;
                    
                    if (ConfigManager.getInstance().isEnabled() && !isHookActive) {
                        isHookActive = true;
                        startInjection();
                        Log.i(TAG, "Hook activated for: " + currentProcess);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleAudioRecordCreation: " + e.getMessage());
        }
    }
    
    private void handleAudioRead(XC_MethodHook.MethodHookParam param) {
        if (!isHookActive || !isInjecting) {
            return;
        }
        
        try {
            if (param.args[0] instanceof byte[] && 
                param.args[1] instanceof Integer && 
                param.args[2] instanceof Integer) {
                
                byte[] buffer = (byte[]) param.args[0];
                int offset = (Integer) param.args[1];
                int length = (Integer) param.args[2];
                
                if (injectedAudioData != null && injectedDataOffset < injectedAudioData.length) {
                    int bytesToCopy = Math.min(length, injectedAudioData.length - injectedDataOffset);
                    
                    ByteBuffer.wrap(buffer)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .position(offset);
                    
                    for (int i = 0; i < bytesToCopy; i++) {
                        buffer[offset + i] = injectedAudioData[injectedDataOffset + i];
                    }
                    
                    injectedDataOffset += bytesToCopy;
                    
                    if (injectedDataOffset >= injectedAudioData.length) {
                        injectedDataOffset = 0;
                    }
                    
                    param.setResult(bytesToCopy);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in handleAudioRead: " + e.getMessage());
        }
    }
    
    private void startInjection() {
        mainHandler.post(() -> {
            try {
                int source = ConfigManager.getInstance().getAudioSource();
                
                switch (source) {
                    case ConfigManager.SOURCE_FILE:
                        String filePath = ConfigManager.getInstance().getAudioFilePath();
                        if (filePath != null && !filePath.isEmpty()) {
                            File audioFile = new File(filePath);
                            if (audioFile.exists()) {
                                injectFromFile(filePath);
                            }
                        }
                        break;
                        
                    case ConfigManager.SOURCE_SYSTEM_CAPTURE:
                        startSystemCapture();
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting injection: " + e.getMessage());
            }
        });
    }
    
    private void injectFromFile(String filePath) {
        try {
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: " + filePath);
                return;
            }
            
            byte[] fileData = readAudioFile(filePath);
            if (fileData != null && fileData.length > 0) {
                injectedAudioData = fileData;
                injectedDataOffset = 0;
                isInjecting = true;
                
                int playMode = ConfigManager.getInstance().getPlayMode();
                
                switch (playMode) {
                    case ConfigManager.MODE_ONCE:
                        audioFileEngine.startPlaybackFile(filePath);
                        break;
                        
                    case ConfigManager.MODE_LOOP:
                        audioFileEngine.startPlaybackFile(filePath);
                        audioFileEngine.setLoopEnabled(true);
                        break;
                        
                    case ConfigManager.MODE_PTT:
                        audioFileEngine.setPttPressed(true);
                        break;
                }
                
                Log.i(TAG, "Started injecting audio from: " + filePath);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error injecting from file: " + e.getMessage());
        }
    }
    
    private byte[] readAudioFile(String filePath) {
        try {
            File file = new File(filePath);
            byte[] data = new byte[(int) file.length()];
            
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            fis.read(data);
            fis.close();
            
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Error reading audio file: " + e.getMessage());
            return null;
        }
    }
    
    private void startSystemCapture() {
        Log.i(TAG, "System capture mode started");
    }
    
    private String getCurrentProcessName() {
        try {
            int pid = Binder.getCallingPid();
            ActivityManager am = (ActivityManager) loadPackageParam.packageInfo.applicationInfo.context
                .getSystemService(Context.ACTIVITY_SERVICE);
            
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo process : processes) {
                        if (process.pid == pid) {
                            return process.processName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting process name: " + e.getMessage());
        }
        return null;
    }
    
    public void stopInjection() {
        isInjecting = false;
        isHookActive = false;
        
        if (audioFileEngine != null) {
            audioFileEngine.stopPlayback();
            audioFileEngine.stopRecording();
            audioFileEngine.release();
        }
        
        injectedAudioData = null;
        injectedDataOffset = 0;
        currentTargetPackage = null;
        
        Log.i(TAG, "Injection stopped");
    }
    
    public void updateConfig() {
        try {
            PrefsHelper prefsHelper = new PrefsHelper(getModuleContext());
            prefsHelper.loadConfigToManager();
            
            if (audioFileEngine != null) {
                audioFileEngine.setVolume(ConfigManager.getInstance().getVolume());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating config: " + e.getMessage());
        }
    }
    
    public boolean isHookActive() {
        return isHookActive;
    }
    
    public boolean isInjecting() {
        return isInjecting;
    }
    
    public String getCurrentTargetPackage() {
        return currentTargetPackage;
    }
}
