package edu.image.majic.model;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

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
}
