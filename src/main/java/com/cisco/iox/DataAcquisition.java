package com.cisco.iox;

import java.util.logging.Logger;

import org.json.simple.JSONObject;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

public class DataAcquisition extends AbstractVerticle {

	private static final Logger LOGGER = Logger.getLogger(DataAcquisition.class.getName());

	enum DATA_MODEL_TYPE{
		DATA_SCHEMA,
		DEVICE_TYPE,
		DEVICE
	}
	

	void configureDataModels() {
		configureDataSchema();
	}

	private void configureDataSchema() {
		final HttpClient httpClient = VertxContext.getInstance().getHttpClient();
		JSONObject jsonObjReq = Util.loadData("temperatureSchema.json");
		StringBuilder uri = getDataModelURI(DATA_MODEL_TYPE.DATA_SCHEMA);
		final HttpClientRequest request = httpClient.post(uri.toString(), new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse response) {
				httpClient.close();
				configureDeviceType();
			}
		});
		addRequestHeaders(jsonObjReq, request);
	}
	
	private void configureDeviceType() {
		final HttpClient httpClient = VertxContext.getInstance().getHttpClient();
		JSONObject jsonObjReq = Util.loadData("TemperatureDeviceType.json");
		StringBuilder uri = getDataModelURI(DATA_MODEL_TYPE.DEVICE_TYPE);
		final HttpClientRequest request = httpClient.post(uri.toString(), new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse response) {
				httpClient.close();
				configureDevice();
			}
		});
		addRequestHeaders(jsonObjReq, request);
	}
	
	private void configureDevice() {
		final HttpClient httpClient = VertxContext.getInstance().getHttpClient();
		JSONObject jsonObjReq = Util.loadData("container-1.json");
		StringBuilder uri = getDataModelURI(DATA_MODEL_TYPE.DEVICE);
		final HttpClientRequest request = httpClient.post(uri.toString(), new Handler<HttpClientResponse>() {
			@Override
			public void handle(HttpClientResponse response) {
				httpClient.close();
			}
		});
		addRequestHeaders(jsonObjReq, request);
	}

	private void addRequestHeaders(JSONObject jsonObjReq, final HttpClientRequest request) {
		request.headers().add("Content-Type", "application/json");
		request.headers().add("Authorization", "Bearer " + VertxContext.getInstance().getAccessToken());
		request.end(jsonObjReq.toJSONString());
	}

	private void deleteDataModels() {

	}

	@Override
	public void start() {
		configureDataModels();
	}

	@Override
	public void stop() {
		deleteDataModels();
	}

	private static StringBuilder getDataModelURI(DATA_MODEL_TYPE type) {
		StringBuilder uri = new StringBuilder("/api/v1/mw/provisioning");
		switch (type) {
		case DATA_SCHEMA:
			uri.append("dataschemas");
			break;
		case DEVICE_TYPE:
			uri.append("devicetypes");
			break;
		case DEVICE:
			uri.append("devices");
			break;
		default:
			break;
		}
		return uri;
	}
}