
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
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
		
		try
		{
			scanner = new Scanner (System.in);
			System.out.println("\n Choose node: \n 1. Node 1 \n 2. Node 2 \n 3. Node 3");
			int currentNode = scanner.nextInt();
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
			}
		}
		finally {
			if(scanner!=null)
				scanner.close();
		}
		
		//Data
//		int [][] appliancePowerProfileMatrix = new int[1][24];
//		appliancePowerProfileMatrix = {{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};
//						
		
				
						
		}


	private static void clientCode(int currentNode) throws Exception {
		// TODO Auto-generated method stub
		String[] ipAddressPortNumber;
		String ipAddress;
		Integer portNumber;
		Scanner scanner = null;
		ArrayList<String> totalPowerConsumed = new ArrayList<String>();
		for(int i =1;i<= noOfNodes ;i++)
		{
			if(i != currentNode)
			{
				String input="";
				ipAddressPortNumber = ipAddressList.get(i).split(" ");
				ipAddress = ipAddressPortNumber[0];
				portNumber = Integer.parseInt(ipAddressPortNumber[1]);
				System.out.println("\n Choose option: \n 1. Request File ");
				scanner = new Scanner (System.in);
				if(scanner.nextInt()==1)
					input = "File Request";
				Socket clientSocket = new Socket(ipAddress, portNumber);
				DataOutputStream outToServer = new DataOutputStream(
		                clientSocket.getOutputStream());
		        BufferedReader inFromServer = 
		                new BufferedReader(new InputStreamReader(
		                    clientSocket.getInputStream()));				        
		        outToServer.writeBytes(input);
		        String fileInput = inFromServer.readLine();
		        totalPowerConsumed.add(fileInput);
		        System.out.println("FROM SERVER: " + fileInput);
		        clientSocket.close();  				        
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
            BufferedReader inFromClient = 
                    new BufferedReader(new InputStreamReader(
                        connectionSocket.getInputStream()));
            DataOutputStream outToClient = 
                    new DataOutputStream(
                        connectionSocket.getOutputStream());
            String clientMessage = inFromClient.readLine();
            if(clientMessage.equalsIgnoreCase("File Request"))
            {
            	String fileContents;
                BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.dir")+File.separator+"TPCN.txt"));
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
            	outToClient.writeBytes(fileContents);
            }
        }
	}

}
