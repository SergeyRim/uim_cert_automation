package uim;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;


public class Simdepot {

	private static final Logger log = LogManager.getLogger("Simdepot");
	WebDriver driver;

	public Simdepot (WebDriver driver) {
		this.driver = driver;
	}

	public ArrayList<String> getSimIP (String[] simIDs) throws InterruptedException, IOException {

		ArrayList<String> simIPs = new ArrayList();
		Navigation navi = new Navigation(driver);

		for (int i=0; i<simIDs.length; i++) {

			String[] sim_zone_ip = simIDs[i].split(":");

			//Check if simID parameter has a proper format: "public:ID" or "portsmouth:ID"
			if (sim_zone_ip.length!=2 || (!sim_zone_ip[0].toLowerCase().equals("public") && !sim_zone_ip[0].toLowerCase().equals("portsmouth")) ) {
				log.fatal("Wrong format for simID: \""+simIDs[i]+"\".");
				return null;
			}

			log.info("Getting IP for simID \""+sim_zone_ip[1]+ "\" in \""+sim_zone_ip[0]+ "\" zone.");
			String simIP = "Not Initialized";
			Boolean isGotStatus, isClicked;
			Integer startCount=0;
			driver.switchTo().frame("control");
			WebElement searchField = driver.findElement(By.id("simidIn"));
			searchField.clear();
			searchField.sendKeys(sim_zone_ip[1]);
			searchField.sendKeys(Keys.ENTER);

			driver.switchTo().defaultContent();
			driver.switchTo().frame("present_selection");

			WebElement clickOnSim = driver.findElement(By.partialLinkText(sim_zone_ip[1]));
			clickOnSim.click();

			//Wait while Public tab will be loaded
			while (driver.findElements(By.xpath("//div[@id='loadedsimsWaiting' and @style='display: none;']")).size()<1)
				Thread.sleep(1000);

			if (sim_zone_ip[0].toLowerCase().equals("portsmouth")) {
				log.info("Switching to Portsmouth tab.");
				WebElement portsmouthTab = navi.getWebElement("//span[text()='Portsmouth']");
				portsmouthTab.click();

				//Wait while Portsmouth tab will be switched
				while (driver.findElements(By.xpath("//div[@id='loadedsimsWaiting' and @style='display: none;']")).size()<1)
					Thread.sleep(1000);
			}
			log.info("Getting current simID state.");


			//Wait for loading page (by checking sim status string length>1)   OBSOLET METHOD. CAN BE REMOVED
			int statusStringLength = 0;
			while (statusStringLength < 1)
			{
				do {
					try {
						isGotStatus=true;
						statusStringLength = driver.findElement(By.xpath("//td[@id='status']")).getText().length();

					} catch (Exception e) {
						isGotStatus=false;
						Thread.sleep(100);
						log.warn("Unable to get simID status string length. Retrying.");
					}
				} while (!isGotStatus);
			}

			//Get current sim status
			String simIDStatus=getSimState();

			if (simIDStatus.equals("Unknown")) {
				//SimID is down
				log.error("SimID status is Unknown. SimID is down. Exiting.");
				continue;

			}

			if (simIDStatus.equals("Stopped")) {
				log.info("SimID is stopped. Starting.");

				startSimId();
				startCount=1;
				Thread.sleep(3000);

				while (!simIDStatus.equals("Running") && startCount<=3 && !simIDStatus.equals("Failed")) {
					//Get simID State
					simIDStatus = getSimState();
					if (simIDStatus.equals("Stopped")) {
						log.warn("SimID state became Stopped unexpectedly. Starting again.");
						startCount++;
						startSimId();
					}
				}

				if (startCount>3) {
					log.error("Failed to start simID. Sim status became stopped unexpectedly after 4 retries. Exiting.");
					return null;
				}

				if (simIDStatus.equals("Failed")) {
					log.error("Failed to start simID. Sim status became 'Failed'. Exiting.");
					return null;
				}

				log.info("SimID started succesfully.");
			}


			do {
				try {
					isGotStatus=true;
					simIDStatus = driver.findElement(By.xpath("//td[@id='status']")).getText();

				} catch (Exception e) {
					isGotStatus=false;
					Thread.sleep(500);
					log.warn("Unable to get simID status. Retrying.");
				}
			} while (!isGotStatus);

			if (simIDStatus.equals("Running")) {
				simIP = driver.findElement(By.xpath("//td[@id='address']")).getText();
				log.info("SimID IP is "+simIP);
			}

			//Check if sysdescription match SimID name
			checkSysDescr(simIP, sim_zone_ip[1]);

			simIPs.add(simIP);
			driver.switchTo().defaultContent();
		}

		return simIPs;
	}

