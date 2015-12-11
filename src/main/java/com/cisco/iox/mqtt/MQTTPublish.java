package com.cisco.iox.mqtt;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;

public class MQTTPublish {

    int qos = 0;
    static String broker = System.getProperty("mqtt_broker");
    String clientId = "Sample";
    MemoryPersistence persistence = new MemoryPersistence();
    private MqttClient client;
    private static MQTTPublish me = new MQTTPublish();
    ThreadPoolExecutor threadPoolExecutor;

    private MQTTPublish()  {
        try {
            client = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            client.connect(connOpts);
            System.out.println("Connected");
            
            threadPoolExecutor = new ThreadPoolExecutor(5, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue());
        } catch (Throwable th) {
            th.printStackTrace();
        }
        
    }
    
    public static MQTTPublish getInstance() {
        return me;
    }

    public void publish(final String topicName, final JSONObject jsondata) {
            Runnable t = new Runnable() {
                public void run() {
                    try {
                        MqttMessage message = new MqttMessage(jsondata.toJSONString().getBytes());
                        message.setQos(qos);
                        client.publish(topicName, message);
                    } catch (MqttException me) {
                        System.out.println("reason " + me.getReasonCode());
                        System.out.println("msg " + me.getMessage());
                        System.out.println("loc " + me.getLocalizedMessage());
                        System.out.println("cause " + me.getCause());
                        System.out.println("excep " + me);
                        me.printStackTrace();
                    }
                }
            };
            threadPoolExecutor.submit(t);
    }

    public static void main(String[] args) {
    	publishTempPassthrough();
    	publicTempMean();
    	publishAmb();
    	publishAmbMean();
    	publishGPSValue();
    }

    private static void publishTempPassthrough()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("device", "test");
        jsonObject.put("timestamp", System.currentTimeMillis());
        jsonObject.put("temperature", 123);
        MQTTPublish.getInstance().publish("passthrough", jsonObject);
    }
    
    private static void publicTempMean()
    {
        JSONObject mean = new JSONObject();
        mean.put("device", "test");
        mean.put("timestamp", System.currentTimeMillis());
        mean.put("average", 67.8);
        MQTTPublish.getInstance().publish("mean", mean);
    }
    
    private static void publishAmb()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("device", "test");
        jsonObject.put("Ambience", 100);
        jsonObject.put("Proximity", 101);
        jsonObject.put("Timestamp", System.currentTimeMillis());
        MQTTPublish.getInstance().publish("VCNLSensor", jsonObject);
    }

    private static void publishAmbMean()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("device", "test");
        jsonObject.put("Ambience", 200);
        jsonObject.put("Proximity", 201);
        jsonObject.put("Timestamp", System.currentTimeMillis());
        MQTTPublish.getInstance().publish("meanAmbienceViolations", jsonObject);
    }

    private static void publishGPSValue()
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("device", "test");
        jsonObject.put("Lat", 100);
        jsonObject.put("Long",100);
        jsonObject.put("Alt", 100);
        jsonObject.put("Speed", 100);
        jsonObject.put("Course", 100);
        jsonObject.put("Timestamp", System.currentTimeMillis());
        MQTTPublish.getInstance().publish("GPSSensor", jsonObject);
    }

}
