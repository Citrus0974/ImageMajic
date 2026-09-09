package edu.image.majic.controller;

import edu.image.majic.model.ImageModel;
import edu.image.majic.util.ColorChannel;
import edu.image.majic.util.ImageUtils;
import edu.image.majic.view.MetadataTableRow;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import static edu.image.majic.util.HistogramUtils.calculateHistogramChartSeries;
import static edu.image.majic.util.ImageUtils.addImageColorModelToMetadata;
import static edu.image.majic.util.ImageUtils.addImageParamsToMetadata;
import static edu.image.majic.util.MetadataUtils.fillImageMetadataHashmap;
import static edu.image.majic.util.MetadataUtils.printAllExifMetadata;

public class MainWindowController {
    @FXML
    private Label scaleLabel;

    @FXML
    private ImageView imageView;
    @FXML
    private ScrollPane imageScrollPane;

    @FXML
    private ComboBox<String> histogramChannelCombo;
    @FXML
    private AreaChart<Number, Number> histogramChart;
    @FXML
    private TableView<MetadataTableRow> metadataTableView;
    @FXML
    private TableColumn<MetadataTableRow, String> metadataParameterColumn;
    @FXML
    private TableColumn<MetadataTableRow, String> metadataValueColumn;


    private final ImageModel imageModel = new ImageModel();

    private final DoubleProperty currentImageZoomProperty = new SimpleDoubleProperty(1.0);
    private double imageOriginalWidth = 0;
    private double imageOriginalHeight = 0;

