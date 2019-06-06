package com.quantum.utils;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.perfecto.reportium.client.ReportiumClient;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.quantum.listeners.QuantumReportiumListener;

public class ReportUtils {

	public static final String PERFECTO_REPORT_CLIENT = "perfecto.report.client";
	private static final int PDF_DOWNLOAD_ATTEMPTS = 12;
	private static final int TIMEOUT_MILLIS = 60000;

	public static ReportiumClient getReportClient() {
		return (ReportiumClient) ConfigurationManager.getBundle().getObject(PERFECTO_REPORT_CLIENT);
	}

	public static void logStepStart(String message) {
		ConsoleUtils.logInfoBlocks(message, ConsoleUtils.lower_block + " ", 10);
		QuantumReportiumListener.logStepStart(message);
	}

	// The Perfecto Continuous Quality Lab you work with
	public static final String CQL_NAME = (ConfigurationManager.getBundle().getString("remote.server").split("\\.")[0])
			.replace("http://", "").replace("https://", "");

	@SuppressWarnings("unchecked")
	private static String getToken() throws Exception {
		String accessToken = "";
		for (Iterator<String> i = getBundle().getKeys(); i.hasNext();) {
			String key = i.next();
			if (key.contains("securityToken")) {
				accessToken = getBundle().getString(key);
			}
		}
		if (accessToken.isEmpty()) {
			if (getBundle().containsKey("perfecto.offlineToken")) {
				accessToken = getBundle().getString("perfecto.offlineToken").trim();
			} else {
				throw new Exception(
						"ERROR!!! Download PDF ERROR!!! Please configure the security token in the driver capabilities of the key 'perfecto.offlineToken'");
			}

		}
		return accessToken;
	}

	// The reporting Server address depends on the location of the lab. Please
	// refer to the documentation at
	// http://developers.perfectomobile.com/display/PD/Reporting#Reporting-ReportingserverAccessingthereports
	// to find your relevant address
	// For example the following is used for US:
	public static final String REPORTING_SERVER_URL = "https://" + CQL_NAME + ".reporting.perfectomobile.com";

	public static final String CQL_SERVER_URL = "https://" + CQL_NAME + ".perfectomobile.com";

	public static void generateSummaryReports(String executionId) throws Exception {
		// Use your personal offline token to obtain an access token

		// Retrieve a list of the test executions in your lab (as a json)
		JsonObject executions = retrieveTestExecutions(getToken(), executionId);
		int counter = 0;
		while ((!executions.get("metadata").getAsJsonObject().get("processingStatus").getAsString()
				.equalsIgnoreCase("PROCESSING_COMPLETE") || executions.getAsJsonArray("resources").size() == 0)
				&& counter < 60) {
			Thread.sleep(1000);
			executions = retrieveTestExecutions(getToken(), executionId);
			counter++;
		}
		JsonObject resources = executions.getAsJsonArray("resources").get(0).getAsJsonObject();
		JsonObject platforms = resources.getAsJsonArray("platforms").get(0).getAsJsonObject();
		String deviceId = platforms.get("deviceId").getAsString();
		downloadExecutionSummaryReport(deviceId, executionId, getToken());

	}

	public static void generateTestReport(String executionId) throws Exception {

		JsonObject executions = retrieveTestExecutions(getToken(), executionId);

		int counter = 0;
		while ((!executions.get("metadata").getAsJsonObject().get("processingStatus").getAsString()
				.equalsIgnoreCase("PROCESSING_COMPLETE") || executions.getAsJsonArray("resources").size() == 0)
				&& counter < 60) {
			Thread.sleep(1000);
			executions = retrieveTestExecutions(getToken(), executionId);
			counter++;
		}

		for (int i = 0; i < executions.getAsJsonArray("resources").size(); i++) {
			JsonObject testExecution = executions.getAsJsonArray("resources").get(i).getAsJsonObject();
			String testId = testExecution.get("id").getAsString();
			String testName = testExecution.get("name").getAsString().replace(" ", "_");
			JsonObject platforms = testExecution.getAsJsonArray("platforms").get(0).getAsJsonObject();
			String deviceName = platforms.get("deviceId").getAsString();
			downloadTestReport(testId,
					deviceName + "_" + (testName.length() >= 100 ? testName.substring(1, 100) : testName), getToken());
		}

	}

