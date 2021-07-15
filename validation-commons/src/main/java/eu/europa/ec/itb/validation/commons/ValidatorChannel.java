package eu.europa.ec.itb.validation.commons;

/**
 * Enum used to identify a given validator channel.
 */
public enum ValidatorChannel {

    /** Web form for manual user-driven uploads via UI. */
    FORM("form"),
    /** Email submission. */
    EMAIL("email"),
    /** REST API call. */
	REST_API("rest_api"),
    /** SOAP API call. */
	SOAP_API("soap_api");

    private final String name;

    /**
     * Constructor
     *
     * @param name The channel name.
     */
    ValidatorChannel(String name) {
        this.name = name;
    }

    /**
     * @return The channel's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the validation channel corresponding to the given name.
     *
     * @param name The name.
     * @return The channel enum instance.
     * @throws IllegalArgumentException If the provided name does not match a specific channel.
     */
    public static ValidatorChannel byName(String name) {
        if (FORM.getName().equals(name)) {
            return FORM;
        } else if (EMAIL.getName().equals(name)) {
            return EMAIL;
        } else if (REST_API.getName().equals(name)) {
            return REST_API;
        } else if (SOAP_API.getName().equals(name)) {
            return SOAP_API;            
        } else {
            throw new IllegalArgumentException("Unknown validator channel ["+name+"]");
        }
    }
}
