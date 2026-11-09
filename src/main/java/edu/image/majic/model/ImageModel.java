package edu.image.majic.model;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

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
}
