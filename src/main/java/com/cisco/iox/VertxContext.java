package com.cisco.iox;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;

public class VertxContext {

	private static VertxContext vertxContext = new VertxContext();
	private Vertx vertx;
	private String accessToken;
	
	private VertxContext() {
		vertx = Vertx.factory.vertx();
	}
	
	public static VertxContext getInstance() {
		return vertxContext;
	}

	public Vertx getVertx() {
		return vertx;
	}
	
	public HttpClient getHttpClient() {
		HttpClientOptions options = new HttpClientOptions()
				.setKeepAlive(false)
				.setDefaultHost(Env.getInstance().getServiceIPAddress("nbi"))
				.setSsl(true)
				.setTrustAll(true)
				.setVerifyHost(false)
				.setMaxPoolSize(10)
				.setDefaultPort(Env.getInstance().getServiceTCPPort("nbi",443));
				
		HttpClient httpClient = vertx.createHttpClient(options);
		return httpClient;
	}

	public void close() {
		vertx.close();
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	

}
