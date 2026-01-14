/*******************************************************************************
 * QMetry Automation Framework provides a powerful and versatile platform to
 * author
 * Automated Test Cases in Behavior Driven, Keyword Driven or Code Driven
 * approach
 * Copyright 2016 Infostretch Corporation
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE
 * You should have received a copy of the GNU General Public License along with
 * this program in the name of LICENSE.txt in the root folder of the
 * distribution. If not, see https://opensource.org/licenses/gpl-3.0.html
 * See the NOTICE.TXT file in root folder of this source files distribution
 * for additional information regarding copyright ownership and licenses
 * of other open source software / files used by QMetry Automation Framework.
 * For any inquiry or need additional information, please contact
 * support-qaf@infostretch.com
 *******************************************************************************/

package com.quantum.listeners;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;
import org.openqa.selenium.support.ui.FluentWait;

import com.perfecto.reports.ReportAttachments;
import com.perfecto.reports.ReportPDF;
import com.perfecto.reports.ReportSummary;
import com.perfecto.reports.ReportVideos;
import com.qmetry.qaf.automation.core.ConfigurationManager;
import com.qmetry.qaf.automation.ui.webdriver.CommandTracker;
import com.qmetry.qaf.automation.ui.webdriver.QAFExtendedWebDriver;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebDriverCommandAdapter;
import com.qmetry.qaf.automation.util.StringUtil;
import com.quantum.utils.ConfigurationUtils;
import com.quantum.utils.ConsoleUtils;
import com.quantum.utils.DeviceUtils;

public class PerfectoDriverListener extends QAFWebDriverCommandAdapter {
	public static final String DRIVER_START_TIMER = "DriverTimer";
	
	private final Log logger = LogFactoryImpl.getLog(PerfectoDriverListener.class);

	private boolean isQuitCommand(CommandTracker commandTracker) {
		
		String currentCommand = commandTracker.getCommand();
		return DriverCommand.QUIT.equalsIgnoreCase(currentCommand);
	}
	
	private boolean isVirtualDevice(Optional<Object> deviceName) {
		Pattern pattern = Pattern.compile(".*(em|sim)ulator.*",Pattern.CASE_INSENSITIVE);
		
		return pattern.matcher(deviceName.toString()).find();
	}
	
	private void closeApp(QAFExtendedWebDriver driver) {
		try {
			String appName = (String) driver.getCapabilities().getCapability("applicationName");
			
			String eclipseExecId = (String) driver.getCapabilities().getCapability("eclipseExecutionId");
			
			if (StringUtil.isNotBlank(appName) && StringUtil.isBlank(eclipseExecId)) {
				DeviceUtils.closeApp(appName, "name", true, driver);
			}
		} catch (Exception ex) {
			
		}
	}
	
	
	@Override
	public void beforeCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {

		if (isQuitCommand(commandTracker)) {
			
			boolean virtualDeviceCap = false;
			boolean virtualDeviceName = false;
			
			Map<String, Object> capabilities = driver.getCapabilities().asMap();
			
			// Is Use Virtual Device Capability set
			virtualDeviceCap =  capabilities.keySet().stream()
					.filter( cap -> cap.endsWith("useVirtualDevice"))
					.map(cap -> capabilities.get(cap)).findFirst().isPresent();

			// Get Device Name
			Optional<Object> deviceNameOptional = capabilities.keySet().stream()
					.filter( cap -> cap.endsWith("deviceName"))
					.map(cap -> capabilities.get(cap)).findFirst();
			
			// Has Virtual Device in name
			virtualDeviceName = isVirtualDevice(deviceNameOptional);

			if (!virtualDeviceCap && !virtualDeviceName) {
				
				// Set the Derised Capability obtained from Driver.
				ConfigurationUtils.setActualDeviceCapabilities(capabilities);
				
				// Close App
				closeApp(driver);
			}
		}

	}

