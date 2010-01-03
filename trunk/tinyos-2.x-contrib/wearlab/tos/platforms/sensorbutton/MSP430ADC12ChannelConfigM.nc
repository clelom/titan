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
 * @author Andreas Bubenhofer
 * @author Christoph Walser
 */

module MSP430ADC12ChannelConfigM {
  uses interface MSP430ADC12ChannelConfig;  
}

implementation
{
  async event msp430adc12_channel_config_t MSP430ADC12ChannelConfig.getConfigurationData(uint8_t channel) {
    msp430adc12_channel_config_t config = {
      channel, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_1_5,
      SHT_SOURCE_SMCLK, SHT_CLOCK_DIV_1, SAMPLE_HOLD_4_CYCLES,
      SAMPCON_SOURCE_SMCLK, SAMPCON_CLOCK_DIV_1 };

    return config;
  }
}

