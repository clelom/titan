package filters;
import server.XYZPacket;


/**
 * Filter for packets, filters out all packets which came too late
 * @author Benedikt Kšppel
 */
public class TimeFilter implements Filter<XYZPacket> {
	
	/**
	 * time when the server started
	 */
	long startTime=0;
	
	/**
	 * in this time period, all packets are accepted
	 */
	long windowLength=0;
	
	/**
	 * enable / disable debugging
	 */
	boolean debug = false;
	
	/**
	 * Constructor for TimeFilter
	 * sets debugging to false
	 */
	public TimeFilter() {
		this.debug = false;
	}
	
	/**
	 * Constructor for TimeFilter
	 * @param debug enable or disable debugging
	 */
	public TimeFilter( boolean debug ) {
		this.debug = debug;
	}
	
	/**
	 * initializes the filter
	 * @param startTime time when the server started
	 * @param windowLength in this time period, all packets are accepted
	 */
	public void init( long startTime, long windowLength ) {
		this.startTime = startTime;
		this.windowLength = windowLength;
		System.out.println("startTime is " + startTime + " and windowLength is " + windowLength );
	}

	/* (non-Javadoc)
	 * @see Filter#filter(java.lang.Object)
	 * filters packets out, if they came in too late
	 */
	public boolean filter(XYZPacket p) {
		
		/*
		 * (number of received packets) * (period for each sample) * (number of samples per packet) after startTime, plus a window.
		 * p.getRecievedAt is in milliseconds
		 * p.getCount is a number >= 0
		 * p.getSamplesPerPacket is a number >= 0
		 * p.getPeriod is a number in milliseconds
		 * startTime is in milliseconds
		 * => windowLength should be in milliseconds too
		 */
		if (  p.getReceivedAt()  <  p.getPacketCount() * p.getSamplingPeriod() * p.getSamplesPerPacket() + startTime + windowLength  ) {
			return true;
		} else {
			if ( debug ) {
				System.out.println("Packet " + p.getPacketCount() + ", " + p.getSampleInPacket() + " not accepted - too late. Packet received at " + p.getReceivedAt() + ", timeline was " + (p.getPacketCount() * p.getSamplingPeriod() + startTime + windowLength) );
			}
			return false;
		}
	}
}
