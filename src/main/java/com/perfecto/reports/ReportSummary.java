package com.perfecto.reports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.http.client.utils.URIBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;

public class ReportSummary extends RESTClient {
	
	private String executionID;
	
	public ReportSummary(String executionID) throws Exception{
		this.executionID = executionID;
	}
	
	private static final Log logger = LogFactoryImpl.getLog(ReportSummary.class);

	private void download() throws Exception {
		
		if(!ConfigurationManager.getBundle().getBoolean("perfecto.download.summaryReports", false)) return ;

		logger.info("Download Report Summary started for execution ID: " + executionID);

		waitForReportsToBeReady(executionID);
		
		JsonArray resourceDetails = getResources(executionID);
		JsonObject resJObject;
		
		for (JsonElement resDetail : resourceDetails) {
			resJObject = resDetail.getAsJsonObject();
			
			JsonObject platforms = resJObject.getAsJsonArray("platforms").get(0).getAsJsonObject();
			String deviceId = platforms.get("deviceId").getAsString();
			
			URIBuilder uriBuilder = new URIBuilder(getReportBaseURL() + "/export/api/v1/test-executions/pdf");
			uriBuilder.addParameter("externalId[0]", executionID);
			
			downloadFile(deviceId + "_ExecutionSummaryReport", uriBuilder.build(), ".pdf", "execution summary PDF report");
			
			logger.info("Download Report Summary completed for execution ID: " + executionID);
		}
		
	}

	@Override
	public void run() {
		try {
			download();
		} catch (Exception e) {
			logger.error(e);
		}
	}

}
