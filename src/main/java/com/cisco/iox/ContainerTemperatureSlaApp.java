package com.cisco.iox;

import java.net.URL;
import java.util.concurrent.Semaphore;

import org.json.simple.JSONObject;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.net.NetClient;
import org.vertx.java.core.net.NetSocket;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;
import org.vertx.java.platform.Verticle;

import com.cisco.iox.analytics.mean.MeanPublish;
import com.cisco.iox.analytics.mean.MeanViolations;
import com.cisco.iox.analytics.mean.ambience.MeanAmbienceViolations;
import com.cisco.iox.analytics.passthrough.TemperaturePassThroughJob;
import com.cisco.iox.analytics.signalnoise.OutliersSLAViolationJob;

public class ContainerTemperatureSlaApp {

	//private static final String MW_HOST = "10.78.106.25";
	private static final String MW_HOST = "localhost";

	private static PlatformManager pm;

	static Semaphore semaphore = new Semaphore(0);

	public static void main(String[] args) throws Exception {
		pm = PlatformLocator.factory.createPlatformManager();
		checkWhetherMiddlewareIsUp();
		addShutdownHook();
		semaphore.acquire();
	}

	private static void checkWhetherMiddlewareIsUp() {
		NetClient client = pm.vertx().createNetClient();
		client.setReconnectAttempts(-1);
		client.setReconnectInterval(10 * 1000); // every 10 sec

		client.connect(9083, MW_HOST, new AsyncResultHandler<NetSocket>() {
			public void handle(AsyncResult<NetSocket> asyncResult) {
				System.out.println("MW NBI server is up");
				if (asyncResult.succeeded()) {
					checkProvisioningServiceIsUp();
				} else {
					asyncResult.cause().printStackTrace();
				}
			}
		});
	}

	static void checkProvisioningServiceIsUp() {
		System.out.println("Checking wheher pservice is up");
		final HttpClient httpClient = getHttpClient();
		final HttpClientRequest request = httpClient.get("/api/v1/mw/provisioning/dataschemas",
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						if (response.statusCode() != 200) {
							try {
								System.out.println(response.statusCode() + "Provisioning service not yet up. waiting for 10 sec and will retry");
								Thread.sleep(10*1000);
								checkProvisioningServiceIsUp();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else {
							System.out.println("We have connected! Socket is " + response.statusCode());
							startRestServer();
							provisionMiddleware();
							provisionMwHelper(new String[]{"Schema_GPS.json",
															"Schema_VCNL.json",
															"DeviceType_GPS_VCNL.json",
															"Device_GPS_VCNL-1.json"}, 
											  new Class[]{
													  IExpoDemoVerticle.class, 
													  MeanAmbienceViolations.class
													  });
						}

					}
				});
		request.exceptionHandler(new Handler<Throwable>() {

			@Override
			public void handle(Throwable arg0) {
				arg0.printStackTrace();
			}
			
		});
		request.end();
	}

