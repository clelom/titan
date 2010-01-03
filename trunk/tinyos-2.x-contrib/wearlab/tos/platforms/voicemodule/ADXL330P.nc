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
* Implementation of the accelerometer ADXL330 driver.
* This is a basic implementation following TEP109.
*
* Created: 27.11.2007
* Last modified: 30.11.2007
*
************************************************************************************/

#include "sensors.h"

module ADXL330P
{
  // user interfaces
  provides interface Read<acc_read_t*>;
  provides interface DeviceMetadata;

  // ADC interfaces
  provides interface AdcConfigure<const msp430adc12_channel_config_t*>[uint8_t channel];  
  uses interface Read<uint16_t> as ReadX;
  uses interface Read<uint16_t> as ReadY;
  uses interface Read<uint16_t> as ReadZ; 
}
implementation
{
  // stores the values read from the accelerometer
  acc_read_t accReadVal;

  // initiates a read sequence X->Y->Z
  command error_t Read.read()
  {
    return call ReadX.read();
  }
  
  // store value and initiate read on next channel
  event void ReadX.readDone(error_t result, uint16_t val)
  {
    atomic accReadVal.x = val; // set to "atomic" to avoid warnings
    call ReadY.read();
  }

  // store value and initiate read on next channel
  event void ReadY.readDone(error_t result, uint16_t val)
  {
    atomic accReadVal.y = val; // set to "atomic" to avoid warnings
    call ReadZ.read();
  }

  // store value and signal completion
  event void ReadZ.readDone(error_t result, uint16_t val)
  {
    atomic accReadVal.z = val; // set to "atomic" to avoid warnings
    signal Read.readDone(result, &accReadVal);
  }

  async command const msp430adc12_channel_config_t* AdcConfigure.getConfiguration[uint8_t channel]()
  {
    return &(channelConfigs[channel]);
  }
  
  command uint8_t DeviceMetadata.getSignificantBits()
  {
    return 12;
  }
}
