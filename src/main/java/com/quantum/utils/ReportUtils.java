package com.quantum.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.testng.Assert;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.perfecto.reportium.client.ReportiumClient;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.core.TestBaseProvider;
import com.quantum.listeners.QuantumReportiumListener;

public class ReportUtils {

	public static final String PERFECTO_REPORT_CLIENT = "perfecto.report.client";

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

	// The reporting Server address depends on the location of the lab. Please
	// refer to the documentation at
	// http://developers.perfectomobile.com/display/PD/Reporting#Reporting-ReportingserverAccessingthereports
	// to find your relevant address
	// For example the following is used for US:
	public static final String REPORTING_SERVER_URL = "https://" + CQL_NAME + ".reporting.perfectomobile.com";

	public static final String CQL_SERVER_URL = "https://" + CQL_NAME + ".perfectomobile.com";

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
			try {
				NTCredentials ntCreds = new NTCredentials(ConfigurationManager.getBundle().getString("proxyUser", ""),
						ConfigurationManager.getBundle().getString("proxyPassword", ""), hostname,
						ConfigurationManager.getBundle().getString("proxyDomain", ""));

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
			} catch (Exception e) {
				System.out.println(
						"ERROR -----------> proxyPort key was not mentioned along with the proxyHost configuration for report downloading");
			}

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
	 * @param message - Assertion message to be displayed in the DZ
	 * @param status  - Assertion flag status - true or false (pass or fail)
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
	 * @param message - Assertion message to be displayed in the DZ
	 * @param status  - Assertion flag status - true or false (pass or fail)
	 * @param e       - If the exception will be passed then the stacktrace will be
	 *                attached on failure flag in DZ
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
	 * @param message - Assertion message to be displayed in the DZ
	 * @param status  - Assertion flag status - true or false (pass or fail)
	 */
	public static void logAssert(String message, boolean status) {
		logVerify(message, status);
		if (!status) {
			
			Assert.fail(message);
//			throw new AssertionError(message);
		}
	}

	/**
	 * Added this method to report assertions with throwable exception.
	 * 
	 * @param message - Assertion message to be displayed in the DZ
	 * @param status  - Assertion flag status - true or false (pass or fail)
	 * @param e-      If the exception will be passed then the stacktrace will be
	 *                attached on failure flag in DZ
	 */
	public static void logAssert(String message, boolean status, Throwable e) {
		logVerify(message, status, e);
		if (!status) {
//			throw new AssertionError(message);
			Assert.fail(message, e);
		}
	}
	
}
