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
 * CHANNEL settings for Sensor Button ADC
 *
 * - Revision -------------------------------------------------------------
 * $Revision: 1.6 $
 * $Date: 2006/10/31
 * @author Andreas Bubenhofer
 * ========================================================================
 */

#ifndef SENSORS_H
#define SENSORS_H

#include <Msp430Adc12.h>



enum {
  
  LIGHT_CHANNEL,
  
  ACC_CHANNEL,
  ACC_CHANNEL_X,
  ACC_CHANNEL_Y,
  ACC_CHANNEL_Z,
  
  MIC_CHANNEL,
  MIC_CHANNEL_DMA,

  DEFAULT_CHANNEL // NONE
};

const msp430adc12_channel_config_t sensorconfigurations[] = {
	
    /* LIGHT_CHANNEL */
    {
        INPUT_CHANNEL_A3, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_512_CYCLES,
        SAMPCON_SOURCE_TACLK, SAMPCON_CLOCK_DIV_1
    },
    /* ACC_CHANNEL */
    {
        INPUT_CHANNEL_A6, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_512_CYCLES,
        SAMPCON_SOURCE_ACLK, SAMPCON_CLOCK_DIV_1
    },
	/* ACC_CHANNEL_X */
    {
        INPUT_CHANNEL_A6, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_512_CYCLES,
        SAMPCON_SOURCE_TACLK, SAMPCON_CLOCK_DIV_1
    },
	/* ACC_CHANNEL_Y */
    {
        INPUT_CHANNEL_A5, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_512_CYCLES,
        SAMPCON_SOURCE_TACLK, SAMPCON_CLOCK_DIV_1
    },
	/* ACC_CHANNEL_Z*/
    {
        INPUT_CHANNEL_A4, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_512_CYCLES,
        SAMPCON_SOURCE_TACLK, SAMPCON_CLOCK_DIV_1
    },
	/* MIC_CHANNEL */
    {
        INPUT_CHANNEL_A7, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_256_CYCLES,
        SAMPCON_SOURCE_ACLK, SAMPCON_CLOCK_DIV_1
    },
        /* MIC_CHANNEL_DMA */
    {
        INPUT_CHANNEL_A7, REFERENCE_AVcc_AVss, REFVOLT_LEVEL_2_5,
        SHT_SOURCE_ADC12OSC, SHT_CLOCK_DIV_1, SAMPLE_HOLD_64_CYCLES,
        SAMPCON_SOURCE_ACLK, SAMPCON_CLOCK_DIV_1
    },
    
    /* DEFAULT_CHANNEL (NONE) */
    {
        INPUT_CHANNEL_NONE,0,0,0,0,0,0,0
    }
};


#endif

