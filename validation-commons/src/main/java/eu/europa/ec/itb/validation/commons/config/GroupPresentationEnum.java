package eu.europa.ec.itb.validation.commons.config;

import java.util.Objects;
import java.util.Optional;

/**
 * Enumeration for the different ways to present a validation type group.
 */
public enum GroupPresentationEnum {

    /**
     * Presentation inline as select option groups.
     */
    INLINE("inline"),
    /**
     * Presentation as separate select.
     */
    SPLIT("split");

    private final String identifier;

    /**
     * Constructor.
     *
     * @param identifier The identifier used to configure and match the presentation type.
     */
    GroupPresentationEnum(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return The presentation type's identifier.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Parse the presentation type from the provided value.
     *
     * @param value The value to parse.
     * @return The presentation type of empty if invalid.
     */
    public static Optional<GroupPresentationEnum> of(String value) {
        if (value != null) {
            value = value.toLowerCase();
        }
        if (Objects.equals(value, INLINE.identifier)) {
          return Optional.of(INLINE);
        } else if (Objects.equals(value, SPLIT.identifier)) {
            return Optional.of(SPLIT);
        } else {
            return Optional.empty();
        }
    }

}
