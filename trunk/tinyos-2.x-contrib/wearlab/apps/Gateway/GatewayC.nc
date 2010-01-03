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
 *  Gateway application configuration  
 *
 *  @author Christoph Walser
 *  @author Fabian Schenkel
 *  @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *  @author Tonio Gsell <tgsell@ee.ethz.ch>
 *  @date   April 8 2008
 *
 **/
 
#include "Gateway.h"

/* Titan Active Message Identifier */
enum {
	 AM_TITANCOMMDATAMSG = 42,
	 AM_TITANCOMMCONFIGMSG = 13,
};
 
configuration GatewayC {}

implementation {

  // standard components
  components MainC, LedsC;
  components CC2420ActiveMessageC as ActiveMessageC;
  components new AMSenderC(AM_TITANCOMMCONFIGMSG)   as ccAMSenderConfig;
  components new AMReceiverC(AM_TITANCOMMCONFIGMSG) as ccAMReceiverConfig;
  components new AMSenderC(AM_TITANCOMMDATAMSG)     as ccAMSenderData;
  components new AMReceiverC(AM_TITANCOMMDATAMSG)   as ccAMReceiverData;
  components SerialActiveMessageC;
  components lmxActiveMessageC;

  // WIGA platform components
  components Resolution;
  components Usart1RadiosC;

  // application
  components Gateway;
  

  // link Gateway
  Gateway 				    		-> MainC.Boot;
  Gateway.Leds	 				  -> LedsC;
  Gateway.AddrRes         -> Resolution;
  
  // Radio interfaces
  Gateway.lmxInit	 				-> Usart1RadiosC.lmxInit;
  Gateway.lmxAMSend	 			-> Usart1RadiosC.lmxAMSend;
  Gateway.lmxReceive	 		-> Usart1RadiosC.lmxReceive;
  Gateway.lmxPacket       -> lmxActiveMessageC;
  
  Gateway.ccSplitControl  -> ActiveMessageC;
  Gateway.ccAMSendConfig  -> ccAMSenderConfig;
  Gateway.ccReceiveConfig -> ccAMReceiverConfig;
  Gateway.ccAMSendData    -> ccAMSenderData;
  Gateway.ccReceiveData   -> ccAMReceiverData;
  Gateway.ccPacket        -> ActiveMessageC;
  Gateway.ccPacketLink    -> ActiveMessageC;
}

