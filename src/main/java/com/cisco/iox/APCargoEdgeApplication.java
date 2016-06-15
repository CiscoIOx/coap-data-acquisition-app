package com.cisco.iox;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Semaphore;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.AsyncResult;
import io.vertx.core.AsyncResultHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;


public class APCargoEdgeApplication {

	private static final Logger LOGGER = Logger.getLogger(APCargoEdgeApplication.class.getName());

	static Semaphore semaphore = new Semaphore(0);

	public static void main(String[] args) throws Exception {
		setupLogging();
		checkWhetherNBIIsUp();
		addShutdownHook();
		semaphore.acquire();
	}

	public static void setupLogging() throws IOException {
		File logDir = new File(Env.getInstance().getLogsDir());
		logDir.mkdirs();

		FileHandler fileHandler = new FileHandler(logDir + "/applog.%u.%g.txt", true);
		fileHandler.setFormatter(new SimpleFormatter());
		Logger.getLogger("").addHandler(fileHandler);
	}

	private static void checkWhetherNBIIsUp() {
		Env env = Env.getInstance();

		NetClientOptions options = new NetClientOptions().setSsl(true).setTrustAll(true);
		options.setReconnectAttempts(20);
		options.setReconnectInterval(20 * 1000); // every 10 sec
		
		NetClient client = VertxContext.getInstance().getVertx().createNetClient(options);

		client.connect(env.getServiceTCPPort("nbi", 443), env.getServiceIPAddress("nbi"),
				new AsyncResultHandler<NetSocket>() {
					public void handle(AsyncResult<NetSocket> asyncResult) {
						System.out.println("NBI server is up");
						if (asyncResult.succeeded()) {
							try {
								getAccessToken();
							} catch (UnsupportedEncodingException e) {
								LOGGER.severe("Error while encoding token" + e.getMessage());
							}
						} else {
							asyncResult.cause().printStackTrace();
						}
					}

				});
	}

	private static void getAccessToken() throws UnsupportedEncodingException  {
		final HttpClient httpClient = VertxContext.getInstance().getHttpClient();
		Env env = Env.getInstance();

		String val = env.getOauthClientId() + ":" + env.getOauthClientSecret();
		byte[] encodedBytes = Base64.encodeBase64(val.getBytes());
		byte[] payload = "grant_type=client_credentials".getBytes();
		String token  = new String(encodedBytes, "UTF-8");
		
		final HttpClientRequest request = httpClient.post(env.getOauthTokenUrl(), new Handler<HttpClientResponse>() {

			@Override
			public void handle(HttpClientResponse response) {
				if (response.statusCode() != 200) {
					LOGGER.warning("Oauth Access token Response code - " + response.statusCode());
				} else {
					response.bodyHandler(new Handler<Buffer>() {

						@Override
						public void handle(Buffer payload) {
							JsonNode tokensAsJson;
							try {
								tokensAsJson = new ObjectMapper().readTree(payload.getBytes());
								String oauthAccessToken = tokensAsJson.get("access_token").asText();
								LOGGER.info("access_token: " + oauthAccessToken);
								VertxContext.getInstance().setAccessToken(oauthAccessToken);
								VertxContext.getInstance().getVertx().deployVerticle(new DataAcquisition());
								VertxContext.getInstance().getVertx().deployVerticle(new WebsocketDataSubscription());
							} catch (JsonProcessingException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});
				}
				httpClient.close();
			}

		});
		request.headers().add("Authorization", "Basic " + token);
		request.headers().add("Content-Type", "application/x-www-form-urlencoded");
		request.headers().add("Content-Length", "" + payload.length);
		request.end();

	}
	
	private static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					VertxContext.getInstance().close();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					semaphore.release();
				}
			}
		});
	}
}
