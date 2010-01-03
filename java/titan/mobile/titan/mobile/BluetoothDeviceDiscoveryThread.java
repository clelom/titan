/*
    This file is part of Titan.

    Titan is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Titan is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Titan. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * BluetoothDeviceDiscoveryThread.java
 * 
 * This Thread discovers all Bluetooth devices in the environment.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 */

package titan.mobile;



import javax.bluetooth.*;

public class BluetoothDeviceDiscoveryThread implements Runnable ,DiscoveryListener {
	// Bluetooth discovery agent
            private DiscoveryAgent discoveryAgent;
 	private boolean stop;
 	
 	private int m_iDeviceCounter;
	
	public void run() {
			stop = false;
			
			m_iDeviceCounter = 0;
			
			LocalDevice localDevice;
			try {
				localDevice = LocalDevice.getLocalDevice();
			}
			catch (BluetoothStateException e) {
				//TODO: error handling
				TitanMobile.getGUI().setDebugText(this.getClass().getName(), "run:getLocalDevice", e.toString());
				return;
			}

			// get the bluetooth discovery agent
			discoveryAgent = localDevice.getDiscoveryAgent();

			try {
				// start an Inquiry with GIAC : General/Unlimited Inquiry Access Code 
				discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
			}
			catch (BluetoothStateException e) {
				//TODO
				TitanMobile.getGUI().setDebugText(this.getClass().getName(), "run:startInquiry", e.toString());
			}
			
			
			while(!stop) {
				try{
					if(TitanMobile.getBTStop()) {
						discoveryAgent.cancelInquiry(this);
					}
					Thread.sleep(500);
				} catch(Exception e){
					TitanMobile.getGUI().setDebugText(this.getClass().getName(), "run:cancelInquiry", e.toString());
				}
			}
	}
	

	// ************************************************
	// Implement javax.bluetooth.DiscoveryListener interface
	// ************************************************
	/** Called when a device is found during an inquiry. */
	public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
		boolean nameAvailable = true;
		String name = "";
		
                // Add Device to a list
                TitanMobile.singleton.m_btDevices.put(new Integer(m_iDeviceCounter), btDevice);
                m_iDeviceCounter++;
                
		try {
			name = btDevice.getFriendlyName(false);         
		}
		catch (Exception e) {
			nameAvailable = false;
		}
		        
		if(!nameAvailable || name.trim().equals(""))
			name = btDevice.getBluetoothAddress();
		
		TitanMobile.getGUI().appendGroup(name);
	}

	/** Called when service(s) are found during a service search. */
	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
	}
	/** Called when a service search is completed or was terminated because of an error. */
	public void serviceSearchCompleted(int transID, int respCode) {
	}
	
	/** Called when an inquiry is completed */
	public void inquiryCompleted(int discType) {
		String text = null;
		
		if ( discType == DiscoveryListener.INQUIRY_COMPLETED )
			text = "Response Code:INQUIRY_COMPLETED";
			//BlueDump.display("Response Code:INQUIRY_COMPLETED");
		else if(discType == DiscoveryListener.INQUIRY_ERROR )
			text = "Response Code:INQUIRY_ERROR";
			//BlueDump.display("Response Code:INQUIRY_ERROR");
		else if(discType == DiscoveryListener.INQUIRY_TERMINATED )
			text = "Response Code:INQUIRY_TERMINATED";
			//BlueDump.display("Response Code:INQUIRY_TERMINATED");

		System.out.println(text);
		
		stop();
	}

	private void stop() {
		TitanMobile.getGUI().discoveryStoped();
		
		stop = true;
	}

	// ************************************************
}
