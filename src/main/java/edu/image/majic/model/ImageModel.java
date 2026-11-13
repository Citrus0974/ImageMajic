package edu.image.majic.model;

import org.opencv.core.*;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ImageModel {
    private Mat originalMat;
    private Mat currentMat;
    private File currentFile;

    public boolean loadImage(File file) {
        if (file == null || !file.exists()) return false;
        if (originalMat != null) originalMat.release();
        if (currentMat != null) currentMat.release();

        try {
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            MatOfByte matOfByte = new MatOfByte(imageBytes);
            originalMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_UNCHANGED);
            System.out.println("Loaded file " + file.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Error reading file " + file.toPath());
            return false;
        }

        if (originalMat.empty()) {
            return false;
        }

        currentMat = originalMat.clone();
        this.currentFile = file;
        return true;
    }

    public void rotateLeft() {
        if (originalMat == null || currentMat == null) return;

        Mat rotatedMat = new Mat();
        Core.rotate(currentMat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE);
        Mat oldMat = currentMat;
        currentMat = rotatedMat;
        oldMat.release();
    }

    public void rotateRight() {
        if (originalMat == null || currentMat == null) return;

        Mat rotatedMat = new Mat();
        Core.rotate(currentMat, rotatedMat, Core.ROTATE_90_CLOCKWISE);
        Mat oldMat = currentMat;
        currentMat = rotatedMat;
        oldMat.release();
    }

    public Mat getOriginalMat() {
        return originalMat;
    }

    public Mat getCurrentMat() {
        return currentMat;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void reset() {

        if (originalMat != null) {
            currentMat.release();
            currentMat = originalMat.clone();
        }
    }


    public void convertToGrayscale() {
        if (currentMat == null || currentMat.channels() == 1) return;
        switch (currentMat.channels()) {
            case 3 -> Imgproc.cvtColor(currentMat, currentMat, Imgproc.COLOR_BGR2GRAY);
            case 4 -> Imgproc.cvtColor(currentMat, currentMat, Imgproc.COLOR_BGRA2GRAY);
            default -> {
            }
        }
    }

    public void applyGradient(int xSize, int ySize, String maskType) {
        if (currentMat == null) return;
        int shape;
        switch (maskType) {
            case "Square" -> shape = Imgproc.MORPH_RECT;
            case "Cross" -> shape = Imgproc.MORPH_CROSS;
            case "Ellipse" -> shape = Imgproc.MORPH_ELLIPSE;
            default -> shape = Imgproc.MORPH_RECT;
        }
        Mat kernel = Imgproc.getStructuringElement(shape, new Size(xSize, ySize));
        Imgproc.morphologyEx(currentMat, currentMat, Imgproc.MORPH_GRADIENT, kernel);
        System.out.println("applied gradient: " + xSize + "x" + ySize + ", " + maskType);
        kernel.release();
    }

    public void applySobel(int size, String direction) {
        if (currentMat == null) return;
        int dx, dy;
        boolean both = false;
        switch (direction) {
            case "Horizontal" -> {
                dx = 0;
                dy = 1;
            }
            case "Vertical" -> {
                dx = 1;
                dy = 0;
            }
            case "Both" -> {
                dx = 0;
                dy = 1;
                both = true;
            }
            default -> {
                dx = 0;
                dy = 1;
            }
        }

        Mat newMat = new Mat();
        Imgproc.Sobel(currentMat, newMat, CvType.CV_16S, dx, dy, size);
        if (both) {
            Mat mat2 = new Mat();
            Imgproc.Sobel(currentMat, mat2, CvType.CV_16S, dy, dx, size);
            Core.addWeighted(newMat, 0.5, mat2, 0.5, 0.0, newMat);
            mat2.release();
        }
        Core.convertScaleAbs(newMat, currentMat);
        System.out.println("Applied Sobel filter: " + size + " " + direction);
        newMat.release();
    }

    public void applyScharr(String direction) {
        if (currentMat == null) return;
        int dx, dy;
        boolean both = false;
        switch (direction) {
            case "Horizontal" -> {
                dx = 0;
                dy = 1;
            }
            case "Vertical" -> {
                dx = 1;
                dy = 0;
            }
            case "Both" -> {
                dx = 0;
                dy = 1;
                both = true;
            }
            default -> {
                dx = 0;
                dy = 1;
            }
        }

        Mat newMat = new Mat();
        Imgproc.Scharr(currentMat, newMat, CvType.CV_16S, dx, dy);
        if (both) {
            Mat mat2 = new Mat();
            Imgproc.Sobel(currentMat, mat2, CvType.CV_16S, dy, dx);
            Core.addWeighted(newMat, 0.5, mat2, 0.5, 0.0, newMat);
            mat2.release();
        }
        Core.convertScaleAbs(newMat, currentMat);
        System.out.println("Applied Scharr filter: " + direction);
        newMat.release();
    }

    public void applyLaplacian(int size) {
        if (currentMat == null) return;
        Mat newMat = new Mat();
        Imgproc.Laplacian(currentMat, newMat, CvType.CV_16S, size);
        Core.convertScaleAbs(newMat, currentMat);
        System.out.println("Applied Laplacian filter: " + size);
        newMat.release();
    }

    public void applyCanny(double threshold1, double threshold2, int size) {
        this.convertToGrayscale();
        Imgproc.Canny(currentMat, currentMat, threshold1, threshold2, size);
        System.out.println("Applied Canny operator: threshold1 " + threshold1 + "; threshold2" + threshold2 + ", size" + size);
    }

    public void applyRoberts(int upperBoundary) {
        this.convertToGrayscale();
        Mat matFloat = new Mat();
        currentMat.convertTo(matFloat, CvType.CV_32F);

        Mat kernelX = new Mat(2, 2, CvType.CV_32F);
        kernelX.put(0, 0, 1.0, 0.0);
        kernelX.put(1, 0, 0.0, -1.0);
        Mat kernelY = new Mat(2, 2, CvType.CV_32F);
        kernelY.put(0, 0, 0.0, 1.0);
        kernelY.put(1, 0, -1.0, 0.0);

        Mat gX = new Mat();
        Mat gY = new Mat();
        Imgproc.filter2D(matFloat, gX, -1, kernelX);
        Imgproc.filter2D(matFloat, gY, -1, kernelY);
        kernelY.release();
        kernelX.release();

        Mat gXSquare = new Mat();
        Mat gYSquare = new Mat();
        Core.multiply(gX, gX, gXSquare);
        Core.multiply(gY, gY, gYSquare);
        gX.release();
        gY.release();


        Mat sumSquares = new Mat();
        Core.add(gXSquare, gYSquare, sumSquares);
        gXSquare.release();
        gYSquare.release();

        Mat gradientMagnitude = new Mat();
        Core.sqrt(sumSquares, gradientMagnitude);

        Mat normalizedEdges = new Mat();
        Core.normalize(gradientMagnitude, normalizedEdges, 0, upperBoundary, Core.NORM_MINMAX);
        gradientMagnitude.release();
        sumSquares.release();

        normalizedEdges.convertTo(currentMat, CvType.CV_8U);
        System.out.println("Applied Roberts");
        normalizedEdges.release();
    }

    public void keypointsFAST(int threshold) {
        if (currentMat == null) return;
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        FastFeatureDetector detector = FastFeatureDetector.create(threshold);
        detector.detect(currentMat, keyPoints);

        Mat newMat = new Mat();
        Features2d.drawKeypoints(currentMat, keyPoints, newMat, new Scalar(0, 0, 255));
        currentMat = newMat;
        System.out.println("Displayed keypoints from FAST detection. threshold=" + threshold);
    }

    public void keypointsSIFT(int count) {
        if (currentMat == null) return;
        MatOfKeyPoint keyPoints = new MatOfKeyPoint();
        SIFT detector = SIFT.create(count);
        detector.detect(currentMat, keyPoints);

        Mat newMat = new Mat();
        Features2d.drawKeypoints(currentMat, keyPoints, newMat, new Scalar(0, 0, 255));
        currentMat = newMat;
        System.out.println("Displayed " + count + " keypoints from SIFT detection");
    }

    public void clusterByColor(int n) {
        if (currentMat == null) return;
        Mat samples = currentMat.reshape(1, currentMat.rows() * currentMat.cols());
        Mat samples32f = new Mat();
        samples.convertTo(samples32f, CvType.CV_32F);

        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 0.1);
        Core.kmeans(samples32f, n, labels, criteria, 3, Core.KMEANS_PP_CENTERS, centers);

        centers.convertTo(centers, CvType.CV_8U);

        int rows = currentMat.rows();
        int cols = currentMat.cols();
        byte[] segmentedData = new byte[rows * cols * 3];
        int[] labelsArray = new int[rows * cols];
        labels.get(0, 0, labelsArray);

        byte[] centersData = new byte[n * 3];
        centers.get(0, 0, centersData);

        for (int i = 0; i < labelsArray.length; i++) {
            int clusterIdx = labelsArray[i];
            segmentedData[i * 3] = centersData[clusterIdx * 3];         // B
            segmentedData[i * 3 + 1] = centersData[clusterIdx * 3 + 1]; // G
            segmentedData[i * 3 + 2] = centersData[clusterIdx * 3 + 2]; // R
        }
        currentMat.put(0, 0, segmentedData);
//            HighGui.imshow("orig", originalMat);
//            HighGui.waitKey(0);
//            HighGui.destroyAllWindows();
        System.out.println("Applied clusterization by color, " + n + " segments");
    }

    public void clusterByColorAndPosition(int n, float spatialWeight) {
        if (currentMat == null) return;
        Mat lab = new Mat();
        Imgproc.cvtColor(currentMat, lab, Imgproc.COLOR_BGR2Lab);
        int rows = currentMat.rows();
        int cols = currentMat.cols();

        Mat samples = new Mat(rows * cols, 5, CvType.CV_32F);
        float[] samplesData = new float[rows * cols * 5];
        byte[] labData = new byte[rows * cols * 3];
        lab.get(0, 0, labData);
        spatialWeight /= 100.0f;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = y * cols + x;

                //  цвет
                samplesData[idx * 5] = labData[idx * 3] & 0xFF;
                samplesData[idx * 5 + 1] = labData[idx * 3 + 1] & 0xFF;
                samplesData[idx * 5 + 2] = labData[idx * 3 + 2] & 0xFF;

                // Нормализуем координаты от 0 до 255 и умножаем на вес
                samplesData[idx * 5 + 3] = ((float) x / cols) * 255.0f * spatialWeight;
                samplesData[idx * 5 + 4] = ((float) y / rows) * 255.0f * spatialWeight;
            }
        }
        samples.put(0, 0, samplesData);

        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 0.1);
        Core.kmeans(samples, n, labels, criteria, 3, Core.KMEANS_PP_CENTERS, centers);

        Mat segmentedLab = new Mat(rows, cols, CvType.CV_8UC3);
        byte[] segData = new byte[rows * cols * 3];
        int[] labelsArray = new int[rows * cols];
        labels.get(0, 0, labelsArray);
        float[] centersData = new float[n * 5];
        centers.get(0, 0, centersData);

        for (int i = 0; i < labelsArray.length; i++) {
            int clusterIdx = labelsArray[i];
            segData[i * 3] = (byte) centersData[clusterIdx * 5];     // L
            segData[i * 3 + 1] = (byte) centersData[clusterIdx * 5 + 1]; // a
            segData[i * 3 + 2] = (byte) centersData[clusterIdx * 5 + 2]; // b
        }
        segmentedLab.put(0, 0, segData);
        Imgproc.cvtColor(segmentedLab, currentMat, Imgproc.COLOR_Lab2BGR);

        lab.release();
        samples.release();
        labels.release();
        centers.release();
        segmentedLab.release();
        System.out.println("Applied clusterization by color and position with weight " + spatialWeight + ", " + n + " segments");
    }

    public void clusterByColorPositionAndGradient(int n, int kernelSize, float spatialWeight, float gradientWeight) {
        if (currentMat == null) return;
        int rows = currentMat.rows();
        int cols = currentMat.cols();

        Mat lab = new Mat();
        Imgproc.cvtColor(currentMat, lab, Imgproc.COLOR_BGR2Lab);
        byte[] labData = new byte[rows * cols * 3];
        lab.get(0, 0, labData);

        Mat gray = new Mat();
        Imgproc.cvtColor(currentMat, gray, Imgproc.COLOR_BGR2GRAY);
        Mat morphGrad = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(kernelSize, kernelSize));
        Imgproc.morphologyEx(gray, morphGrad, Imgproc.MORPH_GRADIENT, kernel);
        Imgproc.GaussianBlur(morphGrad, morphGrad, new Size(9, 9), 0);
        byte[] gradData = new byte[rows * cols];
        morphGrad.get(0, 0, gradData);

        Mat samples = new Mat(rows * cols, 6, CvType.CV_32F);
        float[] samplesData = new float[rows * cols * 6];

        spatialWeight /= 100.0f;
        float textureWeight = gradientWeight / 100.0f;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int idx = y * cols + x;
                // Цвет
                samplesData[idx * 6] = labData[idx * 3] & 0xFF;
                samplesData[idx * 6 + 1] = labData[idx * 3 + 1] & 0xFF;
                samplesData[idx * 6 + 2] = labData[idx * 3 + 2] & 0xFF;
                //  координаты
                samplesData[idx * 6 + 3] = ((float) x / cols) * 255.0f * spatialWeight;
                samplesData[idx * 6 + 4] = ((float) y / rows) * 255.0f * spatialWeight;
                // gradient
                samplesData[idx * 6 + 5] = (gradData[idx] & 0xFF) * textureWeight;
            }
        }
        samples.put(0, 0, samplesData);

        Mat labels = new Mat();
        Mat centers = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.EPS + TermCriteria.MAX_ITER, 100, 0.1);
        Core.kmeans(samples, n, labels, criteria, 3, Core.KMEANS_PP_CENTERS, centers);

        Mat segmentedLab = new Mat(rows, cols, CvType.CV_8UC3);
        byte[] segData = new byte[rows * cols * 3];
        int[] labelsArray = new int[rows * cols];
        labels.get(0, 0, labelsArray);
        float[] centersData = new float[n * 6];
        centers.get(0, 0, centersData);
        for (int i = 0; i < labelsArray.length; i++) {
            int clusterIdx = labelsArray[i];
            segData[i * 3] = (byte) centersData[clusterIdx * 6];
            segData[i * 3 + 1] = (byte) centersData[clusterIdx * 6 + 1];
            segData[i * 3 + 2] = (byte) centersData[clusterIdx * 6 + 2];
        }
        segmentedLab.put(0, 0, segData);

        Imgproc.cvtColor(segmentedLab, currentMat, Imgproc.COLOR_Lab2BGR);
        System.out.println("Applied clusterization by color, position with weight " + spatialWeight + ", gradient weight " + gradientWeight + ", " + n + " segments");

        lab.release();
        samples.release();
        gray.release();
        morphGrad.release();
        kernel.release();
        labels.release();
        centers.release();
        segmentedLab.release();
    }

    public void detectCarNumber() {
        if (currentMat == null || currentMat.empty()) return;
        Mat resultMat = currentMat.clone();


        List<Rect> detectedPlates = new ArrayList<>();



        try{
            detectedPlates = detectWithHaar(currentMat, "haarcascade_russian_plate_number.xml");
            System.out.println("car numbers detection with haarcascade_russian_plate_number");
        } catch (Exception ex) {
            System.out.println("haar cascade failed");
            ex.printStackTrace();
            detectedPlates = detectWithHeuristics(currentMat);
        }



        if (detectedPlates.isEmpty()) {
            detectedPlates = detectWithHeuristics(currentMat);
            System.out.println("car numbers detection with Heuristics");
        }

        for (Rect rect : detectedPlates) {
            Imgproc.rectangle(resultMat, new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 0, 255), 1);
        }

        currentMat = resultMat;
        System.out.println("detected " + detectedPlates.size() + " car number plates");
    }

    private List<Rect> detectWithHaar(Mat src, String cascadePath) {
        List<Rect> plates = new ArrayList<>();
        CascadeClassifier classifier = new CascadeClassifier(cascadePath);
        if (classifier.empty()) {
            return plates;
        }

        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect detections = new MatOfRect();
        classifier.detectMultiScale(gray, detections, 1.1, 4, 0, new Size(60, 20), new Size());
        plates.addAll(detections.toList());
        gray.release();
        detections.release();
        return plates;
    }

    private List<Rect> detectWithHeuristics(Mat src) {
        List<Rect> plates = new ArrayList<>();
        Mat gray = new Mat();
        Mat blackhat = new Mat();
        Mat gradX = new Mat();
        Mat thresh = new Mat();

        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Mat rectKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 5));
        Imgproc.morphologyEx(gray, blackhat, Imgproc.MORPH_BLACKHAT, rectKernel);
        Imgproc.Sobel(blackhat, gradX, CvType.CV_32F, 1, 0, -1);
        Core.convertScaleAbs(gradX, gradX);
        Imgproc.GaussianBlur(gradX, gradX, new Size(5, 5), 0);
        Imgproc.morphologyEx(gradX, gradX, Imgproc.MORPH_CLOSE, rectKernel);
        // Бинаризация по Отсу
        Imgproc.threshold(gradX, thresh, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        // по пропорциям автономера
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            double aspectRatio = (double) rect.width / rect.height;
            if (aspectRatio >= 3.0 && aspectRatio <= 5.5 && rect.width > 60 && rect.height > 15) {
                plates.add(rect);
            }
        }

        gray.release();
        blackhat.release();
        gradX.release();
        thresh.release();
        hierarchy.release();
        rectKernel.release();

        return plates;
    }
}
