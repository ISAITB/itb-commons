package eu.europa.ec.itb.validation.commons.web;

/**
 * DTO to record a key and label pair.
 */
public class KeyWithLabel {

    private final String key;
    private final String label;

    /**
     * Constructor.
     *
     * @param key The key.
     * @param label The label.
     */
    public KeyWithLabel(String key, String label) {
        this.key = key;
        this.label = label;
    }

    /**
     * @return The key.
     */
    public String getKey() {
        return key;
    }

    /**
     * @return The label.
     */
    public String getLabel() {
        return label;
    }

}
