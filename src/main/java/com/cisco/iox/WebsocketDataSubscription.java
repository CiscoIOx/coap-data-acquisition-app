package com.cisco.iox;
import java.io.UnsupportedEncodingException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;

public class WebsocketDataSubscription extends AbstractVerticle {
    
    private HttpClient connectWebsocket;
    String topic = "/topics/api/v1/mw/system.devices.containerA.**";

    
    @Override
    public void start() {
    	subscribe();
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        if(connectWebsocket != null)
            connectWebsocket.close();
    }
  
    private void subscribe() {
        final HttpClient httpClient = VertxContext.getInstance().getHttpClient();
        MultiMap map = new CaseInsensitiveHeaders();
        map.add("Authorization", "Bearer " + VertxContext.getInstance().getAccessToken());
        httpClient.websocket(topic,  map, new Handler<WebSocket>() {
            public void handle(WebSocket ws) {
                ws.handler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
                        byte[] payloadData = event.getBytes();
                        JSONObject readObject = null;
                        try {
                            Object parsedObject = JSONValue.parseWithException(new String(payloadData, "UTF-8"));
                            readObject = (JSONObject) parsedObject;
                            JSONObject context = (JSONObject)readObject.get("context");
                            JSONArray messages = (JSONArray)readObject.get("message");
                            /*for (int i = 0; i < messages.size(); i++) {
                                JSONObject eachMessage = (JSONObject) messages.get(i);
                                JSONArray values = (JSONArray) eachMessage.get("values");
                                String temperature = values.get(0).toString();
                                long timestamp = Long.parseLong(values.get(1).toString());
                                String deviceName = values.get(2).toString();
                                Date date = new Date();
                                date.setTime(timestamp);
                            }*/
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
