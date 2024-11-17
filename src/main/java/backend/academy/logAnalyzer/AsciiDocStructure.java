package backend.academy.logAnalyzer;

import lombok.Getter;

/**
 * Enumeration representing different AsciiDoc structures.
 * Each enum constant holds a specific AsciiDoc structure string.
 */
@Getter public enum AsciiDocStructure {
    /**
     * Represents an AsciiDoc table structure.
     */
    TABLE("|==="),
    /**
     * Represents an AsciiDoc header structure.
     */
    HEADER("===");
    /**
     * The AsciiDoc structure string associated with the enum constant.
     */
    private final String structure;

    /**
     * Constructor to initialize the AsciiDoc structure.
     *
     * @param structure the AsciiDoc structure string.
     */
    AsciiDocStructure(String structure) {
        this.structure = structure;
    }
}
