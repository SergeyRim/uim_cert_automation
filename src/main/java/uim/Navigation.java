package uim;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.testng.annotations.Optional;

import java.io.File;
import java.util.List;

public class Navigation {

	private static final Logger log = LogManager.getLogger("Navigation");
	
	WebDriver driver;
	
	public Navigation (WebDriver driver) {
		this.driver = driver;
	}
	
	protected WebElement getWebElement (String xpath) throws InterruptedException {
		int timeout = 1000*120;
		int waiting = 0;
		
		while (driver.findElements(By.xpath(xpath)).size()<1) {
			Thread.sleep(500);
			waiting+=500;
			if (waiting>=timeout)
				break;
		}
		
		if (waiting>=timeout) {
			log.fatal("getWebElement: Timed out waiting for element "+xpath);
			return null;
		} else 
			return driver.findElement(By.xpath(xpath));		
	}
	
	protected Boolean waitForElement (String xpath, int type, int CustomTimeOut) throws InterruptedException {
		// int type
		// "1" = Wait for element which should BE PRESENT
		// "2" = Wait for element which should BE ABSENT
		
		int timeout;
		if (CustomTimeOut>0) {
			timeout=1000*CustomTimeOut;
		} else {
			timeout=1000*120;
		}

		int waiting = 0;
		
		if (type==1) {
			while (driver.findElements(By.xpath(xpath)).size()<1) {
				Thread.sleep(500);
				waiting+=500;
				if (waiting>=timeout)
					break;
			}
		}
		
		if (type==2) {
			while (driver.findElements(By.xpath(xpath)).size()>0) {
				Thread.sleep(500);
				waiting+=500;
				if (waiting>=timeout)
					break;
			}
		}
		
		if (waiting>=timeout) {
			log.fatal("waitForElement: Timed out waiting for element "+xpath);
			return null;
		} else 
			return true;		
	}
	
	public void loginToAdminConsole () {
		
		WebElement username = driver.findElement(By.id("usrname"));
		username.sendKeys("administrator");
		
		WebElement password = driver.findElement(By.id("pswd"));
		password.sendKeys("1QAZ2wsx");
		
		WebElement signInButton = driver.findElement(By.xpath("//input[@value='Sign In']"));
		signInButton.click();
		
	}
	
	public void findDevice (String sim) throws InterruptedException {
		
		//Wait while page will be loaded
		waitForElement("//div[text()='Profiles']", 1,0);
		
		//Click on Profile
		WebElement profile = getWebElement("//div[text()='Profiles']");
		profile.click();
		
		//Wait for "Loading..." message
		Thread.sleep(300);
		waitForElement("//div[contains(text(),'Loading')]", 2,0);
		
		//Get list of Devices (IPs)
		//List<WebElement> devices = driver.findElements(By.xpath("//div[text()='Profiles']/../../../../following-sibling::tr/td/span/span[3]/div"));
		List<WebElement> devices = driver.findElements(By.xpath("//tr[contains(@id,'.profile:Profile.')]/td/span/span/div"));
		
		
		for (int i=0; i<devices.size(); i++) {
			
			//Refresh webelement list b/c it will be outdated after the 1st clicking
			devices = driver.findElements(By.xpath("//tr[contains(@id,'.profile:Profile.')]/td/span/span/div"));
			
			//Get IP address
			String ip = devices.get(i).getText();
			
			//Click on device
			devices.get(i).click();
			
			//Wait for "Loading..." message
			Thread.sleep(300);
			waitForElement("//div[contains(text(),'Loading')]", 2,0);
			
			//Wait while page will be refreshed
			//waitForElement("//span[@class='mightOverflow rhsHeaderTitle' and text()='"+ipAddress+"']", 1);
												
			//Get device description
			WebElement description = getWebElement("//input[@name='description']");
			if (description.getAttribute("value").equals("Sim"+sim)) {
				WebElement openList = getWebElement("//tr[contains(@id,'.DevId')]/td/span/span/div[text()='"+ip+"']");
				openList.click();
				Thread.sleep(500);
				//Wait for "Loading..." message
				waitForElement("//div[contains(text(),'Loading')]", 2,0);
				break;
			}
				
		}

	}

	public void gotoArchive () throws InterruptedException {

		log.debug("Go to Archive.");
		WebElement navi = getWebElement("//input[@value='Archive']");
		navi.click();

	}

	public void gotoSNMPCollectorProbe (String uimServer) throws InterruptedException {

		log.debug("Select \"Robots\" tab.");
		getWebElement("//input[@id='btnRobotsTab']").click();
		Thread.sleep(500);

		log.debug("Click on \""+uimServer+"\" robot.");
		getWebElement("//td[@class='name' and text()='"+uimServer+"']").click();
		Thread.sleep(200);

		//getWebElement("//td[@title='"+uimServer+"' and @data-column='name']").click();
		//Thread.sleep(500);

		log.debug("Click on \"Probes\" tab.");
		getWebElement("//input[@id='btnProbesTab' and @title='Probes']").click();
		Thread.sleep(500);

		log.debug("Search for \"snmpcollector\" probe.");
		WebElement navi = getWebElement("//div[@id='tableActionsHeaderDiv']/input[@id='tableActionsHeaderFilterInput']");
		navi.sendKeys("snmpcollector");
		navi.sendKeys(Keys.ENTER);
		Thread.sleep(500);

	}

	public String callSNMPCollectorProbeConfiguration (String uimServer) throws InterruptedException {


		getWebElement("//input[@class='tableRowMenu outlineOnHoverFocus round']").click();
		Thread.sleep(500);
		
		String winHandleBefore = driver.getWindowHandle();
		
		getWebElement("//button[@class='menu-item-link configure-probe']").click();
		Thread.sleep(200);
		
		driver.close();
		
		for(String winHandle : driver.getWindowHandles()){
		    driver.switchTo().window(winHandle);
		}
		
		return winHandleBefore;

	}

	public void takeScreenshot (String fileName, String path) throws InterruptedException, Exception {

		File sourceFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		FileUtils.copyFile(sourceFile, new File (path+"\\"+fileName+".png"));

	}

}
