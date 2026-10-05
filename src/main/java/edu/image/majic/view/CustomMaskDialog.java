package edu.image.majic.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class CustomMaskDialog extends Dialog<Object> {
    private final int size;
    private final boolean isMorphology;

    private byte[] byteResult;
    private double[] doubleResult;

    public CustomMaskDialog(int size, boolean isMorphology) {
        this.size = size;
        this.isMorphology = isMorphology;

        setTitle(isMorphology ? "Custom mask" : "Custom kernel");
        setHeaderText("Matrix of size " + size + "×" + size);

        ButtonType applyButtonType = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(applyButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(10));

        if (isMorphology) {
            setupMorphologyGrid(grid);
        } else {
            setupConvolutionGrid(grid);
        }

        VBox content = new VBox(10, grid);
        content.setAlignment(Pos.CENTER);
        getDialogPane().setContent(content);

        setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                if (isMorphology) {
                    return byteResult;
                } else {
                    return doubleResult;
                }
            }
            return null;
        });


    }

    private void setupConvolutionGrid(GridPane grid) {
        doubleResult = new double[size * size];
        Arrays.fill(doubleResult, 0.0);
        TextField[] fields = new TextField[size * size];
        Pattern regex = Pattern.compile("-?\\d*(\\.\\d*)?");
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                TextField textField = new TextField("0.0");
                textField.setPrefSize(35, 35);
                AtomicBoolean isUpdating = new AtomicBoolean(false);
                textField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (isUpdating.get()) return;
                    if (regex.matcher(newValue).matches()) {
                        isUpdating.set(true);
                        textField.setText(newValue);
                        isUpdating.set(false);
                    }
                });
                fields[index] = textField;
                grid.add(textField, col, row);
            }
        }

        this.setOnCloseRequest(event -> {
            if (getResult() != null) { // "Apply" pressed
                for (int i = 0; i < fields.length; i++) {
                    try {
                        doubleResult[i] = Double.parseDouble(fields[i].getText());
                    } catch (NumberFormatException e) {
                        doubleResult[i] = 0.0;
                    }
                }
            }
        });
    }

    private void setupMorphologyGrid(GridPane grid) {
        byteResult = new byte[size * size];
        Arrays.fill(byteResult, (byte) 0);
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int index = row * size + col;
                ToggleButton cell = new ToggleButton("0");
                cell.setSelected(false);
                cell.setMaxSize(5, 5);
                cell.setStyle("-fx-background-color: white; -fx-text-fill: black;");

                cell.selectedProperty().addListener(((observable, oldValue, newValue) -> {
                    if (newValue) {
                        cell.setText("1");
                        cell.setStyle("-fx-background-color: black; -fx-text-fill: white;");
                        byteResult[index] = 1;
                    } else {
                        cell.setText("0");
                        cell.setStyle("-fx-background-color: white; -fx-text-fill: black;");
                        byteResult[index] = 0;
                    }
                }));
                grid.add(cell, col, row);
            }
        }
    }
}
