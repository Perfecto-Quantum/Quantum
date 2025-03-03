package com.perfecto.reports;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

import javax.annotation.CheckForNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.support.ui.FluentWait;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;

public class ReportPDF extends RESTClient {
	
	private String executionID;
	
	public ReportPDF(String executionID) throws Exception{
		this.executionID = executionID;
	}

	private static final Log logger = LogFactoryImpl.getLog(ReportPDF.class);

	public void download() throws Exception {
		
		if(!ConfigurationManager.getBundle().getBoolean("perfecto.download.reports", false)) return ;

		logger.info("Download PDF started for execution ID: " + executionID);

		waitForReportsToBeReady(executionID);

		JsonArray resourceDetails = getResources(executionID);
		JsonObject resJObject;
		String fileName;
		String testID;

		for (JsonElement resDetail : resourceDetails) {
			resJObject = resDetail.getAsJsonObject();

			fileName = getDownloadedFileName(resJObject);
			testID = getTestID(resJObject);

			logger.info("Initiate PDF started for execution ID: " + executionID);
			ReportTaskEntity reportEntity = initiatePDFGeneration(testID, fileName);

			if (reportEntity == null) {
				logger.error("Not able to download the PDF");
			} else {
				logger.info("Waiting for PDF generation for execution ID: " + executionID);
				ReportTaskEntity pdfTask = waitForPDFRenerationTask(reportEntity);
				
				logger.info("Downloading of generated PDF started for execution ID: " + executionID);
				downloadPDF(fileName,pdfTask);
			}
		}
	}

	private @CheckForNull ReportTaskEntity initiatePDFGeneration(String testId, String fileName) throws Exception {

		URIBuilder taskUriBuilder = new URIBuilder(getReportBaseURL() + "/export/api/v2/test-executions/pdf/task");
		taskUriBuilder.addParameter("testExecutionId", testId);

		HttpClientBuilder clientBuilder = HttpClientBuilder.create()
				.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
				.setDefaultRequestConfig(RequestConfig.custom()
						.setSocketTimeout(TIMEOUT_MILLIS)
						.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build());

		addProxyDetailsIfRequired(clientBuilder);

		HttpPost httpPost = new HttpPost(taskUriBuilder.build());
		
		addDefaultRequestHeaders(httpPost);

		try (CloseableHttpClient httpClient = clientBuilder.build()) {
			HttpResponse response = httpClient.execute(httpPost);

			int statusCode = response.getStatusLine().getStatusCode();

			logger.info("Download PDF response code: " + statusCode);

			if (statusCode >= 200 && statusCode < 300) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				return gson.fromJson(EntityUtils.toString(response.getEntity()), ReportTaskEntity.class);
			} else {
				System.out.println("PDF Generation status code : " + statusCode);
				return null;
			}
		}

	}

	private @CheckForNull ReportTaskEntity waitForPDFRenerationTask(ReportTaskEntity entity) {

		FluentWait<String> wait = new FluentWait<String>(entity.getTaskId());
		wait.withTimeout(Duration.ofMinutes(2));
		wait.pollingEvery(Duration.ofSeconds(5));

		return wait.until(new Function<String, ReportTaskEntity>() {

			@Override
			public ReportTaskEntity apply(String taskID) {
				
				try {
					ReportTaskEntity task = getReportTaskStatus(taskID);
					
					if(task==null) {
						logger.error("Not able to get Report Task status");
						return null;
					}else {
						if(ReportTaskEntity.TaskStatus.COMPLETE != task.getStatus()) {
							return null;
						}else {
							return task;
						}
					}
				} catch (Exception e) {
					logger.error("Not able to get Report Task status : " + e.getMessage());
					return null;
				}

				
			}
		});
	}

	private ReportTaskEntity getReportTaskStatus(String taskId) throws Exception {

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		URIBuilder taskUriBuilder = new URIBuilder(
				getReportBaseURL() + "/export/api/v2/test-executions/pdf/task/" + taskId);

		HttpGet httpGet = new HttpGet(taskUriBuilder.build());
		addDefaultRequestHeaders(httpGet);

		HttpClientBuilder clientBuilder = HttpClientBuilder.create()
				.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
				.setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
						.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build());

		addProxyDetailsIfRequired(clientBuilder);

		try (CloseableHttpClient httpClient = clientBuilder.build()) {
			HttpResponse response = httpClient.execute(httpGet);

			int statusCode = response.getStatusLine().getStatusCode();

			if (HttpStatus.SC_OK == statusCode) {
				return gson.fromJson(EntityUtils.toString(response.getEntity()), ReportTaskEntity.class);
			} else {
				throw new RuntimeException("Error while getting AsyncTask: " + response.getStatusLine().toString());
			}
		}

	}
	
	private void downloadPDF(String fileName, ReportTaskEntity pdfTask) throws Exception {
		
		URI url = new URI(pdfTask.getUrl());
		downloadFile(fileName, url,".pdf", "test PDF report");
	}

	@Override
	public void run() {
		try {
			download();
		} catch (Exception e) {
			logger.error("Report PDF Doenload : ", e);
		}
		
	}

}