	public static void downloadReportVideo(String executionId) throws Exception {
		JsonObject executions = retrieveTestExecutions(getToken(), executionId);
		int counter = 0;
		while ((!executions.get("metadata").getAsJsonObject().get("processingStatus").getAsString()
				.equalsIgnoreCase("PROCESSING_COMPLETE") || executions.getAsJsonArray("resources").size() == 0)
				&& counter < 60) {
			Thread.sleep(1000);
			executions = retrieveTestExecutions(getToken(), executionId);
			counter++;
		}

		for (int i = 0; i < executions.getAsJsonArray("resources").size(); i++) {
			JsonObject testExecution = executions.getAsJsonArray("resources").get(i).getAsJsonObject();
			// String testId = testExecution.get("id").getAsString();
			String testName = testExecution.get("name").getAsString().replace(" ", "_");
			JsonObject platforms = testExecution.getAsJsonArray("platforms").get(0).getAsJsonObject();
			String deviceName = platforms.get("deviceId").getAsString();
			downloadVideo(deviceName + "_" + (testName.length() >= 100 ? testName.substring(1, 100) : testName),
					executions);
		}

	}

	public static void downloadReportAttachments(String executionId) throws Exception {
		JsonObject executions = retrieveTestExecutions(getToken(), executionId);
		int counter = 0;
		while ((!executions.get("metadata").getAsJsonObject().get("processingStatus").getAsString()
				.equalsIgnoreCase("PROCESSING_COMPLETE") || executions.getAsJsonArray("resources").size() == 0)
				&& counter < 60) {
			Thread.sleep(1000);
			executions = retrieveTestExecutions(getToken(), executionId);
			counter++;
		}

		for (int i = 0; i < executions.getAsJsonArray("resources").size(); i++) {
			JsonObject testExecution = executions.getAsJsonArray("resources").get(i).getAsJsonObject();
			// String testId = testExecution.get("id").getAsString();
			String testName = testExecution.get("name").getAsString().replace(" ", "_");
			JsonObject platforms = testExecution.getAsJsonArray("platforms").get(0).getAsJsonObject();
			String deviceName = platforms.get("deviceId").getAsString();
			downloadAttachments(deviceName + "_" + (testName.length() >= 100 ? testName.substring(1, 100) : testName),
					executions);
		}

	}

