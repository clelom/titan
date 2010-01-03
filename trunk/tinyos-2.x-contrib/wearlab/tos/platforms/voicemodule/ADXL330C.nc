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
* Sensor driver for the accelerometer ADXL330.  The 3 axes are read and returned in a struct.
* Generic HIL component which connects to the ADC.
*
* Created: 27.11.2007
* Last modified: 30.11.2007
*
************************************************************************************/

#include "sensors.h"
 
generic configuration ADXL330C()
{
  provides interface Read<acc_read_t*> as Acceleration;
  provides interface DeviceMetadata as AccelerationDeviceMetadata;
}
implementation
{
  components new AdcReadClientC() as AdcReadClientX;
  components new AdcReadClientC() as AdcReadClientY;
  components new AdcReadClientC() as AdcReadClientZ;
  components ADXL330P;

  AdcReadClientX.AdcConfigure -> ADXL330P.AdcConfigure[ACC_X];
  AdcReadClientY.AdcConfigure -> ADXL330P.AdcConfigure[ACC_Y];
  AdcReadClientZ.AdcConfigure -> ADXL330P.AdcConfigure[ACC_Z];

  ADXL330P.ReadX -> AdcReadClientX.Read;
  ADXL330P.ReadY -> AdcReadClientY.Read;
  ADXL330P.ReadZ -> AdcReadClientZ.Read;
  
  Acceleration = ADXL330P.Read;
  AccelerationDeviceMetadata = ADXL330P.DeviceMetadata;
}
