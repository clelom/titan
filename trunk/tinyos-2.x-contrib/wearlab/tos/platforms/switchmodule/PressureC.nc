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
* Wrapper for the pressure sensors.
* It's a common naming layer atop a HIL. 
*
* Created: 27.11.2007
* Last modified: 03.12.2007
*
************************************************************************************/
 
#include "sensors.h"
 
generic configuration PressureC()
{
  provides
  {
	interface Init;
	interface Read<press_read_t*> as Pressure;
	interface DeviceMetadata as PressureDeviceMetadata;
	interface Read<temp_read_t> as Temperature;
	interface DeviceMetadata as TemperatureDeviceMetadata;
  }
}
implementation
{
  components new MS5540BC();
  
  Init = MS5540BC.Init;
  Pressure = MS5540BC.Press;
  PressureDeviceMetadata = MS5540BC.PressDeviceMetadata;
  Temperature = MS5540BC.Temp;
  TemperatureDeviceMetadata = MS5540BC.TempDeviceMetadata;
}