	private static void addShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					getHttpClient().close();
					pm.stop();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					semaphore.release();
				}
			}
		});
	}

	private static void startRestServer() {

		RouteMatcher routeMatcher = new RouteMatcher();
		routeMatcher.get("/job/:jobName/stats", new Handler<HttpServerRequest>() {
			@Override
			public void handle(HttpServerRequest request) {
				HttpServerResponse response = request.response();
				String jobName = request.params().get("jobName");
				String stats = "";
				switch (jobName) {
				case "outliers":
					stats = OutliersSLAViolationJob.slaStatsManager.getStats();
					break;
				case "mean":
					stats = MeanViolations.slaStatsManager.getStats();
					break;
				case "passthrough":
					stats = TemperaturePassThroughJob.slaStatsManager.getStats();
					break;
				case "all":
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("PassThrough", TemperaturePassThroughJob.slaStatsManager.getStats());
					jsonObject.put("Mean Temperature Violation", MeanViolations.slaStatsManager.getStats());
					jsonObject.put("After Noise Filtering SLA Violation",
							OutliersSLAViolationJob.slaStatsManager.getStats());
					stats = jsonObject.toJSONString();
					break;
				}
				response.headers().set("Content-Type", "application/json");
				response.end(stats);
			}
		});

		routeMatcher.post("/job/:jobName", new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest request) {
				String jobName = request.params().get("jobName");
				switch (jobName) {
				case "outliers":
					deployVerticle(OutliersSLAViolationJob.class, null);
					break;
				case "mean":
					deployVerticle(MeanPublish.class, null);
					break;
				}
				request.response().end();
			}
		});

		routeMatcher.noMatch(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest request) {
				request.response().end("Pharma Temperature SLA App running!");
			}
		});
		HttpServer httpServer = pm.vertx().createHttpServer().requestHandler(routeMatcher);
		httpServer.listen(9090, "0.0.0.0");
	}

	private static void provisionMiddleware() {
		final HttpClient httpClient = getHttpClient();

		JSONObject temperatureSchema = Util.loadData("temperatureSchema.json");
		final HttpClientRequest request = httpClient.post("/api/v1/mw/provisioning/dataschemas",
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						System.out.println("Temperature Schema add response : " + response.statusCode());
						httpClient.close();
						provisionShippingContainers();
					}
				});
		request.headers().add("Content-Type", "application/json");
		request.end(temperatureSchema.toJSONString());
	}
	
	private static void provisionMwHelper(String[] asArgs, final Class<? extends Verticle>[] asClassVerticle)
	{
		if (asArgs.length == 0)
		{
			if (asClassVerticle != null)
			{
				for (int i = 0; i < asClassVerticle.length; i++)
				{
					deployVerticle(asClassVerticle[i], null);
				}
			}
			
			return;
		}

		String sMWUrl;
		final String sFileName = asArgs[0];
		final String[] asArgsNew = new String[asArgs.length - 1];
		System.arraycopy(asArgs, 1, asArgsNew, 0, asArgsNew.length);
		
		final HttpClient httpClient = getHttpClient();
		JSONObject jsonObjReq = Util.loadData(sFileName);
		if (sFileName.startsWith("Schema_"))
		{
			sMWUrl = "/api/v1/mw/provisioning/dataschemas";
		}
		else if (sFileName.startsWith("DeviceType_"))
		{
			sMWUrl = "/api/v1/mw/provisioning/devicetypes";
		}
		else if (sFileName.startsWith("Device_"))
		{
			sMWUrl = "/api/v1/mw/provisioning/devices";			
		}
		else
		{
			provisionMwHelper(asArgsNew, asClassVerticle);
			return;
		}
		
		
		System.out.println(String.format("MWURL %s", sMWUrl));
		System.out.println(String.format("Request Content %s", jsonObjReq.toJSONString()));
		final HttpClientRequest request = httpClient.post(sMWUrl,
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						System.out.println(String.format("Response for content %s", sFileName));
						httpClient.close();
						provisionMwHelper(asArgsNew, asClassVerticle);
					}
				});
		request.headers().add("Content-Type", "application/json");
		request.end(jsonObjReq.toJSONString());
	}
	
	public static HttpClient getHttpClient() {
		HttpClient httpClient = pm.vertx().createHttpClient();
		httpClient.setHost(MW_HOST);
		httpClient.setPort(9083);
		httpClient.setSSL(true);
		httpClient.setTrustAll(true);
		httpClient.setVerifyHost(false);
		httpClient.setKeepAlive(true);
		httpClient.setMaxPoolSize(10);

		return httpClient;
	}

	private static void provisionShippingContainers() {
		JSONObject deviceType = Util.loadData("TemperatureDeviceType.json");
		final HttpClient httpClient = getHttpClient();
		final HttpClientRequest request = httpClient.post("/api/v1/mw/provisioning/devicetypes",
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						System.out.println("DeviceType add response : " + response.statusCode());
						httpClient.close();
						addContainersDevices();
					}

				});
		request.headers().add("Content-Type", "application/json");
		request.end(deviceType.toJSONString());
	}

	private static HttpClientRequest addContainersDevices() {
		JSONObject container1Device = Util.loadData("container-1.json");
		final HttpClient httpClient = getHttpClient();
		final HttpClientRequest request = httpClient.post("/api/v1/mw/provisioning/devices",
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						System.out.println("Container-1 Device add response : " + response.statusCode());
						httpClient.close();
						addContainer2Device();
					}
				});
		request.headers().add("Content-Type", "application/json");
		request.end(container1Device.toJSONString());
		return request;
	}

	private static void addContainer2Device() {
		final JSONObject containerDevice = Util.loadData("container-2.json");
		final HttpClient httpClient = getHttpClient();
		final HttpClientRequest request = httpClient.post("/api/v1/mw/provisioning/devices",
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						System.out.println("Container-3 Device add response : " + response.statusCode());
						httpClient.close();
						addContainer3Device();
					}
				});
		request.headers().add("Content-Type", "application/json");
		request.end(containerDevice.toJSONString());
	}

	private static void addContainer3Device() {
		final JSONObject containerDevice = Util.loadData("container-3.json");
		final HttpClient httpClient = getHttpClient();
		final HttpClientRequest request = httpClient.post("/api/v1/mw/provisioning/devices",
				new Handler<HttpClientResponse>() {
					@Override
					public void handle(HttpClientResponse response) {
						System.out.println("Container-3 Device add response : " + response.statusCode());
						httpClient.close();
						deployVerticle(TemperaturePassThroughJob.class, null);
						deployVerticle(MeanViolations.class, null);
					}
				});
		request.headers().add("Content-Type", "application/json");
		request.end(containerDevice.toJSONString());
	}

	static void deployVerticle(Class<? extends Verticle> verticleClass, JsonObject conf) {
		pm.deployVerticle(verticleClass.getName(), conf, new URL[] {}, 1, null, null);
	}
}
