package uim;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class TxtParser {

	private static final Logger log = LogManager.getLogger("TxtParser");

	public ArrayList<String[]> getMFVC (String readmeFileLocation){
			
	//File Format	
	//MF: Availability | NormalizedAvailabilityInfo
	//VC: System Statistics | systemMib
	
	ArrayList<String[]> vcmfList = new ArrayList<String[]>();
	
	try (BufferedReader reader = new BufferedReader(
		new InputStreamReader(
			new FileInputStream(readmeFileLocation), StandardCharsets.UTF_8))) {
            
			String lineMF;
			String lineVC;
            String[] parts1 = new String [2];
            String[] parts2 = new String [2];
            
            while ((lineMF = reader.readLine()) != null) {
            	
            	//Check that lineMF is not empty. If empty, read next line
            	while (lineMF.trim().isEmpty())
            		lineMF = reader.readLine();
            	
            	if (lineMF.contains("MF:")) {
            		//Read next NON-EMTPY line
            		do {
            			lineVC = reader.readLine();
            			
            		} while (lineVC.trim().isEmpty());
            		
            		//If lineVC is correct, parse 2 lines: lineMF and lineVC
            		if (lineVC.contains("VC:")) {
            			parts1 = lineMF.split("MF:");
                    	parts2 = parts1[1].split("\\|");
                    	String[] vcmf = new String[4];
                    	//MF in HR format
                    	vcmf[0]=parts2[0].trim();
						log.debug("MF in HR format is \""+vcmf[0]+"\"");
                    	//MF in mib format
                    	vcmf[1]=parts2[1].trim();
                    	log.debug("MF in mib format is \""+vcmf[1]+"\"");
                    	
                    	parts1 = lineVC.split("VC:");
                    	parts2 = parts1[1].split("\\|");
                    	//VC in HR format
                    	vcmf[2]=parts2[0].trim();
                    	log.debug("VC in HR format is \""+vcmf[2]+"\"");
                    	//VC in mib format
                    	vcmf[3]=parts2[1].trim();
                    	log.debug("VC in mib format is \""+vcmf[3]+"\"");

                    	if (vcmf[0].equals("") || vcmf[1].equals("") || vcmf[2].equals("") || vcmf[3].equals("")) {
                    		throw new IOException("One of the parsed value is empty.");
						}

                    	vcmfList.add(vcmf);
            		}
            		
            	}
            }
        } catch (IOException e) {
            log.error("Error parsing Already Certified MF/VC file.");
            log.error(e.toString());
            return null;
        }
		
	return vcmfList;
	}

}
