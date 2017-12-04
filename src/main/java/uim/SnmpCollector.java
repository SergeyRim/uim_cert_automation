package uim;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;



public class SnmpCollector {

	private static final Logger log = LogManager.getLogger("SnmpCollector");
	WebDriver driver;
	
	public SnmpCollector (WebDriver driver) {
		this.driver = driver;
	}
	
	public Boolean checkVendorAndModel () throws InterruptedException {
		
		Navigation navi = new Navigation (driver);
		
		WebElement vendor = navi.getWebElement("//input[@for='vendor']");
		String vendorName = vendor.getAttribute("value");
		if (vendorName.contains("Unknown") || vendorName.contains("unknown")) {
			log.error("Vendor name is incorrect: "+vendorName);
		} else {
			log.info("Vendor: "+vendorName);
		}
		
		WebElement model = navi.getWebElement("//input[@for='model']");
		String modelName = model.getAttribute("value");
		if (modelName.contains("Unknown") || modelName.contains("unknown")) {
			log.error("Model name is incorrect: "+modelName);
		} else {
			log.info("Model: "+modelName);
		}

		return true;
	}
	
	
	public Boolean verifyNewMFVC (ArrayList<ArrayList<String[]>> metricsList, String simID) throws InterruptedException {
		Navigation navi = new Navigation (driver);
		Boolean isDone = true, isVcFound = false;
		String mfHR = new String();
		String vcMib = new String();
		JavascriptExecutor je = (JavascriptExecutor) driver;
				
		checkVendorAndModel();
		
		for (int i=0; i<metricsList.size(); i++) {
			
			isVcFound = false;
			//Look for Metric Family by link in mib format
			List<WebElement> mfMib = driver.findElements(By.xpath("//tr[contains(@id,'.DevId"+metricsList.get(i).get(1)[0]+"')]"));
			if (mfMib.size()==0) {
				log.fatal("MF \""+metricsList.get(i).get(1)[0]+"\" not found.");
				continue;
			}
			
			//If found required MF, click on it
			mfMib.get(0).click();
			Thread.sleep(500);
			
			//Wait for "Loading..." message
			navi.waitForElement("//div[contains(text(),'Loading')]", 2,0);
			
			//Get the Metric Family display name and compare it with HR format from readme
			do {
				try {
					isDone=true;
					mfHR = driver.findElement(By.xpath("//tr[contains(@id,'.DevId"+metricsList.get(i).get(1)[0]+"')]/td/span/span[3]/div")).getText();												
				} catch (Exception e) {
					isDone=false;
					log.warn("Unable to get metric family display name.");
					Thread.sleep(500);						
				}
			} while (!isDone);
			
			if (mfHR.equals(metricsList.get(i).get(1)[1])) {
				//Reporter.log("INFO: Found "+metricsList.get(i).get(1)[1]+" ("+mfHR+") MF.");
			} else {
				log.error("Found \""+metricsList.get(i).get(1)[1]+"\" MF but the display name is \""+mfHR+"\".");
			}
			
			//Search for the required VC element
			//Get the list of elements from table
			List<WebElement> elements = driver.findElements(By.xpath("//table[@class='dataTable']/tbody[@role='alert']/tr"));
			
			//Click on elements and check they VC
			String element_name = new String();
			for (int k=0;k<elements.size();k++) {
				//Scroll to element for each 3rd line
				if (k % 5 == 0) {
					je.executeScript("arguments[0].scrollIntoView(true);",elements.get(k));					
				}
				//Click on element
				elements.get(k).click();
				Thread.sleep(200);
				//Get VC in mib format
				do {
					try {
						isDone=true;
						vcMib = driver.findElement(By.xpath("//input[@name='vendorCert']")).getAttribute("value");						
					} catch (Exception e) {
						isDone=false;
						log.warn("Unable to get VC mib name.");
						Thread.sleep(200);						
					}
				} while (!isDone);
				
				
				//Compare VC mib with required VC mib
				if (vcMib.equals(metricsList.get(i).get(0)[0])) {
					//Get Label (element name)
					element_name = navi.getWebElement("//input[@name='Names']").getAttribute("value");
					log.info("Found \""+vcMib+"\" VC for \""+metricsList.get(i).get(1)[1]+"\" MF with element: \""+element_name+"\"");
					isVcFound = true;
					break;
				} 
			}
			
			// If no required VC is found, just skip this cycle
			if (!isVcFound) {
				log.error("VC \""+metricsList.get(i).get(0)[0]+"\" not found.");
				navi.findDevice(simID);
				continue;
			}
			
			//Click on element
			//WebElement clickOnElement = navi.getWebElement("//div[text()='"+element_name+"']");
			WebElement clickOnElement = navi.getWebElement("//tr[starts-with(@id,'snmpcollector:snmpcollector:snmpcollector')]/.//div[text()='"+element_name+"']");
			clickOnElement.click();
			Thread.sleep(500);
			navi.waitForElement("//div[contains(text(),'Loading')]", 2,0);
			
			//Enable reporting data for metrics
			for (int j=2; j<metricsList.get(i).size();j++) {
				
				//Check if QOS_ metric exists
				if (driver.findElements(By.xpath("//tr/td[4 and text()='"+metricsList.get(i).get(j)[1]+"']")).size()>0) {
					
					//Click on QOS_ metric
					WebElement currentMetric = navi.getWebElement("//tr/td[4 and text()='"+metricsList.get(i).get(j)[1]+"']");
					currentMetric.click();
					
					//Wait while correct page will be loading
					navi.waitForElement("//label[text()='"+metricsList.get(i).get(j)[1]+"']", 1,0);
					Thread.sleep(500);
					
					//Verify if checkbox not checked
					if (driver.findElements(By.xpath("//label[text()='Publish Data']/../div[1][@class='icheckbox checked']")).size()==0) {
						WebElement publishData = navi.getWebElement("//label[text()='Publish Data']/../div[1]");
						publishData.click();
						Thread.sleep(100);
						log.info("Enabled reporting for metric \""+metricsList.get(i).get(j)[1]+"\"");
						//Wait for applying
						//navi.waitForElement("//label[@for='baselineActive' and text()='Compute Baseline' and @disabled]", 2);
					} else {
						log.info("Reporting for metric \""+metricsList.get(i).get(j)[1]+"\" already enabled.");
					}
					
				} else {
					log.error("Metric \""+metricsList.get(i).get(j)[1]+"\" not found.");
				}
								
			}
			
			Thread.sleep(500);
			//Check if Save button disabled or not
			if (driver.findElements(By.xpath("//button[@id='saveBtn' and @aria-disabled='false']")).size()>0) {
				//Click on Save button
				WebElement saveButton = navi.getWebElement("//button[@id='saveBtn']");
				saveButton.click();
			
				navi.waitForElement("//div[contains(text(),'Saving')]", 2,0);

				//Click on OK button
				saveButton = navi.getWebElement("//button[@name='ok']");
				saveButton.click();
			}
			
			Thread.sleep(1000);
			navi.findDevice(simID);
									
		}

		return true;

	}
	
	
	public void verifyAlreadyCertifiedMFVC (ArrayList<String[]> vcmfList) throws InterruptedException {
		
		Navigation navi = new Navigation (driver);
		Boolean isDone = true, isVcFound = false;
		String mfHR = new String();
		String vcMib = new String();
		Actions action = new Actions (driver);
		JavascriptExecutor je = (JavascriptExecutor) driver;
		
		checkVendorAndModel();
		
		for (int i=0; i<vcmfList.size(); i++) {
			
			isVcFound = false;
			
			//Look for MF in mib format
			List<WebElement> mfMib = driver.findElements(By.xpath("//tr[contains(@id,'.DevId"+vcmfList.get(i)[1]+"')]"));
			if (mfMib.size()>0) {
				//If found, click on it
				action.click(mfMib.get(0));
				action.build().perform();
				//mfMib.get(0).click();
				Thread.sleep(500);
				
				//Wait for "Loading..." message
				navi.waitForElement("//div[contains(text(),'Loading')]", 2,0);
				
				//Get the display name for this metric and compare it with HR format from readme
				do {
					try {
						isDone=true;
						mfHR = driver.findElement(By.xpath("//tr[contains(@id,'.DevId"+vcmfList.get(i)[1]+"')]/td/span/span[3]/div")).getText();												
					} catch (Exception e) {
						isDone=false;
						log.warn("Unable to get metric family display name.");
						Thread.sleep(500);
					}
				} while (!isDone);
				
				if (mfHR.equals(vcmfList.get(i)[0])) {
					//Reporter.log("INFO: Found "+vcmfList.get(i)[1]+" ("+mfHR+") MF.");
				} else {
					log.error("Found "+vcmfList.get(i)[1]+" MF but the display name is "+mfHR+".");
				}
				
				//Search for the required VC element
				//Get the list of elements from table
				List<WebElement> elements = driver.findElements(By.xpath("//table[@class='dataTable']/tbody[@role='alert']/tr"));
				
				//Click on elements and check they VC
				String element_name = new String();
				for (int k=0;k<elements.size();k++) {
					
					//Scroll to element
					je.executeScript("arguments[0].scrollIntoView(true);",elements.get(k));
					//Click on element
					elements.get(k).click();
					Thread.sleep(200);
					
					//Get VC in mib format
					do {
						try {
							isDone=true;
							vcMib = driver.findElement(By.xpath("//input[@name='vendorCert']")).getAttribute("value");						
						} catch (Exception e) {
							isDone=false;
							log.warn("Unable to get VC mib name.");
							Thread.sleep(500);						
						}
					} while (!isDone);
					
					//Compare VC mib with required VC mib
					if (vcMib.equals(vcmfList.get(i)[3])) {
						//Get Label (element name)
						element_name = navi.getWebElement("//input[@name='Names']").getAttribute("value");
						log.info("Found \""+vcMib+"\" VC for \""+vcmfList.get(i)[0]+"\" MF with element: \""+element_name+"\"");
						isVcFound = true;
						break;
					} 
				}
				
				// If no required VC is found, just skip this cycle
				if (!isVcFound) {
					log.error("VC \""+vcmfList.get(i)[3]+"\" not found for \""+vcmfList.get(i)[0]+"\" MF");
					//navi.findDevice(simID);
					continue;
				}
				
		} else {
				//MF not found
				log.error("MF \""+vcmfList.get(i)[1]+"\" not found.");
			}
		}
		
	}
	
	
	public void createNewProfile (String ip, String sim) throws InterruptedException {
		
		Navigation navi = new Navigation (driver);
		
		WebElement profile = navi.getWebElement("//div[text()='Profiles']");
		profile.click();		
		
		WebElement newProfile = navi.getWebElement("//td/span[contains(@id,'snmpcollector:idsProbe.devices.category-context-menu')]");
		newProfile.click();
		
		newProfile = navi.getWebElement("//span[text()='Create New Profile']");
		newProfile.click();
		
		//Fill "Hostname or IP Address" field with simdepot IP
		WebElement inputIP = navi.getWebElement("//input[@name='hostnameOrIP']");
		inputIP.sendKeys(ip);
		
		//Fill "Description" field with simdepot IP
		WebElement description = navi.getWebElement("//input[@name='description']");
		description.sendKeys("Sim"+sim);
		
		//Fill "SNMP Port" field with "161"
		WebElement snmpport = navi.getWebElement("//input[@name='Port']");
		snmpport.sendKeys("161");
		
		//Change SNMP version to v2c
		WebElement snmpVer = navi.getWebElement("//span[@class='ddlabel' and text()='SNMP v1']");
		snmpVer.click();
		Thread.sleep(500);
		snmpVer = navi.getWebElement("//span[@class='ddlabel' and text()='SNMP v2c']");
		snmpVer.click();
		Thread.sleep(500);
		
		//Fill "Community String" field with "public"
		WebElement commString = navi.getWebElement("//form[@name='snmpv2c']/input[@for='communityString']");
		commString.sendKeys("public");
		
		//Click on "Submit" button
		WebElement submitButton = navi.getWebElement("//button[@id='actionSubmit']");
		submitButton.click();
		
		//Wait for "Create New Profile" to close
		navi.waitForElement("//span[text()='Create New Profile']", 2,0);
		Thread.sleep(100);
		
		//Get windows title
		String winTitle = navi.getWebElement("//span[@class='ui-dialog-title']").getText();
		
		if (winTitle.equals("Failure")) {
			String alertMessage = driver.findElement(By.xpath("//div[@class='alert-dialog-body']/p")).getText();
			log.error("Creating profile status: "+alertMessage);
		} else 
			if (winTitle.equals("Success")) {
				String alertMessage = driver.findElement(By.xpath("//div[@class='alert-dialog-body']/p")).getText();
				log.info("INFO: Creating profile status: "+alertMessage);
			}
						
		//Click on "Reload" button
		WebElement reloadButton = navi.getWebElement("//button[@name='reload']");
		reloadButton.click();
		Thread.sleep(200);
		
		//Wait for "Success" window to close
		//navi.waitForElement("//span[text()='Success']", 2);
		
		//Wait for "Loading..." message
		navi.waitForElement("//div[contains(text(),'Loading')]", 2,0);
		
		
		
				
	}
	

}
