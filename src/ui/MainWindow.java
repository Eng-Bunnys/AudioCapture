package org.bunnys.ui;

import org.bunnys.audio.device.AudioDeviceManager;
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

    // LPF stuff
    private JSlider alphaSlider;
    private JLabel alphaLabel;
    private LowPassFilter lpf;

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
        setupLPFControls();
        setupButtons();

        // Start with first available device
        List<Mixer.Info> devices = deviceManager.getInputDevices();
        if (!devices.isEmpty()) {
            audioHandler.startCapture(devices.getFirst());
        }

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

        setApplicationIcon(frame);

        return frame;
    }

    private void setApplicationIcon(JFrame frame) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(
                getClass().getClassLoader().getResource("icon.png")));

        if (icon.getImage() != null)
            frame.setIconImage(icon.getImage());
         else
            LOGGER.warning("Icon not found: Make sure icon.png is in the resources folder.");
    }

    private void initializeUI() {
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

            if (selectedIndex >= 0)
                audioHandler.startCapture(inputDevices.get(selectedIndex));
        });

        topPanel.add(label);
        topPanel.add(deviceList);
        frame.add(topPanel, BorderLayout.NORTH);
    }

    // LPF Logic

    private void setupLPFControls() {
        JPanel lpfPanel = new JPanel(new FlowLayout());
        lpfPanel.setBorder(BorderFactory.createTitledBorder("LPF Controls"));

        // Slider for alpha adjustment (0% to 100%)
        // For later, to prevent freezing alpha will be set to 0.01 even if the user wants 0
        alphaSlider = new JSlider(0, 100, 10); // Min 0, Max 100, Default 10 (0.1f)
        alphaLabel = new JLabel("Alpha: 0.10");

        alphaSlider.addChangeListener(e -> {
            float alphaValue = alphaSlider.getValue() / 100.0f;
            alphaLabel.setText(String.format("Alpha: %.2f", alphaValue));
            if (lpf != null) {
                lpf.setAlpha(alphaValue); // Update LPF dynamically
            }
        });

        lpfPanel.add(alphaLabel);
        lpfPanel.add(alphaSlider);
        frame.add(lpfPanel, BorderLayout.EAST); // Add to UI
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
     //   final AudioProcessor[] lpf = {new LowPassFilter(0.1f)};
        // Testing out custom alpha values, 0.1 => aggressive filtering, 0.9 less filtering (gentle smoothing)
//        lpfButton.addActionListener(e -> {
//            // Toggle LPF
//            if (lpfButton.getText().equals("Apply LPF")) {
//                audioHandler.applyAudioProcessor(lpf[0]);
//                lpfButton.setText("Disable LPF");
//            } else {
//                audioHandler.removeAudioProcessor();
//                lpfButton.setText("Apply LPF");
//            }
//        });

        lpfButton.addActionListener(e -> {
            if (lpfButton.getText().equals("Apply LPF")) {
                float alpha = alphaSlider.getValue() / 100.0f;
                // To prevent freezing, to read more about freezing its under the listener
                alpha = (float) Math.max(alpha, 0.01);
                lpf = new LowPassFilter(alpha);
                audioHandler.applyAudioProcessor(lpf);
                lpfButton.setText("Disable LFP");
            } else {
                audioHandler.removeAudioProcessor();
                lpfButton.setText("Apply LPF");
            }
        });

        /* What is freezing
         * LPF formula is filtered[i] = alpha * input[i] + (1 - alpha) * filtered[i-1]
         * so when alpha = 0, it becomes filtered[i-1], so every output is the same as the prev. one
         * this is mathematically correct as far as I know but this is not useful for real-time processing
         * in the real world, alpha should never be 0, as it would be unusable as you can see
         * */

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
