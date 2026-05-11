package com.micinjector.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.util.Log;

import com.micinjector.config.ConfigManager;

public class AudioSystemCaptureEngine {
    
    private static final String TAG = "AudioSystemCaptureEngine";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private Thread captureThread;
    private volatile boolean isCapturing = false;
    
    private int bufferSize;
    private byte[] audioBuffer;
    
    private MediaProjection mediaProjection;
    
    public interface CaptureListener {
        void onAudioData(byte[] data);
        void onCaptureStarted();
        void onCaptureStopped();
        void onCaptureError(String error);
    }
    
    private CaptureListener listener;
    
    public AudioSystemCaptureEngine() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = 4096;
        }
        audioBuffer = new byte[bufferSize];
    }
    
    public void setCaptureListener(CaptureListener listener) {
        this.listener = listener;
    }
    
    public void setMediaProjection(MediaProjection projection) {
        this.mediaProjection = projection;
    }
    
    public boolean startCapture() {
        if (isCapturing) {
            return true;
        }
        
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            return false;
        }
        
        try {
            AudioPlaybackCaptureConfiguration config = 
                new AudioPlaybackCaptureConfiguration(mediaProjection);
            
            AudioFormat playbackFormat = new AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AUDIO_FORMAT)
                .build();
            
            AudioRecord.Builder builder = new AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(playbackFormat)
                .setBufferSizeInBytes(bufferSize);
            
            int audioSource;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                audioSource = MediaRecorder.AudioSource.MIC;
            } else {
                audioSource = MediaRecorder.AudioSource.MIC;
            }
            
            builder.setAudioSource(audioSource);
            
            audioRecord = builder.build();
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return false;
            }
            
            audioRecord.startRecording();
            isCapturing = true;
            
            if (listener != null) {
                listener.onCaptureStarted();
            }
            
            captureThread = new Thread(() -> {
                while (isCapturing) {
                    int bytesRead = audioRecord.read(audioBuffer, 0, bufferSize);
                    if (bytesRead > 0 && listener != null) {
                        byte[] data = new byte[bytesRead];
                        System.arraycopy(audioBuffer, 0, data, 0, bytesRead);
                        listener.onAudioData(data);
                    }
                }
            });
            
            captureThread.start();
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting capture: " + e.getMessage());
            if (listener != null) {
                listener.onCaptureError(e.getMessage());
            }
            return false;
        }
    }
    
    public void stopCapture() {
        isCapturing = false;
        
        if (captureThread != null) {
            try {
                captureThread.join(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping capture");
            }
            captureThread = null;
        }
        
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord: " + e.getMessage());
            }
            audioRecord = null;
        }
        
        if (listener != null) {
            listener.onCaptureStopped();
        }
    }
    
    public boolean isCapturing() {
        return isCapturing;
    }
    
    public void release() {
        stopCapture();
    }
}
