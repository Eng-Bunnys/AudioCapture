package org.bunnys.audio.recording;

import javax.sound.sampled.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class AudioRecorder {
    private static final Logger LOGGER = Logger.getLogger(AudioRecorder.class.getName());
    private final ByteArrayOutputStream audioStream;
    private final AudioFormat format;

    public AudioRecorder(AudioFormat format) {
        this.format = format;
        this.audioStream = new ByteArrayOutputStream();
    }

    public void writeData(byte[] buffer, int bytesRead) {
        synchronized (audioStream) {
            audioStream.write(buffer, 0, bytesRead);
        }
    }

    public void reset() {
        synchronized (audioStream) {
            audioStream.reset();
        }
    }

    public void saveRecording(String filename) {
        byte[] audioData;
        synchronized (audioStream) {
            audioData = audioStream.toByteArray();
        }

        try (AudioInputStream ais = new AudioInputStream(
                new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize())) {
            File outputFile = new File(filename);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            LOGGER.info("Recording saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save recording", e);
        }
    }
}