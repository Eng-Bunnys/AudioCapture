package org.bunnys.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Millisecond;

import javax.swing.BorderFactory;

public class WaveformChart {
    private final DynamicTimeSeriesCollection dataset;
    private final JFreeChart chart;

    public WaveformChart(int sampleSize) {
        this.dataset = createDataset(sampleSize);
        this.chart = createChart();
    }

    private DynamicTimeSeriesCollection createDataset(int sampleSize) {
        DynamicTimeSeriesCollection dataset = new DynamicTimeSeriesCollection(1, sampleSize, new Millisecond());

        dataset.setTimeBase(new Millisecond());

        dataset.addSeries(new float[sampleSize], 0, "Audio Waveform");
        return dataset;
    }

    private JFreeChart createChart() {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Real-Time Waveform",
                "Time",
                "Amplitude",
                dataset,
                false, true, false);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRangeAxis().setRange(-32768, 32767);
        return chart;
    }

    public ChartPanel createChartPanel() {
        ChartPanel panel = new ChartPanel(chart);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    public void updateData(float[] samples) {
        if (samples != null && samples.length > 0) {
            dataset.advanceTime();
            dataset.appendData(new float[] { samples[0] });
        }
    }
}