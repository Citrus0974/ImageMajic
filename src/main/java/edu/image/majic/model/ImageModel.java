package edu.image.majic.model;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
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

    public void applyErosion(int size, String maskType, byte[] customMask) {
        if (currentMat == null || originalMat == null || originalMat.empty()) return;
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
            kernel = new MatOfByte(customMask);
        }
        Imgproc.erode(currentMat, currentMat, kernel);
        System.out.println("applied erosion: " + size + ", " + maskType);
    }
}
