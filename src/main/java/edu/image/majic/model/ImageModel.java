package edu.image.majic.model;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ImageModel {
    private Mat originalMat;
    private Mat currentMat;
    private File currentFile;

    private int brightnessValue = 0;
    private int contrastValue = 0;
    private int saturationValue = 0;
    private boolean isGrayscale = false;
    private boolean isInverted = false;
    private boolean isEqualizeHist = false;
    private int claheValue = 0;
    private int gammaValue = 0;


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

    public void reApplyFilters() {
        if (originalMat == null) return;
        Mat newMat = originalMat.clone();

        newMat = applyBrightnessAndContrast(newMat);
        newMat = applySaturation(newMat);
        newMat = applyGamma(newMat);
        newMat = applyInvert(newMat);
        newMat = applyGrayscale(newMat);

        Mat oldMat = currentMat;
        currentMat = newMat;
        oldMat.release();
    }

    private Mat applyBrightnessAndContrast(Mat newMat) {
        if (brightnessValue != 0 || contrastValue != 0) {
//            double alpha = contrastValue;
//            double beta = brightnessValue;
//            if (alpha <= 0) {
//                alpha = (alpha + 256.0) / 256.0;
//            }

            double alpha;
            if (contrastValue >= 0) {
                alpha = 1.0 + (contrastValue / 255.0) * 6.0;
            } else {
                alpha = (contrastValue + 256.0) / 256.0;
            }
            double beta = 128.0 * (1.0 - alpha) + brightnessValue;

            System.out.println("alpha=" + alpha + " beta=" + beta + " contrast=" + contrastValue);

            newMat.convertTo(newMat, -1, alpha, beta);
        }
        return newMat;
    }

    private Mat applySaturation(Mat newMat) {
        if (saturationValue != 0 && currentMat.channels() >= 3 && !isGrayscale) {
            double k = saturationValue;
            double maxGain = 4.0;
            if (k >= 0) {
                k = 1.0 + (k / 255.0) * (maxGain - 1.0);
            } else {
                k = 1.0 + (k / 255.0);
            }
            System.out.println("saturation k=" + k);
            Mat hsvMat = new Mat();
            Imgproc.cvtColor(newMat, hsvMat, Imgproc.COLOR_BGR2HSV);
            List<Mat> hsvChannels = new ArrayList<>();
            Core.split(hsvMat, hsvChannels);
            hsvChannels.get(1).convertTo(hsvChannels.get(1), -1, k, 0);
            Core.merge(hsvChannels, hsvMat);
            Imgproc.cvtColor(hsvMat, newMat, Imgproc.COLOR_HSV2BGR);

            hsvMat.release();
            for (Mat m : hsvChannels) m.release();
        }

        return newMat;
    }

    private Mat applyGrayscale(Mat newMat) {
        if (isGrayscale) {
            if (newMat.channels() == 4) {
                Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_BGRA2GRAY);
                newMat = applyEqualizeHist(newMat);
                newMat = applyCLAHE(newMat);
                Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_GRAY2BGRA);
            } else if (newMat.channels() == 3) {
                Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_BGR2GRAY);
                newMat = applyEqualizeHist(newMat);
                newMat = applyCLAHE(newMat);
                Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_GRAY2BGR);
            }
            System.out.println("grayscale applied");
        }
        return newMat;
    }

    private Mat applyEqualizeHist(Mat newMat) {
        if (newMat.channels() != 1 || !isEqualizeHist) {
            return newMat;
        }
        Imgproc.equalizeHist(newMat, newMat);
        System.out.println("applied EqualizeHist");
        return newMat;
    }

    private Mat applyCLAHE(Mat newMat) {
        if (newMat.channels() != 1 || claheValue == 0) {
            return newMat;
        }
        double clipLimit = (double) (claheValue + 1) / 25.0;
        CLAHE clahe = Imgproc.createCLAHE(clipLimit, new Size(8, 8));
        clahe.apply(newMat, newMat);
        System.out.println("applied CLAHE: amount:" + claheValue + " ; clipLimit: " + clipLimit);
        return newMat;
    }

    private Mat applyInvert(Mat newMat) {
        if (isInverted) {
            Core.bitwise_not(newMat, newMat);
            System.out.println("Invert applied");
        }
        return newMat;
    }

    private Mat applyGamma(Mat newMat) {
        if(gammaValue == 0) {
            return newMat;
        }

        double gamma = Math.exp((double) -gammaValue /  50.0);
        Mat lut = new Mat(1, 256, CvType.CV_8UC1);
        byte[] lutData = new byte[256];
        for (int i = 0; i < 256; i++) {
            double normalized = (double) i / 255.0;
            double corrected = Math.pow(normalized, gamma) * 255.0;
            lutData[i] = (byte) Math.min(255, Math.max(0, Math.round(corrected)));
        }
        lut.put(0, 0, lutData);

        Core.LUT(newMat, lut, newMat);
        System.out.println("applied gamma=" + gamma);
        return newMat;
    }

    public void rotateLeft() {
        if (originalMat == null) return;

        Mat rotatedMat = new Mat();
        Core.rotate(originalMat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE);
        Mat oldMat = originalMat;
        originalMat = rotatedMat;
        oldMat.release();
    }

    public void rotateRight() {
        if (originalMat == null) return;

        Mat rotatedMat = new Mat();
        Core.rotate(originalMat, rotatedMat, Core.ROTATE_90_CLOCKWISE);
        Mat oldMat = originalMat;
        originalMat = rotatedMat;
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

    public int getBrightnessValue() {
        return brightnessValue;
    }

    public void setBrightnessValue(int brightnessValue) {
        this.brightnessValue = brightnessValue;
    }

    public int getContrastValue() {
        return contrastValue;
    }

    public void setContrastValue(int contrastValue) {
        this.contrastValue = contrastValue;
    }

    public int getSaturationValue() {
        return saturationValue;
    }

    public void setSaturationValue(int saturationValue) {
        this.saturationValue = saturationValue;
    }

    public boolean isGrayscale() {
        return isGrayscale;
    }

    public void setGrayscale(boolean grayscale) {
        isGrayscale = grayscale;
    }

    public boolean isInverted() {
        return isInverted;
    }

    public void setInverted(boolean inverted) {
        isInverted = inverted;
    }

    public boolean isEqualizeHist() {
        return isEqualizeHist;
    }

    public void setEqualizeHist(boolean equalizeHist) {
        isEqualizeHist = equalizeHist;
    }

    public int getClaheValue() {
        return claheValue;
    }

    public void setClaheValue(int claheValue) {
        this.claheValue = claheValue;
    }

    public int getGammaValue() {
        return gammaValue;
    }

    public void setGammaValue(int gammaValue) {
        this.gammaValue = gammaValue;
    }

    public void reset() {
        brightnessValue = 0;
        contrastValue = 0;
        saturationValue = 0;
        gammaValue = 0;
        isGrayscale = false;
        isInverted = false;
        isEqualizeHist = false;
        claheValue = 0;

        if (originalMat != null) {
            currentMat.release();
            currentMat = originalMat.clone();
        }
    }


}
