package uim;

import java.io.IOException;
import java.util.List;

import org.testng.TestNG;
import org.testng.collections.Lists;

public class Runner {
	
	public static void main(String[] args) throws IOException {

	    if(args.length != 1) {
	        System.out.println("Usage : <xmlFile>");
	        System.exit(-1);
	    }

	    //We create a testNG object to run the test specified in the xml file
	    TestNG testng = new TestNG();

	    List<String> suites = Lists.newArrayList();
	    suites.add(args[0]);

	    testng.setTestSuites(suites);

	    testng.run();

	}

}
