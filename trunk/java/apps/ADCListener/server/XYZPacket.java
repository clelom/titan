package server;
/**
 * Stores a sample with X, Y and Z values
 * @author Benedikt Kšppel
 */
public class XYZPacket {
	
	/**
	 * date/time when the packet was received, in milliseconds since 1970
	 */
	private long receivedAt;
	
	/**
	 * the ID of the node, which is the IPv6 address
	 */
	private String nodeID;
	
	/**
	 * the period between two samples
	 */
	private short samplingPeriod;
	
	/**
	 * the number of sent packets
	 */
	private short packetCount;
	
	/**
	 * this sample was the 'sampleInPacket'-ths sample in the received packet
	 */
	private short sampleInPacket;

	/**
	 * number of samples in each packet
	 */
	private short samplesPerPacket;
	
	/**
	 * sampled value in X direction
	 */
	private short valueX;
	
	/**
	 * sampled value in Y direction
	 */
	private short valueY;

	/**
	 * sampled value in Z direction
	 */
	private short valueZ;
	
	
	/**
	 * Constructor for XYZPacket
	 * @param receivedAt date/time when the packet was received, in milliseconds since 1970
	 * @param nodeID the ID of the node, which is the IPv6 address
	 * @param samplingPeriod the period between two samples
	 * @param packetCount the number of sent packets
	 * @param sampleInPacket this sample was the 'sampleInPacket'-ths sample in the received packet
	 * @param samplesPerPacket number of samples in each packet
	 * @param valueX sampled value in X direction
	 * @param valueY sampled value in Y direction
	 * @param valueZ sampled value in Z direction
	 */
	XYZPacket( long receivedAt, String nodeID, short samplingPeriod, short packetCount, short sampleInPacket, short samplesPerPacket, short valueX, short valueY, short valueZ ) {
		this.receivedAt = receivedAt;
		this.nodeID = nodeID;
		this.samplingPeriod = samplingPeriod;
		this.packetCount = packetCount;
		this.sampleInPacket = sampleInPacket;
		this.samplesPerPacket = samplesPerPacket;
		this.valueX = valueX;
		this.valueY = valueY;
		this.valueZ = valueZ;
	}
	
	/**
	 * Prints the packet in IfE's preferred format
	 */
	public void print() {
		System.out.printf("%012d %s %4d %4d %4d %4d %4d %4d\n", receivedAt, nodeID, samplingPeriod, packetCount, sampleInPacket, valueX, valueY, valueZ);
	}

	/**
	 * @return date/time when the packet was received, in milliseconds since 1970
	 */
	public long getReceivedAt() {
		return receivedAt;
	}

	/**
	 * @return the ID of the node, which is the IPv6 address
	 */
	public String getNodeID() {
		return nodeID;
	}

	/**
	 * @return the period between two samples
	 */
	public short getSamplingPeriod() {
		return samplingPeriod;
	}

	/**
	 * @return the number of sent packets
	 */
	public short getPacketCount() {
		return packetCount;
	}

	/**
	 * @return this sample was the 'sampleInPacket'-ths sample in the received packet
	 */
	public short getSampleInPacket() {
		return sampleInPacket;
	}

	/**
	 * @return sampled value in X direction
	 */
	public short getValueX() {
		return valueX;
	}

	/**
	 * @return sampled value in Y direction
	 */
	public short getValueY() {
		return valueY;
	}

	/**
	 * @return sampled value in Z direction
	 */
	public short getValueZ() {
		return valueZ;
	}

	/**
	 * @return number of samples in each packet
	 */
	public short getSamplesPerPacket() {
		return samplesPerPacket;
	}

}
