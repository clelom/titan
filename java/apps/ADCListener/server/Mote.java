package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * Storing all information of a node
 * @author Benedikt Kšppel
 */
public class Mote {
	
	/**
	 * address of the node
	 */
	private InetAddress nodeAddress;
	
	/**
	 * period between two samples
	 */
	private int samplingPeriod;
	
	/**
	 * if node is sampling
	 */
	private Boolean isRunning;
	
	/**
	 * port on which the node listens for connections
	 */
	private int nodePort;
	
	/**
	 * address of the server (i.e. my address)
	 */
	private InetAddress serverAddress;
	
	
	/**
	 * Constructs a Node
	 * @param serverAddress address of the server (i.e. my address)
	 * @param nodeAddress address of the node
	 * @param samplingPeriod period between two samples
	 * @param nodePort port on which the node listens for connections
	 */
	public Mote(InetAddress serverAddress, InetAddress nodeAddress, int samplingPeriod, int nodePort) {
		this.serverAddress = serverAddress;
		this.nodeAddress = nodeAddress;
		this.samplingPeriod = samplingPeriod;
		this.nodePort = nodePort;
		this.isRunning = false;
	}
	
	/**
	 * Starts the sampling on the node
	 * TODO: check if node is really sampling
	 */
	public void startADC() {
		if (isRunning == true) {
			System.out.println("Node " + nodeAddress.getHostAddress() + " is already running.");
		} else {
			
			String message = "startadc " + samplingPeriod + " " + serverAddress.getHostAddress() + ":\n";
			try {
				sendString( message );
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			isRunning = true;
		}
	}
	
	/**
	 * Stops the sampling on a node
	 * TODO: check if node really stopped
	 */
	public void stopADC() {
		if (isRunning == false) {
			System.out.println("Node " + nodeAddress.getHostAddress() + " is alread stopped.");
		} else {

			String message = "stopadc\n";
			try { 
				sendString( message );
			} catch (IOException e) {
				e.printStackTrace();
			}

			isRunning = false;
		}
		
	}
	
	/**
	 * Sends a string to a node
	 * @param message String to send
	 * @throws IOException throws exception if there was a problem with DatagramPacket or DatagramSocket
	 */
	private void sendString( final String message ) throws IOException {
		DatagramSocket socket = new DatagramSocket( nodePort );
		
		byte[] messageChar = message.getBytes();
		
		System.out.println(message.toString() + " will be sent to " + nodeAddress.getHostAddress()); 	
		
		DatagramPacket packet = new DatagramPacket(messageChar, messageChar.length, nodeAddress, nodePort);	
		
		socket.send(packet);
	}

	/**
	 * @return the nodeAddress
	 */
	public InetAddress getNodeAddress() {
		return nodeAddress;
	}

}
