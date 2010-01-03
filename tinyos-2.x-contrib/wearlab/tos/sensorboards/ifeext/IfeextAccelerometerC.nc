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
 * IfeextAccelerometerC.nc
 *
 * Wrapper for the accelerometer on the IfeExt board built by Fabio Gambarara.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
 
#include "sensors.h"
 
generic configuration IfeextAccelerometerC() {
  provides interface Read<acc_read_t*>;
  provides interface DeviceMetadata;
}

implementation {
  components new FreescaleMMA7260QC();
  Read = FreescaleMMA7260QC;
  DeviceMetadata = FreescaleMMA7260QC;
}
