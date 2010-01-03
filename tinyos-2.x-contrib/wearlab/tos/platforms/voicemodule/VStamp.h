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
* Header for the voice synthesiser module V-Stamp.
* The V-Stamp consists of a integrated text-to-speech processor and a tone generator.
* Sound files can be recorded, up- and downloaded and be played. 
*
* Created: 18.12.2007
* Last modified: 18.12.2007
*
************************************************************************************/


#ifndef VSTAMP_H
#define VSTAMP_H

#include "msp430usart.h" // for configuring the USART


// configuration of UART 1 (cf. "..\opt\tinyos-2.x\tos\chips\msp430\usart\msp430usart.h" for the definitions of the following constants)
msp430_uart_union_config_t uart1Config =
{ 
  {
    utxe : 1, 					// 1:enable tx module
    urxe : 1, 					// 1:enable rx module
    ubr:  UBR_1MHZ_9600, 		// or 0x69, 
    umctl: UMCTL_1MHZ_9600,
    ssel: 0x02, 
    pena: 0, 
    pev: 0, 
    spb: 0, 
    clen: 1, 					//Character length (0=7-bit data; 1=8-bit data)
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

// UART commands:

uint8_t vst_signal_comm = 0x01; // Default command character, ASCII character CTRL+A
uint8_t vst_CR_comm = 0x0D; // ASCII character CR
uint8_t vst_signal_stop = 0x18; // stop command

uint8_t vst_textMode_comm = 0x74; // ASCII character t
uint8_t vst_baudRateSel_comm = 0x68; // ASCII character h
uint8_t vst_speechRate_comm = 0x73; // ASCII character s
uint8_t vst_volume_comm = 0x76; // ASCII character v
uint8_t vst_voice_comm = 0x6F; // ASCII character o
uint8_t vst_play_comm = 0x26; // ASCII character &


#endif
