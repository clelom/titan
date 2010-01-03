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
 * sensors.h
 *
 * Data structure used by the IfeExt sensorboard.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

#ifndef IFEEXT_SENSORBOARD_H
#define IFEEXT_SENSORBOARD_H

// data structure for the 3-axis accelerometer readings
typedef struct acc_read_t {
  uint16_t x;
  uint16_t y;
  uint16_t z;
} acc_read_t;

// ADC channel definitions
enum{
  IFEEXT_ACC_X=0,
  IFEEXT_ACC_Y=1, 
  IFEEXT_ACC_Z=2
};

#endif
