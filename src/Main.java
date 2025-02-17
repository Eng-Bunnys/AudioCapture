package org.bunnys;

public class Main {
    private static final int DEFAULT_SAMPLE_SIZE = 100;

    public static void main(String[] args) {
        try {
            WaveformDisplay display = new WaveformDisplay(DEFAULT_SAMPLE_SIZE);
            try (AudioHandler audioHandler = new AudioHandler(display)) {
                Thread.currentThread().join();
            }
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage() + "\n\n");
            e.printStackTrace();
            System.exit(1);
        }
    }
}