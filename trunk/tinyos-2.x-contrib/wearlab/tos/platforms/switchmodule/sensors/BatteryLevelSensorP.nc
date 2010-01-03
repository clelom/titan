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
* Implementation of the "battery level sensor" driver.
* This is a basic implementation following TEP109.
*
* Created: 30.11.2007
* Last modified: 30.11.2007
*
************************************************************************************/

#include "sensors.h"

module BatteryLevelSensorP
{
  // user interfaces
  //provides interface Read<bat_read_t*>;
  provides interface DeviceMetadata;

  // ADC interfaces
  provides interface AdcConfigure<const msp430adc12_channel_config_t>[uint8_t channel];  
  //uses interface Read<uint16_t> ReadBat;
}
implementation
{

/* alternative way, too cumbersome
  // stores the values read from the battery level sensor
  bat_read_t batReadVal;

  // initiate reading
  command error_t Read.read()
  {
    return call ReadBat.read();
  }
  
  // store value
  event void Read.readDone(error_t result, uint16_t val)
  {
    batReadVal = val;
    signal Read.readDone(result, &batReadVal);
  }
*/

  async command const msp430adc12_channel_config_t* AdcConfigure.getConfiguration[uint8_t channel]()
  {
    return &(channelConfigs[channel]);
  }
  
  command uint8_t DeviceMetadata.getSignificantBits()
  {
    return 12;
  }
}
