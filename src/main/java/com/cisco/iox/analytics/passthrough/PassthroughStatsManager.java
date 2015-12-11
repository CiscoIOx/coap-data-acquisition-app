package com.cisco.iox.analytics.passthrough;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

public class PassthroughStatsManager {

    MetricRegistry metrics = new MetricRegistry();
    private static Map<String, Counter> counters = new HashMap<String, Counter>();

    protected Counter createCounter(String counterName) {
        return metrics.counter("Counter - " + counterName);
    }

    public String getStats() {
        JSONArray jsonObject = new JSONArray();
        for (String deviceName : counters.keySet()) {
            JSONObject deviceStats = new JSONObject();
            Counter counter = counters.get(deviceName);
            deviceStats.put("deviceName", deviceName);
            deviceStats.put("total-messages", counter.getCount());
            jsonObject.add(deviceStats);
        }
        return jsonObject.toJSONString();
    }

    public void mark(String deviceName, int count, String temperature, long timestamp) {
        Counter deviceSlaCounter = counters.get(deviceName);
        if(deviceSlaCounter == null) {
            deviceSlaCounter = createCounter(deviceName);
            counters.put(deviceName, deviceSlaCounter);
        }
        deviceSlaCounter.inc(count);
    }
}
