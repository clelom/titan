package server;
import java.net.*;
import java.util.*;
import utils.FIFO;


/**
 * Listens for incoming UDP packets and stores the packet information into FIFO
 * @author Benedikt Kšppel
 */
public class Listener extends Thread {
	
	/**
	 * port number on which ADCListeners listens for incoming packets
	 */
	private int portNumber;
	
	/**
	 * number of samples which will be sent in one packet
	 */
	private int samplesPerPacket;
	
	/**
	 * sampling period between two samples
	 */
	private int samplingPeriod;
	
	/**
	 * FIFO where all samples should be stored into
	 */
	private FIFO<XYZPacket> storageFIFO;
	
	
	/**
	 * Constructor for ADCListener
	 * @param portNumber port number on which ADCListeners listens for incoming packets
	 * @param samplesPerPacket number of samples which will be sent in one packet
	 * @param samplingPeriod sampling period between two samples
	 * @param storageFIFO FIFO where all samples should be stored into
	 */
	public Listener(int portNumber, int samplesPerPacket, int samplingPeriod, FIFO<XYZPacket> storageFIFO) {
		this.portNumber = portNumber;
		this.samplesPerPacket = samplesPerPacket;
		this.samplingPeriod = samplingPeriod;
		this.storageFIFO = storageFIFO;
	}
	
	/**
	 * Starts the server thread
	 */
	public void run() {
		
		try {
			DatagramSocket socket = new DatagramSocket( portNumber );
			DatagramPacket packet;
			System.out.println("Opened Socket on Port " + portNumber);

			while ( true ) {
				
				/*
				 * create a package, and wait until the socket receives one packet.
				 * 'socket' will store the received data into 'packet'.
				 * FIXME: if Listener has an open socket, ShutdownMotes.java can't stop motes!
				 */
				packet = new DatagramPacket( new byte[1024], 1024 );
				socket.receive( packet );
				Date recievedAt = new Date();

				InetAddress receivedFrom = packet.getAddress();
				byte        adcData[]  = packet.getData();

				/*
				 * don't print it, but store it into FIFO
				 */
				storeValuesInFIFO(adcData, receivedFrom, recievedAt);
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stores the data into the FIFO
	 * @param data data received in the UDP packet
	 * @param address sender address of the UDP packet
	 * @param receivedAt time when packet was received, milliseconds since 1.1.1970
	 */
	void storeValuesInFIFO(byte data[], InetAddress address, Date receivedAt) {
		/*
		 * first, fetch the IP-address of the node. This identifies each node.
		 * then, convert the received data (byte) into a array of shorts. Each ADC value is 2 byte (=short) long. Then cut off 0 at the end of the array.
		 * the number of received samples is: ( total length ) - ( bytes used for other information ) / ( 3 values per sample )
		 */
		String nodeID = address.getHostAddress();
		//short[] data_s = trimArray(byteToShort(data));
		short[] data_s = byteToShort(data);

		/*
		 * find out packet count, which is sent directly after the last Z-value
		 * find out period, which is sent after period
		 */
		short period = data_s[samplesPerPacket*3+1];
		short count = data_s[samplesPerPacket*3];
		
		/*
		 * variables for X, Y and Z values
		 */
		short valueX;
		short valueY;
		short valueZ;
		
		/*
		 * iterate over all samples
		 */
		for ( int sampleInPacket=0; sampleInPacket < samplesPerPacket; sampleInPacket++ ) {
			
			/*
			 * first, fetch the X,Y and Z values of the sample.
			 * if all three values are 0, there were no more values stored.
			 * 		this is, because packet = new DatagramPacket( new byte[1024], 1024 ); initializes the packet.data with 0, with a fixed size.
			 *      hence, we can continue with the next packet.
			 */
			valueX = data_s[sampleInPacket+0*samplesPerPacket];
			valueY = data_s[sampleInPacket+1*samplesPerPacket];
			valueZ = data_s[sampleInPacket+2*samplesPerPacket];
			if ( valueX == 0 && valueY == 0 && valueZ == 0) {
				break;
			}

			/*
			 * store data into FIFO
			 */
			XYZPacket temp = new XYZPacket(receivedAt.getTime()-(samplingPeriod-sampleInPacket)*period, nodeID, period, count, (short)sampleInPacket, (short)samplesPerPacket, valueX, valueY, valueZ);
			storageFIFO.offer( temp );			
			System.out.print(".");
		}
	}
	
	/**
	 * Converts char array into short array
	 * @param data to convert from byte to short
	 * @return gives the converted data (array) as array of shorts
	 */
	short[] byteToShort(byte[] data) {
		/*
		 * the result will be a array of shorts, with half length
		 */
		short[] result = new short[data.length/2];
		
		for ( int i=0; i<data.length/2; i++) {
			/*
			 * for each two bytes in the byte array, calculate the value of the short array.
			 * short value = (high byte value)*256 + (low byte value)
			 * because network traffic is big endian, the first byte is the high one
			 */
			result[i] = (short) (data[i*2+1]*256 + (int)(data[i*2] & 0xFF));
		}
		return result;
	}
	
	/**
	 * Trims the array (removes trailing whitespaces)
	 * @param data array to trim
	 * @return trimmed array
	 */
	short[] trimArray(short[] data) {
		/*
		 * get the length of the result array
		 * start at the length of 'data'
		 * go element by element back in the array and decrease length
		 * if the actual element is different from 0, we found the end of the array
		 */
		int length;
		for( length=data.length; length>0; length--) {
			if ( data[length-1] != 0 ) {
				break;
			}
		}
		
		/*
		 * create the result array of this length
		 * and copy elementwise from data into result
		 */
		short[] result = new short[length];
		for( int i=0; i<length; i++) {
			result[i] = data[i];
		}
		return result;		
	}
	
	/**
	 * Convert an unsigned int to a String.
	 * @param i the int to convert.
	 * @return the equivalent unsigned String
	 */
	public String unsignedToString( int i ) {
		return Long.toString( i & 0xffffffffL );
	}
}
