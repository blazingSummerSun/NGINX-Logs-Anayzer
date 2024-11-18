package backend.academy;

import backend.academy.logAnalyzer.CollectedData;
import backend.academy.logAnalyzer.InputParser;
import backend.academy.logAnalyzer.LogAnalyzer;
import backend.academy.logAnalyzer.LogReportGenerator;
import lombok.experimental.UtilityClass;

/**
 * <p>The Main class where the main program starts execution. This class is responsible for initializing
 * the log analysis and report generation processes.</p>
 *
 * <h2>Input Example:</h2>
 * <pre>
 * analyzer --path logs/[file_name] --from [ISO8601_date_pattern]
 * --to [ISO8601_date_pattern] --filter-field agent
 * --filter-value "[filter_pattern]" --format markdown
 * </pre>
 * After the program execution, the generated log file will be in the project directory.
 */
@UtilityClass
public class Main {
    /**
     * The main method which serves as the entry point for the program.
     * It initializes the InputParser, LogAnalyzer, and LogReportGenerator,
     * and coordinates the whole log processing workflow.
     *
     * @param args the command line arguments
     */
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
