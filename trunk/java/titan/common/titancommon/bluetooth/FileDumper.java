/*
 * FileDumper.java
 *
 */

package titancommon.bluetooth;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;
import java.util.TimeZone;
import java.util.Vector;

/**
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class FileDumper implements Observer {

   PrintStream m_file;
   
   public FileDumper() {
		/*
		 * print stream for file output
		 * generate a file 'datastream_yyyyMMdd_HHmmss.txt'
		 */
		Date startTime;
		startTime = new Date();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String logFilename = "datalog_" + dateFormat.format(startTime) + ".txt";
      try {

         m_file = new PrintStream(new FileOutputStream(logFilename));
      } catch (FileNotFoundException ex) {
         System.out.println("ERROR: FileDumper: Could not create file");
         ex.printStackTrace();
      }
   }

   public void update(Observable obs, Object arg) {
         if ( arg instanceof BTDataPacket ) {
            BTDataPacket dp = (BTDataPacket)arg;
            m_file.print(dp);
            //System.out.println(dp);
            // write newline manually to see whether socket is open
            try {
               byte [] newline = {'\r','\n'};
               m_file.write(newline);
            } catch(IOException e) {
               m_file.close();
               System.out.println("Error: FileDumper IOException, closing file.");
               e.printStackTrace();
            }

         } else {
            System.out.println("TCPServer.Connection: Unknown update received");
         }
   }
   
   /**
    * Test code: creates a file and drops one packet content into it.
    * @param args
    */
   public static void main(String [] args) {
      FileDumper fd = new FileDumper();
      
      Vector v = new Vector();
      v.add(new Integer(  1));
      v.add(new Short(  (short)2));
      v.add(new Character(  'w'));
      v.add(new Integer(  4));
      
      Date now = new Date();
      String [] channels = {"1","2","3"};
      BTDataPacket dp = new BTDataPacket(new Date(now.getTime() +  200), v, "a",channels);

      fd.update(null, dp);
   }
}
