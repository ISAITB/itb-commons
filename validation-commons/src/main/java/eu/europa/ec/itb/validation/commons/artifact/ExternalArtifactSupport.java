package eu.europa.ec.itb.validation.commons.artifact;

public enum ExternalArtifactSupport {

    REQUIRED("required"), OPTIONAL("optional"), NONE("none");

    private String name;

    ExternalArtifactSupport(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ExternalArtifactSupport byName(String name) {
        if (REQUIRED.name.equals(name)) {
            return REQUIRED;
        } else if (OPTIONAL.name.equals(name)) {
            return OPTIONAL;
        } else if (NONE.name.equals(name)) {
            return NONE;
        } else {
            throw new IllegalArgumentException("Unknown type name for external artifact support ["+name+"]");
        }
    }
}
