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
 * Features for Sensorbutton
 * provides Mean & Var
 * @author Andreas Bubenhofer
 * 
 */







module SerialCom {
   provides interface Resource;
   provides interface SBSend;
   uses interface Resource as ResourceCmd;
   uses interface HplMsp430Usart as UartControl;
}
 
implementation {
   task void sendDone();
   uint8_t initialised = 0;
   msp430_uart_config_t uartConfig = {
     ubr: UBR_1MHZ_115200,      //Baud rate (use enum msp430_uart_rate_t for predefined rates)
     umctl: UMCTL_1MHZ_115200,    //Modulation (use enum msp430_uart_rate_t for predefined rates)
	 // ubr: UBR_1MHZ_230400,      //Baud rate (use enum msp430_uart_rate_t for predefined rates)
     // umctl: UMCTL_1MHZ_230400,    //Modulation (use enum msp430_uart_rate_t for predefined rates)
      ssel: 2,     //Clock source (00=UCLKI; 01=ACLK; 10=SMCLK; 11=SMCLK)
      pena: 0,     //Parity enable (0=disabled; 1=enabled)
      pev: 1,      //Parity select (0=odd; 1=even)
      spb: 0,      //Stop bits (0=one stop bit; 1=two stop bits)
      clen: 1,     //Character length (0=7-bit data; 1=8-bit data)
      listen: 0,   //Listen enable (0=disabled; 1=enabled, feed tx back to receiver)
      mm: 0,       //Multiprocessor mode (0=idle-line protocol; 1=address-bit protocol)
      0,
      ckpl: 0,     //Clock polarity (0=normal; 1=inverted)
      urxse: 1,    //Receive start-edge detection (0=disabled; 1=enabled)
      urxeie: 1,   //Erroneous-character receive (0=rejected; 1=recieved and URXIFGx set)
      urxwie: 1,   //Wake-up interrupt-enable (0=all characters set URXIFGx; 1=only address sets URXIFGx)
     };
	 
	 

	 
   
   
  /**
   * Request immediate access to a shared resource. You must call 
   * release() when you are done with it.
   *
   * @return SUCCESS You now have cotnrol of the resource.<br>
   *         EBUSY The resource is busy.  You must try again later
   */
  
   async command error_t Resource.request()
   {
   call ResourceCmd.request(); 
   return SUCCESS;
   
   }
  
  
  async command error_t Resource.immediateRequest(){return FAIL;}
  
  
  
  
  default event void Resource.granted(){}
   
  
  
  
  async command void Resource.release(){
   initialised = 0;
   call ResourceCmd.release();
  }

  
  
  async command bool Resource.isOwner(){
  return FALSE;
  }
  
  
   
  command error_t SBSend.sendByte(uint8_t data)
  {
    if (!initialised) return FAIL;
	while (!call UartControl.isTxEmpty());
    call UartControl.tx(data);
	post sendDone();
	return SUCCESS;
  
  }
  
  task void sendDone(){
    signal SBSend.sendDone();
	return;
	}
  
  
  
  event void ResourceCmd.granted() // Uart for RS-232 granted, free for use
   {   
      call UartControl.setModeUart(&uartConfig); //Configure Uart (RS-232)
	  signal Resource.granted();
	  initialised = 1;
   }
   
   command error_t SBSend.sendString( char* msg, uint8_t length)
   {
      uint8_t n;
	 if (!initialised) {return FAIL;}
	
     for (n=0; n <length; n++) {
       while (!call UartControl.isTxEmpty());
       call UartControl.tx(msg[n]);
     }
	 post sendDone();
	 return SUCCESS; 
   }
   
 
   default event void SBSend.sendDone(){}
}
