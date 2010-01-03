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
 * TitanDataCom.java
 * 
 * Handles the data communication coming from the Titan sensor network. It opens 
 * a server TCP port to which it will forward data. Multiple connections are supported.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titancommon;

import java.io.*;
import java.util.*;

import titancommon.messages.DataMsg;

public class TitanDataCom implements Observer {
    
    FileOutputStream m_OutFile; ///< Log file for the incoming data
    PrintStream m_OutPrint; ///< Print stream associated with m_OutFile
    Date m_startTime;

    private Map/*<Integer,TitanDataServer>*/ m_tcpPorts = new HashMap();
    
    /**
     * Constructs a TitanDataCom object.
     * @param bLogging Saves all received data into a logfile named 'datacom.log'
     */
    public TitanDataCom( boolean bLogging )
    {
        
        // create logfile if needed
        if (bLogging) {
            try{
                m_OutFile = new FileOutputStream("datacom.log");
                m_OutPrint= new PrintStream( m_OutFile );
            } catch (Exception e){
                System.err.println("Error opening 'datacom.log'");
            }
        }
    }
    
    /**
     * Closes the file if the object is closed
     */
    protected void finalize()
    {
        m_OutPrint.close();
        try{m_OutFile.close();}catch(java.io.IOException e){}
    }
    
    /**
     * Returns a string with iDigits bytes representing the number given. This relates to printf("%03f",iNumber) with zeroPadding=true and iDigits=3.
     * @param iNumber  Number to convert
     * @param iDigits  Digits to represent
     * @param zeroPadding fill in leading zeros
     * @return string with converted number
     */
    protected String printDigits( long iNumber, int iDigits, boolean zeroPadding) {
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
    
    /**
     * Processes a message received over Titan
     * @param iPort The port to which the message should be delivered
     */
    public void processMessage( short iPort, byte[] msg ) {
        
        if (m_startTime==null) m_startTime = new Date();
        
        Date recvTime = new Date();
        
        String strMessage = printDigits( recvTime.getTime()-m_startTime.getTime(), 12, true) + " " + iPort + " ";
        for (int i = 0; i < msg.length; i++)
        {
            strMessage +=  printDigits( (((int)msg[i])&0xFF), 3,true) + " ";
        }
        strMessage += "\r\n";
        if ( m_OutPrint != null ) m_OutPrint.print(strMessage);
        
        //System.out.print(strMessage);
        
        // check whether the port already exists, and send message
        if( m_tcpPorts.containsKey(new Integer(iPort)) ) {
            ((TitanDataServer)m_tcpPorts.get(new Integer(iPort))).send( strMessage );
        } else {
            TitanDataServer newPort = new TitanDataServer(iPort);
            newPort.start();
            m_tcpPorts.put( new Integer(iPort), newPort );
            newPort.send( strMessage );
        }
        
    }

    /**
     * Called when a message is received by TitanCommand
     * @param source The source from which the update comes
     * @param obs    Argument to the notify call
     */
    public void update(Observable source, Object obs) {
        if ( obs instanceof DataMsg ) {
            DataMsg dmsg = (DataMsg)obs;
            processMessage(dmsg.destPort,dmsg.data);
        }
    }
    
    
}
