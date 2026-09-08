package edu.image.majic;

import edu.image.majic.model.ImageModel;
import edu.image.majic.util.ImageUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;

public class MainWindowController {
    @FXML
    public ImageView imageView;
    @FXML
    public ScrollPane imageScrollPane;

    private final ImageModel imageModel = new ImageModel();

    private double currentImageZoom = 1;
    private double imageOriginalWidth = 0;
    private double imageOriginalHeight = 0;

    @FXML
    public void initialize() {
        imageScrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (imageView.getImage() != null) {
                adjustAfterResize();
            }
        });

        imageScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                event.consume();
                double delta = event.getDeltaY() > 0 ? 0.1 : -0.1;
                zoomImage(delta);
            }
        });
    }

    @FXML
    public void onOpenImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open image");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image files",
                "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.webp"));

        Stage stage = (Stage) imageView.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            boolean success = imageModel.loadImage(selectedFile);
            if (success) {
                displayMatImage(imageModel.getCurrentMat());
            }
        }
    }

    @FXML
    public void onSaveImage() {
        if (imageModel.getCurrentMat() == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JPEG Image (*.jpg)", "*.jpg"),
                new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png")
        );

        Stage stage = (Stage) imageView.getScene().getWindow();
        File saveFile = fileChooser.showSaveDialog(stage);

        if (saveFile != null) {
            String path = saveFile.getAbsolutePath();
            Mat matToSave = imageModel.getCurrentMat();

            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                int quality = showCompressionDialog("jpg");
                if (quality == -1) return;
                MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, quality);
                boolean success = Imgcodecs.imwrite(path, matToSave, params);
                System.out.println("Saved JPG with quality: " + quality + " " + success);
            } else if (path.endsWith(".png")) {
                int quality = showCompressionDialog("png");
                if (quality == -1) return;
                MatOfInt params = new MatOfInt(Imgcodecs.IMWRITE_PNG_COMPRESSION, quality);
                boolean success = Imgcodecs.imwrite(path, matToSave, params);
                System.out.println("Saved JPG with quality: " + quality + " " + success);
            }
        }
    }

    private int showCompressionDialog(String filetype) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Compression options");
        dialog.setHeaderText("Compression setting for " + filetype.toUpperCase());
        ButtonType okButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        Slider slider = new Slider();
        Label valueLabel = new Label();
        Label descriptionLabel = new Label();
        switch (filetype) {
            case "jpg", "jpeg" -> {
                slider.setMin(1);
                slider.setMax(100);
                slider.setValue(80);
                slider.setBlockIncrement(1);
                descriptionLabel.setText("JPEG quality:");
                valueLabel.setText("80%");
                slider.valueProperty().addListener((obs, oldVal, newVal) ->
                        valueLabel.setText(newVal.intValue() + "%"));
            }
            case "png" -> {
                slider.setMin(0);
                slider.setMax(9);
                slider.setValue(3);
                slider.setMajorTickUnit(1);
                slider.setMinorTickCount(0);
                slider.setSnapToTicks(true);
                descriptionLabel.setText("Lossless compression level: ");
                valueLabel.setText("3");
                slider.valueProperty().addListener((obs, oldVal, newVal) ->
                        valueLabel.setText(String.valueOf(newVal.intValue()))
                );
            }
            default -> {
                descriptionLabel.setText("Other formats settings are not supported");
            }
        }
        VBox vBox = new VBox(10);