	private boolean startSimId () throws InterruptedException {
		Boolean isClicked;
		do {
			try {
				isClicked = true;
				WebElement startStopButton = driver.findElement(By.xpath("//input[@id='start' and @class='agentButton']"));
				startStopButton.click();
			} catch (Exception e) {
				isClicked=false;
				Thread.sleep(500);
				log.warn("Unable to click on 'Start' button. Retrying.");
			}
		} while (!isClicked);
		return true;
	}


	private String getSimState () throws InterruptedException {
		String simIDStatus="Unknown";
		Boolean isGotStatus;
		do {
			try {
				simIDStatus = driver.findElement(By.xpath("//td[@id='status']")).getText();
				if (simIDStatus.length() > 1)
					isGotStatus=true;
				else isGotStatus=false;
				Thread.sleep(500);

			} catch (Exception e) {
				isGotStatus=false;
				Thread.sleep(500);
				log.warn("Unable to get simID status. Retrying.");
			}
		} while (!isGotStatus);

		return simIDStatus;
	}


	private boolean checkSysDescr (String simIP, String simID) throws IOException {

		log.info("Checking sysName for IP "+simIP);
		String sysName=null;

		log.debug("Create TransportMapping and Listen.");
		TransportMapping transport = new DefaultUdpTransportMapping();
		transport.listen();

		log.debug("Create Target Address object.");
		CommunityTarget comtarget = new CommunityTarget();
		comtarget.setCommunity(new OctetString("public"));
		comtarget.setVersion(SnmpConstants.version2c);
		comtarget.setAddress(new UdpAddress(simIP + "/161"));
		comtarget.setRetries(2);
		comtarget.setTimeout(1000);

		log.debug("Create the PDU object.");
		PDU pdu = new PDU();
		pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.5.0")));
		pdu.setType(PDU.GET);
		pdu.setRequestID(new Integer32(1));

		log.debug("Create Snmp object for sending data to Agent.");
		Snmp snmp = new Snmp(transport);

		log.debug("Sending Request to Agent...");
		ResponseEvent response = snmp.get(pdu, comtarget);

		log.debug("Process Agent Response.");
		if (response != null)
		{
			log.debug("Got Response from Agent");
			PDU responsePDU = response.getResponse();

			if (responsePDU != null)
			{
				int errorStatus = responsePDU.getErrorStatus();
				int errorIndex = responsePDU.getErrorIndex();
				String errorStatusText = responsePDU.getErrorStatusText();

				if (errorStatus == PDU.noError)
				{
					log.info("SNMP GET Response " + responsePDU.getVariableBindings());
					sysName = responsePDU.getVariableBindings().firstElement().toString().split(" = ")[1];
					if (sysName.contains("Sim"+simID)) {
						log.info("SysName matches current simID.");
					} else {
						log.error("SysName DOES NOT match simID! Please check that proper IP is assigned.");
					}

				}
				else
				{
					log.error("Error: Request Failed.");
					log.error("Error Status = " + errorStatus);
					log.error("Error Index = " + errorIndex);
					log.error("Error Status Text = " + errorStatusText);
				}
			}
			else
			{
				log.error("Error: Response PDU is null");
			}
		}
		else
		{
			log.warn("Agent Timeout... ");
		}
		snmp.close();

		return true;
	}

}