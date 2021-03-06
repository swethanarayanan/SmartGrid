import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import de.progra.charting.model.DefaultChartDataModel;
import de.progra.charting.ChartEncoder;
import de.progra.charting.DefaultChart;
import de.progra.charting.render.LineChartRenderer;

/**
 * Acronyms Used in this code
 * TPC : TPC : Total Power Consumption
 * TPCS : Total Power Consumption of System
 * TPCN : Total Power Consumption of Node
 * TPCN1 : Total Power Consumption of Node 1
 * TPCN2 : Total Power Consumption of Node 2
 * TPCN3 : Total Power Consumption of Node 3
 * PAR : Peak to Average
 */

/**
 * This class implements the client and server codes for a single smart node in a smart grid system
 * @author swethanarayanan and abhishekravi
 *
 */
public class SmartNode{

	//no of times a node becomes a client
	static int noOfIterations = 0;	
	// Node 1 has 63 possible flexible appliance power profile configurations
	// Node 2 has 72 possible flexible appliance power profile configurations
	// Node 3 has 63 possible flexible appliance power profile configurations
	//maximum no of times a node becomes a client : calculated to be 63 * 72 * 63 = 285768 

	static int maxNoOfIterations = 100; 
	//No of nodes in this prototype system has been fixed to be 3
	static int noOfNodes = 3;
	static HashMap<Integer,String> ipAddressList = new HashMap<Integer,String>();
	static boolean clientModeEnabled= false;
	static String bestTPCS="";
	static int currentNode;
	static int objective;
	static double bestVar,bestPAR;
	static ArrayList<String> Constraints; 
	static int node1finished, node2finished, node3finished = 0;
	static int node1active = 1, node2active =1 , node3active = 1;
	static double MINIMUM_DIFFERENCE = 0.001;
	static String FILE_ERR_MSG= "Error in File";
	static Boolean stop = false;
	static int converging_iterations = 0;
	static int MAX_CONVERGING_ITERATIONS = 1000;
	static double[] appliancePowerConsumption = null ;
	static int[][] appliancePowerProfile = {
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1},
		{1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1},
		{0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0},
		{0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
		{0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1},
		{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
		{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
	};

	public static void main(String[] args) throws Exception {

		/**
		 * @TODO Handle Exceptions
		 */
		ipAddressList.put(1,"127.0.0.1 6787");
		ipAddressList.put(2,"127.0.0.1 6788");
		ipAddressList.put(3,"127.0.0.1 6789");		
		Scanner scanner = new Scanner (System.in);
		System.out.println("\n Choose node: \n 1. Node 1 \n 2. Node 2 \n 3. Node 3");
		currentNode = scanner.nextInt();
		scanner = new Scanner (System.in);
		System.out.println("\n Choose objective: \n 1. Minimum PAR \n 2. Minimum Variance");
		objective = scanner.nextInt();
		scanner.close();
		initializePowerData(currentNode);
		if(currentNode == 1)
			clientModeEnabled= true;
		runServer(currentNode);	
	}

	/**
	 * This method finds the adjusted power profile based on the objective specified
	 */
	private static void adjustPowerProfile() {

		try
		{
			writeTPCSToFile();
			if(objective==1)
			{
				minimizePAR();
			}
			else
			{	
				minimizeVariance();
			}
		}
		catch(Exception e)
		{
			printExceptionMessage();
		}
	}

	/**
	 * This method minimizes variance of the system. 
	 * We iterate through all possible appliance power profiles(i.e extensive search) and find the corresponding power consumption of node
	 * Then we find the total power consumption of the system and find the current variance.
	 * If the current variance is lesser than the best variance obtained so far, then the best variance becomes current variance
	 */
	private static void minimizeVariance(){

		try
		{
			double[] currentBestTPCS =  new double[24];
			double[] currentBestTPCN1 = new double[24];
			double[] currentBestTPCN2 = new double[24];
			double[] currentBestTPCN3 = new double[24];

			int duration1 = Integer.parseInt(Constraints.get(0).split(" ")[0]);
			int start1 = Integer.parseInt(Constraints.get(0).split(" ")[1]);
			int end1 = Integer.parseInt(Constraints.get(0).split(" ")[2]);

			int duration2 = Integer.parseInt(Constraints.get(1).split(" ")[0]);
			int start2 = Integer.parseInt(Constraints.get(1).split(" ")[1]);
			int end2 = Integer.parseInt(Constraints.get(1).split(" ")[2]);

			int iter1 = getTotalIterations(duration1,start1,end1);
			int iter2 = getTotalIterations(duration2,start2,end2);
			int total_iterations = iter1*iter2;

			double[] bestTPCSArray;
			if(!bestTPCS.isEmpty())
				bestTPCSArray = getTPCSfromString(bestTPCS);
			else
				bestTPCSArray = new double[24];

			double[] TPCS  = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCS.txt");
			double[] TPCN1 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_1.txt");
			double[] TPCN2 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_2.txt");
			double[] TPCN3 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_3.txt");

			double variance = Double.MAX_VALUE;
			//Extensive search

			int [][] selectedPowerProfile =appliancePowerProfile.clone();
			for (int l = 0; l < selectedPowerProfile.length; l++) 
			{
				selectedPowerProfile[l] = appliancePowerProfile[l].clone();
			}
			for(int l=0; l<24;l++)
			{
				selectedPowerProfile[9][l]=0;
				selectedPowerProfile[10][l]=0;
			}
			for(int i=0;i<iter1;i++)
			{
				int [][] tempPowerProfile =appliancePowerProfile.clone();
				for (int l1 = 0; l1 < tempPowerProfile.length; l1++) 
				{
					tempPowerProfile[l1] = appliancePowerProfile[l1].clone();
				}
				for(int l=0; l<24;l++)
				{
					tempPowerProfile[9][l]=0;
					tempPowerProfile[10][l]=0;
				}
				for(int k=0;k<duration1;k++)
				{
					tempPowerProfile[9][(start1+i+k)%24] = 1;
				}
				for(int j=0;j<iter2;j++)
				{
					for(int l=0; l<24;l++)
					{
						tempPowerProfile[10][l]=0;
					}
					for(int k1=0;k1<duration2;k1++)
						tempPowerProfile[10][(start2+j+k1)%24] = 1;

					double [] TPCN = calculateNodePowerConsumption(appliancePowerConsumption, tempPowerProfile);

					if(currentNode==1)
						TPCN1 = TPCN.clone();
					else if(currentNode == 2)
						TPCN2 = TPCN.clone();
					else if(currentNode == 3)
						TPCN3 = TPCN.clone();

					for(int m=0;m<24;m++)
					{			
						TPCS[m] = 0;
						if(m<TPCN1.length)
						TPCS[m] += TPCN1[m] ;
						if(m<TPCN2.length)
						TPCS[m] +=	TPCN2[m] ;
						if(m<TPCN3.length)
						TPCS[m] +=	TPCN3[m] ;
					}
					double avg = getAvgPCS(TPCS);
					double currentvar = getVariance(TPCS, avg);
					if(currentvar < variance)
					{
						for (int n = 0; n < selectedPowerProfile.length; n++) 
						{
							selectedPowerProfile[n] = tempPowerProfile[n].clone();
						}
						variance = getVariance(TPCS, avg);
						currentBestTPCS =  TPCS.clone();
						currentBestTPCN1 = TPCN1.clone();
						currentBestTPCN2 = TPCN2.clone();
						currentBestTPCN3 = TPCN3.clone();

					}
				}

			}

			if(!bestTPCS.isEmpty())
			{
				bestVar = getVariance(bestTPCSArray,getAvgPCS(bestTPCSArray));
				if(Math.abs(bestVar-variance)<MINIMUM_DIFFERENCE)
				{
					converging_iterations++;
					if(converging_iterations>=MAX_CONVERGING_ITERATIONS)
					{	
						stop = true;
						//TODO : Communicate to network to stop
					}
				}
				else
				{
					converging_iterations=0;
				}

				if(variance < bestVar)
				{
					for(int k =0;k < 24;k++)
					{
						appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
						appliancePowerProfile[10][k] = selectedPowerProfile[10][k];
					}
					bestTPCS = getStringFromTPCS(currentBestTPCS);
					bestVar =  variance;

					if(currentNode==1)
						writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_1.txt", currentBestTPCN1);
					else if(currentNode == 2)
						writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_2.txt", currentBestTPCN2);
					else if(currentNode == 3)
						writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_3.txt", currentBestTPCN3);

					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCS.txt", currentBestTPCS);
				}
			}
			else
			{
				for(int k =0;k < 24;k++)
				{
					appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
					appliancePowerProfile[10][k] = selectedPowerProfile[10][k];
				}
				bestTPCS = getStringFromTPCS(currentBestTPCS);
				bestVar =  variance;
				if(currentNode==1)
					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_1.txt", currentBestTPCN1);
				else if(currentNode == 2)
					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_2.txt", currentBestTPCN2);
				else if(currentNode == 3)
					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_3.txt", currentBestTPCN3);

				writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCS.txt", currentBestTPCS);

			}
		}
		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}
	}

	/**
	 * This method minimizes PAR of the system. 
	 * We iterate through all possible appliance power profiles(i.e extensive search) and find the corresponding power consumption of node
	 * Then we find the total power consumption of the system and find the current variance.
	 * If the current PAR is lesser than the best PAR obtained so far, then the best PAR becomes current PAR
	 */
	private static void minimizePAR(){

		try
		{
			double[] currentBestTPCS =  new double[24];
			double[] currentBestTPCN1 = new double[24];
			double[] currentBestTPCN2 = new double[24];
			double[] currentBestTPCN3 = new double[24];

			int duration1 = Integer.parseInt(Constraints.get(0).split(" ")[0]);
			int start1 = Integer.parseInt(Constraints.get(0).split(" ")[1]);
			int end1 = Integer.parseInt(Constraints.get(0).split(" ")[2]);

			int duration2 = Integer.parseInt(Constraints.get(1).split(" ")[0]);
			int start2 = Integer.parseInt(Constraints.get(1).split(" ")[1]);
			int end2 = Integer.parseInt(Constraints.get(1).split(" ")[2]);

			int iter1 = getTotalIterations(duration1,start1,end1);
			int iter2 = getTotalIterations(duration2,start2,end2);
			int total_iterations = iter1*iter2;

			double[] bestTPCSArray;
			if(!bestTPCS.isEmpty())
				bestTPCSArray = getTPCSfromString(bestTPCS);
			else
				bestTPCSArray = new double[24];

			double[] TPCS  = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCS.txt");
			double[] TPCN1 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_1.txt");
			double[] TPCN2 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_2.txt");
			double[] TPCN3 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_3.txt");

			double peak = Double.MAX_VALUE;
			double PAR = Double.MAX_VALUE;
			double avg;
			//Extensive search

			int [][] selectedPowerProfile =appliancePowerProfile.clone();
			for (int l = 0; l < selectedPowerProfile.length; l++) 
			{
				selectedPowerProfile[l] = appliancePowerProfile[l].clone();
			}
			for(int l=0; l<24;l++)
			{
				selectedPowerProfile[9][l]=0;
				selectedPowerProfile[10][l]=0;
			}
			for(int i=0;i<iter1;i++)
			{
				int [][] tempPowerProfile =appliancePowerProfile.clone();
				for (int l1 = 0; l1 < tempPowerProfile.length; l1++) 
				{
					tempPowerProfile[l1] = appliancePowerProfile[l1].clone();
				}
				for(int l=0; l<24;l++)
				{
					tempPowerProfile[9][l]=0;
					tempPowerProfile[10][l]=0;
				}
				for(int k=0;k<duration1;k++)
				{
					tempPowerProfile[9][(start1+i+k)%24] = 1;
				}
				for(int j=0;j<iter2;j++)
				{
					for(int l=0; l<24;l++)
					{
						tempPowerProfile[10][l]=0;
					}
					for(int k1=0;k1<duration2;k1++)
						tempPowerProfile[10][(start2+j+k1)%24] = 1;
					double [] TPCN = calculateNodePowerConsumption(appliancePowerConsumption, tempPowerProfile);


					if(currentNode==1)
						TPCN1 = TPCN.clone();
					else if(currentNode == 2)
						TPCN2 = TPCN.clone();
					else if(currentNode == 3)
						TPCN3 = TPCN.clone();

					for(int m=0;m<24;m++)
					{			
						TPCS[m] = 0;
						if(m<TPCN1.length)
						TPCS[m] += TPCN1[m] ;
						if(m<TPCN2.length)
						TPCS[m] +=	TPCN2[m] ;
						if(m<TPCN3.length)
						TPCS[m] +=	TPCN3[m] ;
					}

					double largestValue = getLargestValue(TPCS); 
					//System.out.println("largestValue is"+ largestValue);

					if(largestValue<peak)
					{
						for (int n = 0; n < selectedPowerProfile.length; n++) 
						{
							selectedPowerProfile[n] = tempPowerProfile[n].clone();
						}
						peak = getLargestValue(TPCS);
						avg = getAvgPCS(TPCS);
						PAR = peak / avg ;
						currentBestTPCS =  TPCS.clone();
						currentBestTPCN1 = TPCN1.clone();
						currentBestTPCN2 = TPCN2.clone();
						currentBestTPCN3 = TPCN3.clone();
					}
				}

			}

			if(!bestTPCS.isEmpty())
			{

				bestPAR = getLargestValue(bestTPCSArray)/getAvgPCS(bestTPCSArray);


				if(Math.abs(PAR - bestPAR)<MINIMUM_DIFFERENCE)
				{
					converging_iterations++;
					if(converging_iterations>=MAX_CONVERGING_ITERATIONS)
					{	
						stop = true;
						System.out.println("Output converged!");
						printResult();
						//TODO : Communicate to network to stop
					}
				}
				else
				{
					converging_iterations=0;
				}


				if(PAR < bestPAR)
				{
					for(int k =0;k < 24;k++)
					{
						appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
						appliancePowerProfile[10][k] = selectedPowerProfile[10][k];
					}
					bestTPCS = getStringFromTPCS(currentBestTPCS);
					bestPAR = PAR;


					if(currentNode==1)
						writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_1.txt", currentBestTPCN1);
					else if(currentNode == 2)
						writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_2.txt", currentBestTPCN2);
					else if(currentNode == 3)
						writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_3.txt", currentBestTPCN3);

					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCS.txt", currentBestTPCS);
				}
			}
			else
			{
				for(int k =0;k < 24;k++)
				{
					appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
					appliancePowerProfile[10][k] = selectedPowerProfile[10][k];
				}
				bestTPCS = getStringFromTPCS(currentBestTPCS);

				bestPAR = PAR;

				if(currentNode==1)
					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_1.txt", currentBestTPCN1);
				else if(currentNode == 2)
					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_2.txt", currentBestTPCN2);
				else if(currentNode == 3)
					writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_3.txt", currentBestTPCN3);

				writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCS.txt", currentBestTPCS);

			}
		}
		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}

	}

