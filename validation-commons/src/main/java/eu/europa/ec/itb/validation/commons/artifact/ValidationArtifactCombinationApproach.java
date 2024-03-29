package eu.europa.ec.itb.validation.commons.artifact;

/**
 * Enum to determine how multiple validation artifacts are to be combined for the validation.
 */
public enum ValidationArtifactCombinationApproach {

    /** All artifacts must be successfully validated against to consider validation as successful. */
    ALL(ValidationArtifactCombinationApproach.ALL_VALUE),
    /** Any of the artifacts must be successfully validated against to consider validation as successful. */
    ANY(ValidationArtifactCombinationApproach.ANY_VALUE),
    /** At least one of the artifacts must be successfully validated against to consider validation as successful. */
    ONE_OF(ValidationArtifactCombinationApproach.ONE_OF_VALUE);

    public static final String ALL_VALUE = "allOf";
    public static final String ANY_VALUE = "anyOf";
    public static final String ONE_OF_VALUE = "oneOf";

    private final String name;

    /**
     * @param name The enum's text value.
     */
    ValidationArtifactCombinationApproach(String name) {
        this.name = name;
    }

    /**
     * @return The enum's text value.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the enum instance corresponding to the provided text value.
     *
     * @param name The text value.
     * @return The enum instance.
     * @throws IllegalArgumentException if no enum instance could be matched.
     */
    public static ValidationArtifactCombinationApproach byName(String name) {
        if (ALL.name.equals(name)) {
            return ALL;
        } else if (ANY.name.equals(name)) {
            return ANY;
        } else if (ONE_OF.name.equals(name)) {
            return ONE_OF;
        } else {
            throw new IllegalArgumentException("Unknown type name for artifact combination approach ["+name+"]");
        }
    }

}
