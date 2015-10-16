package com.meituan.data.jmxtools.reporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meituan.data.jmxtools.utils.Tuple2;
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

/**
 * Report metrics to Falcon. See
 * http://wiki.sankuai.com/pages/viewpage.action?pageId=217873642
 */
public class FalconReporter extends MetricsReporter {
    static final Logger LOG = LoggerFactory.getLogger(FalconReporter.class);
    static final String FALCON_REPORT_API_URL = "http://127.0.0.1:1988/v1/push";

    private static class PayloadItem {
        enum CounterType {
            GAUGE, COUNTER
        }

        public String endpoint;
        public String metric;
        public int timestamp;
        public int step;
        public Number value;
        public CounterType counterType;
        public String tags;

        public PayloadItem(String endpoint, String metric, int timestamp, Number value) {
            this.endpoint = endpoint;
            this.metric = metric;
            this.timestamp = timestamp;
            this.step = 60;
            this.value = value;
            this.counterType = CounterType.GAUGE;
            this.tags = "";
        }
    }

    public FalconReporter(String serviceHost, String serviceName) {
        super(serviceHost, serviceName);
    }

    private String convertToPayload(List<Tuple2<String, Number>> metrics) throws JsonProcessingException {
        List<PayloadItem> payload = new ArrayList<>();
        for (Tuple2<String, Number> metric : metrics) {
            payload.add(new PayloadItem(serviceHost,
                    serviceName + "." + metric._1,
                    (int) timestamp,
                    metric._2));
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(payload);
    }

    @Override
    public void report(List<Tuple2<String, Number>> metrics) throws MetricsReportException {
        try {
            String payload = convertToPayload(metrics);

            HttpPost post = new HttpPost(FALCON_REPORT_API_URL);
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
