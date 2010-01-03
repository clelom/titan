package filters;

import java.util.HashSet;

import server.XYZPacket;

/**
 * Filter for packets, filters out duplicate samples
 * @author Benedikt Kšppel
 */
public class DuplicateFilter implements Filter<XYZPacket> {
	
	/**
	 * a HashSet contains any Long only once.
	 * Adding the same value twice will fail.
	 */
	HashSet<Double> processedPackets = new HashSet<Double>();
	
	/**
	 * enable / disable debugging
	 */
	private boolean debug = false;
	
	/**
	 * Constructor for DuplicateFilter
	 * sets debugging to false
	 */
	public DuplicateFilter() {
		this.debug = false;
	}
	
	/**
	 * Constructor for DuplicateFilter
	 * @param debug enable or disable debugging
	 */
	public DuplicateFilter( boolean debug ) {
		this.debug = debug;		
	}

	
	/* (non-Javadoc)
	 * @see Filter#filter(java.lang.Object)
	 * filters packets out, if they were already processed (i.e. if they were already checked by this filter)
	 */
	public boolean filter(XYZPacket p) {
		
		/*
		 * packet-count is not unique, all samples of one packet have same UDP-packet count. Use packet-count AND p.getSampleInPackage
		 * check if the packet's count can be added to HashSet.
		 * If that fails, the packet was already processed.
		 */
		if ( processedPackets.add(  (double)p.getPacketCount() + (double)p.getSampleInPacket()/(double)p.getSamplesPerPacket() )  ) {

			/*
			 * adding packet's count to HashSet worked, so this packet is okay
			 */
			return true;
		} else {

			/*
			 * adding to HashSet failed, packet is bad
			 */
			if ( debug ) {
				System.out.println("Packet " + p.getPacketCount() + ", " + p.getSampleInPacket() + " (" + ((double)p.getPacketCount() + (double)p.getSampleInPacket()/(long)p.getSamplesPerPacket()) + ") not accepted - duplicate.");
			}
			return false;
		}
	}

}
