package org.bunnys;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles real-time audio capture and visualization

 * 1. Selects an audio input device (microphone)
 * 2. Continuously captures live sound
 * 3. Converts raw audio into a waveform for visualization
 * 4. Provides recording functionality to save audio as a .wav file
 */
public class AudioHandler implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(AudioHandler.class.getName());

    // Buffer size affects latency and performance
    private static final int BUFFER_SIZE = 4096;

    // Default filename for saved recordings
    private static final String DEFAULT_RECORDING_FILENAME = "recording.wav";

    private final AudioFormat audioFormat;   // Defines the audio sampling settings
    private final WaveformDisplay display;  // Handles real-time visualization
    private final AtomicBoolean isRecording;

    private TargetDataLine targetLine;      // Microphone input stream
    private final ByteArrayOutputStream audioStream;
    private Thread captureThread;
    private volatile boolean isCapturing;

    /**
     * Initializes the audio system and starts capturing sound

     * - Detects available audio input devices
     * - Selects the first available microphone
     * - Starts capturing and visualizing audio in real-time
     */
    public AudioHandler(WaveformDisplay display) {
        this.display = display;
        this.audioFormat = createAudioFormat();  // Standard 44.1 kHz, 16-bit, Mono PCM
        this.isRecording = new AtomicBoolean(false);
        List<Mixer.Info> inputDevices = initializeInputDevices();
        this.audioStream = new ByteArrayOutputStream();

        if (inputDevices.isEmpty()) {
            throw new IllegalStateException("No compatible audio input devices found");
        }

        display.addDeviceSelector(inputDevices, this);
        display.addRecordButtons(this);
        startCapture(inputDevices.get(0));  // Automatically start with the first detected mic
    }

    /**
     * Defines the audio format settings:

     * - 44.1 kHz sample rate (CD quality)
     * - 16-bit depth (high-quality digital representation)
     * - Mono (single channel, most microphones use this by default)
     * - Signed and Big Endian for standard PCM data representation
     */
    private AudioFormat createAudioFormat() {
        return new AudioFormat(
                44100.0f,  // Sample rate (Hz) - higher means more accurate sound capture
                16,        // Bits per sample - determines audio resolution
                1,         // 1 channel (mono)
                true,      // Signed (allows positive and negative values)
                true       // Big endian (data stored in most significant byte first)
        );
    }

    /**
     * Detects available input devices (microphones) that support the desired format
     * Returns a list of supported devices
     */
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

    /**
     * Starts capturing audio from the selected microphone

     * - Stops any ongoing capture first
     * - Opens and starts a new audio stream
     * - Begins reading and processing sound in real-time
     */
    public void startCapture(Mixer.Info selectedDevice) {
        if (selectedDevice == null) {
            throw new IllegalArgumentException("Selected device cannot be null");
        }

        stopCapture(); // Ensure no duplicate captures

        try {
            setupTargetLine(selectedDevice);  // Configure microphone
            startCaptureThread();  // Begin streaming audio data
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.SEVERE, "Failed to start capture", e);
            throw new RuntimeException("Failed to start audio capture", e);
        }
    }

    /**
     * Configures the microphone to capture audio

     * - Ensures the microphone supports our desired format
     * - Opens and starts the microphone stream
     */
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

    /**
     * Creates a separate thread to continuously read audio data
     */
    private void startCaptureThread() {
        isCapturing = true;
        captureThread = new Thread(this::captureAudio, "AudioCaptureThread");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    /**
     * Continuously reads microphone data and processes it in real-time
     p
     * - Reads data in small chunks (buffered processing)
     * - Converts raw byte data into audio waveform values
     * - Feeds waveform data to the display
     */
    private void captureAudio() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (isCapturing && targetLine.isOpen()) {
            int bytesRead = targetLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                processAudioData(buffer, bytesRead);
            }
        }
    }

    /**
     * Converts raw audio data into a waveform for visualization

     * - Extracts amplitude values from the buffer
     * - Normalizes the data for display
     */
    private void processAudioData(byte[] buffer, int bytesRead) {
        if (isRecording.get()) {
            synchronized (audioStream) {
                audioStream.write(buffer, 0, bytesRead);
            }
        }

        // Convert byte buffer into an array of amplitude values
        float[] samples = new float[bytesRead / 2]; // Each sample is 2 bytes (16-bit audio)
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (buffer[2 * i] << 8) | (buffer[2 * i + 1] & 0xFF);
        }

        display.updateWaveform(samples); // Update visualization with new audio data
    }

    /**
     * Begins recording audio to memory
     */
    public void startRecording() {
        synchronized (audioStream) {
            audioStream.reset();
        }
        isRecording.set(true);
        display.updateRecordButton(true);
    }

    /**
     * Stops recording and saves the captured audio to a WAV file
     */
    public void stopRecording() {
        isRecording.set(false);
        saveRecording();
        display.updateRecordButton(false);
    }

    /**
     * Saves recorded audio data into a standard WAV file
     */
    private void saveRecording() {
        byte[] audioData;
        synchronized (audioStream) {
            audioData = audioStream.toByteArray();
        }

        File outputFile = new File(DEFAULT_RECORDING_FILENAME);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
             AudioInputStream ais = new AudioInputStream(bais, audioFormat,
                     audioData.length / audioFormat.getFrameSize())) {

            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            LOGGER.info("Recording saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save recording", e);
            throw new RuntimeException("Failed to save recording", e);
        }
    }

    /**
     * Stops audio capture and releases resources
     */
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

    @Override
    public void close() {
        stopCapture();
    }

    /**
     * Returns the current audio format settings
     */
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }
}
