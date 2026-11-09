package com.quantum.listeners;

import com.qmetry.qaf.automation.step.client.Scenario;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SortingMethodInterceptor implements IMethodInterceptor {

	@Override
	public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
		Map<String, List<IMethodInstance>> methodMap = new java.util.HashMap<>();
		for(IMethodInstance method : methods) {
			String file = (String) ((Scenario)method.getInstance()).getFileName();
			methodMap.computeIfAbsent(file, k -> new ArrayList<>()).add(method);
			methodMap.values().forEach(list -> 
			list.sort(java.util.Comparator.comparingInt(m -> 
			(int) ((Scenario) m.getInstance()).getMetadata().get("lineNo"))));
		}
		return methodMap.values().stream()
			    .flatMap(List::stream)
			    .collect(Collectors.toList());
	}
}