	private static JsonObject retrieveTestExecutions(String accessToken, String executionId)
			throws URISyntaxException, IOException {
		URIBuilder uriBuilder = new URIBuilder(REPORTING_SERVER_URL + "/export/api/v1/test-executions");

		// Optional: Filter by range. In this example: retrieve test executions
		// of the past month (result may contain tests of multiple driver
		// executions)

		// Optional: Filter by a specific driver execution ID that you can
		// obtain at script execution
		uriBuilder.addParameter("externalId[0]", executionId);

		HttpGet getExecutions = new HttpGet(uriBuilder.build());
		addDefaultRequestHeaders(getExecutions, accessToken);

		HttpResponse getExecutionsResponse = null;

		if (ConfigurationManager.getBundle().getString("proxyHost") != null
				&& !ConfigurationManager.getBundle().getString("proxyHost").toString().equals("")) {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			NTCredentials ntCreds = new NTCredentials(
					ConfigurationManager.getBundle().getString("proxyUser").toString(),
					ConfigurationManager.getBundle().getString("proxyPassword").toString(), hostname,
					ConfigurationManager.getBundle().getString("proxyDomain").toString());

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider
					.setCredentials(
							new AuthScope(ConfigurationManager.getBundle().getString("proxyHost").toString(),
									Integer.parseInt(
											ConfigurationManager.getBundle().getString("proxyPort").toString())),
							ntCreds);
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();

			clientBuilder.useSystemProperties();
			clientBuilder.setProxy(new HttpHost(ConfigurationManager.getBundle().getString("proxyHost").toString(),
					Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort").toString())));
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
			clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

			CloseableHttpClient httpClient = clientBuilder.build();
			getExecutionsResponse = httpClient.execute(getExecutions);
		} else {
			HttpClient httpClient = HttpClientBuilder.create().build();
			getExecutionsResponse = httpClient.execute(getExecutions);
		}

		JsonObject executions;
		try (InputStreamReader inputStreamReader = new InputStreamReader(
				getExecutionsResponse.getEntity().getContent())) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			executions = gson.fromJson(IOUtils.toString(inputStreamReader), JsonObject.class);
			// System.out.println("\nList of test executions response:\n" +
			// gson.toJson(executions));
		}
		return executions;
	}

	@SuppressWarnings("unused")
	private static String retrieveTestCommands(String testId, String accessToken)
			throws URISyntaxException, IOException {
		HttpGet getCommands = new HttpGet(
				new URI(REPORTING_SERVER_URL + "/export/api/v1/test-executions/" + testId + "/commands"));
		addDefaultRequestHeaders(getCommands, accessToken);

		HttpResponse getCommandsResponse = null;

		if (ConfigurationManager.getBundle().getString("proxyHost") != null
				&& !ConfigurationManager.getBundle().getString("proxyHost").toString().equals("")) {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			NTCredentials ntCreds = new NTCredentials(
					ConfigurationManager.getBundle().getString("proxyUser").toString(),
					ConfigurationManager.getBundle().getString("proxyPassword").toString(), hostname,
					ConfigurationManager.getBundle().getString("proxyDomain").toString());

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider
					.setCredentials(
							new AuthScope(ConfigurationManager.getBundle().getString("proxyHost").toString(),
									Integer.parseInt(
											ConfigurationManager.getBundle().getString("proxyPort").toString())),
							ntCreds);
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();

			clientBuilder.useSystemProperties();
			clientBuilder.setProxy(new HttpHost(ConfigurationManager.getBundle().getString("proxyHost").toString(),
					Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort").toString())));
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
			clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

			CloseableHttpClient httpClient = clientBuilder.build();
			getCommandsResponse = httpClient.execute(getCommands);
		} else {
			HttpClient httpClient = HttpClientBuilder.create().build();
			getCommandsResponse = httpClient.execute(getCommands);
		}
		try (InputStreamReader inputStreamReader = new InputStreamReader(
				getCommandsResponse.getEntity().getContent())) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			JsonObject commands = gson.fromJson(IOUtils.toString(inputStreamReader), JsonObject.class);
			System.out.println("\nList of commands response:\n" + gson.toJson(commands));
			return gson.toJson(commands);
		}
	}

	private static void downloadExecutionSummaryReport(String deviceId, String driverExecutionId, String accessToken)
			throws Exception {
		URIBuilder uriBuilder = new URIBuilder(REPORTING_SERVER_URL + "/export/api/v1/test-executions/pdf");
		uriBuilder.addParameter("externalId[0]", driverExecutionId);
		// downloadFileAuthenticated(driverExecutionId, uriBuilder.build(),
		// ".pdf", "execution summary PDF report",
		// accessToken);
		downloadFileAuthenticated(deviceId + "_ExecutionSummaryReport", uriBuilder.build(), ".pdf",
				"execution summary PDF report", accessToken);

	}

	/**
	 * This method will download the single test report in PDF format. THe testId
	 * will be incoming as a parameter and is handled in a different method. Updated
	 * this method to go with the v2 download APIs.
	 * 
	 * @param testId
	 *            - Test id of the single test report.
	 * @param fileName
	 *            - Name of the pdf file that will be downloaded.
	 * @param accessToken
	 *            - Security token of the user.
	 * @throws Exception
	 */
	private static void downloadTestReport(String testId, String fileName, String accessToken) throws Exception {
		System.out.println("Starting PDF generation for test ID: " + testId);

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		URIBuilder taskUriBuilder = new URIBuilder(REPORTING_SERVER_URL + "/export/api/v2/test-executions/pdf/task");
		taskUriBuilder.addParameter("testExecutionId", testId);
		HttpPost httpPost = new HttpPost(taskUriBuilder.build());
		addDefaultRequestHeaders(httpPost, accessToken);

		CreatePdfTask task = null;
		for (int attempt = 1; attempt <= PDF_DOWNLOAD_ATTEMPTS; attempt++) {

			// HttpResponse response = httpClient.execute(httpPost);
			HttpResponse response = null;

			if (ConfigurationManager.getBundle().getString("proxyHost") != null
					&& !ConfigurationManager.getBundle().getString("proxyHost").equals("")) {

				if (ConfigurationManager.getBundle().getString("proxyPort") == null) {
					throw new Exception("Please mention the NTLM port in the application properties file");
				}
				InetAddress addr;
				addr = InetAddress.getLocalHost();
				String hostname = addr.getHostName();

				NTCredentials ntCreds = new NTCredentials(ConfigurationManager.getBundle().getString("proxyUser", ""),
						ConfigurationManager.getBundle().getString("proxyPassword", ""), hostname,
						ConfigurationManager.getBundle().getString("proxyDomain", ""));

				CredentialsProvider credsProvider = new BasicCredentialsProvider();
				credsProvider.setCredentials(new AuthScope(ConfigurationManager.getBundle().getString("proxyHost"),
						Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort"))), ntCreds);
				HttpClientBuilder clientBuilder = HttpClientBuilder.create();

				clientBuilder.useSystemProperties();
				clientBuilder.setProxy(new HttpHost(ConfigurationManager.getBundle().getString("proxyHost"),
						Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort"))));
				clientBuilder.setDefaultCredentialsProvider(credsProvider);
				clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

				CloseableHttpClient httpClient = clientBuilder.build();
				response = httpClient.execute(httpPost);
			} else {
				HttpClient httpClient = HttpClientBuilder.create()
						.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
						.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
								.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build())
						.build();
				response = httpClient.execute(httpPost);
			}

			try {
				int statusCode = response.getStatusLine().getStatusCode();
				if (HttpStatus.SC_OK == statusCode) {
					task = gson.fromJson(EntityUtils.toString(response.getEntity()), CreatePdfTask.class);
					break;
				} else if (HttpStatus.SC_NO_CONTENT == statusCode) {

					// if the execution is being processed, the server will respond with empty
					// response and status code 204
					System.out.println("The server responded with 204 (no content). "
							+ "The execution is still being processed. Attempting again in 5 sec (" + attempt + "/"
							+ PDF_DOWNLOAD_ATTEMPTS + ")");
					Thread.sleep(5000);
				} else {
					String errorMsg = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
					System.err.println(
							"Error downloading file. Status: " + response.getStatusLine() + ".\nInfo: " + errorMsg);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				EntityUtils.consumeQuietly(response.getEntity());
			}
		}
		if (task == null) {
			throw new RuntimeException("Unable to create a CreatePdfTask");
		}

		downloadTestReport(fileName, task, accessToken);

	}

	private static void downloadTestReport(String testPdfPath, CreatePdfTask task, String accessToken)
			throws Exception {
		long startTime = System.currentTimeMillis();
		int maxWaitMin = 10;
		long maxGenerationTime = TimeUnit.MINUTES.toMillis(maxWaitMin);
		String taskId = task.getTaskId();

		CreatePdfTask updatedTask;
		do {
			updatedTask = getUpdatedTask(taskId, accessToken);
			try {
				if (updatedTask.getStatus() != TaskStatus.COMPLETE) {
					Thread.sleep(3000);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		} while (updatedTask.getStatus() != TaskStatus.COMPLETE
				&& startTime + maxGenerationTime > System.currentTimeMillis());

		if (updatedTask.getStatus() == TaskStatus.COMPLETE) {
			URIBuilder uriBuilder = new URIBuilder(updatedTask.getUrl());
			downloadFileAuthenticated(testPdfPath, uriBuilder.build(), ".pdf", "test PDF report", accessToken);
		} else {
			throw new RuntimeException(
					"The task is still in " + updatedTask.getStatus() + " status after waiting " + maxWaitMin + " min");
		}
	}

	private static CreatePdfTask getUpdatedTask(String taskId, String accessToken) throws Exception {
		CreatePdfTask task;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		URIBuilder taskUriBuilder = new URIBuilder(
				REPORTING_SERVER_URL + "/export/api/v2/test-executions/pdf/task/" + taskId);
		HttpGet httpGet = new HttpGet(taskUriBuilder.build());
		addDefaultRequestHeaders(httpGet, accessToken);
		HttpResponse response = null;
		if (ConfigurationManager.getBundle().getString("proxyHost") != null
				&& !ConfigurationManager.getBundle().getString("proxyHost").toString().equals("")) {
			if (ConfigurationManager.getBundle().getString("proxyPort") == null) {
				throw new Exception("Please mention the NTLM port in the application properties file");
			}
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			NTCredentials ntCreds = new NTCredentials(ConfigurationManager.getBundle().getString("proxyUser", ""),
					ConfigurationManager.getBundle().getString("proxyPassword", ""), hostname,
					ConfigurationManager.getBundle().getString("proxyDomain", ""));

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider
					.setCredentials(
							new AuthScope(ConfigurationManager.getBundle().getString("proxyHost"),
									Integer.parseInt(
											ConfigurationManager.getBundle().getString("proxyPort"))),
							ntCreds);
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();

			clientBuilder.useSystemProperties();
			clientBuilder.setProxy(new HttpHost(ConfigurationManager.getBundle().getString("proxyHost"),
					Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort"))));
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
			clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

			CloseableHttpClient httpClient = clientBuilder.build();
			response = httpClient.execute(httpGet);
		} else {
			HttpClient httpClient = HttpClientBuilder.create()
					.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
					.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
							.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build())
					.build();
			response = httpClient.execute(httpGet);
		}

		int statusCode = response.getStatusLine().getStatusCode();
		if (HttpStatus.SC_OK == statusCode) {
			task = gson.fromJson(EntityUtils.toString(response.getEntity()), CreatePdfTask.class);
		} else {
			throw new RuntimeException("Error while getting AsyncTask: " + response.getStatusLine().toString());
		}
		return task;
	}

	@SuppressWarnings("unused")
	private static void downloadVideo(String deviceId, JsonObject testExecution)
			throws Exception {
		JsonObject resources = testExecution.getAsJsonArray("resources").get(0).getAsJsonObject();
		JsonArray videos = resources.getAsJsonArray("videos");

		if (videos.size() > 0) {
			JsonObject video = videos.get(0).getAsJsonObject();
			String downloadVideoUrl = video.get("downloadUrl").getAsString();
			String format = "." + video.get("format").getAsString();
			String testId = resources.get("id").getAsString();
			// downloadFile(testId, URI.create(downloadVideoUrl), format,
			// "video");
			downloadFile(deviceId + "_Video", URI.create(downloadVideoUrl), format, "video");
		} else {
			System.out.println("\nNo videos found for test execution");
		}
	}

	private static void downloadAttachments(String deviceId, JsonObject testExecution)
			throws Exception {
		// Example for downloading device logs

		JsonObject resources = testExecution.getAsJsonArray("resources").get(0).getAsJsonObject();
		JsonArray artifacts = resources.getAsJsonArray("artifacts");
		for (JsonElement artifactElement : artifacts) {
			JsonObject artifact = artifactElement.getAsJsonObject();
			String artifactType = artifact.get("type").getAsString();
			if (artifactType.equals("DEVICE_LOGS")) {
				String testId = resources.get("id").getAsString();
				String path = artifact.get("path").getAsString();
				URIBuilder uriBuilder = new URIBuilder(path);
				downloadFile(deviceId + "_" + testId, uriBuilder.build(), "_device_logs.zip", "device logs");
			} else if (artifactType.equals("NETWORK")) {
				String testId = resources.get("id").getAsString();
				String path = artifact.get("path").getAsString();
				URIBuilder uriBuilder = new URIBuilder(path);
				downloadFile(deviceId + "_" + testId, uriBuilder.build(), "_network_logs.zip", "network logs");
			} else if (artifactType.equals("VITALS")) {
				String testId = resources.get("id").getAsString();
				String path = artifact.get("path").getAsString();
				URIBuilder uriBuilder = new URIBuilder(path);
				downloadFile(deviceId + "_" + testId, uriBuilder.build(), "_vitals_logs.zip", "vitals logs");
			}
		}
	}

	private static void downloadFile(String fileName, URI uri, String suffix, String description) throws Exception {
		downloadFileToFS(new HttpGet(uri), fileName, suffix, description);
	}

	private static void downloadFileAuthenticated(String fileName, URI uri, String suffix, String description,
			String accessToken) throws Exception {
		HttpGet httpGet = new HttpGet(uri);
		addDefaultRequestHeaders(httpGet, accessToken);
		downloadFileToFS(httpGet, fileName, suffix, description);
	}

	private static String getReportDirectory() {
		try {
			ConfigurationManager.getBundle().getString("perfecto.report.location");
			if (!ConfigurationManager.getBundle().getString("perfecto.report.location").equals("")) {
				return ConfigurationManager.getBundle().getString("perfecto.report.location");
			} else {
				return "perfectoReports";
			}
		} catch (Exception ex) {
			return "perfectoReports";
		}
	}

	@SuppressWarnings("deprecation")
	private static void downloadFileToFS(HttpGet httpGet, String fileName, String suffix, String description)
			throws Exception {

		HttpResponse response = null;

		if (ConfigurationManager.getBundle().getString("proxyHost") != null
				&& !ConfigurationManager.getBundle().getString("proxyHost").toString().equals("")) {
			if(ConfigurationManager.getBundle().getString("proxyPort") == null) {
				throw new Exception("Please mention the NTLM port in the application properties file");
			}
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();

			NTCredentials ntCreds = new NTCredentials(
					ConfigurationManager.getBundle().getString("proxyUser",""),
					ConfigurationManager.getBundle().getString("proxyPassword",""), hostname,
					ConfigurationManager.getBundle().getString("proxyDomain",""));

			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider
					.setCredentials(
							new AuthScope(ConfigurationManager.getBundle().getString("proxyHost"),
									Integer.parseInt(
											ConfigurationManager.getBundle().getString("proxyPort"))),
							ntCreds);
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();

			clientBuilder.useSystemProperties();
			clientBuilder.setProxy(new HttpHost(ConfigurationManager.getBundle().getString("proxyHost"),
					Integer.parseInt(ConfigurationManager.getBundle().getString("proxyPort"))));
			clientBuilder.setDefaultCredentialsProvider(credsProvider);
			clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());

			CloseableHttpClient httpClient = clientBuilder.build();
			response = httpClient.execute(httpGet);
		} else {
			HttpClient httpClient = HttpClientBuilder.create().build();
			response = httpClient.execute(httpGet);
		}

		FileOutputStream fileOutputStream = null;
		try {
			if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
				String dir = getReportDirectory();

				DateFormat dateFormat = new SimpleDateFormat("MMddyyyyHHmmss");
				Date date = new Date();
				if (!new File(dir).exists()) {
					new File(dir).mkdir();
				}
				File file = new File(dir, fileName + "_" + dateFormat.format(date) + suffix);
				fileOutputStream = new FileOutputStream(file);
				IOUtils.copy(response.getEntity().getContent(), fileOutputStream);
				System.out.println("\nSaved " + description + " to: " + file.getAbsolutePath());
			} else {
				String errorMsg = IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
				System.err.println(
						"Error downloading file. Status: " + response.getStatusLine() + ".\nInfo: " + errorMsg);
			}
		} finally {
			EntityUtils.consumeQuietly(response.getEntity());
			IOUtils.closeQuietly(fileOutputStream);
		}
	}

	private static void addDefaultRequestHeaders(HttpRequestBase request, String accessToken) {
		request.addHeader("PERFECTO_AUTHORIZATION", accessToken);
	}

	public static void reportComment(String message) {
		Map<String, Object> params = new HashMap<>();
		params.put("text", message);
		DeviceUtils.getQAFDriver().executeScript("mobile:comment", params);
	}

	//
	/**
	 * Using this method will continue the scenario execution on failure, but it
	 * will mark the scenario as failed at the end and also display all the failure
	 * messages
	 * 
	 * @param message
	 *            - Assertion message to be displayed in the DZ
	 * @param status
	 *            - Assertion flag status - true or false (pass or fail)
	 */
	public static void logVerify(String message, boolean status) {
		try {
			if (!status) {
				TestBaseProvider.instance().get().addVerificationError(message);
			}
			getReportClient().reportiumAssert(message, status);
		} catch (Exception e) {
			// ignore...
		}
	}

	/**
	 * Added this method to report verifications with throwable exception.
	 * 
	 * @param message
	 *            - Assertion message to be displayed in the DZ
	 * @param status
	 *            - Assertion flag status - true or false (pass or fail)
	 * @param e
	 *            - If the exception will be passed then the stacktrace will be
	 *            attached on failure flag in DZ
	 */
	public static void logVerify(String message, boolean status, Throwable e) {
		try {
			if (!status) {
				TestBaseProvider.instance().get().addVerificationError(e);
				getReportClient().reportiumAssert(message + "\n" + ExceptionUtils.getFullStackTrace(e), status);
			} else {
				getReportClient().reportiumAssert(message, status);
			}
		} catch (Exception ex) {
			// ignore...
		}
	}

	/**
	 * Using this method will add an assertion and stop the execution of the
	 * scenario in case of failure.
	 * 
	 * @param message
	 *            - Assertion message to be displayed in the DZ
	 * @param status
	 *            - Assertion flag status - true or false (pass or fail)
	 */
	public static void logAssert(String message, boolean status) {
		logVerify(message, status);
		if (!status) {
			throw new AssertionError(message);
		}
	}

	/**
	 * Added this method to report assertions with throwable exception.
	 * 
	 * @param message
	 *            - Assertion message to be displayed in the DZ
	 * @param status
	 *            - Assertion flag status - true or false (pass or fail)
	 * @param e-
	 *            If the exception will be passed then the stacktrace will be
	 *            attached on failure flag in DZ
	 */
	public static void logAssert(String message, boolean status, Throwable e) {
		logVerify(message, status, e);
		if (!status) {
			throw new AssertionError(message);
		}
	}

	private enum TaskStatus {
		IN_PROGRESS, COMPLETE
	}

	private static class CreatePdfTask {
		private String taskId;
		private TaskStatus status;
		private String url;

		@SuppressWarnings("unused")
		public CreatePdfTask() {
		}

		public String getTaskId() {
			return taskId;
		}

		@SuppressWarnings("unused")
		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}

		public TaskStatus getStatus() {
			return status;
		}

		@SuppressWarnings("unused")
		public void setStatus(TaskStatus status) {
			this.status = status;
		}

		public String getUrl() {
			return url;
		}

		@SuppressWarnings("unused")
		public void setUrl(String url) {
			this.url = url;
		}
	}
}
