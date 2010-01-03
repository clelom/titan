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

package titancommon.services;

/**
 * This class handles keeps information about the value in the service 
 * directory database. It adjusts the value after each successful affirmation 
 * of still being available, and decrements it if no response is received after 
 * an inquiry.
 * 
 * The idea is shaped after the TCP fallback strategy on retransmission.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class InfoValue {
    private int m_Value;       ///< current value of the information
    private int m_lastNotice;  ///< sequence number of the last heard service affirmation
    private int m_iIsAlive; ///< messages received from the node since last notice
    
    private static int VALUE_INITIAL = 5;
    private static int VALUE_INCREMENT_ADD = 3;
    private static int VALUE_DECREMENT_DIV = 2;
    private static int DEC_AFTER_STEPS     = 5;
    private static int VALUE_MAX = 7;
    private static int VALUE_MIN = 1;
    private static int VALUE_MIN_MSG = 5;
    
    /**
     * Instantiates an InfoValue object
     * @param curNoticeSeqNumber Current service inquiry sequence number
     */
    InfoValue( int curNoticeSeqNumber ) {
        m_Value = VALUE_INITIAL;
        m_lastNotice = curNoticeSeqNumber;
        m_iIsAlive = 0;
    }
    
    /**
     * Checks the value of the information stored here, and gives an indication 
     * whether the entry should be kept or not.
     * 
     * This function is called everytime an inquiry period is over. In case the 
     * node did not answer during a certain period, the value will be reduced.
     * 
     * @param curNoticeSeqNumber The current number of the issued service inquiry 
     * @return true if the information is still useful enough
     */
    public boolean checkValue( int curNoticeSeqNumber ) {
        
        int diff = curNoticeSeqNumber - m_lastNotice;
        
        // is the information still up to date?
        // check whether we have missing sequence numbers, and whether 
        // we are at the periodic point to reduce the number
        if ( diff >= DEC_AFTER_STEPS && ((diff%DEC_AFTER_STEPS)==0) ) {
            
            // the decrement can be saved if enough messages have been received 
            // during the time
            if ( m_iIsAlive < VALUE_MIN_MSG ) {
                m_Value /= VALUE_DECREMENT_DIV;
            }
            m_iIsAlive = 0; // reset message counter
        }
        
        // return whether it is still useful
        return (m_Value > VALUE_MIN);
    }
    
    /**
     * Indicates an answer of a node. The value of the information increases
     * 
     * @param curNoticeSeqNumber The current number of the issued service inquiry
     */
    public void updateValue( int curNoticeSeqNumber ) {
        if (m_Value < VALUE_MAX) m_Value += VALUE_INCREMENT_ADD;
        m_lastNotice = curNoticeSeqNumber;
    }
    
    /**
     * Indicates that some information has been received that the node is still alive. 
     * This keeps the value of the node at a certain level.
     */
    public void isAlive() {
        m_iIsAlive++;
    }
    
    /**
     * Gives back the current value of the information on this node
     * @return current value
     */
    public int getValue() {
    	return m_Value;
    }
}
