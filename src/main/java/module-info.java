module edu.image.majic {
    requires javafx.controls;
    requires javafx.fxml;


    opens edu.image.majic to javafx.fxml;
    exports edu.image.majic;
}