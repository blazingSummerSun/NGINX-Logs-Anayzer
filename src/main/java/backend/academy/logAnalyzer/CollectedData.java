package backend.academy.logAnalyzer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public record CollectedData(long totalRequests,
                            Map<String, AtomicLong> resourceFrequency,
                            Map<String, AtomicLong> responseCodes,
                            long totalResponseSize, List<Long> responseSizes,
                            Map<String, AtomicLong> ips, Map<String, AtomicLong> users, double percentile) {
}
