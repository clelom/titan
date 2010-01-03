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
 *  Configurations for the nRF2401 
 *
 *  @author Christoph Walser
 *  @author Fabian Schenkel
 *
 **/

#ifndef _H_nrf2401_h
#define _H_nrf2401_h

#include "msp430usart.h"		//Needed for configuring the USART

msp430_spi_union_config_t spi1_config = { 
  {
    ubr : 2,
    ssel : 2, 
    clen : 1, 
    listen : 0, 
    mm : 1, 
    ckph : 1, //* 1/0: rx,tx bitverschiebung * 1/1 rx ok, tx tot * 0/0 rx,tx tot * 0/1 tx bitverschiebung, rx bit7 geht verloren
    ckpl : 0,
    stc : 1
  } 
};

msp430_spi_union_config_t spi1_rx_config = { 
  {
    ubr : 2,
    ssel : 2, 
    clen : 1, 
    listen : 0, 
    mm : 1, 
    ckph : 1, //* 1/0: rx,tx bitverschiebung * 1/1 rx ok, tx tot * 0/0 rx,tx tot * 0/1 tx bitverschiebung, rx bit7 geht verloren
    ckpl : 1,
    stc : 1
  } 
};
  
//Constants for configuration word of nRF2401:
enum {
	  RXEN 			= 0x01,          //RX/TX Operation (0=tx; 1=rx)
	  RF_CH 		= 0x34,	//26      //Frequency channel (has to be between 0 and 524)
	  
	  RF_PWR		= 0x01,	//0 dBm	//RF Output Power in TX Mode(00=-20dBm; 01=-10dBm; 10=-5dBm; 11=0dBm)
	  XO_F			= 0x0C,	//16 Mhz	//Crystal Frequency (000=4Mhz; 001=8Mhz; 010=12Mhz; 011=16Mhz; 100=20Mhz)
	  RFDR_SB		= 0x20,	//1		//RF data rate (0=250 kbps; 1=1 Mbps)
	  CM			= 0x40,	//1		//Communication mode (0=Direct Mode; 1=ShockBurst Mode)
	  RX2_EN		= 0x80,	//1		//Enable two channel receive mode (1=enabled; 0=disabled)
	  
	  CRC_EN		= 0x01,	//1		//Enable on-chip CRC generation/checking (1=enabled; 0=disabled)
	  CRC_L			= 0x02,	//16		//8 or 16 bit CRC (0=8bit; 1=16bit)
	  ADDR_W		= 0xA0,	//40		//Number of address bits (both RX channels)
	  
	  ADDR1_1		= 0x10,			//Receiver address channel 1
	  ADDR1_2		= 0x11,			//Receiver address channel 1
	  ADDR1_3		= 0x12,			//Receiver address channel 1
	  ADDR1_4		= 0x13,			//Receiver address channel 1
	  ADDR1_5		= 0x14,			//Receiver address channel 1
	  
	  ADDR2_1		= 0x16,			//Receiver address channel 2
	  ADDR2_2		= 0x17,			//Receiver address channel 2
	  ADDR2_3		= 0x18,			//Receiver address channel 2
	  ADDR2_4		= 0x19,			//Receiver address channel 2
	  ADDR2_5		= 0x20,			//Receiver address channel 2
	  
	  DATA1_W		= 0xC8,	//200	//Number of bits in RF package payload section for receive-channel 1 (256 bit packetlength - 40 bit address - 16 bit CRC)
	  
	  DATA2_W		= 0xC8,	//200	//Number of bits in RF package payload section for receive-channel 2 (256 bit packetlength - 40 bit address - 16 bit CRC)
	  
	  PLL_CTRL		= 0x00,			//Controls the setting of the PLL for test purposes (00=openTX/closed RX; 01=open TX/open RX; 10=closed TX/closed RX; 11=closed TX/closed RX)
	  TEST_1		= 0x1C,			//Reserved for testing
	  
	  TEST_2		= 0x08,			//Reserved for testing
	  
	  TEST_3		= 0x8E,			//Reserved for testing
  };

//Configuration word to initialize the nRF2401:
uint8_t nrf_init_config[15] = {
							DATA2_W,
							DATA1_W,
							ADDR2_1,
							ADDR2_2,
							ADDR2_3,
							ADDR2_4,
							ADDR2_5,
							ADDR1_1,
							ADDR1_2,
							ADDR1_3,
							ADDR1_4,
							ADDR1_5,
							ADDR_W + CRC_L + CRC_EN,
							RF_PWR + XO_F + RFDR_SB + CM,
							RXEN + RF_CH
};

typedef nx_struct nrf2401_header_t {
  nx_uint8_t dest[2];
} nrf2401_header_t;

typedef nx_struct nrf2401_footer_t {
} nrf2401_footer_t;

typedef nx_struct nrf2401_metadata_t {
} nrf2401_metadata_t;

typedef nx_struct nrf2401_packet_t {
  nrf2401_header_t head;
  nrf2401_footer_t foot;
  nx_uint8_t data[];
} nrf2401_packet_t;

#ifndef TOSH_DATA_LENGTH
#define TOSH_DATA_LENGTH 25
#endif

#endif//_H_nrf2401_h
