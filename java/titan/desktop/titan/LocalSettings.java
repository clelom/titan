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
package titan;

/**
 * This class contains settings like file system references that need to be 
 * adapted to the system where Titan is running on. This is somehow not a 
 * very clean solution and should be changed at some point of time.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * TODO: Replace functions in this class with some nicer solution 
 */
public final class LocalSettings {

    /**
     * This function sets together a string starting a python console 
     * in the directory given by strPath. It can be run using
     * Runtime.getRuntime().exec()
     * 
     * @param strPath The path to the directory where the python console is opened
     * @return The execution string for Runtime.getRuntime.exec()
     */
    public static String getPythonStartString(String strPath) {
        return "cmd.exe /C \"C:\\cygwin\\bin\\bash.exe --login -i -c 'cd \""+strPath+"\";python -u -i'\"";
    }

    /**
     * Returns a string with the path to the Titan TinyOS directory, where the Tossim library 
     * can be found.
     * 
     * @return Tossim library path (without filename)
     */
    public static String getTitanTossimPath() {
        return "C:\\cygwin\\opt\\tinyos-2.x-contrib\\wearlab\\apps\\TestTitan";
    }

    /**
     * Returns a string specifying the connection to the WSN used by the 
     * TinyOS BuildSource factory.
     * 
     * @return MOTECOM style connection string
     */
    public static String getMOTECOM() {
       return "serial@COM20:telosb";
    }
    
    public static String getBluetoothCOM() {
    	return "COM26";
    }
}
