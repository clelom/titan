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
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 * 
 * The amType is set to AM_TITANCOMMCONFIGMSG = 13 by default.
 * 
 */
package titan.messages;

public class SerialMsg implements net.tinyos.message.Message {

    static private short GATEWAY_MESSAGE_END_CHAR = '>';
    static public short AM_TITANCOMMCONFIGMSG = 13;
    static public short AM_TITANCOMMDATAMSG = 42;
    static public short DEFAULT_MESSAGE_SIZE = 3;
    private short data[];
    private short length;
    private short address;
    private short type;
    private short rssi;

    /** Create a new SerialMsg of size 3. */
    public SerialMsg() {
        address = -1;
        length = 1;
        data = new short[1];
        data[0] = GATEWAY_MESSAGE_END_CHAR;
        type = AM_TITANCOMMCONFIGMSG;
    }

    /** Create a new SerialMsg of the given data_length. */
    public SerialMsg(int i) {
        address = -1;
        length = (short) i;
        data = new short[i];
        type = AM_TITANCOMMCONFIGMSG;

        for (int x = 0; x < i; x++) {
            data[x] = GATEWAY_MESSAGE_END_CHAR;
        }
    }

    /**
     * Return an element (as a short) of the array 'data'
     */
    public short getElement_data(int i) {
        // hole msgData array

        //TODO: beginnt i bei 1 an oder bei 0????
        return data[i];
    }

    public short get_rssi() {
        return rssi;
    }

    public void set_rssi(short newrssi) {
        rssi = newrssi;
    }

    /**
     * Set the value of the field 'length'
     */
    public void set_length(short s) {
        length = s;
    }

    /**
     * Set the value of the field 'length'
     */
    public short get_length() {
        return length;
    }

    /**
     * Set the contents of the array 'data' from the given short[]
     */
    public void set_data(short[] msgData) {
        // ganzen nutzdaten
        if (length <= msgData.length) {
            for (int index0 = 0; index0 < length; index0++) {
                data[index0] = msgData[index0];
            }
        }

    }

    /**
     * Set the value of the field 'address'
     */
    public void set_address(int i) {
        // adresse vom endknoten/empfaenger im netzwerk

        address = (short) i;
    }

    /**
     * Get the value of the field 'address'
     */
    public int get_address() {
        return address;
    }

    public String amType() {
        return null;
    }

    public void set_type(short t) {
        type = t;
    }

    public short get_type() {
        return type;
    }
}
