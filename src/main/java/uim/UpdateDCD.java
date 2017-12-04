package uim;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;



public class UpdateDCD {
    private static final Logger log = LogManager.getLogger("Navigation");

    WebDriver driver;

    public UpdateDCD (WebDriver driver) {
        this.driver = driver;
    }

    public static class CustomAuthenticator extends Authenticator {
        protected PasswordAuthentication getPasswordAuthentication() {
            String username = "rimse01";
            String password = "6MiniBar34";
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }


    public String downloadDCDFromTeamCity () throws Exception {

        Navigation navi = new Navigation (driver);

        //Login to teamcity
        log.info("Loggin in to TeamCity.");
        driver.get("http://build.dev.fco/teamcity/viewType.html?buildTypeId=Uim_Snmpc_SnmpDeviceCertificationDeployer_SnmpDeviceCertificationDeployer");
        driver.findElement(By.id("username")).sendKeys("rimse01");
        driver.findElement(By.id("password")).sendKeys("6MiniBar36");
        driver.findElement(By.xpath("//input[@class='btn loginButton']")).click();

        log.debug("Wait while page will be loaded.");
        navi.waitForElement("//a[text()='SNMP Device-Certification-Deployer']",1,0);

        log.debug("Click to see build artifact.");
        driver.findElement(By.xpath("//a[@title='View build artifacts']/../../span[2]/span")).click();

        log.debug("Wait while download DCD link will appear.");
        navi.waitForElement("//a[text()='Device_Certification_Deployer.zip']",1,0);

        log.debug("Getting dowload URL.");
        String url = driver.findElement(By.xpath("//a[text()='Device_Certification_Deployer.zip']")).getAttribute("href");
        log.info("Download URL is "+url);

        String dcdDownloadedPath = System.getProperty("user.dir")+File.separator+"Device_Certification_Deployer.zip";

        //NOT SUITABLE FOR REMOTE WEBDRIVER
        log.debug("Check if DCD archive already exists in "+System.getProperty("user.dir"));
        File dcdFile = new File (dcdDownloadedPath);
        if (dcdFile.exists()) {
            log.info("Removing existing DCD archive.");
            try {
                dcdFile.delete();
                log.info("File "+dcdDownloadedPath+" succesfully removed.");
            }
            catch (Exception e) {
                log.error("Unable to delete file. "+e.toString());
            }
        }

        log.info("Copying DCD archive to "+dcdDownloadedPath);
        //log.info("Copying DCD archive to temp folder.");
        driver.findElement(By.xpath("//a[text()='Device_Certification_Deployer.zip']")).click();
        //Thread.sleep(15000);


        //NOT SUITABLE FOR REMOTE WEBDRIVER
        //Wait while file is downloading by compare it's size
        while (dcdFile.length()<4000) {
            log.debug("File size lenght is too small. Sleepeing for 1 sec.");
            Thread.sleep(1000);
        }

        // DIRECT DOWNLOAD PROCEDURE. DOES NOT FIT TO REMOTE WEBDRIVER
        //File dcdAcrhive = new File(dcdDownloadedPath);
        //URL downloadURL = new URL(url);
        //Use Authenticator to authenticate user
        //Authenticator.setDefault(new CustomAuthenticator());
        //FileUtils.copyURLToFile(downloadURL, dcdAcrhive);

        log.info("Download completed.");

        return dcdDownloadedPath;

    }


