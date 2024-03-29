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
 * Switchmodule pinouts
 *
 * @author Andreas Breitenmoser
 *
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
TOSH_ASSIGN_PIN(ADC0, 6, 0);
TOSH_ASSIGN_PIN(ADC1, 6, 1);
TOSH_ASSIGN_PIN(ADC2, 6, 2);
TOSH_ASSIGN_PIN(ADC3, 6, 3);

// HUMIDITY
TOSH_ASSIGN_PIN(HUM_SDA, 1, 5);
TOSH_ASSIGN_PIN(HUM_SCL, 1, 6);
TOSH_ASSIGN_PIN(HUM_PWR, 1, 7);

// GIO pins
TOSH_ASSIGN_PIN(GIO0, 2, 0);
TOSH_ASSIGN_PIN(GIO1, 2, 1);
TOSH_ASSIGN_PIN(GIO2, 2, 3);
TOSH_ASSIGN_PIN(GIO3, 2, 6);

// 1-Wire
TOSH_ASSIGN_PIN(ONEWIRE, 2, 4);

void HUMIDITY_MAKE_CLOCK_OUTPUT() { TOSH_MAKE_HUM_SCL_OUTPUT(); }
void HUMIDITY_MAKE_CLOCK_INPUT() { TOSH_MAKE_HUM_SCL_INPUT(); }
void HUMIDITY_CLEAR_CLOCK() { TOSH_CLR_HUM_SCL_PIN(); }
void HUMIDITY_SET_CLOCK() { TOSH_SET_HUM_SCL_PIN(); }
void HUMIDITY_MAKE_DATA_OUTPUT() { TOSH_MAKE_HUM_SDA_OUTPUT(); }
void HUMIDITY_MAKE_DATA_INPUT() { TOSH_MAKE_HUM_SDA_INPUT(); }
void HUMIDITY_CLEAR_DATA() { TOSH_CLR_HUM_SDA_PIN(); }
void HUMIDITY_SET_DATA() { TOSH_SET_HUM_SDA_PIN(); }
char HUMIDITY_GET_DATA() { return TOSH_READ_HUM_SDA_PIN(); }

#define HUMIDITY_TIMEOUT_MS          30
#define HUMIDITY_TIMEOUT_TRIES       20

enum {
  // Sensirion Humidity addresses and commands
  TOSH_HUMIDITY_ADDR = 5,
  TOSH_HUMIDTEMP_ADDR = 3,
  TOSH_HUMIDITY_RESET = 0x1E
};

// FLASH
TOSH_ASSIGN_PIN(FLASH_PWR, 4, 3);
TOSH_ASSIGN_PIN(FLASH_CS, 4, 4);
TOSH_ASSIGN_PIN(FLASH_HOLD, 4, 7);

// PROGRAMMING PINS (tri-state)
//TOSH_ASSIGN_PIN(TCK, );
TOSH_ASSIGN_PIN(PROG_RX, 1, 1);
TOSH_ASSIGN_PIN(PROG_TX, 2, 2);

// need to undef atomic inside header files or nesC ignores the directive
#undef atomic

#endif // _H_hardware_h
