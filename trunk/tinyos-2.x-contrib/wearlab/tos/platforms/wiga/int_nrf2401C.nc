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
 *  Wiring for the nRF2401  
 *
 *  @author Christoph Walser
 *  @date   November 22 2006
 *
 **/

configuration nrf2401C {
	provides interface Init;
	provides interface AMSend;
	provides interface Receive;
}

implementation {
	components nrf2401;
  
  components LedsC;
	
	//Components to control the important pins of the nRF2401: 
	components HplMsp430GeneralIOC as CsC;
	components HplMsp430GeneralIOC as CeC;
	components HplMsp430GeneralIOC as PwrUpC;
  components HplMsp430GeneralIOC as DrGeneralIOC;
  components HplMsp430InterruptC as DrInterruptC;
  components new Msp430GpioC() as DrC;
  components new Msp430InterruptC() as InterruptDrC;	

  
  components new Msp430Usart1C() as Usart1C;
  
	
	nrf2401.nrfInit 				= Init;
	nrf2401.AMSend 					= AMSend;
	nrf2401.Receive 				= Receive;
	
	nrf2401.Cs					    	-> CsC.Port17; //17 //36;
	nrf2401.Ce						    -> CeC.Port16; //16 //37;
	nrf2401.PwrUp					    -> PwrUpC.Port20;
 
  DrC                       -> DrGeneralIOC.Port21;
	InterruptDrC.HplInterrupt -> DrInterruptC.Port21;
  
  nrf2401.DrInterrupt -> InterruptDrC.Interrupt;
  nrf2401.DrIO -> DrC;
  
	nrf2401.Spi1					-> Usart1C.Resource;
	nrf2401.Spi1Control 	-> Usart1C.HplMsp430Usart;
  
  //Debug:
  nrf2401.Leds       -> LedsC;
}

