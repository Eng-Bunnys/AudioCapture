package org.bunnys.audio.device;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

public class AudioDeviceManager {
    private final AudioFormat format;

    public AudioDeviceManager(AudioFormat format) {
        this.format = format;
    }

    public List<Mixer.Info> getInputDevices() {
        List<Mixer.Info> devices = new ArrayList<>();
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(targetInfo))
                devices.add(mixerInfo);
        }
        return devices;
    }

    public TargetDataLine getTargetLine(Mixer.Info deviceInfo) throws LineUnavailableException {
        Mixer mixer = AudioSystem.getMixer(deviceInfo);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);
        return (TargetDataLine) mixer.getLine(targetInfo);
    }
}
