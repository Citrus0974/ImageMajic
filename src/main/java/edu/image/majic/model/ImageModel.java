package edu.image.majic.model;

import com.drew.lang.annotations.Nullable;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

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

    public void applyErosion(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.erode(currentMat, currentMat, kernel);
        System.out.println("applied erosion: " + size + ", " + maskType);
        kernel.release();
    }

    public void applyDilation(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.dilate(currentMat, currentMat, kernel);
        System.out.println("applied dilation: " + size + ", " + maskType);
        kernel.release();
    }

    public void applyOpening(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.morphologyEx(currentMat, currentMat, Imgproc.MORPH_OPEN, kernel);
        System.out.println("applied open: " + size + ", " + maskType);
        kernel.release();
    }

    public void applyClosing(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.morphologyEx(currentMat, currentMat, Imgproc.MORPH_CLOSE, kernel);
        System.out.println("applied close: " + size + ", " + maskType);
        kernel.release();
    }

    public void applyGradient(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.morphologyEx(currentMat, currentMat, Imgproc.MORPH_GRADIENT, kernel);
        System.out.println("applied gradient: " + size + ", " + maskType);
        kernel.release();
    }

    public void applyTopHat(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.morphologyEx(currentMat, currentMat, Imgproc.MORPH_TOPHAT, kernel);
        System.out.println("applied top hat: " + size + ", " + maskType);
        kernel.release();
    }

    public void applyBlackHat(int size, String maskType, @Nullable byte[] customMask) {
        if (currentMat == null) return;
        Mat kernel = createKernel(size, maskType, customMask);
        if (kernel == null) return;
        Imgproc.morphologyEx(currentMat, currentMat, Imgproc.MORPH_BLACKHAT, kernel);
        System.out.println("applied black hat: " + size + ", " + maskType);
        kernel.release();
    }


    private Mat createKernel(int size, String maskType, byte[] customMask) {
        if (currentMat == null || originalMat == null || originalMat.empty() || size <= 0) return null;
        Mat kernel;
        if (customMask == null || customMask.length == 0) {
            int opencvShape;
            switch (maskType) {
                case "Square" -> opencvShape = Imgproc.MORPH_RECT;
                case "Cross" -> opencvShape = Imgproc.MORPH_CROSS;
                case "Ellipse" -> opencvShape = Imgproc.MORPH_ELLIPSE;
                default -> opencvShape = Imgproc.MORPH_RECT;
            }
            kernel = Imgproc.getStructuringElement(opencvShape, new Size(size, size));
        } else {
            kernel = new Mat(size, size, org.opencv.core.CvType.CV_8UC1);
            kernel.put(0, 0, customMask);
        }
        return kernel;
    }

    public void applyGauss(int size, double sigma) {
        if (currentMat == null) return;
        if (size < 0 || sigma < 0 || (size != 0 && size % 2 == 0)) return;

        if (size == 0) size = Math.toIntExact(Math.round(sigma * 3)) * 2 + 1;
        if (sigma == 0) sigma = 0.3 * (((size - 1.0) / 2.0) - 1.0) + 0.8;
        Imgproc.GaussianBlur(currentMat, currentMat, new Size(size, size), sigma);
        System.out.println("applied gaussian blur: " + size + ", sigma: " + sigma);
    }

    public void applyMedianBlur(int size) {
        if (currentMat == null || currentMat.empty() || size <= 0) return;
        Imgproc.medianBlur(currentMat, currentMat, size);
        System.out.println("applied median blur: " + size);
    }

    public void applyMotionBlur(int size, String direction) {
        if (currentMat == null || currentMat.empty() || size <= 0) return;
        Mat kernel = Mat.zeros(size, size, CvType.CV_32FC1);
        float value = 1.0f / size;

        switch (direction) {
            case "Diagonal" -> {
                for (int i = 0; i < size; i++) {
                    kernel.put(i, i, value);
                }
            }
            case "Horizontal" -> {
                for (int i = 0; i < size; i++) {
                    kernel.put(size / 2, i, value);
                }
            }
            case "Vertical" -> {
                for (int i = 0; i < size; i++) {
                    kernel.put(i, size / 2, value);
                }
            }
            case "Diagonal (Reversed)" -> {
                for (int i = 0; i < size; i++) {
                    kernel.put(size - 1 - i, i, value);
                }
            }
        }

        Imgproc.filter2D(currentMat, currentMat, -1, kernel);
        kernel.release();
        System.out.println("applied motion blur: " + size);
    }

    public void applySharpen(int size) {
        if (currentMat == null || currentMat.empty() || size <= 0 || size % 2 == 0) return;
        Mat kernel = new Mat(size, size, CvType.CV_32FC1);
        float[] data = new float[size * size];
        Arrays.fill(data, -1.0f);
        int center = (size * size) / 2;
        data[center] = size * size;
        kernel.put(0, 0, data);

        Imgproc.filter2D(currentMat, currentMat, -1, kernel);
        kernel.release();
        System.out.println("applied sharpen: " + size);
    }

    public void applySharpen2(int size) {
        if (currentMat == null || currentMat.empty() || size <= 0 || size % 2 == 0) return;
        Mat kernel = new Mat(size, size, CvType.CV_32FC1);
        float[] data = new float[size * size];
        int center = size / 2;

        float sumNeighbors = 0;
        float strength = 0.1f;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                if (row == center && col == center) continue;
                float dist = (float) (Math.pow(row - center, 2) + Math.pow(col - center, 2));
                data[index] = (float) (-strength / (1.0 + dist));
                sumNeighbors += data[index];
            }
        }

        data[center * size + center] = 1.0f - sumNeighbors;
        kernel.put(0, 0, data);
        Imgproc.filter2D(currentMat, currentMat, -1, kernel);
        kernel.release();
        System.out.println("applied soft sharpen: " + size + ", " + strength + ", " + center + ", " + sumNeighbors);
    }

    public void applyEmboss(int size) {
        if (currentMat == null || currentMat.empty() || size <= 0 || size % 2 == 0) return;
        Mat kernel = new Mat(size, size, CvType.CV_32FC1);
        float[] data = new float[size * size];
        int center = size / 2;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                if (row < center && col < center) {
                    data[index] = -1.0f; // shadow
                } else if (row > center && col > center) {
                    data[index] = 1.0f;  // highlight
                } else {
                    data[index] = 0.0f;  // plain
                }
            }
        }
        data[(size * size) / 2] = 1.0f;
        kernel.put(0, 0, data);
        Imgproc.filter2D(currentMat, currentMat, -1, kernel);
        kernel.release();
        System.out.println("applied emboss: " +size);
    }


    public void applyCustomFilter(int size, double[] kernel, boolean shouldNormalize) {
        if (currentMat == null || currentMat.empty() || size <= 0 || kernel == null || kernel.length == 0) return;
        if (shouldNormalize) {
            double sum = 0;
            for (double b : kernel) {
                sum += b;
            }
            if (Math.abs(sum) > 0.00001) {
                for (int i = 0; i < kernel.length; i++) {
                    kernel[i] = kernel[i] / sum;
                }
            }
            System.out.println("normalize sum: " + sum);
        }
        Mat kernelMat = new Mat(size, size, CvType.CV_32FC1);
        kernelMat.put(0, 0, kernel);
        Imgproc.filter2D(currentMat, currentMat, -1, kernelMat);
        kernelMat.release();
        System.out.println("applied custom filter " + Arrays.toString(kernel) + ", " + size);
    }
}
