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
 * Puppet communication messages
 *
 * @author Clemens Lombriser
 */
#include "Puppet.h"

module SmartObjectP{
	uses interface Leds;
	uses interface Boot;
	uses interface Timer<TMilli>;
	
	uses interface Packet;
	uses interface Packet as SerialPacket;
	uses interface CC2420Packet;
	uses interface SplitControl as AMControl;
	
	uses interface AMSend as IDSend;
	uses interface AMSend as PuppetSend;
	
	uses interface Random;
}
implementation{
    message_t m_msg;
    bool m_lock;
    
    // power levels, correspond to CC2420's: 0, -1, -3, -5, -7, -10, -15, -25 dBm
    const uint8_t POWER_LEVELS[] = {31,27,23,19,15,11,7,3};

	event void Boot.booted() {
	  
	    m_lock  = FALSE;
	    call AMControl.start();
	}
	
	event void AMControl.startDone( error_t error ) {
	  
	  if ( error == SUCCESS ) {
	    if( TOS_NODE_ID != 0 ) {
		  call Timer.startPeriodic(10000);
		}
	  } else {
	    call AMControl.start();
	  }
	}
	
	event void AMControl.stopDone(error_t error) {
	  // do nothing
	}
	
	////////////////////////////////////////////////////////////////////////
	// Sending ID

	event void Timer.fired() {
		uint8_t i;
		uint8_t strName[] = "milk";
		uint8_t strVerb[] = "drink";
		
        if (m_lock == TRUE ) return;

		if ( call Random.rand16() < 65535/4 ) {
		    uint8_t strMessage[] = "I am drinking milk";
		    PuppetMsg *pMsg = (PuppetMsg*)call Packet.getPayload(&m_msg,sizeof(PuppetMsg));
			if (pMsg == NULL) return;
			call Leds.led0Toggle();
			pMsg->uiIdentifier = TOS_AM_ADDRESS;
			for(i=0; i<sizeof(strMessage); i++) {
			  pMsg->strMessage[i] = strMessage[i];
			}

			call CC2420Packet.setPower(&m_msg,POWER_LEVELS[0]);
			
			if (call PuppetSend.send(AM_BROADCAST_ADDR,&m_msg,sizeof(PuppetMsg)) == SUCCESS ) {
			  m_lock = TRUE;
			}
		
		} else {
		  
			IDMsg *pMsg = (IDMsg*)call Packet.getPayload(&m_msg,sizeof(IDMsg));
			if (pMsg == NULL) return;
			
			call Leds.led0Toggle();
			
			pMsg->uiIdentifier = TOS_AM_ADDRESS;
			
			for(i=0; i<sizeof(strName); i++) {
			  pMsg->strName[i] = strName[i];
			}
			for(i=0; i<sizeof(strVerb); i++) {
			  pMsg->strVerb[i] = strVerb[i];
			}
			
			call CC2420Packet.setPower(&m_msg,POWER_LEVELS[0]);
			
			if (call IDSend.send(AM_BROADCAST_ADDR,&m_msg,sizeof(IDMsg)) == SUCCESS ) {
			  m_lock = TRUE;
			} else {
			  call Leds.led2Toggle();
			}
		}
	}
	
	////////////////////////////////////////////////////////////////////////
    // send done events - ignored

	event void PuppetSend.sendDone( message_t* msg, error_t error ) {
	  if (msg == &m_msg) {
	    m_lock = FALSE;
	  }
	}
	
	event void IDSend.sendDone( message_t* msg, error_t error ) {
	  if (msg == &m_msg) {
	    m_lock = FALSE;
	  }
	}
	

}
