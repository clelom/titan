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

/*****************************************************************************
 * Diploma Thesis WSPack 3 2005
 * ETH Zürich, Inst. for Electronics
 *
 * Autor:               Marco Graf
 * Target:              MSP430F1611 Processor
 * Build Environment:   IAR Embedded Workbench V3.21A/W32
 * File:                calc.h
 *
 * Feature calculations
 *****************************************************************************/


#ifndef _CALC_ACC_LS_H_
#define _CALC_ACC_LS_H_

//#include "globals.h"
//#include "fft16.h" not needed for WSPACK16

 uint16_t sample_count; // for downsampling

#define CALC_NUMOFSENSORS         4 
// changed from 3 to 4 for WSPACK16
#define CALC_NUMOFFEATURES       2 //had to add this ws16

/*****************************************************************************
 * Variables
 *****************************************************************************/
/**
 * Size of the buffer for the microphone measure values
 */
//#define CALC_MIC_BUF_SIZE         2*FFTSIZE

/**
 * Size of the feature input array for the LDA of the microphone
 */
//#define CALC_MIC_FEAT_SIZE        CALC_MIC_BUF_SIZE/2

/**
 * Storage for the mic data
 */
// int16_t calc_mic_data_store[CALC_MIC_BUF_SIZE];

/**
 * FFT storage. The mic data is copied to here and the FFT are calculated
 * within this array.
 */
// int16_t calc_mic_data[CALC_MIC_BUF_SIZE];

/**
 * FFT absolute value storage. This array is used to store the absolute
 * values derived from the FFT. These are the real features for the LDA input.
 */
// uint16_t calc_fft_data_abs[CALC_MIC_BUF_SIZE/2];

/**
 * Size of the buffer for the sensors measure values
 */
#define CALC_SENSORS_BUF_SIZE     255 //uint8_t

#define CALC_SLIDING_WINDOW_SIZE    16  //20% Overlap

#define CLASSIFICATION_INTERVAL  80 //2.5sec: 80 

/**
 * Size for the calculated means, defines also the the big sequence size:
 * T_{big_sample} = MicSampletime * CALC_MEAN_SIZE
 *                = ((SHORT_SAMPLETIME+1)*FFTSIZE)/32768 * CALC_MEAN_SIZE
 *               ~= 0.5s =>  32768Hz/((SHORT_SAMPLETIME+1)*FFTSIZE) * 0.5s
 * 11 =>  0.516s
 */
#define CALC_MEAN_SIZE            5//11

#define CALC_ENERGY_SIZE          5

/**
 * Size for the calculated peakcounts, defines also the the big sequence size:
 * T_{big_sample} = MicSampletime * CALC_PEAK_SIZE
 *                = ((SHORT_SAMPLETIME+1)*FFTSIZE)/32768 * CALC_PEAK_SIZE
 *               ~= 2s => 32768Hz/((SHORT_SAMPLETIME+1)*FFTSIZE) * 2s
 * 43 =>  2.016s
 */
#define CALC_PEAK_SIZE            1//43

/**
 * Minimal Peaksize to be recognised by peakcount
 */
#define CALC_PEAKS_POS_THRESH     100
#define CALC_PEAKS_NEG_THRESH     100


/**
 * Struct for mean calculation storage.
 */
struct calc_mean_storage_t{
/**
 * Buffer for the sums of the values of the sensors. The maximal number of
 * 12-bit values per sum are 2^4=16 (16bit - 12bit = 4bit => 2^4) to be on
 * the save side.
 */
uint16_t sums[CALC_MEAN_SIZE];

/**
 * Buffer for the number of values for each sum in sums.
 * @see sums
 */
uint8_t num_of_values[CALC_MEAN_SIZE];

/**
 * Actual position of the Buffer for the sums and numbers.
 */
uint8_t position;

/**
 * Total sum of all values in sums. Stored for faster calculation. The maximal
 * number of 16-bit sums are 2^12=4096 (32bit(total) - 16bit(sums)
 * - 4bit(upscaling at the end) = 12bit => 2^12)
 * @see sums
 */
uint32_t total_sum;

/**
 * Total number of values in sums, means also number of values in total_sum.
 */
uint16_t total_num_of_values;
};

