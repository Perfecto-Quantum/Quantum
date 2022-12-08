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

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.DriverCommand;

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

	@Override
	public void beforeCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {

		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.QUIT)) {
			boolean virtualDeviceCap = false;
			boolean virtualDeviceName = false;
			Map<String, Object> map = driver.getCapabilities().asMap();
			for (String cap : map.keySet()) {
				if (cap.equalsIgnoreCase("useVirtualDevice")) {
					virtualDeviceCap = (boolean) map.get(cap);
				}
				if (cap.contains("deviceName")) {
					if (String.valueOf(map.get(cap)).toUpperCase().contains("EMULATOR")
							|| String.valueOf(map.get(cap)).toUpperCase().contains("SIMULATOR"))
						virtualDeviceName = true;
				}
			}
//			System.out.println("Virtual device capability - >" + virtualDeviceCap);
			if (!virtualDeviceCap && !virtualDeviceName) {
				ConfigurationUtils.setActualDeviceCapabilities(driver.getCapabilities().asMap());
				try {
					String appName = (String) driver.getCapabilities().getCapability("applicationName");
					if (StringUtil.isNotBlank(appName) && StringUtil
							.isBlank((String) driver.getCapabilities().getCapability("eclipseExecutionId"))) {

						DeviceUtils.closeApp(appName, "name", true, driver);
					}
				} catch (Exception ex) {
				}

				if (ConfigurationManager.getBundle().getString("remote.server").toLowerCase()
						.contains(".perfectomobile.com")) {
					try {
						Map<String, Object> params = new HashMap<>();
						driver.executeScript("mobile:execution:close", params);
					} catch (Exception ex) {
					}

					try {
						driver.close();
					} catch (Exception ex) {
					}
				}
			}
		}

	}

	@Override
	public void afterCommand(QAFExtendedWebDriver driver, CommandTracker commandTracker) {
		if (commandTracker.getCommand().equalsIgnoreCase(DriverCommand.QUIT)) {

			try {
				
				

				if (ConfigurationUtils.getBaseBundle().getString("remote.server", "").contains("perfecto")) {
					
					String executionID = ConfigurationManager.getBundle().getString("executionId");
					
					Thread[] rptDownloadTasks = new Thread[] {
							new Thread(new ReportPDF(executionID), "Report PDF Downloader"),
							new Thread(new ReportSummary(executionID), "Report Summary Downloader"),
							new Thread(new ReportVideos(executionID), "Report Video Downloader"),
							new Thread(new ReportAttachments(executionID), "Report Attachment Downloader")
					};
					
					for(Thread dwdTask : rptDownloadTasks) {
						dwdTask.start();
					}
					
					for(Thread dwdTask : rptDownloadTasks) {
						dwdTask.join();
					}
				}

			} catch (Exception ex) {

			}
		}

	}

	@Override
	public void beforeInitialize(Capabilities desiredCapabilities) {
		if (ConfigurationUtils.getBaseBundle().getString("remote.server", "").contains("perfecto")) {
			String eclipseExecutionId = ConfigurationUtils.getExecutionIdCapability();

			if (StringUtil.isNotBlank(eclipseExecutionId)) {
				((DesiredCapabilities) desiredCapabilities).setCapability("eclipseExecutionId", eclipseExecutionId);
			}
		}

		String jobName = getBundle().getString("JOB_NAME", System.getProperty("reportium-job-name"));
		int jobNumber = getBundle().getInt("BUILD_NUMBER", System.getProperty("reportium-job-number") == null ? 0
				: Integer.parseInt(System.getProperty("reportium-job-number")));
		String jobBranch = System.getProperty("reportium-job-branch");
		String tags = System.getProperty("reportium-tags");

		if (jobName != null) {
			((DesiredCapabilities) desiredCapabilities).setCapability("report.jobName", jobName);
			((DesiredCapabilities) desiredCapabilities).setCapability("report.jobNumber", jobNumber);
		}
		if (jobBranch != null) {
			((DesiredCapabilities) desiredCapabilities).setCapability("report.jobBranch", jobBranch);
		}
		if (tags != null) {
			((DesiredCapabilities) desiredCapabilities).setCapability("report.tags", tags);
		}

		String pureAppiumBehavior = getBundle().getString("pureAppiumBehavior", "ignore");
		if (desiredCapabilities.getPlatformName() != null) {
			if (pureAppiumBehavior.equalsIgnoreCase("native")) {
				if (desiredCapabilities.getPlatformName().toString().equalsIgnoreCase("android")) {
					((DesiredCapabilities) desiredCapabilities).setCapability("enableAppiumBehavior", true);
				}
			} else if (pureAppiumBehavior.equalsIgnoreCase("hybrid")) {
				if (desiredCapabilities.getPlatformName().toString().equalsIgnoreCase("android")) {
					((DesiredCapabilities) desiredCapabilities).setCapability("enableAppiumBehavior", true);
				}
				((DesiredCapabilities) desiredCapabilities).setCapability("useAppiumForHybrid", true);
			} else if (pureAppiumBehavior.equalsIgnoreCase("web")) {
				if (desiredCapabilities.getPlatformName().toString().equalsIgnoreCase("android")) {
					((DesiredCapabilities) desiredCapabilities).setCapability("enableAppiumBehavior", true);
				}
				((DesiredCapabilities) desiredCapabilities).setCapability("useAppiumForWeb", true);
			} else if (pureAppiumBehavior.equalsIgnoreCase("disable")) {
				((DesiredCapabilities) desiredCapabilities).setCapability("enableAppiumBehavior", false);
				((DesiredCapabilities) desiredCapabilities).setCapability("useAppiumForHybrid", false);
				((DesiredCapabilities) desiredCapabilities).setCapability("useAppiumForWeb", false);
			}
		}
		if (ConfigurationUtils.getBaseBundle().getString("remote.server", "").contains("perfecto")) {
			if (ConfigurationManager.getBundle().getString("perfecto.harfile.enable", "false").equals("true")) {
				Object platformName = ((DesiredCapabilities) desiredCapabilities).getCapability("platformName");
				if (platformName != null) {
					if (platformName.toString().equalsIgnoreCase("windows"))
						((DesiredCapabilities) desiredCapabilities).setCapability("captureHAR", true);
				}
			}
		}

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
		// ConsoleUtils.logWarningBlocks("DEVICE PROPERTIES: " + dcaps.toString());
		ConsoleUtils.logWarningBlocks("DEVICE PROPERTIES: " + driver.getCapabilities().toString());

		long implicitWait = ConfigurationManager.getBundle().getLong("seleniun.wait.implicit", 0);
		driver.manage().timeouts().implicitlyWait(Duration.ofMillis(implicitWait));

		if (ConfigurationManager.getBundle().getString("remote.server").toLowerCase().contains(".perfectomobile.com")) {
			ConfigurationManager.getBundle().addProperty("executionId",
					driver.getCapabilities().getCapability("executionId"));
		}
		ConfigurationManager.getBundle().setProperty(DRIVER_START_TIMER, System.currentTimeMillis());

	}

}
