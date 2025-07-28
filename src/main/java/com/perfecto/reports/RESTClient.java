package com.perfecto.reports;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.support.ui.FluentWait;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;

public class RESTClient implements Runnable{
	
	private static final Log logger = LogFactoryImpl.getLog(RESTClient.class);

	private String reportBaseURL;
	protected static final int TIMEOUT_MILLIS = 60000;
	private String securityToken;

	public RESTClient() throws Exception {
		
		securityToken = "";
		
		String serverURL = (ConfigurationManager.getBundle().getString("remote.server").split("\\.")[0])
				.replace("http://", "").replace("https://", "");
		
		this.reportBaseURL = "https://" + serverURL + ".reporting.perfectomobile.com";
				
		for (@SuppressWarnings("unchecked")
		Iterator<String> i = getBundle().getKeys(); i.hasNext();) {
			String key = i.next();
			if (key.contains("securityToken")) {
				this.securityToken = getBundle().getString(key);
			}
		}

		if ("".equals(this.securityToken)) {
			
			this.securityToken = getBundle().getString("perfecto.offlineToken", "");

			if ("".equals(this.securityToken)) {
				throw new Exception("Security Token or Offline Token is required.");
			}

		}
		
		logger.debug("Report Server URL : " + reportBaseURL);
	}

	public String getReportBaseURL() {
		return this.reportBaseURL;
	}

	private boolean isProxyHostProvided() {

		String proxyHost = ConfigurationManager.getBundle().getString("proxyHost", "");
		return !"".equals(proxyHost);

	}

	protected void addProxyDetailsIfRequired(HttpClientBuilder clientBuilder) throws Exception {

		String proxyHost = ConfigurationManager.getBundle().getString("proxyHost", "");
		String proxyPort = ConfigurationManager.getBundle().getString("proxyPort", "");
		String proxyUser = ConfigurationManager.getBundle().getString("proxyUser", "");
		String proxyPassword = ConfigurationManager.getBundle().getString("proxyPassword", "");
		String proxyDomain = ConfigurationManager.getBundle().getString("proxyDomain", "");

		if (isProxyHostProvided()) {

			if ("".equals(proxyPort)) {
				throw new Exception("Please mention the NTLM port in the application properties file");
			}

			int intPort = Integer.parseInt(proxyPort);

			InetAddress addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			NTCredentials ntCreds = new NTCredentials(proxyUser, proxyPassword, hostname, proxyDomain);

			AuthScope authScope = new AuthScope(proxyHost, intPort);
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(authScope, ntCreds);

			HttpHost httpHost = new HttpHost(proxyHost, intPort);
			clientBuilder.useSystemProperties();
			clientBuilder.setProxy(httpHost);
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
			clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

		}

	}
	

	protected String getSecurityToken() throws Exception {
		return this.securityToken;
	}
	
	protected void addDefaultRequestHeaders(HttpRequestBase request) throws Exception {
		String accessToken = getSecurityToken();
		request.addHeader("PERFECTO_AUTHORIZATION", accessToken);
	}
	
