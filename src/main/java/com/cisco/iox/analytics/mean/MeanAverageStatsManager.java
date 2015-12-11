package com.cisco.iox.analytics.mean;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;


public class MeanAverageStatsManager {

    MetricRegistry metrics = new MetricRegistry();
    private static Map<String, Counter> counters = new HashMap<String, Counter>();

    protected Counter createCounter(String counterName) {
        return metrics.counter("Counter - " + counterName);
    }

    public synchronized void markViolation(String deviceName, long slaViolated) {
        Counter deviceSlaCounter = counters.get(deviceName);
        if(deviceSlaCounter == null) {
            deviceSlaCounter = createCounter(deviceName);
            counters.put(deviceName, deviceSlaCounter);
        }
        deviceSlaCounter.inc(slaViolated);
    }

    public String getStats() {
        JSONArray jsonObject = new JSONArray();
        for (String deviceName : counters.keySet()) {
            JSONObject deviceStats = new JSONObject();
            Counter counter = counters.get(deviceName);
            deviceStats.put("deviceName", deviceName);
            deviceStats.put("total-mean-violations", counter.getCount());
            jsonObject.add(deviceStats);
        }
        return jsonObject.toJSONString();
    }
}
