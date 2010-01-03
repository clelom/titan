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

#ifndef _H_lmx9820a_h
#define _H_lmx9820a_h

#include "msp430usart.h"		//Needed for configuring the USART

//#define USE_115200_BT

msp430_uart_union_config_t uart1_config = { 
  {
    utxe : 1, 
    urxe : 1, 
#ifdef USE_115200_BT
    ubr: UBR_1MHZ_115200,
    umctl: UMCTL_1MHZ_115200,
#else
    ubr: UBR_1MHZ_9600,
    umctl: UMCTL_1MHZ_9600, 
#endif
    ssel: 0x02, 
    pena: 0, // parity enable 0=disabled
    pev: 0, 
    spb: 0, //0, // stop bits 0=1 bit, 1=2 bit
    clen: 1, 
    listen: 0, 
    mm: 0, 
    ckpl: 0, 
    urxse: 0,
    urxeie: 1, 
    urxwie: 0,
    utxe : 1, 
    urxe : 1
  } 
};

typedef nx_struct lmx9820a_header_t {
  nx_uint16_t dest;
  nx_uint16_t src;
  nx_uint8_t length;
  nx_uint8_t type;
} lmx9820a_header_t;
#endif//_H_lmx9820a_h
