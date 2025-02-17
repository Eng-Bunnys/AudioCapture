package org.bunnys.audio;

import javax.sound.sampled.AudioFormat;

public class AudioFormatFactory {
    public static AudioFormat createDefaultFormat() {
        return new AudioFormat(44100.0f, 16, 1, true, true);
    }
}