package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum AsciiDocStructure {
    TABLE("|==="), HEADER("===");
    private final String structure;

    AsciiDocStructure(String structure) {
        this.structure = structure;
    }
}
