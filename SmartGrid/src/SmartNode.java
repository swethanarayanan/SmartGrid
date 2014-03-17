
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


public class SmartNode {

	static int noOfNodes = 3;
	static HashMap<Integer,String> ipAddressList = new HashMap<Integer,String>();
	
	public static void main(String[] args) throws Exception {
	
		ipAddressList.put(1,"127.0.0.1 6787");
		ipAddressList.put(2,"127.0.0.1 6788");
		ipAddressList.put(3,"127.0.0.1 6789");		
		Scanner scanner = null;
		int currentNode;
		
		try
		{
			scanner = new Scanner (System.in);
			System.out.println("\n Choose node: \n 1. Node 1 \n 2. Node 2 \n 3. Node 3");
			currentNode = scanner.nextInt();
			initializePowerData(currentNode);
			scanner = new Scanner (System.in);
			System.out.println("\n Choose option: \n 1. Act as Server \n 2. Act as Client");	
			int option = scanner.nextInt();
			if(option == 1)
			{
				serverCode(currentNode);
				
			}
			else if(option == 2)
			{
				//Client
				//Connect to Servers
				clientCode(currentNode);	
				adjustPowerProfile(currentNode);
			}
		}
		finally {
			if(scanner!=null)
				scanner.close();
		}
		//Send message to the other two nodes job done , randomly choose next client,,make itself a server
		
		
	}


	private static void adjustPowerProfile(int currentNode) {
		// TODO Auto-generated method stub
		/**
		 * @TODO Find PAR,Variance of TPCS = TPCN1 + TPCN2 + TPCN3
		 * Change appliance PowerProfile and hence TPCN of current node s.t PAR and variance of TPCS is minimum
		 * Update TPCN
		 */
		
		/**
		 * @DOUBT How to automate multiple iterations across multiple nodes?
		 */
		//Maintain TPCS
		String TPCS = "";
		String TPCN1 = readFileIntoString("TPCN_1.txt");
		String TPCN2 = readFileIntoString("TPCN_2.txt");
		String TPCN3 = readFileIntoString("TPCN_3.txt");
		
		String[] TPCN1_24Hour = TPCN1.split("");
		String[] TPCN2_24Hour = TPCN2.split("");
		String[] TPCN3_24Hour = TPCN3.split("");
		
		for(int i=0;i<24;i++)
		{
			int TPCN1ByHour = Integer.parseInt(TPCN1_24Hour[i]);
			int TPCN2ByHour = Integer.parseInt(TPCN2_24Hour[i]);
			int TPCN3ByHour = Integer.parseInt(TPCN3_24Hour[i]);
			
			int TPCNSByHour = TPCN1ByHour + TPCN2ByHour + TPCN3ByHour;
			TPCS += String.valueOf(TPCNSByHour)+" ";
		}
	}


	private static String readFileIntoString(String fileName) {
		// TODO Auto-generated method stub
		return null;
	}


	private static void initializePowerData(int currentNode) throws Exception {
		// TODO Auto-generated method stub

		//Data
		//11 by 24 matrix
		int[][] appliancePowerProfile = {
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
		
		double[] AppliancePowerConsumption = null ;
		ArrayList<String> Constraints = new ArrayList<String>();
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
		
		double[] totalPowerConsumptionNode= new double[24];
		
		for(int j=0;j<24;j++)
		{
			totalPowerConsumptionNode[j] = 0;
			for(int i=0 ; i< 11;i++)
			{
				totalPowerConsumptionNode[j]+= appliancePowerProfile[i][j] * AppliancePowerConsumption[i];
			}
		}
		//create TPCN_x
		PrintWriter writer = new PrintWriter(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt", "UTF-8");
		for(int j=0;j<24;j++)
		{
			writer.println(totalPowerConsumptionNode[j]);
		}
		writer.close();
	}


	private static void clientCode(int currentNode) throws Exception {
		// TODO Auto-generated method stub
		String input="";
		String[] ipAddressPortNumber;
		String ipAddress;
		Integer portNumber;
		Scanner scanner = null;
		ArrayList<String> totalPowerConsumed = new ArrayList<String>();
		System.out.println("\n Choose option: \n 1. Request File ");
		scanner = new Scanner (System.in);
		if(scanner.nextInt()==1)
			input = "File Request";
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
		        outToServer.writeBytes(input+"\n");
		        String fileInput = inFromServer.readLine();
		        System.out.println(fileInput);
		        String[] powerConsumedByHour = fileInput.split(" ");
		        PrintWriter writer = new PrintWriter(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(i)+".txt", "UTF-8");
				for(int j=0;j<24;j++)
				{
					writer.println(powerConsumedByHour[j]);
				}
				writer.close();		        
		        clientSocket.close();  				        
			}
			else
			{
				String fileContents;
                BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt"));
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
			}
		}
	}

	private static void serverCode(int currentNode) throws Exception {
		// TODO Auto-generated method stub
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
            if(clientMessage.equalsIgnoreCase("File Request"))
            {
            	String fileContents;
                BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir")+File.separator+"TPCN_"+String.valueOf(currentNode)+".txt"));
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
            	outToClient.writeBytes(fileContents+"\n");
            }
          //change to server/client depending on message
        }
	}

}

