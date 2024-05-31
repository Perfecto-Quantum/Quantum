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
		
		
		System.out.println(capabilities);
		
		if(null == capabilityValue) {
			
			Object desiredCapabilitiesObject = capabilities.getCapability("desired");
			
			if(desiredCapabilitiesObject instanceof Map<?, ?>) {
				
				Map<String,Object> desiredCapabilitiesMap = (Map<String, Object>)desiredCapabilitiesObject;
				
				capabilityValue = desiredCapabilitiesMap.getOrDefault(capabilityName, "");
			}
		}
		
		if(capabilityValue.toString().isBlank()) {
			System.out.println(String.format("%s capability not available!", capabilityName));
		}
		
		return capabilityValue;
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

}
