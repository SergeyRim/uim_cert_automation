package uim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


public class XLSXParser {

	private static final Logger log = LogManager.getLogger("XLSXParser");
	
	public ArrayList<ArrayList<String[]>> getMetrics(String excelFile) throws IOException {
				
		ArrayList<ArrayList<String[]>> metricsList = new ArrayList<>();
		
		// 	metricsList arrayList
		//
		//	[0]VC1_mib_name 	| [0]MF1_mib_name		| [0]metric1_name		| [0]metric2_name		| ...
		//  [1]VC1_display_name | [1]MF1_disaply_name	| [1]metric1_QOS_name	| [1]metric2_QOS_name	| ...

		//	[0]VC2_mib_name 	| [0]MF2_mib_name		| [0]metric1_name		| [0]metric2_name		| ...
		//  [1]VC2_display_name | [1]MF2_disaply_name	| [1]metric1_QOS_name	| [1]metric2_QOS_name	| ...
		
		//	...					| ...					| ...					| ...					| ...
		
		int row_number=0, cell_number=0, currentArrayNumber=0;
		boolean isAddedToArray;
				
		// Cell names
		// 1 - Vendor Cert (mib name)
		// 2 - Vendor Cert Name (Display anem)
		// 3 - Metric Family (mib name)
		// 4 - Metric Family Name (Display name)
		// 5 - New MF Introduced -Yes/No
		// 6 - New VC Introduced -Yes/No
		// 7 - VendorPriority
		// 8 - Metric
		// 9 - New Metrics - Yes / No
		// 10 - Expression
		// 11 - QOS Name
		// 12 - New OID  - Yes/No
		// 13 - Data Type
		// 14 - Rollup Strategy
		// 15 - Unit
		// 16 - Sim
		// 17 - Remarks 
		
		String prevVC = new String(), prevMF = new String();
		String[] currentRow = new String[18];  //Fill array starting with index 1 (to match indexes between spreadsheet and array)
		
		DataFormatter formatter = new DataFormatter();
		
		File myFile = new File(excelFile); 
		FileInputStream fis = new FileInputStream(myFile);

		// Finds the workbook instance for XLSX file 
		XSSFWorkbook myWorkBook = new XSSFWorkbook (fis);
		
		// Return first sheet from the XLSX workbook
		log.debug("Switching to \"Detail\" tab.");
		XSSFSheet mySheet = myWorkBook.getSheet("Detail");
			
		// Get iterator to all the rows in current sheet 
		Iterator<Row> rowIterator = mySheet.iterator();
		// Traversing over each row of XLSX file 

		//For getting value of cell instead of formula
		FormulaEvaluator evaluator = myWorkBook.getCreationHelper().createFormulaEvaluator();

		while (rowIterator.hasNext()) { 
			row_number++;

			Row row = rowIterator.next();
			if (row_number==1) {
				log.debug("Skip 1st (title) row.");
				continue;
			}
			
			// For each row, iterate through each columns 
			Iterator<Cell> cellIterator = row.cellIterator(); 
			cell_number=0;
			log.debug("Starting reading cells in row "+row_number);
			while (cellIterator.hasNext()) {
				cell_number++;
				Cell cell = cellIterator.next();

				if (cell_number>17) {
					log.warn("Skipping extra cell "+cell_number+" in row "+row_number);
					continue;
				}

				//Will change to calculate value instaed of formulas
				//currentRow[cell_number]=formatter.formatCellValue(cell);  ///Returns the formatted value of a cell as a String regardless of the cell type.

				if (cell.getCellTypeEnum() != CellType.BLANK) {
					if (cell.getCellTypeEnum() == CellType.NUMERIC) {
						log.debug("Cell type is NUMERIC");
						currentRow[cell_number]=formatter.formatCellValue(cell);
					} else {
						CellValue cellValue = evaluator.evaluate(cell);
						String curCell = cellValue.getStringValue();

						if (curCell.startsWith("QOS_")) {
							String oldCell=curCell;
							//curCell=curCell.replace("QOS_ ","QOS_");
							curCell=curCell.replace(" ","");
							//log.debug("Removed extra space in QOS metric name: \""+oldCell+"\" -> \""+curCell+"\"");
						}
						currentRow[cell_number]=curCell;
					}
				} else {
					log.debug("Empty cell detected at cell "+cell_number);
					currentRow[cell_number]="";
				}

//				switch (cellValue.getCellType()) {
//
//					case Cell.CELL_TYPE_STRING:
//						System.out.println(cellValue.getStringValue());
//						break;
//				}

				log.debug("Cell "+cell_number+" : "+currentRow[cell_number]);
			}
			
			//Skip if the metric is Names or Descriptions
			if (currentRow[8].equals("Names") || currentRow[8].equals("Descriptions") || currentRow[8].equals("Indexes") || currentRow[8].equals("OperStatus")) {
				log.debug("Skip metric "+currentRow[8]);
				continue;
			}

			if (!currentRow[11].startsWith("QOS_")) {
				log.error("Metric \""+currentRow[8]+"\" has wrong or empty QOS name: \""+currentRow[11]+"\"");
				continue;
			}
			
			//If current row has the same VC and MF, no need to create new arraylist, just add new metric to existing one
			if (currentRow[1].equals(prevVC) && currentRow[3].equals(prevMF)) {
				log.debug("Add \""+currentRow[8]+"\" ("+currentRow[11]+") to current arrayList "+currentArrayNumber);
				//Add metric to current array
				//Add metric (name and QOS_name)
				String[] tmpMetric = new String[2];
				tmpMetric[0] = currentRow[8];
				tmpMetric[1] = currentRow[11];
				metricsList.get(currentArrayNumber).add(tmpMetric);
				
			} else {
				
				isAddedToArray=false;
				//Search if current VC and MF already exists in arraylist, if exists, will find this array and add metric to it
				for (int i=0; i<metricsList.size(); i++) {
					if (metricsList.get(i).get(0)[0].equals(currentRow[1]) && metricsList.get(i).get(1)[0].equals(currentRow[3])) {
						log.debug("Succesfully found arrayList with VC:"+currentRow[1]+" MF:"+currentRow[3]+". Add \""+currentRow[8]+"\" ("+currentRow[11]+") to it.");
						//Add metric (name and QOS_name)
						String[] tmpMetric = new String[2];
						tmpMetric[0] = currentRow[8];
						tmpMetric[1] = currentRow[11];
						metricsList.get(i).add(tmpMetric);
						currentArrayNumber=i;
						prevVC = currentRow[1];
						prevMF = currentRow[3];
						isAddedToArray=true;
						break;
					}
				}
				
				//If metric was not added on steps before, need to create a new array
				if (isAddedToArray==false) {
					log.debug("Create new arrayList for VC:"+currentRow[1]+" MF:"+currentRow[3]+" and add \""+currentRow[8]+"\" ("+currentRow[11]+") to it.");
					//Create a new arrayList
					prevVC = currentRow[1];
					prevMF = currentRow[3];
					metricsList.add(new ArrayList<String[]>());
					currentArrayNumber=metricsList.size()-1;
					
					//Add VC (mib and display names)
					String[] tmpVC = new String[2];
					tmpVC[0] = currentRow[1];
					tmpVC[1] = currentRow[2];
					metricsList.get(currentArrayNumber).add(tmpVC);
					
					//Add MF (mib and display names)
					String[] tmpMF = new String[2];
					tmpMF[0] = currentRow[3];
					tmpMF[1] = currentRow[4];
					metricsList.get(currentArrayNumber).add(tmpMF);
					
					//Add metric (name and QOS_name)
					String[] tmpMetric = new String[2];
					tmpMetric[0] = currentRow[8];
					tmpMetric[1] = currentRow[11];
					metricsList.get(currentArrayNumber).add(tmpMetric);
				}
				
			}
			
		}
	
		myWorkBook.close();
		
		return metricsList;
		
	}
	
	
}
