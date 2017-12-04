package uim;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.TestListenerAdapter;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ScreenshotListener extends TestListenerAdapter {
    @Override
    public void onTestFailure(ITestResult result) {
        //Make a screenshot on failure

        //Get driver from testclass TestCase2
        Object currentClass = result.getInstance();
        WebDriver driver = ((TestCase1) currentClass).getDriver();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formater = new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss");
        String methodName = result.getName();
        String imgPath = System.getProperty("user.dir")+File.separator+formater.format(calendar.getTime())+".png";
        if(!result.isSuccess()){
            try {
                File sourceFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
                FileUtils.copyFile(sourceFile, new File (imgPath));

                //BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                //ImageIO.write(image, "png", new File(imgPath));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Reporter.log("ERR: Error in method \""+methodName+"\". Screenshot saved under "+imgPath);
        }

    }

}