	@Override
	public void afterCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		if (isQuitCommand(commandTracker)) {

			try {

				if (isRunningOnPerfecto()) {

					String executionID = ConfigurationManager.getBundle().getString("executionId");
					
					ExecutorService reportDownloadServ = Executors.newFixedThreadPool(5);
					
					reportDownloadServ.execute(new ReportPDF(executionID));
					reportDownloadServ.execute(new ReportSummary(executionID));
					reportDownloadServ.execute(new ReportVideos(executionID));
					reportDownloadServ.execute(new ReportAttachments(executionID));
					
					reportDownloadServ.shutdown();
					
					FluentWait<ExecutorService> waitForExecutor = new FluentWait<ExecutorService>(reportDownloadServ);
					
					waitForExecutor.withTimeout(Duration.ofMinutes(10));
					waitForExecutor.pollingEvery(Duration.ofSeconds(1));
					waitForExecutor.until(new Function<ExecutorService,Boolean>(){

						@Override
						public Boolean apply(ExecutorService executor) {
							return executor.isTerminated();
						}
						
					});
				}

			} catch (Exception ex) {
				logger.error(ex.getMessage());
			}
		}
	}

	private boolean isRunningOnPerfecto() {
		String remoteServer = ConfigurationUtils.getBaseBundle().getString("remote.server", "");

		Pattern pattern = Pattern.compile(".*perfecto.*", Pattern.CASE_INSENSITIVE);

		return pattern.matcher(remoteServer).find();
	}

	private void enableAppiumBehaviour(Capabilities desiredCapabilities) {

		Platform platform = desiredCapabilities.getPlatformName();

		if (platform != null) {
			String pureAppiumBehavior = getBundle().getString("pureAppiumBehavior", "ignore");

			String platformName = platform.name();

			boolean enableAppiumBehavior = false;
			boolean useAppiumForHybrid = false;
			boolean useAppiumForWeb = false;

			if ("android".equalsIgnoreCase(platformName)) {
				enableAppiumBehavior = true;
			}

			switch (pureAppiumBehavior.toLowerCase()) {
			case "native":
				// no action needed
				break;
			case "hybrid":
				useAppiumForHybrid = true;
				break;
			case "web":
				useAppiumForWeb = true;
				break;
			default:
				enableAppiumBehavior = false;
				break;
			}

			((DesiredCapabilities)desiredCapabilities).setCapability("perfecto:enableAppiumBehavior", enableAppiumBehavior);
			((DesiredCapabilities)desiredCapabilities).setCapability("perfecto:useAppiumForHybrid", useAppiumForHybrid);
			((DesiredCapabilities)desiredCapabilities).setCapability("perfecto:useAppiumForWeb", useAppiumForWeb);

		}
	}

	private void setEclipseExecutionId(Capabilities desiredCapabilities) {

		String eclipseExecutionId = ConfigurationUtils.getExecutionIdCapability();

		if (StringUtil.isNotBlank(eclipseExecutionId)) {
			((DesiredCapabilities)desiredCapabilities).setCapability("eclipseExecutionId", eclipseExecutionId);
		}
	}

	private void setReportiumJobDetails(Capabilities desiredCapabilities) {
		
		
		String jobName = getBundle().getString("JOB_NAME", System.getProperty("reportium-job-name"));
		
		int jobNumber = getBundle().getInt("BUILD_NUMBER", System.getProperty("reportium-job-number") == null ? 0
				: Integer.parseInt(System.getProperty("reportium-job-number")));
		
		String jobBranch = System.getProperty("reportium-job-branch");
		
		String tags = System.getProperty("reportium-tags");

		if(desiredCapabilities.getCapability("perfecto:options")==null) {
			if (jobName != null) {
				((DesiredCapabilities) desiredCapabilities).setCapability("perfecto:report.jobName", jobName);
				((DesiredCapabilities) desiredCapabilities).setCapability("perfecto:report.jobNumber", jobNumber);
			}
			if (jobBranch != null) {
				((DesiredCapabilities) desiredCapabilities).setCapability("perfecto:report.jobBranch", jobBranch);
			}
			if (tags != null) {
				((DesiredCapabilities) desiredCapabilities).setCapability("perfecto:report.tags", tags);
			}
		}else {
			
			@SuppressWarnings("unchecked")
			HashMap<String, Object> perfectoOptions = ((HashMap<String, Object>)desiredCapabilities.getCapability("perfecto:options"));
			if(jobName != null) {
				perfectoOptions.put("report.jobName", jobName);
				perfectoOptions.put("report.jobNumber", jobNumber);
			}
			
			if (jobBranch != null) {
				perfectoOptions.put("report.jobBranch", jobBranch);
			}
			if (tags != null) {
				perfectoOptions.put("report.tags", tags);
			}
			
			((DesiredCapabilities) desiredCapabilities).setCapability("perfecto:options",perfectoOptions);
		}
		
		
	}

	public void enablePerfectoHARFile(Capabilities desiredCapabilities) {

		try {
			boolean enableHARCapture = ConfigurationManager.getBundle().getBoolean("perfecto.harfile.enable");

			if (enableHARCapture) {

				Platform platform = desiredCapabilities.getPlatformName();
				if (platform != null) {
					String platformName = platform.name();
					if ("windows".equalsIgnoreCase(platformName))
						((DesiredCapabilities)desiredCapabilities).setCapability("perfecto:captureHAR", true);
				}
			}
		} catch (Exception e) {
			ConsoleUtils.logWarningBlocks("perfecto.harfile.enable key should be true/false.");
		}
	}

	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {

		// Perfecto specific capabilities dont continue if execution environment is not
		// Perfecto
		if (!isRunningOnPerfecto())
			return;

		// Set Eclipse Execution ID
		setEclipseExecutionId(desiredCapabilities);

		// Set Reportium related capabilities
		setReportiumJobDetails(desiredCapabilities);
		
		if(desiredCapabilities.getCapability("perfecto:options")==null) {
			// Enable Appium Behaviour - Perfecto specific capabilities
			enableAppiumBehaviour(desiredCapabilities);
		}
		
	}

	private void setExecutionId(Capabilities desiredCapabilities) {
		if (isRunningOnPerfecto()) {
			ConfigurationManager.getBundle().addProperty("executionId",
					desiredCapabilities.getCapability("executionId"));
		}
	}

	private void setImplicitWait(WebDriver driver) {
		long implicitWait = ConfigurationManager.getBundle().getLong("seleniun.wait.implicit", 0);
		driver.manage().timeouts().implicitlyWait(Duration.ofMillis(implicitWait));
	}

	@Override
	public void onInitialize(QAFExtendedWebDriver driver) {
		// Commenting the code to get device info as it fails in two conditions - when
		// there is no security token mentioned and when the execution environment is
		// beind proxy.
		// MutableCapabilities dcaps =
		// CloudUtils.getDeviceProperties((MutableCapabilities)
		// driver.getCapabilities());

		ConfigurationUtils.setActualDeviceCapabilities(driver.getCapabilities().asMap());

		ConsoleUtils.logWarningBlocks("DEVICE PROPERTIES: " + driver.getCapabilities().toString());
		
		// Set Implicit wait of Selenium Driver from 'seleniun.wait.implicit' property.
		setImplicitWait(driver);

		// Set Execution ID
		setExecutionId(driver.getCapabilities());

		// Start the Driver Session Timer
		ConfigurationManager.getBundle().setProperty(DRIVER_START_TIMER, System.currentTimeMillis());

	}

}
