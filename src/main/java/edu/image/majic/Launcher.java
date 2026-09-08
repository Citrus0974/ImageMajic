package edu.image.majic;

import javafx.application.Application;
import nu.pattern.OpenCV;
import org.opencv.core.Core;

public class Launcher {
    public static void main(String[] args) {
        OpenCV.loadLocally();
        System.out.println("Initialized OpenCV version " + Core.getVersionString());
        Application.launch(ImageMajicApplication.class, args);
    }
}
