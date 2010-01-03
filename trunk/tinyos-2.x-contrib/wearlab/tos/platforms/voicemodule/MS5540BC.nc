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
* Sensor driver for the miniature barometer module MS5540B.
* The air pressure and temperature are read and returned.
*
* Created: 27.11.2007
* Last modified: 03.12.2007
*
************************************************************************************/

#include "sensors.h"
 
generic configuration MS5540BC()
{  
  provides
  {
	interface Init;
	interface Read<press_read_t*> as Press;
	interface DeviceMetadata as PressDeviceMetadata;
	interface Read<temp_read_t> as Temp;
	interface DeviceMetadata as TempDeviceMetadata;
  }
}
implementation
{
  // sensor components
  components MS5540BP;
  
  // SPI components
  components new Msp430Usart1C() as Usart1;
  components HplMsp430GeneralIOC as PressureMCL;
  
  // components for debugging
  components LedsC;
  components new TimerMilliC() as Timer;
	  
  MS5540BP.Spi1 -> Usart1.Resource;
  MS5540BP.Spi1Control -> Usart1.HplMsp430Usart;
  
  MS5540BP.PressMCL -> PressureMCL.Port20;

  MS5540BP.Timer -> Timer;
  
  // debugging
  MS5540BP.Leds -> LedsC;
  
  
  Init = MS5540BP.MS5540BInit;
  Press = MS5540BP.PressRead;
  PressDeviceMetadata = MS5540BP.PressDevMetadata;
  Temp = MS5540BP.TempRead;
  TempDeviceMetadata = MS5540BP.TempDevMetadata;
}
