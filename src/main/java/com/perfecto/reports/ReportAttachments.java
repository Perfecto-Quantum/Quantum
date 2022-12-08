package com.perfecto.reports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.http.client.utils.URIBuilder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;

public class ReportAttachments extends RESTClient{
	
	private static final Log logger = LogFactoryImpl.getLog(ReportAttachments.class);
	
	private String executionID;
	
	public ReportAttachments(String executionID) throws Exception{
		this.executionID = executionID;
	}
	
	public void download() throws Exception {
		
		if(!ConfigurationManager.getBundle().getBoolean("perfecto.download.attachments", false)) return ;
		
		logger.info("Attachment Download started - " + executionID);
		
		waitForReportsToBeReady(executionID);
		
		JsonArray resourceDetails = getResources(executionID);
		
		JsonObject resJObject;
		JsonArray artifacts;
		JsonObject artifact;
		String artifactType;
		String path;
		String fileName;
		String reportTYpe;
		String suffix;
		
		for(JsonElement resDetail : resourceDetails) {
			resJObject = resDetail.getAsJsonObject();
			
			fileName = getDownloadedFileName(resJObject);
			
			artifacts = resJObject.getAsJsonArray("artifacts");
			
			
			for (JsonElement artifactElement : artifacts) {
				artifact = artifactElement.getAsJsonObject();
				artifactType = artifact.get("type").getAsString();
				path = artifact.get("path").getAsString();
				URIBuilder uriBuilder = new URIBuilder(path);
				
				switch(artifactType) {
				case "DEVICE_LOGS":
					suffix = "_device_logs.zip";
					reportTYpe = "device logs";
					break;
				case "NETWORK":
					suffix = "_network_logs.zip";
					reportTYpe = "network logs";
					break;
				case "VITALS":
					suffix = "_vitals_logs.zip";
					reportTYpe = "vitals logs";
					break;
				case "accessibility":
					suffix = artifact.get("fileName").getAsString().replace(" ", "_");
					reportTYpe = "accessibility reports";
					break;
				default:
					suffix = "";
					reportTYpe = "";
					break;
				}
				
				if(!"".equals(suffix)) {
					downloadFile(fileName, uriBuilder.build(), suffix, reportTYpe);
					logger.info("Attachment Download Completed - " + executionID);
				}
			}
			
		}
		
	}

	@Override
	public void run() {
		try {
			download();
		} catch (Exception e) {
			logger.error("Download Attachments", e);
		}
		
	}

}
