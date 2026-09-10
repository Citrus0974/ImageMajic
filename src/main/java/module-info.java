module edu.image.majic {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;
    requires java.desktop;
    requires jdk.jshell;
    requires java.sql;
    requires com.drew.metadata;
    requires javafx.base;


    opens edu.image.majic to javafx.fxml;
    exports edu.image.majic;
    exports edu.image.majic.controller;
    opens edu.image.majic.controller to javafx.fxml;
    exports edu.image.majic.view;
    opens edu.image.majic.view to javafx.fxml;
}