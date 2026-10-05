package com.quantum.utils;


import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.testng.IClass;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.step.StepExecutionTracker;
import com.qmetry.qaf.automation.step.StringTestStep;
import com.qmetry.qaf.automation.step.TestStep;
import com.qmetry.qaf.automation.step.client.DataDrivenScenario;
import com.qmetry.qaf.automation.step.client.Scenario;
import com.qmetry.qaf.automation.step.client.TestNGScenario;
import com.qmetry.qaf.automation.step.client.text.BDDDefinitionHelper.ParamType;

import io.cucumber.core.backend.ObjectFactory;
import io.cucumber.core.options.RuntimeOptions;
import io.cucumber.core.options.RuntimeOptionsBuilder;

public class CucumberJsonUtils {

	public static JsonObject buildStep(StepExecutionTracker stepExecutionTracker) {
		JsonObject step = new JsonObject();

		int stepIndex = stepExecutionTracker.getStepIndex();

		TestStep testStep = stepExecutionTracker.getStep();
		String stepDesc = testStep.getDescription();

		String[] fileNameArr = testStep.getFileName().split("\\.");

		String keyword = stepExecutionTracker.getType();
		Scenario scn= (Scenario) stepExecutionTracker.getStepCompositer();
		StringTestStep strStep = (StringTestStep) scn.getSteps().stream().toArray()[stepIndex];
		String stepName = stepDesc.replaceAll("^(Given|When|Then)\\s*", "");
		String glueLocation =fileNameArr[fileNameArr.length-1]+"."+testStep.getName()+"()";


		String status= stepExecutionTracker.isSuccess().booleanValue() ? "passed":"failed";;
		long durationNano=(stepExecutionTracker.getEndTime() - stepExecutionTracker.getStartTime());
		Throwable error=stepExecutionTracker.getException();
		// keyword & name
		step.addProperty("keyword", keyword);
		step.addProperty("name", stepName);
		step.addProperty("line", strStep.getLineNumber());

		// match
		JsonObject match = new JsonObject();
		match.addProperty("location", glueLocation);
		step.add("match", match);

		// result
		JsonObject result = new JsonObject();
		result.addProperty("status", status.toLowerCase());
		result.addProperty("duration", durationNano);

		if (error != null) {
			result.addProperty("error_message", error.getMessage());
		}
		step.add("result", result);

		return step;
	}

	@SuppressWarnings("unchecked")
	public static JsonObject buildScenario(ITestResult result) throws IOException {
		String id = "feature-name;scenario-e";
		Scenario dataDrivenScenario = (Scenario)result.getMethod().getInstance();
		String fileName = dataDrivenScenario.getFileName();
		String featureFile = System.getProperty("user.dir")+File.separator+fileName;
		String featureTags = "";
		String featureName = "";
		try (BufferedReader reader = new BufferedReader(new FileReader(featureFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if(line.contains("@")) {
					featureTags = line.trim();					
				}
				if(line.contains("Feature:")) {
					featureName = line.replace("Feature:", "").trim();
					break;
				}
			}
		}
		catch (IOException e) {e.printStackTrace();}
	String featureId = featureName.toLowerCase().replaceAll(" ", "-");
	int lineNumber = (int)dataDrivenScenario.getMetadata().get("lineNo");
	
	JsonObject feature = new JsonObject();
	feature.addProperty("id", featureId);
	feature.addProperty("uri", featureFile);
	feature.addProperty("keyword", "Feature");
	feature.addProperty("name", featureName);
	feature.addProperty("line", 3);
	


	if(featureTags != null && !featureTags.isEmpty()) {
		JsonArray ftagsArray = new JsonArray();
		String[] tags = featureTags.split(" ");
		for (String tag : tags) {
			var tagObj = new JsonObject();
			tagObj.addProperty("name", tag);
			ftagsArray.add(tagObj);
		}
		feature.add("tags", ftagsArray);
	}
	
	
	JsonObject scenario = new JsonObject();
	ArrayList<JsonObject> stepQueue=(ArrayList) getBundle().getProperty("stepObjects");
	scenario.addProperty("id", result.getTestName().replaceAll(" ", "-"));
	scenario.addProperty("keyword", "Scenario");
	scenario.addProperty("name", result.getTestName()+ (result.getParameters().length > 0 ? " [" + result.getParameters()[0] + "]" : ""));
	scenario.addProperty("line", lineNumber);
	scenario.addProperty("type", "scenario");
	scenario.addProperty("start_timestamp", ConfigurationManager.getBundle().getPropertyValueOrNull("test_start_timestamp"));
	
	
	
	
	JsonArray stepArray = new JsonArray();
	for (JsonObject step : stepQueue) {
		stepArray.add(step);
	}
	scenario.add("steps", stepArray);
	JsonArray tagsArray = new JsonArray();


	String[] tags = dataDrivenScenario.getM_groups();
	for (String tag : tags) {
		var tagObj = new JsonObject();
		tagObj.addProperty("name", tag);
		tagsArray.add(tagObj);
	}
	scenario.add("tags", tagsArray);

	JsonArray elementsArr = new JsonArray();
	elementsArr.add(scenario);

	feature.add("elements", elementsArr);
	
	return feature;
}



}

