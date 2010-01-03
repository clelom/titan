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
* Implementation of the miniature barometer module MS5540B:
* pressure and temperature reading and SW compensation following Intersema MS5540B datasheet. 
*
* Created: 09.12.2007
* Last modified: 09.12.2007
*
************************************************************************************/

generic module MS5540BSpiCommP
{
  provides interface MS5540B;
  
  // SPI interfaces
  provides interface Msp430SpiConfigure;
  uses interface Resource as PressResource;
  uses interface SpiByte;
  uses interface SpiPacket;
}
implementation
{
  command error_t sensorInit()
  {
	call PressResource.request();
	return SUCCESS;
  }
  
  event void PressResource.granted()
  {
	call reset();		
  }
  
  command  error_t reset()
  {
	call error_t send(&txBufReset, NULL, 21);
	return SUCCESS;
  }
  
  event void sendDone(uint8_t *txBuf, uint8_t *rxBuf, uint16_t len, error_t error) // parameters ??
  {
    call error_t send(&txBufW1W3, NULL, 21);
  }
  
  event void sendDone(uint8_t *txBuf, uint8_t *rxBuf, uint16_t len, error_t error) // parameters ??
  {
    x = rxBuf;
    call error_t send(&txBufW2W4, NULL, 21);
  }
  
  event void sendDone(uint8_t *txBuf, uint8_t *rxBuf, uint16_t len, error_t error) // parameters ??
  {
    y = rxBuf;
	// convert calibration data into coeff
    signal sensorInitDone(result);
  }


  }
}
