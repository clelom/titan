/**
 * BluetoothDeviceDiscoveryThread.java
 * 
 */

package bluetoothgateway.bluetooth;

import java.util.Observable;
import java.util.Observer;
import javax.bluetooth.*;

/**
 * This class wraps the Bluetooth discovery process. It starts a thread and 
 * reports any findings.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class DeviceDiscoveryThread extends Observable implements Runnable, DiscoveryListener {

   // Bluetooth discovery agent
   private DiscoveryAgent m_discoveryAgent;
   
   public DeviceDiscoveryThread(Observer obs) {
      addObserver(obs);
   }
   
	/**
    * Thread function - here we do everything
    */
	public void run() {
			
         // connect to the bluetooth device
			LocalDevice localDevice;
			try {
				localDevice = LocalDevice.getLocalDevice();
			}
			catch (BluetoothStateException e) {
            setChanged();
            notifyObservers(new String("DeviceDiscoveryThread: could not get local device: '" + e.toString() + "'"));
				return;
			}

      // retrieve the discovery agent and start a GIAC Inquiry 
      // (General/Unlimited Inquiry Access Code)
		try {
				// start an Inquiry with GIAC : General/Unlimited Inquiry Access Code 
   			m_discoveryAgent = localDevice.getDiscoveryAgent();
            System.out.println("Bluetooth Discovery Thread: Started Inquiry");
				m_discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
			}
			catch (BluetoothStateException e) {
            setChanged();
            notifyObservers(new String("DeviceDiscoveryThread: could not start inquiry: '" + e.toString() + "'"));
			}
	}
   
   /**
    * Stops a running inquiry. If the inquiry had not been started, it returns true.
    */
   public boolean stop() {
      return (m_discoveryAgent != null)? m_discoveryAgent.cancelInquiry(this) : true;
   }
	

	/** 
    * Called when a device is found during an inquiry. 
    */
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {

      BTDeviceInfo btdi = new BTDeviceInfo();

      try {
         btdi.name = btDevice.getFriendlyName(false);
      } catch (java.io.IOException e) {
         btdi.name = null;
      }
      
      btdi.address = btDevice.getBluetoothAddress();
      
      setChanged();
      notifyObservers(btdi);
	}
   
   /**
    * Reports all devices within the discovery cache
    */
   public void reportKnownDevices() {
      
      if (m_discoveryAgent == null ) {
			LocalDevice localDevice;
			try {
				localDevice = LocalDevice.getLocalDevice();
            m_discoveryAgent = localDevice.getDiscoveryAgent();
			}
			catch (BluetoothStateException e) {
				return;
			}
      }
      
      // get previously known devices
      RemoteDevice [] devices = m_discoveryAgent.retrieveDevices(DiscoveryAgent.PREKNOWN);
      if (devices == null) {
         System.out.println("DeviceDiscoveryThread: no preknown devices returned");
      } else {
         for(int i=0; i<devices.length; i++) {
            deviceDiscovered(devices[i], null);
         }
      }
   	
      // get cached devices
      devices = m_discoveryAgent.retrieveDevices(DiscoveryAgent.CACHED);
      if (devices == null) {
         System.out.println("DeviceDiscoveryThread: no cached devices returned");
      } else {
         for(int i=0; i<devices.length; i++) {
            deviceDiscovered(devices[i], null);
         }
      }

   }
   
   
	/** 
    * Called when service(s) are found during a service search. 
    */
	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
	}
   
	/** 
    * Called when a service search is completed or was terminated because of an error. 
    */
	public void serviceSearchCompleted(int transID, int respCode) {
	}
   
	/** Called when an inquiry is completed */
	public void inquiryCompleted(int discType) {
		String text = null;
		
		if ( discType == DiscoveryListener.INQUIRY_COMPLETED )
			text = "Bluetooth Inquiry Response Code:INQUIRY_COMPLETED";
		else if(discType == DiscoveryListener.INQUIRY_ERROR )
			text = "Bluetooth Inquiry Response Code:INQUIRY_ERROR";
		else if(discType == DiscoveryListener.INQUIRY_TERMINATED )
			text = "Bluetooth Inquiry Response Code:INQUIRY_TERMINATED";

      // Report outcome
      RemoteDevice [] devices = m_discoveryAgent.retrieveDevices(DiscoveryAgent.CACHED);
		System.out.println(text + ": found " + devices.length + " devices");
		
		stop();
	}

}
