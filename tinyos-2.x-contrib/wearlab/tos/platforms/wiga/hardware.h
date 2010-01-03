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
 * @author Fabian Schenkel
 */

#ifndef _H_hardware_h
#define _H_hardware_h

#include "msp430hardware.h"

// LEDs
TOSH_ASSIGN_PIN(RED_LED, 5, 4);
TOSH_ASSIGN_PIN(GREEN_LED, 5, 5);
TOSH_ASSIGN_PIN(YELLOW_LED, 5, 6);

// CC2420 RADIO #defines
TOSH_ASSIGN_PIN(RADIO_CSN, 4, 2);
TOSH_ASSIGN_PIN(RADIO_VREF, 4, 5);
TOSH_ASSIGN_PIN(RADIO_RESET, 4, 6);
TOSH_ASSIGN_PIN(RADIO_FIFOP, 1, 0);
TOSH_ASSIGN_PIN(RADIO_SFD, 4, 1);
TOSH_ASSIGN_PIN(RADIO_GIO0, 1, 3);
TOSH_ASSIGN_PIN(RADIO_FIFO, 1, 3);
TOSH_ASSIGN_PIN(RADIO_GIO1, 1, 4);
TOSH_ASSIGN_PIN(RADIO_CCA, 1, 4);

TOSH_ASSIGN_PIN(CC_FIFOP, 1, 0);
TOSH_ASSIGN_PIN(CC_FIFO, 1, 3);
TOSH_ASSIGN_PIN(CC_SFD, 4, 1);
TOSH_ASSIGN_PIN(CC_VREN, 4, 5);
TOSH_ASSIGN_PIN(CC_RSTN, 4, 6);

// UART pins
TOSH_ASSIGN_PIN(SOMI0, 3, 2);
TOSH_ASSIGN_PIN(SIMO0, 3, 1);
TOSH_ASSIGN_PIN(UCLK0, 3, 3);
TOSH_ASSIGN_PIN(UTXD0, 3, 4);
TOSH_ASSIGN_PIN(URXD0, 3, 5);
TOSH_ASSIGN_PIN(UTXD1, 3, 6);
TOSH_ASSIGN_PIN(URXD1, 3, 7);
TOSH_ASSIGN_PIN(UCLK1, 5, 3);
TOSH_ASSIGN_PIN(SOMI1, 5, 2);
TOSH_ASSIGN_PIN(SIMO1, 5, 1);

// ADC
TOSH_ASSIGN_PIN(ADC0, 6, 0); // Battery Voltage
TOSH_ASSIGN_PIN(ADC1, 6, 5); // AUX 1
TOSH_ASSIGN_PIN(ADC2, 6, 6); // AUX 2
TOSH_ASSIGN_PIN(ADC3, 6, 7); // AUX 3

// PROGRAMMING PINS (tri-state)
//TOSH_ASSIGN_PIN(TCK, );
TOSH_ASSIGN_PIN(PROG_RX, 1, 1);
TOSH_ASSIGN_PIN(PROG_TX, 2, 2);

// need to undef atomic inside header files or nesC ignores the directive
#undef atomic

#endif // _H_hardware_h
