package eu.europa.ec.itb.validation.commons.artifact;

public enum ValidationArtifactCombinationApproach {

    ALL("allOf"), ANY("anyOf"), ONE_OF("oneOf");

    private String name;

    ValidationArtifactCombinationApproach(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

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
