package edu.image.majic.util;

import javafx.scene.image.Image;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ImageUtils {
    public static Image matToJavaFXImage(Mat mat) {
        if (mat == null || mat.empty()) return null;
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
    public static HashMap<String, String> addImageParamsToMetadata( HashMap<String, String> metadata, Mat image){
        metadata.put("Width", image.width() + " px");
        metadata.put("Height", image.height() + " px");
        metadata.put("Color depth", image.depth() + " bit");
        return metadata;
    }

    public static HashMap<String, String> addImageColorModelToMetadata (HashMap<String, String> metadata, File image){
        if (image == null) {
            return metadata;
        }
        String colorModel = "Unknown";
        try {
            BufferedImage bufferedImage = ImageIO.read(image);
            if (bufferedImage == null) {
                metadata.put("Color model", colorModel);
                return metadata;
            }
            int type = bufferedImage.getType();
            switch (type) {
                case BufferedImage.TYPE_INT_RGB -> colorModel = "RGB";
                case BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_INT_ARGB_PRE -> colorModel = "ARGB";
                case BufferedImage.TYPE_BYTE_GRAY -> colorModel = "Grayscale";
                case BufferedImage.TYPE_BYTE_INDEXED -> colorModel = "Indexed (Palette)";
                case BufferedImage.TYPE_3BYTE_BGR -> colorModel = "BGR";
                default -> colorModel = colorModel + " " + type;
            }
        } catch (IOException e) {
            System.out.println("Error proceeding image " + image.getAbsolutePath() + " : " + e.getMessage());
        }
        metadata.put("Color model", colorModel);
        return metadata;
    }
}
