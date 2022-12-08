package com.perfecto.reports;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;

public class ReportVideos extends RESTClient {
	
	private String executionID;
	
	private static final Log logger = LogFactoryImpl.getLog(ReportVideos.class);
	
	public ReportVideos(String executionID) throws Exception{
		
		this.executionID = executionID;
	}

	public void download() throws Exception {
		
		if(!ConfigurationManager.getBundle().getBoolean("perfecto.download.video", false)) return ;
		
		logger.info("Starting Video Download - " + executionID);
		
		waitForReportsToBeReady(executionID);
		
		JsonArray resourceDetails = getResources(executionID);
		JsonObject resJObject;
		JsonObject videoJObj;
		String videoDownloadURL;
		String videoFormat;
		String fileName;
		
		
		for(JsonElement resDetail : resourceDetails) {
			resJObject = resDetail.getAsJsonObject();
			
			JsonArray videos = resJObject.getAsJsonArray("videos");
			
			if(videos.size()==0) {
				logger.info("No Video found for Test Execution ID - " + executionID);
			}
			
			for(JsonElement video:videos) {
				
				videoJObj = video.getAsJsonObject();
				videoDownloadURL = videoJObj.get("downloadUrl").getAsString();
				videoFormat = "." + videoJObj.get("format").getAsString();
				fileName = getDownloadedFileName(resJObject);
				
				downloadFile(fileName + "_Video", URI.create(videoDownloadURL), videoFormat, "video");
				logger.info("Video Download completed - " + executionID);
			}
			
		}
		
	}

	@Override
	public void run() {
		try {
			download();
		} catch (Exception e) {
			logger.error("Report Video Download", e);
		}
		
	}
	
}
