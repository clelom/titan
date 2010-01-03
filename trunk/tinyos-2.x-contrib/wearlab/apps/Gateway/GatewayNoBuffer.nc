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
 *  Implementation of the gateway functionality. Stripped down to a Bluetooth-802.15.4 interface
 *
 *  @author Christoph Walser
 *  @author Fabian Schenkel
 *  @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *  @author Tonio Gsell <tgsell@ee.ethz.ch>
 *  @date   April 8 2008
 *
 **/
 
#include "Gateway.h"
 
module Gateway {
  uses {
    interface Boot;
    interface Leds;
    
    interface AddressResolution as AddrRes;
  	
    // Radio interfaces

    // Bluetooth LMX
    interface Init     as lmxInit;
  	interface AMSend   as lmxAMSend;
  	interface Receive  as lmxReceive;
    interface AMPacket as lmxPacket;
    
    // CC2420
    interface SplitControl as ccSplitControl; // needed for shared UART
  	interface AMSend   as ccAMSendConfig;
  	interface Receive  as ccReceiveConfig;
  	interface AMSend   as ccAMSendData;
  	interface Receive  as ccReceiveData;
    interface AMPacket as ccPacket;
    
    interface PacketLink   as ccPacketLink;
	}
  
}

implementation {
  
  bool cc_locked = FALSE;
  bool lmx_locked = FALSE;
	
  // to be found in TitanComm.h
  const uint8_t TITANCOMM_VERSION = 1;
  const uint8_t TITANCOMM_CONFIG = 0;
  const uint8_t AM_TITANCOMMCONFIGMSG = 13;
  const uint8_t AM_TITANCOMMDATAMSG = 42;

  /** Initializes the drivers */
  event void Boot.booted() {
  
  	//Initialize the radios:
    call lmxInit.init();
    call ccSplitControl.start();
  }
  
  /** Called when SplitControl gives access to the UART */ 
  event void ccSplitControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call ccSplitControl.start(); // try to start again
    }
  }
  
  event void ccSplitControl.stopDone(error_t err) {}
  
  /** Called when message is done sending */
  event void lmxAMSend.sendDone(message_t *msg, error_t error) {
    lmx_locked = FALSE;
  }
  
  /** Called when message is done sending */
  event void ccAMSendConfig.sendDone(message_t *msg, error_t error) {
    cc_locked = FALSE;
  }
  
  /** Called when message is done sending */
  event void ccAMSendData.sendDone(message_t *msg, error_t error) {
    cc_locked = FALSE;
  }
  
  /** Received packet from the Bluetooth connection */
  event message_t *lmxReceive.receive(message_t *msg, void *payload, uint8_t len) {    

    uint16_t dest = call lmxPacket.destination(msg);
  
    call Leds.led0Toggle();

    // Look where the packet needs to be sent using AdressResolution
    if ( ((call AddrRes.getRadio(dest)) & 0xFF00 ) == CC2420 && !cc_locked) {
      
      cc_locked = TRUE;
      call ccPacketLink.setRetries(msg, 3);
      call ccPacketLink.setRetryDelay(msg, 50);
      if(call lmxPacket.type(msg) == AM_TITANCOMMCONFIGMSG)
        call ccAMSendConfig.send( ((dest&0x00FF)==0xFF)?0xFFFF:(dest & 0xFF), msg, len ); // check for broadcast
      else if(call lmxPacket.type(msg) == AM_TITANCOMMDATAMSG)
        call ccAMSendData.send(dest & 0xFF, msg, len );
    }
  
  	return msg;
  }
  
  /** Received config packet from the CC2420 connection */
  event message_t *ccReceiveConfig.receive(message_t *msg, void *payload, uint8_t len) { 
    if (/*call AddrRes.getRadio(dest) == BLUETOOTH &&*/ !lmx_locked) {
      lmx_locked = TRUE;
      call lmxAMSend.send(0xFFFF, msg, len);
    }
    
  	return msg;
  }
  
  /** Received data packet from the CC2420 connection */
  event message_t *ccReceiveData.receive(message_t *msg, void *payload, uint8_t len) { 
	uint8_t i;
	uint8_t * pay;
	
	pay = (uint8_t*)payload;

    call Leds.led2Toggle();
	
	if(len < TOSH_DATA_LENGTH && !lmx_locked) {
		
	  call Leds.led1Toggle();
	  
	  pay[1] += 1;		// length + 1
	  for(i=len; i>0; i--)
		pay[i] = pay[i-1];
		
	  pay[0] = 7;		// this is a data message
	  
      lmx_locked = TRUE;
      call lmxAMSend.send(0xFFFF, msg, len+1);
	}
    
  	return msg;
  }
}
