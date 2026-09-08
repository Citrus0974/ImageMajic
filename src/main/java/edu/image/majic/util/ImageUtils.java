package edu.image.majic.util;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import javafx.scene.image.Image;
import java.io.ByteArrayInputStream;

public class ImageUtils {
    public static Image matToJavaFXImage(Mat mat) {
        if (mat == null || mat.empty()) return null;
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}
