package com.cisco.iox;

import java.io.File;

public class Env {
	
	private static final String APP_PERSISTENT_DIR = "APP_PERSISTENT_DIR";
	private static final String APP_LOG_DIR = "APP_LOG_DIR";
	private static final String APP_CONFIG_FILE = "APP_CONFIG_FILE";
	private static final String APP_ID = "APP_ID";
	private static final String APP_DIR = "APP_DIR";
	private static final String HOME_ABS_PATH = "HOME_ABS_PATH";
	
    private static final String OAUTH_CLIENT_ID = "OAUTH_CLIENT_ID";
    private static final String OAUTH_CLIENT_SECRET = "OAUTH_CLIENT_SECRET";
    private static final String OAUTH_TOKEN_URL = "OAUTH_TOKEN_URL";
    
	private static final Env env = new Env();

	private final String persistentDir;
	private final String baseDir;
	private final String applicationId;
	private final String logsDir;
	private final String oauthClientId;
	private final String oauthClientSecret;
	private final String oauthTokenUrl;
	
	private File configFile;

	private Env() {
		this.applicationId = System.getenv(APP_ID);
		this.persistentDir = System.getenv(APP_PERSISTENT_DIR);
		String configFilePath = System.getenv(APP_CONFIG_FILE);
		if (configFilePath != null) {
			this.configFile = new File(configFilePath);
		}
		this.logsDir = System.getenv(APP_LOG_DIR);
		this.baseDir = System.getenv(HOME_ABS_PATH) + File.separator + System.getenv(APP_DIR);

		this.oauthClientId = System.getenv(OAUTH_CLIENT_ID);
		this.oauthClientSecret = System.getenv(OAUTH_CLIENT_SECRET);
		this.oauthTokenUrl = System.getenv(OAUTH_TOKEN_URL);
		
	}

	public static Env getInstance() {
		return env;
	}

	public String getApplicationId() {
		return applicationId;
	}

	public String getPersistentDir() {
		return persistentDir;
	}

	/**
	 * @return the baseDir
	 */
	public String getBaseDir() {
		return baseDir;
	}

	/**
	 * @return the logsDir
	 */
	public String getLogsDir() {
		return logsDir;
	}

	public boolean isEnvSet() {
		return (getLogsDir() != null) && (getPersistentDir() != null) && (getBaseDir() != null);
	}

	public File getConfigFile() {
		return configFile;
	}

	public boolean isConfigFileExists() {
		return configFile.exists() && configFile.isFile();
	}

	public String getServiceIPAddress(String serviceLabel) {
		return System.getenv(serviceLabel + "_IP_ADDRESS");
	}

	public int getServiceTCPPort(String serviceLabel, int defaultPort) {
		String portEnvVarName = serviceLabel + "_TCP_" + defaultPort + "_PORT";
		final String value = System.getenv(portEnvVarName);
		return value == null ? defaultPort : Integer.parseInt(value);
	}

	public int getServiceUDPPort(String serviceLabel, int defaultPort) {
		final String value = System.getenv(serviceLabel + "_UDP_" + defaultPort);
		return value == null ? defaultPort : Integer.parseInt(value);
	}

	public String getOauthClientId() {
		return oauthClientId;
	}

	public String getOauthClientSecret() {
		return oauthClientSecret;
	}

	public String getOauthTokenUrl() {
		return oauthTokenUrl;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Env [persistentDir=");
		builder.append(persistentDir);
		builder.append(", baseDir=");
		builder.append(baseDir);
		builder.append(", applicationId=");
		builder.append(applicationId);
		builder.append(", logsDir=");
		builder.append(logsDir);
		builder.append(", oauthClientId=");
		builder.append(oauthClientId);
		builder.append(", oauthClientSecret=");
		builder.append(oauthClientSecret);
		builder.append(", oauthTokenUrl=");
		builder.append(oauthTokenUrl);
		builder.append(", configFile=");
		builder.append(configFile);
		builder.append("]");
		return builder.toString();
	}
	
	
}