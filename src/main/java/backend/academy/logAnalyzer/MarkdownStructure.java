package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum MarkdownStructure {
    HEADER("###"), SPLITERATOR_2("| -------- | -------- |"), SPLITERATOR_3("| -------- | -------- | -------- |");
    private final String structure;

    MarkdownStructure(String structure) {
        this.structure = structure;
    }
}
