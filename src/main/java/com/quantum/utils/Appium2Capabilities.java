package com.quantum.utils;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;

public class Appium2Capabilities implements Capabilities{
	
	private static final long serialVersionUID = 1L;
	private Capabilities capabilities;
	
	public Appium2Capabilities(Capabilities thatCapabilities) {
		this.capabilities = thatCapabilities;
	}

	@Override
	public Map<String, Object> asMap() {
		return this.capabilities.asMap();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getCapability(String capabilityName) {
		
		Object capabilityValue = this.capabilities.getCapability(capabilityName);
		
		if(null == capabilityValue) {
			
			Object desiredCapabilitiesObject = capabilities.getCapability("desired");
			
			if(desiredCapabilitiesObject instanceof Map<?, ?>) {
				
				Map<String,Object> desiredCapabilitiesMap = (Map<String, Object>)desiredCapabilitiesObject;
				
				capabilityValue = desiredCapabilitiesMap.getOrDefault(capabilityName, "");
			}
		}
		
		if(null != capabilityValue) {
			
			if(capabilityName.contains("platformName")) {
				return getPlatformName();
			}
			
			if(capabilityValue.toString().isBlank()) {
				System.out.println(String.format("%s capability not available!", capabilityName));
			}

		}else{
			capabilityValue = "";
		}
		
		
		return capabilityValue;
	}
	
	@Override
	public String getBrowserName() {
		return getCapability("browserName").toString();
	}
	
	@Override
	public String getBrowserVersion() {
		return getCapability("browserVersion").toString();
	}
	
	@Override
	public boolean is(String capabilityName) {
		return !"".equals(getCapability(capabilityName).toString());
	}
	
	
	@Override
	public Platform getPlatformName() {
		
		Platform plateForm = Stream.of("platform","platformName")
			      .map(this::getCapability)
			      .filter(Objects::nonNull)
			      .map(cap -> {
			        if (cap instanceof Platform) {
			          return (Platform) cap;
			        }

			        try {
			          return Platform.fromString((String.valueOf(cap)));
			        } catch (WebDriverException e) {
			          return null;
			        }
			      })
			      .filter(Objects::nonNull)
			      .findFirst()
			      .orElse(null);
		
		if(null != plateForm) {
			return plateForm.equals(Platform.LINUX)? Platform.ANDROID: plateForm.equals(Platform.MAC)? Platform.IOS:plateForm;
		}else {
			return null;
		}
	}
	
	@Override
	public String toString() {
		
		return capabilities.toString();
	}

}
