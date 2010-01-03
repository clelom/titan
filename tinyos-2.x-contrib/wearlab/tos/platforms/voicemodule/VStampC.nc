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

/************************************************************************************
* Actuator Driver
*
* @author Andreas Breitenmoser
*
* Actuator driver for the voice synthesiser module V-Stamp.
* The V-Stamp consists of a integrated text-to-speech processor and a tone generator.
* Sound files can be recorded, up- and downloaded and be played. 
*
* Created: 16.12.2007
* Last modified:  11.01.2008
*
************************************************************************************/

#include "Timer.h"

generic configuration VStampC()
{
  provides
  {
	interface VStamp;
	// interface Init;
	// interface AMSend as Send;
	// interface Receive;
  }
}
implementation
{
  // actuator components
  components VStampP;
  
  components HplMsp430GeneralIOC as TalkStatusIO;
  // components HplMsp430InterruptC as TalkStatusInterrupt;
  components HplMsp430GeneralIOC as Suspend;
  components HplMsp430GeneralIOC as ClearToSendIO;
  // components HplMsp430InterruptC as ClearToSendInterrupt;	
  components HplMsp430GeneralIOC as Standby;
  components HplMsp430GeneralIOC as Reset;

  // UART components
  components new Msp430Usart1C() as Usart1; 
    
  // components for debugging
  components LedsC;
  components new TimerMilliC() as Timer;
	
  VStampP.RES -> Reset.Port12;
  VStampP.SUSP -> Suspend.Port21;
  VStampP.STBY -> Standby.Port23;
  VStampP.CTSio -> ClearToSendIO.Port26;
  // VStampP.CTSinterrupt -> ClearToSendInterrupt.Port26;	// interrupt capability
  VStampP.TSio -> TalkStatusIO.Port27;
  // VStampP.TSinterrupt -> TalkStatusInterrupt.Port27;	// interrupt capability
  
  VStampP.Uart1 -> Usart1.Resource;
  VStampP.Uart1Control -> Usart1.HplMsp430Usart;
  //VStampP.Uart1Interrupt -> Usart1.HplMsp430UsartInterrupts;
	
  // Debug LEDs
  VStampP.Leds -> LedsC;
  VStampP.Timer1 -> Timer;
  VStampP.Timer2 -> Timer;

  VStamp = VStampP;
  // Init = VStampP.VStampInit;
  // Send = VStampP.VstRXD;
  // Receive = VStampP.VstTXD;
}