    public void uploadAndDeployDCD (String dcdPath) throws InterruptedException {

        Navigation navi = new Navigation (driver);

        //NOT SUITABLE FOR REMOTE WEBDRIVER
        log.debug("Check if provided DCD file exists.");
        File dcdFile = new File (dcdPath);
        if (!dcdFile.exists()) {
            log.error("DCD archive "+dcdPath+ "does not exists.");
            Assert.assertTrue(false);
        }


        log.info("Will remove existing DCD packages first.");
        log.debug("Filter Out Packages with \"Device_Certification_Deployer\"");
        WebElement currentElement = navi.getWebElement("//div[@id='tableActionsHeaderDiv']/input[@id='tableActionsHeaderFilterInput']");
        currentElement.click();
        currentElement.sendKeys("Device_Certification_Deployer");
        currentElement.click();
        Thread.sleep(1000);

        log.debug("Click on Select All checkbox.");
        navi.getWebElement("//input[@class='select-all-checkbox']").click();

        log.debug("Click on Actions button.");
        navi.getWebElement("//button[@id='hubSectionActionButton' or @id='hubSectionManagerActionsButton']").click();

        log.debug("Wait while Action menu will appears.");
        navi.waitForElement("//button[contains(@class,'menu-item-link delete-package')]",1,0);

        //Check if Delete button disabled
        if (driver.findElement(By.xpath("//button[contains(@class,'menu-item-link delete-package')]")).getAttribute("disabled")!=null){
            log.info("Delete option is disabled. There are no packages to remove.");
            //Click on Actions button to close action menu
            navi.getWebElement("//button[@id='hubSectionActionButton' or @id='hubSectionManagerActionsButton']").click();
        } else {
            //Click on Delete option
            driver.findElement(By.xpath("//button[@class='menu-item-link delete-package']")).click();
            //Clicmk on Yes button -> Confirm delete
            navi.getWebElement("//button[@id='yesBtn']").click();
            Thread.sleep(2000);
            log.info("Old DCD packages were succesfully removed.");
        }

        log.info("Now will upload new DCD package from "+dcdPath);
        //Click on Actions button
        navi.getWebElement("//button[@id='hubSectionActionButton' or @id='hubSectionManagerActionsButton']").click();

        //Click on Import Packages menu item
        navi.getWebElement("//button[@class='menu-item-link import-package']").click();

        //Send upload link (we don't need to click on Browse button, just send a full path to uploaded file to <input type="file"> input element.
        navi.getWebElement("//input[@type='file']").sendKeys(dcdPath);

        navi.getWebElement("//button[@id='yesBtn']").click();

        log.info("Waiting while DCD package is uploading...");
        Assert.assertTrue(navi.waitForElement("//div[@class='loading image']",2,60));
        log.info("DCD succesfully uploaded.");
        Thread.sleep (500);

        log.debug("Check DCD checkbox.");
        navi.getWebElement("//input[@class='checkBox' and contains(@data-id,'Device_Certification_Deployer')]").click();

        log.debug("Click on | menu.");
        navi.getWebElement("//input[@class='tableRowMenu outlineOnHoverFocus round']").click();

        log.debug("Select deploy menu.");
        navi.getWebElement("//button[@class='menu-item-link deploy-package']").click();
        Thread.sleep(1000);

        log.debug("Select hub to deploy.");
        navi.getWebElement("//input[@class='checkBox']").click();

        log.debug("Wait while hub to deploy will appears.");
        navi.waitForElement("//td[contains(@title,'_hub') and @data-column='hubName']",1,0);

        log.debug("Click on Deploy button.");
        log.info("Deploying DCD. Please wait...");
        navi.getWebElement("//input[@class='save-button']").click();

        navi.waitForElement("//div[text()='Succeeded']",1,300);
        log.info("DCD has been succesfully deployed.");

    }

    public void restartSnmpProbe (String uimServer) throws InterruptedException {

        Navigation navi = new Navigation (driver);

        navi.gotoSNMPCollectorProbe(uimServer);

        log.debug("Execute SNMPcollector menu page.");
        navi.getWebElement("//input[@class='tableRowMenu outlineOnHoverFocus round']").click();
        Thread.sleep(500);

        log.info("Deactivating \"snmpcollector\" probe. Please wait.");
        navi.getWebElement("//span[text()='Deactivate']").click();

        navi.waitForElement("//td[@class='status hub-status-gray' and @title='Deactivated']",1, 0);
        log.info("snmpcollector probe deactivated. Starting probe...");

        log.debug("Execute SNMPcollector menu page.");
        navi.getWebElement("//input[@class='tableRowMenu outlineOnHoverFocus round']").click();
        Thread.sleep(500);
        navi.getWebElement("//span[text()='Activate']").click();

        navi.waitForElement("//td[@class='status hub-status-green' and @title='Running']",1, 0);
        log.info("snmpcollector probe succesfully activated.");

    }
}
