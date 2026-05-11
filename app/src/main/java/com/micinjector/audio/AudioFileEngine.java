package com.micinjector.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

import com.micinjector.config.ConfigManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioFileEngine {
    
    private static final String TAG = "AudioFileEngine";
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread playbackThread;
    private volatile boolean isPlaying = false;
    private volatile boolean isRecording = false;
    private volatile boolean pttPressed = false;
    
    private int bufferSize;
    private byte[] audioBuffer;
    
    private String currentFilePath;
    
    public interface PlaybackListener {
        void onPlaybackStarted();
        void onPlaybackStopped();
        void onPlaybackError(String error);
    }
    
    private PlaybackListener listener;
    
    public AudioFileEngine() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = 4096;
        }
        audioBuffer = new byte[bufferSize];
    }
    
    public void setPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }
    
    public boolean startRecording() {
        if (isRecording) {
            return true;
        }
        
        try {
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            );
            
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return false;
            }
            
            audioRecord.startRecording();
            isRecording = true;
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
            return false;
        }
    }
    
    public void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping recording: " + e.getMessage());
            }
            audioRecord = null;
        }
    }
    
    public byte[] readAudioData() {
        if (audioRecord == null || !isRecording) {
            return null;
        }
        
        int bytesRead = audioRecord.read(audioBuffer, 0, bufferSize);
        if (bytesRead > 0) {
            byte[] data = new byte[bytesRead];
            System.arraycopy(audioBuffer, 0, data, 0, bytesRead);
            return data;
        }
        return null;
    }
    
    public void setPttPressed(boolean pressed) {
        this.pttPressed = pressed;
        if (pressed) {
            startRecording();
        } else {
            stopRecording();
        }
    }
    
    public boolean startPlaybackFile(String filePath) {
        if (isPlaying) {
            stopPlayback();
        }
        
        this.currentFilePath = filePath;
        
        playbackThread = new Thread(() -> {
            FileInputStream fis = null;
            try {
                File audioFile = new File(filePath);
                if (!audioFile.exists()) {
                    if (listener != null) {
                        listener.onPlaybackError("Audio file not found");
                    }
                    return;
                }
                
                fis = new FileInputStream(audioFile);
                isPlaying = true;
                
                if (listener != null) {
                    listener.onPlaybackStarted();
                }
                
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
                
                AudioFormat audioFormat = new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AUDIO_FORMAT)
                    .build();
                
                audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
                
                audioTrack.play();
                
                float volume = ConfigManager.getInstance().getVolume();
                audioTrack.setVolume(volume);
                
                int bytesRead;
                while (isPlaying && (bytesRead = fis.read(audioBuffer)) != -1) {
                    if (pttPressed) {
                        continue;
                    }
                    audioTrack.write(audioBuffer, 0, bytesRead);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Playback error: " + e.getMessage());
                if (listener != null) {
                    listener.onPlaybackError(e.getMessage());
                }
            } finally {
                isPlaying = false;
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing file: " + e.getMessage());
                    }
                }
                if (audioTrack != null) {
                    try {
                        audioTrack.stop();
                        audioTrack.release();
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping audio track: " + e.getMessage());
                    }
                    audioTrack = null;
                }
                if (listener != null) {
                    listener.onPlaybackStopped();
                }
            }
        });
        
        playbackThread.start();
        return true;
    }
    
    public void stopPlayback() {
        isPlaying = false;
        if (playbackThread != null) {
            try {
                playbackThread.join(500);
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping playback");
            }
            playbackThread = null;
        }
    }
    
    public void writeToAudioTrack(byte[] data) {
        if (audioTrack != null && isPlaying) {
            audioTrack.write(data, 0, data.length);
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public boolean isRecording() {
        return isRecording;
    }
    
    public void setVolume(float volume) {
        if (audioTrack != null) {
            audioTrack.setVolume(Math.max(0f, Math.min(1f, volume)));
        }
    }
    
    public void release() {
        stopRecording();
        stopPlayback();
    }
}
