package com.cisco.iox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class Util {

    public static JSONObject loadData(String fileName) {
        InputStream resourceAsStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            resourceAsStream = com.cisco.iox.ContainerTemperatureSlaApp.class.getClassLoader().getResourceAsStream(fileName);
            inputStreamReader = new InputStreamReader(resourceAsStream);
            ObjectMapper objectMapper = new ObjectMapper();
            JSONObject readValue;
            try {
                readValue = objectMapper.readValue(inputStreamReader, JSONObject.class);
                return readValue;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } finally {
            closeIO(inputStreamReader);
            closeIO(resourceAsStream);
        }
    }

    private static void closeIO(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
