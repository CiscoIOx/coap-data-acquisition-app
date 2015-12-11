package com.cisco.iox.analytics.signalnoise;

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

public class OutliersSLAViolationJob extends Verticle {

    public static SLAStatsManager slaStatsManager = new SLAStatsManager();

    @Override
    public void start() {
        addAnalyticsJobs();
    }
    
    private void addAnalyticsJobs() {
        final HttpClient httpClient = ContainerTemperatureSlaApp.getHttpClient();
        JSONObject outliersSchema = Util.loadData("outliersSchema");
        
        final HttpClientRequest dataSchemaPostRequest = httpClient.post("/api/v1/mw/provisioning/dataschema", new Handler<HttpClientResponse>() {
           
            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("outliersSchema add response : " + response.statusCode());
                JSONObject analyticsJob = Util.loadData("outliersTemperatureJob");
               
                final HttpClientRequest analyticsJobRequest = httpClient.post("/api/v1/mw/streamAnalytics/jobs", new Handler<HttpClientResponse>() {
                    @Override
                    public void handle(HttpClientResponse response) {
                        System.out.println("outliersTemperatureJob add response : " + response.statusCode());
                    }
                });
                analyticsJobRequest.headers().add("Content-Type", "application/json");
                analyticsJobRequest.end(analyticsJob.toJSONString());
                
                response.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void arg0) {
                        httpClient.close();
                    }
                });
                subscribe();
            }

        });
        dataSchemaPostRequest.headers().add("Content-Type", "application/json");
        dataSchemaPostRequest.end(outliersSchema.toJSONString());
    }

    private void subscribe() {
        final HttpClient client = ContainerTemperatureSlaApp.getHttpClient();
        client.connectWebsocket("/topics/ouliersViolations", new Handler<WebSocket>() {
            public void handle(WebSocket ws) {
                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        byte[] payloadData = event.getBytes();
                        JSONObject readObject = null;
                        try {
                            Object parsedObject = JSONValue.parseWithException(new String(payloadData, "UTF-8"));
                            readObject = (JSONObject) parsedObject;
                            JSONObject context = (JSONObject)readObject.get("context");
                            JSONArray messages = (JSONArray)readObject.get("message");
                            for (int i = 0; i < messages.size(); i++) {
                                JSONObject eachMessage = (JSONObject) messages.get(i);
                                JSONArray values = (JSONArray) eachMessage.get("values");
                                String deviceName = values.get(0).toString();
                                String allCount = values.get(1).toString();
                                String validCount = values.get(2).toString();
                                String inValidCount = values.get(3).toString();
                                String percentage = values.get(4).toString();
                                slaStatsManager.markViolation(deviceName, 1);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("device", deviceName);
                                jsonObject.put("percentage", percentage);
                                jsonObject.put("timestamp", context.get("timestamp"));
                                MQTTPublish.getInstance().publish("outliers", jsonObject);
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