	protected JsonObject getTestExecutionDetails(String executionId)
			throws Exception {
		
		URIBuilder uriBuilder = new URIBuilder(this.reportBaseURL + "/export/api/v1/test-executions");

		// Optional: Filter by range. In this example: retrieve test executions
		// of the past month (result may contain tests of multiple driver
		// executions)

		// Optional: Filter by a specific driver execution ID that you can
		// obtain at script execution
		uriBuilder.addParameter("externalId[0]", executionId);

		HttpGet getExecutions = new HttpGet(uriBuilder.build());
		addDefaultRequestHeaders(getExecutions);
		
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		
		addProxyDetailsIfRequired(clientBuilder);
		
		try(CloseableHttpClient httpClient = clientBuilder.build()){
			HttpResponse response  = httpClient.execute(getExecutions);
			
			InputStream responseStream = response.getEntity().getContent();
			
			try (InputStreamReader inputStreamReader = new InputStreamReader(responseStream)) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				return gson.fromJson(IOUtils.toString(inputStreamReader), JsonObject.class);
			}
		}
		
	}
	

	protected void waitForReportsToBeReady(String executionId) {
		FluentWait<String> wait = new FluentWait<String>(executionId);
		wait.withTimeout(Duration.ofMinutes(2));
		wait.pollingEvery(Duration.ofSeconds(5));

		wait.until(new Function<String, Boolean>() {

			@Override
			public Boolean apply(String executionId) {

				JsonObject executionDetails;
				try {
					executionDetails = getTestExecutionDetails(executionId);
					
					String execStatus = executionDetails.get("metadata").getAsJsonObject().get("processingStatus")
							.getAsString().toUpperCase();
					
					int resourcesSize = executionDetails.getAsJsonArray("resources").size();

					return "PROCESSING_COMPLETE".equalsIgnoreCase(execStatus) && resourcesSize > 0;
				} catch (Exception e) {
					logger.error(e.getStackTrace());
					return true;
				}

			}
		});
	}
	
	protected void downloadFile(String fileName, URI uri, String suffix, String description) throws Exception {
		
		HttpGet httpGet = new HttpGet(uri);
		addDefaultRequestHeaders(httpGet);
		
		downloadFileToFileSystem(httpGet, fileName, suffix, description);
	}
	
	protected void downloadFileToFileSystem(HttpGet httpGet, String fileName, String suffix, String description)
			throws Exception {
		
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();

		addProxyDetailsIfRequired(clientBuilder);
		
		try(CloseableHttpClient httpClient = clientBuilder.build()){
			HttpResponse response = httpClient.execute(httpGet);
			FileOutputStream fileOutputStream = null;
			try {
				if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
					String dir = getReportDirectory();

					DateFormat dateFormat = new SimpleDateFormat("MMddyyyyHHmmss");
					Date date = new Date();
					File file = new File(dir, fileName + "_" + dateFormat.format(date) + suffix);
					fileOutputStream = new FileOutputStream(file);
					IOUtils.copy(response.getEntity().getContent(), fileOutputStream);
					logger.info("\nSaved " + description + " to: " + file.getAbsolutePath());
				} else {
					String errorMsg = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
					logger.error (
							"Error downloading file. Status: " + response.getStatusLine() + ".\nInfo: " + errorMsg);
				}
			}catch(Exception e) {
				e.printStackTrace();
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
				IOUtils.closeQuietly(fileOutputStream);
			}
		}
	}
	
	protected String getTestName(JsonObject resJObject) {
		
		String testName = resJObject.get("name").getAsString().replace(" " , "_");
		return testName.length()>100 ? testName.substring(0, 100):testName;
	}
	
	protected String getTestID(JsonObject resJObject) {
		return resJObject.get("id").getAsString();
	}
	
	protected JsonObject getPlatforms(JsonObject resJObject) {
		return resJObject.getAsJsonArray("platforms").get(0).getAsJsonObject();
	}
	
	protected String getDeviceName(JsonObject resJObject) {
		JsonObject platforms = getPlatforms(resJObject);
		return platforms.get("deviceId").getAsString();
	}
	
	protected JsonArray getResources(String executionID) throws Exception {
		JsonObject executions = getTestExecutionDetails(executionID);
		return executions.getAsJsonArray("resources");
	}
	
	protected String getDownloadedFileName(JsonObject resJObject) {
		String testId = getTestID(resJObject);
		String testName = getTestName(resJObject);
		
		String deviceName = getDeviceName(resJObject);
		
		return deviceName + "_" + testName + "_" + testId;
		
	}
	
	private synchronized String getReportDirectory() {
		String reportFolder = ConfigurationManager.getBundle().getString("perfecto.report.location","perfectoReports");
		
		File rptFldr = new File(reportFolder);
		
		if(!rptFldr.exists()) {
			rptFldr.mkdir();
		}
		
		return reportFolder;
	}

	@Override
	public void run() {}
	

}