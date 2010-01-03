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
 *  Provides interfaces to initialize, send and receive data over the nRF2401
 *
 *  @author Christoph Walser
 *  @date   November 22 2006
 *
 **/

#include "nrf2401.h"
 
 
module nrf2401 {
  provides {
	interface Init as nrfInit;
	interface AMSend;
	interface Receive;
  }
  
  uses {
	interface Timer<TMilli> as Timer1;
	interface HplMsp430GeneralIO as Ce;
	interface HplMsp430GeneralIO as Cs;
	interface HplMsp430GeneralIO as PwrUp;
	interface HplMsp430GeneralIO as Dr1;
	interface HplMsp430GeneralIO as Din;
	
	interface Resource as Spi1;
	interface HplMsp430Usart as Spi1Control;
  }
  
}



implementation {
  const uint16_t ONE_MS = 1024;		//equals 1 ms
  uint16_t i;
  uint16_t m;
  error_t err;
  uint8_t rxMsg[TOSH_DATA_LENGTH];
  message_t rxMess;
  
 /**
   * Initialize the Spi 1 which is used for communication between the MSP and the nRF.
   *
   * @return SUCCESS if Spi initalized properly, FAIL otherwise.
   */
  command error_t nrfInit.init() {
	//Reserve the Spi 1:
	err = call Spi1.request();
	
	if (err)
		return SUCCESS;
	else
		return FAIL;
  }
  

  event void Spi1.granted() {
	//Configure the Spi 1:
	call Spi1Control.setModeSpi(&spi1_config);
	
	call Spi1Control.clrIntr();
	while (!call Spi1Control.isTxEmpty());
	
	//Initialize the Pins for controlling the nRF2401:
	call Ce.makeOutput();
	call Cs.makeOutput();
	call PwrUp.makeOutput();
	
	call Ce.clr();
	call Cs.clr();
  }
  
  /**
   * Transmit some data over the nRF.
   *
   * @param addr  Address of a node (TOS_NODE_ID). Broadcastaddress is 0xFFFF
   * @param msg  Message to send (length at most 23 Byte)
   * @param len  Length of the Message to send.
   * @return SUCCESS always.
   */
  command error_t AMSend.send(am_addr_t addr, message_t* msg, uint8_t len) {
	//Powerup the  nRF:
	call PwrUp.set();
	for (i=3*ONE_MS;i>0;i--);		//wait 3ms (Tpd2fgm)
	
	//Set the nRF into configuration mode:
	call Cs.set();
	
	//Configure the nRF for sending:
	for (i=1; i<15; i++)
	{
		call Spi1Control.tx(nrf_send_config[i]);
		while (!call Spi1Control.isTxEmpty());		//wait until everythig is sent.
	}
	while (!call Spi1Control.isTxEmpty());		//wait until everything is sent to the nRF
	
	call Cs.clr();		//Disable configuration mode
	call Ce.set();		//Enable active mode

	//Send the hardware-address to the nRF:
	for (i=7; i<12; i++) {
		call Spi1Control.tx(nrf_send_config[i]);
		while (!call Spi1Control.isTxEmpty());		//wait until everything is sent to the nRF
	}
	
	//Write the address into the message:
	msg->header[0] = (uint8_t) (addr>>8);
	msg->header[1] = (uint8_t) addr;
	
	//Send the address to the nRF:
	call Spi1Control.tx(msg->header[0]);
	while (!call Spi1Control.isTxEmpty());		//wait until everything is sent to the nRF
	call Spi1Control.tx(msg->header[1]);
	while (!call Spi1Control.isTxEmpty());		//wait until everything is sent to the nRF
	
	//Send the payload to the nRF:
	for (i=0;i<len;i++)
    {
		call Spi1Control.tx(msg->data[i]);
		while (!call Spi1Control.isTxEmpty());		//wait until everything is sent to the nRF
    }
		
	for (i=3*ONE_MS;i>0;i--);		//wait 3ms
	
	//Start the transmission:
	call Ce.clr();
	
	signal AMSend.sendDone(msg, SUCCESS);
	
	return SUCCESS;
  }
  
  /**
   * Not implemented, hence do not use.
   */
  command error_t AMSend.cancel(message_t *msg) {
	return FAIL;
  }
  
  /**
   * Not implemented, hence do not use.
   */
  command void *AMSend.getPayload(message_t *msg) {}
  
  /**
   * Returns the maximum number of bytes which can be transmited in one message.
   */
  command uint8_t AMSend.maxPayloadLength() {
	return TOSH_DATA_LENGTH-2;
  }
  
  /**
   * Receive a message from the nRF.
   *
   * @return SUCCESS always.
   */
  command void *Receive.getPayload(message_t *msg, uint8_t *len) {
	//Powerup the  nRF:
	call PwrUp.set();
	for (i=3*ONE_MS;i>0;i--);		//wait 3ms (Tpd2fgm)
	
	//Set the nRF into configuration mode:
	call Cs.set();
	
	for (i=1; i<15; i++)
	{
		call Spi1Control.tx(nrf_receive_config[i]);
		while (!call Spi1Control.isTxEmpty());		//wait until everythig is sent.
	}
	while (!call Spi1Control.isTxEmpty());		//wait until everythig is sent.
			
	//Set the Pins DR1 and DIN to input mode
	call Dr1.makeInput();
	call Din.makeInput();
	
	call Cs.clr();
	for (i=10; i>0; i--);
	call Ce.set();
	for (i=3*ONE_MS; i>0; i--);
			
	//Poll the nRF for received data:
	call Timer1.startPeriodic(2);
	
	return SUCCESS;
	
  }
  
  event void Timer1.fired() {
	if (call Dr1.get() && call Ce.get()) {
		call Timer1.stop();
		
		call Ce.clr();
		
		m = 0;
		
		while (call Dr1.get()) {	//Data is ready in the nRF2401
			for (i=0; i<200;i++);
			call Spi1Control.tx(0);	
			while (!call Spi1Control.isTxEmpty());		//wait until everything is sent			
			if (m < 2) {
				rxMess.header[m] = call Spi1Control.rx();
			}
			if (m > 1) {
				rxMess.data[m-2] = call Spi1Control.rx();
			}
			m++;
			if (m > TOSH_DATA_LENGTH) 
				m = 0;
		}

		//Only pass the message to the next layerif the received message was a Broadcast or directed to this node:
		if ((((rxMess.header[0])<<8) + rxMess.header[1] == TOS_NODE_ID) || (((rxMess.header[0])<<8) + rxMess.header[1] == 0xFFFF))
			signal Receive.receive(&rxMess, rxMess.data, TOSH_DATA_LENGTH-2);
		
		//wait 50ns (T_d):
		for (i=0;i<2;i++);
		call Ce.set();
		//wait 202us (Tsby2rx):
		for (i=0;i<250;i++);
		
		call Timer1.startPeriodic(2);
	}
  }

/**
   * Returns the maximum payloadlength of a message in bytes.
   */
  command uint8_t Receive.payloadLength(message_t *msg) {
	return TOSH_DATA_LENGTH;
  }
  
  
}


