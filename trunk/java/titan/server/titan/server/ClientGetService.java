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
 *
 * Client side for a ServiceServer. Downloads and starts a service from the 
 * server.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date 12 July 2008
 * 
 */

package titan.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import titan.applications.Application;

public class ClientGetService {

	/** Path to the local resoruce directory */
	private static String SERVICE_PATH = "titan/applications/";
	private static String RESOURCE_PATH = "cfg/";
	/** server location */
	private static String SERVER_URL = "http://localhost:9000/";
	
	public ClientGetService() {

	}
	
    private static String[] split(String str, String split) {
        
        // find occurrences
        int iCurIndex = 0;
        int iOccurrences = 0;
        while( (iCurIndex = str.indexOf(split, iCurIndex)+split.length()) >= split.length() ) iOccurrences++;
        
        String [] result = new String[iOccurrences+1];
        
        if (result.length==1) { 
        	result[0] = str; 
        	return result; 
        }
        
        iCurIndex = 0;
        for ( int i=0; i<iOccurrences; i++) {
            if (i==0) {
                int iNextIndex = str.indexOf(split);
                if (iNextIndex == -1) break;
                result[i] = str.substring( iCurIndex, iNextIndex );
                iCurIndex = iNextIndex+split.length()-1;
            } else {
                int iNextIndex = str.indexOf(split,iCurIndex+split.length());
                if (iNextIndex==-1) break;
                result[i] = str.substring( iCurIndex+split.length(), iNextIndex );
                iCurIndex = iNextIndex+split.length()-1;
            }
        }
        if ( iCurIndex+split.length() >= str.length() ) {
        	result[result.length-1] = "";
        } else {
        	result[result.length-1] = str.substring(iCurIndex+split.length(), str.length()-1);
        }
        
        return result;
    }

	
	/** writes the data received from the server into a file */
	private static boolean writeData(String strPath, byte [] data ) {
		try {
			File file = new File(strPath);
			FileOutputStream rcStream = new FileOutputStream(file);

			rcStream.write( data );
			
			rcStream.close();
			
			return true;
		
		} catch (Exception e) {
			System.err.println("Could not write: "+strPath);
			return false;
		}
	
	}
	
	/**
	 * This function retrieves the available applications on the server, which 
	 * can be composed of the given services
	 * @param availServices List of services available to the network
	 * @return List of possible appliations and their data
	 */
	public static String [] getServices(int [] availServices ) {
		
		// compose POST request
		try {
			
			String strServices = "services=0";
			for (int i=0; i < availServices.length; i++) {
				strServices += "+"+availServices[i];
			}
			
			
			URL url = new URL(SERVER_URL);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
			writer.write(strServices);
			writer.flush();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			String strLine;
			List<Service> services = new ArrayList<Service>();
			while((strLine = reader.readLine()) != null) {
				try {
					services.add(new Service(strLine));
				} catch(Exception e) {
					// some service format problem
					System.err.println("Could not parse service line: \""+strLine+"\"");
				}
			}
			writer.close();
			reader.close();
	
			String [] strReturn= new String[services.size()];
			for (int i=0; i<strReturn.length;i++) {
				strReturn[i] = services.get(i).getName();
			}
			
			return strReturn;
		} catch (Exception e) { // return on any error
			e.printStackTrace();
			return null;
		}
	}
	
	private static boolean getServiceFile( String strServerPath, String strLocalPath ) throws Exception {

		URL url = new URL(strServerPath);
		URLConnection ucon = url.openConnection();
		ucon.getDoInput();
		InputStream input = ucon.getInputStream();
		
		byte [] data = new byte[ucon.getContentLength()];
		
		input.read(data);
		
		writeData(strLocalPath, data);
		
		return true;
	}
	
	
	/** Get service from the server */
	public static Application getService(String strService) {
		
		try {
			URL url = new URL(SERVER_URL + strService + ".srv" );
			InputStream input = url.openStream();
			
			URLConnection ucon = url.openConnection();
			ucon.connect();
			
			byte [] data = new byte[ucon.getContentLength()];
			
			input.read(data);
			
			Service newService = new Service(new String(data));
			
			// get the class file
			getServiceFile( SERVER_URL + strService + ".class", SERVICE_PATH + strService + ".class");
			
			// get all resource files
			for(int i=0; i < newService.resources.length; i++ ) {
				getServiceFile( SERVER_URL + newService.resources[i], RESOURCE_PATH + newService.resources[i] );
			}
			
			Application newApp = (Application)Class.forName("titan.applications." + strService).newInstance();
			return newApp;

		} catch (Exception e) {
			// Possible exceptions:
			// IOException, InstantiationException, IllegalAccessException, ClassNotFoundException
			e.printStackTrace();
			return null;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////
	// Test function - downloads and starts TestApp from the server.
	
	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {

		ClientGetService.split( "hello, there, how are you doing, now? test,gaga,,gool,", ",");
		
		
		// Test service composition
/*		System.out.println("Testing service composition:");
		int [] services = {1,2,3,4,5,6,7,8,9};
		String [] strApplications = ClientGetService.getServices( services );
		
		System.out.println("Applications found: ");
		for(int i=0; i<strApplications.length;i++) {
			System.out.println(strApplications[i]);
		}
		
		// Test service download
		System.out.println("\n\n\nTesting service download");
		Application ts = ClientGetService.getService("TestApp");
		if (ts != null) {
			ts.startApplication(null, null, null);
		}
*/

	}
		
}
