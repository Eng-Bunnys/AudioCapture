package org.bunnys;

public class Main {
    public static void main(String[] args) {
        int sampleSize = 100;
        WaveformDisplay display = new WaveformDisplay(sampleSize);
        AudioHandler audioHandler = new AudioHandler(display);
        audioHandler.start();
    }
}