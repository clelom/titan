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
 * PuppetApp
 * 
 * This is a test application for the puppet. It will check how the reading works
 * 
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

#include "Puppet.h"

module PuppetP{
	uses interface Leds;
	uses interface Boot;
	uses interface Timer<TMilli>;
	uses interface Random;
	uses interface VStamp;

	uses interface Packet;
	uses interface CC2420Packet;
	uses interface SplitControl as AMControl;
	
	uses interface Receive as PuppetReceive;
	uses interface Receive as IDReceive;
	
}


implementation{
  
  
  
  SmartObjectCache m_KnownSmartObjects[MAX_NUM_SMART_OBJECTS];
  
  event void AMControl.startDone(error_t error){
    
  }
  
  event void AMControl.stopDone(error_t error){
    
  }
  
  
  event void Timer.fired(){
    
  }
  
  
  event void Boot.booted() {
    call VStamp.init(FALSE);
  }
  
  event void VStamp.uartReady(){
    char strVoice[] = "x5a x5e x80f x85p x2t";
    call AMControl.start();
    call Timer.startPeriodic(1000);
    
    // set the voice
    strVoice[0] = strVoice[4] = strVoice[8] = strVoice[13] = strVoice[18] = 0x01; // replace command
    call VStamp.sendText(strVoice);
    
  }
  
  event message_t* PuppetReceive.receive( message_t* msg, void* payload, uint8_t len ) {
    PuppetMsg *pMsg = (PuppetMsg*)payload;
    int8_t iRssi = call CC2420Packet.getRssi(msg);
    
    if ( len != sizeof(PuppetMsg)) return msg;
    
    if ( iRssi > DMIN ) {
      call VStamp.sendText((uint8_t*)pMsg->strMessage);
    } else {
      call Leds.led0Toggle();
    }
    
    return msg;
  }
  
  event message_t* IDReceive.receive( message_t* msg, void* payload, uint8_t len ) {
    IDMsg *pMsg = (IDMsg*)payload;
    int8_t iRssi = call CC2420Packet.getRssi(msg);

    if ( len != sizeof(IDMsg)) return msg;

    if ( iRssi < DMAX ) {
        uint16_t p,r; 
    
	    p  = ((uint16_t)((int8_t)DMAX - iRssi))<<8;
	    p /= (int8_t)DMAX;
	    p *= PMAX;
	    
	    r = call Random.rand16();
	    
	    if ( r > p ) {
	      call VStamp.sendText("I want ");
	      call VStamp.sendText((uint8_t*)pMsg->strVerb);
	      call VStamp.sendText((uint8_t*)pMsg->strName);
	    } else {
	      call Leds.led1Toggle();
	    }
	    
	}

    return msg;
  }
  
}


