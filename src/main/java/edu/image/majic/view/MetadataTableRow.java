package edu.image.majic.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MetadataTableRow {
    private final StringProperty paramName = new SimpleStringProperty();
    private final StringProperty paramValue = new SimpleStringProperty();

    public MetadataTableRow(String name, String value) {
        this.paramName.set(name);
        this.paramValue.set(value);
    }

    public StringProperty paramNameProperty() {
        return paramName;
    }

    public String getParamName() {
        return paramName.get();
    }

    public StringProperty paramValueProperty() {
        return paramValue;
    }

    public String getParamValue() {
        return paramValue.get();
    }

    public void setParamValue(String value) {
        this.paramValue.set(value);
    }

}
