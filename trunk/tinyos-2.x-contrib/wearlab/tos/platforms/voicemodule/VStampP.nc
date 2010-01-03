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
* Implementation of the voice synthesiser module V-Stamp.
*
* Created: 16.12.2007
* Last modified: 11.01.2008
*
************************************************************************************/

#include "VStamp.h"

#include <string.h>


module VStampP
{
  // user interfaces
  provides
  {
	interface VStamp;
    // interface Init as VStampInit;
	// interface AMSend as VstRXD;
	// interface Receive as VstTXD;	
  }

  // actuator interfaces
  uses
  {
	interface HplMsp430GeneralIO as TSio; 
	// interface HplMsp430Interrupt as TSinterrupt; // probably not used
	interface HplMsp430GeneralIO as SUSP;
	interface HplMsp430GeneralIO as CTSio;
	// interface HplMsp430Interrupt as CTSinterrupt; // probably not used
	interface HplMsp430GeneralIO as STBY;
	interface HplMsp430GeneralIO as RES;
  }
  
  // UART interfaces
  uses
  {
    // UART interfaces
	interface Resource as Uart1;
  	interface HplMsp430Usart as Uart1Control;
    //interface HplMsp430UsartInterrupts as Uart1Interrupt;
  }
  
  // debugging
  uses interface Leds;
  uses interface Timer<TMilli> as Timer1;
  uses interface Timer<TMilli> as Timer2;
}
implementation
{
  // function prototypes
  //task void receive();
  
  // declarations, definitions
  //error_t succ;
  /*static*/  //uint8_t rxData;
  //bool baudRateSelOK = FALSE;
  
  const uint16_t ONE_MS = 1330; // = 1.3 MHz, estimated by experiment
  
  // control variable
  uint16_t i;
  
  bool autoDetect; // when true the baud rate is detected automatically
  
  //bool lowToHigh = TRUE;
  //bool highToLow = FALSE;
  
  bool run = TRUE;
  
  

  
 /*
  * Initialisation of the UART 1 which is used for the communication between TmoteMini and V-Stamp.
  */
  command error_t VStamp.init(bool baudAutoDetect)
  {
	autoDetect = baudAutoDetect;
	// Initialisation of the V-Stamp pins
	call RES.makeOutput(); // Tmote Mini output, V-Stamp input 
	call RES.clr(); // enable RESET
	call SUSP.makeOutput(); // Tmote Mini output, V-Stamp input 
	call SUSP.set(); // disable SUSPEND
	call STBY.makeOutput(); // Tmote Mini output, V-Stamp input 
	call STBY.set(); // enable INIT, disable STANDBY
	call CTSio.makeInput(); // Tmote Mini input, V-Stamp output
	call TSio.makeInput(); // Tmote Mini input, V-Stamp output

	for (i = 2*ONE_MS; i > 0; i--); // hold RES low for at least 1ms after power on
	call RES.set(); // disable RESET
	
	for (i = 2*ONE_MS; i > 0; i--); // 2 ms recovery delay
	
	// debug LEDs
    call Leds.led0Off();
	
  	// acquire UART1:
  	//succ = call Uart1.request();
  	if (call Uart1.request())
  		return SUCCESS;
  	else
  		return FAIL;
  }
  
  event void Uart1.granted()
  {
	// After UART1 is assigned, set UART configuration
  	call Uart1Control.setModeUart(&uart1Config);

	if (autoDetect)
	{
		// Baud rate selection by auto-detect
		call Uart1Control.tx(vst_CR_comm);
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		call Uart1Control.tx(vst_CR_comm);
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		call Uart1Control.tx(vst_CR_comm);
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		
		// after command for locking the baud rate is sent 3 times (it's to play it safe), wait for ~75 ms at least
		for (i = 30*ONE_MS; i > 0; i = i-1);
		for (i = 30*ONE_MS; i > 0; i = i-1);
		for (i = 30*ONE_MS; i > 0; i = i-1);
	}
	
  	// debug LEDs
    call Leds.led0On();
	
#ifdef RESET

	// Reset device
	call RES.clr();
	for (i = 2*ONE_MS; i > 0; i--);		// wait for 2ms after RESET until all pins are available
	call RES.set();
	
#endif

 /***********************************************************************************
  * for testing only
  ************************************************************************************/

//#define SONG

#ifdef SONG

	// timer 1: play a song
	call Uart1Control.tx(0x12); // resume command
	while (!call Uart1Control.isTxEmpty());
	
	call Uart1Control.tx(0x01); // signal command
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(0x2D); // -
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(0x38); // 8
	while (!call Uart1Control.isTxEmpty());
	call Uart1Control.tx(0x67); // g
	while (!call Uart1Control.isTxEmpty());
		
	call Uart1Control.tx(0x01); // signal command:  play sound
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(0x30); // 0
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(0x26); // &
	while (!call Uart1Control.isTxEmpty());
		
	call Uart1Control.tx(0x01); // signal command
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(0x39); // 9
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(0x38); // 8
	while (!call Uart1Control.isTxEmpty());
	call Uart1Control.tx(0x69); // i
	while (!call Uart1Control.isTxEmpty());
		
	call Uart1Control.tx(0x0D); // signal to start speaking
	while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent

	call Timer1.startPeriodic(5000);
	
#endif

//#define SPEAK

#ifdef SPEAK

	call Timer2.startPeriodic(1000);
	
#endif

	signal VStamp.uartReady();
  }
  
  event void Timer1.fired() // play stored message
  {

#ifdef SONG

	if (run)
	{
		// debug LEDs
		call Leds.led0Off();
		
		call SUSP.clr();
		
		run = FALSE;
	}
	else
	{
		// debug LEDs
		call Leds.led0On();
		
		call SUSP.set();
		
		run = TRUE;
	}
	
#endif

  }
  
  event void Timer2.fired() // text2speech
  {
  
#ifdef SPEAK
  
	if (run)
	{
		// debug LEDs
		call Leds.led0Off();
		
		call Uart1Control.tx(0x01); // signal command
		while (!call Uart1Control.isTxEmpty()); 
		call Uart1Control.tx(0x74); // set text mode
		while (!call Uart1Control.isTxEmpty());
		
		/*
		call Uart1Control.tx(0x01); // signal command
		while (!call Uart1Control.isTxEmpty()); 
		call Uart1Control.tx(0x39); // select Alvin
		while (!call Uart1Control.isTxEmpty());
		call Uart1Control.tx(0x4F); // select Alvin
		while (!call Uart1Control.isTxEmpty());
		*/
		
		// send message to speak
		call Uart1Control.tx(0x68); // "h"
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		call Uart1Control.tx(0x65); // "e"
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		call Uart1Control.tx(0x6C); // "l"
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		call Uart1Control.tx(0x6C); // "l"
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		call Uart1Control.tx(0x6F); // "o"
		while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent
		

		run = FALSE;
	}
	else
	{
		call Uart1Control.tx(0x0D); // signal to start speaking

		// debug LEDs
		call Leds.led0On();
		
		run = TRUE;
	}
	
#endif
	
  }
  
/*********************************end of test section******************************/

 /*
  * Select Baud-Rate by command after baud rate was set once at the beginning.
  */
  command void VStamp.setBaudRate(uint8_t baudRateN1, uint8_t baudRateN2)
  {
	call Uart1Control.tx(vst_signal_comm); // signal command
	while (!call Uart1Control.isTxEmpty()); 
	if (baudRateN1 != 0x30)
	{
		call Uart1Control.tx(baudRateN1); // send baud rate
		while (!call Uart1Control.isTxEmpty());
		call Uart1Control.tx(baudRateN2); // send baud rate
		while (!call Uart1Control.isTxEmpty());
	}
	else
	{
		call Uart1Control.tx(baudRateN2); // send baud rate
		while (!call Uart1Control.isTxEmpty());
	}
	
	call Uart1Control.tx(vst_baudRateSel_comm); // baud rate command
	while (!call Uart1Control.isTxEmpty());
  }

 /*
  * Adjust synthesiser's speed.
  */
  command void VStamp.setSpeed(uint8_t speechRate1, uint8_t speechRate2)
  {
	call Uart1Control.tx(vst_signal_comm); // signal command
	while (!call Uart1Control.isTxEmpty());
	
	if (speechRate1 != 0x30)
	{
		call Uart1Control.tx(speechRate1); // send speech rate
		while (!call Uart1Control.isTxEmpty());
		call Uart1Control.tx(speechRate2); // send speech rate
		while (!call Uart1Control.isTxEmpty());
	}
	else
	{
		call Uart1Control.tx(speechRate2); // send speech rate
		while (!call Uart1Control.isTxEmpty());
	}
	
	call Uart1Control.tx(vst_speechRate_comm); // speech rate command
	while (!call Uart1Control.isTxEmpty());
  }
  
 /*
  * Adjust synthesiser's output volume. 
  */
  command void VStamp.setVolume(uint8_t volume)
  {
	call Uart1Control.tx(vst_signal_comm); // signal command
	while (!call Uart1Control.isTxEmpty());
	call Uart1Control.tx(volume); // send volume
	while (!call Uart1Control.isTxEmpty());
	call Uart1Control.tx(vst_volume_comm); // volume command
	while (!call Uart1Control.isTxEmpty());
  }

 /*
  * Select one of the standard voices.  
  */
  command void VStamp.setVoice(uint8_t voice1, uint8_t voice2)
  {
	call Uart1Control.tx(vst_signal_comm); // signal command
	while (!call Uart1Control.isTxEmpty());
	
	if (voice1 != 0x30)
	{
		call Uart1Control.tx(voice1); // send voice
		while (!call Uart1Control.isTxEmpty());
		call Uart1Control.tx(voice2); // send voice
		while (!call Uart1Control.isTxEmpty());
	}
	else
	{
		call Uart1Control.tx(voice2); // send voice
		while (!call Uart1Control.isTxEmpty());
	}
	
	call Uart1Control.tx(vst_voice_comm); // voice command
	while (!call Uart1Control.isTxEmpty());
  }

 /*
  * Send text to be spoken.  
  */
  command void VStamp.sendText(const char* text)
  {
	uint16_t n;
	
	call Uart1Control.tx(vst_signal_comm); // signal command
	while (!call Uart1Control.isTxEmpty()); 
	call Uart1Control.tx(vst_textMode_comm); // set text mode
	while (!call Uart1Control.isTxEmpty());
	
	n = strlen(text);
	for (i = 0; i <= n; i = i+1)
	{
		call Uart1Control.tx(text[i]); // send
		while (!call Uart1Control.isTxEmpty());
		
		// all data sent is routed through a high speed 16-byte buffer within V-Stamp
		if ((i+1)%8 == 0) // check CTS every 8 byte
			while (call CTSio.get()); // if CTS goes high -> buffer is full -> wait		
	}
	
	call Uart1Control.tx(vst_CR_comm); // signal to start speaking
	while (!call Uart1Control.isTxEmpty());
  }
  

 /*
  * Play a sound file which was recorded or downloaded.
  */
  command void VStamp.playSound(uint8_t fileN1, uint8_t fileN2)
  {
	call Uart1Control.tx(vst_signal_comm); // signal command
	while (!call Uart1Control.isTxEmpty());
	
	if (fileN1 != 0x30)
	{
		call Uart1Control.tx(fileN1); // send number of file
		while (!call Uart1Control.isTxEmpty());
		call Uart1Control.tx(fileN2); // send number of file
		while (!call Uart1Control.isTxEmpty());
	}
	else
	{
		call Uart1Control.tx(fileN2); // send number of file
		while (!call Uart1Control.isTxEmpty());
	}
	 
	call Uart1Control.tx(vst_play_comm); // play sound
	while (!call Uart1Control.isTxEmpty());
		
	call Uart1Control.tx(vst_CR_comm); // signal to start speaking
	while (!call Uart1Control.isTxEmpty()); // wait until entire byte is sent*/
  }

 /*
  * Playback, text-to-speech or recording is suspended.
  */
  command void VStamp.suspend()
  {
	call SUSP.clr();
  }
  
 /*
  * Playback, text-to-speech or recording is resumed.
  */
  command void VStamp.resume()
  {
	call SUSP.set();
  }
  
 /*
  * Resets the V-Stamp module.
  */
  command void VStamp.reset()
  {
	// Reset device
	call RES.clr();
	for (i = 2*ONE_MS; i > 0; i--);		// wait for 2ms after RESET until all pins are available
	call RES.set();
  }
  
  /**
   * The V-Stamp stops whatever it is doing and flushes the input buffer 
   * of all text and commands.
   */
  command void VStamp.stop() 
  {
    //cl
  	call Uart1Control.tx(vst_signal_stop); // signal command
  	while (!call Uart1Control.isTxEmpty());

  }


/***************TRASH********************************

  // async event void CTSinterrupt.fired()
  // {
	if (
	call CTSinterruppt.disable();
  // }
  
  // async event void TSinterrupt.fired()
  // {
	if (
	call CTSinterruppt.disable();
  // }
 // /*
   // *
   // */	
  // async event void Uart1Interrupt.rxDone(uint8_t data)
  // {
    // rxData = data; 
    // post receive();
  // }
  
  // task void receive()
  // {
    // atomic
	// {
		
		
		// if (rxData == vst_lockAck_comm)
			// baudRateSelOK = TRUE;
		
	//	else if ()
	//	else
    
	// /*if (rxCounter == 0 && rxData == START_BYTE) { // Start of a new packet
      // rxMsg[rxCounter] = rx_data;
      // rxCounter++;
    //  call Leds.led1Off(); 
      // return;
    // } 
    // if (rxCounter > 0) { // byte is inside a packet
      // rxMsg[rxCounter] = rx_data;
      // if (rxCounter == 24) { // last byte of packet received
    //    call Leds.led1On();
        // rxCounter = 0;
        // signal Receive.receive(&rxMess, &rxMsg, 25);
      // } 
      // else { // it was not the last byte of the packet
        // rxCounter++;
      // }*/
	// }
  // }
  


 // /*
   // *
   // */
  // async event void Uart1Interrupt.txDone()
  // {
	// /* do nothing */
  // }
  
 // /*
   // * Returns the maximum payloadlength of a message in bytes.
   // */
  // command uint8_t VstTXD.payloadLength(message_t *msg)
  // {
	// return 1;
  // }
  
  
 // /*
   // *
   // */
  // command void *VstTXD.getPayload(message_t *msg, uint8_t *len)
  // {
	// return SUCCESS;
  // }
  
  
 // /*
   // *
   // */
  // command error_t VstRXD.send(am_addr_t addr, message_t* msg, uint8_t len)
  // {
	// /* yet to implement */
  // }
  
 // /*
   // *
   // */
  // command error_t VstRXD.cancel(message_t *msg)
  // {
	// return FAIL;
  // }
  
 // /*
   // *
   // */
  // command void *VstRXD.getPayload(message_t *msg)
  // {
	// /* yet to implement */
  // }
  
 // /*
   // *
   // */
  // command uint8_t VstRXD.maxPayloadLength()
  // {
	// /* yet to implement */
  // }

/***************TRASH****************/

}
