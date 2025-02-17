package org.bunnys.ui;

import org.bunnys.audio.device.AudioDeviceManager;
import org.bunnys.audio.processing.AudioProcessor;
import org.bunnys.audio.processing.LowPassFilter;
import org.bunnys.constants.AudioConstants;
import org.bunnys.audio.AudioHandler;
import org.jfree.chart.ChartPanel;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class MainWindow {
    private static final Logger LOGGER = Logger.getLogger(MainWindow.class.getName());

    private final JFrame frame;
    private final WaveformChart waveformChart;
    private final AudioHandler audioHandler;
    private final JButton recordButton;
    private final JButton stopButton;
    private final JButton lpfButton;

    private boolean isCleanedUp = false;

    public MainWindow(AudioFormat format, AudioDeviceManager deviceManager, int sampleSize) {
        this.frame = createMainFrame();
        this.waveformChart = new WaveformChart(sampleSize);
        this.audioHandler = new AudioHandler(format, waveformChart::updateData);

        // Initialize buttons
        this.recordButton = new JButton("Record");
        this.stopButton = new JButton("Stop Recording");
        this.lpfButton = new JButton("Apply LPF");

        initializeUI();
        setupDeviceSelector(deviceManager);
        setupButtons();

        // Start with first available device
        List<Mixer.Info> devices = deviceManager.getInputDevices();
        if (!devices.isEmpty()) {
            audioHandler.startCapture(devices.get(0));
        }

        // Add window listener to handle closing
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanupAndExit();
            }
        });
    }

    private JFrame createMainFrame() {
        JFrame frame = new JFrame(AudioConstants.WINDOW_TITLE);
        frame.setLayout(new BorderLayout());
        frame.setSize(AudioConstants.DEFAULT_WINDOW_WIDTH, AudioConstants.DEFAULT_WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);

        // Set application icon
        setApplicationIcon(frame);

        return frame;
    }

    private void setApplicationIcon(JFrame frame) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(
                getClass().getClassLoader().getResource("icon.png")));

        if (icon.getImage() != null) {
            frame.setIconImage(icon.getImage());
        } else {
            LOGGER.warning("Icon not found: Make sure icon.png is in the resources folder.");
        }
    }

    private void initializeUI() {
        // Add chart panel
        ChartPanel chartPanel = waveformChart.createChartPanel();
        frame.add(chartPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private void setupDeviceSelector(AudioDeviceManager deviceManager) {
        JPanel topPanel = new JPanel(new FlowLayout());

        JLabel label = new JLabel("Select Input Device:");

        List<Mixer.Info> inputDevices = deviceManager.getInputDevices();

        JComboBox<String> deviceList = new JComboBox<>(inputDevices.stream()
                .map(Mixer.Info::getName)
                .toArray(String[]::new));

        deviceList.addActionListener(e -> {
            int selectedIndex = deviceList.getSelectedIndex();

            if (selectedIndex >= 0) {
                audioHandler.startCapture(inputDevices.get(selectedIndex));
            }
        });

        topPanel.add(label);
        topPanel.add(deviceList);
        frame.add(topPanel, BorderLayout.NORTH);
    }

    private void setupButtons() {
        JPanel buttonPanel = new JPanel(new FlowLayout());

        // Record button
        recordButton.addActionListener(e -> {
            audioHandler.startRecording();
            updateRecordButton(true);
        });

        // Stop button
        stopButton.addActionListener(e -> {
            audioHandler.stopRecording();
            updateRecordButton(false);
        });
        stopButton.setEnabled(false);

        // LPF button
        AudioProcessor lpf = new LowPassFilter(0.1f);
        lpfButton.addActionListener(e -> {
            // Toggle LPF
            if (lpfButton.getText().equals("Apply LPF")) {
                audioHandler.applyAudioProcessor(lpf);
                lpfButton.setText("Disable LPF");
            } else {
                audioHandler.removeAudioProcessor();
                lpfButton.setText("Apply LPF");
            }
        });

        buttonPanel.add(recordButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(lpfButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.revalidate();
    }

    public void updateRecordButton(boolean isRecording) {
        recordButton.setText(isRecording ? "Recording..." : "Record");
        recordButton.setEnabled(!isRecording);
        stopButton.setEnabled(isRecording);
    }

    public void dispose() {
        audioHandler.close();
        frame.dispose();
    }

    public void cleanupAndExit() {
        if (isCleanedUp)
            return;
        isCleanedUp = true;

        LOGGER.info("Shutting down the application... [From MW]");

        // Stop capturing audio
        audioHandler.stopCapture();
        audioHandler.close();

        // Dispose the window
        SwingUtilities.invokeLater(frame::dispose);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                cleanupAndExit();
                System.exit(0); // Ensure JVM exits after cleanup
            }
        });
    }
}
