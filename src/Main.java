/* Written by Bunnys
   => I tried my best to explain every step but I neither a Java nor Audio expert

What you might benefit from this:
=> Resource Management
=> Thread Management & Interruption Handling
=> Synchronized access to shared resources
=> Proper cleanup

Don't think you'll learn much about signal processing from this code, especially as of Feb. 2025
I do intend on adding signal processing later on, just not right now

Plans:
1. Filtering
2. FFT
3. Freq. Spectrum Visualization
4. Save Audio file [Done]
5. Voice Activity Detection (VAD)
6. Audio Compression
7. Vocal Isolation
8. Multi-track recording
9. MIDI conversion

Will I actually do all of these? Probably not, but I do intend to do some
**/

package org.bunnys;

public class Main {
    // A larger sample size provides more data / frame but may add delay
    private static final int DEFAULT_SAMPLE_SIZE = 100;

    public static void main(String[] args) {
        try {
            // Creates the display that shows the waveform representation of the audio signal
            // A waveform is a graphical representation of how a signal's amplitude varies over time
            WaveformDisplay display = new WaveformDisplay(DEFAULT_SAMPLE_SIZE);
            /* Initializes the audio handler
            1. Captures audio from the default microphone
            2. Breaks the audio signal into samples
            3. Sends those samples to teh waveform display
            **/
            try (AudioHandler audioHandler = new AudioHandler(display)) {
                // Keep the application running indefinitely, this is necessary since the audio processing happens in real time
                Thread.currentThread().join();
            }
        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
