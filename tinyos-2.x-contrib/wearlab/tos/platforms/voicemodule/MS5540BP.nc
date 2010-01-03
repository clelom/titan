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
* Sensor Driver
*
* @author Andreas Breitenmoser
*
* Implementation of the miniature barometer module MS5540B.
*
* Created: 27.11.2007
* Last modified: 22.12.2007
*
************************************************************************************/

#include "sensors.h"

module MS5540BP
{
  // user interfaces
  provides
  {
    interface Init as MS5540BInit;
	interface Read<press_read_t*> as PressRead;
	interface DeviceMetadata as PressDevMetadata;
	interface Read<temp_read_t> as TempRead;
	interface DeviceMetadata as TempDevMetadata;	
  }

  // system clock interface
  uses interface HplMsp430GeneralIO as PressMCL;
  
  // SPI interfaces
  uses interface Resource as Spi1;
  uses interface HplMsp430Usart as Spi1Control;
  uses interface Timer<TMilli> as Timer;
  
  // debugging
  uses interface Leds;
}
implementation
{
  // function prototypes
  void sysInitialisation();
  void reset();
  void readWords();
  void convData();
  void getPress();
  task void readTemperature();
  
#ifdef TEST_SPI	
  void testSPI(); // test SPI: contains a endless loop for permanent writing
#endif

  // declarations, definitions
  uint8_t i = 0;
  bool sensorState; // 1 = temperature only, 0 = temperature and pressure
  bool actMeasurement; // 1 = temperature measurement, 0 = pressure measurement
  
  uint8_t rxAdcMSB; // first  8 bit of data
  uint8_t rxAdcLSB; // last 8 bit of data
  
  uint16_t rxD1; // ADC-data-out of pressure measurement
  uint16_t rxD2; // ADC-data-out of temperature measurement
  
  uint8_t rxCalMSB; // first  8 bit of calibration data
  uint8_t rxCalMSBLSB;
  uint8_t rxCalLSB; // last 8 bit of calibration data
  
  uint16_t rxW1; // calibration data read-out sequence for Word1
  uint16_t rxW2; // calibration data read-out sequence for Word2
  uint16_t rxW3; // calibration data read-out sequence for Word3
  uint16_t rxW4; // calibration data read-out sequence for Word4
  
  uint16_t C1; // Pressure sensitivity (15 bit)
  uint16_t C2; // Pressure offset (12 bit)
  uint16_t C3; // Temperature coefficient of pressure sensitivity (10 bit)
  uint16_t C4; // Temperature coefficient of pressure offset (10 bit)
  uint16_t C5; // Reference Temperature(11 bit)
  uint8_t C6; // Temperature coefficient of the temperature (6 bit)
  
  uint16_t UT1;
  uint16_t OFF;
  uint16_t SENS;
  uint16_t X;
  
  uint16_t dT; // Difference between actual temperature and reference temperature
  
  // Return values
  press_read_t pressReadVal;
  temp_read_t tempReadVal;
  
  
 /**
  * Initialisation of the SPI 1 which is used for the communication between TmoteMini and MS5540B.
  * @return SUCCESS is returned if SPI 1 is initialised properly, FAIL otherwise.
  */
  command error_t MS5540BInit.init()
  {
	// debug LEDs
    call Leds.led0Off();
	
	// acquire SPI 1:
	call Spi1.request();
	return SUCCESS;
	/*
  	return call Spi1.request();
  	*/
  }

  
  event void Spi1.granted()
  {
  	// SPI configuration
  	call Spi1Control.setModeSpi(&spi1Config);
  	
  	// debug LEDs
    call Leds.led0On();
	
	//for SwitchModule
	sysInitialisation();
  }
	
	/* TODO:
	for VoiceModule: spi (pressure sensor) and uart (v-stamp) both share usart 1 interface:
	 that means an arbitration is necessary, both have to release the usart when they've finished communication.
	 Only V-stamp is able to set an interrupt, so it triggers when it needs the resource and when pressure sensor can communicate over SPI. 
	-> pressure sensor (vgl. lmx): "default user" (later power manager will be default user) -> implement using interface ResourceRequested, i.e. whenever V-stamp requests usart 1,
	pressure sensor releases the resource
	-> V-stamp (vgl. nrf): at each interrupt the usart 1 is requested
	
	switch (mode)
	{
		case INIT_SPI:	loadInit();
						break;
		case TX_SPI:	txGranted();
						break;
		case RX_SPI:	rxGranted();
		
		default:		/* should never be the case */
    //}

	
#ifdef TEST_SPI	
	void testSPI()
	{
		/* some code for testing ... */
	}
#endif
  
  
  void sysInitialisation()
  {
	// enable system clock 32.768 kHz
	call PressMCL.makeOutput();
	call PressMCL.selectModuleFunc();
	
	//reset
	reset();
	
	// acquire Word1 to Word4: 16 bit each
	//readWords();
	
	// convert calibration data into coefficients
	//convData();
  }
  
  
  void reset()
  {
	for (i = 0; i < 3; i++) // 21 bit -> 3 byte
	{
		call Spi1Control.tx(ms_reset_sequ[i]);
		while (!call Spi1Control.isTxEmpty());	// wait until the whole byte is sent
	}
  }
  
  
  /* TODO: reading out the calibration words does not work properly */
  void readWords()
  {
	atomic
	{
		// read Word1 (12 bit -> 2 byte)
		call Spi1Control.tx(ms_W1_sequ[0]);
		while (!call Spi1Control.isTxEmpty()); // wait until the whole byte is sent
		call Spi1Control.tx(ms_W1_sequ[1]);
		while (!call Spi1Control.isTxEmpty());
		rxCalMSB = call Spi1Control.rx(); // read first 3 bits of byte 1
		
		call Spi1Control.tx(0);	// Send dummy byte
		while (!call Spi1Control.isTxEmpty());
		rxCalMSBLSB = call Spi1Control.rx(); // read next 5 bits of byte 1 and first 3 bits of byte 2
		
		call Spi1Control.tx(0);	// Send dummy byte
		rxCalLSB = call Spi1Control.rx(); // read last 5 bits of byte 2
		
		// concatenate the bits to Word1
		rxCalMSB = rxCalMSB<<6;
		rxCalMSB = rxCalMSB | rxCalMSBLSB>>2;
		rxCalMSBLSB = rxCalMSBLSB<<6;
		rxCalLSB = rxCalMSBLSB | rxCalLSB>>2;
		
		rxW1 = rxCalMSB<<8 | rxCalLSB;
	}
	
	atomic
	{
		// read Word2 (12 bit -> 2 byte)
		call Spi1Control.tx(ms_W2_sequ[0]);
		while (!call Spi1Control.isTxEmpty()); // wait until the whole byte is sent
		call Spi1Control.tx(ms_W2_sequ[1]);
		while (!call Spi1Control.isTxEmpty());
		rxCalMSB = call Spi1Control.rx(); // read first 3 bits of byte 1
		
		call Spi1Control.tx(0);	// Send dummy byte
		while (!call Spi1Control.isTxEmpty());
		rxCalMSBLSB = call Spi1Control.rx(); // read next 5 bits of byte 1 and first 3 bits of byte 2
		
		call Spi1Control.tx(0);	// Send dummy byte
		rxCalLSB = call Spi1Control.rx(); // read last 5 bits of byte 2
		
		// concatenate the bits to Word2
		rxCalMSB = rxCalMSB<<6;
		rxCalMSB = rxCalMSB | rxCalMSBLSB>>2;
		rxCalMSBLSB = rxCalMSBLSB<<6;
		rxCalLSB = rxCalMSBLSB | rxCalLSB>>2;
		
		rxW2 = rxCalMSB<<8 | rxCalLSB;
	}
	
	atomic
	{
		// read Word3 (12 bit -> 2 byte)
		call Spi1Control.tx(ms_W3_sequ[0]);
		while (!call Spi1Control.isTxEmpty()); // wait until the whole byte is sent
		call Spi1Control.tx(ms_W3_sequ[1]);
		while (!call Spi1Control.isTxEmpty());
		rxCalMSB = call Spi1Control.rx(); // read first 3 bits of byte 1
		
		call Spi1Control.tx(0);	// Send dummy byte
		while (!call Spi1Control.isTxEmpty());
		rxCalMSBLSB = call Spi1Control.rx(); // read next 5 bits of byte 1 and first 3 bits of byte 2
		
		call Spi1Control.tx(0);	// Send dummy byte
		rxCalLSB = call Spi1Control.rx(); // read last 5 bits of byte 2
		
		// concatenate the bits to Word3
		rxCalMSB = rxCalMSB<<6;
		rxCalMSB = rxCalMSB | rxCalMSBLSB>>2;
		rxCalMSBLSB = rxCalMSBLSB<<6;
		rxCalLSB = rxCalMSBLSB | rxCalLSB>>2;
		
		rxW3 = rxCalMSB<<8 | rxCalLSB;
	}
	
	atomic
	{
		// read Word4 (12 bit -> 2 byte)
		call Spi1Control.tx(ms_W3_sequ[0]);
		while (!call Spi1Control.isTxEmpty()); // wait until the whole byte is sent
		call Spi1Control.tx(ms_W3_sequ[1]);
		while (!call Spi1Control.isTxEmpty());
		rxCalMSB = call Spi1Control.rx(); // read first 3 bits of byte 1
		
		call Spi1Control.tx(0);	// Send dummy byte
		while (!call Spi1Control.isTxEmpty());
		rxCalMSBLSB = call Spi1Control.rx(); // read next 5 bits of byte 1 and first 3 bits of byte 2
		
		call Spi1Control.tx(0);	// Send dummy byte
		rxCalLSB = call Spi1Control.rx(); // read last 5 bits of byte 2
		
		/*// only for test
		//pressReadVal.rxCalMSB = rxCalMSB;
		//pressReadVal.rxCalMSBLSB = rxCalMSBLSB;
		//pressReadVal.rxCalLSB = rxCalLSB;
		*/
		
		// concatenate the bits to Word4
		rxCalMSB = rxCalMSB<<6;
		rxCalMSB = rxCalMSB | rxCalMSBLSB>>2;
		rxCalMSBLSB = rxCalMSBLSB<<6;
		rxCalLSB = rxCalMSBLSB | rxCalLSB>>2;
		
		rxW4 = rxCalMSB<<8 | rxCalLSB;
	}
  }
  
  
  void convData()
  {
	C1 = rxW1 >> 1;
	
	C2 = rxW3 & 0x3F;
	C2 = (C2 << 6) | (rxW4 & 0x3F);
	
	C3 = rxW4 >> 6;
	
	C4 = rxW3 >> 6;
	
	C5 = rxW1 & 0x01;
	C5 = (C5 << 10) | (rxW2 >> 6);
	
	C6 = rxW2 & 0x3F;
  }
  
  
 /**
  * Start a temperature measurement.
  * @return SUCCESS is returned if the measurement will be made.
  */
  command error_t TempRead.read()
  {
	sensorState = 1;
	post readTemperature();
	return SUCCESS;	
  }
  
  
 /**
  * Start a pressure measurement.
  * @return SUCCESS is returned if the measurement will be made.
  */
  command error_t PressRead.read()
  {
	sensorState = 0;
	post readTemperature();
	return SUCCESS;
  }
  
  
  task void readTemperature()
  {
	actMeasurement = 1;
	
	atomic
	{
		// reset module before conversion
		reset();
		
		// acquire D2 (10 bit -> 2 byte)
		call Spi1Control.tx(ms_D2_sequ[0]);
		while (!call Spi1Control.isTxEmpty()); // wait until the whole byte is sent
		call Spi1Control.tx(ms_D2_sequ[1]);
		
		// wait 40 ms until conversion is done (average conversion duration:  ~ 33 ms)
		call Timer.startOneShot(40);
	}
  }
  
  
  void getPress()
  {
	actMeasurement = 0;
	
	atomic
	{
		// reset module before conversion
		reset();
		
		// acquire D1 (10 bit -> 2 byte)
		call Spi1Control.tx(ms_D1_sequ[0]);
		while (!call Spi1Control.isTxEmpty()); // wait until the whole byte is sent
		call Spi1Control.tx(ms_D1_sequ[1]);
		
		// wait 40 ms until conversion is done (average conversion duration:  ~ 33 ms)
		call Timer.startOneShot(40);
	}
  } 
  
  
  event void Timer.fired()
  {
	if (actMeasurement) // temperature measurement
	{
		atomic
		{
			// read MS 8-bit
			call Spi1Control.tx(0);	// Send dummy byte
			while (!call Spi1Control.isTxEmpty());
			rxAdcMSB = call Spi1Control.rx();
			
			//  read LS 8-bit
			call Spi1Control.tx(0);	// Send dummy byte
			while (!call Spi1Control.isTxEmpty());
			rxAdcLSB = call Spi1Control.rx();
			
			rxD2 = rxAdcMSB<<8 | rxAdcLSB;
		}
		
		// calculate calibration temperature
		//UT1 = 8*C5 + 20224;
		UT1 = 8*628 + 20224;
		
		// calculate actual temperature
		dT =  rxD2 - UT1;
		//tempReadVal = 200 + dT*(C6+50)/(2^10);
		tempReadVal = 200 + dT*(25+50)/(2^10);
		
		/* additional optimisation: second-order temperature compensation ... cf. datasheet MS5540B */
		
		/*// only for test
		pressReadVal.rxW1 = rxW1;
		pressReadVal.rxW2 = rxW2;
		pressReadVal.rxW3 = rxW3;
		pressReadVal.rxW4 = rxW4;
		pressReadVal.temp = tempReadVal;
		pressReadVal.C1 = C1;
		pressReadVal.C2 = C2;
		pressReadVal.C3 = C3;
		pressReadVal.C4 = C4;
		pressReadVal.C5 = C5;
		pressReadVal.C6 = C6;
		pressReadVal.rxAdcMSB = rxAdcMSB;
		pressReadVal.rxAdcLSB = rxAdcLSB;
		pressReadVal.rxD2 = rxD2;
		*/
		
		if (sensorState) // called by task readTemperature
		{
			//signal TempRead.readDone(SUCCESS, tempReadVal);
			signal TempRead.readDone(SUCCESS, rxD2);
			
			/*// only for test
			signal TempRead.readDone(SUCCESS, &pressReadVal); // only for test
			*/
		}
		else // called by task readPressure
		{
			//pressReadVal.temp = tempReadVal;
			pressReadVal.temp = rxD2;
			
			getPress();
		}
	}
	else // Pressure  measurement
	{	
		atomic
		{
			// read MS 8-bit
			call Spi1Control.tx(0);	// Send dummy byte
			while (!call Spi1Control.isTxEmpty());
			rxAdcMSB = call Spi1Control.rx();		
			//  read LS 8-bit
			call Spi1Control.tx(0);	// Send dummy byte
			while (!call Spi1Control.isTxEmpty());
			rxAdcLSB = call Spi1Control.rx();
			
			rxD1 = rxAdcMSB<<8 | rxAdcLSB;
		}
		
		// calculate temperature compensated pressure
		//OFF = C2*4 + ((C4-512)*dT)/(2^12); // offset at actual temperature
		OFF = 1324*4 + ((393-512)*dT)/(2^12); // offset at actual temperature
		//SENS = C1 + (C3*dT)/(2^10) + 24576; // sensitivity at actual temperature
		SENS = 23470 + (737*dT)/(2^10) + 24576; // sensitivity at actual temperature
		
		X = (SENS * (rxD1-7168))/(2^14) - OFF;
		
		//pressReadVal.press = X*10/(2^5) + 2500;
		pressReadVal.press = rxD1;
		
		/*// only for test
		pressReadVal.rxAdcMSB = rxAdcMSB;
		pressReadVal.rxAdcLSB = rxAdcLSB;
		pressReadVal.rxD1 = rxD1;
		*/
		
		/* additional optimisation: second-order temperature compensation ... cf. datasheet MS5540B */
		
		signal PressRead.readDone(SUCCESS, &pressReadVal);
	 }
  }
  
  
 /**
  * Temperature measurement and pressure measurement. 
  * @return Return number of significant bits for temperature value.
  */
  command uint8_t TempDevMetadata.getSignificantBits()
  {
    return 16;
  }
  
  
 /**
  * Pressure measurement. 
  * @return Return number of significant bits for pressure value.
  */
  command uint8_t PressDevMetadata.getSignificantBits()
  {
    return 16;
  }
}