	/**
	 * This method writes TPCS to TPCS.txt file
	 */
	private static void writeTPCSToFile() 
	{
		try
		{
			double[] currentTPCS= new double[24];
			double[] TPCN1 = readFileIntoDoubleArray("TPCN_1.txt");
			double[] TPCN2 = readFileIntoDoubleArray("TPCN_2.txt");
			double[] TPCN3 = readFileIntoDoubleArray("TPCN_3.txt");

			for(int i=0;i<24;i++)
			{			
				currentTPCS[i]=0;
				if(i<TPCN1.length)
				currentTPCS[i] += TPCN1[i] ;
				if(i<TPCN2.length)
				currentTPCS[i] += TPCN2[i] ;
				if(i<TPCN3.length)
				currentTPCS[i] += TPCN3[i] ;
			}
			writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCS.txt",currentTPCS);
		}

		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}
	}

	/**
	 * @return This method returns a string of 24 TPCS values separated by space from TPCS array of 24 numbers
	 */
	private static String getStringFromTPCS(double[] TPCS) {

		String bestTPCS = "";
		for(int i=0;i<24;i++)
		{
			bestTPCS += String.valueOf(TPCS[i])+" ";
		}

		return bestTPCS;
	}

	/**
	 * @return This method returns a TPCS array of 24 numbers from a string of 24 TPCS values separated by space
	 * @throws Exception
	 */
	private static double[] getTPCSfromString(String bestTPCS) {

		double[] value = new double[24];
		for(int i=0;i<24;i++)
		{
			value[i] = Double.parseDouble(bestTPCS.split(" ")[i]);
		}

		return value;
	}

	/**
	 * @return This method calculates the total no of iterations for all possible flexible appliance power profile configurations
	 */
	private static int getTotalIterations(int duration1, int start1, int end1) {

		int iter1 = 0;
		if(start1<end1)
		{
			iter1 = end1-start1-duration1+1;
		}
		else
		{
			end1 = end1+24;
			iter1 = end1-start1-duration1+1;
		}
		if(iter1<=0)
			iter1 = 0;

		return iter1;		
	}

	/**
	 * @return This method returns variance given an array of 24 numbers and avg of the 24 numbers
	 */
	private static double getVariance(double[] TPCS, double avg) {

		double variance = 0;
		for(int i=0;i<24;i++)
		{
			variance += (TPCS[i]-avg)*(TPCS[i]-avg);
		}
		variance /= 24;

		return variance;
	}
	/**
	 * @return This method returns the largest number of an array
	 */
	private static double getLargestValue(double[] TPCS) {

		double largest = 0;
		for(int i=0;i<24;i++)
		{
			if(TPCS[i]>largest)
				largest = TPCS[i];
		}
		return largest;
	}
	/**
	 * @return This method returns the average of all the values of an array
	 */
	private static double getAvgPCS(double[] TPCS) {

		double sum=0;
		for (int i=0;i<24;i++)
		{
			sum += TPCS[i];
		}

		return sum/24.0;

	}
	/**
	 * @param filePath
	 * @return This method reads the contents of a file into a string of 24 numbers separated by space
	 */
	private static String readFileIntoString(String filePath){

		String fileContents="";
		try
		{

			BufferedReader br = new BufferedReader(new FileReader(filePath));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(" ");
				line = br.readLine();
			}
			fileContents = sb.toString();
			br.close();

		}
		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}
		return fileContents;
	}
	/**
	 * @param filePath
	 * @return This method reads the contents of a file into a double array of 24 values
	 */
	private static double[] readFileIntoDoubleArray(String filePath){

		double[] TPC= new double[24];
		try
		{

			int i =0;
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			TPC[i] = Double.valueOf(br.readLine());
			i++;
			while (i<24) {		  	        	
				TPC[i] = Double.valueOf(br.readLine());
				i++;
			};
			br.close();

		}
		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}

		return TPC;
	}
	/**
	 * This method writes the contents of a string separated by spaces into a file
	 * @param filePath
	 * @param fileInput
	 */
	private static void writeStringToFile(String filePath,String fileInput) 
	{
		try
		{
			String[] TPC = fileInput.split(" ");
			PrintWriter writer = new PrintWriter(filePath, "UTF-8");
			for(int j=0;j<24;j++)
			{
				writer.println(TPC[j]);
			}
			writer.close();
		}
		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}
	}
	/**
	 * This method writes the contents of a double array into a file
	 * @param filePath
	 * @param TPC
	 */
	private static void writeDoubleArrayToFile(String filePath,double TPC[]) {

		try{
			PrintWriter writer = new PrintWriter(filePath, "UTF-8");
			for(int j=0;j<24;j++)
			{
				writer.println(TPC[j]);
			}
			writer.close();
		}
		catch(Exception e)
		{
			System.out.println(FILE_ERR_MSG);
			printExceptionMessage();
		}
	}
	/**
	 * This method initializes power data for the node based on the project data set provided.
	 * @param currentNode
	 * @throws Exception
	 */
	private static void initializePowerData(int currentNode) throws Exception {

		//Data
		//11 by 24 matrix
		Constraints = new ArrayList<String>();
		String constraint1= null,constraint2=null;
		//1 by 11 matrix
		if(currentNode == 1)
		{
			//Appliance 1 and 2
			appliancePowerConsumption = new double[]{0.07, 0.05, 0.1, 0.15, 1.6, 1.5, 2.5, 0.3, 0.04, 2, 1.8};
			//Appliance 1 : 5 hrs, 6pm to 7am
			constraint1 = "5 18 7";
			//Appliance 2 : 2 hrs, 9am to 5pm
			constraint2 = "2 9 17";			
		}
		else if(currentNode == 2)
		{
			//Appliance 1 and 3
			appliancePowerConsumption = new double[]{0.07, 0.05, 0.1, 0.15, 1.6, 1.5, 2.5, 0.3, 0.04, 2, 2}	;
			//Appliance 1 : 5 hrs, 6pm to 7am
			constraint1 = "5 18 7";
			//Appliance 3 : 2 hrs, 11pm to 8am
			constraint2 = "2 23 8";		
		}
		else if(currentNode == 3)
		{
			//Appliance 1 and 2
			appliancePowerConsumption = new double[]{0.07, 0.05, 0.1, 0.15, 1.6, 1.5, 2.5, 0.3, 0.04, 2, 1.8}	;
			//Appliance 1 : 5 hrs, 6pm to 7am
			constraint1 = "5 18 7";
			//Appliance 2 : 2 hrs, 9am to 5pm
			constraint2 = "2 9 17";
		}
		Constraints.add(constraint1);
		Constraints.add(constraint2);

		//Random power profile of flexible appliances
		for(int i=0;i<2;i++)
		{
			int duration = Integer.parseInt(Constraints.get(i).split(" ")[0]);
			int start = Integer.parseInt(Constraints.get(i).split(" ")[1]);
			int end = Integer.parseInt(Constraints.get(i).split(" ")[2]);

			if(end<start)
				end = end+24;
			int startTime = randInt(start, end-duration);


			for(int j=0;j<duration;j++)
			{
				appliancePowerProfile[9+i][(start+j)%24]=1;
			}
		}
		double[] TPCN = calculateNodePowerConsumption(appliancePowerConsumption,appliancePowerProfile);
		//create TPCN_x file
		writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt",TPCN);		
	}
	/**
	 * @param appliancePowerConsumption
	 * @param appliancePowerProfile
	 * @return 24 hour power consumption data of the node
	 */
	private static double[] calculateNodePowerConsumption(
			double[] appliancePowerConsumption, int [][] appliancePowerProfile) {
		double[] TPCN= new double[24];

		for(int j=0;j<24;j++)
		{
			TPCN[j] = 0;
			for(int i=0 ; i< 11;i++)
			{
				TPCN[j]+= appliancePowerProfile[i][j] * appliancePowerConsumption[i];
			}
		}
		return TPCN;
	}
	/**
	 * @param min
	 * @param max
	 * @return random integer between min to max
	 */
	public static int randInt(int min, int max) {

		// Usually this can be a field rather than a method variable
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
	/**
	 * Creates chart given TPCS data
	 * @param tPCS
	 */
	public static void createChart(double[] tPCS)
	{
		double[][] model = {{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}};     // Create data array

		for(int i=0;i<24;i++)
			model[0][i] = tPCS[i];

		double[] columns = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24};  // Create x-axis values

		String[] rows = {"Power"};          // Create data set title

		String title = "Total Power Consumption";          // Create diagram title

		int width = 640;                        // Image size
		int height = 480;

		//Create data model
		DefaultChartDataModel data = new DefaultChartDataModel(model, columns, rows);

		// Create chart with default coordinate system
		DefaultChart c = new DefaultChart(data, title, DefaultChart.LINEAR_X_LINEAR_Y);

		// Add a line chart renderer
		c.addChartRenderer(new LineChartRenderer(c.getCoordSystem(), data), 1);

		// Set the chart size
		c.setBounds(new Rectangle(0, 0, width, height));

		// Export the chart as a PNG image
		try {
			ChartEncoder.createEncodedImage(new FileOutputStream(System.getProperty("user.home")+"/first.png"), c, "png");
		} catch(Exception e) {
			printExceptionMessage();
		}
	}
	/**
	 * @return Obtains next node to act as client
	 */
	private static int getNextNode()
	{
		if(node1active == 0 && currentNode==2)
			return 3;
		if(node1active == 0 && currentNode==1)
			return 2;
		if(node2active == 0 && currentNode==3)
			return 1;
		if(node2active == 0 && currentNode==1)
			return 3;
		if(node3active == 0 && currentNode==1)
			return 2;
		if(node3active == 0 && currentNode==3)
			return 1;
		
		if(currentNode == 1)
		{
			node1finished = 1;
			if(node2finished == 0 &&node3finished ==1)
			{
				return 2;
			}
			else if(node2finished == 1 &&node3finished ==0)
			{
				return 3;
			}
			else if(node2finished == node3finished)
			{				
				node2finished = 0;
				node3finished = 0;
				return randInt(2, 3);
			}
		}
		else if(currentNode==2)
		{
			node2finished = 1;
			if(node1finished == 0 &&node3finished ==1)
			{
				return 1;
			}
			else if(node1finished == 1 &&node3finished ==0)
			{
				return 3;
			}
			else if(node1finished == node3finished)
			{
				node1finished = 0;
				node3finished = 0;
				if(randInt(1,2)==1)
					return 1;
				else return 3;			
			}
		}
		else if(currentNode==3)
		{
			node3finished = 1;
			if(node1finished == 0 &&node2finished ==1)
			{
				return 1;
			}
			else if(node1finished == 1 && node2finished==0)
			{
				return 2;
			}
			else if(node1finished == node2finished)
			{
				node1finished = 0;
				node2finished = 0;
				return randInt(1,2);			
			}
		}

		return 0;
	}
	/**
	 * When exception occurs,the following msg is printed
	 */
	private static void printExceptionMessage()
	{
		System.out.println("Code terminated at iteration no"+ noOfIterations);
		printResult();
	}
	/**
	 * The following output is printed as an output
	 */
	private static void printResult() {
		// TODO Auto-generated method stub
		double[] TPCS  = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCS.txt");
		double[] TPCN1 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_1.txt");
		double[] TPCN2 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_2.txt");
		double[] TPCN3 = readFileIntoDoubleArray(System.getProperty("user.dir")+File.separator+"TPCN_3.txt");

		createChart(TPCS);

		System.out.println("Chosen power profile of appliances");
		for(int i=0;i<2;i++)
		{
			for(int j=0;j<24;j++)
			{
				System.out.print(Double.toString(appliancePowerProfile[9+i][j]));
			}
		}

		try{
			System.out.println("Total power consumption (TPCS) ");
			for(int i=0;i<24;i++)
			{
				System.out.print(Double.toString(TPCS[i]));
			}
		}
		catch(Exception e)
		{
			System.out.println("File is invalid");
		}

		try{
			System.out.println("Total power consumption of Node 1 (TPCN1); ");
			for(int i=0;i<24;i++)
			{
				System.out.print(Double.toString(TPCN1[i]));
			}
		}catch(Exception e)
		{
			System.out.println("Node 1 data is invalid");
		}

		try{
			System.out.println("Total power consumption of Node 2 (TPCN2); ");
			for(int i=0;i<24;i++)
			{
				System.out.print(Double.toString(TPCN2[i]));
			}
		}catch(Exception e)
		{
			System.out.println("Node 2 data is invalid");
		}

		try{
			System.out.println("Total power consumption of Node 3 (TPCN3); ");
			for(int i=0;i<24;i++)
			{
				System.out.print(Double.toString(TPCN3[i]));
			}
		}catch(Exception e)
		{
			System.out.println("Node 3 data is invalid");
		}
		if(objective==1)
		{
			double TPAR = getLargestValue(TPCS)/getAvgPCS(TPCS);	
			System.out.println("Minimum PAR of system is = " + Double.toString(TPAR)+ "\n");

			double PAR1 = getLargestValue(TPCN1)/getAvgPCS(TPCN1);	
			System.out.println("Minimum PAR of Node 1 is = " + Double.toString(PAR1)+ "\n");

			double PAR2 = getLargestValue(TPCN2)/getAvgPCS(TPCN2);	
			System.out.println("Minimum PAR of Node 2 is = " + Double.toString(PAR2)+ "\n");

			double PAR3 = getLargestValue(TPCN3)/getAvgPCS(TPCN3);	
			System.out.println("Minimum PAR of Node 3 is = " + Double.toString(PAR3)+ "\n");

			System.out.println("Number of iterations = " + noOfIterations);

		}

		else if(objective==2)
		{
			double TVAR = getVariance(TPCS, getAvgPCS(TPCS));	
			System.out.println("Minimum Variance of system is = " + Double.toString(TVAR)+ "\n");

			double VAR1 = getLargestValue(TPCN1)/getAvgPCS(TPCN1);	
			System.out.println("Minimum Variance of Node 1 is = " + Double.toString(VAR1)+ "\n");

			double VAR2 = getLargestValue(TPCN2)/getAvgPCS(TPCN2);	
			System.out.println("Minimum Variance of Node 2 is = " + Double.toString(VAR2)+ "\n");

			double VAR3 = getLargestValue(TPCN3)/getAvgPCS(TPCN3);	
			System.out.println("Minimum Variance of Node 3 is = " + Double.toString(VAR3)+ "\n");

			System.out.println("Number of iterations = " + noOfIterations);
		}
	}


	/**
	 * This method starts a new thread to run server code
	 * The server receives messages from the node acting as client and returns the information required by the node
	 * When it receives message to act as client, it starts a new thread to run client code
	 * @param currentNode
	 */
	private static void runServer(final int currentNode){(new Thread(){

		//Server is continuously running
		@Override 
		public void run()
		{

			//System.out.println("Entered server");
			ArrayList<Socket> serverSocket = new ArrayList<Socket>();
			String[] ipAddressPortNumber;
			String ipAddress;
			Integer portNumber;
			ServerSocket welcomeSocket=null;
			try
			{	
				welcomeSocket=new ServerSocket() ;
				welcomeSocket.setReuseAddress(true);
				switch(currentNode)
				{
				case 1 :
					ipAddressPortNumber = ipAddressList.get(1).split(" ");
					ipAddress = ipAddressPortNumber[0];
					portNumber = Integer.parseInt(ipAddressPortNumber[1]);
					welcomeSocket.bind(new InetSocketAddress(portNumber));
					break;
				case 2 :
					ipAddressPortNumber = ipAddressList.get(2).split(" ");
					ipAddress = ipAddressPortNumber[0];
					portNumber = Integer.parseInt(ipAddressPortNumber[1]);
					welcomeSocket.bind(new InetSocketAddress(portNumber));
					break;
				case 3:
					ipAddressPortNumber = ipAddressList.get(3).split(" ");
					ipAddress = ipAddressPortNumber[0];
					portNumber = Integer.parseInt(ipAddressPortNumber[1]);
					welcomeSocket.bind(new InetSocketAddress(portNumber));
					break;
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				printExceptionMessage();
			}

			while(true) {			
				if(clientModeEnabled)
				{
					new Thread(){

						int nextNode;
						String[] ipAddressPortNumber;
						String ipAddress;
						Integer portNumber;
						String messageToServer;
						Socket clientSocket;
						@Override
						public void run() {
							// TODO Auto-generated method stub

							//System.out.println("Entered client");
							noOfIterations++;
							System.out.println("Currently at iteration no. "+ noOfIterations+" out of "+ maxNoOfIterations);
							if(noOfIterations > maxNoOfIterations)
							{
								System.out.println("Smart Grid Application Complete");
								printResult();
								System.exit(0);
							}
							//Request for file and receive file from other two nodes
							int i=0;
							try
							{
								messageToServer = "TPCN_Request";
								for(i =1;i<= noOfNodes ;i++)
								{
									if(i != currentNode)
									{
										ipAddressPortNumber = ipAddressList.get(i).split(" ");
										ipAddress = ipAddressPortNumber[0];
										portNumber = Integer.parseInt(ipAddressPortNumber[1]);
										Socket clientSocket = new Socket(ipAddress, portNumber);
										DataOutputStream outToServer = new DataOutputStream(
												clientSocket.getOutputStream());
										BufferedReader inFromServer = 
												new BufferedReader(new InputStreamReader(
														clientSocket.getInputStream()));	
										outToServer.writeBytes(messageToServer+"\n");
										String fileInput = inFromServer.readLine();
										//System.out.println(fileInput);
										writeStringToFile(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(i)+".txt", fileInput);    
										clientSocket.close();  
									}
								}
								
							}
							catch(Exception e)
							{
								System.out.println(e.getMessage());	
//								if(e.getMessage().equalsIgnoreCase("Connection Refused"))
//								{
//									adjustPowerProfile();
//								}
								printExceptionMessage();
								if(i==1)
									node1active = 0;
								if(i==2)
									node2active = 0;
								if(i==3)
									node3active = 0;
							}

							finally
							{
								try
								{
									adjustPowerProfile();
									//Obtain next node randomly : no priority
										nextNode = getNextNode();
										if(nextNode==0)
										{
											System.out.println("No node has not been assigned as client");
										}
								

									//Send message to next node to change to client mode
									messageToServer = "Change_To_Client"+":"+bestTPCS+":"+String.valueOf(node1finished)+":"+String.valueOf(node2finished)+":"+String.valueOf(node3finished);
									String minTPCS = messageToServer.split(":")[1];
									ipAddressPortNumber = ipAddressList.get(nextNode).split(" ");
									ipAddress = ipAddressPortNumber[0];
									portNumber = Integer.parseInt(ipAddressPortNumber[1]);
									
									clientSocket = new Socket(ipAddress, portNumber);
									DataOutputStream outToServer = new DataOutputStream(
											clientSocket.getOutputStream());
									BufferedReader inFromServer = 
											new BufferedReader(new InputStreamReader(
													clientSocket.getInputStream()));
									outToServer.writeBytes(messageToServer+"\n");
									clientSocket.close();
								}

								catch(Exception e)
								{
									System.out.println(e.getMessage());	
									if(e.getMessage().equalsIgnoreCase("Connection Refused"))
									{
										adjustPowerProfile();
									}
									printExceptionMessage();
								}
								
						
							}
						}


						

					}.start();
					clientModeEnabled = false;
				}
				try
				{
					System.out.println("Waiting for connection from client");
					Socket connectionSocket = welcomeSocket.accept();
					serverSocket.add(connectionSocket);
					
					BufferedReader inFromClient = 
							new BufferedReader(new InputStreamReader(
									connectionSocket.getInputStream()));
					DataOutputStream outToClient = 
							new DataOutputStream(
									connectionSocket.getOutputStream());
					String clientMessage = inFromClient.readLine();
					//System.out.println(clientMessage);
					if(clientMessage.equalsIgnoreCase("TPCN_Request"))
					{
						String fileContents;
						fileContents = readFileIntoString(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt");
						outToClient.writeBytes(fileContents+"\n");
					}
					if(clientMessage.startsWith("Change_To_Client"))
					{
						bestTPCS = clientMessage.split(":")[1];
						node1finished = Integer.parseInt(clientMessage.split(":")[2]);
						node2finished = Integer.parseInt(clientMessage.split(":")[3]);
						node3finished = Integer.parseInt(clientMessage.split(":")[4]);
						clientModeEnabled = true;
					}
					connectionSocket.close();
				}
				catch(Exception e)
				{
					System.out.println(e.getMessage());
					printExceptionMessage();
					continue;
				}

			}
		}
	}).start();
	}

}

