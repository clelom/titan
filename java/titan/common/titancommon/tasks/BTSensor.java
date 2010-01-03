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
import java.util.Vector;
import titancommon.util.StringUtil;

/**
 * BTSensor.java
 *
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 *
 * BTSensor connects and re-connects to a Bluetooth Sensor and collects data from it
 *
 */

public class BTSensor extends Task {

    public final static String NAME   = "btsensor";
    public final static int    TASKID = 35;

    private String address;
    private String dxstring;


    public BTSensor() {}

    public BTSensor(BTSensor bs) {
    	super(bs);
        address = bs.address;
        dxstring = bs.dxstring;
    }


    public String getName() {
        return NAME.toLowerCase();
    }


    public int getID() {
        return TASKID;
    }


    public Object clone() {
        return new BTSensor(this);
    }


    public int getInPorts() {
        return 0;
    }


    /**
     * @return returns 2, because we have two outputs: output 0 is for data, output 1 for status
     */
    public int getOutPorts() {
        return 2;
    }


    /**
     * the configuration is as follows
     *  * first parameter: mac address (withouth spaces or dashes)
     *  * second parameter: DX-string (e.g. DX3;cc-s-s-s-s-s-s), see titancommon.bluetooth.FrameParser
     */
    public boolean setConfiguration(String[] strConfig) {

            if ( strConfig == null || strConfig.length == 0 ) {
                System.out.println("No configuration for BTSensor");
                return false;
            } else if ( strConfig.length == 1 ) {
                dxstring = "DX3;c-s-s-s";
                address = strConfig[0].toString();
                System.out.println("No DX string given. Using DX3;c-s-s-s");
                
            } else if ( strConfig.length == 2 ) {
                address = strConfig[0].toString();
                dxstring = strConfig[1].toString();
                
            } else {
                System.out.println("Too many arguments for BTSensor");
                return false;
            }

            System.out.println("Address is " + address + " | DX-String is " + dxstring );

            return true;
    }


    public short[][] getConfigBytes(int maxBytesPerMsg) {

        /*
         * it is kind of ugly... Java can't convert a Char-Array to a Short-Array
         * 
         */
        short[] config_address = StringUtil.toShortArray(address);
        short[] config_dxstring = StringUtil.toShortArray(dxstring);

        Vector temp = new Vector();
        for( int i=0; i<config_address.length; i++ ) {
            temp.add(new Short(config_address[i]));
        }
        temp.add(new Short((short)'\0'));
        for( int i=0; i<config_dxstring.length; i++ ) {
            temp.add(new Short(config_dxstring[i]));
        }
        temp.add(new Short((short)'\0'));
        
        short[] config_both = new short[ temp.size() ];
        for( int i=0; i<temp.size(); i++) {
            config_both[i] = ((Short)temp.elementAt(i)).shortValue();
        }


        // return the filename
        short[][] config = {config_both};
        return config;



    }

}
