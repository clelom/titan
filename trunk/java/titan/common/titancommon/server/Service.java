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
 * Keeps a service and notifies whether it has is possible with the given service set.
 * For the service configuration string, see the ServiceComposer class file
 *  
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date 13 July 2008
 * 
 */

package titancommon.server;

public class Service {
	
	String m_strConfig;
	String m_strName;
	int    m_ServiceUID;
	int [] m_services;
	public String[] resources;
        
        /**
         * Splits a string around matches of the given delimiter
         * @param str the string to be split
         * @param split the delimiter
         * @return the array of strings computed by splitting the string around matches of the given delimiter
         */
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
                    result[result.length-1] = str.substring(iCurIndex+split.length(), str.length());//cl: str.length()-1
            }

            return result;
        }
	/** Initializes a service by parsing the format string. Throws an Exception 
	 * if format errors are detected.
	 */
	public Service(String servConfig) throws Exception {
		m_strConfig = servConfig.trim();
		m_strConfig = m_strConfig.replace('\n', ' ').replace('\r',' ');
		
		// parse configuration for
		int indexEq = m_strConfig.indexOf("=");
		int indexPO = m_strConfig.indexOf("[");
		int indexPC = m_strConfig.indexOf("]");
		if (indexEq == -1 || indexPO == -1 || indexPC == -1) throw new Exception("Illegal service format");
		
		m_ServiceUID = Integer.parseInt(servConfig.substring(0,indexEq).trim());
		String [] params = split(m_strConfig.substring(indexPO+1,indexPC),","); //cl: indexPC-1

		m_strName = params[0].replace('\"', ' ').trim();
		
		resources = new String[params.length-2];
		for (int i=2; i<params.length; i++) {
			resources[i-2] = params[i].replace('\"',' ').trim();
		}
			
		// extract required service indexes
		String [] serviceIDs = split(params[1].trim().substring(1,params[1].trim().length()-1)," ");
		m_services = new int[serviceIDs.length];
		for (int i=0; i<serviceIDs.length; i++ ) {
                        try {
                            m_services[i] = Integer.parseInt(serviceIDs[i].trim());
                        } catch(NumberFormatException e) {
                            System.out.println("Error when parsing \""+m_strConfig+"\": Number format of: \""  + serviceIDs[i] + "\"");
                        }
		}
		
		
	}
	
	public String getName() {
		return m_strName;
	}
	
	/**
	 * Checks whether the service is executable with the given number of services
	 * @param services List of services available for use
	 * @return true if all required services are given in services
	 */
	public boolean isExecutable( int [] services ) {
		
		if (m_services == null) return false;
		if (services == null ) return true;
		
		// go through each of the required services and check whether they are feasible
		for (int i=0; i<m_services.length; i++ ) {
			boolean bFound = false;
			for (int j=0; j<services.length; j++ ) {
				if ( m_services[i] == services[j] ){
					bFound = true;
					break;
				}
			}
			if (!bFound) return false; // required service not found in the set 
		}
		
		return true;
		
	}

	/**
	 * Returns a configuration string describing the service
	 */
	public String toString() {
		return m_strConfig;
	}

}
