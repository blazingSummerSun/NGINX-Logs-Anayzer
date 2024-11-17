package backend.academy;

import backend.academy.logAnalyzer.CollectedData;
import backend.academy.logAnalyzer.InputParser;
import backend.academy.logAnalyzer.LogAnalyzer;
import backend.academy.logAnalyzer.LogReportGenerator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Main {
    public static void main(String[] args) {
        InputParser inputParser = new InputParser(System.out, System.in);
        inputParser.parseData();
        LogAnalyzer logAnalyzer = new LogAnalyzer(System.out);
        CollectedData parsedData =
            logAnalyzer.analyze(inputParser.path(), inputParser.from(), inputParser.to(), inputParser.agentValue());
        LogReportGenerator logReport =
            new LogReportGenerator(inputParser.format(), inputParser.from(), inputParser.to());
        logReport.generateLog(logAnalyzer.processedFiles(), parsedData, System.out);
    }
}