struct calc_energy_storage_t{
/**
 * Buffer for the sums of the values of the sensors. The maximal number of
 * 12-bit values per sum are 2^4=16 (16bit - 12bit = 4bit => 2^4) to be on
 * the save side.
 */
uint16_t sums[CALC_ENERGY_SIZE];

/**
 * Buffer for the number of values for each sum in sums.
 * @see sums
 */
uint8_t num_of_values[CALC_ENERGY_SIZE];

/**
 * Actual position of the Buffer for the sums and numbers.
 */
uint8_t position;

/**
 * Total sum of all values in sums. Stored for faster calculation. The maximal
 * number of 16-bit sums are 2^12=4096 (32bit(total) - 16bit(sums)
 * - 4bit(upscaling at the end) = 12bit => 2^12)
 * @see sums
 */
uint32_t total_sum;

/**
 * Total number of values in sums, means also number of values in total_sum.
 */
uint16_t total_num_of_values;
};



/**
 * Struct for PeakCount calculation storage.
 */
struct calc_peakcount_storage_t {
/**
 * Buffer for the peaks count of the sensors.
 */
uint8_t peaks[CALC_PEAK_SIZE];

/**
 * Pointer to the actual position of the Buffer for the peaks count of the
 * sensors.
 */
uint8_t position;

/**
 * Total sum of all values in peaks. Stored for faster calculation.
 * @see peaks
 */
uint16_t total_peaks;

/**
 * Identifier if a maximum or minimum was detected last.
 */
uint8_t was_max;

/**
 * The stored maximum
 */
uint16_t max;

/**
 * The stored minimum
 */
uint16_t min;
};


/**
 * Indicates if data is ready for classification.
 */
 uint8_t calc_data_ready;

///////////////////////////////
// private Variables
///////////////////////////////
/**
 * Buffer for the measured values of the microphone.
 */
// uint16_t calc_mic_buf[CALC_MIC_BUF_SIZE];


/**
 * Head pointer for Buffer for the measured values of the microphone.
 */
 uint16_t calc_mic_buf_head;


/**
 * Tail pointer for circular Buffer for the measured values of the sensors.
 */
 uint16_t calc_mic_buf_tail;

/**
 * Buffer for the measured values of the sensors. This is a two dimensional
 * array. The first dimension defines the sensor the second the sample.
 */
 uint16_t calc_sensors_buf[CALC_NUMOFSENSORS][CALC_SENSORS_BUF_SIZE];
/*uint16_t calc_sensors_buf[CALC_NUMOFSENSORS][CALC_SENSORS_BUF_SIZE] = {
{2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376,2376},
{2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201,2201},
{1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549,1549},
{117,117,117,117,117,117,117,117,117,117,117,117,117,117,117,117,117,117,117,117}};
*/

/**
 * Head pointer for circular Buffer for the measured values of the sensors.
 */
uint8_t calc_sensors_buf_head[CALC_NUMOFSENSORS];


/**
 * Tail pointer for circular Buffer for the measured values of the sensors.
 */
 uint8_t calc_sensors_buf_tail[CALC_NUMOFSENSORS];


/**
 * Mean calculation storage.
 * @see calc_mean_storage_t
 */
 struct calc_mean_storage_t calc_mean_storage[CALC_NUMOFSENSORS],calc_std_storage[CALC_NUMOFSENSORS];

/**
 * PeakCount calculation storage.
 * @see calc_mean_storage_t
 */
 struct calc_peakcount_storage_t calc_peakcount_storage[CALC_NUMOFSENSORS];




// to trigger the classification
 uint16_t samples_stored;

#endif // _CALC_H_
