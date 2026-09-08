package edu.image.majic.model;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

public class ImageModel {
    private Mat originalMat;
    private Mat currentMat;
    private File currentFile;

    public boolean loadImage(File file) {
        if (file == null || !file.exists()) return false;
        if (originalMat != null) originalMat.release();
        if (currentMat != null) currentMat.release();

        originalMat = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_UNCHANGED);
        System.out.println("Loaded file " + file.getAbsolutePath());

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
