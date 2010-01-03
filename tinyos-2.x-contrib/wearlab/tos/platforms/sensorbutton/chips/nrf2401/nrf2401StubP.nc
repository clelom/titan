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
 * @author Andreas Bubenhofer
 * @author Christoph Walser
 */

module nrf2401StubP {
  provides {
    interface Receive as Snoop[uint8_t]; // NOT IMPLEMENTED!!!
    interface PacketAcknowledgements;
    interface SplitControl;
  }
  
}

implementation {


  /* ********************************************************************************************************** */
  /* Snoop Interface stub */
  /*TODO: THIS HAS NOT BEEN IMPLEMENTED */

  command void* Snoop.getPayload[uint8_t](message_t* msg, uint8_t* len ) {
    return NULL;
  }
  
  command uint8_t Snoop.payloadLength[uint8_t](message_t* msg ) {
    return 0;
  }

  /* ********************************************************************************************************** */
  /* PacketAcknowledgements Interface stub */

  async command error_t PacketAcknowledgements.noAck(message_t *msg) {
    return FAIL; // no support
  }
  
  async command error_t PacketAcknowledgements.requestAck(message_t *msg) {
    return FAIL; // no support
  }
  
  async command bool PacketAcknowledgements.wasAcked(message_t *msg) {
    return FALSE; // no support
  }
  
  /* ********************************************************************************************************** */
  /* SplitControl Interface stub */
  /* Implemented for completeness and working operation */
  
  task void sendStart() {
    signal SplitControl.startDone(SUCCESS);
  }
  
  task void sendStop() {
    signal SplitControl.stopDone(SUCCESS);
  }
  
  command error_t SplitControl.start() {
    post sendStart();
    return SUCCESS; // always on
  }
  
  command error_t SplitControl.stop() {
    post sendStop();
    return SUCCESS;
  }
  
}
