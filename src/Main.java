package org.bunnys;

import org.bunnys.audio.AudioFormatFactory;
import org.bunnys.audio.device.AudioDeviceManager;
import org.bunnys.constants.AudioConstants;
import org.bunnys.ui.MainWindow;

import javax.sound.sampled.AudioFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> LOGGER.log(Level.SEVERE,
                "Uncaught exception in thread " + thread.getName(), throwable));

        try {
            MainWindow mainWindow = getMainWindow();

            // Add shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Application shutting down... [From Main]");
                // Clean up the resources in the main window
                mainWindow.cleanupAndExit();
                mainWindow.dispose();  // Dispose of the window
            }));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Application failed to start", e);
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static MainWindow getMainWindow() {
        AudioFormat format = AudioFormatFactory.createDefaultFormat();
        AudioDeviceManager deviceManager = new AudioDeviceManager(format);

        if (deviceManager.getInputDevices().isEmpty())
            throw new IllegalStateException("No compatible audio input devices found");

        // Create the main window
        MainWindow mainWindow = new MainWindow(
                format,
                deviceManager,
                AudioConstants.DEFAULT_SAMPLE_SIZE);
        return mainWindow;
    }
}