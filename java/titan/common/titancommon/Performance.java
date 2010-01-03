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
 * This class provides functionality to collect performance data related to the 
 * execution on the mobile phone.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */

package titancommon;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;

/**
 *
 * @author lclemens
 */
public class Performance {

    static PrintStream      m_output;
    static String FILENAME = "/titanperf.log";
    static boolean m_bLogging = true;
    static Date m_StartTime;
    
    /**
     * Returns a string with iDigits bytes representing the number given. This relates to printf("%03f",iNumber) with zeroPadding=true and iDigits=3.
     * @param iNumber  Number to convert
     * @param iDigits  Digits to represent
     * @param zeroPadding fill in leading zeros
     * @return string with converted number
     */
    private static String printDigits( long iNumber, int iDigits, boolean zeroPadding) {
        byte [] converted = new byte[iDigits];
        long curNumber = iNumber;
        
        // step from LSB to MSB
        for (int i=0; i< converted.length; i++) {
            if ( curNumber > 0 ) {
                converted[converted.length-1-i] = (byte) ('0' + curNumber % 10);
                curNumber /= 10;
            } else {
                converted[converted.length-1-i] = (byte) (zeroPadding ? '0' : ' ');
            }
        }
        return new String(converted);
    }
    
    public static void printEvent(String strEvent) {
        if (m_bLogging) {
            Date now = new Date();

            if (m_output == null) {
                try {
                    m_output = new PrintStream(new FileOutputStream("perf.log"));
                } catch (FileNotFoundException ex) {
                    System.err.println("Performance: Could not open performance log");
                }
            }

            m_output.println( printDigits( now.getTime(), 12, true) + ";" + strEvent );
            m_output.flush();
        }
    }
    
    
    public static void begin(String strEvent) {
        if (m_bLogging) {
            printEvent("begin; " + strEvent);
            m_StartTime = new Date();
        }
    }
    
    public static void end(String strEvent) {
        if (m_bLogging) {
            Date endTime = new Date();
            if ( m_StartTime != null ) {
               printEvent("end  ; " + strEvent + "; " + (endTime.getTime()-m_StartTime.getTime()));
            } else {
               printEvent("end  ; " + strEvent + "; ");
            }
        }
    }
    
    
    
    
}
