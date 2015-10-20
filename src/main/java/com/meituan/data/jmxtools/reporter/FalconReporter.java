package com.meituan.data.jmxtools.reporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Report metrics to Falcon. See
 * http://wiki.sankuai.com/pages/viewpage.action?pageId=217873642
 */
public class FalconReporter implements Reporter {
    static final Logger LOG = LoggerFactory.getLogger(FalconReporter.class);

    private final String apiUrl;
    private final int step;
    private final long timestamp;

    private static class PayloadItem {
        public String endpoint;
        public String metric;
        public int timestamp;
        public int step;
        public Number value;
        public Metric.Type counterType;
        public String tags;

        public PayloadItem(String endpoint, String metric, int timestamp, int step,
                           Number value, Metric.Type counterType) {
            this.endpoint = endpoint;
            this.metric = metric;
            this.timestamp = timestamp;
            this.step = step;
            this.value = value;
            this.counterType = counterType;
            this.tags = "";
        }
    }

    FalconReporter(Map<String, String> options) {
        checkNotNull(options, "options is null");
        checkArgument(options.get("apiUrl") != null, "apiUrl not found in options");
        checkArgument(options.get("step") != null, "step not found in options");

        apiUrl = options.get("apiUrl");
        try {
            step = Integer.parseInt(options.get("step"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid step value", e);
        }
        this.timestamp = System.currentTimeMillis() / 1000;
    }

    private String convertToPayload(String serviceHost, String serviceName,
                                    List<Metric> metrics) throws JsonProcessingException {
        List<PayloadItem> payload = new ArrayList<>();
        for (Metric metric : metrics) {
            payload.add(new PayloadItem(serviceHost,
                    serviceName + "." + metric.getName(),
                    (int) this.timestamp,
                    this.step,
                    metric.getValue(),
                    metric.getType()));
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(payload);
    }

    @Override
    public void report(String serviceHost, String serviceName, List<Metric> metrics) throws MetricsReportException {
        checkNotNull(serviceHost, "serviceHost is null");
        checkNotNull(serviceName, "serviceName is null");
        checkNotNull(metrics, "metrics is null");

        try {
            String payload = convertToPayload(serviceHost, serviceName, metrics);

            HttpPost post = new HttpPost(apiUrl);
            post.setEntity(new StringEntity(payload));

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                CloseableHttpResponse resp = client.execute(post);
                LOG.debug("http response status: {}", resp.getStatusLine().toString());

                int statusCode = resp.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new MetricsReportException("POST request returns " + statusCode);
                }
            }

        } catch (IOException e) {
            throw new MetricsReportException("Failed to report metrics to Falcon", e);
        }
    }
}
