package backend.academy.logAnalyzer;

import lombok.Getter;

@Getter public enum AsciiDocStructure {
    TABLE("|===");
    private final String structure;

    AsciiDocStructure(String structure) {
        this.structure = structure;
    }
}
