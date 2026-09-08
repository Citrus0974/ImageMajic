module edu.image.majic {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;
    requires java.desktop;
    requires jdk.jshell;


    opens edu.image.majic to javafx.fxml;
    exports edu.image.majic;
}