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
 *  Provides interfaces to initialize, send and receive data over the lmx9820a
 *
 *  @author Christoph Walser
 *  @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *  @author Tonio Gsell <tgsell@ee.ethz.ch>
 *  @date   April 8 2008
 *
 *  2008/02/29 CL: Added request for resource
 **/

#include "lmx9820a.h"

#define START_BYTE 0x3C // =   	< on keyboard
#define STOP_BYTE 0x3E // =  		> on keyboard

module lmx9820a {
  provides {
  	interface Init as lmxInit;
  	interface AMSend;
  	interface Receive;
  }
  
  uses {  
  	interface HplMsp430GeneralIO as Isel1;
  	interface HplMsp430GeneralIO as Isel2;
    interface HplMsp430GeneralIO as Cts;
  	interface HplMsp430GeneralIO as PwrUp;
    interface HplMsp430GeneralIO as Tx;
  	interface HplMsp430GeneralIO as Rx;
    
    interface ResourceDefaultOwner as Uart1;
  	interface HplMsp430Usart as Uart1Control;
    interface HplMsp430UsartInterrupts as Uart1Interrupt;
    
    interface Resource as Uart1Resource; //cl
    
    interface Leds;
	
    interface AMPacket as lmxPacket;
  }
  
}

implementation {

  void uartInit();

  static uint8_t rx_data;
  static uint8_t m_length;
  static uint8_t m_stuffing = 0;
  static am_addr_t m_addr;
  
  task void recv();

  // receive side
  const uint16_t ONE_MS = 1024;
  uint16_t i;
  uint16_t m;
  error_t err;
  uint8_t rxCounter = 0;
  message_t rxMessBuf;
  message_t *rxMess;

  // receive side
  am_addr_t  m_txAddr;
  uint8_t    m_txLength;
  message_t* m_txBuffer; // buffer to copy the message to be sent
  bool       m_txBusy;     // busy sending...
  
 /**
   * Initialize the Spi 1 which is used for communication between the MSP and the nRF.
   *
   * @return SUCCESS if Spi initalized properly, FAIL otherwise.
   */
  command error_t lmxInit.init() {
    
    //Initialize the Pins for controlling the lmx9820a:
    call PwrUp.makeOutput();
    call Isel1.makeOutput();
    call Isel2.makeOutput();
    call Cts.makeOutput();
    
    rxMess = &rxMessBuf;
    
    // Reset device
    call PwrUp.clr();
    for (i=2*ONE_MS;i>0;i--);		//wait 3ms  (power to reset-time)
    call PwrUp.set();
    
#ifdef USE_115200_BT
    // Sets lmx-uart to 115.2kbaud
    call Isel1.clr();
    call Isel2.set();
    // other values (Isel1,Isel2): (1,1) 921.6k, (0,1) 115.2k, (1,0) 9.6k, (0,0) check NVS
    // LMX NVS standard: 1 stop, 1 start, no parity
#else
    // Sets lmx-uart to 9600 baud
    call Isel1.set();
    call Isel2.clr();
#endif
    
    call Cts.set();
    
    //call Leds.set(0xFF);

    // After boot up (here), the ResourceDefaultOwner already has granted access to the usart according to TEP108
    //cl uartInit();
    call Uart1Resource.request(); //cl
    
  	return SUCCESS;
  }
  
  void uartInit() {
    //Configure the Uart 1 and clear flags/buffers
  	call Uart1Control.setModeUart(&uart1_config);
    
    // Clear rx flag and enable rx interrupt  
    call Uart1Control.clrRxIntr();
    call Uart1Control.enableRxIntr();
    
    // Set "request to send" (active low) to receive messages from lmx
    call Cts.clr();
    
    m_txBusy = FALSE;
  }

  async event void Uart1.granted() { 
    // Usart control has been given back, so configure it for uart use.
    uartInit();
  }
  
  // handling shared uart1
  event void Uart1Resource.granted() {
    uartInit();
  }
  
  async event void Uart1.requested() {
     
    // Clear "request to send" (active low) to stop from lmx sending new messages to uC
    call Cts.set();
    
    // Release usart to spi
    call Uart1.release();

  }
  
  async event void Uart1.immediateRequested() {} // not implemented in nRF, so not necessary 
  
  
  task void send() {
    
      /* *********************************************************************** */
      /* TODO: stream encoding                                       */
      /* *********************************************************************** */
      // Note: this blocks the MSP430 while sending. Using txDone() would allow to 
      //       react as soon as a byte has been sent
	  
      call Uart1Control.tx(START_BYTE);				// send start byte
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
	  
      call Uart1Control.tx(0);						// not important here
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
	  
      call Uart1Control.tx(call lmxPacket.destination(m_txBuffer) | 0xFF );	// send address
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
	  
      call Uart1Control.tx(call lmxPacket.type(m_txBuffer));				// send message type
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
	  
      call Uart1Control.tx(m_txLength);					// send length
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent

      //Send the payload to the lmx (which should be in transparent mode, if PC has connected):
      for (i=0;i<m_txLength;i++)
      {
    		if(m_txBuffer->data[i] == STOP_BYTE) {
    			call Uart1Control.tx(STOP_BYTE);
    			while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
    		}
		
        call Uart1Control.tx(m_txBuffer->data[i]);
        while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
      }
	  
      call Uart1Control.tx(STOP_BYTE);				// send stop byte
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent
	  
      call Uart1Control.tx(0);						// so the STOP_BYTE will be accepted
      while (!call Uart1Control.isTxEmpty());		//wait until byte is sent

      atomic m_txBusy = FALSE;
      //signal that sending is done as an event to the application
      signal AMSend.sendDone(m_txBuffer, SUCCESS);
      
  }
  
  /**
   * Transmit some data over the lmx9820a.
   *
   * @param addr  Address of a node (TOS_NODE_ID). Broadcastaddress is 0xFFFF
   * @param msg  Message to send (length at most 23 Byte)
   * @param len  Length of the Message to send.
   * @return SUCCESS always.
   */
  command error_t AMSend.send(am_addr_t addr, message_t* msg, uint8_t len) {

    atomic {
      if (m_txBusy == TRUE) return FAIL;
      else m_txBusy = TRUE;
    }

    m_txAddr = addr;
    m_txLength = len;
    m_txBuffer = msg;
    
    if( post send() == FAIL ) {
      atomic m_txBusy = FALSE;
      return FAIL;
    }
    
    return SUCCESS;
  }
  
  /**
   * Cannot cancel
   */
  command error_t AMSend.cancel(message_t *msg) {
    return FAIL;
  }
  
  /**
   * Not implemented, hence do not use.
   */
  command void *AMSend.getPayload(message_t *msg, uint8_t len) {}
  
  /**
   * Returns the maximum number of bytes which can be transmited in one message.
   */
  command uint8_t AMSend.maxPayloadLength() {
    return TOSH_DATA_LENGTH;
  }

  /** called when a byte has been sent by the UART */
  /* Note: this would be a nice place to send the next output buffer byte */
  async event void Uart1Interrupt.txDone() {}
  
  /** called when a byte has been received by the UART */
  async event void Uart1Interrupt.rxDone(uint8_t data) {
    rx_data = data; 
    call Uart1Control.clrRxIntr();
//    call Leds.led2Toggle(); // toggle per byte received
    post recv();
  }
  
  
  /* *********************************************************************** */
  /* TODO: Insert here stream decoding                                       */
  /* *********************************************************************** */
  task void recv() {
    //call Leds.led0Toggle();
    atomic {
      if (rxCounter == 0 && rx_data == START_BYTE) { // Start of a new packet
        rxCounter++;
        return;
      }
	  if (rxCounter == 1) {
		m_addr = rx_data << 8;
		rxCounter++;
	  }
	  else if (rxCounter == 2) {
		m_addr |= rx_data;
		call lmxPacket.setDestination(rxMess, m_addr);		// destination address
		rxCounter++;
	  }
	  else if (rxCounter == 3) {
		call lmxPacket.setType(rxMess, rx_data);			// type: contol/data
		rxCounter++;
	  }
	  else if (rxCounter == 4) {
		m_length = rx_data;									// message length
		rxCounter++;
	  }
	  else if (rxCounter > 4) { 					// payload
      uint8_t stop;
      stop = 0;
			
      if(m_stuffing == 1) {
        if(rx_data == STOP_BYTE) {
          rxMess->data[rxCounter-5] = STOP_BYTE;
          rxCounter++;
          m_stuffing = 0;
        }
        else
          stop = 1;
      }
      else if(rx_data == STOP_BYTE)
        m_stuffing = 1;
      else {
        rxMess->data[rxCounter-5] = rx_data;
        rxCounter++;
      }
		
		if (rxCounter == TOSH_DATA_LENGTH + 6 || stop == 1) { // stop byte received or packet length exceeded
			uint8_t len;
			
			if(stop == 1)
				len = rxCounter-5;
			else
				len = TOSH_DATA_LENGTH;
			
			rxCounter = 0;
			m_stuffing = 0;
			if(len == m_length)
				rxMess = signal Receive.receive(rxMess, &(rxMess->data), len);
        }
      } 
    }
  }


}


