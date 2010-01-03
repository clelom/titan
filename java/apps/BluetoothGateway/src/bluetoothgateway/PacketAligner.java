/*
 * PacketAlignmer.java
 *
 */

package bluetoothgateway;

import java.io.InvalidClassException;
import java.util.Date;
import java.util.Vector;

/**
 * This class realigns DataPackets for continuous streams. This evens out bursts
 * of packets occurring during wireless transmission.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class PacketAligner {

   private static final int TIMESTAMPINDEX = 0;
   
   /**
    * Takes a list of packets and realigns all packets between the first and 
    * last packet to be in a line. This assumes that the first value of the 
    * datapacket is a sequence number.
    * 
    * @param datapackets
    */
   
   public static void realign(DataPacket [] datapackets) {
      
      // must contain at least 3 items
      if (datapackets == null) return;
      if (datapackets.length <=2) return;

      try {
         long firstTimestamp = datapackets[0].getTimestamp();
         long lastTimestamp  = datapackets[datapackets.length - 1].getTimestamp();
         int firstSequenceNr = datapackets[0].getIntValue(TIMESTAMPINDEX);
         int lastSequenceNr  = datapackets[datapackets.length - 1].getIntValue(TIMESTAMPINDEX);
         
         /////////////////////////////////////////////////////////////
         // Perform some checks on the timestamps and sequence numbers

         long periodTimestamps = lastTimestamp  - firstTimestamp;
         long periodSequenceNr = lastSequenceNr - firstSequenceNr;
         
         if (periodSequenceNr < 0) {
            // assume there has been only one overflow
            //TODO: handle sequence number overflows more detailed
            lastSequenceNr += datapackets[0].getMaxValue(TIMESTAMPINDEX);
         } if (periodSequenceNr < datapackets.length-1) {
            //TODO: handle out of sequence packets right
            System.out.println("WARNING: PacketAligner: found out-of-sequence packets. Not aligning packets");
            return;
         }
         
         if ( periodTimestamps <= 0 ) {
            System.out.println("WARNING: PacketAligner: timestamps out of sequence. Not aligning packets");
            return;
         }

         /////////////////////////////////////////////////////////////
         // No realign the packet data

         for (int i=1; i<datapackets.length-1; i++) {
            long curSequenceNr= datapackets[i].getIntValue(TIMESTAMPINDEX);
            long newTimestamp = firstTimestamp + (curSequenceNr-firstSequenceNr)*periodTimestamps/periodSequenceNr;
            
            datapackets[i].setTimestamp(new Date(newTimestamp));
         }
         
      } catch (IndexOutOfBoundsException ex) {
         System.out.println("ERROR: PacketAligner: DataPacket does not contain any data");
      } catch (InvalidClassException ex) {
         System.out.println("ERROR: PacketAligner: Could not obtain sequence number");
      }
   }
   
   
   /**
    * Test code
    */
   public static void main(String [] args) {
      System.out.println("Testing PacketAligner");
      
      Vector vdatapackets = new Vector();
      
      String [] channels = {"1","2","3"};
      Vector vp0 = new Vector(1); vp0.add(new Integer(1)); vdatapackets.add(new DataPacket(new Date(1000), vp0, "a", channels));
      Vector vp1 = new Vector(1); vp1.add(new Integer(2)); vdatapackets.add(new DataPacket(new Date(4000), vp1, "a", channels));
//      Vector vp2 = new Vector(1); vp2.add(new Integer(3)); vdatapackets.add(new DataPacket(new Date(4100), vp2, "a", channels));
      Vector vp3 = new Vector(1); vp3.add(new Integer(4)); vdatapackets.add(new DataPacket(new Date(4200), vp3, "a", channels));
      Vector vp4 = new Vector(1); vp4.add(new Integer(5)); vdatapackets.add(new DataPacket(new Date(4300), vp4, "a", channels));
      Vector vp5 = new Vector(1); vp5.add(new Integer(6)); vdatapackets.add(new DataPacket(new Date(6000), vp5, "a", channels));
      
      DataPacket [] datapackets = new DataPacket[vdatapackets.size()];
      for (int i=0; i<vdatapackets.size();i++) {
         datapackets[i] = (DataPacket)vdatapackets.get(i);
      }

      try {
         System.out.println("Before Alignment:");
         for(int i=0; i<datapackets.length; i++) {
            System.out.println("Packet " + i + " at " + datapackets[i].getTimestamp() + "\t seqnr " + datapackets[i].getIntValue(TIMESTAMPINDEX));
         }

         PacketAligner.realign(datapackets);

         System.out.println("");
         System.out.println("After Alignment:");
         for(int i=0; i<datapackets.length; i++) {
            System.out.println("Packet " + i + " at " + datapackets[i].getTimestamp() + "\t seqnr " + datapackets[i].getIntValue(TIMESTAMPINDEX));
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   
}
