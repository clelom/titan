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
 * This is a test application for the Wireless Voice Nodes paper. It is set up to measure RSSI values around the puppet.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

#include "TestRssi.h"

module TestRssiP{
	uses interface Leds;
	uses interface Boot;
	uses interface Timer<TMilli>;
	
	uses interface Packet;
	uses interface Packet as SerialPacket;
	uses interface CC2420Packet;
	uses interface SplitControl as AMControl;
	uses interface SplitControl as SerialControl;
	
	uses interface AMSend as SerialSender;
	
	uses interface AMSend as RssiSender;
	uses interface Receive as RssiReceiver;
	
	uses interface AMSend as CollectSender;
	uses interface Receive as CollectReceiver;
	
}


implementation{

    message_t m_rssiSendMsg;
    message_t m_collectSendMsg;
    message_t m_serialSendMsg;
    uint16_t m_counter;
    bool m_rssiLock;
    bool m_collectLock;
    bool m_serialLock;
    
    // power levels, correspond to CC2420's: 0, -1, -3, -5, -7, -10, -15, -25 dBm
    const uint8_t POWER_LEVELS[] = {31,27,23,19,15,11,7,3};

	event void Boot.booted() {
	  
	    m_rssiLock = m_collectLock = m_serialLock = FALSE;
	    m_counter = 0;
	    call AMControl.start();
	    call SerialControl.start();
	}
	
	event void AMControl.startDone( error_t error ) {
	  
	  if ( error == SUCCESS ) {
	    if( TOS_NODE_ID != 0 ) {
		  call Timer.startPeriodic(100);
		}
	  } else {
	    call AMControl.start();
	  }
	}
	
	event void SerialControl.startDone( error_t error ) {
	  
	  if ( error != SUCCESS ) {
	    call SerialControl.start();
	  }
	}

	event void AMControl.stopDone(error_t error) {
	  // do nothing
	}
	
	event void SerialControl.stopDone(error_t error) {
	  // do nothing
	}
	////////////////////////////////////////////////////////////////////////
	// RSSI measurement

	event void Timer.fired() {
		
		WVN_RssiMsg *pMsg = (WVN_RssiMsg*)call Packet.getPayload(&m_rssiSendMsg,sizeof(WVN_RssiMsg));
		
		if (pMsg == NULL) return;
		if (m_rssiLock == TRUE ) return;
		
		call Leds.led0Toggle();
		
		pMsg->sender = TOS_AM_ADDRESS;
		pMsg->receiver = 0;
		pMsg->counter = m_counter;
		pMsg->power_level = POWER_LEVELS[m_counter&0x7];
		pMsg->rssi_value = 0;
		
		call CC2420Packet.setPower(&m_rssiSendMsg,POWER_LEVELS[m_counter&0x7]);
		
		if (call RssiSender.send(AM_BROADCAST_ADDR,&m_rssiSendMsg,sizeof(WVN_RssiMsg)) == SUCCESS ) {
		  m_rssiLock = TRUE;
		} else {
		  call Leds.led2Toggle();
		}

        m_counter++;
	}
	
	/**
	 * Receives incoming packet, reads out RSSI value, and forwards data to sink
	 */
	event message_t* RssiReceiver.receive( message_t *msg, void *payload, uint8_t len ) {
	  WVN_RssiMsg* pMsgIn  = (WVN_RssiMsg*)payload;
	  WVN_RssiMsg* pMsgOut = (WVN_RssiMsg*)(call Packet.getPayload(&m_collectSendMsg,sizeof(WVN_RssiMsg)));
	  
	  if (pMsgOut == NULL ) return msg;
	  if (TOS_NODE_ID == 0) return msg;
	  if (m_collectLock == TRUE ) return msg;
	  
      call Leds.led1Toggle();

	  pMsgOut->sender = pMsgIn->sender;
	  pMsgOut->receiver = TOS_AM_ADDRESS;
	  pMsgOut->counter  = pMsgIn->counter;
	  pMsgOut->power_level = pMsgIn->power_level;
	  pMsgOut->rssi_value= call CC2420Packet.getRssi(msg);
	  
	  if (call CollectSender.send(0, &m_collectSendMsg, sizeof(WVN_RssiMsg)) == SUCCESS) {
	    m_collectLock = TRUE;
	  } else {
	    call Leds.led2Toggle();
	  }
	  
	  return msg;
	}
	
	event message_t* CollectReceiver.receive( message_t* msg, void* payload, uint8_t len ) {
	  WVN_RssiMsg* pMsgIn  = (WVN_RssiMsg*)payload;
	  WVN_RssiMsg* pMsgOut = (WVN_RssiMsg*)(call SerialPacket.getPayload(&m_serialSendMsg,sizeof(WVN_RssiMsg)));
	  
	  if (pMsgOut == NULL) return msg;
	  if (TOS_NODE_ID != 0) return msg;
	  if (m_serialLock == TRUE ) {
	    call Leds.led0Toggle();
	    return msg; 
	  }
	  
      call Leds.led2Toggle();

	  pMsgOut->sender = pMsgIn->sender;
	  pMsgOut->receiver = pMsgIn->receiver;
	  pMsgOut->counter  = pMsgIn->counter;
	  pMsgOut->power_level = pMsgIn->power_level;
	  pMsgOut->rssi_value= pMsgIn->rssi_value;
	  
	  if ( call SerialSender.send(0, &m_serialSendMsg, sizeof(WVN_RssiMsg)) == SUCCESS) {
	    m_serialLock = TRUE;
	  } else {
	    call Leds.led1Toggle();
	  }
	  
	  return msg;
	}



	////////////////////////////////////////////////////////////////////////
    // send done events - ignored

	event void SerialSender.sendDone( message_t* msg, error_t error ) {
	  if (msg == &m_serialSendMsg) {
	    m_serialLock = FALSE;
	  }
	}
	
	event void RssiSender.sendDone( message_t* msg, error_t error ) {
	  if (msg == &m_rssiSendMsg) {
	    m_rssiLock = FALSE;
	  }
	}
	
	event void CollectSender.sendDone( message_t* msg, error_t error ) {
	  if (msg == &m_collectSendMsg) {
	    m_collectLock = FALSE;
	  }
	}
	

}
