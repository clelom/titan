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

	components new TimerMilliC() as Timer1;
	
	//Components to control the important pins of the nRF2401: 
	components HplMsp430GeneralIOC as CsC;
	components HplMsp430GeneralIOC as CeC;
	components HplMsp430GeneralIOC as PwrUpC;
	components HplMsp430GeneralIOC as Dr1C;
	components HplMsp430GeneralIOC as DinC;
	
	components new Msp430Spi1C() as Spi;
	components HplMsp430Usart1C as Spi1ControlC;
	
	nrf2401.nrfInit 				= Init;
	nrf2401.AMSend 					= AMSend;
	nrf2401.Receive 				= Receive;
	
	nrf2401.Timer1	 				-> Timer1;
	
	nrf2401.Cs						-> CsC.Port36;
	nrf2401.Ce						-> CeC.Port37;
	nrf2401.PwrUp					-> PwrUpC.Port20;
	nrf2401.Dr1						-> Dr1C.Port50;
	nrf2401.Din						-> DinC.Port51;
	
	nrf2401.Spi1					-> Spi.Resource;
	nrf2401.Spi1Control 			-> Spi1ControlC;
}

