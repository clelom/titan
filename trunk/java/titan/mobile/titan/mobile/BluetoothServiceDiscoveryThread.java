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
 * BluetoothServiceDiscoveryThread.java
 * 
 * This Thread discovers all Bluetooth services from a specific device.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 */

package titan.mobile;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

public class BluetoothServiceDiscoveryThread extends Thread implements DiscoveryListener {
 	private DiscoveryAgent discoveryAgent;
	
           private UUID[] uuidSet={new UUID(0x1101)};

    private int[] attrSet = {0x0100, 0x0003, 0x0004};

	private RemoteDevice remoteDevice = null;
	
 	private boolean stop;
 	
 	private int m_iServiceCounter;


    public BluetoothServiceDiscoveryThread(RemoteDevice remoteDevice) {
		this.remoteDevice = remoteDevice;
	}
    
    
	public void run(){


            
		stop = false;
		
		m_iServiceCounter = 0;

		LocalDevice localDevice;
		try {
			localDevice = LocalDevice.getLocalDevice();
			
			discoveryAgent = localDevice.getDiscoveryAgent();
		} catch (BluetoothStateException e1) {
			// TODO Auto-generated catch block
			TitanMobile.getGUI().setDebugText(this.getClass().getName(), "run:getLocalDevice", e1.toString());
			TitanMobile.getGUI().discoveryStoped();
			return;
		}

		boolean nameAvailable = true;
		String name = "";
		try {
			name = remoteDevice.getFriendlyName(false);
		}
		catch(Exception e) {
			nameAvailable = false;
		}
		
		if(!nameAvailable || name.trim().equals("")) {
			name = remoteDevice.getBluetoothAddress();
		}
		
		newStringItem("Searching for Services on "+name + ":");

		try {
			discoveryAgent.searchServices(attrSet, uuidSet, remoteDevice, this);
		} catch (BluetoothStateException e1) {
			TitanMobile.getGUI().setDebugText(this.getClass().getName(), "run:getLocalDevice", e1.toString());
			TitanMobile.getGUI().discoveryStoped();
			return;
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


	public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
		String url;
		
		for(int i = 0; i < servRecord.length; i++) {
	        DataElement serviceNameElement = servRecord[i].getAttributeValue(0x0100);
			String temp_serviceName = (String)serviceNameElement.getValue();
			String serviceName = temp_serviceName.trim();
			
			TitanMobile.getGUI().appendGroup(serviceName);
			
			url = servRecord[i].getConnectionURL(ServiceRecord.AUTHENTICATE_ENCRYPT, false);
			
			TitanMobile.putBTCOMService(m_iServiceCounter, url);
			
			m_iServiceCounter++;
		}
	}


	public void serviceSearchCompleted(int transID, int respCode) {
		if (respCode == DiscoveryListener.SERVICE_SEARCH_DEVICE_NOT_REACHABLE) {
			newStringItem("Response Code: Device not reachable");
		}
		else if (respCode == DiscoveryListener.SERVICE_SEARCH_NO_RECORDS) {
			newStringItem("Response Code: No services available");
		}
		else if (respCode == DiscoveryListener.SERVICE_SEARCH_COMPLETED) {
			newStringItem("Response Code: Service search completed");
		}
		else if (respCode == DiscoveryListener.SERVICE_SEARCH_TERMINATED) {
			newStringItem("Response Code: Service search terminated");
		}
		else if (respCode == DiscoveryListener.SERVICE_SEARCH_ERROR) {
			newStringItem("Response Code: Service search error");
		}
		
		TitanMobile.getGUI().discoveryStoped();
		
		stop = true;
	}


    public void inquiryCompleted(int discType){}


    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod){}

	
	private void newStringItem(String str) {
		System.out.println(str);
	}
}
