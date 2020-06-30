package eu.europa.ec.itb.validation.commons.web;

public class KeyWithLabel {

    private String key;
    private String label;

    public KeyWithLabel(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

}
