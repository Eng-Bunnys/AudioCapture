package org.bunnys.audio;

import org.bunnys.audio.processing.AudioProcessor;
import org.bunnys.constants.AudioConstants;
import org.bunnys.audio.recording.AudioRecorder;

import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AudioHandler implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AudioHandler.class.getName());

    private final AudioFormat audioFormat;
    private final AudioRecorder recorder;
    private final Consumer<float[]> waveformUpdateCallback;
    private final AtomicBoolean isRecording;

    private TargetDataLine targetLine;
    private Thread captureThread;
    private volatile boolean isCapturing;
    private volatile float[] audioBuffer;
    private AudioProcessor currentProcessor; // Add a variable to hold the current processor

    public AudioHandler(AudioFormat audioFormat, Consumer<float[]> waveformUpdateCallback) {
        this.audioFormat = audioFormat;
        this.waveformUpdateCallback = waveformUpdateCallback;
        this.recorder = new AudioRecorder(audioFormat);
        this.isRecording = new AtomicBoolean(false);
        this.currentProcessor = null;  // Initially no processor
    }

    public void startCapture(Mixer.Info selectedDevice) {
        if (selectedDevice == null) {
            throw new IllegalArgumentException("Selected device cannot be null");
        }
        stopCapture();

        try {
            setupTargetLine(selectedDevice);
            startCaptureThread();
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.SEVERE, "Failed to start capture", e);
        }
    }

    private void setupTargetLine(Mixer.Info selectedDevice) throws LineUnavailableException {
        Mixer mixer = AudioSystem.getMixer(selectedDevice);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        if (!mixer.isLineSupported(targetInfo)) {
            throw new LineUnavailableException("Selected device does not support the required format");
        }

        targetLine = (TargetDataLine) mixer.getLine(targetInfo);
        targetLine.open(audioFormat);
        targetLine.start();
    }

    private void startCaptureThread() {
        isCapturing = true;
        captureThread = new Thread(this::captureAudio, "AudioCaptureThread");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void captureAudio() {
        byte[] buffer = new byte[AudioConstants.BUFFER_SIZE];
        while (isCapturing && targetLine.isOpen()) {
            int bytesRead = targetLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                processAudioData(buffer, bytesRead);
            }
        }
    }

    private synchronized void processAudioData(byte[] buffer, int bytesRead) {
        if (isRecording.get()) {
            recorder.writeData(buffer, bytesRead);
        }

        float[] samples = new float[bytesRead / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (buffer[2 * i] << 8) | (buffer[2 * i + 1] & 0xFF);
        }

        // Apply the processor if one is set
        if (currentProcessor != null) {
            samples = currentProcessor.process(samples);
        }

        this.audioBuffer = samples;
        waveformUpdateCallback.accept(samples);
    }

    public void startRecording() {
        recorder.reset();
        isRecording.set(true);
    }

    public void stopRecording() {
        isRecording.set(false);
        recorder.saveRecording(AudioConstants.DEFAULT_RECORDING_FILENAME);
    }

    public void applyAudioProcessor(AudioProcessor processor) {
        synchronized (this) {
            if (audioBuffer == null || audioBuffer.length == 0) {
                LOGGER.warning("Audio processor: No audio data to process.");
                return;
            }

            if (this.currentProcessor != processor) {
                this.currentProcessor = processor; // Store the processor
                float[] processed = processor.process(audioBuffer);
                this.audioBuffer = processed;
                waveformUpdateCallback.accept(processed);
            }
        }
    }

    public void removeAudioProcessor() {
        synchronized (this) {
            this.currentProcessor = null; // Disable the processor
        }
    }

    public void stopCapture() {
        isCapturing = false;
        if (captureThread != null) {
            captureThread.interrupt(); // Interrupt the capture thread
            try {
                captureThread.join(1000); // Wait for the thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
    }

    @Override
    public void close() {
        stopCapture();
    }
}
