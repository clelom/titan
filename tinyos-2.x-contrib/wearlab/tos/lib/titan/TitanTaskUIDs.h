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

/**
 * TitanTaskUIDs.h
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Definition of Universal Task Identifiers.
 *
 */

#ifndef TITANTASKUIDS_H
#define TITANTASKUIDS_H

typedef uint16_t TitanTaskUID;

	/**
	 * All tasks created need to have a Universal Task Identifier in order to 
	 * be distinguishable in the network.
	 */

enum {
  TITAN_TASK_TEMPLATE=((uint16_t)-1),
  TITAN_COMM_MODULE         =  0,
  TITAN_TASK_SIMPLEWRITER   =  1,
  TITAN_TASK_AVERAGE        =  2,
  TITAN_TASK_DUPLICATOR     =  3,
  TITAN_TASK_LEDS           =  4,
  TITAN_TASK_MEAN           =  5,
  TITAN_TASK_ADC            =  6,
  TITAN_TASK_THRESHOLD      =  7,
  TITAN_TASK_VARIANCE       =  8,
  TITAN_TASK_TRANSDETECT    =  9,
  TITAN_TASK_ZEROCROSS      = 10, 
  TITAN_TASK_MAX            = 11,
  TITAN_TASK_FFT            = 12,
  TITAN_TASK_MERGE          = 13,
  TITAN_TASK_NUMAVG         = 14,
  TITAN_TASK_FBANDENERGY    = 15,
  TITAN_TASK_SYNCHRONIZER   = 16,
  TITAN_TASK_SINK           = 17, 
  TITAN_TASK_ACCELEROMETER  = 18,
  TITAN_TASK_KNN            = 19,
  TITAN_TASK_SIMILARITY     = 20,
  TITAN_TASK_MAGNITUDE	    = 21,
  TITAN_TASK_VOICE          = 22,
  TITAN_TASK_LIGHTSENSOR    = 23,
  TITAN_TASK_SWITCH         = 24,
  TITAN_TASK_COVARIANCE     = 25,
  TITAN_TASK_PRESSURESENSOR = 26,
  TITAN_TASK_SUM            = 27,
  TITAN_TASK_FUZZY          = 28,
  TITAN_TASK_DECISIONTREE   = 29,
  TITAN_TASK_MIN            = 30,
  TITAN_TASK_GSENSE         = 31,
  TITAN_TASK_GRAPHPLOT      = 32,
  TITAN_TASK_ROUNDTRIP      = 33
};

#endif
