package org.bunnys;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AudioHandler implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AudioHandler.class.getName());
    private static final int BUFFER_SIZE = 4096;
    private static final String DEFAULT_RECORDING_FILENAME = "recording.wav";

    private final AudioFormat audioFormat;
    private final WaveformDisplay display;
    private final AtomicBoolean isRecording;

    private TargetDataLine targetLine;
    private final ByteArrayOutputStream audioStream;
    private Thread captureThread;
    private volatile boolean isCapturing;
    private volatile float[] audioBuffer;

    public AudioHandler(WaveformDisplay display) {
        this.display = display;
        this.audioFormat = createAudioFormat();
        this.isRecording = new AtomicBoolean(false);
        this.audioStream = new ByteArrayOutputStream();

        List<Mixer.Info> inputDevices = initializeInputDevices();
        if (inputDevices.isEmpty()) {
            throw new IllegalStateException("No compatible audio input devices found");
        }

        display.addDeviceSelector(inputDevices, this);
        display.addRecordButtons(this);
        startCapture(inputDevices.getFirst());
    }

    private AudioFormat createAudioFormat() {
        return new AudioFormat(44100.0f, 16, 1, true, true);
    }

    private List<Mixer.Info> initializeInputDevices() {
        List<Mixer.Info> devices = new ArrayList<>();
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, audioFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(targetInfo)) {
                devices.add(mixerInfo);
            }
        }
        return devices;
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
        byte[] buffer = new byte[BUFFER_SIZE];
        while (isCapturing && targetLine.isOpen()) {
            int bytesRead = targetLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                processAudioData(buffer, bytesRead);
            }
        }
    }

    private synchronized void processAudioData(byte[] buffer, int bytesRead) {
        if (isRecording.get()) {
            synchronized (audioStream) {
                audioStream.write(buffer, 0, bytesRead);
            }
        }

        float[] samples = new float[bytesRead / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (buffer[2 * i] << 8) | (buffer[2 * i + 1] & 0xFF);
        }

        this.audioBuffer = samples;
        display.updateWaveform(samples);
    }

    public void startRecording() {
        synchronized (audioStream) {
            audioStream.reset();
        }
        isRecording.set(true);
        display.updateRecordButton(true);
    }

    public void stopRecording() {
        isRecording.set(false);
        saveRecording();
        display.updateRecordButton(false);
    }

    private void saveRecording() {
        byte[] audioData;
        synchronized (audioStream) {
            audioData = audioStream.toByteArray();
        }

        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(audioData), audioFormat, audioData.length / audioFormat.getFrameSize())) {
            File outputFile = new File(DEFAULT_RECORDING_FILENAME);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            LOGGER.info("Recording saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save recording", e);
        }
    }

    public void stopCapture() {
        isCapturing = false;
        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
    }

    public void applyLowPassFilter() {
        synchronized (this) {
            if (audioBuffer == null || audioBuffer.length == 0) {
                LOGGER.warning("LPF: No audio data to process.");
                return;
            }

            LOGGER.info("Applying LPF...");
            float alpha = 0.1f;
            float[] filtered = new float[audioBuffer.length];
            filtered[0] = audioBuffer[0];

            for (int i = 1; i < audioBuffer.length; i++) {
                filtered[i] = alpha * audioBuffer[i] + (1 - alpha) * filtered[i - 1];
            }

            this.audioBuffer = filtered;
            display.updateWaveform(filtered);
        }
    }

    @Override
    public void close() {
        stopCapture();
    }
}
