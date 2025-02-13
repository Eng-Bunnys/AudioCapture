package org.bunnys;

import javax.sound.sampled.*;

public class AudioHandler {
    private TargetDataLine mic;
    private WaveformDisplay display;

    public AudioHandler(WaveformDisplay display) {
        this.display = display;
    }

    public void start() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            mic = (TargetDataLine) AudioSystem.getLine(info);
            mic.open(format);
            mic.start();

            byte[] buffer = new byte[1024];
            while (true) {
                int bytesRead = mic.read(buffer, 0, buffer.length);
                int sampleCount = bytesRead / 2;
                float[] samples = new float[sampleCount];

                for (int i = 0; i < sampleCount; i++) {
                    samples[i] = (buffer[2 * i] << 8) | (buffer[2 * i + 1] & 0xFF);
                }

                if (samples.length > 1) {
                    samples = new float[]{samples[0]};
                }

                display.updateWaveform(samples);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
