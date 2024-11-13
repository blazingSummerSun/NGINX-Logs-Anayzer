package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum FileExtensions {
    MARKDOWN(".md"),
    ASCIIDOC(".adoc");

    private final String extension;

    FileExtensions(String extension) {
        this.extension = extension;
    }

}
