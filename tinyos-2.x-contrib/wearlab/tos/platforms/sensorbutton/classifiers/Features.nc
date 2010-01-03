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
 * 
 *
 * 
 * @author Andreas Bubenhofer
 */

interface Features {
  /**
   * 
   */
  command void getFeatures( uint16_t* features, uint16_t ** psamples, uint8_t head ,uint8_t tail);

  /**
   * 
   */
  event void featuresDone( uint16_t* features);
}
