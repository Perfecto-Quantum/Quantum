package com.quantum.utils;

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
import java.util.Collections;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.quantum.listerners.QuantumReportiumListener;

public class ReportUtils {

	public static void logStepStart(String message) {
		ConsoleUtils.logInfoBlocks(message, ConsoleUtils.lower_block + " ", 10);
		QuantumReportiumListener.logStepStart(message);
	}

	// The Perfecto Continuous Quality Lab you work with
	public static final String CQL_NAME = (ConfigurationManager.getBundle().getString("remote.server").split("\\.")[0])
			.replace("http://", "").replace("https://", "");

	// See
	// http://developers.perfectomobile.com/display/PD/Using+the+Reporting+Public+API
	// on how to obtain an Offline Token
	public static final String OFFLINE_TOKEN = ConfigurationManager.getBundle()
			.getString("perfecto.offlineToken");

	// The reporting Server address depends on the location of the lab. Please
	// refer to the documentation at
	// http://developers.perfectomobile.com/display/PD/Reporting#Reporting-ReportingserverAccessingthereports
	// to find your relevant address
	// For example the following is used for US:
	public static final String REPORTING_SERVER_URL = "https://" + CQL_NAME + ".reporting.perfectomobile.com";

	public static final String CQL_SERVER_URL = "https://" + CQL_NAME + ".perfectomobile.com";

	public static void generateSummaryReports(String executionId) throws Exception {
		// Use your personal offline token to obtain an access token
		String accessToken = obtainAccessToken();

		// Retrieve a list of the test executions in your lab (as a json)
		JsonObject executions = retrieveTestExecutions(accessToken, executionId);

		// for (int i = 0; i < executions.getAsJsonArray("resources").size(); i++) {
		JsonObject resources = executions.getAsJsonArray("resources").get(0).getAsJsonObject();
		JsonObject platforms = resources.getAsJsonArray("platforms").get(0).getAsJsonObject();
		String deviceId = platforms.get("deviceId").getAsString();
		downloadExecutionSummaryReport(deviceId, executionId, accessToken);
		// }

		// Retrieves a list of commands of a single test (as a json)
		// retrieveTestCommands(testId, accessToken);

		// Download an execution summary report of an execution (may contain
		// several tests)

		// Download a report of a single test
		// String testId = testExecution.get("name").getAsString();
		// downloadTestReport(testId, accessToken);

		// Download video
		// downloadVideo(testExecution);

		// Download attachments such as device logs, vitals or network files
		// (relevant for Mobile tests only)
		// downloadAttachments(testExecution);
	}

	public static void generateTestReport(String executionId) throws Exception {
		String accessToken = obtainAccessToken();
		JsonObject executions = retrieveTestExecutions(accessToken, executionId);

		for (int i = 0; i < executions.getAsJsonArray("resources").size(); i++) {
			JsonObject testExecution = executions.getAsJsonArray("resources").get(i).getAsJsonObject();
			String testId = testExecution.get("id").getAsString();
			String testName = testExecution.get("name").getAsString();
			JsonObject platforms = testExecution.getAsJsonArray("platforms").get(0).getAsJsonObject();
			String deviceName = platforms.get("deviceId").getAsString();
			downloadTestReport(testId, deviceName + "_" + testName.replace(" ", "_"), accessToken);
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
			throws URISyntaxException, IOException {
		URIBuilder uriBuilder = new URIBuilder(REPORTING_SERVER_URL + "/export/api/v1/test-executions/pdf");
		uriBuilder.addParameter("externalId[0]", driverExecutionId);
		// downloadFileAuthenticated(driverExecutionId, uriBuilder.build(),
		// ".pdf", "execution summary PDF report",
		// accessToken);
		downloadFileAuthenticated(deviceId + "_ExecutionSummaryReport", uriBuilder.build(), ".pdf",
				"execution summary PDF report", accessToken);

	}

	private static void downloadTestReport(String testId, String fileName, String accessToken)
			throws URISyntaxException, IOException {
		URIBuilder uriBuilder = new URIBuilder(REPORTING_SERVER_URL + "/export/api/v1/test-executions/pdf/" + testId);
		downloadFileAuthenticated(fileName, uriBuilder.build(), ".pdf", "test PDF report", accessToken);

	}

	@SuppressWarnings("unused")
	private static void downloadVideo(JsonObject testExecution) throws IOException, URISyntaxException {
		JsonArray videos = testExecution.getAsJsonArray("videos");
		if (videos.size() > 0) {
			JsonObject video = videos.get(0).getAsJsonObject();
			String downloadVideoUrl = video.get("downloadUrl").getAsString();
			String format = "." + video.get("format").getAsString();
			String testId = testExecution.get("id").getAsString();
			// downloadFile(testId, URI.create(downloadVideoUrl), format,
			// "video");
			downloadFile("results_video", URI.create(downloadVideoUrl), format, "video");
		} else {
			System.out.println("\nNo videos found for test execution");
		}
	}

	@SuppressWarnings("unused")
	private static void downloadAttachments(JsonObject testExecution) throws IOException, URISyntaxException {
		// Example for downloading device logs
		JsonArray artifacts = testExecution.getAsJsonArray("artifacts");
		for (JsonElement artifactElement : artifacts) {
			JsonObject artifact = artifactElement.getAsJsonObject();
			String artifactType = artifact.get("type").getAsString();
			if (artifactType.equals("DEVICE_LOGS")) {
				String testId = testExecution.get("id").getAsString();
				String path = artifact.get("path").getAsString();
				URIBuilder uriBuilder = new URIBuilder(path);
				downloadFile(testId, uriBuilder.build(), ".zip", "device logs");
			}
		}
	}

	private static String obtainAccessToken() throws URISyntaxException, IOException {
		HttpPost tokenPost = new HttpPost(new URI(CQL_SERVER_URL + "/services/v2.0/auth/access-token"));
		tokenPost.setEntity(new UrlEncodedFormEntity(
				Collections.singletonList(new BasicNameValuePair("offline_token", OFFLINE_TOKEN))));

		HttpResponse tokenResponse = null;

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
			tokenResponse = httpClient.execute(tokenPost);
		} else {
			HttpClient httpClient = HttpClientBuilder.create().build();
			tokenResponse = httpClient.execute(tokenPost);
		}
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String accessToken;
		try (InputStreamReader inputStreamReader = new InputStreamReader(tokenResponse.getEntity().getContent())) {
			JsonObject tokenEntity = gson.fromJson(inputStreamReader, JsonObject.class);
			accessToken = tokenEntity.getAsJsonObject("data").get("access_token").getAsString();
			// System.out.println("Got access token:\n" + accessToken);
		}
		return accessToken;
	}

	private static void downloadFile(String fileName, URI uri, String suffix, String description) throws IOException {
		downloadFileToFS(new HttpGet(uri), fileName, suffix, description);
	}

	private static void downloadFileAuthenticated(String fileName, URI uri, String suffix, String description,
												  String accessToken) throws IOException {
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

	private static void downloadFileToFS(HttpGet httpGet, String fileName, String suffix, String description)
			throws IOException {

		HttpResponse response = null;

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
		request.addHeader("Authorization", "Bearer " + accessToken);
	}

	public static void main(String[] args) throws URISyntaxException, IOException {
		System.out.println(obtainAccessToken());
	}
}
