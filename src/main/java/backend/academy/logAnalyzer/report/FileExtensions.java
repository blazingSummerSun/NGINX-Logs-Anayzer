package backend.academy.logAnalyzer.report;

import lombok.Getter;

/**
 * Enumeration representing file extensions used in the report generation.
 * Each enum constant holds a specific file extension.
 */
@Getter public enum FileExtensions {
    /**
     * Represents the Markdown file extension.
     */
    MARKDOWN(".md"),

    /**
     * Represents the AsciiDoc file extension.
     */
    ASCIIDOC(".adoc");

    /**
     * The file extension string associated with the enum constant.
     */
    private final String extension;

    /**
     * Constructor to initialize the file extension.
     *
     * @param extension the file extension string.
     */
    FileExtensions(String extension) {
        this.extension = extension;
    }

}
