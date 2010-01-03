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

/*
 * FreescaleMMA7260QP.nc
 *
 * Implementation of the accelerometer driver of the IfeExt sensor board. This is a basic implementation following TEP109.
 *
 * TODO: It would be nice to use the Msp430AdcMultiChannel interface to do a more synchronized sampling of the ADC channels.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

#include "sensors.h"

module FreescaleMMA7260QP {

  // user interfaces
  provides interface Read<acc_read_t*>;
  provides interface DeviceMetadata;

  // ADC interfaces
  provides interface AdcConfigure<const msp430adc12_channel_config_t*>[int channel];  
  uses interface Read<uint16_t> as ReadX;
  uses interface Read<uint16_t> as ReadY;
  uses interface Read<uint16_t> as ReadZ;
  
}

implementation {

  /// stores the values read from the accelerometer
  acc_read_t m_AccRead;

  /// initiates a read sequence X->Y->Z
  command error_t Read.read() {
    return call ReadX.read();
  }
  
  /// store value and initiate read on next channel
  event void ReadX.readDone(error_t result, uint16_t val) {
    m_AccRead.x = val;
    call ReadY.read();
  }

  /// store value and initiate read on next channel
  event void ReadY.readDone(error_t result, uint16_t val) {
    atomic m_AccRead.y = val;
    call ReadZ.read();
  }

  /// store value and signal completion
  event void ReadZ.readDone(error_t result, uint16_t val) {
    atomic m_AccRead.z = val;
    signal Read.readDone(result,&m_AccRead);
  }

   const msp430adc12_channel_config_t channelConfigs[] = {
   { // channel X
      INPUT_CHANNEL_A1, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
      SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
      SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
    },
    { // channel Y
      INPUT_CHANNEL_A2, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
      SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
      SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
    },
    { // channel Z
      INPUT_CHANNEL_A7, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
      SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_16_CYCLES,
      SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 
    }

   };

  async command const msp430adc12_channel_config_t* AdcConfigure.getConfiguration[int channel]() {
    return &(channelConfigs[channel]);
  }
  
  command uint8_t DeviceMetadata.getSignificantBits() {
    return 12;
  }

}
