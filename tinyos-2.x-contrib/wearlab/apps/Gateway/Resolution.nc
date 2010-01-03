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
 *  Testapplication for Serial Interface over UART1 (SPI 1)  to the nRF2401
 *
 *  @author Christoph Walser
 *  @date   November 7 2006
 *
 **/
 
#include "Gateway.h"
 
module Resolution {
  provides interface AddressResolution;
}

implementation {
  
  command uint8_t AddressResolution.getRadio(uint16_t addr) {
    uint8_t top = (uint8_t)(addr >> 8); // Decide destination upon top byte of address
    if (top == 0x00)
      return CC2420;
    else if (top == 0x01)
      return NRF2401;
    else if (top == 0x02)
      return BLUETOOTH;
    else
      return UNKNOWN;
  }

}
