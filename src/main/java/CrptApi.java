import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CrptApi {
    private final int requestLimit;
    private final long intervalMillis;
    private final AtomicReference<RateLimiterState> state;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI apiEndpoint;

    public CrptApi(TimeUnit timeUnit, int requestLimit, String apiUrl) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Кол-во запросов не может быть отрицательным");
        }
        this.requestLimit = requestLimit;
        this.intervalMillis = timeUnit.toMillis(1);
        long now = System.currentTimeMillis();
        this.state = new AtomicReference<>(new RateLimiterState(now, 0));

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.apiEndpoint = URI.create(apiUrl);
    }

    private boolean tryAcquireToken() {
        while (true) {
            RateLimiterState current = state.get();
            long now = System.currentTimeMillis();

            if (now - current.windowStart >= intervalMillis) {
                RateLimiterState newState = new RateLimiterState(now, 1);
                if (state.compareAndSet(current, newState)) {
                    return true;
                }
            } else {
                if (current.count >= requestLimit) {
                    return false;
                }
                RateLimiterState newState = new RateLimiterState(current.windowStart, current.count + 1);
                if (state.compareAndSet(current, newState)) {
                    return true;
                }
            }
        }
    }

    public ExternalResponse createDocument(Document document) throws Exception {
        if (!tryAcquireToken()) {
            throw new RejectedExecutionException("Превышено кол-во запросов, повторите попытку позже");
        }

        String json = serializeDocument(document);
        String auth = getToken();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(apiEndpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", auth)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка загрузки документа: " + response.statusCode() + " - " + response.body());
        }

        ExternalResponse externalResponse = parseResponse(response.body());
        return externalResponse;
    }

    private String getToken() {
        //Реализация поулчения токена от честного знака, пока заглушка
        return "Bearer: 287638716ehihi";
    }

    private String serializeDocument(Document document) throws JsonProcessingException {
        return objectMapper.writeValueAsString(document);
    }

    private ExternalResponse parseResponse(String response) throws JsonProcessingException {
        return objectMapper.readValue(response, ExternalResponse.class);
    }

    private static class RateLimiterState {
        final long windowStart;
        final int count;

        RateLimiterState(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    public static class ExternalResponse {
        @JsonProperty("value")
        private String docId;

        public ExternalResponse() {
        }

        public ExternalResponse(String docId) {
            this.docId = docId;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }
    }

    public static class Document {

        @JsonProperty("document_format")
        private String documentFormat;

        @JsonProperty("product_document")
        private String productDocument;

        @JsonProperty("product_group")
        private String productGroup;

        private String signature;

        private String type;

        public Document() {
        }

        public Document(String documentFormat, String productDocument, String productGroup, String signature, String type) {
            this.documentFormat = documentFormat;
            this.productDocument = productDocument;
            this.productGroup = productGroup;
            this.signature = signature;
            this.type = type;
        }

        public String getDocumentFormat() {
            return documentFormat;
        }

        public void setDocumentFormat(String documentFormat) {
            this.documentFormat = documentFormat;
        }

        public String getProductDocument() {
            return productDocument;
        }

        public void setProductDocument(String productDocument) {
            this.productDocument = productDocument;
        }

        public String getProductGroup() {
            return productGroup;
        }

        public void setProductGroup(String productGroup) {
            this.productGroup = productGroup;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
