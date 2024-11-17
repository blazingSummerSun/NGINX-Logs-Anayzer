package backend.academy.logAnalyzer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A record that stores various collected data metrics related to log analysis.
 *
 * @param totalRequests     The total number of requests processed.
 * @param resourceFrequency A map where the keys resource names and the values are the frequency of requests
 *                          for each resource.
 * @param responseCodes     A map where the keys are response codes and the values are the frequency of each
 *                          response code.
 * @param totalResponseSize The total size of all responses.
 * @param responseSizes     A list of individual response sizes.
 * @param ips               A map where the keys are IP addresses and the values are the frequency of requests
 *                          from each IP address.
 * @param users             A map where the keys are usernames and the values are the frequency of requests
 *                          from each user.
 * @param percentile        The percentile value calculated from the response sizes.
 */
public record CollectedData(long totalRequests,
                            Map<String, AtomicLong> resourceFrequency,
                            Map<String, AtomicLong> responseCodes,
                            long totalResponseSize, List<Long> responseSizes,
                            Map<String, AtomicLong> ips, Map<String, AtomicLong> users, double percentile) {
}