//        vBox.setPadding(new Insets(20, 20, 20, 20));
        vBox.setAlignment(Pos.CENTER);
        slider.setMaxWidth(Double.MAX_VALUE);
        vBox.getChildren().addAll(descriptionLabel, slider, valueLabel);
        dialog.getDialogPane().setContent(vBox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return (int) slider.getValue();
            }
            return null;
        });
        var result = dialog.showAndWait();
        return result.orElse(-1);
    }


    private void displayMatImage(Mat mat) {
        Image displayImage = ImageUtils.matToJavaFXImage(mat);
        imageView.setImage(displayImage);

        imageOriginalWidth = displayImage.getWidth();
        imageOriginalHeight = displayImage.getHeight();

        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        imageScrollPane.setHvalue(0.5);
        imageScrollPane.setVvalue(0.5);

        fitImageToViewport();
        centerImageToViewport();
    }

    @FXML
    private void fitImageToViewport() {
        if (imageView.getImage() == null) return;

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        double scaleX = viewportWidth / imageOriginalWidth;
        double scaleY = viewportHeight / imageOriginalHeight;
        currentImageZoom = Math.min(scaleX, scaleY);

        imageView.setFitWidth(imageOriginalWidth * currentImageZoom);
        imageView.setFitHeight(imageOriginalHeight * currentImageZoom);

        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
    }

    public void centerImageToViewport() {
        if (imageView.getImage() == null) return;

        double scaledWidth = imageView.getFitWidth();
        double scaledHeight = imageView.getFitHeight();

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        boolean needCenterX = scaledWidth < viewportWidth;
        boolean needCenterY = scaledHeight < viewportHeight;

        if (needCenterX && needCenterY) {
            // Если изображение меньше viewport по обеим осям - центрируем через Pane
            imageView.setTranslateX((viewportWidth - scaledWidth) / 2);
            imageView.setTranslateY((viewportHeight - scaledHeight) / 2);

            // Отключаем скроллинг
            imageScrollPane.setHvalue(0.5);
            imageScrollPane.setVvalue(0.5);
        } else if (needCenterX) {
            // Только по горизонтали
            imageView.setTranslateX((viewportWidth - scaledWidth) / 2);
            imageView.setTranslateY(0);
            imageScrollPane.setHvalue(0.5);
        } else if (needCenterY) {
            // Только по вертикали
            imageView.setTranslateX(0);
            imageView.setTranslateY((viewportHeight - scaledHeight) / 2);
            imageScrollPane.setVvalue(0.5);
        } else {
            // Изображение больше viewport - сбрасываем translate
            imageView.setTranslateX(0);
            imageView.setTranslateY(0);
        }
    }

    private void adjustAfterResize() {
        if (imageView.getImage() == null) return;

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        double scaledWidth = imageView.getFitWidth();
        double scaledHeight = imageView.getFitHeight();

        boolean isSmallerThanViewport = scaledWidth < viewportWidth && scaledHeight < viewportHeight;

        if (isSmallerThanViewport) {
            centerImageToViewport();
        } else {
            double hValue = imageScrollPane.getHvalue();
            double vValue = imageScrollPane.getVvalue();

            // Сбрасываем translate, если они мешают
            if (scaledWidth < viewportWidth) {
                imageView.setTranslateX((viewportWidth - scaledWidth) / 2);
            } else {
                imageView.setTranslateX(0);
            }

            if (scaledHeight < viewportHeight) {
                imageView.setTranslateY((viewportHeight - scaledHeight) / 2);
            } else {
                imageView.setTranslateY(0);
            }

            // Восстанавливаем позиции скролла
            imageScrollPane.setHvalue(hValue);
            imageScrollPane.setVvalue(vValue);
        }
    }

    @FXML
    private void zoomImage(double delta) {
        if (imageView.getImage() == null) return;

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();
        double scaledWidth = imageView.getFitWidth();
        double scaledHeight = imageView.getFitHeight();

        double centerX = (scaledWidth * imageScrollPane.getHvalue()) + viewportWidth / 2;
        double centerY = (scaledHeight * imageScrollPane.getVvalue()) + viewportHeight / 2;

        double newZoom = currentImageZoom + delta;
        currentImageZoom = newZoom;
        if (newZoom < 0.01 || newZoom > 20) return;
        System.out.println("new scale " + currentImageZoom);
        imageView.setFitWidth(imageOriginalWidth * currentImageZoom);
        imageView.setFitHeight(imageOriginalHeight * currentImageZoom);

        double newScaledWidth = imageView.getFitWidth();
        double newScaledHeight = imageView.getFitHeight();

        if (newScaledWidth < viewportWidth && newScaledHeight < viewportHeight) {
            centerImageToViewport();
        } else {
            // Восстанавливаем позицию скролла относительно центра
            double newHValue = (centerX - viewportWidth / 2) / newScaledWidth;
            double newVValue = (centerY - viewportHeight / 2) / newScaledHeight;

            // Ограничиваем значения
            newHValue = Math.max(0, Math.min(1, newHValue));
            newVValue = Math.max(0, Math.min(1, newVValue));

            imageScrollPane.setHvalue(newHValue);
            imageScrollPane.setVvalue(newVValue);

            imageView.setTranslateX(0);
            imageView.setTranslateY(0);
        }
    }
}
