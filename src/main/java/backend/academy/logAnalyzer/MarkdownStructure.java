package backend.academy.logAnalyzer;

import lombok.Getter;

/**
 * Enumeration representing different markdown structures.
 */
@Getter public enum MarkdownStructure {
    /**
     * Represents a Markdown header structure.
     */
    HEADER("###"),

    /**
     * Represents a Markdown table separator for two columns.
     */
    SPLITERATOR_2("| :--------: | :--------: |"),

    /**
     * Represents a Markdown table separator for three columns.
     */
    SPLITERATOR_3("| :--------: | :--------: | :--------: |");

    private final String structure;

    /**
     * Constructs a MarkdownStructure with the specified structure string.
     *
     * @param structure the markdown structure string.
     */
    MarkdownStructure(String structure) {
        this.structure = structure;
    }
}
