package com.cisco.iox.analytics.passthrough;
import java.io.UnsupportedEncodingException;
import java.util.Date;

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

public class TemperaturePassThroughJob extends Verticle {
    
    public static PassthroughStatsManager slaStatsManager = new PassthroughStatsManager();
    private HttpClient connectWebsocket;
    
    @Override
    public void start() {
        addJob();
    }
    
    @Override
    public void stop() {
        super.stop();
        if(connectWebsocket != null)
            connectWebsocket.close();
    }
    
    private void addJob() {
        final HttpClient httpClient = ContainerTemperatureSlaApp.getHttpClient();
        JSONObject temperaturePassThroughSchema = Util.loadData("temperaturePassThroughSchema.json");
        final HttpClientRequest datSchemaRequest = httpClient.post("/api/v1/mw/provisioning/dataschemas", new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("temperaturePassThroughSchema add response : " + response.statusCode());
                final HttpClientRequest deleteRequest = httpClient.delete("/api/v1/mw/streamanalytics/jobs/passThroughTemperatureValues", new Handler<HttpClientResponse>() {

                    @Override
                    public void handle(HttpClientResponse response) {
                        System.out.println("passThroughTemperatureValuesJob delete response : " + response.statusCode());
                        postPassthruJob(httpClient);
                    }
                });
                deleteRequest.exceptionHandler(new Handler<Throwable>() {

                    @Override
                    public void handle(Throwable arg0) {
                       System.out.println(arg0);
                       postPassthruJob(httpClient);
                    }
                });
                deleteRequest.end();
            }

        });
        datSchemaRequest.headers().add("Content-Type", "application/json");
        datSchemaRequest.end(temperaturePassThroughSchema.toJSONString());
    }
    
    private void postPassthruJob(final HttpClient httpClient) {
        JSONObject passThroughAnalyticsJob = Util.loadData("passThroughTemperatureValuesJob.json");
        final HttpClientRequest request = httpClient.post("/api/v1/mw/streamanalytics/jobs", new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                System.out.println("passThroughTemperatureValuesJob add response : " + response.statusCode());
                subscribe();
            }
        });
        request.headers().add("Content-Type", "application/json");
        request.end(passThroughAnalyticsJob.toJSONString());
    }

    private void subscribe() {
        final HttpClient client = ContainerTemperatureSlaApp.getHttpClient();
        connectWebsocket = client.connectWebsocket("/topics/temperature-passthrough", new Handler<WebSocket>() {
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
                                String temperature = values.get(0).toString();
                                long timestamp = Long.parseLong(values.get(1).toString());
                                String deviceName = values.get(2).toString();
                                Date date = new Date();
                                date.setTime(timestamp);
                                slaStatsManager.mark(deviceName, 1, temperature, timestamp);
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("device", deviceName);
                                jsonObject.put("temperature", temperature);
                                jsonObject.put("timestamp", timestamp);
                                MQTTPublish.getInstance().publish("passthrough", jsonObject);
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
