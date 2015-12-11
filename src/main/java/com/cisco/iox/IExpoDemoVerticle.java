package com.cisco.iox;
import java.io.UnsupportedEncodingException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.WebSocket;
import org.vertx.java.platform.Verticle;

import com.cisco.iox.mqtt.MQTTPublish;

public class IExpoDemoVerticle extends Verticle {
    
    private HttpClient webSocktGPSSensor;
    private HttpClient ws;
    
    @Override
    public void start() {
    	subscribe();
    }
    
    @Override
    public void stop() {
        super.stop();
        if(webSocktGPSSensor != null)
            webSocktGPSSensor.close();
        
        if (ws != null)
        	ws.close();
    }
    /**
     * {
     * 	"message":[{"values":[77.695562,12.935613,910.9,1.06,191.09]}],
     * 	"context":{"timestamp":1449630044579,"sourceType":"SENSOR","dataSchemaId":"GPSSchema","deviceId":"container1","sourceId":"GPSSensor"}
     * }
     */
    
    private void subscribe() {
        final HttpClient client = ContainerTemperatureSlaApp.getHttpClient();
        webSocktGPSSensor = client.connectWebsocket("/topics/system.devices.*.sensors.GPSSensor", new Handler<WebSocket>() {
            public void handle(WebSocket ws) {
                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        byte[] payloadData = event.getBytes();
                        JSONObject readObject = null;
                        try {
                            Object parsedObject = JSONValue.parseWithException(new String(payloadData, "UTF-8"));
                            readObject = (JSONObject) parsedObject;
                            JSONObject context = (JSONObject)readObject.get("context"); // sensor id
                            JSONArray messages = (JSONArray)readObject.get("message");
                            long timestamp = Long.parseLong(String.valueOf(context.get("timestamp")));
                            String deviceName = String.valueOf(context.get("deviceId"));
                            for (int i = 0; i < messages.size(); i++) {
                                JSONObject eachMessage = (JSONObject) messages.get(i);
                                JSONArray values = (JSONArray) eachMessage.get("values");
                                
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("device", deviceName);
                                jsonObject.put("Lat", values.get(0));
                                jsonObject.put("Long", values.get(1));
                                jsonObject.put("Alt", values.get(2));
                                jsonObject.put("Speed", values.get(3));
                                jsonObject.put("Course", values.get(4));
                                jsonObject.put("Timestamp", String.valueOf(timestamp));
                                MQTTPublish.getInstance().publish("GPSSensor", jsonObject);
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

        /**
         * {
         * 	"message":[{"values":[903.0,2288.0]}],
         * 	"context":{"timestamp":1449630255194,"sourceType":"SENSOR","dataSchemaId":"VCNLSchema","deviceId":"container1","sourceId":"VCNLSensor"}
         * }
         */
        ws = client.connectWebsocket("/topics/system.devices.*.sensors.VCNLSensor", new Handler<WebSocket>() {
            public void handle(WebSocket ws) {
                ws.dataHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        byte[] payloadData = event.getBytes();
                        JSONObject readObject = null;
                        try {
                            Object parsedObject = JSONValue.parseWithException(new String(payloadData, "UTF-8"));
                            readObject = (JSONObject) parsedObject;
                            JSONObject context = (JSONObject)readObject.get("context"); // sensor id
                            JSONArray messages = (JSONArray)readObject.get("message");
                            long timestamp = Long.parseLong(String.valueOf(context.get("timestamp")));
                            String deviceName = String.valueOf(context.get("deviceId"));
                            for (int i = 0; i < messages.size(); i++) {
                                JSONObject eachMessage = (JSONObject) messages.get(i);
                                JSONArray values = (JSONArray) eachMessage.get("values");
                                
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("device", deviceName);
                                jsonObject.put("Ambience", values.get(0));
                                jsonObject.put("Proximity", values.get(1));
                                jsonObject.put("Timestamp", String.valueOf(timestamp));
                                MQTTPublish.getInstance().publish("VCNLSensor", jsonObject);
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
