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
 * Defines the wiring of the Gateway components. Originally, the nRF component 
 * was also included, but has been removed as it is not needed anymore and the 
 * USART does not have to be shared with the LMX.
 *
 * @author Fabian Schenkel <schenfab@ee.ethz.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

configuration Usart1RadiosC {
  provides interface Init as nrfInit;
	provides interface AMSend as nrfAMSend;
	provides interface Receive as nrfReceive;
  provides interface Init as lmxInit;
	provides interface AMSend as lmxAMSend;
	provides interface Receive as lmxReceive;
}

implementation {
  components lmx9820a as LMX;
  components nrf2401 as nRF;
  components LedsC;

	//Components to control the important pins of the nRF2401: 
	components HplMsp430GeneralIOC as CsC;
	components HplMsp430GeneralIOC as CeC;
	components HplMsp430GeneralIOC as PwrUpC;
  components HplMsp430GeneralIOC as DrGeneralIOC;
  components HplMsp430InterruptC as DrInterruptC;
  components new Msp430GpioC() as DrC;
  components new Msp430InterruptC() as InterruptDrC;	 
  
  //Components to control the important pins of the lmx9820a: 
	components HplMsp430GeneralIOC as Isel1C;
	components HplMsp430GeneralIOC as Isel2C;
  components HplMsp430GeneralIOC as CtsC;
	components HplMsp430GeneralIOC as LmxPwrUpC;
  components HplMsp430GeneralIOC as TxC;
  components HplMsp430GeneralIOC as RxC;
  
  // Components to use and share the usart1 module
  components new Msp430Usart1C() as Usart1C;
  components HplMsp430Usart1C as Uart1InterruptC;
  
  // Component to handle lmxMessages
  components lmxActiveMessageC;

  // nRF wiring
	nRF.nrfInit                  = nrfInit;
	nRF.AMSend                   = nrfAMSend;
	nRF.Receive                  = nrfReceive;
	nRF.Cs                      -> CsC.Port17;
	nRF.Ce						          -> CeC.Port16;
	nRF.PwrUp					          -> PwrUpC.Port20;
  DrC                         -> DrGeneralIOC.Port21;
	InterruptDrC.HplInterrupt   -> DrInterruptC.Port21; 
  nRF.DrInterrupt             -> InterruptDrC.Interrupt;
	nRF.Spi1Control 			      -> Usart1C.HplMsp430Usart;
  
  // lmx wiring
  LMX.lmxInit 			   = lmxInit;
	LMX.AMSend 		 		   = lmxAMSend;
	LMX.Receive 		 	   = lmxReceive;
	LMX.Isel1			    	-> Isel1C.Port22;
	LMX.Isel2			      -> Isel2C.Port23;
  LMX.Cts				      -> CtsC.Port24;
	LMX.PwrUp			      -> LmxPwrUpC.Port43;
	LMX.Uart1Control 		-> Usart1C.HplMsp430Usart;
  LMX.Uart1Interrupt	-> Uart1InterruptC;
  LMX.Tx				      -> TxC.Port35;
	LMX.Rx			        -> RxC.Port34;
  LMX.Uart1Resource   -> Usart1C.Resource;
  LMX.Leds            -> LedsC;
  
  LMX.lmxPacket         -> lmxActiveMessageC;
}
