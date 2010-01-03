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

package titancommon.tasks;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 * Filewriter acts similar as the Sink task but it writes the received data
 * into a file. The file is named like "20091012-161954.txt".
 * 
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class TitanFileWriter extends Task {
	
	public final static String NAME = "titanfilewriter";
	public final static int    TASKID = 34;
	
	protected String filename;
	
	public TitanFileWriter() {}
	
	public TitanFileWriter( TitanFileWriter f ) {
		super(f);
		filename = f.filename;
	}
	
	
	public Object clone() {
		return new TitanFileWriter(this);
	}


	/**
         * This has to return the configuration
         * @param maxBytesPerMsg
         * @return
         */
        public short[][] getConfigBytes(int maxBytesPerMsg) {
                /*
                 * it is kind of ugly... Java can't convert a Char-Array to a Short-Array
                 */
                short[] config_file = new short[filename.length()];
                char[] filename_c = filename.toCharArray();
                
                for( int i=0; i<filename.length(); i++) {
                    config_file[i] = (short)filename_c[i];
                }

                // return the filename
                short[][] config = {config_file};
                return config;
        }


	public int getID() {
		return TASKID;
	}


	public int getInPorts() {
		return 127;	// TODO: Is this correct?
	}


	public String getName() {
		return NAME.toLowerCase();
	}


	public int getOutPorts() {
		return 0;
	}


	public boolean setConfiguration(String[] strConfig) {

                /* TODO: if file exists, create a new filename ..._1.txt, ..._2.txt etc.
                 * TODO: do some replacement, e.g. replace %DATE% by actual date... etc.
                 */
                if ( strConfig == null || strConfig.length == 0 ) {
                        System.out.println("No configuration for TitanFileWriter");

                        // We take the current date and time as filename
			Date now = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
			filename = dateFormat.format(now) + ".txt";

                        return true;
                }  else if ( strConfig.length > 1 ) {
                        System.out.println("Wrong configuration for TitanFileWriter");
			return false;
		} 
		
		filename = strConfig[0].toString();
		return true;
	}
	
	/**
	 * Getter for the filename.
	 */
	public String getFilename() {
		return filename;
	}
        
}