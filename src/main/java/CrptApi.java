import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CrptApi {
    private TimeUnit timeUnit;
    private int requestLimit;

    private final Semaphore semaphore;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.semaphore = new Semaphore(requestLimit);
    }

    public String create(Doc doc, String signature) throws InterruptedException, JsonProcessingException {
        if (timeUnit.inTimeUnit(LocalTime.now())) {
            semaphore.acquire();
            try {
                return work(doc, signature);
            } finally {
                semaphore.release();
            }
        }
        else {
            return work(doc, signature);
        }
    }

    public String work(Doc doc, String signature) throws InterruptedException, JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();

        String json = ow.writeValueAsString(doc);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + signature);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        return restTemplate.postForObject(this.url, request, String.class);
    }

    static class TimeUnit {
        private LocalTime minTime;
        private LocalTime maxTime;

        public TimeUnit(LocalTime minTime, LocalTime maxTime) {
            this.minTime = minTime;
            this.maxTime = maxTime;
        }

        public boolean inTimeUnit(LocalTime currentTime) {
            return maxTime.isAfter(currentTime) && minTime.isBefore(currentTime);
        }
    }

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    class Doc {
        List<DescriptionItem> description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String producerInn;
        private Date productionDate;
        private String productionType;
        List<Product> products;

        class DescriptionItem {
            String participationInn;
        }

        class Product {
            String certificateDocument;
            Date certificateDocumentDate;
            String ownerInn;
            String producerInn;
            Date productionDate;
            String tnvedCode;
            String uitCode;
            String uituCode;
            Date regDate;
            Date regNumber;
        }
    }
}
