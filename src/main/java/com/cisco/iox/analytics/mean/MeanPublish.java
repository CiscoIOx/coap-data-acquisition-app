package com.cisco.iox.analytics.mean;

import org.json.simple.JSONObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.platform.Verticle;

import com.cisco.iox.ContainerTemperatureSlaApp;
import com.cisco.iox.Util;

public class MeanPublish extends Verticle {
    
    @Override
    public void start() {
        addAnalyticsJobs();
    }
    
    private void addAnalyticsJobs() {
        final HttpClient httpClient = ContainerTemperatureSlaApp.getHttpClient();
        final HttpClientRequest deleteRequest = httpClient.delete("/api/v1/mw/storeandforward/policies/MeanTemperatureForwardPolicy", new Handler<HttpClientResponse>() {

            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("MeanTemperatureForwardPolicy delete response : " + response.statusCode());
                postPolicy();
            }
        });
        deleteRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable arg0) {
               System.out.println(arg0);
               postPolicy();
            }
        });
        deleteRequest.end();
    }

    private void postPolicy() {
        JSONObject outliersSchema = Util.loadData("meanforwardPolicy.json");
        final HttpClientRequest storeAndForwardRequest = ContainerTemperatureSlaApp.getHttpClient().post("/api/v1/mw/storeandforward/policies", new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("MeanTemperatureForwardPolicy  response: " + response.statusCode());
            }

        });
        storeAndForwardRequest.headers().add("Content-Type", "application/json");
        storeAndForwardRequest.end(outliersSchema.toJSONString());
    }
}