    @FXML
    public void initialize() {
        scaleLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            int scale = (int) Math.round(currentImageZoomProperty.get() * 100);
            return "Scale: " + scale + "%";
        }, currentImageZoomProperty));

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

        histogramChannelCombo.getItems().addAll("All (Luminance)",
//                "All (Comparison)",
                "Red", "Green", "Blue");
        histogramChannelCombo.getSelectionModel().selectFirst();

        metadataParameterColumn.setCellValueFactory(new PropertyValueFactory<>("paramName"));
        metadataValueColumn.setCellValueFactory(new PropertyValueFactory<>("paramValue"));
        metadataValueColumn.setCellFactory(col -> new TableCell<>() {
            private final Tooltip tooltip = new Tooltip();
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isEmpty()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    tooltip.setText(item);
                    setTooltip(tooltip);
                }
            }
        });
        var tableItems = fillMetadataTableRows();
        metadataTableView.setItems(tableItems);
    }

    @FXML
    public void onScalePlus() {
        zoomImage(0.1);
    }

    @FXML
    public void onScaleMinus() {
        zoomImage(-0.1);
    }

    @FXML
    public void onScaleFit() {
        fitImageToViewport();
        centerImageToViewport();
    }

    @FXML
    public void onScale100() {
        if (imageView.getImage() == null) return;
        currentImageZoomProperty.set(1.0);
        imageView.setFitWidth(imageOriginalWidth);
        imageView.setFitHeight(imageOriginalHeight);
        centerImageToViewport();
    }


    @FXML
    public void onHistogramChannelChanged() {
        updateHistogram();
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
                updateHistogram();
                updateMetadataTableView(selectedFile, imageModel.getCurrentMat());
            }
        }
    }

    @FXML
    public void onSaveImage() {
        if (imageModel.getCurrentMat() == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save image");
        fileChooser.setInitialFileName(imageModel.getCurrentFile().getName());
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
        currentImageZoomProperty.set(Math.min(scaleX, scaleY));

        imageView.setFitWidth(imageOriginalWidth * currentImageZoomProperty.get());
        imageView.setFitHeight(imageOriginalHeight * currentImageZoomProperty.get());

        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
    }

    public void centerImageToViewport() {
        if (imageView.getImage() == null) return;

        double scaledWidth = imageView.getFitWidth();
        double scaledHeight = imageView.getFitHeight();

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        boolean needCenterX = scaledWidth <= viewportWidth;
        boolean needCenterY = scaledHeight <= viewportHeight;

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

    private void zoomImage(double delta) {
        if (imageView.getImage() == null) return;

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();
        double scaledWidth = imageView.getFitWidth();
        double scaledHeight = imageView.getFitHeight();

        double centerX = (scaledWidth * imageScrollPane.getHvalue()) + viewportWidth / 2;
        double centerY = (scaledHeight * imageScrollPane.getVvalue()) + viewportHeight / 2;

        double newZoom = currentImageZoomProperty.get() + delta;
        if (newZoom < 0.01 || newZoom > 20) return;
        System.out.println("new scale " + currentImageZoomProperty.get());
        currentImageZoomProperty.set(newZoom);
        imageView.setFitWidth(imageOriginalWidth * currentImageZoomProperty.get());
        imageView.setFitHeight(imageOriginalHeight * currentImageZoomProperty.get());

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

    @FXML
    public void updateHistogram() {
        Mat currentMat = imageModel.getCurrentMat();
        if (currentMat == null || currentMat.empty()) return;

        histogramChart.getData().clear();

        int channelsCount = currentMat.channels();
        if (channelsCount == 4 && !histogramChannelCombo.getItems().contains("Alpha")) {
            histogramChannelCombo.getItems().add("Alpha");
        } else if (channelsCount == 3) {
            histogramChannelCombo.getItems().remove("Alpha");
        }

        histogramChart.getData().clear();
        String selectedMode = histogramChannelCombo.getSelectionModel().getSelectedItem();

        if (channelsCount >= 3) {
            switch (selectedMode) {
                case "Red" ->
                        histogramChart.getData().add(calculateHistogramChartSeries(currentMat, ColorChannel.RED, "Red"));
                case "Green" ->
                        histogramChart.getData().add(calculateHistogramChartSeries(currentMat, ColorChannel.GREEN, "Green"));
                case "Blue" ->
                        histogramChart.getData().add(calculateHistogramChartSeries(currentMat, ColorChannel.BLUE, "Blue"));
                case "Alpha" -> {
                    if (channelsCount == 4) {
                        histogramChart.getData().add(calculateHistogramChartSeries(currentMat, ColorChannel.ALPHA, "Alpha"));
                    }
                }
                case "All (Comparison)" -> {
                    XYChart.Series<Number, Number> redSeries = calculateHistogramChartSeries(currentMat, ColorChannel.RED, "Red");
                    XYChart.Series<Number, Number> greenSeries = calculateHistogramChartSeries(currentMat, ColorChannel.GREEN, "Green");
                    XYChart.Series<Number, Number> blueSeries = calculateHistogramChartSeries(currentMat, ColorChannel.BLUE, "Blue");
                    histogramChart.getData().addAll(blueSeries, greenSeries, redSeries);
                }
                default -> {
                    Mat grayMat = new Mat();
                    if (channelsCount == 4) {
                        Imgproc.cvtColor(currentMat, grayMat, Imgproc.COLOR_BGRA2GRAY);
                    } else {
                        Imgproc.cvtColor(currentMat, grayMat, Imgproc.COLOR_BGR2GRAY);
                    }
                    histogramChart.getData().add(calculateHistogramChartSeries(grayMat, ColorChannel.BLUE, "Luminance")); //BLUE = channel 0
                    grayMat.release();
                }
            }
        } else if (channelsCount == 1) {
            histogramChart.getData().add(calculateHistogramChartSeries(currentMat, ColorChannel.BLUE, "Grayscale")); //BLUE = channel 0
        }
    }

    private ObservableList<MetadataTableRow> fillMetadataTableRows() {
        ObservableList<MetadataTableRow> metadataItems = FXCollections.observableArrayList();
        metadataItems.add(new MetadataTableRow("Camera manufacturer", ""));
        metadataItems.add(new MetadataTableRow("Camera model", ""));
//        metadataItems.add(new MetadataTableRow("Lens manufacturer", ""));
        metadataItems.add(new MetadataTableRow("Lens model", ""));
        metadataItems.add(new MetadataTableRow("Lens specification", ""));
        metadataItems.add(new MetadataTableRow("Exposure Time", ""));
        metadataItems.add(new MetadataTableRow("F-Number (Aperture)", ""));
        metadataItems.add(new MetadataTableRow("ISO Speed", ""));
        metadataItems.add(new MetadataTableRow("Focal length", ""));
        metadataItems.add(new MetadataTableRow("Focal length (35-mm equivalent)", ""));
        metadataItems.add(new MetadataTableRow("Editing software", ""));
        metadataItems.add(new MetadataTableRow("Date/Time", ""));
        metadataItems.add(new MetadataTableRow("File name", ""));
        metadataItems.add(new MetadataTableRow("File type", ""));
        metadataItems.add(new MetadataTableRow("File size", ""));
        metadataItems.add(new MetadataTableRow("Last modified", ""));
        metadataItems.add(new MetadataTableRow("Width", ""));
        metadataItems.add(new MetadataTableRow("Height", ""));
        metadataItems.add(new MetadataTableRow("Color model", ""));
        metadataItems.add(new MetadataTableRow("Color space", ""));
        metadataItems.add(new MetadataTableRow("Color transform", ""));
        return metadataItems;
    }

    private void updateMetadataTableView(File file, Mat image) {
        HashMap<String, String> metadataHashMap = new HashMap<>();
        fillImageMetadataHashmap(metadataHashMap, file);
        addImageParamsToMetadata(metadataHashMap, image);
        addImageColorModelToMetadata(metadataHashMap, file);
//        MetadataUtils.printAllMetadata(file);
        printAllExifMetadata(file);
        System.out.println(metadataHashMap);
        List<MetadataTableRow> rows = metadataTableView.getItems();
        for (MetadataTableRow row : rows) {
            String parameter = row.getParamName();
            String value = metadataHashMap.get(parameter);
            row.setParamValue(value != null ? value : "");
        }

    }


}
