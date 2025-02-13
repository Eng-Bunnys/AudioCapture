package org.bunnys;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Millisecond;

import javax.swing.*;
import java.awt.*;

public class WaveformDisplay {
    private DynamicTimeSeriesCollection dataset;
    private JFreeChart chart;

    public WaveformDisplay(int sampleSize) {
        dataset = new DynamicTimeSeriesCollection(1, sampleSize, new Millisecond());
        dataset.setTimeBase(new Millisecond());
        dataset.addSeries(new float[sampleSize], 0, "Audio Waveform");

        chart = ChartFactory.createTimeSeriesChart(
                "Real-Time Waveform",
                "Time",
                "Amplitude",
                dataset,
                false, true, false
        );

        // Fix the Y-axis range to avoid constant rescaling
        XYPlot plot = chart.getXYPlot();
        plot.getRangeAxis().setRange(-32768, 32767);

        JFrame frame = new JFrame("Waveform Display");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(new ChartPanel(chart), BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }

    public void updateWaveform(float[] samples) {
        dataset.advanceTime();
        dataset.appendData(samples);
    }
}