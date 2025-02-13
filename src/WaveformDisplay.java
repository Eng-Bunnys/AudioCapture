//To-do: Complete docs lol

package org.bunnys;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Millisecond;

import javax.sound.sampled.Mixer;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Displays real-time audio waveform and provides UI controls for audio capture
 */
public class WaveformDisplay {
    private static final Logger LOGGER = Logger.getLogger(WaveformDisplay.class.getName());
    private static final String WINDOW_TITLE = "Bunnys's Audio Waveform Display";
    private static final int DEFAULT_WINDOW_WIDTH = 800;
    private static final int DEFAULT_WINDOW_HEIGHT = 500;

    private final DynamicTimeSeriesCollection dataset;
    private final JFrame frame;
    private final JButton recordButton;
    private final JButton stopButton;

    /**
     * Creates a new WaveformDisplay with the specified sample size
     *
     * @param sampleSize The number of samples to display
     */
    public WaveformDisplay(int sampleSize) {
        this.dataset = createDataset(sampleSize);
        this.frame = createMainFrame();
        this.recordButton = new JButton("Record");
        this.stopButton = new JButton("Stop Recording");

        initializeUI();
    }

    /**
     * Creates and initializes the dataset for real-time waveform display
     *
     * @param sampleSize The number of samples in the dataset
     * @return The initialized DynamicTimeSeriesCollection dataset
     */
    private DynamicTimeSeriesCollection createDataset(int sampleSize) {
        DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(1, sampleSize, new Millisecond());
        dataset.setTimeBase(new Millisecond());
        dataset.addSeries(new float[sampleSize], 0, "Audio Waveform");
        return dataset;
    }

    /**
     * Creates and configures the main application window
     *
     * @return The initialized JFrame
     */
    private JFrame createMainFrame() {
        JFrame frame = new JFrame(WINDOW_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);

        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("icon.png")));
        if (icon.getImage() != null)
            frame.setIconImage(icon.getImage());
        else
            LOGGER.warning("Icon not found: Make sure icon.png is in the resources folder.");

        return frame;
    }

    /**
     * Initializes the user interface components
     */
    private void initializeUI() {
        JFreeChart chart = createChart();
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(chartPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    /**
     * Creates and configures the real-time waveform chart
     *
     * @return The configured JFreeChart instance
     */
    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Real-Time Waveform",
                "Time",
                "Amplitude",
                dataset,
                false, true, false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRangeAxis().setRange(-32768, 32767);
        return chart;
    }

    /**
     * Adds device selection controls to the display
     *
     * @param inputDevices List of available input devices
     * @param handler The AudioHandler instance
     */
    public void addDeviceSelector(List<Mixer.Info> inputDevices, AudioHandler handler) {
        JPanel topPanel = new JPanel(new FlowLayout());

        JLabel label = new JLabel("Select Input Device:");
        JComboBox<String> deviceList = new JComboBox<>(
                inputDevices.stream()
                        .map(Mixer.Info::getName)
                        .toArray(String[]::new)
        );

        deviceList.addActionListener(e -> {
            int selectedIndex = deviceList.getSelectedIndex();
            if (selectedIndex >= 0) {
                handler.startCapture(inputDevices.get(selectedIndex));
            }
        });

        topPanel.add(label);
        topPanel.add(deviceList);
        frame.add(topPanel, BorderLayout.NORTH);
        frame.revalidate();
    }

    /**
     * Adds recording control buttons to the display
     *
     * @param handler The AudioHandler instance
     */
    public void addRecordButtons(AudioHandler handler) {
        JPanel buttonPanel = new JPanel(new FlowLayout());

        recordButton.addActionListener(e -> handler.startRecording());
        stopButton.addActionListener(e -> handler.stopRecording());
        stopButton.setEnabled(false);

        buttonPanel.add(recordButton);
        buttonPanel.add(stopButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.revalidate();
    }

    /**
     * Updates the record button state and text
     *
     * @param isRecording true if recording is in progress
     */
    public void updateRecordButton(boolean isRecording) {
        recordButton.setText(isRecording ? "Recording..." : "Record");
        recordButton.setEnabled(!isRecording);
        stopButton.setEnabled(isRecording);
    }

    /**
     * Updates the waveform display with new audio samples
     *
     * @param samples Array of audio samples to display
     */
    public void updateWaveform(float[] samples) {
        if (samples.length > 0) {
            dataset.advanceTime();
            dataset.appendData(new float[]{samples[0]});
        }
    }
}
