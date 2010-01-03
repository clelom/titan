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
 * @author Fabian Schenkel
 */

configuration nrf2401ActiveMessageC {
  provides {
    interface Init;
    interface AMSend[uint8_t];
    interface Receive[uint8_t];
    interface Receive as Snoop[uint8_t]; // NOT IMPLEMENTED
    interface AMPacket;
    interface PacketAcknowledgements;
    interface SplitControl;
    interface Packet;
  }
}

implementation {

  components nrf2401C, nrf2401StubP;
  
  Init     = nrf2401C;
  AMSend   = nrf2401C;
  Receive  = nrf2401C;
  AMPacket = nrf2401C;
  Packet   = nrf2401C;
  
  // these are not implented interfaces
  Snoop    = nrf2401StubP;
  PacketAcknowledgements = nrf2401StubP;
  SplitControl = nrf2401StubP;
  


}
