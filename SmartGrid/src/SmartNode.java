
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

//TPCS : Total Power Consumption of System
//TPCN : Total Power Consumption of Node
//TPC : Total Power Consumption

public class SmartNode {

	//no of iterations = no of times I became a client
	static int noOfIterations = 0;
	/**
	 * @TODO : maxNoOfIterations to be updated by Abhishek
	 */
	static int maxNoOfIterations = 100;
	static int noOfNodes = 3;
	static HashMap<Integer,String> ipAddressList = new HashMap<Integer,String>();
	static boolean clientModeEnabled= false;
	static String minTPCS;
	static int currentNode;
	static int objective;

	static ArrayList<String> Constraints; 
	static double[] AppliancePowerConsumption = null ;
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
		runClient(currentNode);	
	}


	private static void adjustPowerProfile(int currentNode) throws Exception {

		/**
		 * @TODO Find PAR,Variance of TPCS = TPCN1 + TPCN2 + TPCN3
		 * Change appliance PowerProfile and hence TPCN of current node s.t PAR and variance of TPCS is minimum
		 * Update TPCN
		 */
		//Maintain TPCS
		double[] TPCS= new double[24];
		double[] TPCN1 = readFileIntoDoubleArray("TPCN_1.txt");
		double[] TPCN2 = readFileIntoDoubleArray("TPCN_2.txt");
		double[] TPCN3 = readFileIntoDoubleArray("TPCN_3.txt");

		for(int i=0;i<24;i++)
		{			
			TPCS[i] = TPCN1[i] + TPCN2[i] + TPCN3[i];
		}
		writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCS.txt",TPCS);

		double average = getAvgPCS(TPCS);
		double peakToAvg = 0;
		double variance = 0;

		if(average!=0)
			peakToAvg = getLargestValue(TPCS)/average;

		variance = getVariance(TPCS, average);

		if(objective==1)
		{
			minimizePAR(appliancePowerProfile,TPCS);
		}
		else
		{	
			minimizeVariance(appliancePowerProfile, TPCS);
		}
		/**
		 * @TODO : if objective 1 (PAR) or objective 2(Variance)
		 */
		/**
		 * @TODO if new currentMinTPCS(PAR) < minTPCS(PAR) then minTPCS = currentMinTPCS
		 * 
		 */
	}


	private static void minimizeVariance(int[][] appliancePowerProfile2, double[] tPCS) throws Exception {
		// TODO Auto-generated method stub

		int duration1 = Integer.parseInt(Constraints.get(0).split(" ")[0]);
		int start1 = Integer.parseInt(Constraints.get(0).split(" ")[1]);
		int end1 = Integer.parseInt(Constraints.get(0).split(" ")[2]);

		int duration2 = Integer.parseInt(Constraints.get(1).split(" ")[0]);
		int start2 = Integer.parseInt(Constraints.get(1).split(" ")[1]);
		int end2 = Integer.parseInt(Constraints.get(1).split(" ")[2]);

		int iter1 = getTotalIterations(duration1,start1,end1);
		int iter2 = getTotalIterations(duration2,start2,end2);
		int total_iterations = iter1*iter2;

		double[] TPCS= new double[24];
		double[] TPCN1 = readFileIntoDoubleArray("TPCN_1.txt");
		double[] TPCN2 = readFileIntoDoubleArray("TPCN_2.txt");
		double[] TPCN3 = readFileIntoDoubleArray("TPCN_3.txt");

		double variance = Double.MAX_VALUE;
		//Extensive search

		int [][] selectedPowerProfile =appliancePowerProfile2.clone();
		for (int l = 0; l < selectedPowerProfile.length; l++) 
		{
			selectedPowerProfile[l] = appliancePowerProfile2[l].clone();
		}
		for(int l=0; l<24;l++)
		{
			selectedPowerProfile[9][l]=0;
			selectedPowerProfile[10][l]=0;
		}
		for(int i=0;i<iter1;i++)
		{
			int [][] tempPowerProfile =appliancePowerProfile2.clone();
			for (int l1 = 0; l1 < tempPowerProfile.length; l1++) 
			{
				tempPowerProfile[l1] = appliancePowerProfile2[l1].clone();
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
				for(int k1=0;k1<duration1;k1++)
					tempPowerProfile[10][(start2+j+k1)%24] = 1;
				double [] tpcn = calculateNodePowerConsumption(AppliancePowerConsumption, tempPowerProfile);
				for(int m=0;m<24;m++)
				{			
					TPCS[m] = tpcn[m] + TPCN2[m] + TPCN3[i];
				}
				double avg = getAvgPCS(TPCS);
				if(getVariance(TPCS, avg)<variance)
				{
					for (int n = 0; n < selectedPowerProfile.length; n++) 
					{
						selectedPowerProfile[n] = tempPowerProfile[n].clone();
					}
					variance = getVariance(TPCS, avg);
				}
			}

		}

		double [] oldTpcs = getTPCSfromString(minTPCS);
		double oldVar = getVariance(oldTpcs,getAvgPCS(oldTpcs));

		if(variance < oldVar)
		{
			for(int k =0;k < 24;k++)
			{
				appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
				appliancePowerProfile[10][k] = selectedPowerProfile[10][k];

			}

			oldVar = variance;
		}
		System.out.println(variance);

	}


	private static void minimizePAR(int[][] appliancePowerProfile2, double[] tPCS) throws Exception {
		// TODO Auto-generated method stub

		int duration1 = Integer.parseInt(Constraints.get(0).split(" ")[0]);
		int start1 = Integer.parseInt(Constraints.get(0).split(" ")[1]);
		int end1 = Integer.parseInt(Constraints.get(0).split(" ")[2]);

		int duration2 = Integer.parseInt(Constraints.get(1).split(" ")[0]);
		int start2 = Integer.parseInt(Constraints.get(1).split(" ")[1]);
		int end2 = Integer.parseInt(Constraints.get(1).split(" ")[2]);

		int iter1 = getTotalIterations(duration1,start1,end1);
		int iter2 = getTotalIterations(duration2,start2,end2);
		int total_iterations = iter1*iter2;

		double[] TPCS= new double[24];
		double[] TPCN1 = readFileIntoDoubleArray("TPCN_1.txt");
		double[] TPCN2 = readFileIntoDoubleArray("TPCN_2.txt");
		double[] TPCN3 = readFileIntoDoubleArray("TPCN_3.txt");

		double peak = Double.MAX_VALUE;
		//Extensive search

		int [][] selectedPowerProfile =appliancePowerProfile2.clone();
		for (int l = 0; l < selectedPowerProfile.length; l++) 
		{
			selectedPowerProfile[l] = appliancePowerProfile2[l].clone();
		}
		for(int l=0; l<24;l++)
		{
			selectedPowerProfile[9][l]=0;
			selectedPowerProfile[10][l]=0;
		}
		for(int i=0;i<iter1;i++)
		{
			int [][] tempPowerProfile =appliancePowerProfile2.clone();
			for (int l1 = 0; l1 < tempPowerProfile.length; l1++) 
			{
				tempPowerProfile[l1] = appliancePowerProfile2[l1].clone();
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
				for(int k1=0;k1<duration1;k1++)
					tempPowerProfile[10][(start2+j+k1)%24] = 1;
				double [] tpcn = calculateNodePowerConsumption(AppliancePowerConsumption, tempPowerProfile);
				for(int m=0;m<24;m++)
				{			
					TPCS[m] = tpcn[m] + TPCN2[m] + TPCN3[i];
				}
				double avg = getAvgPCS(TPCS);
				double largestValue = getLargestValue(TPCS);
				if(largestValue<peak)
				{
					for (int n = 0; n < selectedPowerProfile.length; n++) 
					{
						selectedPowerProfile[n] = tempPowerProfile[n].clone();
					}
					peak = getLargestValue(TPCS);
					
				}
			}

		}
		for(int k =0;k < 24;k++)
		{
			appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
			appliancePowerProfile[10][k] = selectedPowerProfile[10][k];
			
		}
		System.out.println(peak);
		System.out.println(TPCS);

		double [] oldTpcs = getTPCSfromString(minTPCS);

		double oldPeak = getLargestValue(oldTpcs);

		if(peak < oldPeak)
		{
			for(int k =0;k < 24;k++)
			{
				appliancePowerProfile[9][k] = selectedPowerProfile[9][k];
				appliancePowerProfile[10][k] = selectedPowerProfile[10][k];

			}

			oldPeak = peak;
		}
		System.out.println(oldPeak);

	}


	private static double[] getTPCSfromString(String minTPCS2) {

		double[] value = new double[24];
		for(int i=0;i<24;i++)
		{
			value[i] = Double.parseDouble(minTPCS2.split(" ")[i]);
		}

		return value;
	}


	private static int getTotalIterations(int duration1, int start1, int end1) {
		// TODO Auto-generated method stub

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

	private static double getVariance(double[] tPCS, double avg) {

		double variance = 0;
		for(int i=0;i<24;i++)
		{
			variance += (tPCS[i]-avg)*(tPCS[i]-avg);
		}
		variance /= 24;

		return variance;
	}


	private static double getLargestValue(double[] tPCS) {

		double largest = 0;
		for(int i=0;i<24;i++)
		{
			if(tPCS[i]>largest)
				largest = tPCS[i];
		}
		return largest;
	}


	private static double getAvgPCS(double[] TPCS) {

		double sum=0;
		for (int i=0;i<24;i++)
		{
			sum += TPCS[i];
		}

		return sum/24.0;

	}


	private static String readFileIntoString(String filePath) throws Exception {

		String fileContents="";
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append(" ");
				line = br.readLine();
			}
			fileContents = sb.toString();
		} finally {
			br.close();
		}
		return fileContents;
	}

	private static double[] readFileIntoDoubleArray(String filePath) throws Exception {

		double[] TPC= new double[24];
		int i =0;
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		try {
			TPC[i] = Double.valueOf(br.readLine());
			i++;
			while (i<24) {		  	        	
				TPC[i] = Double.valueOf(br.readLine());
				i++;
			};
		} finally {
			br.close();
		}
		return TPC;

	}
	private static void writeStringArrayToFile(String filePath,String fileInput) throws Exception 
	{
		String[] TPC = fileInput.split(" ");
		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		for(int j=0;j<24;j++)
		{
			writer.println(TPC[j]);
		}
		writer.close();
	}
	private static void writeDoubleArrayToFile(String filePath,double TPC[]) throws Exception {

		PrintWriter writer = new PrintWriter(filePath, "UTF-8");
		for(int j=0;j<24;j++)
		{
			writer.println(TPC[j]);
		}
		writer.close();
	}

	private static void initializePowerData(int currentNode) throws Exception {

		//Data
		//11 by 24 matrix


		Constraints = new ArrayList<String>();
		String constraint1= null,constraint2=null;
		//1 by 11 matrix
		if(currentNode == 1)
		{
			//Appliance 1 and 2
			AppliancePowerConsumption = new double[]{0.07, 0.05, 0.1, 0.15, 1.6, 1.5, 2.5, 0.3, 0.04, 2, 1.8};
			//Appliance 1 : 5 hrs, 6pm to 7am
			constraint1 = "5 18 7";
			//Appliance 2 : 2 hrs, 9am to 5pm
			constraint2 = "2 9 17";			
		}
		else if(currentNode == 2)
		{
			//Appliance 1 and 3
			AppliancePowerConsumption = new double[]{0.07, 0.05, 0.1, 0.15, 1.6, 1.5, 2.5, 0.3, 0.04, 2, 2}	;
			//Appliance 1 : 5 hrs, 6pm to 7am
			constraint1 = "5 18 7";
			//Appliance 3 : 2 hrs, 11pm to 8am
			constraint2 = "2 23 8";		
		}
		else if(currentNode == 3)
		{
			//Appliance 1 and 2
			AppliancePowerConsumption = new double[]{0.07, 0.05, 0.1, 0.15, 1.6, 1.5, 2.5, 0.3, 0.04, 2, 1.8}	;
			//Appliance 1 : 5 hrs, 6pm to 7am
			constraint1 = "5 18 7";
			//Appliance 2 : 2 hrs, 9am to 5pm
			constraint2 = "2 9 17";
		}
		Constraints.add(constraint1);
		Constraints.add(constraint2);
		/**
		 * @TODO Random power profile of flexible appliances
		 */

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


		double[] TPCN = calculateNodePowerConsumption(AppliancePowerConsumption,appliancePowerProfile);
		//create TPCN_x file
		writeDoubleArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt",TPCN);		
	}


	private static double[] calculateNodePowerConsumption(
			double[] AppliancePowerConsumption, int [][] appliancePowerProfile1) {
		double[] TPCN= new double[24];

		for(int j=0;j<24;j++)
		{
			TPCN[j] = 0;
			for(int i=0 ; i< 11;i++)
			{
				TPCN[j]+= appliancePowerProfile1[i][j] * AppliancePowerConsumption[i];
			}
		}
		return TPCN;
	}

	public static int randInt(int min, int max) {

		// Usually this can be a field rather than a method variable
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}
	private static void runClient(final int currentNode){
		(new Thread(){


			int nextNode;
			String[] ipAddressPortNumber;
			String ipAddress;
			Integer portNumber;
			String messageToServer;


			@Override
			public void run()
			{
				System.out.println("Enter client");
				while(true) {

					//or is it 'if client mode enabled'?
					if(clientModeEnabled)
					{
						noOfIterations ++;
						if(noOfIterations > maxNoOfIterations)
						{
							System.out.println("Smart Grid Application Complete");
							/**
							 * @TODO Plot final TPCS,TPCN1,TPCN2,TPCN3
							 */
							System.exit(0);
						}
						//Request for file and receive file from other two nodes
						try
						{
							messageToServer = "TPCN_Request";
							for(int i =1;i<= noOfNodes ;i++)
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
									System.out.println(fileInput);
									writeStringArrayToFile(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(i)+".txt", fileInput);    
									clientSocket.close();  
								}
							}
							adjustPowerProfile(currentNode);
							//Find the next node to be client
							//Round Robin ok?

							if(currentNode == noOfNodes)
								nextNode = 1;
							else
								nextNode = currentNode + 1;

							clientModeEnabled = false;


							//Send message to next node to change to client mode
							messageToServer = "Change_To_Client"+" "+minTPCS;
							ipAddressPortNumber = ipAddressList.get(nextNode).split(" ");
							ipAddress = ipAddressPortNumber[0];
							portNumber = Integer.parseInt(ipAddressPortNumber[1]);
							Socket clientSocket = new Socket(ipAddress, portNumber);
							DataOutputStream outToServer = new DataOutputStream(
									clientSocket.getOutputStream());
							BufferedReader inFromServer = 
									new BufferedReader(new InputStreamReader(
											clientSocket.getInputStream()));	
							outToServer.writeBytes(messageToServer+"\n");
						}
						catch (Exception e) {
							e.printStackTrace();
						} 
					}
				}
			}
		}).start();
	}

	private static void runServer(final int currentNode){(new Thread(){

		//Server is continuously running
		@Override 
		public void run()
		{
			System.out.println("Enter server");
			while(true) {

				try{
					String[] ipAddressPortNumber;
					String ipAddress;
					Integer portNumber;
					ServerSocket welcomeSocket=new ServerSocket();
					switch(currentNode)
					{
					case 1 :
						ipAddressPortNumber = ipAddressList.get(1).split(" ");
						ipAddress = ipAddressPortNumber[0];
						portNumber = Integer.parseInt(ipAddressPortNumber[1]);
						welcomeSocket = new ServerSocket(portNumber);
						break;
					case 2 :
						ipAddressPortNumber = ipAddressList.get(2).split(" ");
						ipAddress = ipAddressPortNumber[0];
						portNumber = Integer.parseInt(ipAddressPortNumber[1]);
						welcomeSocket = new ServerSocket(portNumber);
						break;
					case 3:
						ipAddressPortNumber = ipAddressList.get(3).split(" ");
						ipAddress = ipAddressPortNumber[0];
						portNumber = Integer.parseInt(ipAddressPortNumber[1]);
						welcomeSocket = new ServerSocket(portNumber);
						break;
					}
					while(true) {
						Socket connectionSocket = welcomeSocket.accept();
						System.out.println(connectionSocket.toString());
						BufferedReader inFromClient = 
								new BufferedReader(new InputStreamReader(
										connectionSocket.getInputStream()));
						DataOutputStream outToClient = 
								new DataOutputStream(
										connectionSocket.getOutputStream());
						String clientMessage = inFromClient.readLine();
						System.out.println(clientMessage);
						if(clientMessage.equalsIgnoreCase("TPCN_Request"))
						{
							String fileContents;
							fileContents = readFileIntoString(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt");
							outToClient.writeBytes(fileContents+"\n");
						}
						if(clientMessage.startsWith("Change_To_Client"))
						{
							minTPCS = clientMessage.split(" ")[1];
							clientModeEnabled = true;
						}
					}
				}catch (Exception e) {
					e.printStackTrace();
				} 
			}
		}
	}).start();
	}

}

