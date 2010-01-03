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
* Driver for reading out the battery level .
* Generic HIL component which connects to the ADC.
*
* Created: 30.11.2007
* Last modified: 30.11.2007
*
************************************************************************************/

#include "sensors.h"
 
generic configuration BatteryLevelSensorC()
{
  provides interface Read<bat_read_t*>;
  provides interface DeviceMetadata;
}
implementation
{
  components new AdcReadClientC();
  components BatteryLevelSensorP;

  AdcReadClient.AdcConfigure -> BatteryLevelSensorP.AdcConfigure[VOLT];
    
  Read = AdcReadClientC;
  DeviceMetadata = BatteryLevelSensorP.DeviceMetadata;
}
