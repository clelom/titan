package server;
import java.util.Formatter;
import utils.FIFO;
import filters.*;

/**
 * Thread to read data from FIFO, process it and write it into logfile
 * @author Benedikt Kšppel
 */
public class FileWriter extends Thread {
	
	/**
	 * FIFO where samples are stored in
	 */
	private FIFO<XYZPacket> storageFIFO;
	
	/**
	 * Formatter to write the output
	 */
	private Formatter outputFormatter;
	
	/**
	 * Set of filters, each sample will be filtered by all those filters
	 */
	private FilterBank<XYZPacket> filterBank;
	

	/**
	 * Constructor for FileWriter
	 * @param storageFIFO FIFO, where samples are stored in
	 * @param outputFormatter formatter to write the output
	 * @param filterBank set of filters
	 */
	public FileWriter(FIFO<XYZPacket> storageFIFO, Formatter outputFormatter, FilterBank<XYZPacket> filterBank) {
		this.storageFIFO = storageFIFO;
		this.outputFormatter = outputFormatter;
		this.filterBank = filterBank;
		System.out.println("Started FileWriter...");
	}
	
	/**
	 * the server thread
	 * if there is a packet in the FIFO, it will be filtered and then print to the logfile
	 * if there is no packet in FIFO, the thread will pause
	 */
	public void run() {
			
		while( true ) {
			if ( storageFIFO.isEmpty() ) {
				yield();
			} else {
				write();
			}
		}
	}
	
	/**
	 * Takes one element from FIFO, processes it and writes it to logfile
	 * @warning call this method only if there is an element in the FIFO!
	 */
	private void write() {
		
		XYZPacket packet = storageFIFO.poll();
		
		/*
		 * if filter returns != 0, packet is not accepted => abort
		 */
		if ( filterBank.filter( packet ) ) {
			
			/*
			 * print out in a nice format:
			 * 		received at (time stamp) - i*(sampling period)
			 * 		nodeID (IPv6 address)
			 *      period
			 *      packet count
			 *      sample count (within packet)
			 * 		X value
			 * 		Y value
			 * 		Z value
			 * 
			 *                       ..... time stamp
			 *                         |   .. nodeID
			 *                         |   |  ... period
			 *                         |   |   |  ... packet count
			 *                         |   |   |   |  ... sample count (within packet)
			 *                         |   |   |   |   |  ... x value
			 *                         |   |   |   |   |   |  ... y value
			 *                         |   |   |   |   |   |   |  ... z value
			 *                         v   v   v   v   v   v   v   v  */				
			outputFormatter.format("%012d %s %4d %4d %4d %4d %4d %4d\n", packet.getReceivedAt(), packet.getNodeID(), packet.getSamplingPeriod(), packet.getPacketCount(), packet.getSampleInPacket(), packet.getValueX(), packet.getValueY(), packet.getValueZ());
			System.out.print("*");
		} else {
			System.out.print("!");
		}
	}
}
