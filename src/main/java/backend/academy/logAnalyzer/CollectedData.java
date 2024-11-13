package backend.academy.logAnalyzer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public record CollectedData(List<String> fileNames, long totalRequests,
                            Map<String, AtomicLong> resourceFrequency,
                            Map<String, Long> responseCodes,
                            long totalResponseSize, List<Long> responseSizes) {
}
