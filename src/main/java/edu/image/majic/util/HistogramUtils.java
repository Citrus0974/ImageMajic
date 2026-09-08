package edu.image.majic.util;

import javafx.scene.chart.XYChart;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class HistogramUtils {
    public static XYChart.Series<Number, Number> calculateHistogramChartSeries(Mat source, ColorChannel channel, String seriesName){
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        if (source == null || source.empty()) return series;

        List<Mat> images = new ArrayList<>();
        images.add(source);

        MatOfInt channels = new MatOfInt(channel.ordinal()); // Индекс канала (0-Blue, 1-Green, 2-Red, 3-Alpha)
        Mat mask = new Mat();                                // Маска не нужна
        Mat histogram = new Mat();
        MatOfInt size = new MatOfInt(256);         // Количество "корзин" (оттенков от 0 до 255)
        MatOfFloat ranges = new MatOfFloat(0f, 256f);

        Imgproc.calcHist(images, channels, mask, histogram, size, ranges);
        float[] histData = new float[256];
        histogram.get(0, 0, histData);

        var seriesData = series.getData();
        for (int i = 0; i < 256; i++) {
            seriesData.add(new XYChart.Data<>(i, histData[i]));
        }

        mask.release();
        histogram.release();

        return series;
    }
}
