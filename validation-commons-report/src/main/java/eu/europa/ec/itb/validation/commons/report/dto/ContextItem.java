package eu.europa.ec.itb.validation.commons.report.dto;

/**
 * DTO class for items placed in reports' context maps.
 */
public class ContextItem {

    private String key;
    private String value;

    /**
     * @return The map key (item name).
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key The map key (item name).
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return The item's value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value The item's value.
     */
    public void setValue(String value) {
        this.value = value;
    }
}
