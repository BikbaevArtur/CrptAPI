package ru.crpt.ismp;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final String url;
    private final String token;
    private final Lock lock = new ReentrantLock();

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private AtomicLong lastRequestTime = new AtomicLong(System.currentTimeMillis());
    private AtomicInteger requestCount = new AtomicInteger(0);

    /**
     * @param url          ссылка запроса
     * @param token        токен авторизации
     * @param timeUnit     установка времени
     * @param requestLimit кол-во запросов
     */
    public CrptApi(String url, String token, TimeUnit timeUnit, int requestLimit) {
        this.url = url;
        this.token = String.format("Bearer<%s>", token);
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

    /**
     * @param document  отправляемый документ
     * @param signature открепленная подпись
     * @return идентификатор документа в ГИС МТ или statusCode
     */
    public String creatDocument(Document document, String signature) {

        try {
            lock.lock();
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastRequestTime.get();

            if (elapsedTime > timeUnit.toMillis(1)) {
                requestCount.set(0);
                lastRequestTime.set(currentTime);
            }

            if (requestCount.get() >= requestLimit) {
                throw new RuntimeException("Превышено ограничение количества запросов");
            }
            requestCount.incrementAndGet();


            try (CloseableHttpClient httpClient = HttpClientBuilder
                    .create()
                    .build()) {

                ClassicHttpRequest httpPost = ClassicRequestBuilder
                        .post(url)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", token)
                        .build();


                String documentJson = objectMapper.writeValueAsString(document);
                String jsonBody = String.format("{ \"product_document\": \"%s\", \"document_format\": \"MANUAL\", \"type\": \"LP_INTRODUCE_GOODS\", \"signature\": \"%s\" }", documentJson, signature);

                httpPost.setEntity(new StringEntity(jsonBody));


                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getCode();
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (statusCode == 200) {
                        return responseBody;
                    } else {
                        return String.valueOf(statusCode);
                    }
                }
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }

        } finally {
            lock.unlock();
        }
    }
}




@AllArgsConstructor
@Getter
@Setter
@ToString
class Description {
    private String participantInn;
}


@AllArgsConstructor
@Getter
@Setter
@ToString

 class Document {
    Description description;
    String doc_id;
    String doc_status;
    String doc_type;
    boolean importRequest;
    String owner_inn;
    String participant_inn;
    String producer_inn;
    String production_date;
    String production_type;
    List<Product> products;
    String reg_date;
    String reg_number;

}

@AllArgsConstructor
@Getter
@Setter
@ToString
  class Product {
    private String certificate_document;
    private String certificate_document_date;
    private String certificate_document_number;
    private String owner_inn;
    private String producer_inn;
    private String production_date;
    private String tnved_code;
    private String uit_code;
    private String uitu_code;


}


class Main{
    public static void main(String[] args) {
        Description description = new Description("123123123");
        Product product_1 = new Product(
                "certificate_document1",
                "certificate_document_date1",
                "certificate_document_number1",
                "owner_inn1",
                "producer_inn1",
                "producer_date1",
                "tnved_code1",
                "unit_code1",
                "uitu_code1");
        Product product_2 = new Product(
                "certificate_document2",
                "certificate_document_date2",
                "certificate_document_number2",
                "owner_inn2",
                "producer_inn2",
                "producer_date2",
                "tnved_code2",
                "unit_code2",
                "uitu_code2");

        List<Product> productList = new ArrayList<>() {{
            add(product_1);
            add(product_2);
        }};

        Document document = new Document(
                description,
                "doc_id",
                "doc_status",
                "doc_type",
                true,
                "owner_inn",
                "participant_inn",
                "producer_inn",
                "production_date",
                "production_type",
                productList,
                "reg_date",
                "reg_number");

        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        String name = "crptApi";

        String signature = "123123123";


        CrptApi crptApi3 = new CrptApi(url,"asdasd",TimeUnit.SECONDS,10);

        Thread thread  = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100 ; i++) {
                    System.out.println(crptApi3.creatDocument(document, signature));
                }
            }

        });
        thread.start();


        for (int i = 0; i < 100 ; i++) {
            System.out.println(crptApi3.creatDocument(document, signature));
        }

    }
}

