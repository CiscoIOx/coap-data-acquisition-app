package com.cisco.iox.analytics.mean;

import java.io.UnsupportedEncodingException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.platform.Verticle;

import com.cisco.iox.ContainerTemperatureSlaApp;
import com.cisco.iox.Util;
import com.cisco.iox.mqtt.MQTTPublish;

public class MeanViolations extends Verticle {

    public static MeanAverageStatsManager slaStatsManager = new MeanAverageStatsManager();
    private HttpClient connectWebsocket;

    @Override
    public void start() {
        addAnalyticsJobs();
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
        super.stop();
        if (connectWebsocket != null)
            connectWebsocket.close();
    }

    private void addAnalyticsJobs() {
        final HttpClient httpClient = ContainerTemperatureSlaApp.getHttpClient();
        JSONObject outliersSchema = Util.loadData("meanViolationsSchema.json");
        final HttpClientRequest dataSchemaPostRequest = httpClient.post("/api/v1/mw/provisioning/dataschemas", new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("meanViolationsSchema add response : " + response.statusCode());
                final HttpClientRequest deleteRequest = httpClient.delete("/api/v1/mw/streamanalytics/jobs/meanSLAJob", new Handler<HttpClientResponse>() {

                    @Override
                    public void handle(HttpClientResponse response) {
                        System.out.println("meanSLAJob delete response : " + response.statusCode());
                        postJob(httpClient);
                    }
                });
                deleteRequest.exceptionHandler(new Handler<Throwable>() {

                    @Override
                    public void handle(Throwable arg0) {
                        System.out.println(arg0);
                        postJob(httpClient);
                    }
                });
                deleteRequest.end();
            }
        });
        dataSchemaPostRequest.headers().add("Content-Type", "application/json");
        dataSchemaPostRequest.end(outliersSchema.toJSONString());
    }

    private void postJob(final HttpClient httpClient) {
        JSONObject analyticsJob = Util.loadData("meanViolationsJob.json");

        final HttpClientRequest analyticsJobRequest = httpClient.post("/api/v1/mw/streamanalytics/jobs", new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("meanViolationsJob add response : " + response.statusCode());
            }
        });
        analyticsJobRequest.headers().add("Content-Type", "application/json");
        analyticsJobRequest.end(analyticsJob.toJSONString());
        subscribe();
    }

    private void subscribe() {
        final HttpClient httpClient = ContainerTemperatureSlaApp.getHttpClient();
        connectWebsocket = httpClient.connectWebsocket("/topics/meanTemperatureViolated", new Handler<WebSocket>() {
            public void handle(WebSocket ws) {
                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        byte[] payloadData = event.getBytes();
                        JSONObject readObject = null;
                        try {

                            Object parsedObject = JSONValue.parseWithException(new String(payloadData, "UTF-8"));
                            readObject = (JSONObject) parsedObject;
                            JSONObject context = (JSONObject) readObject.get("context");
                            JSONArray messages = (JSONArray) readObject.get("message");
                            for (int i = 0; i < messages.size(); i++) {
                                JSONObject eachMessage = (JSONObject) messages.get(i);
                                JSONArray values = (JSONArray) eachMessage.get("values");
                                String deviceName = values.get(0).toString();
                                String average = values.get(1).toString();
                                slaStatsManager.markViolation(deviceName, 1);

                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("device", deviceName);
                                jsonObject.put("average", average);
                                jsonObject.put("timestamp", context.get("timestamp"));
                                System.out.println("Mean Violations: " + jsonObject.toJSONString());
                                MQTTPublish.getInstance().publish("meanViolations", jsonObject);
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

}
