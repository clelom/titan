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
 * FreescaleMMA7260QC.nc
 *
 * Sensor driver for the accelerometer on the IfeExt board. It reads the 3 axes and returns them 
 * in a struct.
 *
 * TODO: It would be nice to use the Msp430Adc12MultiChannel interface to do a faster and more synchronized channel sampling.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
 
#include "sensors.h"
 
generic configuration FreescaleMMA7260QC() {
  provides interface Read<acc_read_t*>;
  provides interface DeviceMetadata;
}

implementation {

  components new AdcReadClientC() as AdcReadClientX;
  components new AdcReadClientC() as AdcReadClientY;
  components new AdcReadClientC() as AdcReadClientZ;
  components FreescaleMMA7260QP;

  AdcReadClientX.AdcConfigure -> FreescaleMMA7260QP.AdcConfigure[IFEEXT_ACC_X];
  AdcReadClientY.AdcConfigure -> FreescaleMMA7260QP.AdcConfigure[IFEEXT_ACC_Y];
  AdcReadClientZ.AdcConfigure -> FreescaleMMA7260QP.AdcConfigure[IFEEXT_ACC_Z];

  FreescaleMMA7260QP.ReadX -> AdcReadClientX;
  FreescaleMMA7260QP.ReadY -> AdcReadClientY;
  FreescaleMMA7260QP.ReadZ -> AdcReadClientZ;
  
  Read = FreescaleMMA7260QP;
  DeviceMetadata = FreescaleMMA7260QP;

}
