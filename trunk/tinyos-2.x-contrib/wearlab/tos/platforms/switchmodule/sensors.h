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

/************************************************************************
* Sensor Driver
*
* @author: Andreas Breitenmoser
*
 * Data structures used by acceleration, light and pressure sensors.
*
* Created: 27.11.2007
* Last modified: 30.11.2007
*
************************************************************************/

#ifndef SENSORS_H
#define SENSORS_H

#include <Msp430Adc12.h>	// for configuring the ADC
#include <Msp430usart.h>	// for configuring the USART


/****************
*  Data structures	*
*****************/

// data structure for the 3-axis accelerometer readings
typedef struct acc_read_t
{
  uint16_t x;
  uint16_t y;
  uint16_t z;
} acc_read_t;

// data structure for the ambient light sensor readings
typedef uint16_t light_read_t;

// data structure for the miniature barometer module readings
typedef uint16_t temp_read_t;

typedef struct press_read_t
{
  uint16_t temp;
  uint16_t press;
} press_read_t;

/*// only for test
typedef struct press_read_t
{
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
  pressReadVal.rxCalMSB = rxCalMSB;
  pressReadVal.rxCalMSBLSB = rxCalMSBLSB;
  pressReadVal.rxCalLSB = rxCalLSB;
  pressReadVal.rxAdcMSB = rxAdcMSB;
  pressReadVal.rxAdcLSB = rxAdcLSB;
  pressReadVal.rxD1 = rxD1;
  pressReadVal.rxD2 = rxD2;
} press_read_t;
*/


// data structure for the battery level sensor readings
typedef uint16_t bat_read_t;


/***********
*  ADC	*
***********/

// ADC channel definitions
enum
{
  // acceleration sensor ADXL330
  ACC_X,
  ACC_Y,
  ACC_Z,
  
  // light sensor APDS9003
  LUX,
  
  // battery level sensor
  VOLT
};

// configuration of ADC channels (cf. "..\opt\tinyos-2.x\tos\chips\msp430\adc12\Msp430Adc12.h" for the definitions of the following constants)
const msp430adc12_channel_config_t channelConfigs[] =
{
  // acceleration sensor ADXL330
  { // channel X
	INPUT_CHANNEL_A0, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
    SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
    SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
  },
  { // channel Y
    INPUT_CHANNEL_A1, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
    SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
    SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
  },
  { // channel Z
    INPUT_CHANNEL_A2, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
    SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
    SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
  },
  
  // light sensor APDS9003
  {
    INPUT_CHANNEL_A3, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
    SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
    SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
  },
  
  // battery level sensor
  {
    INPUT_CHANNEL_A4, REFERENCE_VREFplus_AVss, REFVOLT_LEVEL_1_5,
    SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
    SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
  }
};


/***********
*  SPI	*
***********/

// mode SPI 1
enum
{
	INIT_SPI = 0,
    RX_SPI   = 1,
    TX_SPI   = 2
};

// configuration of SPI 1 bus (cf. "..\opt\tinyos-2.x\tos\chips\msp430\usart\msp430usart.h" for the definitions of the following constants)
msp430_spi_union_config_t spi1Config =
{
  {
    ubr : 0x0002, 	// clock division factor
    ssel : 0x02, 	// clock source: 10=SMCLK [master]
    clen : 1, 		// Character length (0=7-bit data; 1=8-bit data) 
    listen : 0, 	//Listen enable (0=disabled; 1=enabled, feed tx back to receiver) 
    mm : 1, 		//master mode
    // returns data stream correctly from slave
	ckph : 0, 		// Clock phase (0=normal; 1=half-cycle delayed) 
	// needs shift operations but sending is proper
	//ckph : 1, 		// Clock phase (0=normal; 1=half-cycle delayed) 
    ckpl : 0, 		//Clock polarity (0=inactive is low && data at rising edge; 1=inverted) 
    stc : 1			//Slave transmit (0=4-pin SPI && STE enabled; 1=3-pin SPI && STE disabled) 
  }
};


// SPI data sequences:

// RESET sequence (21-bit)
uint8_t ms_reset_sequ[] = {0xAA, 0xAA, 0x00};

// Word1, Word3 READING sequence (12-bit)
uint8_t ms_W1_sequ[] = {0xEA, 0x80};
uint8_t ms_W3_sequ[] = {0xEC, 0x80};

// Word2, Word4 READING sequence (12-bit)
uint8_t ms_W2_sequ[] = {0xEB, 0x00};
uint8_t ms_W4_sequ[] = {0xED, 0x00};

// D1 ACQUISITION sequence (10-bit)
uint8_t ms_D1_sequ[] = {0xF4, 0x00};

// D2 ACQUISITION sequence (10-bit)
uint8_t ms_D2_sequ[] = {0xF2, 0x00};

 
#endif
