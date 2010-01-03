/*
 * BTDeviceInfo.java
 */

package titancommon.bluetooth;

/**
 * This class stores information about the devices found.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class BTDeviceInfo {
   public String name;
   public String address;
   
   public String getConnectionString() {
      return "btspp://"  + address + ":1;authenticate=true;encrypt=true;master=false";      
   }
}
